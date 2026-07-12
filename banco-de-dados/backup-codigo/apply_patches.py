#!/usr/bin/env python3
"""
Aplica patches idempotentes aos stubs auto-gerados.
Deve rodar SEMPRE depois de gen_stubs.py.

Estratégia: pra classes que existem, lê o arquivo, extrai o corpo,
adiciona os membros faltantes e reescreve. Pra classes novas, cria do zero.
"""

import os
import re

STUBS = "stubs"

def ensure_dir(path):
    os.makedirs(os.path.dirname(path), exist_ok=True)

def write_full(path, content):
    full = os.path.join(STUBS, path)
    ensure_dir(full)
    with open(full, "w", encoding="utf-8") as f:
        f.write(content)

def _method_sig(java_line):
    """Extrai assinatura simples para dedup (nome + tipos de param, ignora nomes)."""
    m = re.search(r"\b(\w+)\s*\(([^)]*)\)", java_line)
    if not m:
        return None
    name = m.group(1)
    params = m.group(2)
    # extrai tipos (primeira "palavra" de cada param, mas trata `net.minecraft.X.Y`)
    types = []
    for p in [x.strip() for x in params.split(",") if x.strip()]:
        # último token = nome do var; o resto = tipo
        toks = p.split()
        if len(toks) >= 2:
            types.append(" ".join(toks[:-1]))
        else:
            types.append(p)
    return (name, tuple(types))

def patch_class(path, class_pattern_end, additions_lines,
                package=None, class_decl=None, extends=None):
    """
    Se o arquivo existe, injeta additions_lines antes do último `}`.
    Se não existe, cria com header padrão.
    additions_lines: lista de linhas Java, cada uma indentada com 4 espaços já.
    Dedup por assinatura de método/constructor.
    """
    full = os.path.join(STUBS, path)
    if os.path.exists(full):
        with open(full, "r", encoding="utf-8") as f:
            src = f.read()
        existing_sigs = set()
        for line in src.split("\n"):
            sig = _method_sig(line)
            if sig:
                existing_sigs.add(sig)
        to_add = []
        for ln in additions_lines:
            if not ln.strip():
                continue
            sig = _method_sig(ln)
            if sig and sig in existing_sigs:
                continue
            if ln.strip() in src:
                continue
            to_add.append(ln)
            if sig:
                existing_sigs.add(sig)
        if not to_add:
            return
        # find last '}' and inject before it
        idx = src.rfind("}")
        if idx == -1:
            return
        injected = "\n".join(to_add) + "\n"
        new_src = src[:idx] + injected + src[idx:]
        with open(full, "w", encoding="utf-8") as f:
            f.write(new_src)
        print("patched", path, "(+%d lines)" % len(to_add))
    else:
        ensure_dir(full)
        header = "package %s;\n\n@SuppressWarnings({\"all\"})\npublic class %s%s {\n" % (
            package, class_decl, (" extends " + extends) if extends else ""
        )
        body = "\n".join(additions_lines) + "\n}\n"
        with open(full, "w", encoding="utf-8") as f:
            f.write(header + body)
        print("created", path)

