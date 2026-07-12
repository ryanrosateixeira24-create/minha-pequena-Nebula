package com.voiddim.dimension;

import net.minecraft.entity.Entity;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;

/**
 * Teleporter que ignora portais e simplesmente coloca a entidade num ponto fixo.
 * Zera velocidade e fallDistance para evitar dano ao spawnar.
 */
public class FixedPointTeleporter extends Teleporter {

    private final double targetX;
    private final double targetY;
    private final double targetZ;
    private final float  targetYaw;

    public FixedPointTeleporter(WorldServer world, double x, double y, double z, float yaw) {
        super(world);
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
        this.targetYaw = yaw;
    }

    @Override
    public void placeInPortal(Entity entity, double x, double y, double z, float rotationYaw) {
        entity.setLocationAndAngles(targetX, targetY, targetZ, targetYaw, 0.0F);
        entity.motionX = 0.0;
        entity.motionY = 0.0;
        entity.motionZ = 0.0;
        entity.fallDistance = 0.0F;
    }

    @Override
    public boolean placeInExistingPortal(Entity entity, double x, double y, double z, float rotationYaw) {
        placeInPortal(entity, x, y, z, rotationYaw);
        return true;
    }

    @Override
    public boolean makePortal(Entity entity) {
        return true;
    }

    @Override
    public void removeStalePortalLocations(long totalWorldTime) {
        // No-op: não usamos portais.
    }
}
