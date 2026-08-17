import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.io.*;
import java.util.jar.*;

/**
 * Patch v2 - USA TEXTURAS VANILLA DO MC
 * - Blockstate aponta pra minecraft:blocks/wool_colored_*
 * - Sem texturas customizadas (que crashavam)
 * - Sem models customizados (usa o cube_all vanilla)
 * - 4 blocos Tower indestrutíveis (white, red, gray, green)
 */
public class PatchAtelieTowersV2 {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Uso: java -cp asm-all-5.0.3.jar:. PatchAtelieTowersV2 <in.jar> <out.jar>");
            System.exit(1);
        }
        String inJar = args[0];
        String outJar = args[1];

        File outFile = new File(outJar);
        if (outFile.exists()) outFile.delete();

        // Carrega langs existentes
        String origEnUS = readLangFile(inJar, "assets/nebulaatelier/lang/en_US.lang");
        String origPtBR = readLangFile(inJar, "assets/nebulaatelier/lang/pt_BR.lang");

        try (JarFile jar = new JarFile(inJar);
             JarOutputStream jos = new JarOutputStream(new FileOutputStream(outJar))) {

            // Copia TUDO, exceto os blockstates/models/textures antigos dos towers
            jar.stream().forEach(entry -> {
                String name = entry.getName();
                try {
                    if (name.endsWith("/")) return;
                    // Pula os arquivos velhos dos towers
                    if (name.contains("/tower_") || name.contains("/towers/")) return;
                    // Pula as langs (vamos adicionar manualmente)
                    if (name.equals("assets/nebulaatelier/lang/en_US.lang") ||
                        name.equals("assets/nebulaatelier/lang/pt_BR.lang")) return;

                    if (name.equals("com/nebula/atelier/NebulaAtelier.class")) {
                        System.out.println("[PATCH] Modificando: " + name);
                        InputStream is = jar.getInputStream(entry);
                        byte[] patched = patchNebulaAtelier(is);
                        is.close();
                        jos.putNextEntry(new JarEntry(name));
                        jos.write(patched);
                        jos.closeEntry();
                    } else {
                        jos.putNextEntry(new JarEntry(name));
                        try (InputStream is = jar.getInputStream(entry)) {
                            byte[] buf = new byte[8192];
                            int n;
                            while ((n = is.read(buf)) > 0) jos.write(buf, 0, n);
                        }
                        jos.closeEntry();
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Erro processando " + name, e);
                }
            });

            // === ADICIONA AS 4 CLASSES TOWER (extends Block direto) ===
            String[] colors = {"White", "Red", "Gray", "Green"};
            String[] names = {"white", "red", "gray", "green"};
            for (int i = 0; i < 4; i++) {
                addClass(jos, "com/nebula/atelier/towers/Tower" + colors[i] + ".class",
                    BuildSimpleTowers.generateTowerClass("Tower" + colors[i], "tower_" + names[i]));
            }

            // === BLOCKSTATES que apontam pra wool vanilla do MC ===
            String[] woolColors = {"white", "red", "gray", "green"};
            for (int i = 0; i < 4; i++) {
                // USA TEXTURA VANILLA: minecraft:blocks/wool_colored_white
                String blockstate =
                    "{\"variants\":{\"normal\":{\"model\":\"minecraft:cube_all\"," +
                    "\"textures\":{\"all\":\"minecraft:blocks/wool_colored_" + woolColors[i] + "\"}}}}";
                addText(jos, "assets/nebulaatelier/blockstates/tower_" + woolColors[i] + ".json", blockstate);
            }

            // === ADICIONA LANG (com entries novos) ===
            addText(jos, "assets/nebulaatelier/lang/en_US.lang",
                origEnUS +
                "tile.nebulaatelier:tower_white.name=Indestructible White\n" +
                "tile.nebulaatelier:tower_red.name=Indestructible Red\n" +
                "tile.nebulaatelier:tower_gray.name=Indestructible Gray\n" +
                "tile.nebulaatelier:tower_green.name=Indestructible Green\n");
            addText(jos, "assets/nebulaatelier/lang/pt_BR.lang",
                origPtBR +
                "tile.nebulaatelier:tower_white.name=Branco Indestrutivel\n" +
                "tile.nebulaatelier:tower_red.name=Vermelho Indestrutivel\n" +
                "tile.nebulaatelier:tower_gray.name=Cinza Indestrutivel\n" +
                "tile.nebulaatelier:tower_green.name=Verde Indestrutivel\n");
        }
        System.out.println("[DONE] JAR gerado: " + outJar);
    }

    static String readLangFile(String jarPath, String path) {
        try (JarFile jar = new JarFile(jarPath)) {
            InputStream is = jar.getInputStream(jar.getEntry(path));
            byte[] buf = new byte[8192];
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            int n;
            while ((n = is.read(buf)) > 0) baos.write(buf, 0, n);
            is.close();
            return baos.toString("UTF-8");
        } catch (Exception e) {
            return "";
        }
    }

    static void addClass(JarOutputStream jos, String path, byte[] data) throws IOException {
        jos.putNextEntry(new JarEntry(path));
        jos.write(data);
        jos.closeEntry();
        System.out.println("  [+] Adicionado: " + path);
    }

    static void addText(JarOutputStream jos, String path, String text) throws IOException {
        jos.putNextEntry(new JarEntry(path));
        jos.write(text.getBytes("UTF-8"));
        jos.closeEntry();
    }

    static byte[] patchNebulaAtelier(InputStream is) throws Exception {
        ClassReader cr = new ClassReader(is);
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);

        @SuppressWarnings("unchecked")
        java.util.List<MethodNode> methodList = cn.methods;
        for (MethodNode mn : methodList) {
            if (mn.name.equals("preInit") && mn.desc.equals("(Lcpw/mods/fml/common/event/FMLPreInitializationEvent;)V")) {
                System.out.println("  [+] Patching preInit()");

                // Adiciona 4 fields estáticos
                addFieldIfMissing(cn, "towerWhite", "Lnet/minecraft/block/Block;");
                addFieldIfMissing(cn, "towerRed", "Lnet/minecraft/block/Block;");
                addFieldIfMissing(cn, "towerGray", "Lnet/minecraft/block/Block;");
                addFieldIfMissing(cn, "towerGreen", "Lnet/minecraft/block/Block;");

                // Constrói código
                InsnList inject = new InsnList();
                String[] colors = {"White", "Red", "Gray", "Green"};
                String[] names = {"white", "red", "gray", "green"};
                for (int i = 0; i < 4; i++) {
                    String className = "com/nebula/atelier/towers/Tower" + colors[i];
                    String field = "tower" + colors[i];
                    String registry = "tower_" + names[i];

                    // NebulaAtelier.towerWhite = new TowerWhite()
                    inject.add(new TypeInsnNode(Opcodes.NEW, className));
                    inject.add(new InsnNode(Opcodes.DUP));
                    inject.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, className, "<init>", "()V", false));
                    inject.add(new FieldInsnNode(Opcodes.PUTSTATIC,
                        "com/nebula/atelier/NebulaAtelier", field, "Lnet/minecraft/block/Block;"));

                    // GameRegistry.registerBlock(NebulaAtelier.towerWhite, "tower_white")
                    inject.add(new FieldInsnNode(Opcodes.GETSTATIC,
                        "com/nebula/atelier/NebulaAtelier", field, "Lnet/minecraft/block/Block;"));
                    inject.add(new LdcInsnNode(registry));
                    inject.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                        "cpw/mods/fml/common/registry/GameRegistry", "registerBlock",
                        "(Lnet/minecraft/block/Block;Ljava/lang/String;)Lnet/minecraft/block/Block;", false));
                    inject.add(new InsnNode(Opcodes.POP));
                }

                // Insere no FINAL do preInit (antes do RETURN)
                AbstractInsnNode ret = mn.instructions.getLast();
                while (ret != null && ret.getOpcode() != Opcodes.RETURN) {
                    ret = ret.getPrevious();
                }
                if (ret != null) {
                    mn.instructions.insertBefore(ret, inject);
                } else {
                    mn.instructions.add(inject);
                }
                mn.maxStack = 4;
            }
        }

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cn.accept(cw);
        return cw.toByteArray();
    }

    @SuppressWarnings("unchecked")
    static void addFieldIfMissing(ClassNode cn, String name, String desc) {
        java.util.List<FieldNode> fields = cn.fields;
        for (FieldNode f : fields) {
            if (f.name.equals(name) && f.desc.equals(desc)) return;
        }
        FieldNode fn = new FieldNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
            name, desc, null, null);
        fields.add(fn);
    }
}
