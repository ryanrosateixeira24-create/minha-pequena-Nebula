import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.io.*;
import java.util.jar.*;

/**
 * Patcher: Injeta animação de pulo estilo Mixamo no BetterMovePlayerModel
 *
 * O que faz:
 * - Substitui o método applyJumpAnimation inteiro
 * - A animação tem 4 fases baseadas no motionY (velocidade vertical do player):
 *   * motionY > +0.2  → PROPULSION (subindo rápido, braços pra cima)
 *   * +0.2 > motionY > -0.1 → APEX (no topo, suspendido)
 *   * -0.1 > motionY > -0.4 → FALL (caindo, preparando pouso)
 *   * motionY < -0.4   → LAND (quase aterrissando, joelhos dobrados)
 *
 * USO: java -cp "asm-all-5.0.3.jar" InjectMixamoJump <in.jar> <out.jar>
 */
public class InjectMixamoJump {

    // === ÂNGULOS EM GRAUS (vindos do JSON) ===
    // Cada fase: [bodyX, headX, rArmX, rArmY, lArmX, lArmY, rLegX, rLegZ, lLegX, lLegZ, knee]

    static final float[] ANTICIPATION = {25, -10, -60, 0, -60, 0, -45, 0, -45, 0, 80};
    static final float[] PROPULSION    = {-10, 15, -150, -20, -150, 20, 20, -10, 20, 10, 20};
    static final float[] APEX          = {5, 0, -90, -45, -90, 45, -15, -10, 15, 10, 25};
    static final float[] FALL          = {15, 10, -45, -30, -45, 30, -30, -10, 30, 10, 40};
    static final float[] LAND          = {30, -15, -90, -25, -90, 25, -60, 0, -60, 0, 90};

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Uso: java -cp asm-all-5.0.3.jar:. InjectMixamoJump <in.jar> <out.jar>");
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
                        System.out.println("[OK] Animacao de pulo Mixamo injetada!");
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

    // Helper: cria instrucoes para "this.bone.rotateAngleX = value"
    static InsnList setBoneRot(String field, float value) {
        InsnList list = new InsnList();
        list.add(new VarInsnNode(Opcodes.ALOAD, 0));
        list.add(new FieldInsnNode(Opcodes.GETFIELD,
            "net/gobbob/bettermove/BetterMovePlayerModel", field,
            "Lnet/minecraft/client/model/ModelRenderer;"));
        list.add(new LdcInsnNode(value));
        list.add(new FieldInsnNode(Opcodes.PUTFIELD,
            "net/minecraft/client/model/ModelRenderer", "field_78795_f", "F"));
        return list;
    }

    static InsnList setBoneRotY(String field, float value) {
        InsnList list = new InsnList();
        list.add(new VarInsnNode(Opcodes.ALOAD, 0));
        list.add(new FieldInsnNode(Opcodes.GETFIELD,
            "net/gobbob/bettermove/BetterMovePlayerModel", field,
            "Lnet/minecraft/client/model/ModelRenderer;"));
        list.add(new LdcInsnNode(value));
        list.add(new FieldInsnNode(Opcodes.PUTFIELD,
            "net/minecraft/client/model/ModelRenderer", "field_78796_g", "F"));
        return list;
    }

    static InsnList setBoneRotZ(String field, float value) {
        InsnList list = new InsnList();
        list.add(new VarInsnNode(Opcodes.ALOAD, 0));
        list.add(new FieldInsnNode(Opcodes.GETFIELD,
            "net/gobbob/bettermove/BetterMovePlayerModel", field,
            "Lnet/minecraft/client/model/ModelRenderer;"));
        list.add(new LdcInsnNode(value));
        list.add(new FieldInsnNode(Opcodes.PUTFIELD,
            "net/minecraft/client/model/ModelRenderer", "field_78808_h", "F"));
        return list;
    }

