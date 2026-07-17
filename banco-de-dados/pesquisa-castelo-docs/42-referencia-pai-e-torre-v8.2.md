# Referência enviada pelo Pai e torre v8.2

**Data:** 2026-07-16  
**Status:** a imagem virou referência principal; v8.2 reprovada pelo Pai.

A v8.2 interpretou incorretamente a iluminação roxa como paleta, reduziu as ribs
de oito para quatro e manteve base/eixo com footprints diferentes. O eixo alto,
os medalhões redondos e o finial fino foram preservados como aprendizado.

## Arquivo original preservado

`38-referencia-torre-enviada-pelo-pai.png`  
SHA-256: `54ba3d1cdbb6b9e364691d124766d879d8c07ff4bb736d34eb259ba8879205bb`

## O que a imagem resolveu

- finial é uma esfera vermelha com agulha fina;
- topo tem curva côncava e nervuras finas escuras;
- eave é estreito, não uma borda branca dupla;
- eixo tem quatro módulos altos;
- cantos e cintas são roxo/vermelho escuro;
- medalhões são discos simples, não cruzes com centro amarelo;
- saia inferior tem oito nervuras sobre telha verde;
- base é octogonal com arcada contínua e plinth baixo;
- medição posterior com grade Y: base+saia 31%, eixo 38%, topo 31%. A estimativa antiga 27/52/21 estava errada e foi substituída.

Prancha anotada: `39-analise-referencia-pai.png`.

## Alterações v8.2

Sem alterar 21×21×79 nem a base aprovada:

- medalhões viraram discos 3×3 sem cantos;
- cintas e nervuras passaram a roxo escuro;
- nervuras do shaft também ficaram escuras;
- eaves brancos foram reduzidos a uma camada;
- telhados mantiveram o verde com ribs finos;
- finial ganhou esfera vermelha e iron bars como agulha real;
- renderer de turnaround passou a representar blocos finos.

## Validação

- 5.287 blocos;
- dimensões 21×21×79;
- um único componente conectado;
- schematic gzip válido.

## Artefatos

- `build_torre_v8_2.py`
- `40-torre-v8.2-referencia-pai.png`
- `41-comparacao-referencia-pai-v8.2.png`
- `torre-v8.2.schematic`
- SHA schematic: `f92fe033ce05f9c45ba2fc49172f69cb9cfc7b26566bec328e5065048ca9b903`
- SHA turnaround: `c8a9dee384c744aff625bd45b1d7ffe83d033eca948310f02c66a85f095cca6a`

## Pendência visual

A referência sugere arcadas também nas faces diagonais da base. A v8.2 ainda
mantém arcadas completas somente nas quatro faces principais. Não replicar até
o Pai avaliar a nova linguagem de telhado/eixo.
