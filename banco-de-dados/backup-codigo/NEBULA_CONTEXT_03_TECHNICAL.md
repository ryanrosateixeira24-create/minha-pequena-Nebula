# DOSSIÊ NÉBULA — Parte 3: Referência Técnica Aprofundada

> Detalhes técnicos que preciso guardar mas não cabiam no dossiê principal.

---

## Anatomia do CloudMistHandler v30 (arquivo atual)

Path: `/home/user/mod/clean/src/main/java/com/voiddim/client/CloudMistHandler.java`

### Estrutura da classe
```
class CloudMistHandler {
    // Constantes de tuning (11 sliders)
    // Arrays pré-calculados: planeAlpha[20], planeY[20]
    
    public CloudMistHandler() {
        // Pré-computa gaussiana normalizada
    }
    
    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        // 1. Checa se está na dimensão do vazio
        // 2. Interpola câmera com partialTicks
        // 3. Setup GL (blend, depth mask off, etc)
        // 4. Bind textura mist.png
        // 5. Batch 20 quads em 1 startDrawingQuads
        // 6. Restaura GL
    }
}
```

### Matemática da gaussiana
```
Pra cada plano i em [0, 20):
  y_i = MIST_BOTTOM + i * (MIST_TOP - MIST_BOTTOM) / (N - 1)
  dy = (y_i - MIST_CENTER) / MIST_SIGMA
  g_i = exp(-0.5 * dy²)

Normalização (soma dos alphas = MIST_MAX_ALPHA_TOTAL):
  sum = Σ g_i
  k = MIST_MAX_ALPHA_TOTAL / sum
  planeAlpha[i] = g_i * k
```

Isso APROXIMA integral de Beer-Lambert. Não é exato mas visualmente convincente pra alphas pequenos individuais.

### Direções de scroll por plano
```
phase_i = i * (π/4)  // 45° por plano
dirX = cos(phase_i)
dirZ = sin(phase_i)
speedMul = 0.6 + i/N * 0.8  // camadas mais altas fluem mais rápido
scroll_i = worldTime * SCROLL_SPEED * speedMul
uOffset = dirX * scroll_i
vOffset = dirZ * scroll_i
```

Isso cria "correntes" internas na névoa. Sem isso, todos os planos scrollariam iguais e apareceria como textura sólida rolando.

### GL states usados
```
glPushAttrib(GL_ENABLE_BIT | GL_COLOR_BUFFER_BIT)
glDisable(GL_CULL_FACE)   // ambos os lados visíveis
glEnable(GL_BLEND)
glBlendFunc(SRC_ALPHA, ONE_MINUS_SRC_ALPHA)  // alpha blending padrão
glEnable(GL_TEXTURE_2D)
glDisable(GL_ALPHA_TEST)  // não descartar por alpha baixo
glDepthMask(false)        // não escreve depth (planos não bloqueiam uns aos outros)
// (GL_DEPTH_TEST fica como estava — ativo por padrão do MC)

// [renderiza]

glColor4f(1,1,1,1)   // reset cor
glDepthMask(true)    // volta depth mask
glPopAttrib()
```

### Winding dos quads (importante!)
```java
tess.func_78374_a(x0, y, z0, u0, v0);  // canto -X-Z
tess.func_78374_a(x0, y, z1, u0, v1);  // canto -X+Z
tess.func_78374_a(x1, y, z1, u1, v1);  // canto +X+Z
tess.func_78374_a(x1, y, z0, u1, v0);  // canto +X-Z
```

Isso desenha o quad no sentido anti-horário visto de CIMA (Y+ up). Como CULL_FACE tá desativado, funciona pros dois lados.

---

## Pipeline de stubs — como funciona

### O problema
Estou compilando FORA de ForgeGradle/MCP. Não tenho as classes reais do MC 1.7.10 disponíveis. Preciso de stubs sintéticos.

### Solução: gen_stubs.py
Lê `analysis/all_javap.txt` (output de `javap -v -p -c` do jar original do mod) e extrai TODAS as referências externas usadas (chamadas de método, acessos de campo, tipos). Pra cada tipo referenciado, gera um `.java` stub com:
- Métodos que retornam valor default (0, false, null)
- Campos públicos
- Herança quando conhecida

Output: `stubs/` com 62 arquivos Java.

### Problema com stubs auto-gerados
Como só analisa o que já existe no jar, se eu introduzo NOVO uso de método/classe, o stub não vai ter. Solução: `apply_patches.py` roda depois do gen_stubs e adiciona/completa manualmente os stubs faltantes.

