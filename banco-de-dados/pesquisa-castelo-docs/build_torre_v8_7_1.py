"""Torre Check-In v8.7.1 — base cinza octogonal adicionada à base aprovada V8.7.

V8.7 tinha 35x35x117, 15.263 blocos, octógono correto, oito portas, quatro módulos, telhado travado, mas sem fundação cinza.
Pai apontou que falta base cinza octogonal que serve como plataforma.

Esta versão adiciona 3 blocos de espessura de base cinza octogonal clara (stained clay 8) com face 19 (39x39), maior que plinth 33x33, servindo como transição entre grama e torre.

Mantém tudo de V8.7 idêntico acima de Y=0.
"""

from pathlib import Path
from collections import Counter
import build_torre as legacy
import build_torre_v8_7 as source
import build_torre_v8_3 as geom

W,R,G,DG,M,Y,L,Q,NEEDLE,GR,AR=(source.W,source.R,source.G,source.DG,source.M,
                               source.Y,source.L,source.Q,source.NEEDLE,
                               source.GR,source.AR)
# Cinza claro para base
LG=(159, 8)  # light gray stained clay
world={}

def fill_profile(cx,cz,y,half,chamfer,block):
    for x,z in geom.octagon(half,chamfer):
        world[(cx+x,y,cz+z)]=block

def build_tower(cx,cz,ground_y=1):
    # Primeiro constrói V8.7 normal
    source.build_tower(cx,cz,ground_y)
    # Copia tudo de V8.7 para nosso world
    world.clear()
    for (x,y,z),block in source.world.items():
        world[(x,y,z)]=block

    # Adiciona base cinza octogonal 3 blocos de espessura abaixo do plinth
    # Plinth de V8.7 é 33x33 em Y=0 (fill_profile half 16 chamfer 11)
    # Base cinza será 39x39 (half 19 chamfer 13) em Y=-3,-2,-1
    base_half, base_chamfer = 19, 13  # 39x39
    for dy in range(3):
        y = ground_y - 1 - dy - 1  # -1, -2, -3 abaixo do chão original (que era 0)
        # Na verdade ground_y=1, então y = 1-1-0-1 = -1? Vamos fazer Y=-2,-3,-4
        y = ground_y - 2 - dy
        fill_profile(cx,cz,y,base_half,base_chamfer,LG)

    # Grama embaixo da base cinza, maior ainda 45x45
    for x in range(cx-22, cx+23):
        for z in range(cz-22, cz+23):
            world[(x, ground_y-5, z)]=GR

    return max(y for (_,y,_),block in world.items() if block!=AR)

def main():
    top=build_tower(25,25,5)  # centro maior para caber base 39x39
    legacy.world=world
    out=Path(__file__).with_name('torre-v8.7.1-com-base-cinza.schematic')
    legacy.write_schematic(str(out),55,max(130,top+2),55)
    structure=[b for b in world.values() if b not in (AR,GR)]
    print(f'V8.7.1 com base cinza octogonal: topY={top} estrutura={len(structure)} (V8.7 era 15263)')
    print('distribuição=',dict(Counter(structure).most_common()))
    print(f'salvo {out}')

if __name__=='__main__': main()
