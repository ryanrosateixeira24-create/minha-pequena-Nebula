package com.voiddim.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;

/**
 * Dados persistentes por jogador:
 *  - pendingDeathTeleport: usado para enviar o jogador para a dim 30 no respawn.
 *  - returnPoint: última posição segura no overworld, gravada durante o tick.
 */
public class ExtendedPlayer implements IExtendedEntityProperties {

    public static final String EXT_PROP_NAME = "VoidDimExtendedPlayer";

    private boolean pendingDeathTeleport;

    private boolean hasReturnPoint;
    private int returnDimension;
    private double returnX;
    private double returnY = 64.0;
    private double returnZ;

    public static void register(EntityPlayer player) {
        player.registerExtendedProperties(EXT_PROP_NAME, new ExtendedPlayer());
    }

    public static ExtendedPlayer get(EntityPlayer player) {
        return (ExtendedPlayer) player.getExtendedProperties(EXT_PROP_NAME);
    }

    // --- pending death ---
    public boolean isPendingDeathTeleport() { return pendingDeathTeleport; }
    public void setPendingDeathTeleport(boolean value) { this.pendingDeathTeleport = value; }

    // --- return point ---
    public boolean hasReturnPoint() { return hasReturnPoint; }

    public void setReturnPoint(int dimension, double x, double y, double z) {
        this.hasReturnPoint = true;
        this.returnDimension = dimension;
        this.returnX = x;
        this.returnY = y;
        this.returnZ = z;
    }

    public int getReturnDimension() { return returnDimension; }
    public double getReturnX() { return returnX; }
    public double getReturnY() { return returnY; }
    public double getReturnZ() { return returnZ; }

    @Override
    public void saveNBTData(NBTTagCompound compound) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setBoolean("pendingDeathTeleport", pendingDeathTeleport);
        tag.setBoolean("hasReturnPoint", hasReturnPoint);
        tag.setInteger("returnDimension", returnDimension);
        tag.setDouble("returnX", returnX);
        tag.setDouble("returnY", returnY);
        tag.setDouble("returnZ", returnZ);
        compound.setTag(EXT_PROP_NAME, tag);
    }

    @Override
    public void loadNBTData(NBTTagCompound compound) {
        NBTTagCompound tag = compound.getCompoundTag(EXT_PROP_NAME);
        if (tag != null) {
            pendingDeathTeleport = tag.getBoolean("pendingDeathTeleport");
            hasReturnPoint       = tag.getBoolean("hasReturnPoint");
            returnDimension      = tag.getInteger("returnDimension");
            returnX              = tag.getDouble("returnX");
            returnY              = tag.getDouble("returnY");
            returnZ              = tag.getDouble("returnZ");
        }
    }

    @Override
    public void init(Entity entity, World world) {
        // No-op.
    }
}