# -------- GL11 --------
# Overwrite completamente pra ter certeza que tem tudo.
write_full("org/lwjgl/opengl/GL11.java", """package org.lwjgl.opengl;

/** PATCHED STUB. */
@SuppressWarnings({"all"})
public class GL11 {
    public GL11() {}
    public static void glBegin(int p0) { }
    public static void glCallList(int p0) { }
    public static void glColor4f(float p0, float p1, float p2, float p3) { }
    public static void glColor3f(float p0, float p1, float p2) { }
    public static void glDepthMask(boolean p0) { }
    public static void glDisable(int p0) { }
    public static void glEnable(int p0) { }
    public static void glEnd() { }
    public static void glEndList() { }
    public static int glGenLists(int p0) { return 0; }
    public static void glNewList(int p0, int p1) { }
    public static void glNormal3d(double p0, double p1, double p2) { }
    public static void glPopAttrib() { }
    public static void glPopMatrix() { }
    public static void glPushAttrib(int p0) { }
    public static void glPushMatrix() { }
    public static void glRotatef(float p0, float p1, float p2, float p3) { }
    public static void glTexCoord2f(float p0, float p1) { }
    public static void glTranslatef(float p0, float p1, float p2) { }
    public static void glTranslated(double p0, double p1, double p2) { }
    public static void glVertex3d(double p0, double p1, double p2) { }
    public static void glBlendFunc(int p0, int p1) { }
    public static void glShadeModel(int p0) { }
    public static void glDepthFunc(int p0) { }
    public static void glAlphaFunc(int p0, float p1) { }
    public static void glLineWidth(float p0) { }
    public static void glScalef(float p0, float p1, float p2) { }
    public static void glFogi(int p0, int p1) { }
    public static void glFogf(int p0, float p1) { }
    public static void glHint(int p0, int p1) { }
    public static void glMatrixMode(int p0) { }
    public static void glLoadIdentity() { }
    public static boolean glIsEnabled(int cap) { return false; }

    public static final int GL_CULL_FACE = 2884;
    public static final int GL_TEXTURE_2D = 3553;
    public static final int GL_LIGHTING = 2896;
    public static final int GL_BLEND = 3042;
    public static final int GL_ALPHA_TEST = 3008;
    public static final int GL_DEPTH_TEST = 2929;
    public static final int GL_FOG = 2912;
    public static final int GL_COMPILE = 4864;
    public static final int GL_TRIANGLE_STRIP = 5;
    public static final int GL_TRIANGLES = 4;
    public static final int GL_QUADS = 7;
    public static final int GL_LINES = 1;
    public static final int GL_LINE_LOOP = 2;
    public static final int GL_SRC_ALPHA = 770;
    public static final int GL_ONE_MINUS_SRC_ALPHA = 771;
    public static final int GL_ONE = 1;
    public static final int GL_ZERO = 0;
    public static final int GL_LEQUAL = 515;
    public static final int GL_LESS = 513;
    public static final int GL_GREATER = 516;
    public static final int GL_GEQUAL = 518;
    public static final int GL_SMOOTH = 7425;
    public static final int GL_FLAT = 7424;
    public static final int GL_FOG_MODE = 2917;
    public static final int GL_FOG_DENSITY = 2914;
    public static final int GL_FOG_START = 2915;
    public static final int GL_FOG_END = 2916;
    public static final int GL_EXP = 2048;
    public static final int GL_EXP2 = 2049;
    public static final int GL_LINEAR = 9729;
    public static final int GL_ENABLE_BIT = 8192;
    public static final int GL_COLOR_BUFFER_BIT = 16384;
    public static final int GL_DEPTH_BUFFER_BIT = 256;
    public static final int GL_CURRENT_BIT = 1;
    public static final int GL_FOG_BIT = 128;
    public static final int GL_TRANSFORM_BIT = 4096;
    public static final int GL_ALL_ATTRIB_BITS = 1048575;
    public static final int GL_MODELVIEW = 5888;
    public static final int GL_PROJECTION = 5889;
}
""")

# -------- Tessellator: patch (add missing) --------
patch_class("net/minecraft/client/renderer/Tessellator.java",
    None,
    [
        "    public void func_78377_a(double p0, double p1, double p2) { }",
        "    public void func_78369_a(float p0, float p1, float p2, float p3) { }",
        "    public void func_78370_a(int p0, int p1, int p2, int p3) { }",
        "    public void func_78384_a(int p0, int p1) { }",
        "    public void func_78371_b(int p0) { }",
    ],
    package="net.minecraft.client.renderer", class_decl="Tessellator")

# -------- SubscribeEvent / EventPriority / Event / EventBus --------
write_full("cpw/mods/fml/common/eventhandler/SubscribeEvent.java",
"""package cpw.mods.fml.common.eventhandler;
import java.lang.annotation.*;
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SubscribeEvent {
    EventPriority priority() default EventPriority.NORMAL;
    boolean receiveCanceled() default false;
}
""")
write_full("cpw/mods/fml/common/eventhandler/EventPriority.java",
"""package cpw.mods.fml.common.eventhandler;
public enum EventPriority { HIGHEST, HIGH, NORMAL, LOW, LOWEST }
""")
write_full("cpw/mods/fml/common/eventhandler/Event.java",
"""package cpw.mods.fml.common.eventhandler;
@SuppressWarnings({"all"})
public class Event {
    public Event() {}
    public boolean isCanceled() { return false; }
    public void setCanceled(boolean c) {}
}
""")
# EventBus provavelmente já existe do gen_stubs.
patch_class("cpw/mods/fml/common/eventhandler/EventBus.java", None,
    [
        "    public void register(Object o) {}",
        "    public boolean post(cpw.mods.fml.common.eventhandler.Event e) { return false; }",
    ],
    package="cpw.mods.fml.common.eventhandler", class_decl="EventBus")

