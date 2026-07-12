package com.voiddim.client;

import org.lwjgl.opengl.GL11;

import com.voiddim.block.VoidDimBlocks;
import com.voiddim.dimension.WorldProviderVoidDim;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderWorldLastEvent;

/**
 * V31 -- HALO/BLOOM ao redor das nuvens existentes.
 *
 * Ideia: em vez de renderizar "neblina" separada (planos, particulas, etc),
 * eu leio os blocos de nuvem em volta do jogador (raio pequeno) e pra cada
 * um desenho um cubo ligeiramente MAIOR com alpha baixo dourado.
 *
 * Isso faz as nuvens ganharem um "halo volumetrico" que:
 *  - E worldspace 100% (posicoes fixas dos blocos, nunca segue camera)
 *  - E PROPRIEDADE das nuvens (nao e uma placa separada, nunca vira placa)
 *  - Integra naturalmente porque ta ligado a geometria que ja existe
 *
 * CUSTO:
 *  - Raio 12 = ate 12^3*8 = ~14k blocos scan, mas 99% nao sao nuvem, filtro rapido
 *  - Cada nuvem = 24 vertices (cubo com culling frontal)
 *  - Cache: se position/tick nao mudou muito, reusa lista de nuvens encontradas
 *  - Tipico: ~50-200 nuvens visiveis -> 1200-4800 vertices/frame. Ridiculo.
 *
 * TUNING (3 sliders):
 *  - HALO_EXPAND: quanto o cubo do halo cresce alem do bloco (0.5 = 0.5 bloco)
 *  - HALO_ALPHA: opacidade base do halo
 *  - SCAN_RADIUS: raio em blocos ao redor do player pra scan
 */
public class CloudMistHandler {

    // ================= TUNING =================

    /** Quanto o cubo do halo cresce alem das bordas do bloco (em blocos). */
    private static final float HALO_EXPAND = 0.65F;

    /** Opacidade base do halo. */
    private static final float HALO_ALPHA = 0.22F;

    /** Cor dourada do halo. */
    private static final float HALO_R = 1.00F;
    private static final float HALO_G = 0.82F;
    private static final float HALO_B = 0.30F;

    /** Raio horizontal (X/Z) de scan. Y usa faixa fixa. */
    private static final int SCAN_RADIUS = 12;

    /** Faixa Y de scan (nuvens existem tipicamente em Y=3..60). */
    private static final int SCAN_Y_MIN = 0;
    private static final int SCAN_Y_MAX = 60;

    // ================= CACHE =================

    /**
     * Cache das nuvens encontradas: posicoes empacotadas em int[].
     * Reusa se player nao mudou de bloco.
     */
    private int[] cloudPositions = new int[0];
    private int cloudCount = 0;
    private int lastPlayerBX = Integer.MIN_VALUE;
    private int lastPlayerBY = Integer.MIN_VALUE;
    private int lastPlayerBZ = Integer.MIN_VALUE;

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.func_71410_x();
        if (mc == null || mc.field_71439_g == null || mc.field_71441_e == null) return;

        EntityPlayer player = mc.field_71439_g;
        if (!(player.field_70170_p.field_73011_w instanceof WorldProviderVoidDim)) return;

        WorldClient world = mc.field_71441_e;
        float partialTicks = event.partialTicks;

        // Camera interpolada (worldspace absoluto).
        double camX = player.field_70142_S + (player.field_70165_t - player.field_70142_S) * partialTicks;
        double camY = player.field_70137_T + (player.field_70163_u - player.field_70137_T) * partialTicks;
        double camZ = player.field_70136_U + (player.field_70161_v - player.field_70136_U) * partialTicks;

        int pbx = (int) Math.floor(camX);
        int pbz = (int) Math.floor(camZ);
        int pby = (int) Math.floor(camY);

        // Rescan cache se player andou mais que 4 blocos em qualquer direcao.
        if (Math.abs(pbx - lastPlayerBX) > 4 ||
            Math.abs(pbz - lastPlayerBZ) > 4 ||
            Math.abs(pby - lastPlayerBY) > 4) {
            rescan(world, pbx, pby, pbz);
            lastPlayerBX = pbx;
            lastPlayerBY = pby;
            lastPlayerBZ = pbz;
        }

        if (cloudCount == 0) return;

        // --- Setup GL ---
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDepthMask(false);

        Tessellator tess = Tessellator.field_78398_a;
        tess.func_78382_b();
        tess.func_78369_a(HALO_R, HALO_G, HALO_B, HALO_ALPHA);

