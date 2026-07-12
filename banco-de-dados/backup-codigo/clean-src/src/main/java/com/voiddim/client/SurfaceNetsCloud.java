package com.voiddim.client;

import com.voiddim.block.VoidDimBlocks;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

/**
 * Surface Nets V26 — "Nébula Bold Style, mas com bom senso desta vez" 🎨
 * ================================================================
 * V25 saiu do controle: cel-shading agressivo + AO multiplicativo se somaram
 * e transformaram tudo numa lata de sardinha preto e amarelo 😭
 *
 * V26 volta pra base linda do V13 e adiciona MELHORIAS SUTIS em vez de agressivas:
 *
 *   1) ✨ Highlights mágicos MENOS FREQUENTES (~1% em vez de 5%)
 *      e MENOS INTENSOS (mistura suave, não pinta de branco).
 *
 *   2) 🌫️ Fade atmosférico MUITO sutil — só nuvens a 100+ blocos, e nem
 *      totalmente rosa (só 30% mistura no máximo).
 *
 *   3) 💫 Bloom nos topos MUITO leve — só topos MUITO altos (Y > 42),
 *      e o clarão é sutil (10% branco).
 *
 *   4) 🎭 Jitter senoidal SUAVE — amplitude reduzida pela metade.
 *
 * REMOVIDO do V25 (o que ficou horrível):
 *   ❌ Cel-shading nas bordas (transformou tudo em zebra)
 *   ❌ Trick de packing AO + notCloudNeighbors (bug!)
 *   ❌ Valores agressivos
 *
 * FILOSOFIA: MELHOR SUTIL DEMAIS QUE EXAGERADO. Sempre pode aumentar depois.
 */
public final class SurfaceNetsCloud {

    // ---------- Paleta ----------
    private static final float HIGH_R = 1.00F, HIGH_G = 0.94F, HIGH_B = 0.75F;
    private static final float MID_R  = 0.97F, MID_G  = 0.83F, MID_B  = 0.45F;
    private static final float LOW_R  = 0.88F, LOW_G  = 0.62F, LOW_B  = 0.28F;

    // Cor do céu (rosa)
    private static final float SKY_R = 0.93F, SKY_G = 0.55F, SKY_B = 0.75F;

    private static final float Y_LOW  = 0.0F;
    private static final float Y_MID  = 25.0F;
    private static final float Y_HIGH = 50.0F;

    private static final float AO_STRENGTH = 0.20F;

    // ---------- Efeitos V26 (todos sutis) ----------

    /** Fade atmosférico: só nuvens LONGE ficam rosadas, e no máximo 30% de mistura. */
    private static final float FADE_START = 80.0F;
    private static final float FADE_END = 160.0F;
    private static final float FADE_MAX = 0.30F;

    /** Highlights mágicos: raros e sutis. */
    private static final float GLITTER_CHANCE = 0.015F;    // ~1.5% dos vértices
    private static final float GLITTER_INTENSITY = 0.35F;  // mix 35% no máximo

    /** Bloom topo: só bem alto, muito sutil. */
    private static final float BLOOM_Y_MIN = 42.0F;
    private static final float BLOOM_STRENGTH = 0.15F;

    /** Jitter: sutil como o V13, mas via senoide. */
    private static final float JITTER_AMP = 0.08F;
    private static final float JITTER_FREQ = 0.35F;

    private static final int PADDING = 2;
    private static final int CACHE_DIM = 16 + 2 * PADDING;
    private static final int CACHE_SIZE = CACHE_DIM * CACHE_DIM * CACHE_DIM;

    // ---------- Lookup tables (Surface Nets clássico) ----------
    private static final int[] CUBE_EDGES = new int[24];
    private static final int[] EDGE_TABLE = new int[256];

    // ---------- Buffers thread-local ----------
    private static final ThreadLocal<boolean[]> IS_CLOUD_CACHE =
            new ThreadLocal<boolean[]>() {
                @Override protected boolean[] initialValue() { return new boolean[CACHE_SIZE]; }
            };
    private static final ThreadLocal<float[]> VERTEX_BUFFER =
            new ThreadLocal<float[]>() {
                @Override protected float[] initialValue() {
                    int cellsPerSlice = 19 * 19;
                    return new float[cellsPerSlice * 2 * 4];
                }
            };