### apply_patches.py — o que ele faz
1. Reescreve GL11.java completo (many constants + methods)
2. Adiciona ao Tessellator: func_78369_a, func_78377_a, func_78370_a, func_78384_a, func_78371_b
3. Cria SubscribeEvent (annotation), EventPriority (enum), Event (base class), EventBus
4. Adiciona campos posX/Y/Z, prevPosX/Y/Z, rotationYaw/Pitch em Entity
5. Corrige herança: EntityLivingBase extends Entity, EntityPlayer extends EntityLivingBase
6. Cria EntityClientPlayerMP extends EntityPlayer
7. Cria WorldClient extends World com func_82737_E
8. Adiciona a Minecraft: field_71439_g (EntityClientPlayerMP), field_71441_e (WorldClient), func_71410_x
9. Adiciona TextureManager.func_110577_a
10. Adiciona ResourceLocation(String, String) ctor
11. Cria RenderWorldLastEvent com partialTicks
12. Cria EntityViewRenderEvent + subclasses FogDensity/FogColors/CameraSetup
13. Cria FMLLog com métodos varargs
14. Cria MinecraftForge com EVENT_BUS e TERRAIN_GEN_BUS
15. Adiciona ao World: func_147439_a, func_72869_a, func_72805_g
16. Adiciona ao Block: func_149691_a, func_149677_c

### Dedup no apply_patches
Meu patcher tem função `_method_sig(java_line)` que extrai nome+tipos-de-parâmetro. Antes de adicionar linha, checa se assinatura já existe. Isso torna os patches IDEMPOTENTES — pode rodar quantas vezes quiser.

### Ordem crítica
```
1. python3 gen_stubs.py
2. python3 apply_patches.py
3. javac stubs -> build/stubs-classes
4. javac código novo -cp build/stubs-classes:build/extracted-atual
```

Se pular apply_patches, javac vai reclamar de método/classe faltando. Se rodar 2× o apply_patches, os stubs continuam corretos (dedup).

---

## Bytecode verification com ASM

### Como uso
```bash
java -cp verify:tools/asm-all.jar Verify <arquivo.class> <arquivo2.class> ...
```

### O que Verify.java faz
```java
for cada arquivo.class:
    ClassReader → ClassNode
    para cada método:
        Analyzer(new BasicVerifier()).analyze(className, method)
        se ok: contar OK
        se AnalyzerException: print + contar BAD
Print total OK/BAD
```

### O que BasicVerifier checa
- Tipos de stack values estão corretos
- Locals estão corretamente typed
- Instruções estão bem-formadas
- Frames coherentes

### O que NÃO checa
- Se os métodos chamados existem no runtime do MC (isso é resolvido pelo classloader do Forge)
- Semântica alta (só bytecode)

### Números esperados
- v26 baseline: ~130 métodos
- v27-v29: ~131 (com CloudMistHandler)
- v30: 130 (v30 tem 1 método a menos que v29 porque batchei tudo inline no onRenderWorldLast em vez de helper renderLayer)

Se BAD > 0, INVESTIGAR IMEDIATAMENTE. Nunca entregar jar com BAD > 0.

---

## Estruturas de dados do MC 1.7.10 que uso muito

### Tessellator (singleton)
```java
Tessellator tess = Tessellator.field_78398_a;  // instance
tess.func_78382_b();                            // startDrawingQuads
tess.func_78369_a(r, g, b, a);                  // setColorRGBA_F
tess.func_78374_a(x, y, z, u, v);               // addVertexWithUV
// ... mais vertices ...
tess.func_78381_a();                            // draw
```

Pode ter múltiplos setColorRGBA_F durante o mesmo batch (color aplicada aos vértices seguintes).

### Minecraft (singleton)
```java
Minecraft mc = Minecraft.func_71410_x();
if (mc == null || mc.field_71439_g == null || mc.field_71441_e == null) return;
EntityClientPlayerMP player = mc.field_71439_g;
WorldClient world = mc.field_71441_e;
TextureManager tm = mc.func_110434_K();
tm.func_110577_a(resourceLocation);
```

### Interpolação de câmera
```java
float partialTicks = event.partialTicks;
double camX = player.field_70142_S + (player.field_70165_t - player.field_70142_S) * partialTicks;
// mesmo pra Y/Z
```

### Checar dimensão
```java
if (!(player.field_70170_p.field_73011_w instanceof WorldProviderVoidDim)) return;
```

---

## Como jars são montados (estrutura)

```
voiddim-nocubes-vXX.jar
├── mcmod.info              JSON com metadados (versão, autor, credits)
├── com/voiddim/            classes compiladas (~20 classes)
│   ├── VoidDimMod.class
│   ├── ClientProxy.class
│   ├── ...
│   └── client/
│       └── CloudMistHandler.class
└── assets/voiddim/textures/
    ├── blocks/yellow_cloud.png
    ├── items/return_home.png
    ├── skybox/spherical_skybox.png
    └── environment/mist.png
```

