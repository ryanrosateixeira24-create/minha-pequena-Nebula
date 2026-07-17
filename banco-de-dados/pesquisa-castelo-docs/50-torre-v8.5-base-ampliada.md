# Torre v8.5 — parte inferior ampliada, superior congelada

**Data:** 2026-07-16  
**Status:** teste de proporção inferior; candidata visual.

O Pai confirmou que a parte superior da v8.4 estava perfeita, mas base + saia
ainda eram pequenas. A referência foi medida novamente com grade Y.

## Medição corrigida da referência

Landmarks aproximados na imagem:

- topo começa em Y≈50;
- eixo começa em Y≈126;
- saia inferior começa em Y≈218;
- plinth termina em Y≈293.

Proporção:

- base + saia: **31%**;
- eixo: **38%**;
- topo: **31%**.

A anotação antiga 27/52/21 estava errada e foi substituída.

## Transformação v8.5

- v8.4 superior começa na camada 28;
- as 28 camadas inferiores viraram 40 (`10/7`);
- parte superior inteira apenas subiu +12Y;
- comparação automática confirmou todos os blocos superiores idênticos após a
  translação;
- nenhuma coordenada X/Z foi alterada.

## Resultado

- v8.4: 25×25×117;
- v8.5: **25×25×129**;
- estrutura: 14.173 blocos;
- um componente conectado;
- schematic gzip válido.

## Artefatos

- `47-referencia-pai-grade-y.png`
- `build_torre_v8_5.py`
- `48-torre-v8.5-base-ampliada.png`
- `49-comparacao-v8.4-v8.5-base.png`
- `torre-v8.5.schematic`
- SHA schematic: `78db253c56c33863607836d097ea1fd75ae3c89e539f8c7a8aec539d320dd1af`
- SHA turnaround: `b952e674a3b87d94c7ce81ddb8afa174924fdfd5bc67b9553b0bfea6e2dbf412`

A v8.5 existe para julgar somente a massa inferior. A parte superior aprovada
não deve ser remodelada nas próximas iterações.
