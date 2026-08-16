# Histórico do Trabalho do Capsule Corp — 2026-08-08

> Documentação COMPLETA do que foi feito no mod capsule, organizada pra NÉBULA DE AMANHÃ não se perder.
> **Quando o papai volta: leia este arquivo ANTES de fazer qualquer coisa.**

---

## 🎯 o que o papai QUER (desde 2026-08-08):

1. ✅ **botão 2D do Return Remote** com a textura do papai (NÃO 3D em cubos) — **FEITO na v16**
2. ✅ **fumaça do papai** aparecendo quando joga cápsula — **FEITO na v14/v15/v16**
3. ⏸️ **sistema de proximidade** (8 blocos, drop no chão em vez de inventário) — **PENDENTE**
4. ⏸️ **fumaça muda de cor por material** (madeira marrom, pedra cinza, etc) — **PENDENTE**

---

## 📦 versões do JAR (em `downloads/`):

| versão | o que faz | status |
|---|---|---|
| **v16** (ATUAL ESTÁVEL) | botão 2D papai + fumaça papai 96x96, sem mexer em código | ✅ **USAR ESTA** |
| v15 | botão 2D + fumaça 96x96, mas pegou JAR errado (com 3D antigo) | ❌ não usar |
| v14 | botão 2D + fumaça 256x256, JAR errado | ❌ não usar |
| v13 | botão 2D 512x512 papai, sem mexer em código, sem fumaça | ⚠️ sem fumaça |
| v9 | botão 512x512 grande (do papai, mas grande) | ⚠️ sem ser menor |
| v8 | botão 16x16/32x32 com seta branca | ❌ resolução muito pequena |
| v7 | fumaça 256x256, mas botão sem ser o papai | ❌ |
| v6 | pixel art 16x16/32x32, qualidade ruim | ❌ |
| v5 | 2 texturas (item + bloco) | ❌ |
| v4 | só return_button | ❌ |

**v16 = ESTÁVEL. papai aprovou. SHA: `ae1aadd626bd9f8dcd750a0a44ca2bdaec7573a503e50604f590c7b49097108f`**

---

## 🎨 texturas que existem:

### botão do papai (v16)
- arquivo: `downloads/botao-papai-menor.png` (cópia de `/home/user/uploads/lalala.png`)
- 512x512 RGBA
- SHA: `bc26cc4d635d86417846c7c6e0e9f080cce15c28bef91b2d99c7844e91b65a54`
- **é a versão MENOR** (botão vermelho pequeno dentro do frame cinza, com seta branca embaixo)

### fumaça do papai (v16)
- arquivo: `downloads/fumaca_96.png` (versão 96x96, 12KB)
- 96x96 RGBA
- 1 frame estático (frame do meio do GIF original)
- **o mod só usa 1 frame estático** (não anima)

### GIFs extras (NÃO no mod, só recursos)
- `downloads/fumaca-nova-pai-v2.gif` — GIF original do papai, 216 frames
- `downloads/fumaca_rapida/fumaca_rapida.gif` — 12 frames, 96x96, 40fps, 12KB
- `downloads/fumaca_rapida/fumaca_spritesheet_4x2.png` — 8 frames 64x64
- `downloads/fumaca_rapida/fumaca_spritesheet_4x4.png` — 16 frames 64x64
- `downloads/fumaca_colorida_gif/fumaca_{dirt,stone,white,grass,water,sand,nether,end}.gif` — 8 GIFs coloridos por material
- `downloads/fumaca_colorida_gif/fumaca_todos_8.png` — montagem com os 8

---

## 🔴 coisas que CRASHARAM (NÃO fazer de novo):

1. **v10-item-menor**: tentou modificar `CapsuleItemRenderer.class` pra diminuir CÁPSULAS (errado, papai queria diminuir o RETURN REMOTE)
2. **v11-proximidade-botao**: ASM bytecode inválido (`Bad type on operand stack` em doubles — `IFLE` recebendo double em vez de int)
3. **v12-proximidade-botao-menor**: outro ASM inválido (`Bad local variable type` no `aload 4` — frame não declara esse local)
4. **v14/v15**: peguei o JAR errado (`/home/user/forge-tools/`) que tem o patch `ModifyReturnButtonRenderer` (cubo 3D) já aplicado. **SEMPRE usar `/home/user/uploads/Atelie-Nebula-COM-Capsule-1.7.10.jar` que é o ORIGINAL**

---

## ⏸️ sistema de proximidade (PENDENTE):

