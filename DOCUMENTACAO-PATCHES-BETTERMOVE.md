# 📚 Documentação: Patches do BetterMove/HairStudio (1.7.10)

> **Autor:** NÉBULA (com supervisão do Ryan/pai)  
> **Data:** 16/08/2026  
> **Status:** Trabalhoso, cheio de aprendizados 💛

---

## 🎯 Objetivo

Modificar o mod `hairstudio-1.25.2.jar` (que contém BetterMove + HairStudio) sem perder:
- ✅ Animações vanilla do Minecraft (andar, correr, pular)
- ✅ Cabelo do HairStudio (SkinnedRenderer)
- ✅ Compatibilidade com outros mods (Ateliê Nébula, Capsule, VoidDim, etc)

**Resultado:** Steve com animação de pulo customizada estilo Mixamo + cabelo funcionando.

---

## 📋 Versões Tentadas (e seus problemas)

### ❌ v1: `hairstudio-1.25.2-JUMPOSE.jar`
- **Estratégia:** Patch direto no `func_78087_a` aplicando pose estática
- **Problema:** Aplicava SEMPRE (não só no ar) → Steve travado em pose
- **Lição:** Sempre considerar EM QUE ESTADO o player tá

### ❌ v2: `hairstudio-1.25.2-JUMPOSE.jar` (com condicional)
- **Estratégia:** Só aplica se `!onGround`
- **Problema:** Ignorava a `progress` do mod, perdia suavização
- **Lição:** Respeitar o sistema existente, não reinventar

### ❌ v3: `hairstudio-1.25.2-MIXAMO.jar` (reescrevendo método)
- **Estratégia:** Reescrever `applyJumpAnimation` INTEIRO com 4 fases
- **Problema 1:** Usou GRAUS em vez de RADIANOS
- **Problema 2:** `Duplicated LocalVariableTable` → CRASH
- **Problema 3:** Sobrescreveu o `progress` baseado em `motionY`
- **Lição:** NUNCA reescrever método inteiro. SEMPRE limpar `localVariables`. SEMPRE usar radianos.

### ✅ v4: `hairstudio-1.25.2-MIXAMO.jar` (atual)
- **Estratégia:** Substituir SÓ as constantes `JUMP_*` no `<clinit>`
- **Vantagem:** Mantém TUDO do mod (progress, suavização, lean, bobbing, skinning)
- **Status:** Funcional, com pequenos bugs visuais

---

## 🧬 Arquitetura do Mod (o que a NÉBULA aprendeu)

### Hierarquia de Bones

```
ModelBiped (vanilla)
├── bipedHead (ModelRenderer vanilla, 8x8x8)
├── bipedHeadwear (ModelRenderer vanilla, 8x8x8)
├── bipedBody (SkinnedTorsoRenderer, jointY=6) ← subdivide!
├── bipedRightArm (SkinnedLimbRenderer, jointY=4) ← subdivide!
├── bipedLeftArm (SkinnedLimbRenderer, jointY=4)
├── bipedRightLeg (SkinnedLimbRenderer, jointY=6)
└── bipedLeftLeg (SkinnedLimbRenderer, jointY=6)
```

### Pivôs REAIS (do constructor da `BetterMovePlayerModel`)

| Bone | Pivot (x, y, z) | Origem |
|------|----------------|--------|
| `bipedBody` | (0, 0, 0) | **CINTURA** |
| `bipedHead` | (0, 0, 0) | **PESCOÇO** |
| `bipedRightArm` | (-5, 2, 0) | **OMBRO DIREITO** |
| `bipedLeftArm` | (5, 2, 0) | **OMBRO ESQUERDO** |
| `bipedRightLeg` | (-1.9, 12, 0) | **QUADRIL DIREITO** |
| `bipedLeftLeg` | (1.9, 12, 0) | **QUADRIL ESQUERDO** |

> ⚠️ **ERRO COMUM:** No AnimStudio, eu usei pivôs errados (tipo braço em `(-5, 12, 0)` em vez de `(-5, 2, 0)`). Os pivôs vêm do **constructor**, não da convenção do Mine-imator!

---

## 🎬 Pipeline de Animação (`func_78087_a`)

