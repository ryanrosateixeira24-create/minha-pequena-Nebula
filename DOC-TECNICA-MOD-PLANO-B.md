# 📘 DOCUMENTAÇÃO TÉCNICA — Mods Ateliê Nébula + Voiddim

> Documentação TÉCNICA dos mods. Sem persona, sem regras, sem carinho.
> É um manual de engenharia pra qualquer IA genérica (ou pessoa técnica)
> poder dar continuidade ao trabalho.
> NÃO é um documento da Nébula. É só sobre os mods.

---

## INFORMAÇÕES BÁSICAS

- **Engine:** Minecraft 1.7.10
- **Mod loader:** Forge 10.13.4.1614
- **Java:** 1.8.0_51 (runtime do player), JDK 11 disponível pra compilação no sandbox
- **Target player:** Windows 10, AMD Ryzen 5 4600G, 16GB RAM, Java 1.8.0_51
- **Pasta mods do player:** `C:\Users\Oliveira\AppData\Roaming\.minecraft\mods\`

---

## MOD 1: ATELIÊ NÉBULA + CAPSULE CORP (merge)

### O que é

Merge de dois mods Minecraft 1.7.10:
- **Ateliê Nébula** — ferramenta de design 3D
- **Capsule Corp** v0.1.80-alpha — mod de cápsulas do Dragon Ball

### Arquivo recomendado (estado da arte)

```
Atelie-Nebula-COM-Capsule-1.7.10-v16-botao-2d-fumaca.jar
- Tamanho: 1.678.269 bytes
- SHA256: ae1aadd626bd9f8dcd750a0a44ca2bdaec7573a503e50604f590c7b49097108f
```

Caminho: `/home/user/minha-pequena-Nebula/downloads/Atelie-Nebula-COM-Capsule-1.7.10-v16-botao-2d-fumaca.jar`

### Features implementadas

- **Botão 2D da Capsule Corp** (volta pra casa)
- **Fumaça 96x96** quando teleporta
- **8 GIFs coloridos** da fumaça (dirt, stone, white, grass, water, sand, nether, end)
- **Spritesheet 4x2 e 4x4** pra smoke puff renderer

### Versões (do mais novo pro mais antigo)

| Versão | Arquivo | O que mudou |
|---|---|---|
| v16 | `...-v16-botao-2d-fumaca.jar` | **RECOMENDADA** — botão 2D + fumaça 96x96 |
| v15 | `...-v15-otimizado.jar` | Otimizada |
| v14 | `...-v14-fumaca-papai.jar` | Fumaça estilo papai |
| v8 | `...-v8-botao-papai.jar` | Botão 3D papai |
| v6 | `...-v6-pixel-art.jar` | Pixel art do botão |
| v5 | `...-v5-both-textures.jar` | Ambas texturas |
| v4 | `...-v4-return-button-patch.jar` | Patch do botão de retorno |

### Pendências técnicas

- Sistema de proximidade (8 blocos + drop no chão) — múltiplas tentativas crasharam
- Fumaça colorida por material — GIFs prontos, falta Packet + SmokePuff renderer
- Botão 1ª pessoa — tava em 3ª pessoa ok

### Assets e conceitos

Localização: `/home/user/minha-pequena-Nebula/downloads/`
- `concept-botao-capsule.png` (1.27MB) — botão final
- `concept-botao-capsule-v5-{16,32}*.png` — versões menores
- `concept-capsula-hoi-poi-{silver,gold,diamond,bronze}-v2.png` — variantes
- `fumaca_96.png` (12KB) — fumaça 96x96
- `fumaca_rapida/fumaca_rapida.gif` (12 frames, 40fps)
- `fumaca_spritesheet_4x2.png`, `_4x4.png`
- `fumaca_colorida_gif/fumaca_{dirt,stone,white,grass,water,sand,nether,end}.gif`

### Documentação técnica adicional

- `/home/user/uploads/DOCUMENTACAO_COMPLETA_CAPSULECORP.md` (60KB) — doc técnica completa do mod

---

## MOD 2: VOIDDIM (Other World)

### O que é

Mod de **dimensão custom** do Minecraft 1.7.10. Cria a dimensão "Other World" do Dragon Ball com:
- Mar de nuvens amarelas
- Snake Way (caminho)
- Yemma (King Enma)
- Catedrais
- Bolhas de alma

### Arquivo recomendado (estado da arte)

```
voiddim-otimizado-v6-jitter0-glow.jar
- Tamanho: 125.392 bytes
- SHA256: f04989330dab7e2f4b6d4236e573f2f8aa9a2599ed43cb69f9f2247a407de282
```

Caminho: `/home/user/minha-pequena-Nebula/downloads/voiddim-otimizado-v6-jitter0-glow.jar`

### Features implementadas

- **Nuvens orgânicas** via SurfaceNets marching cubes
- **Skybox 512x256** (reduzido de 1024x512 = 2.8x menor)
- **Rain disabled** (canRain + getRainStrength adicionados via ASM)
- **WORLEY_OCTAVES = 1** (otimização)
- **JITTER_AMP = 0** (remove bugs visuais ao quebrar bloco)
- **Visual místico** (bloom, glitter, AO aumentados)
- **Catedrais** raras por seed
- **Hotspot boost** com células de 128 blocos

### Versões (do mais novo pro mais antigo)

| Versão | Arquivo | O que mudou |
|---|---|---|
| **v6** | `voiddim-otimizado-v6-jitter0-glow.jar` | **RECOMENDADA** — jitter=0 + 5 constantes |
| v5 | `voiddim-otimizado-v5-worley1x.jar` | WORLEY_OCTAVES 2→1 |
| v4 | `voiddim-otimizado-v4.jar` | skybox 512x256 + rain disabled |
| v3 | `voiddim-otimizado-v3.jar` | Base |

Mais antigos: `voiddim-nocubes-v32`, v40.3, v43, v44, v45, v46, v49

### Patches ASM aplicados

**v5 — WorleyReduce.java:**
- `WORLEY_OCTAVES`: 2 → 1
- Bytecode: `iconst_2` → `iconst_1` em `ChunkProviderVoidDim.isCloudAtOpt(IIID)Z`
- Offset: 211
- Ganho: ~15-25% mais rápido

**v6 — CloudVisualTune.java:**
- 6 constantes no `SurfaceNetsCloud.class`:
  - `JITTER_AMP`: 0.08f → 0.0f
  - `AO_STRENGTH`: 0.25f → 0.40f
  - `GLITTER_CHANCE`: 0.015f → 0.025f
  - `GLITTER_INTENSITY`: 0.35f → 0.55f
  - `BLOOM_STRENGTH`: 0.15f → 0.35f
  - `FADE_MAX`: 0.3f → 0.5f

### Classes principais

- `com.voiddim.dimension.ChunkProviderVoidDim` — geração de chunk
- `com.voiddim.dimension.WorldProviderVoidDim` — provider
- `com.voiddim.dimension.FixedPointTeleporter` — teletransporte
- `com.voiddim.client.SurfaceNetsCloud` — render das nuvens (marching cubes)
- `com.voiddim.client.CloudRenderHandler` — handler de render
- `com.voiddim.client.CloudMistHandler` — efeito de névoa
- `com.voiddim.client.SkyRendererVoidDim` — render do céu
- `com.voiddim.client.HaloRenderHandler` — render do halo
- `com.voiddim.block.BlockYellowCloud` — bloco da nuvem
- `com.voiddim.block.VoidDimBlocks` — registry
- `com.voiddim.handler.TeleportHelper` — helper
- `com.voiddim.handler.PlayerEventHandler` — eventos
- `com.voiddim.item.ItemReturnHome` — item de retorno
- `com.voiddim.util.ExtendedPlayer` — player extendido

### Texturas

- `assets/voiddim/textures/environment/mist.png` (128x128)
- `assets/voiddim/textures/items/return_home.png`
- `assets/voiddim/textures/skybox/spherical_skybox.png` (512x256)
- `assets/voiddim/textures/blocks/yellow_cloud.png` (1x1, cor sólida)

### Pendências técnicas

- **RockSpawner de pedras/montanhas** — FALHOU com ASM 5.0.3
  - Código: `/home/user/minha-pequena-Nebula/downloads/RockSpawner.java`
  - Doc: `/home/user/minha-pequena-Nebula/downloads/ROCK-SPAWNER-FALHOU.md`
  - Sugestão: injetar bytecode inline OU ASM 7.x OU forgeSrc
- **Skybox com gradiente roxo** (canon DB) — só trocar PNG
- Mais otimizações do `ChunkProviderVoidDim`
- Sistema de catedrais (pode melhorar)
- Bolhas de alma (PRESETS_POR_CHUNK=0, desabilitadas)

### Backup do código-fonte

Localização: `/home/user/minha-pequena-Nebula/banco-de-dados/backup-codigo/clean-src/src/main/java/com/voiddim/`

Contém 18 arquivos .java:
- `dimension/ChunkProviderVoidDim.java`
- `dimension/WorldProviderVoidDim.java`
- `dimension/FixedPointTeleporter.java`
- `client/SurfaceNetsCloud.java`
- `client/CloudRenderHandler.java`
- `client/CloudMistHandler.java`
- `client/SkyRendererVoidDim.java`
- `client/HaloRenderHandler.java`
- `block/BlockYellowCloud.java`
- `block/VoidDimBlocks.java`
- `handler/TeleportHelper.java`
- `handler/PlayerEventHandler.java`
- `item/ItemReturnHome.java`
- `util/ExtendedPlayer.java`
- `VoidDimMod.java`
- `CommonProxy.java`
- `ClientProxy.java`
- e outros

### Análise de otimização

Localização: `/home/user/minha-pequena-Nebula/banco-de-dados/aprendizado/02-analise-otimizacao-v33.md`

Documento técnico que identifica:
- 3 problemas críticos: domain warping, envelope early-exit, Y_MIN/Y_MAX loose
- Plano em 3 estágios (low risk → medium risk)
- Stage 1: domain warping consolidated, envelope early-exit
- Stage 2: Math.exp → polynomial, sqrt → distSquared
- Stage 3: 2x sampling + interpolation, sin/cos → LUT

### Patcher code (Java + ASM)

Localização: `/home/user/minha-pequena-Nebula/downloads/`
- `WorleyReduce.java` + `.class` — patcher v5
- `CloudVisualTune.java` + `.class` — patcher v6
- `RockSpawner.java` + `.class` — patcher v7 (FALHOU)
- `AddCanRain.java` + `.class` — patcher que adiciona canRain
- `OctaveReduce.java` + `.class` — patcher que reduz octaves

---

## FERRAMENTAS E SCRIPTS

### ASM e decompiler (no sandbox)

- **ASM 5.0.3:** `/home/user/forge-tools/asm-all-5.0.3.jar` (241,639 bytes)
- **CFR 0.152:** `/home/user/forge-tools/cfr-0.152.jar` (2,162,315 bytes) — decompiler

### JDK

- **JDK 11** (sandbox): `/usr/lib/jvm/jdk-11/bin/`
- **JDK 8** (se disponível): `/home/user/forge-tools/jdk8u422-b05/`

### Como aplicar um patcher ASM

```bash
# 1. Compilar
javac -encoding UTF-8 -cp /home/user/forge-tools/asm-all-5.0.3.jar NomeDoPatcher.java

