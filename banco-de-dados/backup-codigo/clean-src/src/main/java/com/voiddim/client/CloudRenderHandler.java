package com.voiddim.client;

import org.lwjgl.opengl.GL11;

import com.voiddim.block.BlockYellowCloud;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

/**
 * V22 — Cache de volta pra otimizar, mas sem os bugs dos V18/V20.
 *
 * V21 removi TODO cache -> 500 nuvens/chunk = 500 chamadas de Surface Nets/frame ->
 * derreteu o pc do papai. Precisamos do cache.
 *
 * V18 fix falho: sé setava lastKey quando renderChunk retornava true. Piorou o bug
 * porque quando dava false por edge case, chunk sumia mais.
 *
 * V22: solução correta —
 *   1) Cache lastKey no ThreadLocal (evita re-renderizar mesmo chunk 100x).
 *   2) SurfaceNetsCloud NUNCA retorna false por early-out (V20 removi o !anyCloud).
 *   3) Sempre setamos lastKey (mesmo se emitted=false). Se realmente não tem nuvem
 *      no chunk, é so 1 chamada e pronto.
 *   4) Try/catch protetor: se algo dá exception no algoritmo, retorna false
 *      pra vanilla cair no cubo default.
 */
public class CloudRenderHandler implements ISimpleBlockRenderingHandler {

    /** Ultima chave de chunk vista nesta thread. */
    private final ThreadLocal<Long> lastKey = new ThreadLocal<Long>() {
        @Override protected Long initialValue() { return Long.MIN_VALUE; }
    };

    private static long chunkKey(int cx, int cy, int cz) {
        long lx = cx & 0x3FFFFFL;
        long ly = cy & 0xFFL;
        long lz = cz & 0x3FFFFFL;
        return (lx << 30) | (ly << 22) | lz;
    }

    @Override
    public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
        Tessellator tess = Tessellator.field_78398_a;
        GL11.glPushMatrix();
        GL11.glRotatef(30.0F, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
        IIcon icon = block.func_149691_a(0, metadata);
        if (icon != null) {
            float minU = icon.func_94209_e(), maxU = icon.func_94212_f();
            float minV = icon.func_94206_g(), maxV = icon.func_94210_h();
            GL11.glDisable(GL11.GL_CULL_FACE);
            tess.func_78382_b();
            tess.func_78375_b(0.0F, -1.0F, 0.0F);
            tess.func_78374_a(0.0, 0.0, 1.0, minU, maxV);
            tess.func_78374_a(0.0, 0.0, 0.0, minU, minV);
            tess.func_78374_a(1.0, 0.0, 0.0, maxU, minV);
            tess.func_78374_a(1.0, 0.0, 1.0, maxU, maxV);
            tess.func_78381_a();
            GL11.glEnable(GL11.GL_CULL_FACE);
        }
        GL11.glPopMatrix();
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z,
                                    Block block, int modelId, RenderBlocks renderer) {
        int cx = x >> 4;
        int cy = y >> 4;
        int cz = z >> 4;
        long key = chunkKey(cx, cy, cz);

        // Mesmo chunk que a chamada anterior? Não faz nada (evita overdraw).
        if (key == lastKey.get()) {
            return false;
        }

        int chunkBaseX = cx << 4;
        int chunkBaseY = cy << 4;
        int chunkBaseZ = cz << 4;
        int brightness = block.func_149677_c(world, x, y, z);

        // Sempre atualiza lastKey ANTES de chamar. Se retornar true/false, tudo bem
        // — o Surface Nets do V20+ sempre executa até o fim (sem early-out) e emite
        // exatamente os quads necessários.
        lastKey.set(key);

        try {
            return SurfaceNetsCloud.renderChunk(
                    chunkBaseX, chunkBaseY, chunkBaseZ,
                    world,
                    Tessellator.field_78398_a,
                    brightness);
        } catch (Throwable t) {
            // Exception silenciosa -> nao trava o jogo, apenas retorna vanilla
            return false;
        }
    }

    @Override public boolean shouldRender3DInInventory(int modelId) { return true; }
    @Override public int getRenderId() { return BlockYellowCloud.renderId; }
}
