"""Torre Check-In v8.8 — acabamento autoral fiel sobre a v8.7 aprovada.

A ocupação de blocos é idêntica à v8.7. Só materiais/padrões são refinados:
medalhões com profundidade, cintas marrom-avermelhadas, portas em madeira escura,
luz âmbar, soffit fino e finial em duas tonalidades.
"""
from pathlib import Path
from collections import Counter
import build_torre as legacy
import build_torre_v8_7 as source

W,R,G,DG,M,Y,L,Q,NEEDLE,GR,AR=(source.W,source.R,source.G,source.DG,source.M,
                               source.Y,source.L,source.Q,source.NEEDLE,
                               source.GR,source.AR)
DEEP_RED=(112,0)       # nether brick: vinho profundo
WOOD=(5,5)             # dark oak planks
AMBER=(95,1)           # vidro laranja
RED_ACCENT=(35,14)     # lã vermelha, borda macia do medalhão
EXTRA_COLORS=dict(source.EXTRA_COLORS)
EXTRA_COLORS.update({DEEP_RED:(92,31,30),WOOD:(72,43,27),AMBER:(214,126,45),RED_ACCENT:(150,39,42)})
THIN_BLOCKS=set(source.THIN_BLOCKS)
world={}


def refine_shaft(cx,cz):
    half,chamfer=9,6
    segments=source.geom.face_segments(half,chamfer)
    border=source.geom.boundary(source.geom.octagon(half,chamfer))
    corners=set(source.geom.vertices(half,chamfer))
    shaft_min,shaft_max=28,87

    # Limpa os medalhões antigos sem tocar cintas nem quinas estruturais.
    for (x,y,z),block in list(world.items()):
        if shaft_min<=y<=shaft_max and block==Y:
            world[(x,y,z)]=W

    # Quatro cintas finas, em vinho, e quinas vermelhas preservadas.
    belt_layers=(42,57,72,87)
    for y in belt_layers:
        for x,z in border: world[(cx+x,y,cz+z)]=DEEP_RED
    for x,z in corners:
        for y in range(shaft_min,shaft_max+1):
            if y not in belt_layers: world[(cx+x,y,cz+z)]=R

    # Disco 3×3 sem cantos: centro profundo + quatro pixels vermelhos.
    for center_y in (35,50,65,80):
        for segment in segments:
            m=len(segment)//2
            x,z=segment[m]
            world[(cx+x,center_y,cz+z)]=DEEP_RED
            world[(cx+x,center_y-1,cz+z)]=RED_ACCENT
            world[(cx+x,center_y+1,cz+z)]=RED_ACCENT
            for index in (m-1,m+1):
                sx,sz=segment[index]
                world[(cx+sx,center_y,cz+sz)]=RED_ACCENT


def refine_doors(cx,cz):
    segments=source.geom.face_segments(15,10)
    for segment in segments:
        m=len(segment)//2
        # Cinco blocos de largura: moldura interna em clay marrom, miolo em madeira.
        for index in range(m-2,m+3):
            x,z=segment[index]
            material=M if index in (m-2,m+2) else WOOD
            for y in range(1,11):
                # Não encobre o arco vermelho superior.
                if world.get((cx+x,y,cz+z)) in (M,L):
                    world[(cx+x,y,cz+z)]=material
        x,z=segment[m]
        world[(cx+x,6,cz+z)]=AMBER
        world[(cx+x,7,cz+z)]=AMBER

    # Linha de sombra sob o eave, apenas material: a geometria não muda.
    for x,z in source.geom.boundary(source.geom.octagon(15,10)):
        block=world.get((cx+x,15,cz+z))
        if block not in (AR,None): world[(cx+x,15,cz+z)]=DEEP_RED


def refine_finial(cx,cz):
    needle_y=sorted(y for (x,y,z),block in world.items()
                    if x==cx and z==cz and block==NEEDLE)
    if not needle_y: return
    bottom=needle_y[0]
    # Mantém a mesma ocupação, apenas cria sombra na esfera.
    if (cx,bottom-1,cz) in world: world[(cx,bottom-1,cz)]=DEEP_RED
    if (cx,bottom-2,cz) in world: world[(cx,bottom-2,cz)]=R


def build_tower(cx,cz,ground_y=1):
    source.build_tower(cx,cz,ground_y)
    world.clear(); world.update(source.world)
    refine_shaft(cx,cz)
    refine_doors(cx,cz)
    refine_finial(cx,cz)
    return max(y for (_,y,_),block in world.items() if block!=AR)


def main():
    top=build_tower(22,22,1)
    legacy.world=world
    out=Path(__file__).with_name('torre-v8.8.schematic')
    legacy.write_schematic(str(out),45,max(130,top+2),45)
    structure=[b for b in world.values() if b not in (AR,GR)]
    print('ocupacao=idêntica-v8.7 topY=',top,'estrutura=',len(structure))
    print('distribuição=',dict(Counter(structure).most_common()))

if __name__=='__main__': main()
