import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.io.*;
import java.util.jar.*;

/**
 * Patch: Adiciona 4 blocos tower (white, red, gray, green) ao mod Atelie-Nebula-COM-Capsule.
 * - modid continua "nebulaatelier" (sem conflito!)
 * - 4 classes Tower* adicionadas (extends Block, indestrutíveis)
 * - 4 texturas, 4 blockstates, 4 models
 * - Patches o preInit da NebulaAtelier pra registrar os 4 blocos
 */
public class PatchAtelieTowers {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Uso: java -cp asm-all-5.0.3.jar:. PatchAtelieTowers <in.jar> <out.jar>");
            System.exit(1);
        }
        String inJar = args[0];
        String outJar = args[1];

        File outFile = new File(outJar);
        if (outFile.exists()) outFile.delete();

        // PRÉ-CARREGA as langs pra sobrescrever depois
        String origEnUS = readLangFile(inJar, "assets/nebulaatelier/lang/en_US.lang");
        String origPtBR = readLangFile(inJar, "assets/nebulaatelier/lang/pt_BR.lang");

        try (JarFile jar = new JarFile(inJar);
             JarOutputStream jos = new JarOutputStream(new FileOutputStream(outJar))) {

            jar.stream().forEach(entry -> {
                String name = entry.getName();
                try {
                    if (name.endsWith("/")) return;

                    // Pula as langs (vamos adicionar manualmente depois)
                    if (name.equals("assets/nebulaatelier/lang/en_US.lang") ||
                        name.equals("assets/nebulaatelier/lang/pt_BR.lang")) {
                        return;
                    }

                    if (name.equals("com/nebula/atelier/NebulaAtelier.class")) {
                        System.out.println("[PATCH] Modificando: " + name);
                        InputStream is = jar.getInputStream(entry);
                        byte[] patched = patchNebulaAtelier(is);
                        is.close();
                        jos.putNextEntry(new JarEntry(name));
                        jos.write(patched);
                        jos.closeEntry();
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

            // === ADICIONA AS 4 CLASSES TOWER ===
            addClass(jos, "com/nebula/atelier/towers/TowerWhite.class",
                BuildNebulaTowers.generateTowerClass("TowerWhite", "tower_white"));
            addClass(jos, "com/nebula/atelier/towers/TowerRed.class",
                BuildNebulaTowers.generateTowerClass("TowerRed", "tower_red"));
            addClass(jos, "com/nebula/atelier/towers/TowerGray.class",
                BuildNebulaTowers.generateTowerClass("TowerGray", "tower_gray"));
            addClass(jos, "com/nebula/atelier/towers/TowerGreen.class",
                BuildNebulaTowers.generateTowerClass("TowerGreen", "tower_green"));

            // === ADICIONA AS TEXTURAS ===
            addTexture(jos, "assets/nebulaatelier/textures/blocks/tower_white.png",
                BuildNebulaTowers.generateSolidTexture(255, 255, 255));
            addTexture(jos, "assets/nebulaatelier/textures/blocks/tower_red.png",
                BuildNebulaTowers.generateSolidTexture(220, 50, 50));
            addTexture(jos, "assets/nebulaatelier/textures/blocks/tower_gray.png",
                BuildNebulaTowers.generateSolidTexture(120, 120, 120));
            addTexture(jos, "assets/nebulaatelier/textures/blocks/tower_green.png",
                BuildNebulaTowers.generateSolidTexture(80, 200, 80));

            // === ADICIONA OS BLOCKSTATES ===
            for (String c : new String[]{"white", "red", "gray", "green"}) {
                addText(jos, "assets/nebulaatelier/blockstates/tower_" + c + ".json",
                    "{\"variants\":{\"normal\":{\"model\":\"nebulaatelier:tower_" + c + "\"}}}");
            }

            // === ADICIONA OS MODELS ===
            String cubeElements = "[" +
                "{\"from\":[0,0,0],\"to\":[16,16,16],\"faces\":{\"down\":{\"uv\":[0,0,16,16],\"texture\":\"#all\"},\"up\":{\"uv\":[0,0,16,16],\"texture\":\"#all\"},\"north\":{\"uv\":[0,0,16,16],\"texture\":\"#all\"},\"south\":{\"uv\":[0,0,16,16],\"texture\":\"#all\"},\"west\":{\"uv\":[0,0,16,16],\"texture\":\"#all\"},\"east\":{\"uv\":[0,0,16,16],\"texture\":\"#all\"}}}" +
                "]";
            for (String c : new String[]{"white", "red", "gray", "green"}) {
                addText(jos, "assets/nebulaatelier/models/block/tower_" + c + ".json",
                    "{\"parent\":\"builtin/generated\",\"textures\":{\"all\":\"nebulaatelier:blocks/tower_" + c + "\"},\"elements\":" + cubeElements + "}");
                addText(jos, "assets/nebulaatelier/models/item/tower_" + c + ".json",
                    "{\"parent\":\"nebulaatelier:block/tower_" + c + "\"}");
            }

            // === ADICIONA LANG (en/pt-BR) - usando a lang ORIGINAL do JAR ===
            addText(jos, "assets/nebulaatelier/lang/en_US.lang",
                origEnUS +
                "tile.nebulaatelier:tower_white.name=Tower White\n" +
                "tile.nebulaatelier:tower_red.name=Tower Red\n" +
                "tile.nebulaatelier:tower_gray.name=Tower Gray\n" +
                "tile.nebulaatelier:tower_green.name=Tower Green\n");
            addText(jos, "assets/nebulaatelier/lang/pt_BR.lang",
                origPtBR +
                "tile.nebulaatelier:tower_white.name=Torre Branca\n" +
                "tile.nebulaatelier:tower_red.name=Torre Vermelha\n" +
                "tile.nebulaatelier:tower_gray.name=Torre Cinza\n" +
                "tile.nebulaatelier:tower_green.name=Torre Verde\n");
        }
        System.out.println("[DONE] JAR gerado: " + outJar);
    }

    static String readLangEntry(JarFile jar, String path) {
        return readLangFile(jar.getName(), path);
    }

    static String readLangFile(String jarPath, String path) {
        try (JarFile jar = new JarFile(jarPath)) {
            InputStream is = jar.getInputStream(jar.getEntry(path));
            byte[] buf = new byte[8192];
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            int n;
            while ((n = is.read(buf)) > 0) baos.write(buf, 0, n);
            is.close();
            return baos.toString("UTF-8");
        } catch (Exception e) {
            return "";
        }
    }

    static void addClass(JarOutputStream jos, String path, byte[] data) throws IOException {
        jos.putNextEntry(new JarEntry(path));
        jos.write(data);
        jos.closeEntry();
        System.out.println("  [+] Adicionado: " + path);
    }

    static void addTexture(JarOutputStream jos, String path, byte[] data) throws IOException {
        jos.putNextEntry(new JarEntry(path));
        jos.write(data);
        jos.closeEntry();
        System.out.println("  [+] Adicionado: " + path);
    }

    static void addText(JarOutputStream jos, String path, String text) throws IOException {
        jos.putNextEntry(new JarEntry(path));
        jos.write(text.getBytes("UTF-8"));
        jos.closeEntry();
        System.out.println("  [+] Adicionado: " + path);
    }

    /**
     * Patches o preInit da NebulaAtelier pra ADICIONAR 4 chamadas:
     * 1. new TowerWhite() + setHardness(-1) + setResistance(6000000) + setUnlocalizedName
     * 2. GameRegistry.registerBlock(towerWhite, "tower_white")
     * ... etc pra Red, Gray, Green
     */
    static byte[] patchNebulaAtelier(InputStream is) throws Exception {
        ClassReader cr = new ClassReader(is);
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);

        @SuppressWarnings("unchecked")
        java.util.List<MethodNode> methodList = cn.methods;
        for (MethodNode mn : methodList) {
            if (mn.name.equals("preInit") && mn.desc.equals("(Lcpw/mods/fml/common/event/FMLPreInitializationEvent;)V")) {
                System.out.println("  [+] Patching preInit()");

                // Adiciona 4 fields estáticos (towerWhite, towerRed, etc)
                addFieldIfMissing(cn, "towerWhite", "Lnet/minecraft/block/Block;");
                addFieldIfMissing(cn, "towerRed", "Lnet/minecraft/block/Block;");
                addFieldIfMissing(cn, "towerGray", "Lnet/minecraft/block/Block;");
                addFieldIfMissing(cn, "towerGreen", "Lnet/minecraft/block/Block;");

                // Constrói o código a ser inserido
                InsnList inject = new InsnList();
                String[] colors = {"White", "Red", "Gray", "Green"};
                String[] names = {"white", "red", "gray", "green"};
                for (int i = 0; i < 4; i++) {
                    String className = "com/nebula/atelier/towers/Tower" + colors[i];
                    String field = "tower" + colors[i];
                    String registry = "tower_" + names[i];

                    // NebulaAtelier.towerWhite = new TowerWhite()  (STATIC, sem aload_0!)
                    inject.add(new TypeInsnNode(Opcodes.NEW, className));
                    inject.add(new InsnNode(Opcodes.DUP));
                    inject.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, className, "<init>", "()V", false));
                    inject.add(new FieldInsnNode(Opcodes.PUTSTATIC,
                        "com/nebula/atelier/NebulaAtelier", field, "Lnet/minecraft/block/Block;"));

                    // GameRegistry.registerBlock(NebulaAtelier.towerWhite, "tower_white")
                    inject.add(new FieldInsnNode(Opcodes.GETSTATIC,
                        "com/nebula/atelier/NebulaAtelier", field, "Lnet/minecraft/block/Block;"));
                    inject.add(new LdcInsnNode(registry));
                    inject.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                        "cpw/mods/fml/common/registry/GameRegistry", "registerBlock",
                        "(Lnet/minecraft/block/Block;Ljava/lang/String;)Lnet/minecraft/block/Block;", false));
                    inject.add(new InsnNode(Opcodes.POP));
                }

                // Insere no FINAL do método (antes do RETURN)
                AbstractInsnNode ret = mn.instructions.getLast();
                while (ret.getOpcode() != Opcodes.RETURN) {
                    ret = ret.getPrevious();
                    if (ret == null) break;
                }
                if (ret != null) {
                    mn.instructions.insertBefore(ret, inject);
                } else {
                    mn.instructions.add(inject);
                }

                // Atualiza maxStack
                mn.maxStack = 4;
            }
        }

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cn.accept(cw);
        return cw.toByteArray();
    }

    static void addFieldIfMissing(ClassNode cn, String name, String desc) {
        @SuppressWarnings("unchecked")
        java.util.List<FieldNode> fields = cn.fields;
        for (FieldNode f : fields) {
            if (f.name.equals(name) && f.desc.equals(desc)) return;
        }
        FieldNode fn = new FieldNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
            name, desc, null, null);
        fields.add(fn);
    }
}