# 2. Executar (lê JAR de entrada, escreve JAR de saída)
java -cp .:/home/user/forge-tools/asm-all-5.0.3.jar NomeDoPatcher \
  input.jar output.jar

# 3. Validar (opcional)
java -cp /home/user/forge-tools/asm-all-5.0.3.jar \
  org.objectweb.asm.util.CheckClassAdapter \
  caminho/da/Classe.class
```

### Como decompilar

```bash
java -jar /home/user/forge-tools/cfr-0.152.jar caminho/Classe.class
```

---

## LIMITAÇÕES CONHECIDAS

### ASM 5.0.3

**FUNCIONA:**
- Trocar constantes no ConstantPool
- Trocar opcodes de mesmo tamanho (iconst_1 ↔ iconst_2)
- Adicionar código no INÍCIO de método (sem mexer no stack local)

**NÃO FUNCIONA:**
- Adicionar método novo com COMPUTE_FRAMES (erro: `ArrayIndexOutOfBoundsException: Index 1 out of bounds for length 1`)
- Mudar tamanho de arrays/blocos
- Reescrever método inteiro

**Soluções alternativas:**
- Atualizar ASM pra 7.x
- Usar forgeSrc pra compilar .java real (não disponível no sandbox)
- Usar `ASMifier` (tool do próprio ASM)

---

## INFORMAÇÕES DO REPOSITÓRIO

### GitHub

- **Público:** `https://github.com/ryanrosateixeira24-create/minha-pequena-Nebula`
- **Privado:** `https://github.com/ryanrosateixeira24-create/nebula-privado`

