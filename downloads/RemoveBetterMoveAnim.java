import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.io.*;
import java.util.jar.*;

/**
 * Patcher: Remove animacoes do BetterMove Player Model
 * - Mantem a chamada super.func_78087_a() (animacoes vanilla do MC)
 * - Insere RETURN logo apos, impedindo todas as animacoes custom
 * - NAO mexe no setup do constructor (cabelo/skinnedrenderers intactos)
 *
 * USO: java -cp "asm-all-5.0.3.jar" RemoveBetterMoveAnim <in.jar> <out.jar>
 */
public class RemoveBetterMoveAnim {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Uso: java -cp asm-all-5.0.3.jar:. RemoveBetterMoveAnim <in.jar> <out.jar>");
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
                    if (name.endsWith("/")) return; // skip directories
                    if (name.equals("net/gobbob/bettermove/BetterMovePlayerModel.class")) {
                        System.out.println("[PATCH] Patcheando: " + name);
                        InputStream is = jar.getInputStream(entry);
                        byte[] patched = patchClass(is);
                        is.close();
                        JarEntry newEntry = new JarEntry(name);
                        jos.putNextEntry(newEntry);
                        jos.write(patched);
                        jos.closeEntry();
                        System.out.println("[OK] Patch aplicado em " + name);
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

        boolean patched = false;
        @SuppressWarnings("unchecked")
        java.util.List<MethodNode> methodList = cn.methods;
        for (MethodNode mn : methodList) {
            // func_78087_a = setRotationAngles do ModelBiped
            if (mn.name.equals("func_78087_a") && mn.desc.equals("(FFFFFFLnet/minecraft/entity/Entity;)V")) {
                System.out.println("  [FOUND] metodo: " + mn.name + mn.desc);
                System.out.println("  [BEFORE] " + mn.instructions.size() + " instrucoes");

                InsnList ins = mn.instructions;
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

                AbstractInsnNode afterSuper = superCall.getNext();
                if (afterSuper == null) throw new RuntimeException("Sem instrucao apos super!");

                System.out.println("  [SUPER] achado, proxima instrucao opcode=" + afterSuper.getOpcode());

                // Insere RETURN antes de afterSuper
                InsnList toInsert = new InsnList();
                toInsert.add(new InsnNode(Opcodes.RETURN));
                ins.insertBefore(afterSuper, toInsert);

                // Remove TUDO depois do RETURN (a partir de afterSuper)
                AbstractInsnNode cur = afterSuper;
                while (cur != null) {
                    AbstractInsnNode next = cur.getNext();
                    ins.remove(cur);
                    cur = next;
                }

                System.out.println("  [AFTER] " + mn.instructions.size() + " instrucoes");
                patched = true;
            }
        }

        if (!patched) throw new RuntimeException("Metodo func_78087_a nao encontrado!");

        // COMPUTE_MAXS: recalcula stack/locals
        // COMPUTE_FRAMES: recalcula frames (necessario se mudamos fluxo de controle)
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cn.accept(cw);
        return cw.toByteArray();
    }
}
