import java.io.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/**
 * RockSpawner — Adiciona pedras/montanhas flutuantes no voiddim
 *
 * ESTRATÉGIA CIRÚRGICA (risco médio-baixo):
 *   1. NÃO mexe no generateOrganicClouds existente (não toca em nada do que já funciona)
 *   2. ADICIONA um novo método generateRocks(Chunk, chunkX, chunkZ) na classe
 *   3. INJETA uma chamada a esse método no FINAL do generateOrganicClouds
 *
 * O método novo:
 *   - Usa mixHash (que JÁ EXISTE na classe, só chamamos) pra decidir SE tem pedras
 *   - Se sim, sorteia 1-3 pedras pequenas (seixos de 2-4 cubos)
 *   - Usa Blocks.stone (vanilla) — não precisa de bloco novo
 *   - Posiciona em Y entre 75-82 (em cima das nuvens)
 *
 * IMPORTANTE:
 *   - Blocks.stone é accessed via getstatic (campo estático do vanilla)
 *   - O método precisa retornar void
 *   - Args: Chunk, int chunkX, int chunkZ
 *
 * Algoritmo (escrito em pseudo-Java, depois compilado pra bytecode):
 *
 *   private void generateRocks(Chunk chunk, int cx, int cz) {
 *       long seed = worldObj.func_72905_C();  // getSeed
 *       long h = mixHash(cx, cz, 55555, seed);
 *       int chance = (int)((h >>> 16) & 0xFF);  // 0-255
 *       if (chance > 80) return;  // 30% dos chunks têm pedras
 *
 *       long h2 = mixHash(cx, cz, 66666, seed);
 *       int count = 1 + (int)((h2 >>> 8) & 3);  // 1-4 pedras
 *
 *       for (int i = 0; i < count; i++) {
 *           long h3 = mixHash(cx, cz, 77777 + i*31, seed);
 *           int dx = 2 + (int)((h3 & 0xF));       // 2..17
 *           int dz = 2 + (int)((h3 >>> 4) & 0xF);
 *           int y  = 76 + (int)((h3 >>> 8) & 7);  // 76..82
 *           int size = 2 + (int)((h3 >>> 12) & 1); // 2..3 (raio)
 *
 *           // gera seixo: cubo central + alguns adjacentes
 *           for (int sx = -size; sx <= size; sx++) {
 *               for (int sz = -size; sz <= size; sz++) {
 *                   for (int sy = 0; sy <= size-1; sy++) {
 *                       int dist = sx*sx + sy*sy + sz*sz;
 *                       if (dist > size*size) continue;
 *                       chunk.func_150807_a(dx+sx, y+sy, dz+sz, Blocks.stone, 0);
 *                   }
 *               }
 *           }
 *       }
 *   }
 *
 * IMPORTANTE: O loop usa 55555, 66666, 77777+ como "salt" pra ter 3 hashes diferentes
 * mixHash é static, então invocamos ele direto (não precisa de this)
 */
public class RockSpawner {