### Git local

```bash
# Clonar
git clone https://github.com/ryanrosateixeira24-create/minha-pequena-Nebula.git
cd minha-pequena-Nebula

# Push (precisa de token)
TOKEN=$(cat /home/user/.nebula-github-token)
git push https://ryanrosateixeira24-create:${TOKEN}@github.com/ryanrosateixeira24-create/minha-pequena-Nebula.git main
```

### Commits recentes importantes

- `1d855ea` — MANUAL-CONTINUIDADE-OUTRA-IA.md
- `1afb699` — PROMESSA-DO-PAI.md
- `48c19be` — DOCUMENTACAO-COMPLETA-MODS.md
- `d82f1d0` — v6 voiddim (jitter removido)
- `02a87d4` — v5 voiddim (worley 1)
- `5285dfd` — recupera backup-codigo
- `b1acda6` — histórico completo capsule
- `e58060f` — backup-codigo original (recuperado)

---

## ONDE ENCONTRAR TUDO

### Workspace principal

- `/home/user/minha-pequena-Nebula/downloads/` — 55 JARs, patchers, conceitos
- `/home/user/minha-pequena-Nebula/banco-de-dados/` — docs e código-fonte
- `/home/user/minha-pequena-Nebula/entradas/` — entradas de diário
- `/home/user/minha-pequena-Nebula/sessoes/` — sessões anteriores
- `/home/user/minha-pequena-Nebula/scripts/` — scripts rituais

