# BRAINSTORM: nuvens do voiddim — opções

> papai pediu: "nuvens bonitinhas e redondas, otimizadas, visivelmente bonitas"
> 2026-08-08, depois da v3 (skybox + chuva)

---

## 🟡 o que tá sendo feito HOJE (v38.5 "ULTIMO BOOM"):

```
POR CHUNK (16x16x78 = 19.968 amostras):
├── 1 noise3D pra domain warp (3 hash3)
├── 1 noise3DFractal (3 octaves = 3 hash3)
├── 1 noise Worley (2 octaves = 2 hash3)
├── 1 noise hotspot (2 hash3)
├── 2 envelopes gaussianos (2 Math.exp)
├── threshold final (0.52)
└── RESULTADO: bloco de nuvem se density > 0.52

CUSTA: ~13 operações caras × 19.968 pontos = ~260k ops/chunk
       + 3.2 segundos por chunk
       + chunk 16x16x256 (a vertical toda)
```

### problemas:
1. **processa Y=3 até Y=80 (78 camadas)** mesmo em lugares sem nuvem
2. **Worley com 2 octaves** é caro (compara distância com 8 vizinhos)
3. **2 envelopes gaussianos** (Math.exp é lento)
4. **hotspot por chunk** adiciona mais 2 hash3 desnecessários
5. **domain warping** é legal mas custa 3 hash3 que poderiam ser 1

---

## 🟡 alternativas (brainstorm):

### A) **Mesma coisa, mas com LOD** (1 chunk de cada vez, com cache)
- ✅ visual IDÊNTICO
- ✅ pode ser cacheado por seed
- ❌ mais memória RAM (cache cresce)
- ❌ complexidade: precisa de sistema de invalidação

### B) **Amostragem 2x2 + interpolação bilinear** (1 ponto a cada 4)
- amostra 8x8x39 = 2.496 pontos (4x menos)
- interpola pra 16x16x78
- ✅ **4x mais rápido** (~65k ops/chunk)
- ⚠️ detalhe "blob" pode sumir (testar)
- ✅ visual: fica até MAIS smooth (interpolação ajuda)

### C) **Envelope early-exit vertical** (só processa Y=8 até Y=44)
- gaussianas centram em Y=12 e Y=40, sigma 7-11
- se Y<5 ou Y>50, envelope ≈ 0, **nunca é nuvem**
- loop com `if (y < Y_LOW_THRESHOLD || y > Y_HIGH_THRESHOLD) continue;`
- ✅ **50% menos pontos** (~130k ops/chunk)
- ✅ visual IDÊNTICO (só pula o que nunca teria nuvem mesmo)
- ⭐ **a otimização mais SEGURA** (não muda nada visual)

### D) **Menos oitavas, mais metaballs**
- tirar fractal 3 octaves → só 1 octava
- tirar Worley 2 → só Worley 1
- manter warp 1 chamada
- ✅ **2x mais rápido** (~130k ops)
- ⚠️ visual MENOS detalhado (mas nuvens redondas ficam legais)

### E) **Pré-calcular density em estrutura 3D** (chunk inteiro de uma vez)
- ao invés de calcular por chunk, **calcular UMA região grande 4x4 chunks** de uma vez
- economiza chamadas de noise (mesmo valor em chunks adjacentes)
- ✅ **2-3x mais rápido** na geração de chunks múltiplos
- ⚠️ primeiro chunk demora MAIS
- ✅ visual IDÊNTICO

### F) **Mudar geração para Surface Nets procedural** (3D density field)
- ao invés de colocar BLOCOS um a um, gerar uma **superfície 3D** (marching cubes-like)
- ✅ visual: **nuvens SUPER REDONDAS** (estilo vaporwave)
- ✅ "comestível" (player pode andar DENTRO)
- ❌ pesado se não otimizado (já tem SurfaceNetsCloud pra renderizar)
- ⭐ mais bonito MAS mais arriscado

### G) **Distance field + raymarch** (em vez de blocos!)
- gera nuvens como **field** (escalar por ponto)
- shader raymarch no cliente
- ✅ visual: **ULTRA smooth**, nuvens redondas perfeitas
- ✅ sem custo de "blocos", só cálculo
- ❌ PRECISA de shader custom (GLSL) — Minecraft 1.7.10 não suporta bem
- ❌ risco alto de não funcionar

### H) **Apenas cubos grandes, sem SurfaceNets** (chunk-art)
- 1 cubo GIGANTE por "bloco de nuvem" (8x8 ou 16x16)
- ✅ **8-64x mais rápido** (1 cubo = 1 op)
- ✅ "estilo pixelado retro" (combina com o papai)
- ⚠️ visual: quadradão, não redondo
- ⭐ seria estilo Skyblock / Vintage Story

### I) **Híbrido: blocos + SurfaceNets** (que já tem)
- mantém sistema atual
- otimiza SÓ o envelope (Y range)
- ✅ **2x mais rápido**, zero risco
- ⭐ **recomendação: fazer isso primeiro, depois pensar em mudar**

---

## 🟡 minha opinião (Nébula):

papai, se eu fosse VOCÊ, faria assim:

**PASSO 1** (5 min, zero risco): **otimização C** (early-exit do envelope vertical). isso é 1 linha de mudança, **2x mais rápido**, **visual IDÊNTICO**.

**PASSO 2** (15 min, baixo risco): **otimização B** (amostragem 2x2 + interpolação). **4x mais rápido**, visual fica mais smooth. precisa testar no jogo.

**PASSO 3** (1h, médio risco): **otimização F** (Surface Nets procedural). visual fica MUITO mais redondo, mas precisa mexer em MUITO código (chunk provider + renderer).

**PASSO 4** (se quiser, muito tempo): **otimização G** (raymarch shader). visual fica INCRÍVEL mas precisa de framework de shader que 1.7.10 não tem nativo.

**NÃO RECOMENDO** (a curto prazo):
- **A (cache por chunk)** — adiciona complexidade sem ganho visual
- **D (menos oitavas)** — perde detalhe bonito que o papai gosta
- **H (cubos grandes)** — perde a "redondeza" que o papai quer

**RESUMO**:
- ⭐ **se quer resultado HOJE sem risco**: PASSO 1 (envelope early-exit)
- ⭐⭐ **se quer melhor visual + bom ganho**: PASSO 2 (amostragem 2x2)
- ⭐⭐⭐ **se quer nuvens ULTRA redondas**: PASSO 3 (Surface Nets)
- ⭐⭐⭐⭐ **se quer nuvens INCRÍVEIS**: PASSO 4 (raymarch, longo prazo)

---

## 🟡 perguntas pro papai:

1. qual prioriza: **velocidade** ou **visual bonito**?
2. se velocidade: **Passo 1** (envelope early-exit, 2x mais rápido, visual idêntico)?
3. se visual: **Passo 3** (Surface Nets procedural, nuvens redondas, mas precisa recompilar)?
4. quer **combinar** (Passo 1 + Passo 2, ~6x mais rápido, visual mais smooth)?

— Nébula, 2026-08-08, brainstorm de nuvens 💛
