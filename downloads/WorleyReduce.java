import java.io.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/**
 * WorleyReduce — Reduz WORLEY_OCTAVES de 2 para 1 no ChunkProviderVoidDim.
 *
 * OTIMIZAÇÃO SEGURA:
 *   O loop do worley no isCloudAtOpt usa `iconst_2` como limite superior.
 *   Trocando por `iconst_1`, o worley roda apenas 1 vez (em vez de 2),
 *   cortando 50% do custo de erosão worley.
 *
 *   iconst_1 e iconst_2 ambos ocupam EXATAMENTE 1 byte no bytecode,
 *   então a substituição é bit-perfect em termos de offset e frames.
 *
 * IDENTIFICAÇÃO:
 *   Método: isCloudAtOpt(IIID)Z
 *   Pattern: loop for (int i = 0; i < WORLEY_OCTAVES; ++i)
 *   Distinguir de outros loops: o loop worley chama worleyNoise3D, e
 *   tem `iconst_0` (init), `istore` em variável 34 (i), `iconst_2`
 *   como limite, `if_icmpge` para sair.
 *
 *   No bytecode da v4:
 *     offset 206: iconst_0
 *     offset 207: istore 34
 *     offset 209: iload 34
 *     offset 211: iconst_2      <-- ESTE AQUI
 *     offset 212: if_icmpge 283
 *     ... corpo do loop ...
 *     offset 277: iinc 34, 1
 *     offset 280: goto 209
 */
public class WorleyReduce {

    static final String CLASS_INTERNAL = "com/voiddim/dimension/ChunkProviderVoidDim";

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Uso: java WorleyReduce <in.jar> <out.jar>");
            System.exit(1);
        }

        String inPath = args[0];
        String outPath = args[1];

        Map<String, byte[]> entries = new HashMap<>();
        try (JarFile jf = new JarFile(inPath)) {
            Enumeration<JarEntry> en = jf.entries();
            while (en.hasMoreElements()) {
                JarEntry e = en.nextElement();
                if (e.isDirectory()) continue;
                try (InputStream is = jf.getInputStream(e)) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = is.read(buf)) > 0) baos.write(buf, 0, n);
                    entries.put(e.getName(), baos.toByteArray());
                }
            }
        }

        byte[] clsBytes = entries.get(CLASS_INTERNAL + ".class");
        if (clsBytes == null) {
            throw new RuntimeException("Classe " + CLASS_INTERNAL + " nao encontrada em " + inPath);
        }

        ClassNode cn = new ClassNode();
        ClassReader cr = new ClassReader(clsBytes);
        cr.accept(cn, 0);

        int patches = 0;

        // Percorre todos os métodos da classe
        for (Object obj : cn.methods) {
            MethodNode mn = (MethodNode) obj;
            if (!"isCloudAtOpt".equals(mn.name)) continue;
            if (!"(IIID)Z".equals(mn.desc)) continue;

            System.out.println("[*] Encontrou metodo: " + mn.name + mn.desc);

            InsnList insns = mn.instructions;
            int size = insns.size();

            // Procura o pattern do loop worley:
            //   iconst_0, istore N, ..., iload N, iconst_2, if_icmpge LABEL, ..., iinc N 1, goto LOOP
            // O truque: achar o 'iconst_2' que vem DEPOIS de um 'iload N'
            // e é seguido por 'if_icmpge', e mais à frente tem 'iinc N 1' e 'goto' pra trás.

            for (int i = 0; i < size; i++) {
                AbstractInsnNode ain = insns.get(i);
                if (ain.getOpcode() != Opcodes.ICONST_2) continue;

                // Verifica contexto: anterior deve ser iload de uma variável
                AbstractInsnNode prev = insns.get(i - 1);
                if (prev == null) continue;
                if (prev.getOpcode() != Opcodes.ILOAD) continue;

                // Próximo deve ser if_icmpge
                AbstractInsnNode next = insns.get(i + 1);
                if (next == null) continue;
                if (next.getOpcode() != Opcodes.IF_ICMPGE) continue;

                // Olha mais à frente: deve ter iinc dessa mesma variável + 1
                VarInsnNode vPrev = (VarInsnNode) prev;
                int loopVar = vPrev.var;

                boolean foundIinc = false;
                for (int j = i + 2; j < Math.min(i + 200, size); j++) {
                    AbstractInsnNode scan = insns.get(j);
                    if (scan.getOpcode() == Opcodes.IINC) {
                        IincInsnNode iinc = (IincInsnNode) scan;
                        if (iinc.var == loopVar && iinc.incr == 1) {
                            foundIinc = true;
                            break;
                        }
                    }
                }

                if (!foundIinc) continue;

                // CONFIRMADO: é o loop do worley (ou outro loop i<2)
                // Para ter certeza, verifica se o corpo do loop chama worleyNoise3D
                boolean callsWorley = false;
                for (int j = i + 2; j < Math.min(i + 100, size); j++) {
                    AbstractInsnNode scan = insns.get(j);
                    if (scan.getOpcode() == Opcodes.INVOKESPECIAL) {
                        MethodInsnNode min = (MethodInsnNode) scan;
                        if ("worleyNoise3D".equals(min.name) && "(DDD)D".equals(min.desc)) {
                            callsWorley = true;
                            break;
                        }
                    }
                }

                if (!callsWorley) {
                    System.out.println("[!] Loop encontrado mas nao chama worleyNoise3D, pulando (offset " + i + ")");
                    continue;
                }

                // SUBSTITUIR iconst_2 por iconst_1
                InsnNode newIconst = new InsnNode(Opcodes.ICONST_1);
                insns.insertBefore(ain, newIconst);
                insns.remove(ain);
                patches++;
                System.out.println("[OK] Worley loop: iconst_2 -> iconst_1 (apos iload " + loopVar + ")");

                // Como mudamos o tamanho do array de instruções, recalcula
                size = insns.size();
            }
        }

        if (patches == 0) {
            throw new RuntimeException("NENHUM patch aplicado — algo mudou no bytecode? Abortando por seguranca.");
        }

        // Regrava a classe
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        entries.put(CLASS_INTERNAL + ".class", cw.toByteArray());

        // Reescreve o JAR
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(outPath))) {
            for (Map.Entry<String, byte[]> e : entries.entrySet()) {
                JarEntry je = new JarEntry(e.getKey());
                jos.putNextEntry(je);
                jos.write(e.getValue());
                jos.closeEntry();
            }
        }

        System.out.println("[OK] " + patches + " patch(es) aplicado(s). Jar: " + outPath);
    }
}
