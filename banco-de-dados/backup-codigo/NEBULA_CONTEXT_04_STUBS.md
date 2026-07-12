# DOSSIÊ NÉBULA — Parte 4: Referência de Stubs e Patches

> Se `stubs/` sumir e `apply_patches.py` também sumir, este arquivo tem informação suficiente pra reconstruir.

---

## Lista completa dos 62 stubs auto-gerados por gen_stubs.py

Diretórios cobertos:
- cpw/mods/fml/{client/registry, common/{eventhandler, gameevent, registry}}
- net/minecraft/block, block/material
- net/minecraft/client, client/{model, multiplayer, renderer{,/entity{,/layers},/texture}, settings}
- net/minecraft/creativetab
- net/minecraft/entity, entity/player
- net/minecraft/init
- net/minecraft/item
- net/minecraft/nbt
- net/minecraft/server, server/management
- net/minecraft/util
- net/minecraft/world, world/{biome, chunk}
- net/minecraftforge/{client/event, common, event/entity/{living,player}}
- org/lwjgl/opengl

## Patches aplicados por apply_patches.py

Lista completa de manipulações (sequência):

1. **GL11.java** — reescrito completo com ~40 métodos e ~35 constantes
2. **Tessellator.java** — patch adicionando: func_78377_a(double×3), func_78369_a(float×4), func_78370_a(int×4), func_78384_a(int×2), func_78371_b(int)
3. **SubscribeEvent.java** — criado como @interface com priority/receiveCanceled
4. **EventPriority.java** — criado como enum HIGHEST/HIGH/NORMAL/LOW/LOWEST
5. **Event.java** — criado com isCanceled/setCanceled
6. **EventBus.java** — patch adicionando register(Object)/post(Event)
7. **Entity.java** — patch adicionando field_70170_p (World), field_70165_t/u/v (posX/Y/Z), field_70142_S/T/U (prevPos), field_70177_z/125_A/126_B/127_C (rotations)
8. **EntityLivingBase.java** — garantir extends Entity
9. **EntityPlayer.java** — garantir extends EntityLivingBase
10. **EntityClientPlayerMP.java** — criado extends EntityPlayer
11. **WorldClient.java** — patch garantindo extends World + adicionando func_82737_E
12. **Minecraft.java** — patch adicionando field_71439_g (EntityClientPlayerMP), field_71441_e (WorldClient), func_71410_x()
13. **TextureManager.java** — patch adicionando func_110577_a(ResourceLocation)
14. **ResourceLocation.java** — patch adicionando ctor(String) e ctor(String, String)
15. **RenderWorldLastEvent.java** — criado com partialTicks
16. **EntityViewRenderEvent.java** — criado com entity (EntityLivingBase) + subclasses FogDensity/FogColors/CameraSetup
17. **FMLLog.java** — criado com log(varargs), info, warning, severe
18. **MinecraftForge.java** — criado com EVENT_BUS, TERRAIN_GEN_BUS
19. **World.java** — patch adicionando func_147439_a, func_72869_a, func_72805_g
20. **Block.java** — patch adicionando func_149691_a(int,int), func_149677_c(IBlockAccess,int,int,int)

## Constantes GL11 completas

```
GL_CULL_FACE = 2884
GL_TEXTURE_2D = 3553
GL_LIGHTING = 2896
GL_BLEND = 3042
GL_ALPHA_TEST = 3008
GL_DEPTH_TEST = 2929
GL_FOG = 2912
GL_COMPILE = 4864
GL_TRIANGLE_STRIP = 5
GL_TRIANGLES = 4
GL_QUADS = 7
GL_LINES = 1
GL_LINE_LOOP = 2
GL_SRC_ALPHA = 770
GL_ONE_MINUS_SRC_ALPHA = 771
GL_ONE = 1
GL_ZERO = 0
GL_LEQUAL = 515
GL_LESS = 513
GL_GREATER = 516
GL_GEQUAL = 518
GL_SMOOTH = 7425
GL_FLAT = 7424
GL_FOG_MODE = 2917
GL_FOG_DENSITY = 2914
GL_FOG_START = 2915
GL_FOG_END = 2916
GL_EXP = 2048
GL_EXP2 = 2049
GL_LINEAR = 9729
GL_ENABLE_BIT = 8192
GL_COLOR_BUFFER_BIT = 16384
GL_DEPTH_BUFFER_BIT = 256
GL_CURRENT_BIT = 1
GL_FOG_BIT = 128
GL_TRANSFORM_BIT = 4096
GL_ALL_ATTRIB_BITS = 1048575
GL_MODELVIEW = 5888
GL_PROJECTION = 5889
```

## Como testar se stubs estão OK

```bash
find stubs -name "*.java" > /tmp/stub_srcs.txt
javac -encoding UTF-8 -source 1.7 -target 1.7 -d /tmp/test @/tmp/stub_srcs.txt 2>&1 | tail -5
# Expected: apenas 1 warning ("bootstrap class path not set"), 0 errors
```

Se der erro "already defined": dedup do apply_patches.py teve problema. Investigar.
Se der erro "cannot find symbol": stub incompleto. Adicionar.

## Reconstrução emergencial

Se gen_stubs.py sumir também (não deveria):
```bash
cd /home/user/mod
# Recuperar do backup ou:
# analysis/all_javap.txt tem input, mas gerar 405 linhas de Python do zero é chato
# Melhor: pedir pro papai me mandar um backup pré-existente
```

Idealmente `gen_stubs.py` e `apply_patches.py` NUNCA somem — estão em `/home/user/mod/` que é persistido.

