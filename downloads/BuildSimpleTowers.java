import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.io.*;
import java.util.jar.*;

/**
 * v19 - SIMPLES E FUNCIONAL
 * - 4 blocos vanilla customizados (white, red, gray, green wool)
 * - setHardness(-1) = indestrutivel
 * - Texture = wool (nao precisa de textura customizada)
 * - Funciona standalone
 */
public class BuildSimpleTowers {

    public static void main(String[] args) throws Exception {
        String outJar = args.length > 0 ? args[0] : "Atelie-Nebula-COM-Capsule-1.7.10-v19-SIMPLE.jar";
        File f = new File(outJar);
        if (f.exists()) f.delete();

        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(outJar))) {

            // Blockstates MINIMALISTAS
            // Cada bloco é um cubo 16x16x16 (1x1x1) usando wool como textura
            // wool:0 = white, wool:14 = red, wool:7 = gray, wool:5 = green
            String[] names = {"white", "red", "gray", "green"};
            String[] woolMetas = {"0", "14", "7", "5"};
            for (int i = 0; i < 4; i++) {
                addText(jos, "assets/nebulaatelier/blockstates/tower_" + names[i] + ".json",
                    "{\"variants\":{\"normal\":{\"model\":\"minecraft:cube_all\",\"textures\":{\"all\":\"minecraft:blocks/wool_colored_" + names[i] + "\"}}}}");
            }

            // Models MINIMALISTAS (referenciam textures vanilla do MC)
            // NAO PRECISA criar model JSON - o "minecraft:cube_all" ja existe
            // Basta o blockstate apontar pro model vanilla

            // Lang
            addText(jos, "assets/nebulaatelier/lang/en_US.lang",
                "tile.nebulaatelier:sawbench.name=Form Table\n" +
                "tile.nebulaatelier:shape.name=Parametric Piece\n" +
                "item.nebulaatelier:largePulley.name=Precision Pulley\n" +
                "item.nebulaatelier:sawblade.name=Form Blade\n" +
                "item.nebulaatelier:chisel.name=Profile Chisel\n" +
                "item.nebulaatelier:hammer.name=Orientation Compass\n" +
                "item.nebulaatelier:cladding.name=Material Skin\n" +
                "itemGroup.nebulaatelier=Nebula Atelier\n" +
                "tile.nebulaatelier:tower_white.name=Indestructible White\n" +
                "tile.nebulaatelier:tower_red.name=Indestructible Red\n" +
                "tile.nebulaatelier:tower_gray.name=Indestructible Gray\n" +
                "tile.nebulaatelier:tower_green.name=Indestructible Green\n");
            addText(jos, "assets/nebulaatelier/lang/pt_BR.lang",
                "tile.nebulaatelier:sawbench.name=Mesa de Moldes\n" +
                "tile.nebulaatelier:shape.name=Peça Paramétrica\n" +
                "item.nebulaatelier:largePulley.name=Polia de Precisão\n" +
                "item.nebulaatelier:sawblade.name=Lâmina de Molde\n" +
                "item.nebulaatelier:chisel.name=Cinzel de Perfil\n" +
                "item.nebulaatelier:hammer.name=Compasso de Orientação\n" +
                "item.nebulaatelier:cladding.name=Pele de Material\n" +
                "itemGroup.nebulaatelier=Ateliê Nébula\n" +
                "tile.nebulaatelier:tower_white.name=Branco Indestrutível\n" +
                "tile.nebulaatelier:tower_red.name=Vermelho Indestrutível\n" +
                "tile.nebulaatelier:tower_gray.name=Cinza Indestrutível\n" +
                "tile.nebulaatelier:tower_green.name=Verde Indestrutível\n");

            // Mcmod
            addText(jos, "mcmod.info",
                "[\n" +
                "  {\n" +
                "    \"modid\": \"nebulaatelier\",\n" +
                "    \"name\": \"Ateliê Nébula + Tower Blocks\",\n" +
                "    \"description\": \"Ateliê + 4 indestructible colored blocks (white/red/gray/green)\",\n" +
                "    \"version\": \"0.3.2-towers-1.0.0\",\n" +
                "    \"mcversion\": \"1.7.10\",\n" +
                "    \"authorList\": [\"Nebula\", \"Ryan5555fakie\"],\n" +
                "    \"dependencies\": []\n" +
                "  }\n" +
                "]");

            // 4 classes Tower (extends Block direto - simples e funcional)
            for (int i = 0; i < 4; i++) {
                String className = "Tower" + capitalize(names[i]);
                addClass(jos, "com/nebula/atelier/towers/" + className + ".class",
                    generateTowerClass(className, "tower_" + names[i]));
            }
        }
        System.out.println("[DONE] JAR gerado: " + outJar);
    }

    static String capitalize(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
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

    /**
     * Gera classe Tower<Cor> que extends Block DIRETAMENTE
     * (sem BaseBlock, sem BaseMod - mais simples)
     *
     * IMPORTANTE: cada bloco é um cubo 16x16x16 unidades (1x1x1 bloco MC)
     * com textura wool (vanilla do MC).
     */
    static byte[] generateTowerClass(String className, String registryName) {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_7, Opcodes.ACC_PUBLIC, "com/nebula/atelier/towers/" + className,
                null, "net/minecraft/block/Block", null);

        // Construtor
        MethodVisitor init = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);

        // super(Material.rock) - field_151576_e
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

        // setResistance(6000000.0f) - Invencível (retorna Block)
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitLdcInsn(6000000.0f);
        init.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "net/minecraft/block/Block",
                "func_149752_b", "(F)Lnet/minecraft/block/Block;", false);
        init.visitInsn(Opcodes.POP);

        // setUnlocalizedName("tile.nebulaatelier:tower_X")
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitLdcInsn("tile.nebulaatelier:" + registryName);
        init.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "net/minecraft/block/Block",
                "func_149663_c", "(Ljava/lang/String;)Lnet/minecraft/block/Block;", false);
        init.visitInsn(Opcodes.POP);

        // setCreativeTab(CreativeTabs.tabBlock) - field_78030_b
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
