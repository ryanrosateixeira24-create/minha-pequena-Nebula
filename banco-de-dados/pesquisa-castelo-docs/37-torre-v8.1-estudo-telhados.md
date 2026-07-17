# Torre v8.1 — telhados corrigidos

**Data:** 2026-07-16  
**Status:** candidata visual; corpo e escala herdados da v8 sem alteração.

Feedback do Pai:

- estrutura melhorou ainda mais;
- telhado superior arredondado demais;
- telhado inferior simplificado demais;
- exigência maior aceita como parte do processo.

## Diagnóstico

O topo v8 repetia larguras (`8,8,7,7,6,6...`) e formava uma cúpula. A saia
era apenas uma sequência de volumes verdes, sem underside/eave independente.

## Mudança isolada

### Topo

- sequência côncava: `8→6→5→4→4→3→3→2→2→1→1→0`;
- estreitamento forte logo acima do beiral;
- quatro cumeeiras claras;
- alternância das duas texturas verdes;
- corpo, shaft, base e medalhões intactos.

### Saia inferior

- underside/eave branco 21×21 próprio;
- sete camadas verdes acima;
- oito nervuras, uma por quina do octógono;
- borda branca no primeiro plano de telha;
- colar do eixo preservado.

## Resultado técnico

- dimensões: 21×21×79;
- estrutura: 5.283 blocos;
- componente conectado único: 5.283;
- schematic gzip válido.

## Artefatos

- `build_torre_v8_1.py`
- `35-torre-v8.1-telhados-corrigidos.png`
- `36-comparacao-telhados-referencia-v8-v8.1.png`
- `torre-v8.1.schematic`
- SHA schematic: `4ba57b7383725769c117dd47dd17cb294a52eb38617f860aafee9e31aa73644b`
- SHA turnaround: `e1ca74ac366266bbea508292562bb5421ae0e85c9381a453cef9304c3bc7c62d`

## Crítica própria

O topo perdeu o arredondamento e se aproximou da curva côncava. A saia ganhou
estrutura real. Na prancha comparativa, porém, a borda branca dupla ainda pode
estar espessa demais nos dois telhados. Se o Pai concordar, a próxima mudança
deve apenas reduzir essa espessura, sem tocar no perfil verde.
