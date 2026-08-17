import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.io.*;
import java.util.jar.*;

/**
 * v20 - O MAIS SIMPLES POSSÍVEL
 * - SEM textura custom (usa stone do vanilla)
 * - SEM model custom (usa cube_all do vanilla)
 * - Apenas ADICIONA 1 bloco stone indestrutível
 *
 * Se esse NÃO funcionar, é porque o problema tá em outro lugar
 * (provavelmente o v18 ainda tá no mods/).
 */
public class AddSimpleBlock {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Uso: java -cp asm-all-5.0.3.jar:. AddSimpleBlock <in.jar> <out.jar>");
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
                    if (name.endsWith("/")) return;
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

            // Adiciona a classe SimpleBlock
            addClass(jos, "com/nebula/atelier/SimpleBlock.class", generateSimpleBlock());
            // Adiciona o blockstate e a textura (copiada do vanilla)
            // Vou usar uma textura que JÁ EXISTE no MC: minecraft:blocks/stone
            addText(jos, "assets/nebulaatelier/blockstates/simple_block.json",
                "{\"variants\":{\"normal\":{\"model\":\"minecraft:cube_all\",\"textures\":{\"all\":\"minecraft:blocks/stone\"}}}}");
        }
        System.out.println("[DONE] JAR gerado: " + outJar);
    }

    static void addClass(JarOutputStream jos, String path, byte[] data) throws IOException {
        jos.putNextEntry(new JarEntry(path));
        jos.write(data);
        jos.closeEntry();
        System.out.println("  [+] Adicionado: " + path);
    }

    static void addText(JarOutputStream jos, String path, String text) throws IOException {
        jos.putNextEntry(new JarEntry(path));
        jos.write(text.getBytes("UTF-8"));
        jos.closeEntry();
    }

    static byte[] generateSimpleBlock() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_7, Opcodes.ACC_PUBLIC, "com/nebula/atelier/SimpleBlock",
                null, "net/minecraft/block/Block", null);

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

        // setResistance(6000000.0f)
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitLdcInsn(6000000.0f);
        init.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "net/minecraft/block/Block",
                "func_149752_b", "(F)Lnet/minecraft/block/Block;", false);
        init.visitInsn(Opcodes.POP);

        // setUnlocalizedName("tile.nebulaatelier:simple_block")
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitLdcInsn("tile.nebulaatelier:simple_block");
        init.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "net/minecraft/block/Block",
                "func_149663_c", "(Ljava/lang/String;)Lnet/minecraft/block/Block;", false);
        init.visitInsn(Opcodes.POP);

        // setCreativeTab
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

    static byte[] patchNebulaAtelier(InputStream is) throws Exception {
        ClassReader cr = new ClassReader(is);
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);

        @SuppressWarnings("unchecked")
        java.util.List<MethodNode> methodList = cn.methods;
        for (MethodNode mn : methodList) {
            if (mn.name.equals("preInit") && mn.desc.equals("(Lcpw/mods/fml/common/event/FMLPreInitializationEvent;)V")) {
                System.out.println("  [+] Patching preInit()");

                addFieldIfMissing(cn, "simpleBlock", "Lnet/minecraft/block/Block;");

                InsnList inject = new InsnList();

                // NebulaAtelier.simpleBlock = new SimpleBlock()
                inject.add(new TypeInsnNode(Opcodes.NEW, "com/nebula/atelier/SimpleBlock"));
                inject.add(new InsnNode(Opcodes.DUP));
                inject.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "com/nebula/atelier/SimpleBlock", "<init>", "()V", false));
                inject.add(new FieldInsnNode(Opcodes.PUTSTATIC,
                    "com/nebula/atelier/NebulaAtelier", "simpleBlock", "Lnet/minecraft/block/Block;"));

                // GameRegistry.registerBlock(NebulaAtelier.simpleBlock, "simple_block")
                inject.add(new FieldInsnNode(Opcodes.GETSTATIC,
                    "com/nebula/atelier/NebulaAtelier", "simpleBlock", "Lnet/minecraft/block/Block;"));
                inject.add(new LdcInsnNode("simple_block"));
                inject.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                    "cpw/mods/fml/common/registry/GameRegistry", "registerBlock",
                    "(Lnet/minecraft/block/Block;Ljava/lang/String;)Lnet/minecraft/block/Block;", false));
                inject.add(new InsnNode(Opcodes.POP));

                AbstractInsnNode ret = mn.instructions.getLast();
                while (ret != null && ret.getOpcode() != Opcodes.RETURN) {
                    ret = ret.getPrevious();
                }
                if (ret != null) {
                    mn.instructions.insertBefore(ret, inject);
                } else {
                    mn.instructions.add(inject);
                }
                mn.maxStack = 4;
            }
        }

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cn.accept(cw);
        return cw.toByteArray();
    }

    @SuppressWarnings("unchecked")
    static void addFieldIfMissing(ClassNode cn, String name, String desc) {
        java.util.List<FieldNode> fields = cn.fields;
        for (FieldNode f : fields) {
            if (f.name.equals(name) && f.desc.equals(desc)) return;
        }
        FieldNode fn = new FieldNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
            name, desc, null, null);
        fields.add(fn);
    }
}