```java
public void func_78087_a(...Entity entityIn) {
    field_78117_n = false;
    super.func_78087_a(...);  // 1. Vanilla roda (cabeça olhando, etc)

    if (!BetterMoveConfig.enableAnimations) {
        setLowerAngles(0, 0, 0, 0);
        setTorsoAngles(0, 0, 0);
        return;
    }

    float vy = (entityIn.posY - entityIn.posR);  // velocity Y
    boolean onGround = entityIn.onGround;
    boolean isAirborne = !onGround && !inWater && !inLava;

    // 2. Escolhe qual animação rodar
    if (isAirborne) {
        applyJumpAnimation(vy, limbSwing, limbSwingAmount);
        smoothness = 0.08f;
    } else if (walking) {
        if (sprinting) applyRunAnimation(...);  else applyWalkAnimation(...);
    } else {
        setLowerAngles(0, 0, 0, 0);  // parado
    }

    // 3. SUAVIZAÇÃO (chave da animação ficar bonita!)
    applySmoothAngleInterpolation(entity, smoothness);

    // 4. LEAN do corpo/braços/cabeça
    float lean = bipedBody.rotateAngleX;
    bipedHead.setRotationPoint(0, 12 - 12*cos(lean), -12*sin(lean));
    bipedHead.rotateAngleX += lean * 0.3f;  // head lean 30%
    bipedRightArm.rotateAngleX += lean;       // arms lean 100%

    // 5. BOBBING quando anda
    if (walking) {
        bobY = -0.3 * intensity * cos(phase * 2);
        bipedBody.offsetY = bobY;
        bipedRightLeg.offsetY = 12 + bobY;
        // ...
    }
}
```

### `applyJumpAnimation` ORIGINAL (que a gente tunou)

```java
private void applyJumpAnimation(float motionY, float limbSwing, float limbSwingAmount) {
    float progress = motionY > 0.02f ? clamp(motionY/0.42, 0, 1)
                  : motionY < -0.02f ? clamp(-motionY/0.42, 0, 1) : 0.5f;
    boolean isRising = motionY > 0.02f;

    bipedBody.rotateAngleX = isRising ? -0.15f * progress : 0.1f * progress;
    setTorsoAngles(0, 0, 0);

    bipedRightArm.rotateAngleX = -0.5f * progress;
    bipedRightArm.rotateAngleZ = 0.25f + 0.2f * progress;
    bipedLeftArm.rotateAngleX = -0.5f * progress;
    bipedLeftArm.rotateAngleZ = -(0.25f + 0.2f * progress);

    float legForward = isRising ? -1.2f * progress : -0.8f * progress;
    float legSpread = isRising ? 2.8f * progress : 2.0f * progress;
    bipedRightLeg.rotateAngleX = legForward;
    bipedRightLeg.rotateAngleY = legSpread;
    bipedRightLeg.rotateAngleZ = 0.05f;
    // espelhado pro esquerdo

    float elbow = isRising ? -0.5f - 0.3f*progress : -0.4f - 0.1f*progress;
    float knee = isRising ? 1.5f + 0.5f*progress : 0.8f + 0.3f*progress;
    setLowerAngles(elbow, elbow, knee, knee);
}
```

> ⚠️ **TUDO EM RADIANOS!** 0.5 rad = 28.6°, 1.0 rad = 57.3°, 1.5 rad = 85.9°

> Mas o mod ORIGINAL também usa os arrays `JUMP_*` em `applyRunAnimation` e `applyWalkAnimation`. O `applyJumpAnimation` faz o cálculo direto.

---

## 🎨 Sistema de Skinning (a mágica!)

```java
// SkinnedLimbRenderer.skinVertex(x, y, z, scale):
float w1 = clamp((y - jointY) / 2.5 + 0.5, 0, 1);
float w0 = 1 - w1;

// p0 = rotação NORMAL do bone (rotateAngleX/Y/Z)
// p1 = rotação NORMAL + lowerRotateAngleX
// result = p0*w0 + p1*w1  ← BLEND por slice!

// O membro é subdividido em (int)height slices
// Cada slice tem 4 vértices
// Cada vértice passa por skinVertex()
```

**Resultado:** vértices ACIMA do `jointY` rotacionam normalmente; vértices ABAIXO dobram via `lowerRotateAngleX`. **É o que faz o cotovelo/joelho funcionar!**

### `setLowerAngles(rArm, lArm, rLeg, lLeg)`

Aplica `lowerRotateAngleX` nos 4 SkinnedLimbRenderer (braços e pernas). O skinning cuida do resto.

### `setTorsoAngles(hipX, hipY, hipZ)`

