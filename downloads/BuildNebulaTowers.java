import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.io.*;
import java.util.jar.*;

/**
 * Gera o mod "NebulaTowers" CORRETO:
 * - @Mod annotation
 * - preInit registra os 4 blocos no GameRegistry
 * - Os 4 blocos extends Block, setHardness(-1), setResistance(6000000)
 * - Blocos aparecem no creative tab
 */
public class BuildNebulaTowers {

    public static void main(String[] args) throws Exception {
        String outJar = args.length > 0 ? args[0] : "NebulaTowers-1.7.10-1.0.0.jar";
        File f = new File(outJar);
        if (f.exists()) f.delete();

        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(outJar))) {

            // === TEXTURAS ===
            addTexture(jos, "assets/nebulatowers/textures/blocks/tower_white.png", generateSolidTexture(255, 255, 255));
            addTexture(jos, "assets/nebulatowers/textures/blocks/tower_red.png", generateSolidTexture(220, 50, 50));
            addTexture(jos, "assets/nebulatowers/textures/blocks/tower_gray.png", generateSolidTexture(120, 120, 120));
            addTexture(jos, "assets/nebulatowers/textures/blocks/tower_green.png", generateSolidTexture(80, 200, 80));

            // === BLOCKSTATES ===
            addText(jos, "assets/nebulatowers/blockstates/tower_white.json",
                "{\"variants\":{\"normal\":{\"model\":\"nebulatowers:tower_white\"}}}");
            addText(jos, "assets/nebulatowers/blockstates/tower_red.json",
                "{\"variants\":{\"normal\":{\"model\":\"nebulatowers:tower_red\"}}}");
            addText(jos, "assets/nebulatowers/blockstates/tower_gray.json",
                "{\"variants\":{\"normal\":{\"model\":\"nebulatowers:tower_gray\"}}}");
            addText(jos, "assets/nebulatowers/blockstates/tower_green.json",
                "{\"variants\":{\"normal\":{\"model\":\"nebulatowers:tower_green\"}}}");

            // === MODELS (cube 8x12x4 - tamanho padrão de torso) ===
            String cubeElements = "[" +
                "{\"from\":[0,0,0],\"to\":[8,12,4],\"faces\":{\"down\":{\"uv\":[0,0,16,16],\"texture\":\"#all\"},\"up\":{\"uv\":[0,0,16,16],\"texture\":\"#all\"},\"north\":{\"uv\":[0,0,16,16],\"texture\":\"#all\"},\"south\":{\"uv\":[0,0,16,16],\"texture\":\"#all\"},\"west\":{\"uv\":[0,0,16,16],\"texture\":\"#all\"},\"east\":{\"uv\":[0,0,16,16],\"texture\":\"#all\"}}}" +
                "]";
            for (String c : new String[]{"white", "red", "gray", "green"}) {
                addText(jos, "assets/nebulatowers/models/block/tower_" + c + ".json",
                    "{\"parent\":\"builtin/generated\",\"textures\":{\"all\":\"nebulatowers:blocks/tower_" + c + "\"},\"elements\":" + cubeElements + "}");
                addText(jos, "assets/nebulatowers/models/item/tower_" + c + ".json",
                    "{\"parent\":\"nebulatowers:block/tower_" + c + "\"}");
            }

            // === LANG ===
            addText(jos, "assets/nebulatowers/lang/en_US.lang",
                "tile.nebulatowers:tower_white.name=Tower White\n" +
                "tile.nebulatowers:tower_red.name=Tower Red\n" +
                "tile.nebulatowers:tower_gray.name=Tower Gray\n" +
                "tile.nebulatowers:tower_green.name=Tower Green\n" +
                "itemGroup.nebulatowers=Nebula Towers");
            addText(jos, "assets/nebulatowers/lang/pt_BR.lang",
                "tile.nebulatowers:tower_white.name=Torre Branca\n" +
                "tile.nebulatowers:tower_red.name=Torre Vermelha\n" +
                "tile.nebulatowers:tower_gray.name=Torre Cinza\n" +
                "tile.nebulatowers:tower_green.name=Torre Verde\n" +
                "itemGroup.nebulatowers=Torres Nébula");

            // === MCMOD.INFO ===
            addText(jos, "mcmod.info",
                "[\n" +
                "  {\n" +
                "    \"modid\": \"nebulatowers\",\n" +
                "    \"name\": \"Nebula Towers\",\n" +
                "    \"description\": \"4 indestructible colored tower blocks. Recreated from schematic torreIA2.\",\n" +
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
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
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

    // === MOD CLASS com @Mod + preInit que registra blocos ===
    static byte[] generateModClass() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cw.visit(Opcodes.V1_7, Opcodes.ACC_PUBLIC, "com/nebula/towers/NebulaTowers", null,
                "java/lang/Object", null);

        // Annotation @Mod(modid = "nebulatowers", name = "Nebula Towers", version = "1.0.0")
        AnnotationVisitor modAnn = cw.visitAnnotation("Lcpw/mods/fml/common/Mod;", true);
        modAnn.visit("modid", "nebulatowers");
        modAnn.visit("name", "Nebula Towers");
        modAnn.visit("version", "1.0.0");
        modAnn.visitEnd();

        // Annotation @Mod$Instance("nebulatowers")
        AnnotationVisitor instAnn = cw.visitAnnotation("Lcpw/mods/fml/common/Mod$Instance;", true);
        instAnn.visit("value", "nebulatowers");
        instAnn.visitEnd();

        // public static NebulaTowers instance
        // IMPORTANTE: O nome do campo TEM QUE ser igual ao modid!
        // Porque o @Mod.Instance("nebulatowers") procura por um campo chamado "nebulatowers"
        cw.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "nebulatowers",
                "Lcom/nebula/towers/NebulaTowers;", null, null).visitEnd();

        // public static Block towerWhite
        cw.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "towerWhite",
                "Lnet/minecraft/block/Block;", null, null).visitEnd();
        // public static Block towerRed
        cw.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "towerRed",
                "Lnet/minecraft/block/Block;", null, null).visitEnd();
        // public static Block towerGray
        cw.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "towerGray",
                "Lnet/minecraft/block/Block;", null, null).visitEnd();
        // public static Block towerGreen
        cw.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "towerGreen",
                "Lnet/minecraft/block/Block;", null, null).visitEnd();

        // <clinit> - vazio
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

        // ============ preInit(@Mod.EventHandler) ============
        // Esse método REGISTRA os blocos no GameRegistry
        MethodVisitor preInit = cw.visitMethod(Opcodes.ACC_PUBLIC, "preInit",
                "(Lcpw/mods/fml/common/event/FMLPreInitializationEvent;)V", null, null);
        preInit.visitAnnotation("Lcpw/mods/fml/common/Mod$EventHandler;", true).visitEnd();
        preInit.visitCode();

        // 1. towerWhite = new TowerWhite()
        preInit.visitTypeInsn(Opcodes.NEW, "com/nebula/towers/TowerWhite");
        preInit.visitInsn(Opcodes.DUP);
        preInit.visitMethodInsn(Opcodes.INVOKESPECIAL, "com/nebula/towers/TowerWhite", "<init>", "()V", false);
        preInit.visitFieldInsn(Opcodes.PUTSTATIC, "com/nebula/towers/NebulaTowers", "towerWhite", "Lnet/minecraft/block/Block;");

        // 2. towerRed = new TowerRed()
        preInit.visitTypeInsn(Opcodes.NEW, "com/nebula/towers/TowerRed");
        preInit.visitInsn(Opcodes.DUP);
        preInit.visitMethodInsn(Opcodes.INVOKESPECIAL, "com/nebula/towers/TowerRed", "<init>", "()V", false);
        preInit.visitFieldInsn(Opcodes.PUTSTATIC, "com/nebula/towers/NebulaTowers", "towerRed", "Lnet/minecraft/block/Block;");

        // 3. towerGray = new TowerGray()
        preInit.visitTypeInsn(Opcodes.NEW, "com/nebula/towers/TowerGray");
        preInit.visitInsn(Opcodes.DUP);
        preInit.visitMethodInsn(Opcodes.INVOKESPECIAL, "com/nebula/towers/TowerGray", "<init>", "()V", false);
        preInit.visitFieldInsn(Opcodes.PUTSTATIC, "com/nebula/towers/NebulaTowers", "towerGray", "Lnet/minecraft/block/Block;");

        // 4. towerGreen = new TowerGreen()
        preInit.visitTypeInsn(Opcodes.NEW, "com/nebula/towers/TowerGreen");
        preInit.visitInsn(Opcodes.DUP);
        preInit.visitMethodInsn(Opcodes.INVOKESPECIAL, "com/nebula/towers/TowerGreen", "<init>", "()V", false);
        preInit.visitFieldInsn(Opcodes.PUTSTATIC, "com/nebula/towers/NebulaTowers", "towerGreen", "Lnet/minecraft/block/Block;");

        // 5. GameRegistry.registerBlock(towerWhite, "tower_white")
        preInit.visitFieldInsn(Opcodes.GETSTATIC, "com/nebula/towers/NebulaTowers", "towerWhite", "Lnet/minecraft/block/Block;");
        preInit.visitLdcInsn("tower_white");
        preInit.visitMethodInsn(Opcodes.INVOKESTATIC, "cpw/mods/fml/common/registry/GameRegistry", "registerBlock",
                "(Lnet/minecraft/block/Block;Ljava/lang/String;)Lnet/minecraft/block/Block;", false);

        // 6. GameRegistry.registerBlock(towerRed, "tower_red")
        preInit.visitFieldInsn(Opcodes.GETSTATIC, "com/nebula/towers/NebulaTowers", "towerRed", "Lnet/minecraft/block/Block;");
        preInit.visitLdcInsn("tower_red");
        preInit.visitMethodInsn(Opcodes.INVOKESTATIC, "cpw/mods/fml/common/registry/GameRegistry", "registerBlock",
                "(Lnet/minecraft/block/Block;Ljava/lang/String;)Lnet/minecraft/block/Block;", false);

        // 7. GameRegistry.registerBlock(towerGray, "tower_gray")
        preInit.visitFieldInsn(Opcodes.GETSTATIC, "com/nebula/towers/NebulaTowers", "towerGray", "Lnet/minecraft/block/Block;");
        preInit.visitLdcInsn("tower_gray");
        preInit.visitMethodInsn(Opcodes.INVOKESTATIC, "cpw/mods/fml/common/registry/GameRegistry", "registerBlock",
                "(Lnet/minecraft/block/Block;Ljava/lang/String;)Lnet/minecraft/block/Block;", false);

        // 8. GameRegistry.registerBlock(towerGreen, "tower_green")
        preInit.visitFieldInsn(Opcodes.GETSTATIC, "com/nebula/towers/NebulaTowers", "towerGreen", "Lnet/minecraft/block/Block;");
        preInit.visitLdcInsn("tower_green");
        preInit.visitMethodInsn(Opcodes.INVOKESTATIC, "cpw/mods/fml/common/registry/GameRegistry", "registerBlock",
                "(Lnet/minecraft/block/Block;Ljava/lang/String;)Lnet/minecraft/block/Block;", false);

        preInit.visitInsn(Opcodes.RETURN);
        preInit.visitMaxs(3, 2);
        preInit.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }

    // === CLASSE Tower<Cor> extends Block ===
    static byte[] generateTowerClass(String className, String registryName) {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_7, Opcodes.ACC_PUBLIC, "com/nebula/towers/" + className, null,
                "net/minecraft/block/Block", null);

        // Construtor
        MethodVisitor init = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);

        // super(Material.rock = field_151576_e)
        init.visitFieldInsn(Opcodes.GETSTATIC, "net/minecraft/block/material/Material",
                "field_151576_e", "Lnet/minecraft/block/material/Material;");
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "net/minecraft/block/Block",
                "<init>", "(Lnet/minecraft/block/material/Material;)V", false);

        // setHardness(-1.0f) - INDESTRUTÍVEL
        // IMPORTANTE: func_149711_c RETORNA Block (não void!)
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitLdcInsn(-1.0f);
        init.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "net/minecraft/block/Block",
                "func_149711_c", "(F)Lnet/minecraft/block/Block;", false);
        init.visitInsn(Opcodes.POP);

        // setResistance(6000000.0f) - também RETORNA Block
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitLdcInsn(6000000.0f);
        init.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "net/minecraft/block/Block",
                "func_149752_b", "(F)Lnet/minecraft/block/Block;", false);
        init.visitInsn(Opcodes.POP);

        // setUnlocalizedName("tile.nebulatowers:" + registryName)
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitLdcInsn("tile.nebulatowers:" + registryName);
        init.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "net/minecraft/block/Block",
                "func_149663_c", "(Ljava/lang/String;)Lnet/minecraft/block/Block;", false);
        init.visitInsn(Opcodes.POP);

        // NOTA: setTextureName (func_111022_d) NÃO EXISTE em MC 1.7.10 vanilla.
        // A textura é resolvida pelo blockstate JSON (model + texture name)
        // Não precisa chamar aqui.

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
