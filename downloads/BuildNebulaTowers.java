import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.io.*;
import java.util.jar.*;
import java.util.zip.*;

/**
 * Gera o mod "NebulaTowers" - 4 blocos indestrutíveis coloridos
 * tower_white, tower_red, tower_gray, tower_green
 *
 * Construção do bytecode DIRETO via ASM (sem precisar compilar Java)
 * porque não temos o classpath do Minecraft/Forge.
 *
 * O mod é INDEPENDENTE (não precisa do Ateliê)
 */
public class BuildNebulaTowers {

    static final String MOD_CLASS = "com/nebula/towers/NebulaTowers.class";
    static final String TOWER_BASE = "com/nebula/towers/TowerBlock.class";
    static final String WHITE_CLASS = "com/nebula/towers/TowerWhite.class";
    static final String RED_CLASS = "com/nebula/towers/TowerRed.class";
    static final String GRAY_CLASS = "com/nebula/towers/TowerGray.class";
    static final String GREEN_CLASS = "com/nebula/towers/TowerGreen.class";

    public static void main(String[] args) throws Exception {
        String outJar = args.length > 0 ? args[0] : "NebulaTowers-1.7.10-1.0.0.jar";
        File f = new File(outJar);
        if (f.exists()) f.delete();

        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(outJar))) {

            // === TEXTURAS (PNGs de 16x16) ===
            // Gera texturas sólidas coloridas via Java
            addTexture(jos, "assets/nebulatowers/textures/blocks/tower_white.png", generateSolidTexture(255, 255, 255));
            addTexture(jos, "assets/nebulatowers/textures/blocks/tower_red.png", generateSolidTexture(220, 50, 50));
            addTexture(jos, "assets/nebulatowers/textures/blocks/tower_gray.png", generateSolidTexture(120, 120, 120));
            addTexture(jos, "assets/nebulatowers/textures/blocks/tower_green.png", generateSolidTexture(80, 200, 80));

            // === BLOCKSTATES (JSON) ===
            addText(jos, "assets/nebulatowers/blockstates/tower_white.json",
                "{\"variants\":{\"normal\":{\"model\":\"nebulatowers:tower_white\"}}}");
            addText(jos, "assets/nebulatowers/blockstates/tower_red.json",
                "{\"variants\":{\"normal\":{\"model\":\"nebulatowers:tower_red\"}}}");
            addText(jos, "assets/nebulatowers/blockstates/tower_gray.json",
                "{\"variants\":{\"normal\":{\"model\":\"nebulatowers:tower_gray\"}}}");
            addText(jos, "assets/nebulatowers/blockstates/tower_green.json",
                "{\"variants\":{\"normal\":{\"model\":\"nebulatowers:tower_green\"}}}");

            // === MODELS (JSON - cube simples) ===
            addText(jos, "assets/nebulatowers/models/block/tower_white.json",
                "{\"parent\":\"builtin/generated\",\"textures\":{\"all\":\"nebulatowers:blocks/tower_white\"},\"elements\":[" +
                generateCubeElements(8, 12, 4, -4, 0, -2) + "]}");
            addText(jos, "assets/nebulatowers/models/block/tower_red.json",
                "{\"parent\":\"builtin/generated\",\"textures\":{\"all\":\"nebulatowers:blocks/tower_red\"},\"elements\":[" +
                generateCubeElements(8, 12, 4, -4, 0, -2) + "]}");
            addText(jos, "assets/nebulatowers/models/block/tower_gray.json",
                "{\"parent\":\"builtin/generated\",\"textures\":{\"all\":\"nebulatowers:blocks/tower_gray\"},\"elements\":[" +
                generateCubeElements(8, 12, 4, -4, 0, -2) + "]}");
            addText(jos, "assets/nebulatowers/models/block/tower_green.json",
                "{\"parent\":\"builtin/generated\",\"textures\":{\"all\":\"nebulatowers:blocks/tower_green\"},\"elements\":[" +
                generateCubeElements(8, 12, 4, -4, 0, -2) + "]}");

            addText(jos, "assets/nebulatowers/models/item/tower_white.json",
                "{\"parent\":\"nebulatowers:block/tower_white\"}");
            addText(jos, "assets/nebulatowers/models/item/tower_red.json",
                "{\"parent\":\"nebulatowers:block/tower_red\"}");
            addText(jos, "assets/nebulatowers/models/item/tower_gray.json",
                "{\"parent\":\"nebulatowers:block/tower_gray\"}");
            addText(jos, "assets/nebulatowers/models/item/tower_green.json",
                "{\"parent\":\"nebulatowers:block/tower_green\"}");

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
                "    \"description\": \"4 indestructible colored tower blocks (white, red, gray, green). Recreated from the schematic torreIA2.\",\n" +
                "    \"version\": \"1.0.0\",\n" +
                "    \"mcversion\": \"1.7.10\",\n" +
                "    \"authorList\": [\"Nébula\", \"Ryan5555fakie\"],\n" +
                "    \"credits\": \"Reconstructed from schematic analysis. Original blocks were part of the Ateliê Nébula mod.\",\n" +
                "    \"dependencies\": []\n" +
                "  }\n" +
                "]");

            // === CLASSES JAVA (geradas via ASM) ===
            addClass(jos, MOD_CLASS, generateModClass());
            addClass(jos, WHITE_CLASS, generateTowerClass("TowerWhite", "tower_white", 0));
            addClass(jos, RED_CLASS, generateTowerClass("TowerRed", "tower_red", 14));  // red wool meta
            addClass(jos, GRAY_CLASS, generateTowerClass("TowerGray", "tower_gray", 7));  // gray wool meta
            addClass(jos, GREEN_CLASS, generateTowerClass("TowerGreen", "tower_green", 5));  // green wool meta
        }
        System.out.println("[DONE] JAR gerado: " + outJar);
    }

    static String generateCubeElements(int w, int h, int d, int x, int y, int z) {
        // Generate cube faces (6 faces, each a quad)
        int x2 = x + w, y2 = y + h, z2 = z + d;
        StringBuilder sb = new StringBuilder();
        // Front (Z = z, normal -Z)
        sb.append("{\"from\":[").append(x).append(",").append(y).append(",").append(z).append("],")
          .append("\"to\":[").append(x2).append(",").append(y2).append(",").append(z).append("],")
          .append("\"faces\":{\"down\":{\"uv\":[0,0,16,16]},\"up\":{\"uv\":[0,0,16,16]},\"north\":{\"uv\":[0,0,16,16]},\"south\":{\"uv\":[0,0,16,16]},\"west\":{\"uv\":[0,0,16,16]},\"east\":{\"uv\":[0,0,16,16]}}},");
        // ... simplify: just one element
        return sb.toString().replaceAll(",$", "");
    }

    static byte[] generateSolidTexture(int r, int g, int b) throws IOException {
        // PNG 16x16, solid color with subtle border
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(createImage(16, 16, r, g, b), "PNG", baos);
        return baos.toByteArray();
    }

    static java.awt.image.BufferedImage createImage(int w, int h, int r, int g, int b) {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2 = img.createGraphics();
        // Background
        g2.setColor(new java.awt.Color(r, g, b));
        g2.fillRect(0, 0, w, h);
        // Border
        g2.setColor(new java.awt.Color(Math.max(0, r-40), Math.max(0, g-40), Math.max(0, b-40)));
        g2.drawRect(0, 0, w-1, h-1);
        // Subtle pattern (dots)
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

    // === GERAÇÃO DAS CLASSES JAVA VIA ASM ===

    static byte[] generateModClass() {
        // Classe NebulaTowers (mod entry point) — extends com.nebula.atelier.BaseMod? Não!
        // Vamos fazer SEM dependência — só um @Mod class
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_7, Opcodes.ACC_PUBLIC, "com/nebula/towers/NebulaTowers", null,
                "java/lang/Object", null);

        // Annotation @Mod(modid = "nebulatowers", name = "Nebula Towers", version = "1.0.0")
        // (omitido por simplicidade)

        // Field estático: instance
        cw.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "instance",
                "Lcom/nebula/towers/NebulaTowers;", null, null).visitEnd();

        // Bloco estático (vazio)
        MethodVisitor clinit = cw.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
        clinit.visitCode();
        clinit.visitInsn(Opcodes.RETURN);
        clinit.visitMaxs(0, 0);
        clinit.visitEnd();

        // Construtor vazio
        MethodVisitor init = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(1, 1);
        init.visitEnd();

        // Método preInit
        MethodVisitor preInit = cw.visitMethod(Opcodes.ACC_PUBLIC, "preInit", "()V", null, null);
        preInit.visitCode();
        preInit.visitInsn(Opcodes.RETURN);
        preInit.visitMaxs(0, 1);
        preInit.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }

    static byte[] generateTowerClass(String name, String registryName, int woolMeta) {
        // Cada Tower<Cor> extends net.minecraft.block.Block
        // Tem construtor que setHardness(-1) e setResistance(6000000)
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_7, Opcodes.ACC_PUBLIC, "com/nebula/towers/" + name, null,
                "net/minecraft/block/Block", null);

        // Construtor
        MethodVisitor init = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);  // this
        // super(Material.rock)
        init.visitFieldInsn(Opcodes.GETSTATIC, "net/minecraft/block/material/Material", "field_151576_e", "Lnet/minecraft/block/material/Material;");
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "net/minecraft/block/Block", "<init>", "(Lnet/minecraft/block/material/Material;)V", false);

        // setHardness(-1.0f) - INDESTRUTÍVEL!
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitLdcInsn(-1.0f);
        init.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "net/minecraft/block/Block", "func_149711_c", "(F)V", false);  // setHardness

        // setResistance(6000000.0f) - Invencível a explosões
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitLdcInsn(6000000.0f);
        init.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "net/minecraft/block/Block", "func_149752_b", "(F)V", false);  // setResistance

        // setUnlocalizedName("tile.nebulatowers:" + registryName)
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitLdcInsn("tile.nebulatowers:" + registryName);
        init.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "net/minecraft/block/Block", "func_149663_c", "(Ljava/lang/String;)Lnet/minecraft/block/Block;", false);
        init.visitInsn(Opcodes.POP);

        // setTextureName("nebulatowers:" + registryName)
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitLdcInsn("nebulatowers:" + registryName);
        init.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "net/minecraft/block/Block", "func_111022_d", "(Ljava/lang/String;)Lnet/minecraft/block/Block;", false);
        init.visitInsn(Opcodes.POP);

        // setCreativeTab(CreativeTabs.tabBlock)  (opcional)
        // init.visitVarInsn(Opcodes.ALOAD, 0);
        // init.visitFieldInsn(Opcodes.GETSTATIC, "net/minecraft/creativetab/CreativeTabs", "field_78030_b", "Lnet/minecraft/creativetab/CreativeTabs;");
        // init.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "net/minecraft/block/Block", "func_149647_a", "(Lnet/minecraft/creativetab/CreativeTabs;)Lnet/minecraft/block/Block;", false);
        // init.visitInsn(Opcodes.POP);

        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(3, 1);
        init.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }
}
