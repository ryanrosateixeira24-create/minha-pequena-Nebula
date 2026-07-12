package com.voiddim.handler;

import com.voiddim.VoidDimMod;
import com.voiddim.util.ExtendedPlayer;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.PlayerTickEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.entity.EntityEvent.EntityConstructing;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.Clone;

/**
 * Gerencia o ciclo de vida do jogador em relação à dimensão do vazio.
 *
 * Regras:
 *  - Ao construir um EntityPlayer, anexa ExtendedPlayer.
 *  - Ao morrer no OVERWORLD: marca pendingDeathTeleport (vai pra dim 30 no respawn).
 *  - Ao morrer NA DIM 30: NÃO marca (para não ficar preso ciclando).
 *  - Ao clonar (respawn), preserva os dados extendidos.
 *  - Tick: salva returnPoint sempre que o jogador está com pés no chão no overworld.
 *  - Tick: se cair Y < -32 na dim 30, teleporta para o Nether.
 */
public class PlayerEventHandler {

    private static final double VOID_FALL_LIMIT = -32.0;

    @SubscribeEvent
    public void onEntityConstruct(EntityConstructing event) {
        if (event.entity instanceof EntityPlayer
                && ExtendedPlayer.get((EntityPlayer) event.entity) == null) {
            ExtendedPlayer.register((EntityPlayer) event.entity);
        }
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (!(event.entityLiving instanceof EntityPlayer)) return;
        if (event.entityLiving.worldObj.isRemote) return;

        EntityPlayer player = (EntityPlayer) event.entityLiving;

        // Só manda pra void quando o jogador morreu FORA da própria void.
        // Isso evita loops (morre na void -> respawn -> void -> morre -> void...).
        if (player.dimension == VoidDimMod.VOID_DIMENSION_ID) {
            return;
        }

        ExtendedPlayer ext = ExtendedPlayer.get(player);
        if (ext != null) {
            ext.setPendingDeathTeleport(true);
        }
    }

    @SubscribeEvent
    public void onPlayerClone(Clone event) {
        ExtendedPlayer oldExt = ExtendedPlayer.get(event.original);
        ExtendedPlayer newExt = ExtendedPlayer.get(event.entityPlayer);
        if (oldExt == null || newExt == null) return;

        NBTTagCompound tag = new NBTTagCompound();
        oldExt.saveNBTData(tag);
        newExt.loadNBTData(tag);
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (event.player.worldObj.isRemote) return;
        if (!(event.player instanceof EntityPlayerMP)) return;

        EntityPlayerMP player = (EntityPlayerMP) event.player;
        ExtendedPlayer ext = ExtendedPlayer.get(player);
        if (ext != null && ext.isPendingDeathTeleport()) {
            ext.setPendingDeathTeleport(false);
            TeleportHelper.sendToVoidDimension(player);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.worldObj.isRemote) return;
        if (!(event.player instanceof EntityPlayerMP)) return;

        EntityPlayerMP player = (EntityPlayerMP) event.player;
        ExtendedPlayer ext = ExtendedPlayer.get(player);
        if (ext == null) return;

        if (player.dimension == 0) {
            // Só salva returnPoint quando os pés estão no chão (evita salvar meio-pulo/void-fall).
            if (player.onGround) {
                ext.setReturnPoint(0, player.posX, player.posY, player.posZ);
            }
        } else if (player.dimension == VoidDimMod.VOID_DIMENSION_ID
                && player.posY < VOID_FALL_LIMIT) {
            TeleportHelper.sendToNether(player);
        }
    }
}
