import java.io.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/**
 * AddCanRain — adiciona o método canRain() retornando false
 * em WorldProviderVoidDim, pra desativar chuva no voiddim.
 */
public class AddCanRain {

    static final String CLASS_INTERNAL = "com/voiddim/dimension/WorldProviderVoidDim";

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Uso: java AddCanRain <in.jar> <out.jar>");
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
        if (clsBytes == null) {
            throw new RuntimeException("Classe nao encontrada");
        }

        ClassNode cn = new ClassNode();
        ClassReader cr = new ClassReader(clsBytes);
        cr.accept(cn, 0);

        // adiciona método canRain() que retorna false
        // método: public boolean canRain() { return false; }
        MethodNode canRain = new MethodNode(
            Opcodes.ACC_PUBLIC,
            "canRain",
            "()Z",
            null,
            new String[0]
        );
        canRain.instructions.add(new InsnNode(Opcodes.ICONST_0));  // false
        canRain.instructions.add(new InsnNode(Opcodes.IRETURN));
        canRain.maxStack = 1;
        canRain.maxLocals = 1;
        cn.methods.add(canRain);

        // também adiciona getRainStrength retornando 0
        MethodNode getRainStrength = new MethodNode(
            Opcodes.ACC_PUBLIC,
            "getRainStrength",
            "(F)F",
            null,
            new String[0]
        );
        getRainStrength.instructions.add(new InsnNode(Opcodes.FCONST_0));  // 0.0f
        getRainStrength.instructions.add(new InsnNode(Opcodes.FRETURN));
        getRainStrength.maxStack = 1;
        getRainStrength.maxLocals = 2;
        cn.methods.add(getRainStrength);

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
        System.out.println("[OK] canRain() e getRainStrength() adicionados");
        System.out.println("[OK] Jar: " + args[1]);
    }
}