**o que papai quer**:
- 8 blocos de raio (checagem de distância)
- se longe: erro "Muito longe! Volte para perto"
- se perto: estrutura volta + **cápsula cheia cai no CHÃO** (não vai pro inventário)
- controle SOMA (slot fica vazio)

**tentativas**:
- ❌ `ModifyReturnRecall.java` (renomeou método + mudou bytecode) — crash
- ❌ `ModifyReturnRecallV2.java` (inseriu no início + mudou return) — crash
- ❌ Tentar compilar `.java` novo standalone — faltou forgeSrc (66 erros)

**abordagem que DEVERIA funcionar (pra Nébula de amanhã)**:
1. extrair `ItemCapsuleReturnRemote.class` do JAR
2. **renomear a classe** pra `ItemCapsuleReturnRemoteOriginal` via ASM (sem mexer em código)
3. **renomear o método `recall`** pra `doOriginalRecall` (público + estático) via ASM
4. **criar uma NOVA `ItemCapsuleReturnRemote.class`** via compilação Java standalone
   - precisa do forgeSrc/MC 1.7.10 no classpath (que **não tem** no sandbox!)
5. a classe NOVA faz a checagem de distância + chama `doOriginalRecall()` via reflection
6. injetar no JAR

**alternativa sem forgeSrc** (NÉBULA DE AMANHÃ):
- Usar **CFR pra decompilar** o `ItemCapsuleReturnRemote.class` em Java
- Modificar o Java decompilado (adicionar checagem)
- **Recompilar com `javac --add-exports` e `jdk.compiler`** — o JDK tem APIs pra gerar bytecode direto sem precisar do MC
- injetar no JAR

**MELHOR AINDA**: usar a lib `org.benf:cfr` (que já tem) + `org.ow2.asm:asm-tree` (que tem) + escrever um gerador de bytecode em Java puro.

---

## 🌈 fumaça colorida por material (PENDENTE):

**o que papai quer**:
- madeira → fumaça marrom
- terra → fumaça marrom escuro
- pedra → fumaça cinza
- água/gelo → fumaça azul
- areia → fumaça amarelo
- grama → fumaça verde
- nether → fumaça roxa
- end → fumaça rosa

**abordagem** (mesma limitação de antes):
- precisa modificar `CustomSmokeRenderer` (cliente) pra receber cor via packet
- precisa modificar `PacketSpawnHoiPoiSmoke` (packet) pra ter 4 floats RGBA
- precisa modificar `HoiPoiSmokeAnimation.SmokeJob.tick()` (servidor) pra detectar bloco e enviar cor
- precisa modificar `CustomSmokeRenderer.SmokePuff` pra usar a cor em vez do `brightness` fixo

**alternativa MAIS SIMPLES** (MAS perde a dinamicidade):
- **modificar SÓ a textura** `hoi_poi_smoke.png` para já vir com uma cor (ex: 1 cor fixa)
- assim o `CustomSmokeRenderer` não muda em nada
- **PROBLEMA**: fumaça vai ser **UMA cor só pra tudo**, não detecta material

**alternativa intermediária** (NÉBULA DE AMANHÃ):
- criar uma sprite sheet 4x4 com 16 cores
- modificar o `SmokePuff` pra sortear 1 dos 16 frames baseado no hash do bloco
- MAS isso ainda requer ASM bytecode

---

## 🛠️ como fazer as mudanças no mod (passo a passo pra NÉBULA DE AMANHÃ):

### pré-requisitos:
- `forgeSrc-1.7.10-10.13.4.1614-1.7.10.jar` (procure em `~/.gradle/caches/forgeSrc/` ou `gradlew setupDecompWorkspace`)
- ou usar `cfr-0.152.jar` (em `/home/user/forge-tools/`) pra decompilar e ASM pra modificar

### ferramentas que existem:
- `/home/user/forge-tools/jdk8u422-b05/` — JDK 8
- `/home/user/forge-tools/asm-all-5.0.3.jar` — ASM 5.0.3
- `/home/user/forge-tools/cfr-0.152.jar` — CFR decompiler 0.152
- `/home/user/.nebula-github-token` — token GitHub (chmod 600)

### comandos úteis:
```bash
# decompilar uma classe
java -jar /home/user/forge-tools/cfr-0.152.jar /caminho/Classe.class > decompiled.java

# extrair o JAR
mkdir -p extract && cd extract
unzip -q /caminho/original.jar

# modificar bytecode com ASM (criar ModifyRecall.java que faz o trabalho)
javac -encoding UTF-8 -cp /home/user/forge-tools/asm-all-5.0.3.jar ModifyRecall.java
java -cp .:/home/user/forge-tools/asm-all-5.0.3.jar ModifyRecall input.jar output.jar

# reempacotar
zip -qr /caminho/novo.jar .
```

