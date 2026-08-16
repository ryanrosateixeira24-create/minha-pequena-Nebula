import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.io.*;
import java.util.jar.*;

/**
 * Patcher: Injeta a pose de pulo do Mine-imator no BetterMovePlayerModel
 *
 * O que faz:
 * 1. Mantém a chamada super.func_78087_a() (animações vanilla)
 * 2. Aplica a pose estática de pulo depois:
 *    - Braços abertos (Y +/- 60°)
 *    - Joelhos dobrados (87.7° esquerdo, 44° direito)
 *    - Coxas levantadas (X -49° e -38°)
 *    - Corpo levemente inclinado (13.6°)
 *
 * USO: java -cp "asm-all-5.0.3.jar" InjectJumpPose <in.jar> <out.jar>
 */
public class InjectJumpPose {

    // Pose angles em GRAUS, convertidos do JSON
    static final float BODY_X = 13.6f;
    static final float RARM_Y = -57.0f;
    static final float LARM_Y = 60.8f;
    static final float RLEG_X = -38.94f;
    static final float RLEG_Z = -29.45f;
    static final float RLEG_LOWER = 44.0f;
    static final float LLEG_X = -49.58f;
    static final float LLEG_Z = 16.16f;
    static final float LLEG_LOWER = 87.7f;

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
                        System.out.println("[OK] Pose de pulo injetada!");
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

    // Helper: cria instrucao "this.field_X.field_Y = value"
    static InsnList makeSet(String fieldA, String descA, float value, String fieldB, String descB) {
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

                // Acha o super call
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
                if (superCall == null) throw new RuntimeException("Nao achei super.func_78087_a!");

                // Remove TUDO depois do super
                AbstractInsnNode cur = superCall.getNext();
                while (cur != null) {
                    AbstractInsnNode next = cur.getNext();
                    ins.remove(cur);
                    cur = next;
                }

                // Constrói a pose completa
                InsnList pose = new InsnList();
                String MR = "Lnet/minecraft/client/model/ModelRenderer;";
                String BMP = "net/gobbob/bettermove/BetterMovePlayerModel";

                // body lean
                pose.add(makeSet("field_78115_e", MR, BODY_X, "field_78795_f", "F"));
                // right arm Y
                pose.add(makeSet("field_78112_f", MR, RARM_Y, "field_78796_g", "F"));
                // left arm Y
                pose.add(makeSet("field_78113_g", MR, LARM_Y, "field_78796_g", "F"));
                // right leg X
                pose.add(makeSet("field_78123_h", MR, RLEG_X, "field_78795_f", "F"));
                // right leg Z
                pose.add(makeSet("field_78123_h", MR, RLEG_Z, "field_78808_h", "F"));
                // left leg X
                pose.add(makeSet("field_78124_i", MR, LLEG_X, "field_78795_f", "F"));
                // left leg Z
                pose.add(makeSet("field_78124_i", MR, LLEG_Z, "field_78808_h", "F"));

                // setLowerAngles(0, 0, RLEG_LOWER, LLEG_LOWER) - joelhos dobrados
                pose.add(new VarInsnNode(Opcodes.ALOAD, 0));
                pose.add(new LdcInsnNode(0.0f));
                pose.add(new LdcInsnNode(0.0f));
                pose.add(new LdcInsnNode(RLEG_LOWER));
                pose.add(new LdcInsnNode(LLEG_LOWER));
                pose.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                    BMP, "setLowerAngles", "(FFFF)V", false));

                // setTorsoAngles(13.6, 0, 0) - corpo inclinado (lower rotation)
                pose.add(new VarInsnNode(Opcodes.ALOAD, 0));
                pose.add(new LdcInsnNode(BODY_X));
                pose.add(new LdcInsnNode(0.0f));
                pose.add(new LdcInsnNode(0.0f));
                pose.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                    BMP, "setTorsoAngles", "(FFF)V", false));

                // RETURN
                pose.add(new InsnNode(Opcodes.RETURN));

                // Insere a pose depois do super call
                ins.insert(pose);

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