        // Pra cada nuvem no cache, desenha um cubo expandido.
        for (int i = 0; i < cloudCount; i++) {
            int packed = cloudPositions[i];
            int bx = ((packed >> 20) & 0xFFF) - 2048 + lastPlayerBX;
            int by = ((packed >> 12) & 0xFF);
            int bz = (packed & 0xFFF) - 2048 + lastPlayerBZ;
            drawHaloCube(tess, bx, by, bz);
        }

        tess.func_78381_a();

        GL11.glColor4f(1F, 1F, 1F, 1F);
        GL11.glDepthMask(true);
        GL11.glPopAttrib();
    }

    /**
     * Escaneia blocos de nuvem num raio ao redor do player e guarda no cache.
     */
    private void rescan(WorldClient world, int cx, int cy, int cz) {
        int r = SCAN_RADIUS;
        // Estimativa max = (2r+1)^2 * altura, mas maioria descartada. Alloc gordo:
        int maxCap = (2 * r + 1) * (2 * r + 1) * (SCAN_Y_MAX - SCAN_Y_MIN + 1);
        if (cloudPositions.length < maxCap) {
            cloudPositions = new int[maxCap];
        }
        cloudCount = 0;

        Block target = VoidDimBlocks.yellowCloud;
        if (target == null) return;

        for (int dx = -r; dx <= r; dx++) {
            int bx = cx + dx;
            for (int dz = -r; dz <= r; dz++) {
                int bz = cz + dz;
                for (int by = SCAN_Y_MIN; by <= SCAN_Y_MAX; by++) {
                    Block b = world.func_147439_a(bx, by, bz);
                    if (b == target) {
                        // Empacota (dx+2048, by, dz+2048) em int (12+8+12 bits)
                        int packed = ((dx + 2048) << 20) | ((by & 0xFF) << 12) | (dz + 2048);
                        cloudPositions[cloudCount++] = packed;
                    }
                }
            }
        }
    }

    /**
     * Desenha um cubo expandido em torno da posicao do bloco (bx, by, bz).
     * 24 vertices (6 faces * 4). UV dummy pois GL_TEXTURE_2D esta desligado.
     */
    private void drawHaloCube(Tessellator tess, int bx, int by, int bz) {
        double x0 = bx - HALO_EXPAND;
        double x1 = bx + 1 + HALO_EXPAND;
        double y0 = by - HALO_EXPAND;
        double y1 = by + 1 + HALO_EXPAND;
        double z0 = bz - HALO_EXPAND;
        double z1 = bz + 1 + HALO_EXPAND;

        // Bottom (-Y)
        tess.func_78374_a(x0, y0, z0, 0, 0);
        tess.func_78374_a(x1, y0, z0, 0, 0);
        tess.func_78374_a(x1, y0, z1, 0, 0);
        tess.func_78374_a(x0, y0, z1, 0, 0);
        // Top (+Y)
        tess.func_78374_a(x0, y1, z0, 0, 0);
        tess.func_78374_a(x0, y1, z1, 0, 0);
        tess.func_78374_a(x1, y1, z1, 0, 0);
        tess.func_78374_a(x1, y1, z0, 0, 0);
        // North (-Z)
        tess.func_78374_a(x0, y0, z0, 0, 0);
        tess.func_78374_a(x0, y1, z0, 0, 0);
        tess.func_78374_a(x1, y1, z0, 0, 0);
        tess.func_78374_a(x1, y0, z0, 0, 0);
        // South (+Z)
        tess.func_78374_a(x0, y0, z1, 0, 0);
        tess.func_78374_a(x1, y0, z1, 0, 0);
        tess.func_78374_a(x1, y1, z1, 0, 0);
        tess.func_78374_a(x0, y1, z1, 0, 0);
        // West (-X)
        tess.func_78374_a(x0, y0, z0, 0, 0);
        tess.func_78374_a(x0, y0, z1, 0, 0);
        tess.func_78374_a(x0, y1, z1, 0, 0);
        tess.func_78374_a(x0, y1, z0, 0, 0);
        // East (+X)
        tess.func_78374_a(x1, y0, z0, 0, 0);
        tess.func_78374_a(x1, y1, z0, 0, 0);
        tess.func_78374_a(x1, y1, z1, 0, 0);
        tess.func_78374_a(x1, y0, z1, 0, 0);
    }
}