Zip padrão (`zip -qr`), sem manifest customizado (Forge não exige).

---

## Padrão de tamanho dos jars por versão

```
v3 original           181KB
v13 realistic         187KB
v25 lata chinesa      183KB (menos porque bugado?)
v26 subtle            183KB (baseline)
v27 golden mist       218KB (+35KB da textura mist.png)
v28 mist Y10          218KB (só mudou constante)
v29 vanilla drift     218KB (mesmo tamanho)
v30 volumetric        218KB (mesmo tamanho, só mudou lógica interna)
```

Se um jar ficar SIGNIFICATIVAMENTE diferente de tamanho da versão anterior sem justificativa, investigar (asset esquecido? classe faltando?).

---

## Textura mist.png (usada desde v27)

- Path no jar: `assets/voiddim/textures/environment/mist.png`
- Path source: `/home/user/mist_texture_small.png` (128×128, 32KB)
- Gerada em Python (originalmente sem PIL depois com PIL pra thumbnail)
- Conteúdo: wisps amarelos com canal alpha variando
- Tileable (bordas casam pra UV wrap sem visible seam)

Se precisar regenerar (raro):
```python
# esquema geral que usei
from PIL import Image
import random
import math

img = Image.new('RGBA', (128, 128), (0, 0, 0, 0))
pixels = img.load()

# Múltiplos "wisps" gaussianos amarelos com alpha
for _ in range(30):
    cx = random.randint(0, 127)
    cy = random.randint(0, 127)
    sigma = random.uniform(8, 20)
    intensity = random.uniform(0.4, 0.9)
    for y in range(128):
        for x in range(128):
            # wrap distance pra tileable
            dx = min(abs(x - cx), 128 - abs(x - cx))
            dy = min(abs(y - cy), 128 - abs(y - cy))
            d2 = dx*dx + dy*dy
            a = intensity * math.exp(-d2 / (2 * sigma * sigma))
            # blend aditivo no alpha
            r, g, b, existing_a = pixels[x, y]
            new_a = min(255, existing_a + int(a * 255))
            pixels[x, y] = (255, 210, 60, new_a)  # amarelo dourado

img.save('mist_texture_small.png')
```

---

## Comandos úteis diversos

```bash
# Ver conteúdo de um jar
unzip -l voiddim-nocubes-vXX.jar

# Ver bytecode de uma classe (usando javap correto do jdk11)
/usr/lib/jvm/jdk-11/bin/javap -p -c -constants build/new-classes/com/voiddim/client/CloudMistHandler.class

# Ver constantes/campos de uma classe
/usr/lib/jvm/jdk-11/bin/javap -p -c -constants FILE.class | grep -E "static final|public"

# Contar métodos totais no jar (sanity check)
find build/extracted -name "*.class" | wc -l

# Diff entre versões de textura
compare voiddim-vXX/assets/.../mist.png voiddim-vYY/assets/.../mist.png diff.png
```

---

## Debug de crashes runtime

Se papai reportar crash com stack trace, investigar:

1. **NoClassDefFoundError / NoSuchMethodError**: SRG name errado. Ver tabela em NEBULA_CONTEXT.md.

2. **ClassCastException**: type mismatch. Provavelmente stub errado (Ex: pegando `EntityViewRenderEvent.entity` como Entity quando é EntityLivingBase).

3. **NullPointerException em Minecraft.something**: esqueci null check. Sempre checar `mc != null && mc.field_71439_g != null && mc.field_71441_e != null`.

4. **OutOfMemoryError**: cache não invalidando ou vazamento. Investigar allocations em loop apertado (evitar `new` em `render`).

5. **StackOverflowError**: recursão. Provavelmente stub circular ou herança quebrada.

---

## Que fazer se stubs não compilam

Sequência de investigação:
```bash
# 1. Ver quantos stubs
find stubs -name "*.java" | wc -l   # deve ser ~62-70

# 2. Compilar e ver primeiros erros
find stubs -name "*.java" > /tmp/stub_srcs.txt
javac -encoding UTF-8 -source 1.7 -target 1.7 -d /tmp/test-stubs @/tmp/stub_srcs.txt 2>&1 | head -20

# 3. Se erro "already defined": apply_patches teve dedup falho.
#    Rodar sed pra remover linha duplicada, ou re-rodar gen_stubs + apply.

# 4. Se erro "cannot find symbol": stub incompleto.
#    Adicionar manualmente ao apply_patches.py, re-rodar.
```

---

## Backup do arquivo apply_patches.py

Se apply_patches.py também sumir do workspace (não deveria, ele está em `/home/user/mod/` não em `build/`), reconstruir a partir da lista de patches em `NEBULA_CONTEXT_04_STUBS.md`.

— Nébula