# -------- Entity: add pos/rot fields --------
patch_class("net/minecraft/entity/Entity.java", None,
    [
        "    public net.minecraft.world.World field_70170_p;",
        "    public double field_70165_t;",
        "    public double field_70163_u;",
        "    public double field_70161_v;",
        "    public double field_70142_S;",
        "    public double field_70137_T;",
        "    public double field_70136_U;",
        "    public float field_70177_z;",
        "    public float field_70125_A;",
        "    public float field_70126_B;",
        "    public float field_70127_C;",
    ],
    package="net.minecraft.entity", class_decl="Entity")

# -------- EntityLivingBase: já existe, garante que herda Entity --------
# Se o auto-gen não deu herança correta, patch: forçamos escrever inteiro.
elb_path = os.path.join(STUBS, "net/minecraft/entity/EntityLivingBase.java")
if os.path.exists(elb_path):
    with open(elb_path) as f:
        elb_src = f.read()
    if "extends net.minecraft.entity.Entity" not in elb_src:
        # regenerate
        write_full("net/minecraft/entity/EntityLivingBase.java",
"""package net.minecraft.entity;
@SuppressWarnings({"all"})
public class EntityLivingBase extends net.minecraft.entity.Entity {
    public EntityLivingBase() {}
}
""")

# -------- EntityPlayer: garante que herda EntityLivingBase --------
epl_path = os.path.join(STUBS, "net/minecraft/entity/player/EntityPlayer.java")
if os.path.exists(epl_path):
    with open(epl_path) as f:
        epl_src = f.read()
    if "extends net.minecraft.entity.EntityLivingBase" not in epl_src:
        # Extrai membros originais e reescreve com extends correto
        # (simplifica: só garante header)
        # Injeta a herança
        new = re.sub(
            r"public class EntityPlayer\b[^{]*\{",
            "public class EntityPlayer extends net.minecraft.entity.EntityLivingBase {",
            epl_src
        )
        with open(epl_path, "w") as f:
            f.write(new)
        print("fixed EntityPlayer inheritance")

# -------- EntityClientPlayerMP --------
write_full("net/minecraft/client/entity/EntityClientPlayerMP.java",
"""package net.minecraft.client.entity;
@SuppressWarnings({"all"})
public class EntityClientPlayerMP extends net.minecraft.entity.player.EntityPlayer {
    public EntityClientPlayerMP() {}
}
""")

# -------- WorldClient --------
patch_class("net/minecraft/client/multiplayer/WorldClient.java", None,
    ["    public long func_82737_E() { return 0L; }"],
    package="net.minecraft.client.multiplayer", class_decl="WorldClient",
    extends="net.minecraft.world.World")

# Garante extends World mesmo se já existia
wc_path = os.path.join(STUBS, "net/minecraft/client/multiplayer/WorldClient.java")
if os.path.exists(wc_path):
    with open(wc_path) as f: s = f.read()
    if "extends net.minecraft.world.World" not in s:
        s = re.sub(r"public class WorldClient\b[^{]*\{",
                   "public class WorldClient extends net.minecraft.world.World {", s)
        with open(wc_path, "w") as f: f.write(s)
        print("fixed WorldClient inheritance")

# -------- Minecraft: garantir field_71439_g e field_71441_e, func_71410_x --------
mc_path = os.path.join(STUBS, "net/minecraft/client/Minecraft.java")
if os.path.exists(mc_path):
    with open(mc_path) as f: s = f.read()
    additions = []
    if "field_71439_g" not in s:
        additions.append("    public net.minecraft.client.entity.EntityClientPlayerMP field_71439_g;")
    if "field_71441_e" not in s:
        additions.append("    public net.minecraft.client.multiplayer.WorldClient field_71441_e;")
    if "func_71410_x" not in s:
        additions.append("    public static net.minecraft.client.Minecraft func_71410_x() { return null; }")
    if additions:
        idx = s.rfind("}")
        s = s[:idx] + "\n".join(additions) + "\n" + s[idx:]
        with open(mc_path, "w") as f: f.write(s)
        print("patched Minecraft (+%d)" % len(additions))