    private SurfaceNetsCloud() {}

    private static int cacheIdx(int lx, int ly, int lz) {
        return (ly * CACHE_DIM + lz) * CACHE_DIM + lx;
    }

    public static boolean renderChunk(int chunkBaseX, int chunkBaseY, int chunkBaseZ,
                                      IBlockAccess world, Tessellator tess,
                                      int brightness) {
        final Block cloudBlock = VoidDimBlocks.yellowCloud;
        if (cloudBlock == null) return false;
        IIcon icon = cloudBlock.func_149691_a(0, 0);
        if (icon == null) return false;

        final float minU = icon.func_94209_e();
        final float maxU = icon.func_94212_f();
        final float minV = icon.func_94206_g();
        final float maxV = icon.func_94210_h();

        tess.func_78380_c(brightness);

        // Popula cache 20x20x20.
        final boolean[] isCloud = IS_CLOUD_CACHE.get();
        for (int ly = 0; ly < CACHE_DIM; ly++) {
            int wy = chunkBaseY + ly - PADDING;
            for (int lz = 0; lz < CACHE_DIM; lz++) {
                int wz = chunkBaseZ + lz - PADDING;
                for (int lx = 0; lx < CACHE_DIM; lx++) {
                    int wx = chunkBaseX + lx - PADDING;
                    isCloud[cacheIdx(lx, ly, lz)] = (world.func_147439_a(wx, wy, wz) == cloudBlock);
                }
            }
        }

        // Pega camera pra fade atmosférico.
        float camX = 0, camZ = 0;
        Minecraft mc = Minecraft.func_71410_x();
        if (mc != null && mc.field_71439_g != null) {
            camX = (float) mc.field_71439_g.field_70165_t;
            camZ = (float) mc.field_71439_g.field_70161_v;
        }

        // Loop Surface Nets.
        final int planeStride = 19;
        final int volumeStride = planeStride * planeStride;
        final int[] r = { 1, planeStride, volumeStride };
        final float[] grid = new float[8];
        final float[] buffer = VERTEX_BUFFER.get();
        for (int i = 0; i < buffer.length; i += 4) buffer[i] = Float.NaN;

        int bufno = 1;
        boolean emittedAny = false;

        for (int x2 = 0; x2 < 17; r[2] = -r[2]) {
            int m = 1 + planeStride * (1 + bufno * planeStride);

            for (int x1 = 0; x1 < 17; m += 2) {
                for (int x0 = 0; x0 < 17; ++m) {

                    int mask = 0;
                    int g = 0;
                    for (int k = 0; k < 2; k++) {
                        for (int j = 0; j < 2; j++) {
                            for (int i = 0; i < 2; i++, g++) {
                                float dens = densityFromCache(x0 + i, x1 + j, x2 + k, isCloud);
                                grid[g] = dens;
                                if (dens > 0f) mask |= (1 << g);
                            }
                        }
                    }

                    if (mask == 0 || mask == 255) {
                        x0++;
                        continue;
                    }

                    int edgeMask = EDGE_TABLE[mask];
                    int ecount = 0;
                    float vx = 0f, vy = 0f, vz = 0f;
                    for (int i = 0; i < 12; i++) {
                        if ((edgeMask & (1 << i)) == 0) continue;
                        ecount++;
                        int e0 = CUBE_EDGES[i << 1];
                        int e1 = CUBE_EDGES[(i << 1) + 1];
                        float g0 = grid[e0];
                        float g1 = grid[e1];
                        float t = g0 - g1;
                        if (Math.abs(t) > 0f) {
                            t = g0 / t;
                            int axis = 0;
                            for (int k = 1; axis < 3; k <<= 1) {
                                int a = e0 & k;
                                int b = e1 & k;
                                float add;
                                if (a != b) add = (a != 0) ? (1f - t) : t;
                                else        add = (a != 0) ? 1f : 0f;
                                if      (axis == 0) vx += add;
                                else if (axis == 1) vy += add;
                                else                vz += add;
                                axis++;
                            }
                        }
                    }

                    float s = 1f / ecount;
                    vx = (chunkBaseX + x0) + s * vx;
                    vy = (chunkBaseY + x1) + s * vy;
                    vz = (chunkBaseZ + x2) + s * vz;

                    // Jitter senoidal SUAVE.
                    vx += (float) Math.sin(vx * JITTER_FREQ + vz * 0.13) * JITTER_AMP;
                    vy += (float) Math.sin(vx * 0.17 + vy * JITTER_FREQ) * JITTER_AMP * 0.5F;
                    vz += (float) Math.cos(vz * JITTER_FREQ + vx * 0.11) * JITTER_AMP;

                    // Vertex AO puro (SEM packing hack do v25 que dava bug).
                    int cloudCount = 0;
                    for (int k = 0; k < 2; k++) {
                        for (int j = 0; j < 2; j++) {
                            for (int i = 0; i < 2; i++) {
                                if (isCloudAt(x0 + i, x1 + j, x2 + k, isCloud)) cloudCount++;
                            }
                        }
                    }
                    float ao = 1.0F - (cloudCount / 8.0F) * AO_STRENGTH;

                    int slot = m * 4;
                    buffer[slot    ] = vx;
                    buffer[slot + 1] = vy;
                    buffer[slot + 2] = vz;
                    buffer[slot + 3] = ao;  // AO puro, sem hack

                    // Emit quads.
                    for (int i = 0; i < 3; i++) {
                        if ((edgeMask & (1 << i)) == 0) continue;
                        int iu = (i + 1) % 3;
                        int iv = (i + 2) % 3;
                        int xu = (iu == 0) ? x0 : (iu == 1) ? x1 : x2;
                        int xv = (iv == 0) ? x0 : (iv == 1) ? x1 : x2;
                        if (xu == 0 || xv == 0) continue;

                        int du = r[iu];
                        int dv = r[iv];
                        int s0 = m * 4;
                        int s1 = (m - du) * 4;
                        int s2 = (m - du - dv) * 4;
                        int s3 = (m - dv) * 4;

                        if (Float.isNaN(buffer[s0]) || Float.isNaN(buffer[s1])
                         || Float.isNaN(buffer[s2]) || Float.isNaN(buffer[s3])) {
                            continue;
                        }

                        if ((mask & 1) != 0) {
                            emitVertex(tess, buffer, s0, camX, camZ, minU, maxV);
                            emitVertex(tess, buffer, s1, camX, camZ, maxU, maxV);
                            emitVertex(tess, buffer, s2, camX, camZ, maxU, minV);
                            emitVertex(tess, buffer, s3, camX, camZ, minU, minV);
                        } else {
                            emitVertex(tess, buffer, s0, camX, camZ, minU, maxV);
                            emitVertex(tess, buffer, s3, camX, camZ, maxU, maxV);
                            emitVertex(tess, buffer, s2, camX, camZ, maxU, minV);
                            emitVertex(tess, buffer, s1, camX, camZ, minU, minV);
                        }
                        emittedAny = true;
                    }

                    x0++;
                }
                x1++;
            }
            x2++;
            bufno ^= 1;
        }

        return emittedAny;
    }

