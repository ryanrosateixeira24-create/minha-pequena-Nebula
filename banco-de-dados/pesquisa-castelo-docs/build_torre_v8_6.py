"""Torre Check-In v8.6 — altura v8.4 restaurada; base aberta em X/Z.

A parte superior (Y>=28) é copiada bit a bit da v8.4. O plinth cresce para
31×31 e o telhado-saia para 37×37, aproximando a relação 1,94× da referência.
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


def eight_ribs(cx,cz,y,half,chamfer):
    for x,z in geom.vertices(half,chamfer): world[(cx+x,y,cz+z)]=Q


def build_tower(cx,cz,ground_y=1):
    source.build_tower(cx,cz,ground_y)
    world.clear()
    # Copia tudo, menos plinth antigo e telhado inferior antigo.
    for (x,y,z),block in source.world.items():
        if y==0 and block in (GR,Q):
            continue
        if 16<=y<=27:
            continue
        world[(x,y,z)]=block

    # Plataforma de visualização maior e plinth octogonal 31×31.
    for x in range(cx-20,cx+21):
        for z in range(cz-20,cz+21): world[(x,0,z)]=GR
    fill_profile(cx,cz,0,15,10,Q)

    # Eave 37×37, dez degraus verdes, collar retorna ao corpo 19×19.
    fill_profile(cx,cz,16,18,12,Q)
    profiles=[(18,12),(17,11),(16,11),(15,10),(14,9),
              (13,9),(12,8),(11,7),(10,7),(9,6)]
    for i,(half,chamfer) in enumerate(profiles,start=17):
        fill_profile(cx,cz,i,half,chamfer,G if i%2 else DG)
        eight_ribs(cx,cz,i,half,chamfer)
    fill_profile(cx,cz,27,9,6,Q)
    return max(y for (_,y,_),block in world.items() if block!=AR)


def main():
    top=build_tower(22,22,1)
    legacy.world=world
    out=Path(__file__).with_name('torre-v8.6.schematic')
    legacy.write_schematic(str(out),45,max(130,top+2),45)
    structure=[b for b in world.values() if b not in (AR,GR)]
    print('lower_eave=37 shaft=19 ratio=',37/19,'topY=',top,'estrutura=',len(structure))
    print('distribuição=',dict(Counter(structure).most_common()))

if __name__=='__main__': main()
