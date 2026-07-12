package com.voiddim.client;

import org.lwjgl.opengl.GL11;

import com.voiddim.dimension.WorldProviderVoidDim;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderPlayerEvent;

/**
 * Desenha um halo torus dourado acima da cabeça do jogador,
 * ativo apenas na Dimensão do Vazio.
 *
 * Otimização: usa display list construído uma vez.
 * Compat 1.7.10: registrado via RenderPlayerEvent.Specials.Post no ClientProxy.
 */
public class HaloRenderHandler {

    private static final float RING_RADIUS       = 0.32F;
    private static final float TUBE_RADIUS       = 0.045F;
    private static final int   RING_SEGMENTS     = 24;
    private static final int   TUBE_SEGMENTS     = 10;
    private static final float HEIGHT_ABOVE_HEAD = 0.85F;
    private static final float[] HALO_COLOR      = { 1.0F, 0.9F, 0.5F };

    private int displayList = -1;

    @SubscribeEvent
    public void onRenderPlayerSpecialsPost(RenderPlayerEvent.Specials.Post event) {
        EntityPlayer player = event.entityPlayer;
        if (player.worldObj == null || !(player.worldObj.provider instanceof WorldProviderVoidDim)) return;
        if (player.isInvisible()) return;

        final RenderPlayer renderer = event.renderer;
        GL11.glPushMatrix();

        // Alinha o halo com o modelo da cabeça (respeitando animação).
        renderer.modelBipedMain.bipedHead.postRender(0.0625F);
        GL11.glTranslatef(0.0F, -HEIGHT_ABOVE_HEAD, 0.0F);

        // Salva SOMENTE os states que vamos mudar (mais barato que glPushAttrib(ALL)).
        final boolean texWasEnabled     = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        final boolean lightWasEnabled   = GL11.glIsEnabled(GL11.GL_LIGHTING);
        final boolean blendWasEnabled   = GL11.glIsEnabled(GL11.GL_BLEND);

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDepthMask(true);

        if (displayList == -1) buildTorusDisplayList();

        GL11.glColor4f(HALO_COLOR[0], HALO_COLOR[1], HALO_COLOR[2], 1.0F);
        GL11.glCallList(displayList);

        // Restaura só o que mudamos.
        if (texWasEnabled)   GL11.glEnable(GL11.GL_TEXTURE_2D);
        if (lightWasEnabled) GL11.glEnable(GL11.GL_LIGHTING);
        if (blendWasEnabled) GL11.glEnable(GL11.GL_BLEND);
        GL11.glColor4f(1F, 1F, 1F, 1F);

        GL11.glPopMatrix();
    }

    private void buildTorusDisplayList() {
        displayList = GL11.glGenLists(1);
        GL11.glNewList(displayList, GL11.GL_COMPILE);
        for (int i = 0; i < RING_SEGMENTS; i++) {
            double theta1 = Math.PI * 2 * i       / RING_SEGMENTS;
            double theta2 = Math.PI * 2 * (i + 1) / RING_SEGMENTS;
            GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
            for (int j = 0; j <= TUBE_SEGMENTS; j++) {
                double phi = Math.PI * 2 * j / TUBE_SEGMENTS;
                emitTorusVertex(theta1, phi);
                emitTorusVertex(theta2, phi);
            }
            GL11.glEnd();
        }
        GL11.glEndList();
    }

    private void emitTorusVertex(double theta, double phi) {
        double cosTheta = Math.cos(theta), sinTheta = Math.sin(theta);
        double cosPhi   = Math.cos(phi),   sinPhi   = Math.sin(phi);
        double cx = RING_RADIUS * cosTheta;
        double cz = RING_RADIUS * sinTheta;
        double nx = cosPhi * cosTheta;
        double ny = sinPhi;
        double nz = cosPhi * sinTheta;
        double vx = cx + TUBE_RADIUS * nx;
        double vy =      TUBE_RADIUS * ny;
        double vz = cz + TUBE_RADIUS * nz;
        GL11.glNormal3d(nx, ny, nz);
        GL11.glVertex3d(vx, vy, vz);
    }
}
