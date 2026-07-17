"""Torre Check-In v8.5 — parte superior v8.4 congelada; base ampliada.

A referência medida pede ~31% base+saia. As 28 camadas inferiores da v8.4
viram 40 (10/7); tudo a partir do shaft apenas sobe 12 blocos, sem deformação.
"""
from pathlib import Path
from collections import Counter
import build_torre as legacy
import build_torre_v8_4 as source

W,R,G,DG,M,Y,L,Q,NEEDLE,GR,AR=(source.W,source.R,source.G,source.DG,source.M,
                               source.Y,source.L,source.Q,source.NEEDLE,
                               source.GR,source.AR)
EXTRA_COLORS=dict(source.EXTRA_COLORS)
THIN_BLOCKS=set(source.THIN_BLOCKS)
world={}

UPPER_START=28
LOW_NUM=10
LOW_DEN=7
LOW_DEST_HEIGHT=40
SHIFT=LOW_DEST_HEIGHT-UPPER_START  # +12


def enlarge_lower_only(source_world):
    result={}
    for (x,y,z),block in source_world.items():
        if y<UPPER_START:
            start=(y*LOW_NUM)//LOW_DEN
            end=((y+1)*LOW_NUM)//LOW_DEN
            if end<=start: end=start+1
            for dest_y in range(start,end): result[(x,dest_y,z)]=block
        else:
            result[(x,y+SHIFT,z)]=block
    return result


def build_tower(cx,cz,ground_y=1):
    source.build_tower(cx,cz,ground_y)
    world.clear(); world.update(enlarge_lower_only(source.world))
    return max(y for (_,y,_),block in world.items() if block!=AR)


def main():
    top=build_tower(17,17,1)
    legacy.world=world
    out=Path(__file__).with_name('torre-v8.5.schematic')
    legacy.write_schematic(str(out),35,max(140,top+2),35)
    structure=[b for b in world.values() if b not in (AR,GR)]
    print('lower=28->40 upper_shift=12 topY=',top,'estrutura=',len(structure))
    print('distribuição=',dict(Counter(structure).most_common()))

if __name__=='__main__': main()