else:
    write_full("net/minecraft/client/Minecraft.java",
"""package net.minecraft.client;
@SuppressWarnings({"all"})
public class Minecraft {
    public net.minecraft.client.entity.EntityClientPlayerMP field_71439_g;
    public net.minecraft.client.multiplayer.WorldClient field_71441_e;
    public net.minecraft.client.settings.GameSettings field_71474_y;
    public Minecraft() {}
    public static Minecraft func_71410_x() { return null; }
    public net.minecraft.client.renderer.texture.TextureManager func_110434_K() { return null; }
}
""")

# -------- TextureManager: func_110577_a --------
patch_class("net/minecraft/client/renderer/texture/TextureManager.java", None,
    ["    public void func_110577_a(net.minecraft.util.ResourceLocation loc) {}"],
    package="net.minecraft.client.renderer.texture", class_decl="TextureManager")

# -------- ResourceLocation 2-arg ctor --------
rl_path = os.path.join(STUBS, "net/minecraft/util/ResourceLocation.java")
if os.path.exists(rl_path):
    with open(rl_path) as f: s = f.read()
    if "ResourceLocation(String" not in s or "String domain, String path" not in s:
        # Add ctor
        idx = s.rfind("}")
        add = "    public ResourceLocation(String domain, String path) {}\n"
        if "public ResourceLocation(String" not in s:
            add = "    public ResourceLocation(String path) {}\n" + add
        s = s[:idx] + add + s[idx:]
        with open(rl_path, "w") as f: f.write(s)
        print("patched ResourceLocation")
else:
    write_full("net/minecraft/util/ResourceLocation.java",
"""package net.minecraft.util;
@SuppressWarnings({"all"})
public class ResourceLocation {
    public ResourceLocation() {}
    public ResourceLocation(String path) {}
    public ResourceLocation(String domain, String path) {}
}
""")

# -------- RenderWorldLastEvent --------
write_full("net/minecraftforge/client/event/RenderWorldLastEvent.java",
"""package net.minecraftforge.client.event;
@SuppressWarnings({"all"})
public class RenderWorldLastEvent extends cpw.mods.fml.common.eventhandler.Event {
    public float partialTicks;
    public RenderWorldLastEvent() {}
}
""")

# -------- EntityViewRenderEvent --------
write_full("net/minecraftforge/client/event/EntityViewRenderEvent.java",
"""package net.minecraftforge.client.event;
@SuppressWarnings({"all"})
public class EntityViewRenderEvent extends cpw.mods.fml.common.eventhandler.Event {
    public net.minecraft.entity.EntityLivingBase entity;
    public float renderPartialTicks;
    public EntityViewRenderEvent() {}
    public static class FogDensity extends EntityViewRenderEvent {
        public float density;
        public FogDensity() {}
    }
    public static class FogColors extends EntityViewRenderEvent {
        public float red, green, blue;
        public FogColors() {}
    }
    public static class CameraSetup extends EntityViewRenderEvent {
        public float roll, yaw, pitch;
        public CameraSetup() {}
    }
}
""")

# -------- FMLLog --------
write_full("cpw/mods/fml/common/FMLLog.java",
"""package cpw.mods.fml.common;
@SuppressWarnings({"all"})
public class FMLLog {
    public FMLLog() {}
    public static void log(String level, String fmt, Object... args) {}
    public static void log(String level, Throwable t, String fmt, Object... args) {}
    public static void info(String fmt, Object... args) {}
    public static void warning(String fmt, Object... args) {}
    public static void severe(String fmt, Object... args) {}
}
""")

# -------- MinecraftForge --------
write_full("net/minecraftforge/common/MinecraftForge.java",
"""package net.minecraftforge.common;
@SuppressWarnings({"all"})
public class MinecraftForge {
    public static cpw.mods.fml.common.eventhandler.EventBus EVENT_BUS =
        new cpw.mods.fml.common.eventhandler.EventBus();
    public static cpw.mods.fml.common.eventhandler.EventBus TERRAIN_GEN_BUS =
        new cpw.mods.fml.common.eventhandler.EventBus();
}
""")