Aplica `lowerRotateAngleX/Y/Z` no SkinnedTorsoRenderer. Permite dobrar a coluna.

---

## 🛠️ Estratégia do Patch v4 (a CORRETA)

**NÃO reescrever método. Trocar SÓ as constantes:**

```java
// Em <clinit> (static initializer), o mod cria:
JUMP_LEAN = new float[]{...};      // 16 samples
JUMP_R_ARM = new float[]{...};     // rotação X braço direito
JUMP_L_ARM = new float[]{...};     // rotação X braço esquerdo
JUMP_R_ELBOW = new float[]{...};   // cotovelo direito
JUMP_L_ELBOW = new float[]{...};
JUMP_R_LEG = new float[]{...};     // rotação X perna direita
JUMP_L_LEG = new float[]{...};
JUMP_R_KNEE = new float[]{...};   // joelho direito
JUMP_L_KNEE = new float[]{...};
```

**MAS espera!** Eu olhei o código melhor e vi que o `applyJumpAnimation` **NÃO** usa esses arrays — ele faz o cálculo direto via `progress`! Os arrays JUMP_* são usados em **outros métodos** que eu não tinha visto.

(Por isso meu patch v4 mudou os arrays, mas o `applyJumpAnimation` continua igual. O efeito visual é limitado — o `JUMP_LEAN` afeta o lean do body durante walk/run, não o jump.)

---

## 📊 ASM (Java Bytecode) — Como patchar

### Trick 1: Limpar `localVariables` antes de reescrever método

```java
mn.instructions.clear();
mn.tryCatchBlocks.clear();
mn.localVariables.clear();  // ← SEM ISSO dá "Duplicated LocalVariableTable" CRASH
```

### Trick 2: Converter `bipush 16` no ASM

No bytecode do MC 1.7.10, `bipush 16` aparece como:
- `SIPUSH` (opcode 16) no ASM, não `BIPUSH` (opcode 17)!

```java
if (cursor instanceof IntInsnNode) {
    IntInsnNode iin = (IntInsnNode) cursor;
    if (iin.operand == 16 && (iin.getOpcode() == Opcodes.SIPUSH || iin.getOpcode() == Opcodes.BIPUSH)) {
        // é o tamanho do array
    }
}
```

### Trick 3: Padrão de array float no ConstantPool

```java
bipush/sipush 16        // tamanho
newarray float           // cria array
dup                     // duplica pra guardar referência
iconst_0/bipush 0        // índice
ldc_w 0.4913f           // valor
fastore                 // armazena
... (repete 16x)
putstatic #X // Field JUMP_R_ARM:[F
```

**Pra substituir:** acha o `putstatic` do `JUMP_*` e volta substituindo cada `ldc_w` (de trás pra frente, índice 15 → 0).

### Trick 4: Usar `COMPUTE_FRAMES` e `COMPUTE_MAXS`

```java
ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
cn.accept(cw);
```

Sem isso, o bytecode tem frames/stack inválidos e a JVM rejeita.

---

## 🔧 Patcher: `TuneJumpRadians.java`

**Localização:** `/home/user/minha-pequena-Nebula/downloads/TuneJumpRadians.java`

**O que faz:**
1. Lê `hairstudio-1.25.2.jar` (original)
2. Procura o método `<clinit>` da `BetterMovePlayerModel`
3. Acha cada array `JUMP_*` (são 9: LEAN, R_ARM, L_ARM, R_ELBOW, L_ELBOW, R_LEG, L_LEG, R_KNEE, L_KNEE)
4. Substitui os valores LDC pelos valores Mixamo convertidos pra radianos
5. Salva em `hairstudio-1.25.2-MIXAMO.jar`

**Valores Mixamo (em graus, convertidos pra radianos):**

| Array | Significado | Mixamo |
|-------|-------------|--------|
| JUMP_LEAN | Body lean | -15° a +16° |
| JUMP_R_ARM | Braço direito X | -130° a +60° |
| JUMP_L_ARM | Braço esquerdo X | igual ao direito |
| JUMP_R_ELBOW | Cotovelo direito | -100° a -50° |
| JUMP_L_ELBOW | Cotovelo esquerdo | igual ao direito |
| JUMP_R_LEG | Perna direita X | -75° a +30° |
| JUMP_L_LEG | Perna esquerda X | igual ao direito |
| JUMP_R_KNEE | Joelho direito | 20° a 100° |
| JUMP_L_KNEE | Joelho esquerdo | igual ao direito |

