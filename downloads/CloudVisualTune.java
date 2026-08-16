import java.io.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/**
 * CloudVisualTune — Ajusta constantes visuais do SurfaceNetsCloud
 * para nuvens mais bonitas e sem bugs visuais.
 *
 * MUDANÇAS (todas em ConstantPool, risco quase zero):
 *
 *   JITTER_AMP:        0.08f -> 0.0f   (remove jitter que buga ao quebrar bloco)
 *   AO_STRENGTH:       0.25f -> 0.40f  (mais ambient occlusion, compensa a perda de textura)
 *   GLITTER_CHANCE:    0.015f -> 0.025f  (mais brilho)
 *   GLITTER_INTENSITY: 0.35f -> 0.55f  (brilho mais forte)
 *   BLOOM_STRENGTH:    0.15f -> 0.35f  (bloom mais intenso, nuvens "resplandecem")
 *   FADE_MAX:          0.3f  -> 0.5f   (fade-out mais visível, melhor profundidade)
 *
 * Por que essas mudanças:
 *
 * 1. JITTER=0 remove o "tremido" que faz nuvens bugarem quando blocos são quebrados.
 *    O jitter é baseado em hash da posição, então ao recalcular (bloco removido)
 *    o hash dá um valor diferente e o vértice "pula" pra outra posição.
 *
 * 2. AO_STRENGTH maior compensa a "perda" de detalhe que vinha do jitter.
 *    Sem jitter as nuvens parecem um pouco mais "chapadas" — AO mais forte
 *    dá a impressão de profundidade nas dobras.
 *
 * 3. GLITTER/BLOOM mais fortes dão o visual "mágico" que o Other World tem.
 *    Olhando a paleta do canon (HIGH=quase branco quente, MID=amarelo mostarda,
 *    LOW=caramelo), as nuvens têm um "glow" natural — mais bloom captura isso.
 *
 * 4. FADE_MAX maior faz nuvens distantes sumirem mais gradualmente,
 *    melhorando a percepção de profundidade no mar de nuvens.
 */
public class CloudVisualTune {

    static final String CLASS_INTERNAL = "com/voiddim/client/SurfaceNetsCloud";

    // Mudanças a aplicar
    static class Change {
        String fieldName;
        String fieldDesc;
        Object newValue;
        Change(String f, String d, Object v) { fieldName=f; fieldDesc=d; newValue=v; }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Uso: java CloudVisualTune <in.jar> <out.jar>");
            System.exit(1);
        }

        String inPath = args[0];
        String outPath = args[1];

        List<Change> changes = Arrays.asList(
            new Change("JITTER_AMP", "F", 0.0f),
            new Change("AO_STRENGTH", "F", 0.40f),
            new Change("GLITTER_CHANCE", "F", 0.025f),
            new Change("GLITTER_INTENSITY", "F", 0.55f),
            new Change("BLOOM_STRENGTH", "F", 0.35f),
            new Change("FADE_MAX", "F", 0.5f)
        );

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

        for (Object objF : cn.fields) {
            FieldNode fn = (FieldNode) objF;
            for (Change c : changes) {
                if (!c.fieldName.equals(fn.name)) continue;
                if (!c.fieldDesc.equals(fn.desc)) continue;

                Object oldValue = fn.value;
                fn.value = c.newValue;
                System.out.println("[OK] " + fn.name + ": " + oldValue + " -> " + c.newValue);
                patches++;
                break;
            }
        }

        if (patches != changes.size()) {
            System.out.println("[!] Esperado " + changes.size() + " patches, aplicado " + patches);
            // Lista os que faltaram
            Set<String> done = new HashSet<>();
            for (Object objF2 : cn.fields) {
                FieldNode fn = (FieldNode) objF2;
                for (Change c : changes) {
                    if (c.fieldName.equals(fn.name) && c.fieldDesc.equals(fn.desc)) {
                        done.add(c.fieldName);
                    }
                }
            }
            for (Change c : changes) {
                if (!done.contains(c.fieldName)) {
                    System.out.println("[!] FALTOU: " + c.fieldName);
                }
            }
        }

        if (patches == 0) {
            throw new RuntimeException("Nenhum patch aplicado, abortando por seguranca");
        }

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        entries.put(CLASS_INTERNAL + ".class", cw.toByteArray());

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
