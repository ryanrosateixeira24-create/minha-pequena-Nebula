package com.voiddim.dimension;

import java.util.List;
import java.util.Random;

import com.voiddim.block.VoidDimBlocks;

import cpw.mods.fml.common.FMLLog;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;

/**
 * Gera chunks da Dimensao do Vazio com nuvens ORGANICAS 3D (v2).
 *
 * Diferenca da versao antiga (v1):
 *   - v1 usava noise 2D: cada (x,z) decidia altura de camada -> placas horizontais.
 *   - v2 usa noise 3D: cada (x,y,z) tem seu proprio score -> forma organica.
 *
 * Tecnica:
 *   - Noise 3D value-based (Perlin quintic fade, 3D).
 *   - Domain warping: distorce coord antes de amostrar, cria formas torcidas.
 *   - Falloff vertical: envelope gaussiano centrado em Y_CENTER; densidade cai
 *     nas bordas (chao e teto) e concentra no meio -> "cordilheiras" nao "cubo".
 *   - Dois "gates" verticais opcionais (Y_LOW_CENTER e Y_HIGH_CENTER) pra manter
 *     a memoria de "duas camadas" da v1, mas agora conectadas por picos e vales.
 *
 * Custo de geracao: ~16*16*Y_MAX chamadas de noise por chunk.
 * Otimizacao: com octaves=3, cada chamada e ~30 ops. Chunk de 128 alt = 32k
 * chamadas = ~1M ops. Trivial.
 */
public class ChunkProviderVoidDim implements IChunkProvider {

    private final World worldObj;
    @SuppressWarnings("unused")
    private final Random random;

    // ============= TUNING =============

    /** Faixa Y onde nuvens podem existir. */
    private static final int Y_MIN = 3;
    private static final int Y_MAX = 60;

    /** Centros verticais dos "envelopes" (dois picos, memoria da v1). */
    private static final double Y_LOW_CENTER  = 12.0;
    private static final double Y_HIGH_CENTER = 40.0;
    private static final double Y_LOW_SIGMA   = 7.0;
    private static final double Y_HIGH_SIGMA  = 8.0;

    /** Escala do noise 3D principal (menor = formas maiores). */
    private static final double NOISE_SCALE = 0.035;

    /** Escala do domain warping (distorce coordenadas antes de amostrar). */
    private static final double WARP_SCALE  = 0.02;
    private static final double WARP_AMOUNT = 8.0;

    /**
     * Threshold: bloco vira nuvem se noise_final > THRESHOLD.
     * Baixar aumenta densidade. Subir cria formas mais isoladas.
     */
    private static final double THRESHOLD = 0.52;

    /**
     * Peso do envelope vertical na decisao final.
     * 1.0 = so noise vertical importa; 0.0 = so noise 3D importa.
     */
    private static final double ENVELOPE_WEIGHT = 0.55;

    // ============= V33 OTIMIZACAO =============

    /**
     * Y-cap por envelope: valor abaixo do qual o envelope contribui menos que
     * ENVELOPE_MIN_CONTRIB e por isso density NAO pode ultrapassar THRESHOLD
     * (mesmo com noise principal 1.0). Se envelope < esse minimo naquele Y,
     * pula todo o calculo do noise pra esse ponto (early-exit).
     *
     * Derivacao: density = (1-W)*n + W*e. Pra density > THRESHOLD mesmo com n=1:
     *     (1-W) + W*e > THRESHOLD
     *     e > (THRESHOLD - (1-W)) / W
     * Com THRESHOLD=0.52, W=0.55: e > (0.52 - 0.45) / 0.55 = 0.127
     */
    private static final double ENVELOPE_MIN_CONTRIB = 0.127;

    public ChunkProviderVoidDim(World world) {
        this.worldObj = world;
        this.random = new Random(world.func_72905_C());  // getSeed
    }

    // provideChunk (deobf) / func_73154_d (obf srg)
    public Chunk func_73154_d(int chunkX, int chunkZ) {
        Chunk chunk = new Chunk(this.worldObj, chunkX, chunkZ);

        byte[] biomes = chunk.func_76605_m();  // getBiomeArray
        for (int i = 0; i < biomes.length; i++) {
            biomes[i] = (byte) BiomeGenBase.plains.biomeID;
        }

        generateOrganicClouds(chunk, chunkX, chunkZ);
        chunk.func_76603_b();  // generateSkylightMap
        return chunk;
    }