# -------- World: garantir func_147439_a, func_72869_a, func_149677_c helpers --------
patch_class("net/minecraft/world/World.java", None,
    [
        "    public net.minecraft.block.Block func_147439_a(int x, int y, int z) { return null; }",
        "    public void func_72869_a(String name, double x, double y, double z, double vx, double vy, double vz) {}",
        "    public int func_72805_g(int x, int y, int z) { return 0; }",
    ],
    package="net.minecraft.world", class_decl="World")

# -------- Block: getIcon, getMixedBrightnessForBlock --------
patch_class("net/minecraft/block/Block.java", None,
    [
        "    public net.minecraft.util.IIcon func_149691_a(int side, int meta) { return null; }",
        "    public int func_149677_c(net.minecraft.world.IBlockAccess world, int x, int y, int z) { return 0; }",
    ],
    package="net.minecraft.block", class_decl="Block")

# -------- WorldProvider: enriquecer stub com SRG names (v33 fix AbstractMethodError) --------
patch_class("net/minecraft/world/WorldProvider.java", None,
    [
        "    public net.minecraft.world.biome.WorldChunkManager worldChunkMgr;",
        "    public net.minecraft.world.World worldObj;",
        "    public boolean hasNoSky;",
        "    public int dimensionId;",
        "    public float[] lightBrightnessTable = new float[16];",
        "    protected void func_76572_b() {}",  # registerWorldChunkManager
        "    public net.minecraft.world.chunk.IChunkProvider func_76555_c() { return null; }",  # createChunkGenerator
        "    public String func_80007_l() { return \"\"; }",  # getDimensionName (abstract)
        "    public boolean func_76567_e() { return true; }",  # canRespawnHere
        "    public boolean func_76569_d() { return true; }",  # isSurfaceWorld
        "    public float[] func_76560_a(float celestialAngle, float partialTicks) { return null; }",  # calcSunriseSunsetColors
        "    protected void func_76556_a() {}",  # generateLightBrightnessTable
        "    public double func_72919_O() { return 64.0; }",  # getHorizon
        "    public net.minecraft.util.Vec3 func_76562_b(float ca, float pt) { return null; }",  # getFogColor
        "    public float func_76571_f() { return 128.0F; }",  # getCloudHeight
    ],
    package="net.minecraft.world", class_decl="WorldProvider")

# -------- Vec3: createVectorHelper --------
patch_class("net/minecraft/util/Vec3.java", None,
    [
        "    public static net.minecraft.util.Vec3 createVectorHelper(double x, double y, double z) { return null; }",
    ],
    package="net.minecraft.util", class_decl="Vec3")

# -------- WorldChunkManager: constructor com World --------
patch_class("net/minecraft/world/biome/WorldChunkManager.java", None,
    [
        "    public WorldChunkManager(net.minecraft.world.World w) {}",
    ],
    package="net.minecraft.world.biome", class_decl="WorldChunkManager")

# -------- BiomeGenBase.plains: field usado pro biome default do voiddim --------
patch_class("net/minecraft/world/biome/BiomeGenBase.java", None,
    [
        "    public static net.minecraft.world.biome.BiomeGenBase plains = new net.minecraft.world.biome.BiomeGenBase();",
        "    public int biomeID = 1;",
    ],
    package="net.minecraft.world.biome", class_decl="BiomeGenBase")

# -------- EnumCreatureType (usado por ChunkProviderVoidDim) --------
write_full("net/minecraft/entity/EnumCreatureType.java",
"""package net.minecraft.entity;
@SuppressWarnings({"all"})
public class EnumCreatureType {}
""")

# -------- IProgressUpdate --------
write_full("net/minecraft/util/IProgressUpdate.java",
"""package net.minecraft.util;
@SuppressWarnings({"all"})
public interface IProgressUpdate {}
""")

# -------- ChunkPosition --------
write_full("net/minecraft/world/ChunkPosition.java",
"""package net.minecraft.world;
@SuppressWarnings({"all"})
public class ChunkPosition {
    public ChunkPosition() {}
}
""")

print("apply_patches.py done.")
