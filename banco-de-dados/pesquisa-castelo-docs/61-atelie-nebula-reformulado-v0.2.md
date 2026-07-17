# Ateliê Nébula v0.2 — reformulação após rejeição da cópia literal

**Data:** 2026-07-16

O Pai rejeitou corretamente a v0.1:

> “Tá basicamente uma cópia barata [...] até as formas são as mesmas.”

## Destino da v0.1

- projeto movido para `mod/experimental/nebula-atelier-v0.1-fork-literal-reprovado/`;
- JAR e fontes movidos para `arquivo-morto/atelie-nebula-v0.1-fork-literal-reprovado/`;
- nada apagado;
- continua servindo como prova de toolchain, MIT e compilação, não como produto.

## Direção v0.2

Preservar capacidades, não catálogo:

- material dinâmico;
- rotação/espelhamento;
- cantos e encontros;
- coberturas, arcos, colunas, painéis e grades;
- propriedades herdadas do material.

Nova composição:

```text
família + perfil + topologia + acabamento
```

Exemplo:

`Cobertura + Pagoda Côncava + Canto Externo + Nervura Clara`

## Primeiros perfis próprios

- Linear 45°;
- Suave 33°;
- Pagoda Côncava;
- Ponta Ascendente.

Topologias geradas:

- reta;
- canto externo;
- canto interno;
- cumeeira;
- vale;
- beiral;
- ponta.

## Interface própria

A Mesa de Moldes abandona a grade de 83 ícones. O jogador escolhe material,
família, perfil e topologia ao redor de uma prévia 3D central.

Pranchas:

- `/home/user/mod/nebula-atelier/concepts/01-linguagem-coberturas.png`
- `/home/user/mod/nebula-atelier/concepts/02-mesa-de-moldes.png`

## Estado

A primeira geometria paramétrica já existe fora do Minecraft:

- quatro perfis originais;
- reta, canto externo e canto interno;
- 12 malhas OBJ;
- 6.936 vértices;
- 13.056 faces;
- bordas equivalentes e determinismo testados;
- zero modelo `.smeg` reutilizado.

Prancha: `concepts/03-matriz-parametrica-renderizada.png`.

## Primeiro JAR de teste

O Pai escolheu participar como primeiro tester. Foi criado um protótipo mínimo:

- catálogo visível com somente 12 formas novas;
- quatro perfis × reta/canto externo/canto interno;
- 12 modelos gerados por código, sem modelos visíveis antigos;
- engine MIT apenas como infraestrutura temporária de material/orientação;
- Java 7 major 51;
- ASM 1484 OK / 0 BAD;
- LICENSE/NOTICE incluídos.

A v0.2.0 falhou no pre-init por carregamento ansioso de janelas e foi arquivada.
A v0.2.1 abriu no Minecraft, mas revelou winding invertido: curvas eram
cortadas pelo backface culling. Também faltava aba criativa própria.

Runtime corrigido:
`/home/user/Atelie-Nebula-v0.2.2-prototype-mc1.7.10.jar`

SHA-256:
`1733bf9478ec7858e8020dbe2d5a6c3bfa48c95b18989d7d534c5cb6b0e61200`

O Pai confirmou que a v0.2.2 ficou ótima no runtime e pediu uma proposta completa,
não apenas 12 telhados. A geometria/winding e a aba criativa estão aprovadas.
A direção agora é v0.3: HUD `Livro de Moldes`, cinco famílias e 52 combinações
significativas próprias. Ver `docs/PROPOSTA-COMPLETA-V0.3.md` e
`concepts/04-hud-livro-de-moldes.png`.
