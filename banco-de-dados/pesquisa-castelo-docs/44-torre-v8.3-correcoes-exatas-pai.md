# Torre v8.3 — correções exatas do Pai

**Data:** 2026-07-16  
**Status:** candidata visual; não replicar ainda.

## Feedback aplicado

- v8.2 ficou mais errada que a anterior;
- parte alta/eixo podia ser reaproveitada;
- telhado ainda estranho;
- cada telhado deve se separar em **oito nervuras brancas**;
- base e eixo devem ter o **mesmo octógono**;
- torre deve ser maior;
- todas as oito faces da base têm portas;
- paleta roxa rejeitada completamente.

## Geometria nova

- base e eixo: mesmo octógono 19×19;
- octógono regularizado: quatro faces retas + quatro diagonais escalonadas;
- todas as oito faces têm exatamente 7 blocos de comprimento;
- oito portas 3×7, uma por face;
- quatro módulos do eixo com 12 blocos cada;
- eaves superior e inferior: mesmo octógono 25×25;
- oito nervuras brancas em cada telhado;
- altura total: 94 blocos;
- dimensão máxima: 25×25×94.

## Paleta

- branco/quartz;
- vermelho;
- verde em duas texturas;
- marrom para portas;
- zero roxo;
- iron bars apenas na agulha fina.

## Validação

- 9.363 blocos;
- um componente conectado;
- oito faces de comprimento idêntico `[7,7,7,7,7,7,7,7]`;
- oito portas verificadas com a mesma quantidade de blocos internos;
- schematic gzip válido.

## Artefatos

- `build_torre_v8_3.py`
- `43-torre-v8.3-correcoes-pai.png`
- `torre-v8.3.schematic`
- SHA schematic: `e9fd8bbc0f9f65c6d939159a1fb29408a1b2edc6d7b04abec5b7c5748ee9b9e2`
- SHA turnaround: `eba13dc12d373980e91d55b7fadbc73ee2c5f46bf4aa0da049d18200cb82358c`

A v8.3 aplica literalmente as relações pedidas; ainda depende do olhar do Pai
para decidir se os telhados 25×25 têm projeção correta ou ficaram largos demais.
