"""Torre Check-In v8.7 — térreo largo; telhado inferior 35×35 travado.

A parte superior Y>=28 é idêntica à v8.4. A construção inferior cresce para
31×31, plinth 33×33 e oito portas maiores. O telhado v8.6 reduz 37→35 e congela.
"""
from pathlib import Path
from collections import Counter
import build_torre as legacy
import build_torre_v8_4 as source
import build_torre_v8_3 as geom

W,R,G,DG,M,Y,L,Q,NEEDLE,GR,AR=(source.W,source.R,source.G,source.DG,source.M,
                               source.Y,source.L,source.Q,source.NEEDLE,
                               source.GR,source.AR)
EXTRA_COLORS=dict(source.EXTRA_COLORS)
THIN_BLOCKS=set(source.THIN_BLOCKS)
world={}
UPPER_START=28


def fill_profile(cx,cz,y,half,chamfer,block):
    for x,z in geom.octagon(half,chamfer): world[(cx+x,y,cz+z)]=block


def shell_profile(cx,cz,y,half,chamfer,block):
    for x,z in geom.boundary(geom.octagon(half,chamfer)): world[(cx+x,y,cz+z)]=block


def corners(cx,cz,y0,y1,half,chamfer,block):
    for x,z in geom.vertices(half,chamfer):
        for y in range(y0,y1+1): world[(cx+x,y,cz+z)]=block


def eight_ribs(cx,cz,y,half,chamfer):
    for x,z in geom.vertices(half,chamfer): world[(cx+x,y,cz+z)]=Q


def stamp_large_eight_doors(cx,cz,y0,half,chamfer):
    segments=geom.face_segments(half,chamfer)
    for segment in segments:
        m=len(segment)//2
        interior=segment[m-2:m+3]  # cinco blocos
        left=segment[m-3]; right=segment[m+3]
        for x,z in interior:
            for y in range(y0,y0+10): world[(cx+x,y,cz+z)]=M
        for x,z in (left,right):
            for y in range(y0,y0+12): world[(cx+x,y,cz+z)]=R
        for x,z in interior: world[(cx+x,y0+10,cz+z)]=R
        for x,z in segment[m-1:m+2]: world[(cx+x,y0+11,cz+z)]=R
        x,z=segment[m]
        world[(cx+x,y0+12,cz+z)]=R
        world[(cx+x,y0+5,cz+z)]=L
        world[(cx+x,y0+6,cz+z)]=L


def build_tower(cx,cz,ground_y=1):
    source.build_tower(cx,cz,ground_y)
    world.clear()
    # Superior perfeito: cópia literal e imóvel.
    for (x,y,z),block in source.world.items():
        if y>=UPPER_START: world[(x,y,z)]=block

    # Plataforma e plinth.
    for x in range(cx-20,cx+21):
        for z in range(cz-20,cz+21): world[(x,0,z)]=GR
    fill_profile(cx,cz,0,16,11,Q)  # 33×33

    # Térreo 31×31, 15 blocos de altura, oito faces iguais de 11 blocos.
    base_half,base_chamfer=15,10
    edge=geom.boundary(geom.octagon(base_half,base_chamfer))
    for y in range(1,16):
        for x,z in edge: world[(cx+x,y,cz+z)]=W
    fill_profile(cx,cz,1,base_half,base_chamfer,Q)
    corners(cx,cz,1,15,base_half,base_chamfer,R)
    stamp_large_eight_doors(cx,cz,1,base_half,base_chamfer)

    # Telhado inferior 35×35 TRAVADO: afunila até o eixo 19×19.
    fill_profile(cx,cz,16,17,11,Q)
    profiles=[(17,11),(16,11),(15,10),(14,9),(13,9),
              (12,8),(11,7),(10,7),(10,7),(9,6)]
    for y,(half,chamfer) in enumerate(profiles,start=17):
        fill_profile(cx,cz,y,half,chamfer,G if y%2 else DG)
        eight_ribs(cx,cz,y,half,chamfer)
    fill_profile(cx,cz,27,9,6,Q)
    return max(y for (_,y,_),block in world.items() if block!=AR)


def main():
    top=build_tower(22,22,1)
    legacy.world=world
    out=Path(__file__).with_name('torre-v8.7.schematic')
    legacy.write_schematic(str(out),45,max(130,top+2),45)
    structure=[b for b in world.values() if b not in (AR,GR)]
    print('base=31 plinth=33 roof_locked=35 shaft=19 topY=',top,'estrutura=',len(structure))
    print('distribuição=',dict(Counter(structure).most_common()))

if __name__=='__main__': main()
