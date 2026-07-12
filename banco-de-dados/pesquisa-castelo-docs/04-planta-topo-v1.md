# Planta topo v1 — Check-In Station

Escala: 1 caractere = ~2 blocos. Norte pra cima. Total ~64x80 blocos.

```
                        NORTE
    ═══════════════════════════════════════════
   ║  ╔════════════════════════════════════╗  ║
   ║  ║ MURO VERMELHO PERIMETRAL           ║  ║
   ║  ╚════════════════════════════════════╝  ║
   ║                                          ║
   ║     ┌───┐                    ┌───┐       ║
   ║     │TNE│                    │TNW│       ║   ← torres traseiras (NE/NW)
   ║     └───┘                    └───┘       ║
   ║                                          ║
   ║              ┌─────────┐                 ║
   ║              │ ▓▓▓▓▓▓▓ │                 ║
   ║              │ ▓▓▓▓▓▓▓ │  ← SALÃO        ║
   ║              │ ▓CENTRAL▓│    principal    ║
   ║              │ ▓▓▓▓▓▓▓ │    (vertical,   ║
   ║              │ ▓▓▓▓▓▓▓ │    pagode 3     ║
   ║              │ ▓▓▓▓▓▓▓ │    andares)     ║
   ║              └─────────┘                 ║
   ║                                          ║
   ║     ┌───┐                    ┌───┐       ║
   ║     │TSE│                    │TSW│       ║   ← torres frontais (SE/SW)
   ║     └───┘                    └───┘       ║
   ║                                          ║
   ║           ┌───────────────┐              ║
   ║           │ PORTÃO WELCOME│              ║
   ║           └───────┬───────┘              ║
    ═══════════════════╪═══════════════════════
                       │
                       │  caminho verde
                       │  borda vermelha
                       ▼
                    (nuvens)
                        SUL
```

## Elementos numerados

| # | Elemento | Dimensão aprox (blocos) | Cor telhado | Cor parede |
|---|---|---|---|---|
| 1 | Muro perimetral | 60×70, altura 4 | — | vermelho |
| 2 | Salão central | 16×16 base, altura 30 | verde escalonado | branco |
| 3 | Torre NE | 6×6 base, altura 20 | verde cone | branco+vermelho |
| 4 | Torre NW | idem | idem | idem |
| 5 | Torre SE | idem | idem | idem |
| 6 | Torre SW | idem | idem | idem |
| 7 | Portão frontal | 14×6, altura 8 | verde | branco+placa |
| 8 | Caminho de acesso | 4 largura | — | verde+vermelho |
| 9 | Base de nuvens | 60×70+, espessura 10 | — | amarelo |

## Notas

- **Salão central** = ponto mais alto, 3 andares de telhado escalonado, chifres brancos no topo
- **4 torres** formam quadrado ao redor do salão
- **Portão** fica ao SUL (frente), fora do muro principal
- **Caminho verde** conecta portão às nuvens externas
- Tudo em plataforma flutuante de nuvens amarelas
