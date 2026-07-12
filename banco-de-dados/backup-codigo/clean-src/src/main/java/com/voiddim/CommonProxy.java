package com.voiddim;

import net.minecraft.world.WorldProvider;

/**
 * Proxy comum (server). Métodos client-only ficam vazios aqui.
 */
public class CommonProxy {

    public void registerVoidSkyRenderer(WorldProvider provider) {
        // No-op no servidor.
    }

    public void preInit() {}
    public void init() {}
    public void postInit() {}
}
