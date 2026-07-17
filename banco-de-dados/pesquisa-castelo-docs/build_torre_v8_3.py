"""Torre Check-In v8.3 — correções exatas do Pai.

Base e eixo usam o mesmo octógono 19×19; oito portas; dois telhados com oito
nervuras brancas; paleta branca/vermelha/verde; escala ampliada para 25×25×94.
"""
from pathlib import Path
from collections import Counter
import build_torre as legacy

W=(159,0); R=(159,14); G=(159,13); DG=(35,13); M=(159,12)
Y=(35,14); L=(89,0); Q=(155,0); NEEDLE=(101,0); GR=(35,4); AR=(0,0)
EXTRA_COLORS={DG:(53,76,28),Q:(230,230,226),L:(255,205,82),
              Y:(130,35,40),NEEDLE:(196,174,194)}
THIN_BLOCKS={NEEDLE}
world={}


def sb(x,y,z,block): world[(x,y,z)]=block


def octagon(half,chamfer):
    """Octógono regularizado: quatro faces retas + quatro diagonais escalonadas."""
    straight=half-chamfer
    limit=half+straight
    return {(x,z) for x in range(-half,half+1) for z in range(-half,half+1)
            if abs(x)+abs(z)<=limit}


def boundary(cells):
    return {p for p in cells if any((p[0]+dx,p[1]+dz) not in cells
             for dx,dz in ((1,0),(-1,0),(0,1),(0,-1)))}


def vertices(half,chamfer):
    s=half-chamfer
    return [(-s,-half),(s,-half),(half,-s),(half,s),
            (s,half),(-s,half),(-half,s),(-half,-s)]


def face_segments(half,chamfer):
    s=half-chamfer; h=half; c=chamfer
    return [
      [(x,-h) for x in range(-s,s+1)],
      [(s+i,-h+i) for i in range(c+1)],
      [(h,z) for z in range(-s,s+1)],
      [(h-i,s+i) for i in range(c+1)],
      [(x,h) for x in range(s,-s-1,-1)],
      [(-s-i,h-i) for i in range(c+1)],
      [(-h,z) for z in range(s,-s-1,-1)],
      [(-h+i,-s-i) for i in range(c+1)],
    ]


def fill_profile(cx,cz,y,half,chamfer,block):
    for x,z in octagon(half,chamfer): sb(cx+x,y,cz+z,block)


def shell_profile(cx,cz,y,half,chamfer,block):
    for x,z in boundary(octagon(half,chamfer)): sb(cx+x,y,cz+z,block)


def vertical_corners(cx,cz,y0,y1,half,chamfer,block):
    for x,z in vertices(half,chamfer):
        for y in range(y0,y1+1): sb(cx+x,y,cz+z,block)


def eight_roof_ribs(cx,cz,y,half,chamfer,block=Q):
    for x,z in vertices(half,chamfer): sb(cx+x,y,cz+z,block)


def stamp_eight_doors(cx,cz,y0,half,chamfer):
    """Uma porta arqueada em cada face do octógono; todas com a mesma largura."""
    for segment in face_segments(half,chamfer):
        m=len(segment)//2
        interior=segment[m-1:m+2]
        left=segment[m-2]; right=segment[m+2]
        for x,z in interior:
            for y in range(y0,y0+7): sb(cx+x,y,cz+z,M)
        for x,z in (left,right):
            for y in range(y0,y0+8): sb(cx+x,y,cz+z,R)
        for x,z in interior: sb(cx+x,y0+7,cz+z,R)
        cxp,czp=segment[m]
        sb(cx+cxp,y0+8,cz+czp,R)
        sb(cx+cxp,y0+3,cz+czp,L)
        sb(cx+cxp,y0+4,cz+czp,L)


def stamp_round_medallions(cx,cz,y,half,chamfer):
    """Disco de cinco pixels em cada uma das oito faces."""
    for segment in face_segments(half,chamfer):
        m=len(segment)//2
        for index in (m-1,m,m+1):
            x,z=segment[index]; sb(cx+x,y,cz+z,Y)
        x,z=segment[m]
        sb(cx+x,y-1,cz+z,Y); sb(cx+x,y+1,cz+z,Y)