    /**
     * V26 — emit sutil. AO + tricolor + 3 melhorias BEM discretas.
     */
    private static void emitVertex(Tessellator tess, float[] verts, int slot,
                                   float camX, float camZ, float u, float v) {
        float vx = verts[slot    ];
        float vy = verts[slot + 1];
        float vz = verts[slot + 2];
        float ao = verts[slot + 3];

        // (1) TRICOLOR por Y absoluto (base V13).
        float r, gg, b;
        if (vy <= Y_LOW) {
            r = LOW_R; gg = LOW_G; b = LOW_B;
        } else if (vy >= Y_HIGH) {
            r = HIGH_R; gg = HIGH_G; b = HIGH_B;
        } else if (vy < Y_MID) {
            float t = (vy - Y_LOW) / (Y_MID - Y_LOW);
            r = LOW_R + (MID_R - LOW_R) * t;
            gg = LOW_G + (MID_G - LOW_G) * t;
            b = LOW_B + (MID_B - LOW_B) * t;
        } else {
            float t = (vy - Y_MID) / (Y_HIGH - Y_MID);
            r = MID_R + (HIGH_R - MID_R) * t;
            gg = MID_G + (HIGH_G - MID_G) * t;
            b = MID_B + (HIGH_B - MID_B) * t;
        }

        // (2) AO — direto e simples, como V13.
        r *= ao; gg *= ao; b *= ao;

        // (3) BLOOM nos topos — sutil. Só afeta vértices bem altos.
        if (vy > BLOOM_Y_MIN) {
            float bloomFactor = (vy - BLOOM_Y_MIN) / (Y_HIGH - BLOOM_Y_MIN) * BLOOM_STRENGTH;
            if (bloomFactor > BLOOM_STRENGTH) bloomFactor = BLOOM_STRENGTH;
            r = r + (1F - r) * bloomFactor;
            gg = gg + (1F - gg) * bloomFactor;
            b = b + (1F - b) * bloomFactor;
        }

        // (4) HIGHLIGHTS MÁGICOS — raros e sutis.
        long h = (long)(vx * 73856093) ^ (long)(vy * 19349663) ^ (long)(vz * 83492791);
        h = h * 2654435769L;
        float glitterRoll = ((h & 0xFFFF) / 65535F);
        if (glitterRoll < GLITTER_CHANCE) {
            float glitterMix = (1F - (glitterRoll / GLITTER_CHANCE)) * GLITTER_INTENSITY;
            r = r + (1.0F - r) * glitterMix;   // pinta pra branco/amarelo claro
            gg = gg + (1.0F - gg) * glitterMix;
            b = b + (0.85F - b) * glitterMix;
        }

        // (5) FADE ATMOSFÉRICO — só nuvens longe do jogador.
        float dx = vx - camX;
        float dz = vz - camZ;
        float dist = (float) Math.sqrt(dx * dx + dz * dz);
        if (dist > FADE_START) {
            float fade = (dist - FADE_START) / (FADE_END - FADE_START);
            if (fade > FADE_MAX) fade = FADE_MAX;
            r = r + (SKY_R - r) * fade;
            gg = gg + (SKY_G - gg) * fade;
            b = b + (SKY_B - b) * fade;
        }

        // Clamp de segurança.
        if (r < 0F) r = 0F; else if (r > 1F) r = 1F;
        if (gg < 0F) gg = 0F; else if (gg > 1F) gg = 1F;
        if (b < 0F) b = 0F; else if (b > 1F) b = 1F;

        tess.func_78386_a(r, gg, b);
        tess.func_78374_a(vx, vy, vz, u, v);
    }

