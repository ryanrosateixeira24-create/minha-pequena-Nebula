# Estudo de silhueta da torre — correção antes da v7

**Data:** 2026-07-16  
**Decisão:** parar de modelar até entender forma e proporção. V5 e V6 não são módulos finais.

## Material relido de verdade

O pacote pesado do relatório foi materializado e validado por SHA-256. Foram
comparados:

- torre isolada do anime/build de referência;
- turnaround preciso em seis ângulos;
- vista frontal do complexo;
- planta geral;
- referência dos telhados em camadas;
- código `tower_v9.py` do relatório, executado e renderizado nos mesmos quatro
  ângulos das nossas versões.

Pranchas:

- `27-estudo-referencias-torre.png`
- `28-torre-v9-relatorio-turnaround.png`
- `29-comparacao-silhueta-referencia-v5-v6-v9.png`

## Correção principal: não é hexágono

As notas antigas diziam hexágono, mas o turnaround em seis vistas mostra um
**quadrado chanfrado / octógono**:

- quatro faces principais largas;
- quatro faces diagonais curtas nos cantos;
- a base repete a mesma linguagem em escala maior.

O `tower_v9.py` também usa explicitamente `chamfered_square`, confirmando a
leitura. A v6 hexagonal partiu de uma premissa errada.

## Proporção correta

A torre de referência é muito mais alta e esbelta que v5/v6. O código v9 fornece
uma régua coerente:

- footprint máximo: aproximadamente 17×17;
- altura total: aproximadamente 65–66 blocos;
- base de entrada: 7 blocos;
- saia + colar: 7 blocos;
- eixo: **4 módulos de 9 blocos = 36 blocos**;
- eave/telhado/pináculo: cerca de 15 blocos.

Mais da metade da altura está no eixo. V5/v6 comprimiam os quatro andares em só
12–13 blocos e terminavam com 38 blocos totais, por isso pareciam atarracadas.

## Linguagem visual correta

### Base

- octogonal/chanfrada e larga;
- paredes brancas;
- arcadas verticais vermelhas;
- entrada central iluminada;
- plinth branco baixo;
- não deve parecer uma caixa quadrada.

### Telhado-saia

- inclinação alta e contínua em várias camadas;
- verde dominante;
- nervuras/pontas brancas seguindo quinas reais;
- nada de laje verde larga e quase plana.

### Eixo

- branco dominante;
- quatro andares altos e claramente separados;
- anéis vermelhos **finos**, horizontais;
- medalhão vermelho pequeno e centralizado em cada face principal;
- cantos claros/quartz, não pilares vermelhos contínuos;
- muito espaço branco ao redor do medalhão.

### Topo

- beiral branco fino e projetado;
- telhado verde alto, afunilando suavemente;
- pináculo estreito vermelho/branco;
- não usar capitel branco cúbico grande.

## O que cada tentativa ensinou

### V5

- altura correta apenas segundo a nota antiga, mas errada contra a referência;
- quase quadrada;
- detalhes só na fachada norte;
- traseira vazia.

### V6

- simetria melhor;
- forma hexagonal errada;
- vermelho excessivo;
- ainda atarracada;
- não continuar esse ramo.

### V9 do relatório

- escala e topologia muito mais próximas;
- quatro módulos de 9 blocos são a base correta;
- porém não copiar sem crítica:
  - pontas brancas flutuam fora do telhado em alguns cantos;
  - medalhões e cintas ficam grandes/escuros demais no renderer voxel;
  - roof precisa de uma curva escalonada mais limpa.

## Especificação para a futura v7

1. octógono por `chamfered_square`;
2. 65 blocos de altura como alvo inicial;
3. base ~17×17, eixo ~9×9;
4. quatro módulos de 9 blocos;
5. branco dominante, vermelho só em cintas/arcadas/medalhões;
6. saia de seis camadas e topo de oito–nove camadas;
7. detalhes em quatro faces principais, com diagonais mais discretas;
8. gerar frontal, lateral, traseira e diagonais diretamente dos voxels;
9. comparar a silhueta antes de exportar schematic;
10. não replicar no palácio até aprovação visual do Pai.

## Próximo passo

Produzir uma blueprint ortográfica v7 — frente, lado e topo — usando essas
proporções, **antes** de escrever outro gerador de blocos.