def build_tower(cx,cz,ground_y=1):
    world.clear()
    # Plataforma visual.
    for x in range(cx-15,cx+16):
        for z in range(cz-15,cz+16): sb(x,ground_y-1,z,GR)

    body_half,body_chamfer=9,6  # bbox 19; oito faces de 7 blocos
    roof_half,roof_chamfer=12,8 # bbox 25; oito faces de 9 blocos

    # Plinth e BASE: 12 blocos, mesmo octógono do eixo, oito portas.
    fill_profile(cx,cz,ground_y-1,10,7,Q)
    cells=octagon(body_half,body_chamfer); border=boundary(cells)
    for y in range(ground_y,ground_y+12):
        for x,z in border: sb(cx+x,y,cz+z,W)
    fill_profile(cx,cz,ground_y,body_half,body_chamfer,Q)
    vertical_corners(cx,cz,ground_y,ground_y+11,body_half,body_chamfer,R)
    stamp_eight_doors(cx,cz,ground_y,body_half,body_chamfer)

    # TELHADO-SAIA: eave 25×25, oito nervuras brancas, termina no corpo 19×19.
    skirt_y=ground_y+12
    fill_profile(cx,cz,skirt_y,roof_half,roof_chamfer,Q)
    skirt_profiles=[(12,8),(12,8),(11,7),(11,7),(10,7),(10,7),(9,6),(9,6)]
    for i,(half,chamfer) in enumerate(skirt_profiles,start=1):
        y=skirt_y+i
        fill_profile(cx,cz,y,half,chamfer,G if i%2 else DG)
        eight_roof_ribs(cx,cz,y,half,chamfer,Q)
    collar_y=skirt_y+1+len(skirt_profiles)
    fill_profile(cx,cz,collar_y,body_half,body_chamfer,Q)

    # EIXO: mesmo octógono da base, quatro módulos de 12 blocos.
    shaft_y=collar_y+1; edge=boundary(cells)
    for section in range(4):
        y0=shaft_y+section*12
        for y in range(y0,y0+11):
            for x,z in edge: sb(cx+x,y,cz+z,W)
        vertical_corners(cx,cz,y0,y0+10,body_half,body_chamfer,R)
        stamp_round_medallions(cx,cz,y0+5,body_half,body_chamfer)
        shell_profile(cx,cz,y0+11,body_half,body_chamfer,R)
    shaft_top=shaft_y+48

    # TELHADO SUPERIOR: mesmo octógono/eave da saia; curva côncava e 8 ribs.
    fill_profile(cx,cz,shaft_top,roof_half,roof_chamfer,Q)
    roof_y=shaft_top+1
    top_profiles=[(12,8),(9,6),(8,5),(7,5),(6,4),(5,3),(5,3),
                  (4,3),(3,2),(3,2),(2,1),(2,1),(1,0),(1,0),(0,0)]
    for i,(half,chamfer) in enumerate(top_profiles):
        y=roof_y+i
        fill_profile(cx,cz,y,half,chamfer,G if i%2==0 else DG)
        if half>0: eight_roof_ribs(cx,cz,y,half,chamfer,Q)

    # Esfera vermelha e agulha fina.
    finial_y=roof_y+len(top_profiles)
    for dx,dz in ((0,0),(-1,0),(1,0),(0,-1),(0,1)): sb(cx+dx,finial_y,cz+dz,R)
    sb(cx,finial_y+1,cz,R)
    for i in range(2,7): sb(cx,finial_y+i,cz,NEEDLE)
    return finial_y+6


def main():
    top=build_tower(17,17,1)
    legacy.world=world
    out=Path(__file__).with_name('torre-v8.3.schematic')
    legacy.write_schematic(str(out),35,max(100,top+2),35)
    structure=[b for b in world.values() if b not in (AR,GR)]
    print('topY=',top,'estrutura=',len(structure))
    print('distribuição=',dict(Counter(structure).most_common()))

if __name__=='__main__': main()
