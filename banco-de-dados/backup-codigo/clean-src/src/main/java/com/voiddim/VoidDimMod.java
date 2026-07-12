package com.voiddim;

import com.voiddim.block.VoidDimBlocks;
import com.voiddim.dimension.WorldProviderVoidDim;
import com.voiddim.handler.PlayerEventHandler;
import com.voiddim.item.ItemReturnHome;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.item.Item;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;

/**
 * Entry point do mod "Dimensão do Vazio".
 * Registra a dimensão 30, o bloco de nuvem amarela e o cristal de retorno.
 */
@Mod(modid = VoidDimMod.MODID, name = "Dimensão do Vazio", version = "1.0.4")
public class VoidDimMod {

    public static final String MODID = "voiddim";
    public static final int VOID_DIMENSION_ID = 30;

    @SidedProxy(clientSide = "com.voiddim.ClientProxy", serverSide = "com.voiddim.CommonProxy")
    public static CommonProxy proxy;

    public static Item returnHomeCrystal;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // IMPORTANTE: registrar blocos ANTES da dimensão, senão o ChunkProvider
        // recebe VoidDimBlocks.yellowCloud == null durante a geração.
        VoidDimBlocks.init();

        if (!DimensionManager.isDimensionRegistered(VOID_DIMENSION_ID)) {
            DimensionManager.registerProviderType(VOID_DIMENSION_ID, WorldProviderVoidDim.class, false);
            DimensionManager.registerDimension(VOID_DIMENSION_ID, VOID_DIMENSION_ID);
        }

        returnHomeCrystal = new ItemReturnHome();
        GameRegistry.registerItem(returnHomeCrystal, "returnHomeCrystal");

        proxy.preInit();
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        PlayerEventHandler handler = new PlayerEventHandler();
        MinecraftForge.EVENT_BUS.register(handler);
        FMLCommonHandler.instance().bus().register(handler);
        proxy.init();
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit();
    }
}
