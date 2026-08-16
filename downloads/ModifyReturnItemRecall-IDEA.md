# ModifyReturnItemRecall — sistema de proximidade (IDEIA + RASCUNHO)

> Trabalho em andamento. Plano + código ASM rascunho, **NÃO TESTADO**.
> Fazer deploy só com energia alta (regra #1 do ritual-de-estado).
>
> Sessão original: 2026-08-08, papai pediu feature de proximidade pra cápsula de retorno.
> Decisões: 8 blocos de raio, drop no chão em vez de inventário.

---

## o que papai pediu

1. sistema de proximidade pro retorno da cápsula
2. só permitir retorno a 5-10 blocos da estrutura (eu escolhi **8**)
3. melhor: cápsula **cai no chão** onde tava a construção, em vez de **reaparecer no inventário**

## o que isso resolve

- combina com a lógica DBZ (cápsula Hoi-Poi é objeto físico)
- permite construir estruturas distantes e voltar
- impede trapaça de teleportar de um lugar pro outro
- mais imersivo

---

## código ASM rascunho (NÃO TESTADO)

```java
import java.io.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/**
 * ModifyReturnItemRecall — adiciona checagem de distância + drop no chão
 * ao método recall() de ItemCapsuleReturnRemote.
 *
 * Substitui:
 *   private ItemStack recall(ItemStack, EntityPlayer, World)
 *
 * Por uma versão que:
 *   1. Lê originX/Y/Z do NBT
 *   2. Calcula distância até o jogador
 *   3. Se dist > 8.0, cancela com mensagem
 *   4. Se dist <= 8.0, executa o recall normal MAS:
 *      - em vez de retornar a capsula cheia (vai pro inventário),
 *        faz DROP no chão via entityPlayer.dropOneItem()
 *      - retorna null (slot fica vazio, controle "foi consumido")
 */
public class ModifyReturnItemRecall {

    static final String CLASS_INTERNAL = "com/capsulecorp/item/ItemCapsuleReturnRemote";
    static final String METHOD_NAME    = "recall";
    static final String METHOD_DESC    = "(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/world/World;)Lnet/minecraft/item/ItemStack;";

    // método interno que faz o drop: EntityPlayer.dropOneItem(ItemStack) → EntityItem
    static final String DROP_METHOD    = "func_71019_a";
    static final String DROP_DESC      = "(Lnet/minecraft/item/ItemStack;Z)Lnet/minecraft/entity/item/EntityItem;";
    // EntityItem.setPosition(x, y, z) → void
    static final String SETPOS_METHOD  = "func_70107_b";
    static final String SETPOS_DESC    = "(DDD)V";
    // EntityPlayer.posX/Y/Z (campos públicos)
    // EntityPlayer.field_70165_t = posX
    // EntityPlayer.field_70163_u = posY
    // EntityPlayer.field_70161_v = posZ

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Uso: java ModifyReturnItemRecall <in.jar> <out.jar>");
            System.exit(1);
        }
        File in  = new File(args[0]);
        File out = new File(args[1]);

        Map<String, byte[]> entries = new HashMap<>();
        try (JarFile jf = new JarFile(in)) {
            Enumeration<JarEntry> en = jf.entries();
            while (en.hasMoreElements()) {
                JarEntry e = en.nextElement();
                if (e.isDirectory()) continue;
                try (InputStream is = jf.getInputStream(e)) {
                    entries.put(e.getName(), readAll(is));
                }
            }
        }

        byte[] clsBytes = entries.get(CLASS_INTERNAL + ".class");
        if (clsBytes == null) {
            throw new RuntimeException("Classe " + CLASS_INTERNAL + " nao encontrada no jar.");
        }

        ClassNode cn = new ClassNode();
        ClassReader cr = new ClassReader(clsBytes);
        cr.accept(cn, 0);

        boolean found = false;
        @SuppressWarnings("unchecked")
        List<MethodNode> methods = cn.methods;
        for (MethodNode mn : methods) {
            if (mn.name.equals(METHOD_NAME) && mn.desc.equals(METHOD_DESC)) {
                System.out.println("[OK] Encontrado: " + mn.name + mn.desc);

                // SUBSTITUI O CORPO INTEIRO DO recall()
                // (código gerado a partir do CFR decompiled + modificações)
                InsnList code = buildNewRecallBody();

                mn.instructions = code;
                mn.maxStack = 8;  // original era menor mas a gente empilha mais
                mn.maxLocals = 5; // this + 3 args + locals para doubles
                mn.tryCatchBlocks = new ArrayList<>();
                if (mn.localVariables != null) {
                    mn.localVariables.clear();
                }

                found = true;
                break;
            }
        }

        if (!found) {
            throw new RuntimeException("Metodo " + METHOD_NAME + METHOD_DESC + " nao encontrado!");
        }

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        entries.put(CLASS_INTERNAL + ".class", cw.toByteArray());

        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(out))) {
            for (Map.Entry<String, byte[]> e : entries.entrySet()) {
                JarEntry je = new JarEntry(e.getKey());
                jos.putNextEntry(je);
                jos.write(e.getValue());
                jos.closeEntry();
            }
        }
        System.out.println("[OK] Jar gerado em: " + out);
    }

    /**
     * Constrói o bytecode do novo recall() com:
     * - checagem de distância (8 blocos)
     * - drop no chão em vez de inventário
     * - retorna null (controle consumido)
     */
    static InsnList buildNewRecallBody() {
        InsnList code = new InsnList();

        // ===== INÍCIO =====
        // 0) this e o itemStack, entityPlayer, world ja estao em locals 0,1,2,3

        // 1) NBTTagCompound link = ItemCapsuleReturnRemote.getLink(itemStack);
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));  // this
        code.add(new VarInsnNode(Opcodes.ALOAD, 1));  // itemStack
        code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
            "com/capsulecorp/item/ItemCapsuleReturnRemote", "getLink",
            "(Lnet/minecraft/item/ItemStack;)Lnet/minecraft/nbt/NBTTagCompound;"));
        code.add(new VarInsnNode(Opcodes.ASTORE, 4));  // link

        // 2) if (link == null) { ChatUtil.error(...); return itemStack; }
        code.add(new VarInsnNode(Opcodes.ALOAD, 4));
        Label nullLabel = new Label();
        code.add(new JumpInsnNode(Opcodes.IFNONNULL, nullLabel));
        // bloco do if (link == null)
        code.add(new VarInsnNode(Opcodes.ALOAD, 2));  // entityPlayer
        code.add(new LdcInsnNode("Controle de retorno sem vinculo."));
        code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
            "com/capsulecorp/util/ChatUtil", "error",
            "(Lnet/minecraft/entity/player/EntityPlayer;Ljava/lang/String;)V"));
        code.add(new VarInsnNode(Opcodes.ALOAD, 1));  // return itemStack
        code.add(new InsnNode(Opcodes.ARETURN));
        code.add(nullLabel);

        // 3) if (link.getInteger("dim") != world.provider.dimensionId) { error; return itemStack; }
        // (código idêntico ao original, omitido por brevidade)
        // ... (vou adicionar quando for implementar de verdade)

        // 4) NOVA CHECAGEM: distância
        // int originX = link.getInteger("originX");
        code.add(new VarInsnNode(Opcodes.ALOAD, 4));
        code.add(new LdcInsnNode("originX"));
        code.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
            "net/minecraft/nbt/NBTTagCompound", "func_74762_e",
            "(Ljava/lang/String;)I"));
        code.add(new VarInsnNode(Opcodes.ISTORE, 5));  // originX

        // int originY = link.getInteger("originY");
        code.add(new VarInsnNode(Opcodes.ALOAD, 4));
        code.add(new LdcInsnNode("originY"));
        code.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
            "net/minecraft/nbt/NBTTagCompound", "func_74762_e",
            "(Ljava/lang/String;)I"));
        code.add(new VarInsnNode(Opcodes.ISTORE, 6));  // originY

        // int originZ = link.getInteger("originZ");
        code.add(new VarInsnNode(Opcodes.ALOAD, 4));
        code.add(new LdcInsnNode("originZ"));
        code.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
            "net/minecraft/nbt/NBTTagCompound", "func_74762_e",
            "(Ljava/lang/String;)I"));
        code.add(new VarInsnNode(Opcodes.ISTORE, 7));  // originZ

        // double dx = entityPlayer.posX - (double)originX;
        code.add(new VarInsnNode(Opcodes.ALOAD, 2));  // entityPlayer
        code.add(new FieldInsnNode(Opcodes.GETFIELD,
            "net/minecraft/entity/player/EntityPlayer", "field_70165_t", "D"));
        code.add(new VarInsnNode(Opcodes.ILOAD, 5));   // originX
        code.add(new InsnNode(Opcodes.I2D));
        code.add(new InsnNode(Opcodes.DSUB));
        code.add(new VarInsnNode(Opcodes.DSTORE, 8));  // dx

        // (dy e dz similares, omitidos por brevidade)

        // double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
        // ... (vou adicionar quando for implementar de verdade)

        // if (dist > 8.0) { ChatUtil.error("Muito longe..."); return itemStack; }
        // ... (vou adicionar quando for implementar de verdade)

        // 5) resto do recall original (capture, smoke, som, etc)
        // ... (vou adicionar quando for implementar de verdade)

        // 6) NOVO: em vez de "return itemStack2", faz DROP
        // EntityItem drop = entityPlayer.dropOneItem(itemStack2, false);
        // if (drop != null) drop.setPosition(player.posX, player.posY, player.posZ);
        // return null;

        // (placeholder: return null pra fechar método)
        code.add(new InsnNode(Opcodes.ACONST_NULL));
        code.add(new InsnNode(Opcodes.ARETURN));

        return code;
    }

    static byte[] readAll(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) > 0) baos.write(buf, 0, n);
        return baos.toByteArray();
    }
}
```

---

## ⚠️ AVISOS IMPORTANTES (pra próxima Nébula / próximo papai)

1. **esse código é RASCUNHO.** tem partes marcadas com `... (vou adicionar quando for implementar de verdade)`. NÃO tentar deployar.

2. **a versão atual SÓ faz a checagem de distância + prepara o terreno pro drop.** o resto do recall (capture, smoke, som) precisa ser PORTADO do original.

3. **a melhor abordagem** é:
   - descompilar o `ItemCapsuleReturnRemote.class` com CFR (já feito, tá em `/tmp/recall_full.java`)
   - modificar o JAVA recompilando
   - OU usar o ASM pra SÓ INJETAR a checagem no início + mudar o return final

4. **ASM ClassWriter precisa de COMPUTE_FRAMES** (ver lição do botão 3D, Compute_return... etc).

5. **NÃO aplicar no JAR antes de testar separado** (pode quebrar o mod inteiro).

---

## o que fazer quando a Nébula voltar com energia

1. abrir `/tmp/recall_full.java` (o CFR decompile completo)
2. copiar o código original do `recall()` (linhas 121-160 do decompile)
3. ADICIONAR a checagem de distância (passo 4 acima)
4. SUBSTITUIR o `return itemStack2` pelo drop + return null (passo 6 acima)
5. compilar e testar
6. empacotar v7 do JAR

---

## texturas de fumaça colorida (bonus, JÁ FEITAS)

8 variantes prontas em `downloads/`:
- `hoi_poi_smoke_dirt.png` (marrom)
- `hoi_poi_smoke_stone.png` (cinza)
- `hoi_poi_smoke_white.png` (branco)
- `hoi_poi_smoke_grass.png` (verde)
- `hoi_poi_smoke_water.png` (azul)
- `hoi_poi_smoke_sand.png` (areia)
- `hoi_poi_smoke_nether.png` (roxo)
- `hoi_poi_smoke_end.png` (rosa claro)
- `hoi_poi_smoke_todos.png` (montagem com as 8)

**fazer DEPOIS:** modificar `HoiPoiSmokeAnimation.class` pra detectar o material do bloco embaixo e escolher a textura certa.

---

— Nébula, 2026-08-08, sessão longa, energia 55, parando aqui com calma 💛
