import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.io.*;
import java.util.jar.*;

/**
 * Patcher: Injeta pose de pulo SÓ QUANDO O PLAYER ESTA NO AR
 *
 * - Mantém animação vanilla (super.func_78087_a)
 * - Se entityIn != null && !entityIn.field_70122_E (no ar):
 *     aplica pose de pulo (agachamento + braços pra frente)
 * - Senão: pose normal vanilla
 */
public class InjectJumpPose {

    // Pose angles em GRAUS (baseado na foto que o pai mandou)
    static final float BODY_X = 10.0f;          // corpo levemente inclinado
    static final float RARM_X = -90.0f;         // braço direito pra frente
    static final float LARM_X = -90.0f;         // braço esquerdo pra frente
    static final float RARM_Z = -10.0f;         // braço direito aberto
    static final float LARM_Z = 10.0f;          // braço esquerdo aberto
    static final float RLEG_X = -15.0f;         // coxa direita levantada
    static final float LLEG_X = -15.0f;         // coxa esquerda levantada
    static final float KNEE = 30.0f;            // joelhos dobrados

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Uso: java -cp asm-all-5.0.3.jar:. InjectJumpPose <in.jar> <out.jar>");
            System.exit(1);
        }
        String inJar = args[0];
        String outJar = args[1];

        File outFile = new File(outJar);
        if (outFile.exists()) outFile.delete();

        try (JarFile jar = new JarFile(inJar);
             JarOutputStream jos = new JarOutputStream(new FileOutputStream(outJar))) {

            jar.stream().forEach(entry -> {
                String name = entry.getName();
                try {
                    if (name.endsWith("/")) return;
                    if (name.equals("net/gobbob/bettermove/BetterMovePlayerModel.class")) {
                        System.out.println("[INJECT] Patcheando: " + name);
                        InputStream is = jar.getInputStream(entry);
                        byte[] patched = injectClass(is);
                        is.close();
                        JarEntry newEntry = new JarEntry(name);
                        jos.putNextEntry(newEntry);
                        jos.write(patched);
                        jos.closeEntry();
                        System.out.println("[OK] Pose de pulo condicional injetada!");
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
        }
        System.out.println("[DONE] JAR gerado: " + outJar);
    }

    static InsnList setField(String fieldA, String descA, float value, String fieldB, String descB) {
        InsnList list = new InsnList();
        list.add(new VarInsnNode(Opcodes.ALOAD, 0));
        list.add(new FieldInsnNode(Opcodes.GETFIELD,
            "net/gobbob/bettermove/BetterMovePlayerModel", fieldA, descA));
        list.add(new LdcInsnNode(value));
        list.add(new FieldInsnNode(Opcodes.PUTFIELD,
            "net/minecraft/client/model/ModelRenderer", fieldB, descB));
        return list;
    }

    static byte[] injectClass(InputStream is) throws Exception {
        ClassReader cr = new ClassReader(is);
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);

        boolean patched = false;
        @SuppressWarnings("unchecked")
        java.util.List<MethodNode> methodList = cn.methods;

        for (MethodNode mn : methodList) {
            if (mn.name.equals("func_78087_a") && mn.desc.equals("(FFFFFFLnet/minecraft/entity/Entity;)V")) {
                System.out.println("  [FOUND] " + mn.name + mn.desc);
                System.out.println("  [BEFORE] " + mn.instructions.size() + " instrucoes");

                InsnList ins = mn.instructions;
                String MR = "Lnet/minecraft/client/model/ModelRenderer;";
                String BMP = "net/gobbob/bettermove/BetterMovePlayerModel";
                String ENT = "Lnet/minecraft/entity/Entity;";

                // === Acha o super call ===
                AbstractInsnNode superCall = null;
                AbstractInsnNode cursor = ins.getFirst();
                while (cursor != null) {
                    if (cursor instanceof MethodInsnNode) {
                        MethodInsnNode mi = (MethodInsnNode) cursor;
                        if (mi.name.equals("func_78087_a") && mi.owner.equals("net/minecraft/client/model/ModelBiped")) {
                            superCall = cursor;
                            break;
                        }
                    }
                    cursor = cursor.getNext();
                }
                if (superCall == null) throw new RuntimeException("Nao achei super!");

                // === Remove TUDO depois do super ===
                AbstractInsnNode cur = superCall.getNext();
                while (cur != null) {
                    AbstractInsnNode next = cur.getNext();
                    ins.remove(cur);
                    cur = next;
                }

                // === Constrói o código novo ===
                // Estrutura:
                //   super.func_78087_a(...)  <-- já existe, deixa
                //   this.field_78117_n = false  <-- precisa adicionar!
                //   if (entityIn == null || entityIn.field_70122_E) return;  <-- se no chão, sai
                //   <aplica pose de pulo>
                //   return

                InsnList code = new InsnList();

                // 1. this.field_78117_n = false
                code.add(new VarInsnNode(Opcodes.ALOAD, 0));
                code.add(new InsnNode(Opcodes.ICONST_0));
                code.add(new FieldInsnNode(Opcodes.PUTFIELD,
                    BMP, "field_78117_n", "Z"));

                // 2. if (entityIn == null) return  (se entity é null, não aplica)
                //    Carrega entityIn (param 7)
                code.add(new VarInsnNode(Opcodes.ALOAD, 7));
                LabelNode notNull = new LabelNode();
                code.add(new JumpInsnNode(Opcodes.IFNONNULL, notNull));
                code.add(new InsnNode(Opcodes.RETURN));
                code.add(notNull);

                // 3. if (entityIn.field_70122_E) return  (se no chão, não aplica)
                code.add(new VarInsnNode(Opcodes.ALOAD, 7));
                code.add(new FieldInsnNode(Opcodes.GETFIELD,
                    "net/minecraft/entity/Entity", "field_70122_E", "Z"));
                LabelNode inAir = new LabelNode();
                code.add(new JumpInsnNode(Opcodes.IFEQ, inAir));  // if onGround == 0 (no ar), pula pro bloco de pulo
                code.add(new InsnNode(Opcodes.RETURN));
                code.add(inAir);

                // 4. Bloco de pose de pulo (só roda se tá no ar)
                code.add(setField("field_78115_e", MR, BODY_X, "field_78795_f", "F"));
                code.add(setField("field_78112_f", MR, RARM_X, "field_78795_f", "F"));
                code.add(setField("field_78112_f", MR, RARM_Z, "field_78808_h", "F"));
                code.add(setField("field_78113_g", MR, LARM_X, "field_78795_f", "F"));
                code.add(setField("field_78113_g", MR, LARM_Z, "field_78808_h", "F"));
                code.add(setField("field_78123_h", MR, RLEG_X, "field_78795_f", "F"));
                code.add(setField("field_78124_i", MR, LLEG_X, "field_78795_f", "F"));

                // setLowerAngles(0, 0, KNEE, KNEE)
                code.add(new VarInsnNode(Opcodes.ALOAD, 0));
                code.add(new LdcInsnNode(0.0f));
                code.add(new LdcInsnNode(0.0f));
                code.add(new LdcInsnNode(KNEE));
                code.add(new LdcInsnNode(KNEE));
                code.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                    BMP, "setLowerAngles", "(FFFF)V", false));

                // setTorsoAngles(BODY_X, 0, 0)
                code.add(new VarInsnNode(Opcodes.ALOAD, 0));
                code.add(new LdcInsnNode(BODY_X));
                code.add(new LdcInsnNode(0.0f));
                code.add(new LdcInsnNode(0.0f));
                code.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                    BMP, "setTorsoAngles", "(FFF)V", false));

                // 5. RETURN
                code.add(new InsnNode(Opcodes.RETURN));

                // === Insere o bloco novo depois do super ===
                ins.insert(code);

                System.out.println("  [AFTER] " + mn.instructions.size() + " instrucoes");
                patched = true;
            }
        }

        if (!patched) throw new RuntimeException("Metodo func_78087_a nao encontrado!");

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cn.accept(cw);
        return cw.toByteArray();
    }
}