    static final String CLASS_INTERNAL = "com/voiddim/dimension/ChunkProviderVoidDim";

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Uso: java RockSpawner <in.jar> <out.jar>");
            System.exit(1);
        }

        String inPath = args[0];
        String outPath = args[1];

        // Carrega JAR
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

        // === PASSO 1: Adiciona método generateRocks(Chunk, int, int)V ===
        MethodNode mn = new MethodNode(
            Opcodes.ACC_PRIVATE,                      // private
            "generateRocks",                          // nome
            "(Lnet/minecraft/world/chunk/Chunk;II)V", // desc: (Chunk, int, int) -> void
            null,                                     // signature genérica
            null                                      // exceções
        );

        // Vou montar as instruções na mão, opcode por opcode
        // pra ter controle total. Baseado no pseudo-código acima.

        InsnList il = mn.instructions;

        // ============ BODY START ============
        // Variáveis locais:
        // 0: this (ChunkProviderVoidDim)
        // 1: chunk (Chunk)
        // 2: cx (int)
        // 3: cz (int)
        // 4: seed (long) — 2 slots
        // 6: h (long) — 2 slots
        // 7: chance (int)
        // 8: h2 (long) — 2 slots
        // 10: count (int)
        // 11: i (int)
        // 12: h3 (long) — 2 slots
        // 14: dx (int)
        // 15: dz (int)
        // 16: y (int)
        // 17: size (int)
        // 18: sx, sz, sy, dist — temporários no loop
        // Vamos precisar de mais slots pra variáveis do loop interno

        // --- Pega seed ---
        // aload_0; getfield worldObj; invokevirtual func_72905_C()J; lstore 4
        il.add(new VarInsnNode(Opcodes.ALOAD, 0)); // aload_0
        il.add(new FieldInsnNode(Opcodes.GETFIELD,
            "com/voiddim/dimension/ChunkProviderVoidDim",
            "worldObj", "Lnet/minecraft/world/World;"));
        il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
            "net/minecraft/world/World", "func_72905_C", "()J"));
        il.add(new VarInsnNode(Opcodes.LSTORE, 4)); // lstore 4 — long usa 2 slots

        // Vou refazer tudo mais cuidadosamente. Limpa e começa de novo.

        // === RESET: limpa as instruções que adicionei errado ===
        il.clear();

        // Recomeça com sintaxe correta
        // Pra long stores: LSTORE 4 (opcode 63 = 0x3F)
        // Pra long loads: LLOAD 4 (opcode 30 = 0x1E)
        // Pra int stores: ISTORE N (opcode 54 = 0x36)
        // Pra int loads: ILOAD N (opcode 21 = 0x15)

        // Vou usar constantes dos opcodes direto
        final int ALOAD = Opcodes.ALOAD;
        final int ASTORE = Opcodes.ASTORE;
        final int ILOAD = Opcodes.ILOAD;
        final int ISTORE = Opcodes.ISTORE;
        final int LLOAD = Opcodes.LLOAD;
        final int LSTORE = Opcodes.LSTORE;
        final int ICONST_0 = Opcodes.ICONST_0;
        final int ICONST_1 = Opcodes.ICONST_1;
        final int ICONST_2 = Opcodes.ICONST_2;
        final int ICONST_3 = Opcodes.ICONST_3;
        final int ICONST_4 = Opcodes.ICONST_4;
        final int ICONST_5 = Opcodes.ICONST_5;
        final int ICONST_M1 = Opcodes.ICONST_M1;
        final int BIPUSH = Opcodes.BIPUSH;
        final int SIPUSH = Opcodes.SIPUSH;
        final int GETFIELD = Opcodes.GETFIELD;
        final int GETSTATIC = Opcodes.GETSTATIC;
        final int INVOKEVIRTUAL = Opcodes.INVOKEVIRTUAL;
        final int INVOKESTATIC = Opcodes.INVOKESTATIC;
        final int LDC = Opcodes.LDC;
        final int L2I = Opcodes.L2I;
        final int I2I = Opcodes.NOP; // dummy
        final int IAND = Opcodes.IAND;
        final int LAND = Opcodes.LAND;
        final int LUSHR = Opcodes.LUSHR;
        final int IUSHR = Opcodes.IUSHR;
        final int IFEQ = Opcodes.IFEQ;
        final int IFNE = Opcodes.IFNE;
        final int IFGE = Opcodes.IFGE;
        final int IFGT = Opcodes.IFGT;
        final int IFLE = Opcodes.IFLE;
        final int IFLT = Opcodes.IFLT;
        final int IF_ICMPEQ = Opcodes.IF_ICMPEQ;
        final int IF_ICMPGE = Opcodes.IF_ICMPGE;
        final int IF_ICMPGT = Opcodes.IF_ICMPGT;
        final int IF_ICMPLE = Opcodes.IF_ICMPLE;
        final int IF_ICMPLT = Opcodes.IF_ICMPLT;
        final int IADD = Opcodes.IADD;
        final int ISUB = Opcodes.ISUB;
        final int IMUL = Opcodes.IMUL;
        final int IINC = Opcodes.IINC;
        final int GOTO = Opcodes.GOTO;
        final int RETURN = Opcodes.RETURN;
        final int DUP = Opcodes.DUP;
        final int POP = Opcodes.POP;
        final int POP2 = Opcodes.POP2;
        final int SWAP = Opcodes.SWAP;
        final int NEWARRAY = Opcodes.NEWARRAY;
        final int CHECKCAST = Opcodes.CHECKCAST;

        // ==== CORPO: ====
        // Pra COMPUTE_FRAMES funcionar, precisa de LABEL no início
        LabelNode lblStart = new LabelNode();
        il.add(lblStart);

        // long seed = worldObj.func_72905_C()
        il.add(new VarInsnNode(ALOAD, 0));
        il.add(new FieldInsnNode(GETFIELD,
            "com/voiddim/dimension/ChunkProviderVoidDim",
            "worldObj", "Lnet/minecraft/world/World;"));
        il.add(new MethodInsnNode(INVOKEVIRTUAL,
            "net/minecraft/world/World", "func_72905_C", "()J"));
        il.add(new VarInsnNode(LSTORE, 4)); // lstore 4

        // long h = mixHash(cx, cz, 55555, seed)
        il.add(new VarInsnNode(ILOAD, 2));
        il.add(new VarInsnNode(ILOAD, 3));
        il.add(new LdcInsnNode(55555));
        il.add(new VarInsnNode(LLOAD, 4));
        il.add(new MethodInsnNode(INVOKESTATIC,
            "com/voiddim/dimension/ChunkProviderVoidDim",
            "mixHash", "(IIIJ)J"));
        il.add(new VarInsnNode(LSTORE, 6));

        // int chance = (int)((h >>> 16) & 0xFF)
        il.add(new VarInsnNode(LLOAD, 6));
        il.add(new LdcInsnNode(16L));
        il.add(new InsnNode(LUSHR));
        il.add(new LdcInsnNode(255L));
        il.add(new InsnNode(LAND));
        il.add(new InsnNode(L2I));
        il.add(new VarInsnNode(ISTORE, 7));

        // if (chance > 80) return;
        il.add(new VarInsnNode(ILOAD, 7));
        il.add(new LdcInsnNode(80));
        LabelNode lblReturn1 = new LabelNode();
        // if chance > 80, volta pro return
        // IF_ICMPGT jumps if value1 > value2
        // We want: if (chance > 80) goto return
        // if_icmpgt takes value1, value2 from stack — so push chance then 80
        il.add(new JumpInsnNode(IF_ICMPGT, lblReturn1));

        // long h2 = mixHash(cx, cz, 66666, seed)
        il.add(new VarInsnNode(ILOAD, 2));
        il.add(new VarInsnNode(ILOAD, 3));
        il.add(new LdcInsnNode(66666));
        il.add(new VarInsnNode(LLOAD, 4));
        il.add(new MethodInsnNode(INVOKESTATIC,
            "com/voiddim/dimension/ChunkProviderVoidDim",
            "mixHash", "(IIIJ)J"));
        il.add(new VarInsnNode(LSTORE, 8));

        // int count = 1 + (int)((h2 >>> 8) & 3)
        il.add(new VarInsnNode(LLOAD, 8));
        il.add(new LdcInsnNode(8L));
        il.add(new InsnNode(LUSHR));
        il.add(new LdcInsnNode(3L));
        il.add(new InsnNode(LAND));
        il.add(new InsnNode(L2I));
        il.add(new InsnNode(ICONST_1));
        il.add(new InsnNode(IADD));
        il.add(new VarInsnNode(ISTORE, 10));

        // for (int i = 0; i < count; i++)
        il.add(new InsnNode(ICONST_0));
        il.add(new VarInsnNode(ISTORE, 11));
        LabelNode lblLoopStart = new LabelNode();
        LabelNode lblLoopEnd = new LabelNode();
        il.add(lblLoopStart);
        il.add(new VarInsnNode(ILOAD, 11));
        il.add(new VarInsnNode(ILOAD, 10));
        il.add(new JumpInsnNode(IF_ICMPGE, lblLoopEnd));

        // long h3 = mixHash(cx, cz, 77777 + i*31, seed)
        il.add(new VarInsnNode(ILOAD, 2));
        il.add(new VarInsnNode(ILOAD, 3));
        il.add(new LdcInsnNode(77777));
        il.add(new VarInsnNode(ILOAD, 11));
        il.add(new LdcInsnNode(31));
        il.add(new InsnNode(IMUL));
        il.add(new InsnNode(IADD));
        il.add(new VarInsnNode(LLOAD, 4));
        il.add(new MethodInsnNode(INVOKESTATIC,
            "com/voiddim/dimension/ChunkProviderVoidDim",
            "mixHash", "(IIIJ)J"));
        il.add(new VarInsnNode(LSTORE, 12));

        // int dx = 2 + (int)(h3 & 0xF)
        il.add(new VarInsnNode(LLOAD, 12));
        il.add(new LdcInsnNode(15L));
        il.add(new InsnNode(LAND));
        il.add(new InsnNode(L2I));
        il.add(new InsnNode(ICONST_2));
        il.add(new InsnNode(IADD));
        il.add(new VarInsnNode(ISTORE, 14));

        // int dz = 2 + (int)((h3 >>> 4) & 0xF)
        il.add(new VarInsnNode(LLOAD, 12));
        il.add(new LdcInsnNode(4L));
        il.add(new InsnNode(LUSHR));
        il.add(new LdcInsnNode(15L));
        il.add(new InsnNode(LAND));
        il.add(new InsnNode(L2I));
        il.add(new InsnNode(ICONST_2));
        il.add(new InsnNode(IADD));
        il.add(new VarInsnNode(ISTORE, 15));

        // int y = 76 + (int)((h3 >>> 8) & 7)
        il.add(new VarInsnNode(LLOAD, 12));
        il.add(new LdcInsnNode(8L));
        il.add(new InsnNode(LUSHR));
        il.add(new LdcInsnNode(7L));
        il.add(new InsnNode(LAND));
        il.add(new InsnNode(L2I));
        il.add(new LdcInsnNode(76));
        il.add(new InsnNode(IADD));
        il.add(new VarInsnNode(ISTORE, 16));

        // int size = 2 + (int)((h3 >>> 12) & 1)
        il.add(new VarInsnNode(LLOAD, 12));
        il.add(new LdcInsnNode(12L));
        il.add(new InsnNode(LUSHR));
        il.add(new LdcInsnNode(1L));
        il.add(new InsnNode(LAND));
        il.add(new InsnNode(L2I));
        il.add(new InsnNode(ICONST_2));
        il.add(new InsnNode(IADD));
        il.add(new VarInsnNode(ISTORE, 17));

        // === LOOP: gera seixo esférico ===
        // for (int sx = -size; sx <= size; sx++)
        // NOTA: size é local var 17, sx será 18
        il.add(new VarInsnNode(ILOAD, 17));
        il.add(new InsnNode(Opcodes.INEG));
        il.add(new VarInsnNode(ISTORE, 18));
        LabelNode lblSxStart = new LabelNode();
        LabelNode lblSxEnd = new LabelNode();
        il.add(lblSxStart);
        il.add(new VarInsnNode(ILOAD, 18));
        il.add(new VarInsnNode(ILOAD, 17));
        il.add(new JumpInsnNode(IF_ICMPGT, lblSxEnd));

        // for (int sz = -size; sz <= size; sz++)
        il.add(new VarInsnNode(ILOAD, 17));
        il.add(new InsnNode(Opcodes.INEG));
        il.add(new VarInsnNode(ISTORE, 19));
        LabelNode lblSzStart = new LabelNode();
        LabelNode lblSzEnd = new LabelNode();
        il.add(lblSzStart);
        il.add(new VarInsnNode(ILOAD, 19));
        il.add(new VarInsnNode(ILOAD, 17));
        il.add(new JumpInsnNode(IF_ICMPGT, lblSzEnd));

        // for (int sy = 0; sy <= size-1; sy++)
        il.add(new InsnNode(ICONST_0));
        il.add(new VarInsnNode(ISTORE, 20));
        LabelNode lblSyStart = new LabelNode();
        LabelNode lblSyEnd = new LabelNode();
        il.add(lblSyStart);
        il.add(new VarInsnNode(ILOAD, 20));
        il.add(new VarInsnNode(ILOAD, 17));
        il.add(new InsnNode(ICONST_1));
        il.add(new InsnNode(ISUB));
        il.add(new JumpInsnNode(IF_ICMPGT, lblSyEnd));

        // int dist = sx*sx + sy*sy + sz*sz
        // sx²
        il.add(new VarInsnNode(ILOAD, 18));
        il.add(new VarInsnNode(ILOAD, 18));
        il.add(new InsnNode(IMUL));
        // + sy²
        il.add(new VarInsnNode(ILOAD, 20));
        il.add(new VarInsnNode(ILOAD, 20));
        il.add(new InsnNode(IMUL));
        il.add(new InsnNode(IADD));
        // + sz²
        il.add(new VarInsnNode(ILOAD, 19));
        il.add(new VarInsnNode(ILOAD, 19));
        il.add(new InsnNode(IMUL));
        il.add(new InsnNode(IADD));
        il.add(new VarInsnNode(ISTORE, 21));

        // if (dist > size*size) continue
        il.add(new VarInsnNode(ILOAD, 21));
        il.add(new VarInsnNode(ILOAD, 17));
        il.add(new VarInsnNode(ILOAD, 17));
        il.add(new InsnNode(IMUL));
        il.add(new JumpInsnNode(IF_ICMPGT, lblSyEnd));

        // chunk.func_150807_a(dx+sx, y+sy, dz+sz, Blocks.stone, 0)
        // args: chunk, int, int, int, Block, int
        il.add(new VarInsnNode(ALOAD, 1));   // chunk
        il.add(new VarInsnNode(ILOAD, 14));  // dx
        il.add(new VarInsnNode(ILOAD, 18));  // sx
        il.add(new InsnNode(IADD));
        il.add(new VarInsnNode(ILOAD, 16));  // y
        il.add(new VarInsnNode(ILOAD, 20));  // sy
        il.add(new InsnNode(IADD));
        il.add(new VarInsnNode(ILOAD, 15));  // dz
        il.add(new VarInsnNode(ILOAD, 19));  // sz
        il.add(new InsnNode(IADD));
        il.add(new FieldInsnNode(GETSTATIC,
            "net/minecraft/init/Blocks", "stone", "Lnet/minecraft/block/Block;"));
        il.add(new InsnNode(ICONST_0));
        il.add(new MethodInsnNode(INVOKEVIRTUAL,
            "net/minecraft/world/chunk/Chunk",
            "func_150807_a", "(IIILnet/minecraft/block/Block;I)Z"));
        il.add(new InsnNode(POP)); // pop the boolean result

        // sy++
        il.add(new IincInsnNode(20, 1));
        il.add(new JumpInsnNode(GOTO, lblSyStart));
        il.add(lblSyEnd);

        // sz++
        il.add(new IincInsnNode(19, 1));
        il.add(new JumpInsnNode(GOTO, lblSzStart));
        il.add(lblSzEnd);

        // sx++
        il.add(new IincInsnNode(18, 1));
        il.add(new JumpInsnNode(GOTO, lblSxStart));
        il.add(lblSxEnd);

        // i++ (incremento do for externo)
        il.add(new IincInsnNode(11, 1));
        il.add(new JumpInsnNode(GOTO, lblLoopStart));
        il.add(lblLoopEnd);

        // return
        il.add(lblReturn1);
        il.add(new InsnNode(RETURN));
        il.add(new LabelNode()); // end label final pra frame

        // ============ BODY END ============

        // Adiciona o método à classe
        cn.methods.add(mn);

        // === PASSO 2: Injeta chamada no FINAL do generateOrganicClouds ===
        MethodNode goc = null;
        for (Object objM : cn.methods) {
            MethodNode m = (MethodNode) objM;
            if ("generateOrganicClouds".equals(m.name)) {
                goc = m;
                break;
            }
        }
        if (goc == null) throw new RuntimeException("generateOrganicClouds nao encontrado");

        // Acha o último RETURN do método
        InsnList gocInsns = goc.instructions;
        AbstractInsnNode lastReturn = null;
        AbstractInsnNode cur = gocInsns.getFirst();
        while (cur != null) {
            if (cur.getOpcode() == Opcodes.RETURN) {
                lastReturn = cur; // pega o último
            }
            cur = cur.getNext();
        }
        if (lastReturn == null) throw new RuntimeException("RETURN nao encontrado em generateOrganicClouds");

        // Insere ANTES do último RETURN:
        // aload_0 (this) - mas é private, então tem 'this' como var 0
        // aload_1 (chunk) - é o parâmetro 1
        // iload_2 (cx) - é o parâmetro 2
        // iload_3 (cz) - é o parâmetro 3
        // invokevirtual generateRocks(Chunk, int, int)V
        InsnList toInsert = new InsnList();
        toInsert.add(new VarInsnNode(ALOAD, 0));
        toInsert.add(new VarInsnNode(ALOAD, 1));
        toInsert.add(new VarInsnNode(ILOAD, 2));
        toInsert.add(new VarInsnNode(ILOAD, 3));
        toInsert.add(new MethodInsnNode(INVOKEVIRTUAL,
            "com/voiddim/dimension/ChunkProviderVoidDim",
            "generateRocks", "(Lnet/minecraft/world/chunk/Chunk;II)V"));

        gocInsns.insertBefore(lastReturn, toInsert);

        // === PASSO 3: Regrava classe ===
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        entries.put(CLASS_INTERNAL + ".class", cw.toByteArray());

        // === PASSO 4: Reescreve JAR ===
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(outPath))) {
            for (Map.Entry<String, byte[]> e : entries.entrySet()) {
                JarEntry je = new JarEntry(e.getKey());
                jos.putNextEntry(je);
                jos.write(e.getValue());
                jos.closeEntry();
            }
        }

        System.out.println("[OK] RockSpawner aplicado: " + outPath);
    }
}