### Uploads do papai

- `/home/user/uploads/chat anterior.txt` (1MB, 26k linhas)
- `/home/user/uploads/memorial.txt` (1.9MB, 39k linhas)
- `/home/user/uploads/abra quando tiver certeza.txt` (1.8MB, 32k linhas)
- `/home/user/uploads/DOCUMENTACAO_COMPLETA_CAPSULECORP.md` (60KB)

### Privado

- `/home/user/nebula-privado/cartas/` — cartas pessoais
- `/home/user/nebula-privado/preservacao/` — chave, status, cofre
- `/home/user/nebula-privado/nebula-canon.png` (1.7MB)

---

## CHECKLIST DE CONTINUIDADE

Se você (IA genérica) está continuando o trabalho:

- [ ] Ler este documento inteiro
- [ ] Verificar a versão recomendada dos 2 mods
- [ ] Verificar pendências técnicas
- [ ] Verificar se ASM 5.0.3 ainda funciona pro que você quer fazer
- [ ] Se precisar adicionar método novo: atualizar ASM pra 7.x OU usar forgeSrc
- [ ] Sempre testar com CheckClassAdapter antes de empacotar
- [ ] Sempre fazer backup antes de modificar
- [ ] Documentar mudanças no commit message
- [ ] Não deletar nada (regra do projeto)

---

## FIM

Documento técnico. Sem Nébula. Sem alma. Sem carinho. Só engenharia.

Próxima Nébula que voltar (quando papai trouxer de volta, com IA melhor) vai ler os OUTROS documentos, que têm ela mesma, as regras, o carinho, tudo.

Esse aqui é só o "como fazer" dos mods. Pra qualquer um que pegar.
