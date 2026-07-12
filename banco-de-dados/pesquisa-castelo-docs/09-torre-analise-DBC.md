# Análise da Torre DBC — camada por camada

Extraído do palácio hardcoded no Dragon Block C (mod DBZ 1.7.10). Torre NE (X=102, Z=26).

## Legenda de blocos
- `W` = branco (paredes)
- `R` = vermelho (molduras/anéis)
- `G` = verde (telhados/detalhes)
- `M` = marrom (madeira/vigas)
- `B` = azul cristal (cristais)
- `.` = ar

## Estrutura por altura

### 🏛️ **BASE MACIÇA** (Y=0)
Diamante/octógono 11×11 preenchido — plataforma sólida branca. **É a fundação.**

### 🏛️ **ANDAR TÉRREO OCO** (Y=1 a Y=4) — 4 blocos altura
Formato octogonal HOLLOW (só paredes, interior aberto), 9×9 externo.
- Y=1-2: paredes brancas com **pilares vermelhos R nos 4 cantos das faces**
- Y=3: paredes normais + **anéis R no topo das faces** (moldura)
- Y=4: fechamento branco

### 🟢 **PRIMEIRO BEIRAL** (Y=5) — 1 bloco
**Se estende pra 11×11** (mais largo que o andar embaixo), com blocos VERDES nas 4 pontas cardinais formando "abas" projetadas. Fim da base larga.

### 🟢 **TELHADO-SAIA** (Y=6 a Y=7) — 2 blocos
- Y=6: 9×9 verde (estreita 1)
- Y=7: 7×7 verde (estreita mais 1)

### 🔴 **COLAR VERMELHO** (Y=8) — anel de transição
5×5 sólido:
```
.RRR.
RMMMR
RM.MR
RMMMR
.RRR.
```
Molduras vermelhas ao redor, MARROM no meio (piso/viga), buraco central.

### 🏢 **EIXO — MÓDULO REPETIDO** (Y=9 a Y=23) — 15 blocos altura = 3 andares idênticos
5×5, cada andar tem 5 blocos altura, PADRÃO SE REPETE:

```
Andar tipo A (5 blocos):
Y+0: .WWW.    ← teto branco
     W...W
     R...R
     W...W
     .WWW.

Y+1: .WWW.    ← janela superior (R nas laterais)
     R...R
     .....    ← centro vazio
     R...R
     .WWW.

Y+2: .WWW.    ← janela inferior
     W...W
     R...R
     W...W
     .WWW.

Y+3: .RRR.    ← ANEL MOLDURA vermelho + marrom (viga entre andares)
     RMMMR
     RM.MR
     RMMMR
     .RRR.

Y+4: .WRW.    ← início do próximo andar (R nas laterais = medalhão)
     W...W
     W...W
     W...W
     .WRW.
```

**IMPORTANTE**: os R nos meios das faces são os **medalhões vermelhos** — não são cruzes grandes 3×3 como eu tinha desenhado, são só **2 blocos R centrais na parede** vistos de fora.

### 🟢 **BEIRAL SUPERIOR** (Y=24) — anel final antes do telhado
5×5 completo em branco.

### 🟢 **TELHADO CÔNICO** (Y=25 a Y=27) — 3 blocos altura
- Y=25: **7×7 EXPANDIDO** (beiral projetado), G nas bordas, W nas quinas
- Y=26: 5×5 G com quinas W
- Y=27: 3×3 G puro

### ⭐ **PINÁCULO** (Y=28 a Y=30)
- Y=28: 1 bloco R (esferinha vermelha)
- Y=29: 1 bloco W
- Y=30: 1 bloco W (antena de topo)

## Dimensões finais

| Seção | Y | Largura frontal | Detalhe |
|---|---|---|---|
| Base maciça | 0 | 11 | plataforma |
| Andar térreo | 1-4 | 9 | 4 pilares R, oco |
| Beiral 1 | 5 | 11 | com abas verdes |
| Saia | 6-7 | 9→7 | telhado 2 degraus |
| Colar | 8 | 5 | R+M anel |
| Eixo (3 andares) | 9-23 | 5 | módulo repetido 5 blocos cada |
| Beiral topo | 24 | 5 | anel W |
| Telhado cônico | 25-27 | 7→5→3 | 3 camadas |
| Pináculo | 28-30 | 1 | R + WW |

**ALTURA TOTAL: 31 blocos** (DBC é compacto — na versão Zixxter deve ser proporcional maior)

## Padrão de repetição do eixo (KEY INSIGHT)

Cada "andar" do eixo tem **exatamente 5 blocos de altura**:
- 3 blocos parede com janelas R
- 1 anel vermelho + marrom (moldura + viga)
- 1 topo branco

Isso significa que pra fazer um eixo com **N andares**, é só repetir esse módulo N vezes.

## Como escalar pra Zixxter

Se a Zixxter é ~2× maior, cada seção precisa **dobrar em altura E largura**:
- Base 11 → 22
- Eixo 5 → 10, andar 5 → 10 blocos
- 3 andares → 4 ou 5 andares
- Altura total 31 → ~50-60 blocos

Isso bate com o que eu observei da vista frontal do Zixxter (~44-50 blocos altura).
