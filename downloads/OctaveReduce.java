import java.io.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/**
 * OctaveReduce — REDUZ AS OCTAVAS do noise3DFractal no isCloudAtOpt.
 *
 * Procura o método isCloudAtOpt, dentro dele acha a chamada INVOKEVIRTUAL
 * noise3DFractal, e muda o ICONST_3 (último argumento = octaves) pra ICONST_2.
 *
 * MUDANÇA CIRÚRGICA: só mexe no isCloudAtOpt. Nenhum outro lugar.
 */
public class OctaveReduce {
    static final String CLASS_INTERNAL = "com/voiddim/dimension/ChunkProviderVoidDim";

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Uso: java OctaveReduce <in.jar> <out.jar>");
            System.exit(1);
        }
        Map<String, byte[]> entries = new HashMap<>();
        try (JarFile jf = new JarFile(args[0])) {
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
        if (clsBytes == null) throw new RuntimeException("Classe nao encontrada");

        ClassNode cn = new ClassNode();
        ClassReader cr = new ClassReader(clsBytes);
        cr.accept(cn, 0);

        @SuppressWarnings("unchecked")
        List<MethodNode> methods = cn.methods;
        boolean found = false;
        for (Object obj : methods) {
            MethodNode mn = (MethodNode) obj;
            if (!"isCloudAtOpt".equals(mn.name)) continue;
            if (!"(IIID)Z".equals(mn.desc)) continue;

            System.out.println("[OK] isCloudAtOpt encontrado");

            // procura o ICONST_3 que vem ANTES do INVOKEVIRTUAL noise3DFractal
            // dentro deste método
            InsnList insns = mn.instructions;
            AbstractInsnNode[] arr = insns.toArray();
            for (int i = 0; i < arr.length; i++) {
                AbstractInsnNode insn = arr[i];
                if (insn instanceof MethodInsnNode) {
                    MethodInsnNode m = (MethodInsnNode) insn;
                    if (m.name.equals("noise3DFractal")) {
                        // o argumento 'octaves' (int n2) é o ÚLTIMO antes do INVOKEVIRTUAL
                        // INVOKEVIRTUAL recebe (this, d, d2, d3, n, n2) = 6 valores na pilha
                        // os args são empilhados da direita pra esquerda
                        // então o n2 (octaves, ICONST_3) é o PRIMEIRO a ser empilhado
                        // fica ANTES dos outros args... vou procurar ICONST_3 nos 10 anteriores
                        for (int j = Math.max(0, i - 15); j < i; j++) {
                            if (arr[j] instanceof InsnNode) {
                                InsnNode in = (InsnNode) arr[j];
                                if (in.getOpcode() == Opcodes.ICONST_3) {
                                    // troca ICONST_3 por ICONST_2
                                    // insere ANTES, depois remove o antigo
                                    insns.insertBefore(in, new InsnNode(Opcodes.ICONST_2));
                                    insns.remove(in);
                                    System.out.println("  [OK] octaves 3 -> 2 no noise3DFractal");
                                    found = true;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!found) {
            throw new RuntimeException("Nao encontrei noise3DFractal(...,3) no isCloudAtOpt");
        }

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        entries.put(CLASS_INTERNAL + ".class", cw.toByteArray());

        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(args[1]))) {
            for (Map.Entry<String, byte[]> e : entries.entrySet()) {
                JarEntry je = new JarEntry(e.getKey());
                jos.putNextEntry(je);
                jos.write(e.getValue());
                jos.closeEntry();
            }
        }
        System.out.println("[OK] Jar: " + args[1]);
    }
}
