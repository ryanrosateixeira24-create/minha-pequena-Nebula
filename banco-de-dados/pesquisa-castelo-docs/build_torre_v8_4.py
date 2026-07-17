"""Torre Check-In v8.4 — v8.3 preservada e esticada somente em Y (5/4).

Nenhuma coordenada X/Z, material ou decisão arquitetônica é redesenhada.
Cada quatro camadas verticais da v8.3 viram cinco.
"""
from pathlib import Path
from collections import Counter
import build_torre as legacy
import build_torre_v8_3 as source

W,R,G,DG,M,Y,L,Q,NEEDLE,GR,AR=(source.W,source.R,source.G,source.DG,source.M,
                               source.Y,source.L,source.Q,source.NEEDLE,
                               source.GR,source.AR)
EXTRA_COLORS=dict(source.EXTRA_COLORS)
THIN_BLOCKS=set(source.THIN_BLOCKS)
world={}

NUM=5
DEN=4


def stretch_y(source_world):
    result={}
    for (x,y,z),block in source_world.items():
        if y<=0:
            result[(x,y,z)]=block
            continue
        start=(y*NUM)//DEN
        end=((y+1)*NUM)//DEN
        if end<=start: end=start+1
        for dest_y in range(start,end):
            result[(x,dest_y,z)]=block
    return result


def build_tower(cx,cz,ground_y=1):
    # A v8.3 usa ground_y=1; mantemos a mesma âncora e só escalamos as camadas.
    source.build_tower(cx,cz,ground_y)
    world.clear()
    world.update(stretch_y(source.world))
    return max(y for (_,y,_),block in world.items() if block!=AR)


def main():
    top=build_tower(17,17,1)
    legacy.world=world
    out=Path(__file__).with_name('torre-v8.4.schematic')
    legacy.write_schematic(str(out),35,max(130,top+2),35)
    structure=[b for b in world.values() if b not in (AR,GR)]
    print('escala_y=5/4 topY=',top,'estrutura=',len(structure))
    print('distribuição=',dict(Counter(structure).most_common()))

if __name__=='__main__': main()
