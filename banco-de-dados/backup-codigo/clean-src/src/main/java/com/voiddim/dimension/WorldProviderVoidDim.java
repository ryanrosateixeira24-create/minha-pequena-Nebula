package com.voiddim.dimension;

import com.voiddim.VoidDimMod;

import net.minecraft.util.Vec3;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.biome.WorldChunkManager;
import net.minecraft.world.chunk.IChunkProvider;

/**
 * WorldProvider da Dimensao do Vazio (id 30).
 *
 * V33 FIX: usa SRG names em TODOS os overrides (nao so getCloudHeight).
 * Antes eu usava nomes deobf tipo getDimensionName() -- que sao abstract
 * na WorldProvider vanilla. JVM nao reconhecia como override, gerava
 * AbstractMethodError no primeiro tick do server.
 *
 * SRG map:
 *   getDimensionName       = func_80007_l    (abstract, obrigatorio)
 *   getHorizon             = func_72919_O
 *   getFogColor            = func_76562_b
 *   isSurfaceWorld         = func_76569_d
 *   canRespawnHere         = func_76567_e
 *   calcSunriseSunsetColors= func_76560_a
 *   generateLightBrightnessTable = func_76556_a
 *   createChunkGenerator   = func_76555_c
 *   registerWorldChunkManager = func_72913_a  (protected)
 *   getCloudHeight         = func_76571_f
 *
 * Metodos que nao tem SRG name (foram adicionados pelo Forge):
 *   getSaveFolder          -- Forge, nome deobf normal OK
 *   canDoLightning         -- Forge, nome deobf normal OK
 */
public class WorldProviderVoidDim extends WorldProvider {

    @Override
    protected void func_76572_b() {  // registerWorldChunkManager (SRG do MCP CSV)
        this.worldChunkMgr = new WorldChunkManager(this.worldObj);
        this.hasNoSky = true;
        this.dimensionId = VoidDimMod.VOID_DIMENSION_ID;
        VoidDimMod.proxy.registerVoidSkyRenderer(this);
    }

    @Override
    public IChunkProvider func_76555_c() {  // createChunkGenerator
        return new ChunkProviderVoidDim(this.worldObj);
    }

    @Override
    public String func_80007_l() {  // getDimensionName (ABSTRACT)
        return "Dimensão do Vazio";
    }

    /** Forge-added, sem SRG. Nome deobf. */
    public String getSaveFolder() {
        return "DIM_VOIDDIM";
    }

    @Override
    public boolean func_76567_e() {  // canRespawnHere
        return false;
    }

    @Override
    public boolean func_76569_d() {  // isSurfaceWorld
        return false;
    }

    @Override
    public float[] func_76560_a(float celestialAngle, float partialTicks) {  // calcSunriseSunsetColors
        return null;
    }

    /** Forge-added, sem SRG. Nome deobf. */
    public boolean canDoLightning(net.minecraft.world.chunk.Chunk chunk) {
        return false;
    }

    @Override
    protected void func_76556_a() {  // generateLightBrightnessTable
        for (int i = 0; i < this.lightBrightnessTable.length; i++) {
            this.lightBrightnessTable[i] = 1.0F;
        }
    }

    @Override
    public double func_72919_O() {  // getHorizon
        return 1.0;
    }

    @Override
    public Vec3 func_76562_b(float celestialAngle, float partialTicks) {  // getFogColor
        return Vec3.createVectorHelper(0.93, 0.55, 0.75);
    }

    /**
     * V33: desabilita nuvens brancas vanilla do MC.
     * SRG: func_76571_f = getCloudHeight.
     */
    @Override
    public float func_76571_f() {
        return -1.0F;
    }
}
