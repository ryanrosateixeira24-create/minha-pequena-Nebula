package com.voiddim;

import com.voiddim.block.BlockYellowCloud;
import com.voiddim.client.CloudMistHandler;
import com.voiddim.client.CloudRenderHandler;
import com.voiddim.client.HaloRenderHandler;
import com.voiddim.client.SkyRendererVoidDim;

import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.FMLLog;
import net.minecraft.world.WorldProvider;
import net.minecraftforge.client.IRenderHandler;
import net.minecraftforge.common.MinecraftForge;

/**
 * Proxy client. Registra renderers, halo e skybox.
 */
public class ClientProxy extends CommonProxy {

    @Override
    public void registerVoidSkyRenderer(WorldProvider provider) {
        provider.setSkyRenderer((IRenderHandler) new SkyRendererVoidDim());
    }

    @Override
    public void init() {
        super.init();

        // Halo dourado acima da cabeça do jogador quando ele estiver na dimensão do vazio.
        try {
            MinecraftForge.EVENT_BUS.register(new HaloRenderHandler());
            FMLLog.info("[voiddim] HaloRenderHandler registrado via RenderPlayerEvent.Specials.Post (1.7.10 compat)");
        } catch (Exception e) {
            FMLLog.severe("[voiddim] Falha ao registrar HaloRenderHandler: " + e);
        }

        // Renderer custom das nuvens (estilo NoCubes).
        try {
            BlockYellowCloud.renderId = RenderingRegistry.getNextAvailableRenderId();
            RenderingRegistry.registerBlockHandler(new CloudRenderHandler());
            FMLLog.info("[voiddim] CloudRenderHandler registrado! renderId=" + BlockYellowCloud.renderId
                    + " - nuvens estilo NoCubes ativadas");
        } catch (Exception e) {
            FMLLog.severe("[voiddim] Falha ao registrar CloudRenderHandler: " + e);
            e.printStackTrace();
        }

        // V25: NENHUM handler de fog. Removidos totalmente — todo o efeito
        // visual das nuvens (incluindo "atmosfera") esta dentro do SurfaceNetsCloud
        // via fade atmosferico, bloom, glitter, cel-shading etc. Custo REAL zero.

        // V33: CloudMistHandler REMOVIDO.
        // O halo dourado desenhado ao redor dos blocos de nuvem estava causando
        // artefatos visuais (placas amarelas quadradas flutuando no ceu quando
        // vistas de longe/em angulo). Papai confirmou o bug com print.
        // As nuvens em si (BlockYellowCloud + SurfaceNetsCloud) ja tem cor
        // dourada satisfatoria; o halo extra era supérfluo.
        // Arquivo CloudMistHandler.java mantido em disk mas nao registrado.
        FMLLog.info("[voiddim] CloudMistHandler DESATIVADO em v33 (bug placas amarelas no ceu)");
    }

    @Override
    public void postInit() {
        super.postInit();
        // NOTA: a antiga registerHaloLayer() (com LayerHalo / LayerRenderer do 1.8) foi removida.
        // O halo é 100% gerenciado por HaloRenderHandler via RenderPlayerEvent.
        FMLLog.info("[voiddim] Halo em modo compat 1.7.10 (via RenderPlayerEvent).");
    }
}
