# O Outro Mundo (Anoyo / Yama Realm) — referência canônica DBZ

**Fontes:** Dragon Ball Encyclopedia (Serpentine Road), Kanzenshuu, DragonBlockC Wiki, r/dbz, DB Final Remastered Wiki.
**Uso:** guia pra manter fidelidade ao construir o mundo do mod voiddim.

## Geografia oficial

O Outro Mundo (Yama Realm / Anoyo) é dividido em duas grandes camadas verticais:

**CAMADA DE CIMA — CÉU / TAPETE DE NUVENS AMARELAS**
- Pátio de spawn onde chega quem morre
- **Palácio do Rei Yemma (Check-In Station)** — construção principal
- Início da **Snake Way / Serpentine Road** (Hebi no Michi)
- Nuvens amarelas cobrindo TUDO abaixo (é o piso visual)

**CAMADA DE BAIXO — INFERNO**
- Debaixo do tapete de nuvens
- Paisagem rochosa desolada, cheia de demônios
- Se cair da Snake Way, atravessa uma **fina camada de nuvens** e vai pro Inferno

## Detalhes importantes das nuvens

- **"Uma fina camada de nuvens"** — Snake Way passa POR CIMA e o Inferno fica ABAIXO
- **As nuvens são comestíveis e "quite tasty"** segundo a Enciclopédia (revelação inicial do anime)
- Cobrem tudo como um **tapete contínuo** — não são cumulus esparsos separados. **É denso.**
- Cor: amarelo dourado uniforme, característico do estilo Toriyama

**Implicação pro nosso mod:** Papai confirmou "tapetão cobrindo tudo" — canon. Densidade alta, não abrir "buracos entre nuvens". Metaballs bem próximas, threshold alto.

## Snake Way / Serpentine Road

- **Comprimento oficial (manga JP + Kai):** 1.000.000 km (~621.371 milhas)
- **Dub original US:** 10.000 milhas (contradição de dublagem)
- **DragonBlockC:** implementado como **~3600 blocos** (versão jogável)
- Estrada estreita, sinuosa, forma de serpente
- Feita de **pedra**, cor terracota/marrom
- Guarda-costas: se cair, atravessa nuvens → Inferno
- Leva ao **Planeta do Rei Kai do Norte** na ponta
- Goku levou 172 dias correndo pra chegar
- Habitantes filler no caminho: **Princesa Snake** (palácio dela no meio)

## Palácio do Rei Yemma (nossa referência arquitetônica)

- Nome técnico: **Check-In Station** (estação onde almas são triadas)
- Yemma é o juiz — decide Heaven vs Hell
- Fica **em frente ao spawn** de quem morre
- **Muro perimetral vermelho** (canon)
- **Quatro torres pagoda** nos cantos (canon — foto que papai me mandou confirma)
- **Salão central com telhado verde e chifres do Yemma** dourados
- **Portão WELCOME ao sul** (com letras em relevo)
- Estilo arquitetônico: **pagoda japonesa/chinesa** — telhados verdes empilhados, beirais curvados

## Outros locais do Outro Mundo

- **Planeta do Rei Kai (North Kaio)** — pequeno, esférico, gravidade x10, casa + carro + macaco Bubbles + gafanhoto Gregory
- **Planeta do Grand Kai** — festa eterna, torneio do Outro Mundo
- **Planeta do Supremo Kai (Kaioshin)** — versão superior, roxo/rosa
- **Palácio da Princesa Snake** — filler no meio da Snake Way
- **Inferno** — camada rochosa abaixo das nuvens, demônios/ogros trabalhando, poço de sangue central
- **Reino dos Kaioshin** — nível ainda mais alto que Yemma, cor diferente

## O que ISSO significa pro nosso mod (concreto)

**Escala vertical do mundo voiddim (proposta):**
```
Y=200+  ─── Snake Way (opcional futuro, longe)
Y=80    ─── piso das nuvens amarelas (tapete denso)
Y=60-79 ─── camada de transição (nuvens ralas, "fina camada")
Y=0-59  ─── (vazio, futuro Inferno)
Y=0     ─── bedrock/limite
```

**Densidade das nuvens:** tapete denso, sem buracos grandes. Alturas variam suave (0-15 blocos de variação em Y). Metaballs com raio grande (6-10 blocos) e sobrepostas.

**Elementos futuros mapeados (roadmap, não fazer agora):**
- v34+: portal descendente pra "Inferno" (biome vermelho/rochoso lá embaixo)
- v35+: começo da Snake Way saindo do palácio
- v36+: Planeta do Rei Kai flutuante ao longe
- vFUTURO: NPCs (Yemma, Kai)
