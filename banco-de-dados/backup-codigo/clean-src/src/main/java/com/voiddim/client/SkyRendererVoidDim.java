package com.voiddim.client;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.FMLLog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IRenderHandler;

/**
 * Skybox esférico com textura equirretangular. Roda lentamente sobre o eixo Y.
 * O raio é adaptativo ao render distance do jogador (mas clampado 24..100).
 * Otimizações desta versão:
 *  - display list rebuild só quando raio muda significativamente.
 *  - salva/restaura apenas os states GL que altera (evita glPushAttrib(ALL)).
 */
public class SkyRendererVoidDim extends IRenderHandler {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("voiddim", "textures/skybox/spherical_skybox.png");

    private static final int   SLICES         = 32;
    private static final int   STACKS         = 16;
    private static final float MAX_RADIUS     = 100.0F;
    private static final float MIN_RADIUS     = 24.0F;
    private static final float ROTATION_SPEED = 0.02F;

    private int   displayList  = -1;
    private boolean printedDebug = false;
    private float builtRadius  = -1.0F;

    @Override
    public void render(float partialTicks, WorldClient world, Minecraft mc) {
        if (!printedDebug) {
            printedDebug = true;
            FMLLog.info("[voiddim] SkyRendererVoidDim ativo (spherical skybox)");
        }

        // Salva só o essencial (evita glPushAttrib(GL_ALL_ATTRIB_BITS) que é caro em algumas GPUs).
        final boolean alphaWasEnabled = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        final boolean blendWasEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        final boolean fogWasEnabled   = GL11.glIsEnabled(GL11.GL_FOG);

        GL11.glPushMatrix();

        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_FOG);
        GL11.glDepthMask(false);
        GL11.glColor4f(1F, 1F, 1F, 1F);
        GL11.glEnable(GL11.GL_TEXTURE_2D);

        try {
            mc.getTextureManager().bindTexture(TEXTURE);
        } catch (Exception e) {
            FMLLog.severe("[voiddim] Falha ao carregar a textura do skybox (" + TEXTURE + "): " + e);
        }

        float angle = ((float) world.getWorldTime() + partialTicks) * ROTATION_SPEED;
        angle %= 360.0F;
        GL11.glRotatef(angle, 0.0F, 1.0F, 0.0F);

        float radius = computeSafeRadius(mc);
        if (displayList == -1 || Math.abs(radius - builtRadius) > 0.01F) {
            buildSphereDisplayList(radius);
        }
        GL11.glCallList(displayList);

        GL11.glDepthMask(true);

        // Restaura só o que mudamos.
        if (fogWasEnabled)   GL11.glEnable(GL11.GL_FOG);
        if (blendWasEnabled) GL11.glEnable(GL11.GL_BLEND);
        if (alphaWasEnabled) GL11.glEnable(GL11.GL_ALPHA_TEST);

        GL11.glPopMatrix();
    }

    private float computeSafeRadius(Minecraft mc) {
        int renderDistanceChunks = mc.gameSettings.renderDistanceChunks;
        float radius = (renderDistanceChunks + 1) * 16.0F * 1.414F;
        radius *= 0.7F;
        if (radius > MAX_RADIUS) radius = MAX_RADIUS;
        if (radius < MIN_RADIUS) radius = MIN_RADIUS;
        return radius;
    }

    private void buildSphereDisplayList(float radius) {
        if (displayList == -1) displayList = GL11.glGenLists(1);
        builtRadius = radius;
        GL11.glNewList(displayList, GL11.GL_COMPILE);
        for (int stack = 0; stack < STACKS; stack++) {
            double lat1 = Math.PI * (-0.5 + (double) stack       / STACKS);
            double lat2 = Math.PI * (-0.5 + (double) (stack + 1) / STACKS);
            double sinLat1 = Math.sin(lat1), cosLat1 = Math.cos(lat1);
            double sinLat2 = Math.sin(lat2), cosLat2 = Math.cos(lat2);
            float v1 = 1.0F - (float) stack       / STACKS;
            float v2 = 1.0F - (float) (stack + 1) / STACKS;
            GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
            for (int slice = 0; slice <= SLICES; slice++) {
                double lon = Math.PI * 2 * slice / SLICES;
                double cosLon = Math.cos(lon);
                double sinLon = Math.sin(lon);
                float u = (float) slice / SLICES;
                GL11.glTexCoord2f(u, v1);
                GL11.glVertex3d(cosLon * cosLat1 * radius, sinLat1 * radius, sinLon * cosLat1 * radius);
                GL11.glTexCoord2f(u, v2);
                GL11.glVertex3d(cosLon * cosLat2 * radius, sinLat2 * radius, sinLon * cosLat2 * radius);
            }
            GL11.glEnd();
        }
        GL11.glEndList();
    }
}
