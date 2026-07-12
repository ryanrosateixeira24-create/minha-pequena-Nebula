package com.voiddim.block;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cpw.mods.fml.common.FMLLog;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Bloco de nuvem amarela.
 *
 * - Sem colisão (você atravessa).
 * - Indestrutível por hit (hardness -1).
 * - Come em 3 mordidas via right-click (som + partícula).
 * - Render controlado por CloudRenderHandler.
 *
 * Otimizações desta versão:
 *  - biteCounts com auto-limpeza para evitar leak de memória em mundos longos.
 *  - shouldSideBeRendered corrige o offset do vizinho.
 */
public class BlockYellowCloud extends Block {

    public static int renderId = -1;

    private static final int BITES_TO_DISAPPEAR = 3;
    /** Limite acima do qual a mordida menos usada é descartada. */
    private static final int BITE_MAP_MAX_ENTRIES = 4096;
    private static final Map<String, Integer> biteCounts = new ConcurrentHashMap<String, Integer>();

    public BlockYellowCloud() {
        super(Material.cloth);
        setStepSound(soundTypeCloth);
        setHardness(-1.0F);
        setResistance(1.0F);
        setCreativeTab(CreativeTabs.tabBlock);
        setLightOpacity(0);
        // Bloco não ocupa cubo cheio -> reduz batches de iluminação
        useNeighborBrightness = true;
    }

    @Override
    public int getRenderType() {
        return renderId >= 0 ? renderId : super.getRenderType();
    }

    @Override public boolean isOpaqueCube()     { return false; }
    @Override public boolean renderAsNormalBlock() { return false; }
    @Override public boolean isNormalCube()     { return false; }
    @Override public int getRenderBlockPass()   { return 0; }

    /**
     * Corrigido: agora consulta o vizinho na direção correta (side offsets),
     * antes consultava o próprio bloco.
     */
    @Override
    public boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int side) {
        int nx = x, ny = y, nz = z;
        switch (side) {
            case 0: ny = y - 1; break; // DOWN
            case 1: ny = y + 1; break; // UP
            case 2: nz = z - 1; break; // NORTH
            case 3: nz = z + 1; break; // SOUTH
            case 4: nx = x - 1; break; // WEST
            case 5: nx = x + 1; break; // EAST
        }
        Block neighbor = world.getBlock(nx, ny, nz);
        return neighbor != this;
    }

    /** Sem colisão. */
    @Override
    public void addCollisionBoxesToList(World world, int x, int y, int z,
                                        AxisAlignedBB mask, List list, Entity entity) {
        // No-op de propósito.
    }

    @Override
    public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity) {
        // No-op de propósito.
    }

    /**
     * Mordida por right-click. Após BITES_TO_DISAPPEAR consome o bloco.
     */
    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player,
                                    int side, float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return true;
        }
        final String key = world.provider.dimensionId + ":" + x + ":" + y + ":" + z;
        Integer previous = biteCounts.get(key);
        int bites = (previous == null ? 0 : previous) + 1;

        FMLLog.info("[voiddim] onBlockActivated em (" + x + "," + y + "," + z + ") mordida " + bites);

        world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5,
                "random.eat", 0.6F, 0.9F + world.rand.nextFloat() * 0.2F);
        int effectData = Block.getIdFromBlock(this);
        world.playAuxSFX(2001, x, y, z, effectData);

        if (bites >= BITES_TO_DISAPPEAR) {
            world.setBlockToAir(x, y, z);
            biteCounts.remove(key);
        } else {
            biteCounts.put(key, bites);
            // Prevenção contra leak: se o mapa crescer demais, descarta entradas antigas.
            if (biteCounts.size() > BITE_MAP_MAX_ENTRIES) {
                trimBiteCounts();
            }
        }
        return true;
    }

    private static void trimBiteCounts() {
        // Trim simples: mantém apenas metade das entradas (as mais recentes segundo a ordem de iteração).
        int toRemove = biteCounts.size() - BITE_MAP_MAX_ENTRIES / 2;
        java.util.Iterator<String> it = biteCounts.keySet().iterator();
        while (it.hasNext() && toRemove > 0) {
            it.next();
            it.remove();
            toRemove--;
        }
    }

    @Override
    public void registerBlockIcons(IIconRegister iconRegister) {
        this.blockIcon = iconRegister.registerIcon("voiddim:yellow_cloud");
    }

    @Override
    public int getLightValue(IBlockAccess world, int x, int y, int z) {
        return 0;
    }

    @Override
    public float getAmbientOcclusionLightValue() {
        return 1.0F;
    }

    @Override
    public boolean getUseNeighborBrightness() {
        return true;
    }
}
