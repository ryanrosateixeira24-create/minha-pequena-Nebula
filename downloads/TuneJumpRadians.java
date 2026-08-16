import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.io.*;
import java.util.jar.*;

/**
 * Patcher: TUNEA as constantes de pulo do BetterMove com valores Mixamo.
 *
 * ESTRATÉGIA:
 * - NÃO reescreve nenhum método
 * - Sobrescreve SÓ as constantes estáticas (JUMP_*) no ConstantPool
 * - Mantém progress, suavização, lean, bobbing do mod
 * - Converte graus → radianos automaticamente
 */
public class TuneJumpRadians {

    // === ÂNGULOS MIXAMO (em GRAUS, convertidos pra radianos automaticamente) ===
    // Cada array tem 16 samples (0=chão, 0.5=apex, 1=landing)

    // Body lean
    static final float[] MIXAMO_LEAN = toRad(new float[]{
        -15f, -18f, -20f, -22f,
        -18f, -12f,  -8f,  -4f,
          4f,   8f,  12f,  16f,
         14f,  10f,   6f,   2f
    });

    // Braços X (sobe no start, abre no apex, desce no landing)
    static final float[] MIXAMO_R_ARM = toRad(new float[]{
        -60f, -90f, -110f, -130f,
        -110f, -90f, -60f, -30f,
         20f,  40f,  60f,  40f,
         20f,   0f, -20f, -40f
    });
    static final float[] MIXAMO_L_ARM = toRad(new float[]{
        -60f, -90f, -110f, -130f,
        -110f, -90f, -60f, -30f,
         20f,  40f,  60f,  40f,
         20f,   0f, -20f, -40f
    });

    // Cotovelos (mais dobrado quando braço esticado)
    static final float[] MIXAMO_R_ELBOW = toRad(new float[]{
        -60f, -70f, -80f, -90f,
        -80f, -70f, -60f, -50f,
        -50f, -60f, -80f, -100f,
        -100f, -90f, -70f, -50f
    });
    static final float[] MIXAMO_L_ELBOW = toRad(new float[]{
        -60f, -70f, -80f, -90f,
        -80f, -70f, -60f, -50f,
        -50f, -60f, -80f, -100f,
        -100f, -90f, -70f, -50f
    });

    // Pernas X (sobe start, desce apex, vai pra trás landing)
    static final float[] MIXAMO_R_LEG = toRad(new float[]{
        -45f, -30f, -15f,   0f,
          0f,  15f,  30f,  20f,
         10f,   0f, -10f, -20f,
        -30f, -45f, -60f, -75f
    });
    static final float[] MIXAMO_L_LEG = toRad(new float[]{
        -45f, -30f, -15f,   0f,
          0f,  15f,  30f,  20f,
         10f,   0f, -10f, -20f,
        -30f, -45f, -60f, -75f
    });

    // Joelhos (agacha start, estica apex, dobra landing)
    static final float[] MIXAMO_R_KNEE = toRad(new float[]{
         80f,  70f,  50f,  30f,
         20f,  25f,  35f,  30f,
         40f,  60f,  80f,  90f,
         95f, 100f,  90f,  70f
    });
    static final float[] MIXAMO_L_KNEE = toRad(new float[]{
         80f,  70f,  50f,  30f,
         20f,  25f,  35f,  30f,
         40f,  60f,  80f,  90f,
         95f, 100f,  90f,  70f
    });

