# Torre v8.6 — parte inferior larga em X/Z

**Data:** 2026-07-16  
**Status:** candidata de silhueta; superior congelado.

O Pai esclareceu que “parte baixa pequena” significava largura, não altura. A
v8.5, que aumentou Y inferior, foi uma interpretação errada e permanece apenas
como comparação.

## Medição horizontal da referência

- eixo: aproximadamente 47 px;
- telhado inferior: aproximadamente 91 px;
- relação: `91/47 ≈ 1,94×`.

Na v8.4:

- eixo: 19 blocos;
- telhado inferior: 25 blocos;
- relação: `25/19 ≈ 1,32×`.

## Alteração v8.6

A altura da v8.4 foi restaurada integralmente.

- eixo: 19 blocos, intacto;
- telhado inferior: **37 blocos**;
- relação: `37/19 ≈ 1,947×`;
- plinth: 31×31;
- eave inferior: 37×37;
- dez degraus afunilando até 19×19;
- oito nervuras brancas;
- todos os blocos em Y>=28 são bit a bit idênticos à v8.4.

## Resultado

- dimensões: 37×37×117;
- estrutura: 15.091 blocos;
- um componente conectado;
- schematic gzip válido.

## Artefatos

- `build_torre_v8_6.py`
- `51-torre-v8.6-base-larga.png`
- `torre-v8.6.schematic`
- SHA schematic: `4c75b145626ceeedb36d5fa91c0ed7c0513fdbd6def229af9d7eee10bb74b223`
- SHA turnaround: `dda6b813ab3b8491cb3440684633aa24ae535afc478204ecef4c17875ae60fbf`

A v8.6 testa somente a largura da saia/plinth. Nenhuma decisão da torre superior
aprovada foi remodelada.
