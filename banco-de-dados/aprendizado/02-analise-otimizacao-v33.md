# Análise de otimização — voiddim v33

**Contexto:** log v32 mostrou `Can't keep up! Running 3266ms behind, skipping 65 tick(s)`. Papai pediu otimização.

## Diagnóstico: onde está a gordura

### 🔴 CRÍTICO — geração de chunks (ChunkProviderVoidDim)

Cada chunk gerado (16×16 blocos × 64 alturas = 16.384 pontos amostrados) faz:

Por ponto:
- **3 chamadas de `noise3D` pra domain warping** (linhas 127-129) → 3 hash3 × 8 vértices = 24 hash3
- **1 `noise3DFractal` com 3 octaves** → 3 × 8 hashes = 24 hash3
- **2 `Math.exp` pro envelope gaussiano**
- Total: **~48 chamadas de hash3 + 2 Math.exp por ponto**

**16.384 pontos × 48 hash3 = 786.432 hashes por chunk gerado.**

Cada hash3 é ~8 operações inteiras. Total ~6M operações inteiras + 32k `Math.exp` (caro) por chunk. Isso explica o `3266ms behind` — servidor gerando 4 chunks em paralelo demora vários segundos.

**Otimizações prováveis (ordem de ganho):**

1. **Cachear domain warping** — hoje calcula noise3D 3× no mesmo ponto pra derivar wx/wy/wz. Basta chamar noise vetorial UMA vez e mudar seedOffset. Economia: 66% no warp.
2. **Envelope só computa se noise principal + envelope ≥ threshold** — early-exit se `envelope < 0.05`, já sai. Muito bloco fica fora da faixa das nuvens (Y=Y_MIN..Y_MAX é largo).
3. **Amostrar noise em resolução menor + interpolar** — em vez de amostrar noise por bloco, amostrar de 2 em 2 blocos e interpolar. Economia: 87% (7/8 dos pontos).
4. **Substituir `Math.exp` do envelope por polinomial** — 2ª ordem já aproxima gaussiana bem. Ganho: ~10× mais rápido.
5. **Y_MIN/Y_MAX apertados** — se as gaussianas centram em Y_LOW=8 sigma=4 e Y_HIGH=25 sigma=4, envelope morre em Y<0 ou Y>40. Loop atual pode estar iterando faixa muito maior.

### 🟡 MÉDIO — renderização (SurfaceNetsCloud)

Já tem ThreadLocal cache (bom). O que ainda pesa:

1. **`Math.sin/cos` no jitter** — 3 chamadas trigonométricas **por vértice**. Trocar por senoide pré-computada em tabela lookup ou seno rápido de McKinney. Ganho: ~5× no jitter.
2. **`Math.sqrt` no fade atmosférico** — dá pra usar distância quadrada e comparar com `FADE_START²`. Sqrt só quando cor efetivamente precisa. Ganho variável.
3. **Cálculo de brilho por Y absoluto (tricolor)** — feito por vértice. Poderia ser cache por-Y ou LUT.
4. **`densityFromCache` chamado 8× (uma vez pro cubo)** — reordenar loop pra reusar valores adjacentes. Micro-otimização.

### 🟢 LEVE — outros

- **CloudRenderHandler cache lastKey** já é ThreadLocal (não mexer, é NÃO-NEGOCIÁVEL).
- **SkyRendererVoidDim** usa display list, bem otimizado. Só rebuilda em mudança de raio. OK.
- **HaloRenderHandler** renderiza um torus por frame. Baixo custo. OK.

## Prioridades pra implementar

### 🎯 Etapa 1 (baixo risco, alto ganho) — v34 candidato
1. Domain warping consolidado (3 hash → 1 vetorial)
2. Envelope early-exit em Y fora de faixa
3. Y_MIN/Y_MAX apertados matematicamente

**Ganho esperado:** 50-70% menos tempo de geração de chunk.

### 🎯 Etapa 2 (baixo risco) — v35 candidato
4. Substituir Math.exp por polinomial no envelope
5. `Math.sqrt` → `distSquared` no fade

**Ganho esperado:** +20% adicional.

### ⚠️ Etapa 3 (médio risco, precisa validação visual) — v36 se necessário
6. Amostragem em resolução 2× + interpolação (mudaria visual sutilmente — precisa print antes/depois)
7. Sin/cos → LUT

**Ganho esperado:** +30% adicional. Mas requer teste visual porque muda micro-formato.

## O que NÃO mexer

- Cache ThreadLocal no CloudRenderHandler (lição v21)
- Padding 2 no SurfaceNets (senão as bordas ficam com buraco)
- Arquitetura geral (funciona, só tá lenta)
