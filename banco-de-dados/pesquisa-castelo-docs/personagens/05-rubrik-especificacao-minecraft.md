# Rubrik — especificação para Minecraft 1.7.10

## Escala

- altura visual: **2,35 blocos**;
- largura máxima de ombros: 1,05 bloco;
- caixa física inicial: 0,82 × 2,20 × 0,82;
- olhos/câmera do NPC: Y≈2,05;
- não cabe em porta vanilla de 2 blocos sem inclinar/crouch narrativo.

## Partes do modelo (unidades de ModelRenderer)

| Parte | Dimensão aproximada |
|---|---|
| Cabeça | 10×9×9 |
| Nariz/focinho | 6×3×2 |
| Orelhas | 3×4×1 cada |
| Chifre reto | cone 3 base × 6 altura |
| Óculos | aro fino separado |
| Torso | 14×16×8 |
| Barriga | volume frontal +2 |
| Braços | 5×16×5 |
| Mãos | 6×5×6 |
| Pernas | 6×14×6 |
| Sapatos | 7×4×10 |
| Gravata | plano/mesh separado |
| Crachá/caneta/chave | detalhes de textura + volumes simples |

## Esqueleto compartilhável

```text
root
├── pelvis
│   ├── leg.L → shoe.L
│   └── leg.R → shoe.R
├── torso
│   ├── arm.L → hand.L
│   ├── arm.R → hand.R
│   └── head
│       ├── horn
│       ├── glasses
│       └── ears
├── clipboard.socket
└── stamp.socket
```

O mesmo rig serve aos ogros procedurais; presets trocam malhas de cabeça,
chifres, barriga, roupa e acessórios.

## Orçamento

- alvo: 700–1.200 triângulos;
- limite rígido inicial: 1.800;
- textura: 128×128 RGBA;
- sem normal map no primeiro mod;
- um material principal + props;
- LOD futuro somente depois de vários NPCs simultâneos.

## Animações iniciais

1. idle respirando/ajustando óculos;
2. andar pesado;
3. apontar direção;
4. folhear prancheta;
5. carimbar balcão;
6. irritação contida com pé batendo;
7. ajudar escondido, inclinando a ficha ao jogador.

## Estado

Esta ficha define o design/modelo. Ainda não existe entidade, IA, textura UV ou
arquivo Blockbench. A folha visual v3 continua canônica.
