package com.voiddim.block;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;

/**
 * Registry central dos blocos do mod.
 */
public class VoidDimBlocks {

    public static Block yellowCloud;

    public static void init() {
        yellowCloud = new BlockYellowCloud().setBlockName("yellowCloud");
        GameRegistry.registerBlock(yellowCloud, "yellowCloud");
    }
}
