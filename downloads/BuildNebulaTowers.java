import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.io.*;
import java.util.jar.*;

/**
 * Gera o mod "NebulaTowers" v6 (COMPATÍVEL com o schematic torreIA2):
 * - modid = "nebulaatelier" (IGUAL ao original, pra schematic funcionar)
 * - Blocos: tower_white, tower_red, tower_gray, tower_green
 * - setHardness(-1) = indestrutível
 * - Model 16x16x16 (1x1x1 bloco) com texturas 16x16
 */
public class BuildNebulaTowers {

    static final String MODID = "nebulaatelier";
    static final String TEXTURE_PREFIX = "nebulaatelier";  // namespace das texturas

    public static void main(String[] args) throws Exception {
        String outJar = args.length > 0 ? args[0] : "NebulaTowers-1.7.10-1.0.0.jar";
        File f = new File(outJar);
        if (f.exists()) f.delete();

        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(outJar))) {

            // === TEXTURAS ===
            addTexture(jos, "assets/" + TEXTURE_PREFIX + "/textures/blocks/tower_white.png", generateSolidTexture(255, 255, 255));
            addTexture(jos, "assets/" + TEXTURE_PREFIX + "/textures/blocks/tower_red.png", generateSolidTexture(220, 50, 50));
            addTexture(jos, "assets/" + TEXTURE_PREFIX + "/textures/blocks/tower_gray.png", generateSolidTexture(120, 120, 120));
            addTexture(jos, "assets/" + TEXTURE_PREFIX + "/textures/blocks/tower_green.png", generateSolidTexture(80, 200, 80));

            // === BLOCKSTATES ===
            for (String c : new String[]{"white", "red", "gray", "green"}) {
                addText(jos, "assets/" + TEXTURE_PREFIX + "/blockstates/tower_" + c + ".json",
                    "{\"variants\":{\"normal\":{\"model\":\"" + TEXTURE_PREFIX + ":tower_" + c + "\"}}}");
            }

            // === MODELS (cube 16x16x16 unidades = 1x1x1 bloco) ===
            // Em MC 1.7.10, 1 unidade = 1/16 de bloco
            // O schematic torreIA2 tem blocos 8x12x4 pixels MC (0.5x0.75x0.25 bloco)
            // Vou usar 16x16x16 (1x1x1 bloco) que é o padrão e fica visualmente OK
            String cubeElements = "[" +
                "{\"from\":[0,0,0],\"to\":[16,16,16],\"faces\":{\"down\":{\"uv\":[0,0,16,16],\"texture\":\"#all\"},\"up\":{\"uv\":[0,0,16,16],\"texture\":\"#all\"},\"north\":{\"uv\":[0,0,16,16],\"texture\":\"#all\"},\"south\":{\"uv\":[0,0,16,16],\"texture\":\"#all\"},\"west\":{\"uv\":[0,0,16,16],\"texture\":\"#all\"},\"east\":{\"uv\":[0,0,16,16],\"texture\":\"#all\"}}}" +
                "]";
            for (String c : new String[]{"white", "red", "gray", "green"}) {
                addText(jos, "assets/" + TEXTURE_PREFIX + "/models/block/tower_" + c + ".json",
                    "{\"parent\":\"builtin/generated\",\"textures\":{\"all\":\"" + TEXTURE_PREFIX + ":blocks/tower_" + c + "\"},\"elements\":" + cubeElements + "}");
                addText(jos, "assets/" + TEXTURE_PREFIX + "/models/item/tower_" + c + ".json",
                    "{\"parent\":\"" + TEXTURE_PREFIX + ":block/tower_" + c + "\"}");
            }

            // === LANG ===
            addText(jos, "assets/" + TEXTURE_PREFIX + "/lang/en_US.lang",
                "tile." + MODID + ":tower_white.name=Tower White\n" +
                "tile." + MODID + ":tower_red.name=Tower Red\n" +
                "tile." + MODID + ":tower_gray.name=Tower Gray\n" +
                "tile." + MODID + ":tower_green.name=Tower Green\n" +
                "itemGroup." + MODID + "=Nebula Towers");
            addText(jos, "assets/" + MODID + "/lang/pt_BR.lang",
                "tile." + MODID + ":tower_white.name=Torre Branca\n" +
                "tile." + MODID + ":tower_red.name=Torre Vermelha\n" +
                "tile." + MODID + ":tower_gray.name=Torre Cinza\n" +
                "tile." + MODID + ":tower_green.name=Torre Verde\n" +
                "itemGroup." + MODID + "=Torres Nébula");

            // === MCMOD.INFO ===
            addText(jos, "mcmod.info",
                "[\n" +
                "  {\n" +
                "    \"modid\": \"" + MODID + "\",\n" +
                "    \"name\": \"Nebula Towers (Compat Layer)\",\n" +
                "    \"description\": \"Compat layer providing tower_white/red/gray/green blocks. Recreated from schematic torreIA2.\",\n" +
                "    \"version\": \"1.0.0\",\n" +
                "    \"mcversion\": \"1.7.10\",\n" +
                "    \"authorList\": [\"Nebula\", \"Ryan5555fakie\"],\n" +
                "    \"dependencies\": []\n" +
                "  }\n" +
                "]");

            // === CLASSES ===
            addClass(jos, "com/nebula/towers/NebulaTowers.class", generateModClass());
            addClass(jos, "com/nebula/towers/TowerWhite.class", generateTowerClass("TowerWhite", "tower_white"));
            addClass(jos, "com/nebula/towers/TowerRed.class", generateTowerClass("TowerRed", "tower_red"));
            addClass(jos, "com/nebula/towers/TowerGray.class", generateTowerClass("TowerGray", "tower_gray"));
            addClass(jos, "com/nebula/towers/TowerGreen.class", generateTowerClass("TowerGreen", "tower_green"));
        }
        System.out.println("[DONE] JAR gerado: " + outJar);
    }

    static byte[] generateSolidTexture(int r, int g, int b) throws IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(createImage(16, 16, r, g, b), "PNG", baos);
        return baos.toByteArray();
    }

    static java.awt.image.BufferedImage createImage(int w, int h, int r, int g, int b) {
        // IMPORTANTE: usar TYPE_INT_RGB (sem alpha) pra compatibilidade com MC 1.7.10
        // MC 1.7.10 não lida bem com transparência em alguns casos
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g2 = img.createGraphics();
        g2.setColor(new java.awt.Color(r, g, b));
        g2.fillRect(0, 0, w, h);
        g2.setColor(new java.awt.Color(Math.max(0, r-40), Math.max(0, g-40), Math.max(0, b-40)));
        g2.drawRect(0, 0, w-1, h-1);
        g2.setColor(new java.awt.Color(Math.max(0, r-20), Math.max(0, g-20), Math.max(0, b-20)));
        for (int y = 2; y < h; y += 4) {
            for (int x = 2; x < w; x += 4) {
                g2.fillRect(x, y, 1, 1);
            }
        }
        g2.dispose();
        return img;
    }

    static void addTexture(JarOutputStream jos, String path, byte[] data) throws IOException {
        jos.putNextEntry(new JarEntry(path));
        jos.write(data);
        jos.closeEntry();
    }

    static void addText(JarOutputStream jos, String path, String text) throws IOException {
        jos.putNextEntry(new JarEntry(path));
        jos.write(text.getBytes("UTF-8"));
        jos.closeEntry();
    }

    static void addClass(JarOutputStream jos, String path, byte[] data) throws IOException {
        jos.putNextEntry(new JarEntry(path));
        jos.write(data);
        jos.closeEntry();
    }

    // === MOD CLASS ===
    static byte[] generateModClass() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cw.visit(Opcodes.V1_7, Opcodes.ACC_PUBLIC, "com/nebula/towers/NebulaTowers", null,
                "java/lang/Object", null);

        // @Mod(modid = "nebulaatelier", name = "Nebula Towers", version = "1.0.0")
        // IMPORTANTE: usa "nebulaatelier" pra ser COMPATÍVEL com o schematic torreIA2
        AnnotationVisitor modAnn = cw.visitAnnotation("Lcpw/mods/fml/common/Mod;", true);
        modAnn.visit("modid", MODID);
        modAnn.visit("name", "Nebula Towers (Compat Layer)");
        modAnn.visit("version", "1.0.0");
        modAnn.visitEnd();

        // @Mod$Instance("nebulaatelier")
        AnnotationVisitor instAnn = cw.visitAnnotation("Lcpw/mods/fml/common/Mod$Instance;", true);
        instAnn.visit("value", MODID);
        instAnn.visitEnd();

        // public static NebulaTowers nebulatelier (mesmo nome do modid)
        cw.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, MODID,
                "Lcom/nebula/towers/NebulaTowers;", null, null).visitEnd();

        // public static Block towerWhite
        cw.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "towerWhite",
                "Lnet/minecraft/block/Block;", null, null).visitEnd();
        cw.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "towerRed",
                "Lnet/minecraft/block/Block;", null, null).visitEnd();
        cw.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "towerGray",
                "Lnet/minecraft/block/Block;", null, null).visitEnd();
        cw.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "towerGreen",
                "Lnet/minecraft/block/Block;", null, null).visitEnd();

        // <clinit>
        MethodVisitor clinit = cw.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
        clinit.visitCode();
        clinit.visitInsn(Opcodes.RETURN);
        clinit.visitMaxs(0, 0);
        clinit.visitEnd();

        // <init>
        MethodVisitor init = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(1, 1);
        init.visitEnd();

        // preInit
        MethodVisitor preInit = cw.visitMethod(Opcodes.ACC_PUBLIC, "preInit",
                "(Lcpw/mods/fml/common/event/FMLPreInitializationEvent;)V", null, null);
        preInit.visitAnnotation("Lcpw/mods/fml/common/Mod$EventHandler;", true).visitEnd();
        preInit.visitCode();

        // 4 instancias + 4 registers
        for (String color : new String[]{"White", "Red", "Gray", "Green"}) {
            // X = new TowerX()
            preInit.visitTypeInsn(Opcodes.NEW, "com/nebula/towers/Tower" + color);
            preInit.visitInsn(Opcodes.DUP);
            preInit.visitMethodInsn(Opcodes.INVOKESPECIAL, "com/nebula/towers/Tower" + color, "<init>", "()V", false);
            String field = "tower" + color;
            preInit.visitFieldInsn(Opcodes.PUTSTATIC, "com/nebula/towers/NebulaTowers", field, "Lnet/minecraft/block/Block;");

            // GameRegistry.registerBlock(towerX, "tower_x")  - RETORNA Block
            preInit.visitFieldInsn(Opcodes.GETSTATIC, "com/nebula/towers/NebulaTowers", field, "Lnet/minecraft/block/Block;");
            preInit.visitLdcInsn("tower_" + color.toLowerCase());
            preInit.visitMethodInsn(Opcodes.INVOKESTATIC, "cpw/mods/fml/common/registry/GameRegistry", "registerBlock",
                    "(Lnet/minecraft/block/Block;Ljava/lang/String;)Lnet/minecraft/block/Block;", false);
            preInit.visitInsn(Opcodes.POP);
        }

        preInit.visitInsn(Opcodes.RETURN);
        preInit.visitMaxs(3, 2);
        preInit.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }

    // === TOWER CLASS ===
    // IMPORTANTE: package = com.nebula.atelier.towers (não com.nebula.towers)
    // pra ser COMPATÍVEL com o Ateliê Nébula e o schematic
    static byte[] generateTowerClass(String className, String registryName) {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_7, Opcodes.ACC_PUBLIC, "com/nebula/atelier/towers/" + className, null,
                "net/minecraft/block/Block", null);

        MethodVisitor init = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);

        // super(Material.rock)
        init.visitFieldInsn(Opcodes.GETSTATIC, "net/minecraft/block/material/Material",
                "field_151576_e", "Lnet/minecraft/block/material/Material;");
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "net/minecraft/block/Block",
                "<init>", "(Lnet/minecraft/block/material/Material;)V", false);

        // setHardness(-1.0f) - INDESTRUTÍVEL (retorna Block)
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitLdcInsn(-1.0f);
        init.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "net/minecraft/block/Block",
                "func_149711_c", "(F)Lnet/minecraft/block/Block;", false);
        init.visitInsn(Opcodes.POP);

        // setResistance(6000000.0f) - Invencível
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitLdcInsn(6000000.0f);
        init.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "net/minecraft/block/Block",
                "func_149752_b", "(F)Lnet/minecraft/block/Block;", false);
        init.visitInsn(Opcodes.POP);

        // setUnlocalizedName("tile.nebulaatelier:tower_X")
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitLdcInsn("tile." + MODID + ":" + registryName);
        init.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "net/minecraft/block/Block",
                "func_149663_c", "(Ljava/lang/String;)Lnet/minecraft/block/Block;", false);
        init.visitInsn(Opcodes.POP);

        // setCreativeTab(Blocks tab)
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitFieldInsn(Opcodes.GETSTATIC, "net/minecraft/creativetab/CreativeTabs",
                "field_78030_b", "Lnet/minecraft/creativetab/CreativeTabs;");
        init.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "net/minecraft/block/Block",
                "func_149647_a", "(Lnet/minecraft/creativetab/CreativeTabs;)Lnet/minecraft/block/Block;", false);
        init.visitInsn(Opcodes.POP);

        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(3, 1);
        init.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }
}
