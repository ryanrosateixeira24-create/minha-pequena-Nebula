# Torre v8.7 — térreo largo e telhado inferior travado

**Data:** 2026-07-16  
**Status:** candidata visual; telhado inferior congelado em 35×35.

Feedback do Pai sobre v8.6:

- telhado estava perfeito, precisava apenas ficar levemente menor;
- telhado devia ser travado;
- a construção abaixo continuava pequena porque só a cobertura havia crescido.

## Alterações

- telhado inferior: 37×37 → **35×35**, agora travado;
- térreo: 19×19 → **31×31**;
- plinth: 31×31 → **33×33**;
- base e eixo continuam octogonais, mas o térreo agora tem massa própria;
- oito faces do térreo têm 11 blocos cada;
- oito portas aumentaram para cinco blocos de abertura visual;
- altura voltou/permaneceu exatamente a da v8.4;
- tudo em Y>=28 continua bit a bit idêntico à parte superior aprovada.

## Resultado

- dimensões máximas: 35×35×117;
- 15.263 blocos;
- um componente conectado;
- faces `[11,11,11,11,11,11,11,11]`;
- oito portas com contagem idêntica;
- schematic gzip válido.

## Artefatos

- `build_torre_v8_7.py`
- `53-torre-v8.7-terreo-largo.png`
- `torre-v8.7.schematic`
- SHA schematic: `3c4a2b546b18f26607e759f8fc8aaebf561216eed040e083db2204b80ec9a7c8`
- SHA turnaround: `680a505cd41f799f998f895a38a115a04ef57e222f404f89cd7369064dc82e49`

O telhado inferior não deve mais ser remodelado sem pedido explícito do Pai.
