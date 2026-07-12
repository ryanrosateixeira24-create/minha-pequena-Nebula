# Análise do Salão Central — DBC

Extraído dos 906 blocos do salão + entorno.

## Dimensões

- **Largura X**: 24 blocos (X=63 a X=86 sendo o footprint principal)
- **Profundidade Z**: 17 blocos (Z=47 a Z=63)
- **Altura Y**: 17 blocos (Y=5 a Y=21)
- **Centro**: X=74, Z=55

## Vista de topo — telhado escalonado em LOSANGO/DIAMANTE

O telhado do salão NÃO é retangular chapado — é um LOSANGO com cantos cortados:

```
        WGGGGGGGGGGGGGGGGGGGGGGW       ← andar mais alto (24 wide, apenas topo visível)
        GWGGGGGGGGGGGGGGGGGGGGWG       ← reduzindo 1 bloco em cada lado
        GGWGGGGGGGGGGGGGGGGGGWGG       ← camadas escalonadas em diamante
        GGGWGGGGGGGGGGGGGGGGWGGG
        GGGGWWGGGGGGGGGGGGWWGGGG       ← beiral acentuado
        ...
```

Isso é o **telhado piramidal** visto do alto. Cada linha é 1 bloco mais estreita = camadas do telhado.

## Corte transversal (visto de frente Y×X)

```
Y=21 |            .RRRR.          ← chifres brancos + esfera vermelha
Y=20 |            WGGGGGGW        ← camada 1 do telhado piramidal (7 larg)
Y=19 |            .G......G.
Y=18 |            .G......G.       ← telhado se abrindo
Y=17 |            G........G
Y=16 |           G..........G
Y=15 |          G............G
Y=14 |         GG..............GG
Y=13 |         W..............W   ← parede lateral do salão
Y=12 |
Y=11 |         W..............W   ← janela? ou pilar?
Y=10 |     R.MMBBBBBBBBBBMM.R    ← FAIXA de vidro azul (janelas circulares!)
Y= 9 |    G..................G
Y= 8 |    G..................G
Y= 7 |    G....................G
Y= 6 |  GGMMMGGGGGMMGG         ← base larga do salão (com detalhes marrom)
Y= 5 |  R..........R           ← início do corredor conectando
Y= 0 |  WWWWMMMMMMWWWWWWWW    ← chão (piso alterna madeira/branco)
```

## Detalhes importantes

### 🪟 Janelas circulares azuis
No Y=10 tem uma faixa `BBBBBBBBBB` = **10 blocos de vidro azul cristal em fileira**. Isso é a **fileira de janelas** icônica do palácio (as janelas redondas laranja no anime — no DBC ficaram azul cristal).

### 🏛️ Telhado piramidal (LOSANGO)
O telhado principal é feito de **camadas retangulares que estreitam 1 bloco de cada lado a cada altura**, formando pirâmide escalonada.

### 🌟 Chifres no topo
Y=21 tem os **4 chifres brancos + esfera vermelha** central.

### 🔨 Corredores
- **Y=5-6, Z=55** tem faixas GGGG de ~30 blocos = corredores conectando salão às torres laterais
- Muito longos (correm no eixo X)

### 🪑 Altares menores frente/trás
- Frente (Z=38): pequena estrutura WGGW (uma varanda?)
- Trás (Z=73-75): estrutura maior WGGGGGGW × 3 linhas (altar ou entrada traseira)

## Escala DBC vs anime/Zixxter

DBC: salão 24×17×17 blocos.
Zixxter (canon anime): provavelmente 2× = **48×34×34 blocos**.

Isso faria o salão dominar visualmente o palácio, como no canon.
