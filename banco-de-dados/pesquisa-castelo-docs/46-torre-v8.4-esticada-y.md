# Torre v8.4 — v8.3 esticada somente em Y

**Data:** 2026-07-16  
**Status:** teste puro de proporção vertical; candidata visual.

O Pai confirmou que a v8.3 tinha a melhor estrutura e telhado até agora:

- octógonos corretos;
- oito portas;
- ribs brancas;
- espaçamento e arquitetura corretos.

O erro restante era a razão altura/largura: precisava ser mais esticada em Y.

## Transformação

- X: intocado;
- Z: intocado;
- blocos/materiais: intactos;
- relações horizontais: intactas;
- Y multiplicado por **5/4 (1,25×)**;
- cada quatro camadas da v8.3 viraram cinco.

## Resultado

- v8.3: 25×25×94;
- v8.4: **25×25×117**;
- 11.623 blocos;
- um componente conectado;
- oito portas e oito faces preservadas;
- schematic gzip válido.

## Artefatos

- `build_torre_v8_4.py`
- `45-torre-v8.4-esticada-y.png`
- `torre-v8.4.schematic`
- SHA schematic: `847917a14ed466cb818c153776ec5a17f52294148266e9d54981f5514baec5bc`
- SHA turnaround: `ed087a08aca7dbc9c57ce3ae1645b8ac346bbbaacaaab8a6652c0d9e297b3568`

A v8.4 não substitui automaticamente a v8.3. Ela existe para o Pai julgar se
1,25× é a proporção vertical correta, exagerada ou ainda insuficiente.
