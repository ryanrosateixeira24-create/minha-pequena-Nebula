package com.voiddim.handler;

import com.voiddim.VoidDimMod;
import com.voiddim.dimension.FixedPointTeleporter;
import com.voiddim.util.ExtendedPlayer;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.WorldServer;

/**
 * Helpers de teleporte entre dimensões.
 * Constrói uma pequena plataforma segura no destino (bedrock na void, netherrack no Nether).
 */
public class TeleportHelper {

    private static final int VOID_PLATFORM_X   = 0;
    private static final int VOID_PLATFORM_Z   = 0;
    private static final int VOID_SCAN_START_Y = 80;
    private static final int VOID_FALLBACK_Y   = 20;

    private static final int NETHER_PLATFORM_X = 0;
    private static final int NETHER_PLATFORM_Y = 100;
    private static final int NETHER_PLATFORM_Z = 0;

    /** Constrói uma plataforma 5x5 no destino, com 2 blocos de ar acima. */
    private static void buildPlatform(WorldServer world, int px, int py, int pz, Block platformBlock) {
        for (int x = px - 2; x <= px + 2; x++) {
            for (int z = pz - 2; z <= pz + 2; z++) {
                world.setBlock(x, py - 1, z, platformBlock);
                world.setBlock(x, py,     z, Blocks.air);
                world.setBlock(x, py + 1, z, Blocks.air);
            }
        }
    }

    /** Escaneia de VOID_SCAN_START_Y pra baixo, retorna Y seguro (topo das nuvens + 4). */
    private static int findTopOfClouds(WorldServer world, int x, int z) {
        for (int y = VOID_SCAN_START_Y; y > 0; y--) {
            if (world.isAirBlock(x, y, z)) continue;
            return y + 4;
        }
        return VOID_FALLBACK_Y;
    }

    public static void sendToVoidDimension(EntityPlayerMP player) {
        MinecraftServer server = MinecraftServer.getServer();
        WorldServer voidWorld = server.worldServerForDimension(VoidDimMod.VOID_DIMENSION_ID);

        // Garante que o chunk de destino existe antes de tentar achar a superfície.
        voidWorld.getChunkProvider().loadChunk(0, 0);

        int platformY = findTopOfClouds(voidWorld, VOID_PLATFORM_X, VOID_PLATFORM_Z);
        buildPlatform(voidWorld, VOID_PLATFORM_X, platformY, VOID_PLATFORM_Z, Blocks.bedrock);

        double x = VOID_PLATFORM_X + 0.5;
        double y = platformY;
        double z = VOID_PLATFORM_Z + 0.5;

        FixedPointTeleporter teleporter = new FixedPointTeleporter(voidWorld, x, y, z, player.rotationYaw);
        server.getConfigurationManager().transferPlayerToDimension(
                player, VoidDimMod.VOID_DIMENSION_ID, teleporter);
        player.setPositionAndUpdate(x, y, z);
        player.fallDistance = 0.0F;
    }

    public static void sendToNether(EntityPlayerMP player) {
        MinecraftServer server = MinecraftServer.getServer();
        WorldServer netherWorld = server.worldServerForDimension(-1);
        buildPlatform(netherWorld, NETHER_PLATFORM_X, NETHER_PLATFORM_Y, NETHER_PLATFORM_Z, Blocks.netherrack);

        double x = NETHER_PLATFORM_X + 0.5;
        double y = NETHER_PLATFORM_Y;
        double z = NETHER_PLATFORM_Z + 0.5;

        FixedPointTeleporter teleporter = new FixedPointTeleporter(netherWorld, x, y, z, player.rotationYaw);
        server.getConfigurationManager().transferPlayerToDimension(player, -1, teleporter);
        player.setPositionAndUpdate(x, y, z);
        player.fallDistance = 0.0F;
    }

    public static void sendToOverworld(EntityPlayerMP player) {
        MinecraftServer server = MinecraftServer.getServer();
        WorldServer overworld = server.worldServerForDimension(0);

        double x, y, z;
        ExtendedPlayer ext = ExtendedPlayer.get(player);
        if (ext != null && ext.hasReturnPoint()) {
            x = ext.getReturnX();
            y = ext.getReturnY();
            z = ext.getReturnZ();
        } else {
            ChunkCoordinates spawn = overworld.getSpawnPoint();
            x = spawn.posX + 0.5;
            y = spawn.posY;
            z = spawn.posZ + 0.5;
        }

        FixedPointTeleporter teleporter = new FixedPointTeleporter(overworld, x, y, z, player.rotationYaw);
        server.getConfigurationManager().transferPlayerToDimension(player, 0, teleporter);
        player.setPositionAndUpdate(x, y, z);
        player.fallDistance = 0.0F;
    }
}
