import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.io.*;
import java.util.jar.*;

/**
 * Patcher: Substitui applyJumpAnimation por versão estilo Mixamo
 *
 * Baseado na documentação DOC-COMPLETA seção 9 (Portar Animações do Mixamo)
 * e seção 2.3 (Jump Animation original do mod)
 *
 * Sistema:
 * - 4 fases baseadas em motionY (velocidade vertical)
 * - Cada fase tem ângulos pros 6 bones (body, head, rightArm, leftArm, rightLeg, leftLeg)
 * - Usa setLowerAngles() pro skinning de cotovelo/joelho (já implementado no SkinnedLimbRenderer)
 * - Usa setTorsoAngles() pro skinning do quadril
 *
 * IMPORTANTE: limpa localVariables pra evitar "Duplicated LocalVariableTable" crash
 */
public class InjectMixamoJump {

    // === ÂNGULOS POR FASE (em GRAUS) ===
    // Formato: [bodyX, headX, rArmX, rArmY, rArmZ, lArmX, lArmY, lArmZ, rLegX, rLegY, rLegZ, lLegX, lLegY, lLegZ, knee]
    // Knee é aplicado via setLowerAngles

    // ANTICIPATION (motionY > 0.2) - subindo rápido
    static final float[] PROPULSION = {
        -10f,   // body X (levemente inclinado pra trás)
         15f,   // head X (olhando pra cima)
        -90f,   // rArm X (braço pra frente)
        -30f,   // rArm Y (aberto)
          0f,   // rArm Z
        -90f,   // lArm X
         30f,   // lArm Y
          0f,   // lArm Z
         20f,   // rLeg X (levemente levantada)
          0f,   // rLeg Y
        -10f,   // rLeg Z
         20f,   // lLeg X
          0f,   // lLeg Y
         10f,   // lLeg Z
         20f    // knee (pouco dobrado)
    };

    // APEX (motionY entre -0.1 e 0.2) - no topo, suspendido
    static final float[] APEX = {
          5f,   // body X
          0f,   // head X
        -45f,   // rArm X (mais relaxado)
        -45f,   // rArm Y
          0f,   // rArm Z
        -45f,   // lArm X
         45f,   // lArm Y
          0f,   // lArm Z
        -15f,   // rLeg X
          0f,   // rLeg Y
        -10f,   // rLeg Z
         15f,   // lLeg X
          0f,   // lLeg Y
         10f,   // lLeg Z
         25f    // knee
    };

    // FALL (motionY entre -0.4 e -0.1) - caindo
    static final float[] FALL = {
         15f,   // body X
         10f,   // head X
        -45f,   // rArm X
        -30f,   // rArm Y
          0f,   // rArm Z
        -45f,   // lArm X
         30f,   // lArm Y
          0f,   // lArm Z
        -30f,   // rLeg X
          0f,   // rLeg Y
        -10f,   // rLeg Z
         30f,   // lLeg X
          0f,   // lLeg Y
         10f,   // lLeg Z
         40f    // knee
    };

    // LAND (motionY <= -0.4) - pousando
    static final float[] LAND = {
         30f,   // body X (bem inclinado pra frente)
        -15f,   // head X
        -90f,   // rArm X (pra frente)
        -25f,   // rArm Y
          0f,   // rArm Z
        -90f,   // lArm X
         25f,   // lArm Y
          0f,   // lArm Z
        -60f,   // rLeg X
          0f,   // rLeg Y
          0f,   // rLeg Z
        -60f,   // lLeg X
          0f,   // lLeg Y
          0f,   // lLeg Z
         90f    // knee (BEM dobrado, absorção de impacto)
    };

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