    static float[] toRad(float[] degrees) {
        float[] rads = new float[degrees.length];
        for (int i = 0; i < degrees.length; i++) {
            rads[i] = (float) Math.toRadians(degrees[i]);
        }
        return rads;
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Uso: java -cp asm-all-5.0.3.jar:. TuneJumpRadians <in.jar> <out.jar>");
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
                        System.out.println("[TUNE] Patcheando: " + name);
                        InputStream is = jar.getInputStream(entry);
                        byte[] patched = patchClass(is);
                        is.close();
                        JarEntry newEntry = new JarEntry(name);
                        jos.putNextEntry(newEntry);
                        jos.write(patched);
                        jos.closeEntry();
                        System.out.println("[OK] Constantes Mixamo injetadas!");
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

    static byte[] patchClass(InputStream is) throws Exception {
        ClassReader cr = new ClassReader(is);
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);

        int patchCount = 0;
        @SuppressWarnings("unchecked")
        java.util.List<MethodNode> methodList = cn.methods;

        for (MethodNode mn : methodList) {
            if (!mn.name.equals("<clinit>")) continue;
            System.out.println("  [FOUND] <clinit>");

            InsnList ins = mn.instructions;
            AbstractInsnNode cursor = ins.getFirst();

            while (cursor != null) {
                // Detecta: sipush/bipush 16 seguido de newarray float
                boolean isPush16 = false;
                if (cursor instanceof IntInsnNode) {
                    IntInsnNode iin = (IntInsnNode) cursor;
                    int op = iin.getOpcode();
                    if (iin.operand == 16 && (op == Opcodes.SIPUSH || op == Opcodes.BIPUSH)) {
                        isPush16 = true;
                    }
                }

                if (isPush16) {
                    AbstractInsnNode next = cursor.getNext();
                    if (next instanceof IntInsnNode && next.getOpcode() == Opcodes.NEWARRAY) {
                        IntInsnNode newarr = (IntInsnNode) next;
                        if (newarr.operand == Opcodes.T_FLOAT) {
                            // Acha o PUTSTATIC correspondente
                            AbstractInsnNode put = newarr.getNext();
                            int safety = 0;
                            while (put != null && safety < 100) {
                                if (put instanceof FieldInsnNode &&
                                    put.getOpcode() == Opcodes.PUTSTATIC) {
                                    FieldInsnNode field = (FieldInsnNode) put;
                                    String fieldName = field.name;
                                    if (fieldName.startsWith("JUMP_")) {
                                        System.out.println("  [ARRAY] " + fieldName);
                                        float[] newValues = getMixamoArray(fieldName);
                                        if (newValues != null) {
                                            // Substitui os LDC float (de trás pra frente)
                                            AbstractInsnNode arrEnd = put.getPrevious();
                                            int idx = newValues.length - 1;
                                            AbstractInsnNode cur = arrEnd;
                                            int replaced = 0;
                                            while (cur != newarr && idx >= 0) {
                                                if (cur instanceof LdcInsnNode) {
                                                    LdcInsnNode oldLdc = (LdcInsnNode) cur;
                                                    oldLdc.cst = newValues[idx--];
                                                    replaced++;
                                                }
                                                cur = cur.getPrevious();
                                            }
                                            patchCount++;
                                            System.out.println("    -> patched " + replaced + " values");
                                        }
                                    }
                                    break;
                                }
                                put = put.getNext();
                                safety++;
                            }
                        }
                    }
                }
                cursor = cursor.getNext();
            }
        }

        System.out.println("  [PATCHED] " + patchCount + " arrays");
        if (patchCount == 0) throw new RuntimeException("Nenhum array JUMP_* encontrado!");

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cn.accept(cw);
        return cw.toByteArray();
    }

    static float[] getMixamoArray(String name) {
        switch (name) {
            case "JUMP_LEAN":    return MIXAMO_LEAN;
            case "JUMP_R_ARM":   return MIXAMO_R_ARM;
            case "JUMP_L_ARM":   return MIXAMO_L_ARM;
            case "JUMP_R_ELBOW": return MIXAMO_R_ELBOW;
            case "JUMP_L_ELBOW": return MIXAMO_L_ELBOW;
            case "JUMP_R_LEG":   return MIXAMO_R_LEG;
            case "JUMP_L_LEG":   return MIXAMO_L_LEG;
            case "JUMP_R_KNEE":  return MIXAMO_R_KNEE;
            case "JUMP_L_KNEE":  return MIXAMO_L_KNEE;
            default: return null;
        }
    }
}
