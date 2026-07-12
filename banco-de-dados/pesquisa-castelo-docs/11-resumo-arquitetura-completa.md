# Arquitetura completa do Check-In Station — extraída do DBC

**Fonte:** Dragon Block C v1.4.85, classes `ChkInSt.class` + `ChkInStP2.class`, 11.888 blocos hardcoded.

## Escala geral

| Dimensão | DBC | Zixxter (2×) estimado |
|---|---|---|
| Largura X | 148 | ~300 |
| Profundidade Z | 130 | ~260 |
| Altura Y | 31 | ~60 |
| Total blocos | 11.888 | ~95.000 (2³ = 8× volumétrico) |

## Componentes identificados

### 1. 🏛️ TORRES (4 unidades)
- **posições exatas**: (47,26), (102,26), (47,84), (102,84) — retângulo 55×58
- **dimensão**: 11×11 base, altura 31
- **estrutura vertical** (camada por camada):
  - Y=0: plataforma sólida 11×11
  - Y=1-4: andar térreo oco 9×9 com portas em arco + pilares R nas quinas
  - Y=5: primeiro beiral 11×11 com pontas G projetadas
  - Y=6-7: telhado-saia 9×9 → 7×7 verde
  - Y=8: colar 5×5 (R+M anel de transição)
  - Y=9-23: eixo 5×5, **3 andares idênticos de 5 blocos cada**
    - módulo: 3 paredes brancas c/ janelas R laterais + anel R+M + topo W
  - Y=24: beiral topo 5×5 branco
  - Y=25-27: telhado cônico 7→5→3 verde
  - Y=28-30: pináculo (R+W+W)

### 2. 🏯 SALÃO CENTRAL
- **centro**: X=74, Z=55
- **dimensão**: 24×17 base, altura 17 (Y=5 a Y=21)
- **estrutura vertical**:
  - Y=5-6: base larga (contém detalhes M/marrom, alterna GGGMMMGG)
  - Y=7-9: paredes recuando com G aos lados
  - Y=10: **FILEIRA DE JANELAS AZUL CRISTAL** (10 blocos B em linha) ← icônico
  - Y=11-13: parede branca + anel superior
  - Y=14-20: **TELHADO PIRAMIDAL ESCALONADO** (losango que estreita 1 bloco/camada)
  - Y=21: chifres brancos + esferinha vermelha central

### 3. 🚪 PORTÕES LATERAIS (2 unidades — leste e oeste)
- **maiores do que parecem**: 11 largura × 35 profundidade
- **telhado plano verde** com muitos blocos (não portão pequeno)
- **são praticamente segundos edifícios** — varandas cobertas gigantes
- ficam DENTRO do muro do palácio (conectam torres traseiras às frontais)

### 4. 🚪 PORTÃO FRONTAL (WELCOME)
- **posição**: X=66-83, Z=100-109
- **dimensão**: 18×10, altura 8
- **telhado verde plano** com detalhes W (que devem ser onde vai a placa "WEL COME")

### 5. 🧱 MURO PERIMETRAL
- vermelho, retangular, envolve todo o complexo
- altura baixa (~4 blocos)
- meta 14 (vermelho) dominante

## Paleta (metas do BlockColoredStone)

| Meta | Cor extraída | Uso principal |
|---|---|---|
| 0 | branco (#fcfcfc) | paredes principais |
| 14 | vermelho tijolo (#c4433c) | muro/molduras/pilares/anéis |
| 13 | verde escuro (#415920) | telhados |
| 12 | marrom (#653e24) | vigas/detalhes de piso |
| 3 | azul cristal (#9ab9f6) | janelas circulares + centros de medalhão |

## Módulo repetido crucial: EIXO DA TORRE

**Padrão de 5 blocos que se repete pra fazer cada "andar" do eixo:**

```
[bloco 1 - topo do andar anterior]     .WRW.
[bloco 2 - janela topo]                W...W
[bloco 3 - meio (medalhão)]            W...W  (com R nas laterais)
[bloco 4 - janela baixo]               W...W
[bloco 5 - anel + viga (fim andar)]    .RRR. no topo
                                       RMMMR
                                       RM.MR
                                       RMMMR
                                       .RRR.
```

Fazer torre com N andares = repetir isso N vezes. Fácil de portar pra Java.

## Insights pra o mod voiddim

1. **Estrutura hardcoded funciona**: 11.888 chamadas de `setBlock` num handler roda tranquilo em 1.7.10 (JinRyuu provou)
2. **Alternativa mais elegante**: fazer geração modular (função `buildTower(cx, cz)`, `buildHall(cx, cz)`, `buildGate(...)`)
3. **Escalabilidade**: podemos gerar 1× (DBC) ou 2× (Zixxter) só ajustando um multiplicador
4. **Paleta modular**: pode ser argila tingida vanilla OU bloco custom nosso — mesmo layout
5. **Custo de RAM**: 11.888 blocos = ~100KB de dados. Trivial.
6. **Generation trigger**: fazer spawn only once na primeira vez que dimensão for carregada, salvar flag em NBT do mundo

## Próximos passos possíveis

- (a) Reproduzir o mapa DBC INTEIRO em SVG (planta + cortes + 3D isométrico)
- (b) Extrair o código Python que gera cada componente separadamente (torre, salão, portões)
- (c) Portar diretamente pra Java (WorldGenerator subclass)
- (d) Aguardar papai definir escala/estilo antes de continuar
