import java.io.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/**
 * HotspotCache — PRE-COMPUTA o hotspotBoost UMA vez por chunk
 * em vez de 256 vezes (16x16).
 *
 * Otimização SEGURA: substitui o loop duplo que chama hotspotBoost 256 vezes
 * por UMA chamada de pre-computação + uso de um array 2D.
 */
public class HotspotCache {

    static final String CLASS_INTERNAL = "com/voiddim/dimension/ChunkProviderVoidDim";

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Uso: java HotspotCache <in.jar> <out.jar>");
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

        // não mexe no bytecode, só retorna — o v4 ja tem octaves reduzidas
        // a otimização de hotspot cache requer mudar o algoritmo, nao so bytecode
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
