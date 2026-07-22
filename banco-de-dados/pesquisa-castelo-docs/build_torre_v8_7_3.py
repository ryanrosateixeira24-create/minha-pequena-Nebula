"""V8.7.3 - Fix telhado em cima de telhado: subir parte amarela pra cima
Amarela = térreo + telhado inferior verde que tava em cima do outro telhado
Sobe 5 blocos pra não ter telhado em cima de telhado
"""
from pathlib import Path
from collections import Counter
import build_torre as legacy
import build_torre_v8_7 as source
import build_torre_v8_3 as geom

W,R,G,DG,M,Y,L,Q,NEEDLE,GR,AR=source.W,source.R,source.G,source.DG,source.M,source.Y,source.L,source.Q,source.NEEDLE,source.GR,source.AR
LG=(159,8)
world={}

def fill_profile(cx,cz,y,half,chamfer,block):
    for x,z in geom.octagon(half,chamfer):
        world[(cx+x,y,cz+z)]=block

def build_tower(cx,cz,ground_y=1):
    world.clear()
    # Copia topo de V8.7 (Y>=28) - telhado superior perfeito
    source.build_tower(cx,cz,ground_y)
    for (x,y,z),block in list(source.world.items()):
        if y>=28+ground_y:
            world[(x,y,z)]=block

    # Base cinza octogonal 39x39 em Y=-1,-2,-3
    base_half, base_chamfer = 19,13
    for dy in range(3):
        y=ground_y-1-dy
        fill_profile(cx,cz,y,base_half,base_chamfer,LG)

    # Grama 45x45 em Y=-4
    for x in range(cx-22,cx+23):
        for z in range(cx-22,cx+23):
            world[(x,ground_y-4,z)]=GR

    # Plinth 33x33 em Y=0
    fill_profile(cx,cz,ground_y,16,11,Q)

    # Térreo 31x31 - SUBIR AMARELA PRA CIMA: antes era Y=1..15, agora Y=6..20 (sobe 5 blocos)
    # Isso tira o telhado em cima de telhado, deixa espaço entre telhados
    base_half_t, base_chamfer_t = 15,10
    for y in range(ground_y+6, ground_y+21):  # subiu 5 blocos (era 1..15, agora 6..20)
        for x,z in geom.boundary(geom.octagon(base_half_t,base_chamfer_t)):
            world[(cx+x,y,cz+z)]=W
    fill_profile(cx,cz,ground_y+6,base_half_t,base_chamfer_t,Q)
    # Portas e pilares do térreo - copia de V8.7 mas com offset +5
    for (x,y,z),block in source.world.items():
        rel=y-ground_y
        if 1 <= rel <=15:
            # Copia mas sobe 5 blocos
            world[(x,y+5,z)]=block

    # Colar branco em Y=21 (era 16, sobe 5)
    fill_profile(cx,cz,ground_y+21,9,6,Q)
    for x,z in geom.boundary(geom.octagon(9,6)):
        world[(cx+x,ground_y+21,cz+z)]=W

    # Fuste grosso 23x23 em Y=22..36 (era 17..28, sobe 5)
    shaft_half, shaft_chamfer = 11,7
    for y in range(ground_y+22, ground_y+37):
        for x,z in geom.boundary(geom.octagon(shaft_half,shaft_chamfer)):
            world[(cx+x,y,cz+z)]=W
        for x,z in geom.vertices(shaft_half,shaft_chamfer):
            world[(cx+x,y,cz+z)]=R
        if (y-ground_y-22)%3==1:
            for dx in range(-1,1):
                world[(cx+dx,y,cz-shaft_half)]=R
            world[(cx,y,cz-shaft_half)]=Y

    return max(y for (_,y,_),b in world.items() if b!=AR)

def main():
    top=build_tower(25,25,5)
    legacy.world=world
    out=Path(__file__).with_name('torre-v8.7.3-amarela-subida.schematic')
    legacy.write_schematic(str(out),55,max(130,top+2),55)
    from collections import Counter
    structure=[b for b in world.values() if b not in (AR,GR)]
    print(f'V8.7.3 amarela subida: topY={top} blocos={len(structure)}')
    print(Counter(structure).most_common(3))

if __name__=='__main__': main()