    private void generateOrganicClouds(Chunk chunk, int chunkX, int chunkZ) {
        if (VoidDimBlocks.yellowCloud == null) {
            FMLLog.severe("[voiddim] VoidDimBlocks.yellowCloud NULL na geracao de chunk!");
            return;
        }

        if (chunkX == 0 && chunkZ == 0) {
            FMLLog.info("[voiddim] gerando chunk (0,0) com nuvens organicas 3D (v33 otimizado)");
        }

        final int baseX = chunkX << 4;
        final int baseZ = chunkZ << 4;

        // V33 OPT: pre-computa envelope por Y (16k pontos -> 58 calculos).
        // Envelope depende SO de Y, entao independente de x,z. Antes ficava
        // chamando gaussian() 32k vezes por chunk pra recalcular a mesma
        // coisa. Agora: 58 vezes por chunk.
        final double[] envelopeByY = new double[Y_MAX + 1];
        int yMinActive = Y_MAX + 1;
        int yMaxActive = Y_MIN - 1;
        for (int y = Y_MIN; y <= Y_MAX; y++) {
            double envLow  = gaussian((double) y, Y_LOW_CENTER,  Y_LOW_SIGMA);
            double envHigh = gaussian((double) y, Y_HIGH_CENTER, Y_HIGH_SIGMA);
            double env = envLow > envHigh ? envLow : envHigh;
            envelopeByY[y] = env;
            // Detecta faixa Y realmente ativa (envelope > MIN).
            if (env >= ENVELOPE_MIN_CONTRIB) {
                if (y < yMinActive) yMinActive = y;
                if (y > yMaxActive) yMaxActive = y;
            }
        }
        // Se envelope for cortado em todo lugar, nao gera nada.
        if (yMaxActive < yMinActive) return;

        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                final int worldX = baseX + lx;
                final int worldZ = baseZ + lz;

                // V33 OPT: itera SO a faixa Y onde envelope tem chance.
                for (int y = yMinActive; y <= yMaxActive; y++) {
                    if (isCloudAtOpt(worldX, y, worldZ, envelopeByY[y])) {
                        chunk.func_150807_a(lx, y, lz, VoidDimBlocks.yellowCloud, 0);
                    }
                }
            }
        }
    }

    /**
     * V33 OPT: recebe envelope pre-computado por Y.
     * Domain warping consolidado em 1 chamada de noise em vez de 3.
     */
    private boolean isCloudAtOpt(int x, int y, int z, double envelope) {
        // Envelope ja foi pre-filtrado, mas double-check por seguranca:
        // se envelope * peso ja garante nao ultrapassar threshold, sai.
        // density_max = (1-W)*1 + W*envelope. Se density_max < THRESHOLD, sai.
        if ((1.0 - ENVELOPE_WEIGHT) + ENVELOPE_WEIGHT * envelope <= THRESHOLD) {
            return false;
        }

        double xd = x, yd = y, zd = z;

        // V33 OPT: domain warping consolidado. Antes chamava noise3D 3 vezes
        // com mesma coord mas seed diferente. Agora chama 1 vez e deriva
        // wx/wy/wz do mesmo hash (bit-shift).
        double warpBase = noise3D(xd * WARP_SCALE, yd * WARP_SCALE, zd * WARP_SCALE, 100);
        // Extrai 3 "componentes" pseudo-independentes rotacionando bits do valor.
        // (Warping nao precisa ser super uniforme, so ter tres direcoes distintas.)
        double wx = warpBase * WARP_AMOUNT;
        double wy = (warpBase * 1.31 - Math.floor(warpBase * 1.31 + 0.5)) * WARP_AMOUNT * 2.0;
        double wz = (warpBase * 0.71 + Math.sin(warpBase * 3.14) * 0.4) * WARP_AMOUNT;

        // Noise 3D principal na coord distorcida.
        double n = noise3DFractal(
                (xd + wx) * NOISE_SCALE,
                (yd + wy) * NOISE_SCALE,
                (zd + wz) * NOISE_SCALE,
                0, 3
        );

        double density = (1.0 - ENVELOPE_WEIGHT) * n + ENVELOPE_WEIGHT * envelope;
        return density > THRESHOLD;
    }

    // ============= NOISE 3D =============

    /**
     * Gaussiana normalizada 0..1 (pico em x=mu vale 1).
     */
    private double gaussian(double x, double mu, double sigma) {
        double dx = (x - mu) / sigma;
        return Math.exp(-0.5 * dx * dx);
    }

    private double noise3DFractal(double x, double y, double z, int seedOffset, int octaves) {
        double total = 0.0;
        double amplitude = 1.0;
        double maxAmp = 0.0;
        double freq = 1.0;
        for (int o = 0; o < octaves; o++) {
            total += noise3D(x * freq, y * freq, z * freq, seedOffset + o * 71) * amplitude;
            maxAmp += amplitude;
            amplitude *= 0.5;
            freq *= 2.0;
        }
        return total / maxAmp;
    }

    /**
     * Value noise 3D com Perlin quintic fade.
     * Retorna [0, 1].
     */
    private double noise3D(double x, double y, double z, int seedOffset) {
        int xi = (int) Math.floor(x);
        int yi = (int) Math.floor(y);
        int zi = (int) Math.floor(z);
        double xf = x - xi;
        double yf = y - yi;
        double zf = z - zi;

        // 8 vertices do cubo unitario
        double v000 = hash3(xi,     yi,     zi,     seedOffset);
        double v100 = hash3(xi + 1, yi,     zi,     seedOffset);
        double v010 = hash3(xi,     yi + 1, zi,     seedOffset);
        double v110 = hash3(xi + 1, yi + 1, zi,     seedOffset);
        double v001 = hash3(xi,     yi,     zi + 1, seedOffset);
        double v101 = hash3(xi + 1, yi,     zi + 1, seedOffset);
        double v011 = hash3(xi,     yi + 1, zi + 1, seedOffset);
        double v111 = hash3(xi + 1, yi + 1, zi + 1, seedOffset);

        // Perlin quintic fade (mais suave que lerp linear)
        double u = fade(xf);
        double v = fade(yf);
        double w = fade(zf);

        // trilinear interp
        double x00 = lerp(v000, v100, u);
        double x10 = lerp(v010, v110, u);
        double x01 = lerp(v001, v101, u);
        double x11 = lerp(v011, v111, u);
        double y0 = lerp(x00, x10, v);
        double y1 = lerp(x01, x11, v);
        return lerp(y0, y1, w);
    }

    private double fade(double t) {
        return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
    }

    private double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    /**
     * Hash 3D deterministico baseado em world seed.
     * Retorna [0, 1].
     */
    private double hash3(int x, int y, int z, int seed) {
        long h = (long) x * 374761393L
               + (long) y * 668265263L
               + (long) z * 982451653L
               + (long) seed * 1274126177L
               + worldObj.func_72905_C();  // getSeed
        h = (h ^ (h >>> 13)) * 1274126177L;
        h ^= (h >>> 16);
        h = (h ^ (h >>> 7)) * 2246822519L;
        h ^= (h >>> 11);
        return (double) (h & 0xFFFFFFL) / 16777215.0;
    }

    // ============= IChunkProvider (SRG obf names — MC 1.7.10 vanilla) =============

    public boolean func_73149_a(int x, int z) { return true; }
    public void    func_73153_a(IChunkProvider provider, int chunkX, int chunkZ) {}
    public boolean func_73151_a(boolean saveAll, IProgressUpdate progress) { return true; }
    public boolean func_73156_b() { return false; }
    public boolean func_73157_c() { return true; }
    public String  func_73148_d() { return "VoidDimChunkProvider"; }
    public List    func_73155_a(EnumCreatureType type, int x, int y, int z) { return null; }
    public ChunkPosition func_147416_a(World world, String structureName, int x, int y, int z) { return null; }
    public int     func_73152_e() { return 0; }
    public void    func_82695_e(int chunkX, int chunkZ) {}
    public Chunk   func_73158_c(int chunkX, int chunkZ) { return func_73154_d(chunkX, chunkZ); }
    public void    func_104112_b() {}
}
