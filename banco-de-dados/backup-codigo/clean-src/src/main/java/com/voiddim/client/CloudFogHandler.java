package com.voiddim.client;

import java.util.Random;

import com.voiddim.block.VoidDimBlocks;
import com.voiddim.dimension.WorldProviderVoidDim;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

/**
 * V24 — Fog via partículas nativas do Minecraft (custo ~ZERO).
 *
 * FILOSOFIA (baseada em feedback do papai):
 *   Esse mod vai crescer com muito mais features (Kamehameha, dragões,
 *   transformações...). Cada feature nova tem que caber num orçamento
 *   apertado. Fog custando 40+ quads/frame + hashes é insustentável.
 *
 * SOLUÇÃO V24:
 *   - Usa o sistema de partículas NATIVO do Minecraft (World.spawnParticle).
 *   - Handler client-side: no TickEvent.ClientTick, spawna umas 4-8 partículas
 *     "smoke" amareladas por tick apenas em posições que SÃO bloco de nuvem
 *     perto do jogador.
 *   - MC handleia: rendering (billboard grátis), culling, físicas, cleanup.
 *   - Não usamos handler de render — zero overhead no pipeline visual custom.
 *
 * CUSTO REAL:
 *   - ~5 partículas/tick × 20 ticks/s = 100 spawns/s = irrelevante.
 *   - MC já tem pool de partículas otimizado com LOD por distância.
 *   - Se o jogador NÃO estiver na dim voiddim: return imediato (1 instanceof).
 *
 * EFEITO VISUAL:
 *   - "Vapor" saindo naturalmente das nuvens.
 *   - Só aparece PERTO de nuvens (não cobre o horizonte).
 *   - Não atravessa o jogador (partícula é entity).
 *   - Fade natural por distância (MC handleia).
 */
public class CloudFogHandler {

    /** Nome da partícula MC. "cloud" = smoke branca leve. */
    private static final String PARTICLE_NAME = "cloud";

    /** Quantas partículas spawnar por tick (rate). */
    private static final int PARTICLES_PER_TICK = 6;

    /** Raio ao redor do jogador onde procurar nuvens (blocos). */
    private static final int SEARCH_RADIUS = 8;

    private final Random rand = new Random();

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        // Só age no fim do tick (evita rodar 2x por tick).
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.func_71410_x();
        if (mc == null || mc.field_71439_g == null || mc.field_71441_e == null) return;

        EntityPlayer player = mc.field_71439_g;
        World world = mc.field_71441_e;

        // Filtro rápido: só na dim voiddim.
        if (!(world.field_73011_w instanceof WorldProviderVoidDim)) return;

        Block cloudBlock = VoidDimBlocks.yellowCloud;
        if (cloudBlock == null) return;

        // Base do jogador em coords inteiras.
        int px = (int) Math.floor(player.field_70165_t);
        int py = (int) Math.floor(player.field_70163_u);
        int pz = (int) Math.floor(player.field_70161_v);

        // Spawna N partículas por tick em posições ALEATÓRIAS perto do jogador,
        // mas SOMENTE se aquela posição for um bloco de nuvem.
        for (int i = 0; i < PARTICLES_PER_TICK; i++) {
            int dx = rand.nextInt(SEARCH_RADIUS * 2) - SEARCH_RADIUS;
            int dy = rand.nextInt(SEARCH_RADIUS * 2) - SEARCH_RADIUS;
            int dz = rand.nextInt(SEARCH_RADIUS * 2) - SEARCH_RADIUS;
            int bx = px + dx;
            int by = py + dy;
            int bz = pz + dz;

            // world.getBlock(x, y, z)
            Block b = world.func_147439_a(bx, by, bz);
            if (b != cloudBlock) continue;

            // Posição aleatória dentro do bloco.
            double sx = bx + rand.nextDouble();
            double sy = by + rand.nextDouble();
            double sz = bz + rand.nextDouble();

            // Movimento sutil pra cima (vapor subindo).
            double vx = (rand.nextDouble() - 0.5) * 0.02;
            double vy = 0.01 + rand.nextDouble() * 0.02;
            double vz = (rand.nextDouble() - 0.5) * 0.02;

            // world.spawnParticle(name, x, y, z, vx, vy, vz)
            world.func_72869_a(PARTICLE_NAME, sx, sy, sz, vx, vy, vz);
        }
    }
}