    private static float densityFromCache(int cx, int cy, int cz, boolean[] isCloud) {
        int d = 0;
        for (int k = 0; k < 2; k++) {
            for (int j = 0; j < 2; j++) {
                for (int i = 0; i < 2; i++) {
                    d += isCloudAt(cx - i, cy - j, cz - k, isCloud) ? 1 : -1;
                }
            }
        }
        return d;
    }

    private static boolean isCloudAt(int cx, int cy, int cz, boolean[] isCloud) {
        int lx = cx + PADDING;
        int ly = cy + PADDING;
        int lz = cz + PADDING;
        if (lx < 0 || lx >= CACHE_DIM || ly < 0 || ly >= CACHE_DIM
         || lz < 0 || lz >= CACHE_DIM) {
            return false;
        }
        return isCloud[cacheIdx(lx, ly, lz)];
    }

    // ---------- Static init ----------
    static {
        int k = 0;
        for (int i = 0; i < 8; i++) {
            for (int j = 1; j <= 4; j <<= 1) {
                int p = i ^ j;
                if (i <= p) {
                    CUBE_EDGES[k++] = i;
                    CUBE_EDGES[k++] = p;
                }
            }
        }
        for (int i = 0; i < 256; i++) {
            int em = 0;
            for (int j = 0; j < 24; j += 2) {
                boolean a = (i & (1 << CUBE_EDGES[j]))     != 0;
                boolean b = (i & (1 << CUBE_EDGES[j + 1])) != 0;
                if (a != b) em |= (1 << (j >> 1));
            }
            EDGE_TABLE[i] = em;
        }
    }
}