    // Helper: aplica uma fase da animação
    static InsnList applyPhase(float[] phase) {
        InsnList code = new InsnList();
        // 0: bodyX, 1: headX, 2: rArmX, 3: rArmY, 4: lArmX, 5: lArmY,
        // 6: rLegX, 7: rLegZ, 8: lLegX, 9: lLegZ, 10: knee
        code.add(setBoneRot("field_78115_e", phase[0]));   // body X
        code.add(setBoneRot("field_78116_c", phase[1]));   // head X (field_78116_c = bipedHead)
        code.add(setBoneRot("field_78112_f", phase[2]));   // rArm X
        code.add(setBoneRotY("field_78112_f", phase[3]));  // rArm Y
        code.add(setBoneRot("field_78113_g", phase[4]));   // lArm X
        code.add(setBoneRotY("field_78113_g", phase[5]));  // lArm Y
        code.add(setBoneRot("field_78123_h", phase[6]));   // rLeg X
        code.add(setBoneRotZ("field_78123_h", phase[7]));  // rLeg Z
        code.add(setBoneRot("field_78124_i", phase[8]));   // lLeg X
        code.add(setBoneRotZ("field_78124_i", phase[9]));  // lLeg Z
        // setLowerAngles(cotovelo, cotovelo, joelho, joelho)
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new LdcInsnNode(0.0f));  // cotovelo direito
        code.add(new LdcInsnNode(0.0f));  // cotovelo esquerdo
        code.add(new LdcInsnNode(phase[10]));  // joelho direito
        code.add(new LdcInsnNode(phase[10]));  // joelho esquerdo
        code.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
            "net/gobbob/bettermove/BetterMovePlayerModel",
            "setLowerAngles", "(FFFF)V", false));
        return code;
    }

    static byte[] injectClass(InputStream is) throws Exception {
        ClassReader cr = new ClassReader(is);
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);

        boolean patched = false;
        @SuppressWarnings("unchecked")
        java.util.List<MethodNode> methodList = cn.methods;

        for (MethodNode mn : methodList) {
            // Acha applyJumpAnimation(float, float, float)
            if (mn.name.equals("applyJumpAnimation") && mn.desc.equals("(FFF)V")) {
                System.out.println("  [FOUND] " + mn.name + mn.desc);
                System.out.println("  [BEFORE] " + mn.instructions.size() + " instrucoes");

                // Limpa o método (instruções, try-catch E local variable table)
                mn.instructions.clear();
                mn.tryCatchBlocks.clear();
                mn.localVariables.clear();  // ← ESTA É A CHAVE! Sem isso dá crash

                InsnList code = new InsnList();

                // === LÓGICA ===
                // if (motionY > 0.2) → PROPULSION
                // else if (motionY > -0.1) → APEX
                // else if (motionY > -0.4) → FALL
                // else → LAND

                // if (motionY > 0.2f) goto propulsion
                // FCMPL: compara motionY e 0.2, deixa 1/0/-1 na stack
                // IFGT: pula se valor > 0 (ou seja, motionY > 0.2)
                code.add(new VarInsnNode(Opcodes.FLOAD, 1));     // push motionY
                code.add(new LdcInsnNode(0.2f));                 // push 0.2
                code.add(new InsnNode(Opcodes.FCMPL));           // motionY - 0.2 (consome os 2, deixa int 1/0/-1)
                LabelNode propulsionLabel = new LabelNode();
                code.add(new JumpInsnNode(Opcodes.IFGT, propulsionLabel));  // if result > 0, motionY > 0.2 → propulsion

                // === APEX (motionY <= 0.2 && motionY > -0.1) ===
                code.add(new VarInsnNode(Opcodes.FLOAD, 1));
                code.add(new LdcInsnNode(-0.1f));
                code.add(new InsnNode(Opcodes.FCMPL));
                LabelNode fallLabel = new LabelNode();
                code.add(new JumpInsnNode(Opcodes.IFLE, fallLabel));  // if motionY <= -0.1, vai pra fall
                code.add(applyPhase(APEX));
                code.add(new InsnNode(Opcodes.RETURN));

                // === FALL (motionY <= -0.1 && motionY > -0.4) ===
                code.add(fallLabel);
                code.add(new VarInsnNode(Opcodes.FLOAD, 1));
                code.add(new LdcInsnNode(-0.4f));
                code.add(new InsnNode(Opcodes.FCMPL));
                LabelNode landLabel = new LabelNode();
                code.add(new JumpInsnNode(Opcodes.IFLE, landLabel));  // if motionY <= -0.4, vai pra land
                code.add(applyPhase(FALL));
                code.add(new InsnNode(Opcodes.RETURN));

                // === LAND (motionY <= -0.4) ===
                code.add(landLabel);
                code.add(applyPhase(LAND));
                code.add(new InsnNode(Opcodes.RETURN));

                // === PROPULSION (motionY > 0.2) ===
                code.add(propulsionLabel);
                code.add(applyPhase(PROPULSION));
                code.add(new InsnNode(Opcodes.RETURN));

                mn.instructions.add(code);

                // Marca que houve mudança de tamanho do frame
                mn.maxStack = 6;
                mn.maxLocals = 4;

                System.out.println("  [AFTER] " + mn.instructions.size() + " instrucoes");
                patched = true;
            }
        }

        if (!patched) throw new RuntimeException("Metodo applyJumpAnimation nao encontrado!");

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cn.accept(cw);
        return cw.toByteArray();
    }
}
