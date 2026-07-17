# Auditoria da torre v5 e candidata v6

**Data:** 2026-07-16  
**Status:** v5 e v6 reprovadas como módulos definitivos.

> Atualização após o olhar do Pai e releitura das referências pesadas: a v6
> continuou desfigurada. A premissa hexagonal estava errada; a torre correta é
> um quadrado chanfrado/octógono, com cerca de 65 blocos e quatro módulos altos.
> Ver `30-estudo-silhueta-torre.md`.

## Por que auditar antes de continuar o palácio

A torre seria replicada quatro vezes. Um erro pequeno nela viraria quatro erros e
contaminaria muro, corredores e escala do salão. O script v5 já gerava um
schematic, mas nunca tinha sido renderizado diretamente dos voxels para comparar
o documento com o resultado real.

## Torre v5 — resultado real do gerador

Arquivo histórico preservado: `build_torre.py`.

- estrutura sem gramado: **2.646 blocos**;
- footprint real: **17×15**, embora o documento declarasse base 14;
- altura: **38**, correta;
- quatro andares e medalhões aparecem principalmente na fachada norte;
- traseira e laterais viram um eixo branco quase contínuo;
- função `hex_fill` produz um perfil “gordo”, visualmente próximo de caixa;
- três camadas brancas superiores + telhado cheio deixam o topo muito cúbico.

Evidências exatas:

- `23-torre-v5-voxel-real-preview.png`
- `24-torre-v5-turnaround-voxel-real.png`
- `torre.schematic`

A v5 continua preservada; não deve ser replicada quatro vezes.

## Torre v6 — correção isolada

Novo arquivo: `build_torre_v6.py`.

Mudanças:

1. perfis horizontais simétricos definidos por largura, profundidade e face;
2. footprint máximo reduzido para **15×11**, mais perto da base declarada;
3. seis pilares nos seis vértices do perfil;
4. quatro andares separados por anéis vermelhos em todo o perímetro;
5. seis medalhões R-Y-R por andar, não apenas na face norte;
6. pontas brancas nas primeiras camadas dos telhados;
7. porta norte 3×4 preservada;
8. v5 não foi sobrescrita.

Resultado:

- estrutura sem gramado: **1.269 blocos**;
- footprint: **15×11×38**;
- branco: 607;
- verde: 408;
- vermelho: 228;
- glowstone: 26;
- componente estrutural único: 1.269 blocos conectados;
- perfis simétricos em X/Z;
- porta 3×4 aberta;
- schematic gzip válido.

Artefatos:

- `25-torre-v6-turnaround-hexagonal.png`
- `torre-v6.schematic`
- SHA-256 schematic: `8729da8184ab74d7f42b5ab416d2c119bdd3cfcd16c3cfbbf20ea17709afe09f`
- SHA-256 preview: `95b8e20c78c1bcd8fc9ce588542b8417e82346a81ee8994ab46b504070756b9b`

## O que ainda não está aprovado

A v6 corrige coerência geométrica, mas pode estar vermelha demais no eixo e o
topo ainda pode precisar de mais curva/projeção. Não chamar de torre final sem o
olhar do Pai.

## Próximo passo correto

- se o volume v6 estiver no caminho certo: ajustar somente paleta/densidade dos
  anéis e congelar o módulo;
- depois replicar quatro torres numa planta v1;
- só então construir o salão central e corredores.