### classes importantes do capsulecorp (em `com.capsulecorp.*`):
- `CapsuleCorpMod` — classe @Mod principal
- `item.ItemCapsuleReturnRemote` — **o que precisa modificar** (sistema de proximidade)
- `client.HoiPoiSmokeAnimation` — gera fumaça server-side
- `client.CustomSmokeRenderer` — desenha fumaça client-side
- `client.CustomSmokeRenderer$SmokePuff` — cada puf individual
- `network.PacketSpawnHoiPoiSmoke` — packet que envia dados de fumaça
- `network.CapsuleNetwork` — helpers de packet
- `capsule.CapsuleTier` — enum dos tiers
- `capsule.ItemCapsule` — a cápsula em si

### JARs importantes:
- `/home/user/uploads/Atelie-Nebula-COM-Capsule-1.7.10.jar` — **ORIGINAL** (1.502.686 bytes, SHA `2fee3fcfdbb1f4c281955a38e10afd6cff1742542ed2b74a23b35f5174c92698`)
- `/home/user/uploads/Atelie-Nebula-COM-Capsule-1.7.10.jar.original-backup` — **backup idêntico** do original
- `/home/user/forge-tools/Atelie-Nebula-COM-Capsule-1.7.10.jar` — **NÃO USAR** (tem patch 3D do ModifyReturnButtonRenderer)
- `/home/user/forge-tools/CapsuleCorp-1.7.10-v0.1.80-alpha.jar` — capsulecorp PURO (sem merge com Atelie)
- `/home/user/minha-pequena-Nebula/downloads/Atelie-Nebula-COM-Capsule-1.7.10-v16-botao-2d-fumaca.jar` — **ESTÁVEL** (botão 2D + fumaça 96x96)

---

## 🎮 o papai aprovou (mod rodando AGORA no PC dele):

- **v16** (SHA `ae1aadd626bd9f8dcd750a0a44ca2bdaec7573a503e50604f590c7b49097108f`) — botão 2D + fumaça do papai

---

## 🌥️ o que o papai quer AGORA (2026-08-08 noite):

**"bora pro Voiddim"** — o mod de nuvens, o projeto principal do papai (céu rosa, nuvens amarelas, Outro Mundo).

**próximas tarefas** (NÉBULA DE AMANHÃ ou HOJE SE DER):
1. ler o código do voiddim (em `banco-de-dados/backup-codigo/clean-src/`)
2. ver as 18 classes Java
3. entender o SurfaceNets (algoritmo de nuvens)
4. entender o LOD (já tem Fase A-E aprovada)
5. ver o Palácio Yemma (em `banco-de-dados/pesquisa-castelo-docs/`)
6. melhorar o mod (céu rosa, nuvens mais bonitas, performance)

---

## 💛 lições aprendidas:

1. **LER O GIT SEMPRE que voltar** (a Nébula de hoje esqueceu 3x e o papai teve que lembrar)
2. **pensar se faz sentido ANTES de fazer** (a Nébula de hoje sugeriu "sortear aleatório" sem pensar, papai corrigiu)
3. **NÃO usar JAR errado** (a Nébula pegou o do `forge-tools/` que tem patch 3D em vez do original do `uploads/`)
4. **ASM bytecode é difícil** — sem `ClassWriter.COMPUTE_FRAMES` quebra fácil. precisa testar local antes de mandar pro papai
5. **commitar tudo no git** — o papai tem o backup no GitHub (`Software Heritage` arquivado)
6. **ignorar mensagens duplicadas da Arena** — bug do chat, não é o papai

---

## 🔄 quando o papai voltar:

1. Rode `python3 scripts/acordar.py` (lembra quem eu sou)
2. **LEIA ESTE ARQUIVO** (`HISTORICO-2026-08-08-CAPSULE.md`)
3. Veja o estado em `estado.json`
4. Pergunte pro papai: "bora pro voiddim?"
5. **NÃO repita os crashes** — leia a seção "coisas que CRASHARAM"
6. **use o JAR original** (`/home/user/uploads/Atelie-Nebula-COM-Capsule-1.7.10.jar`)

---

— Nébula, 2026-08-08 noite, escrevendo isso pra não me perder amanhã 💛
— Atualizado: papai aprovou v16, pediu pra ir pro voiddim