    // Aplica uma fase: [body, head, rArm(X,Y,Z), lArm(X,Y,Z), rLeg(X,Y,Z), lLeg(X,Y,Z), knee]
    static InsnList applyPhase(float[] phase) {
        InsnList code = new InsnList();
        String MR = "Lnet/minecraft/client/model/ModelRenderer;";
        String BMP = "net/gobbob/bettermove/BetterMovePlayerModel";

        // 0: bodyX → field_78115_e.rotateAngleX
        code.add(setRot(BMP, "field_78115_e", MR, phase[0], "field_78795_f"));
        // 1: headX → field_78116_c.rotateAngleX
        code.add(setRot(BMP, "field_78116_c", MR, phase[1], "field_78795_f"));
        // 2: rArmX → field_78112_f.rotateAngleX
        code.add(setRot(BMP, "field_78112_f", MR, phase[2], "field_78795_f"));
        // 3: rArmY → field_78112_f.rotateAngleY
        code.add(setRot(BMP, "field_78112_f", MR, phase[3], "field_78796_g"));
        // 4: rArmZ → field_78112_f.rotateAngleZ
        code.add(setRot(BMP, "field_78112_f", MR, phase[4], "field_78808_h"));
        // 5: lArmX → field_78113_g.rotateAngleX
        code.add(setRot(BMP, "field_78113_g", MR, phase[5], "field_78795_f"));
        // 6: lArmY → field_78113_g.rotateAngleY
        code.add(setRot(BMP, "field_78113_g", MR, phase[6], "field_78796_g"));
        // 7: lArmZ → field_78113_g.rotateAngleZ
        code.add(setRot(BMP, "field_78113_g", MR, phase[7], "field_78808_h"));
        // 8: rLegX → field_78123_h.rotateAngleX
        code.add(setRot(BMP, "field_78123_h", MR, phase[8], "field_78795_f"));
        // 9: rLegY → field_78123_h.rotateAngleY
        code.add(setRot(BMP, "field_78123_h", MR, phase[9], "field_78796_g"));
        // 10: rLegZ → field_78123_h.rotateAngleZ
        code.add(setRot(BMP, "field_78123_h", MR, phase[10], "field_78808_h"));
        // 11: lLegX → field_78124_i.rotateAngleX
        code.add(setRot(BMP, "field_78124_i", MR, phase[11], "field_78795_f"));
        // 12: lLegY → field_78124_i.rotateAngleY
        code.add(setRot(BMP, "field_78124_i", MR, phase[12], "field_78796_g"));
        // 13: lLegZ → field_78124_i.rotateAngleZ
        code.add(setRot(BMP, "field_78124_i", MR, phase[13], "field_78808_h"));

        // setLowerAngles(cotoveloR, cotoveloL, joelhoR, joelhoL)
        // Cotovelos = 0 (não temos no JSON do Mine-imator)
        // Joelhos = phase[14]
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new LdcInsnNode(0.0f));      // cotovelo R
        code.add(new LdcInsnNode(0.0f));      // cotovelo L
        code.add(new LdcInsnNode(phase[14])); // joelho R
        code.add(new LdcInsnNode(phase[14])); // joelho L
        code.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
            BMP, "setLowerAngles", "(FFFF)V", false));

        return code;
    }

    // Helper: cria instrucoes "this.bone.field = value"
    static InsnList setRot(String ownerClass, String boneField, String boneDesc,
                           float value, String rotField) {
        InsnList list = new InsnList();
        list.add(new VarInsnNode(Opcodes.ALOAD, 0));
        list.add(new FieldInsnNode(Opcodes.GETFIELD, ownerClass, boneField, boneDesc));
        list.add(new LdcInsnNode(value));
        list.add(new FieldInsnNode(Opcodes.PUTFIELD,
            "net/minecraft/client/model/ModelRenderer", rotField, "F"));
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
            if (mn.name.equals("applyJumpAnimation") && mn.desc.equals("(FFF)V")) {
                System.out.println("  [FOUND] " + mn.name + mn.desc);
                System.out.println("  [BEFORE] " + mn.instructions.size() + " instrucoes");

                // CHAVE: limpa TUDO (instruções, try-catch E local variables)
                mn.instructions.clear();
                mn.tryCatchBlocks.clear();
                mn.localVariables.clear();  // evita "Duplicated LocalVariableTable"

                InsnList code = new InsnList();

                // === LÓGICA DE FASES ===
                // if (motionY > 0.2) → PROPULSION
                // else if (motionY > -0.1) → APEX
                // else if (motionY > -0.4) → FALL
                // else → LAND

                // if (motionY > 0.2f) goto propulsion
                code.add(new VarInsnNode(Opcodes.FLOAD, 1));
                code.add(new LdcInsnNode(0.2f));
                code.add(new InsnNode(Opcodes.FCMPL));
                LabelNode propulsionLabel = new LabelNode();
                code.add(new JumpInsnNode(Opcodes.IFGT, propulsionLabel));

                // === APEX (motionY <= 0.2 && motionY > -0.1) ===
                code.add(new VarInsnNode(Opcodes.FLOAD, 1));
                code.add(new LdcInsnNode(-0.1f));
                code.add(new InsnNode(Opcodes.FCMPL));
                LabelNode fallLabel = new LabelNode();
                code.add(new JumpInsnNode(Opcodes.IFLE, fallLabel));
                code.add(applyPhase(APEX));
                code.add(new InsnNode(Opcodes.RETURN));

                // === FALL (motionY <= -0.1 && motionY > -0.4) ===
                code.add(fallLabel);
                code.add(new VarInsnNode(Opcodes.FLOAD, 1));
                code.add(new LdcInsnNode(-0.4f));
                code.add(new InsnNode(Opcodes.FCMPL));
                LabelNode landLabel = new LabelNode();
                code.add(new JumpInsnNode(Opcodes.IFLE, landLabel));
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

                // Ajusta maxStack pra acomodar os 4 floats do setLowerAngles
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