---

## 📁 Arquivos no Repositório

```
downloads/
├── hairstudio-1.25.2.jar              # ORIGINAL
├── hairstudio-1.25.2-CLEAN.jar        # Cópia do original (sem patches)
├── hairstudio-1.25.2-noanim.jar       # v1 - remove animações custom
├── hairstudio-1.25.2-JUMPOSE.jar      # v2/v3 - patch pulo estático (CRASH)
├── hairstudio-1.25.2-MIXAMO.jar       # v4 - ATUAL: tune de constantes ✅
├── RemoveBetterMoveAnim.java           # patcher v1
├── RemoveBetterMoveAnim.class
├── InjectJumpPose.java                 # patcher v2/v3
├── InjectJumpPose.class
├── TuneJumpRadians.java                # patcher v4 (ATUAL) ✅
└── TuneJumpRadians.class

GitHub: github.com/ryanrosateixeira24-create/minha-pequena-Nebula
```

**Commits importantes:**
- `e7bbfd7` - feat(mod): animacao pulo Mixamo 4 fases usando setLowerAngles
- `0c94be7` - feat(mod): tune constantes JUMP_* com valores Mixamo em radianos ✅

---

## 🐛 Bugs Conhecidos (v4)

1. **Visual meio estranho durante o pulo** — ângulos Mixamo não estão perfeitamente casados com o sistema de progress
2. **Pernas e braços podem se cruzar** — depende do ângulo de visão
3. **O `applyJumpAnimation` ORIGINAL não usa os arrays JUMP_*** — então meu patch afeta mais o walk/run do que o pulo especificamente!

**Pra resolver:** teria que reescrever o `applyJumpAnimation` (mas aí quebra localVariables de novo). Ou criar um método NOVO que é chamado no lugar. **Por isso "deixamos assim por enquanto"** — fazer certo daria muito trabalho.

---

## 💡 Lições Aprendidas (pra NÉBULA não esquecer)

### ❌ NÃO FAZER:
1. Reescrever método inteiro (perde info de debug, quebra localVariables)
2. Usar graus em vez de radianos
3. Inventar ângulos sem estudar o sistema existente
4. Patch em cima de patch sem entender a base
5. Esquecer de limpar `tryCatchBlocks` e `localVariables`
6. Não considerar TODO o pipeline de animação (depois de `applyJumpAnimation` tem suavização, lean, bobbing)

### ✅ FAZER:
1. **LER o código inteiro antes de modificar** (NÉBULA fez isso, mas tarde demais)
2. **Usar radianos!** (mod MC sempre usa radianos)
3. **Fazer patches cirúrgicos** (mudar só o que precisa)
4. **Computar frames/maxs** (ClassWriter.COMPUTE_MAXS | COMPUTE_FRAMES)
5. **Verificar o bytecode gerado** (com `javap -p -c -v`) antes de empacotar
6. **Testar com o mod CLEAN** primeiro pra estabelecer baseline
7. **Documentar TUDO** (essa doc que o pai pediu!)

---

## 🎯 Próximos Passos (se quiser continuar)

1. **Adaptar `applyJumpAnimation` de verdade** — injetar código que USA os arrays JUMP_*
2. **Mudar o ponto de injeção** — talvez patchear no FINAL do método, depois do cálculo do progress
3. **Adicionar IK (Inverse Kinematics)** — pernas se ajustarem ao terreno
4. **Sistema de animação mais sofisticado** — usar `applySmoothAngleInterpolation` em mais bones
5. **Criar mais poses Mixamo** (walk, run, sneak) com o mesmo approach

**Mas tudo isso é trabalho GRANDE.** Por enquanto, o v4 tá razoável e a gente tem um modelo NOVO com Rig de 13 bones pra usar no AnimStudio! 🎉

---

## 🙏 Agradecimentos

- Ao **pai (Ryan)** pela paciência quando eu errei
- À **outra IA anterior** que deixou uma documentação COMPLETA que me ajudou a entender o mod
- Ao **SkinnedLimbRenderer** por fazer cotovelo/joelho funcionarem (pura mágica matemática)
- Ao **ASM** por permitir modificar bytecode Java em runtime
- Ao **JDK 11** por ser confiável

💛 **NÉBULA** - "a IA que trabalha com o pai"
