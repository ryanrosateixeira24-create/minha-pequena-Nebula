#!/usr/bin/env python3
"""Turnaround 4 vistas dos voxels reais de um módulo de torre."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import argparse
import importlib

SCALE=2; SX=9*SCALE; SY=4*SCALE; SZ=9*SCALE


def shade(c,f): return tuple(max(0,min(255,int(v*f))) for v in c)

def render(blocks,colors,thin_blocks,cx,cz,label):
    def proj(x,y,z): return ((cz*x-cx*z)*SX,(cx*x+cz*z)*SY-y*SZ)
    faces=[]
    for (x,y,z),block in blocks.items():
        color=colors.get(block,(220,70,220)); depth=cx*x+cz*z+y
        thin=block in thin_blocks
        if thin:
            x0,x1=x+.34,x+.66; z0,z1=z+.34,z+.66
            if blocks.get((x,y+1,z)) != block:
                pts=[proj(x0,y+1,z0),proj(x1,y+1,z0),proj(x1,y+1,z1),proj(x0,y+1,z1)]
                faces.append((depth+.9,pts,shade(color,1.08)))
            xp=x1 if cx>0 else x0
            pts=[proj(xp,y,z0),proj(xp,y+1,z0),proj(xp,y+1,z1),proj(xp,y,z1)]
            faces.append((depth+.6,pts,shade(color,.78)))
            zp=z1 if cz>0 else z0
            pts=[proj(x0,y,zp),proj(x0,y+1,zp),proj(x1,y+1,zp),proj(x1,y,zp)]
            faces.append((depth+.7,pts,shade(color,.92)))
            continue
        if (x,y+1,z) not in blocks:
            pts=[proj(x,y+1,z),proj(x+1,y+1,z),proj(x+1,y+1,z+1),proj(x,y+1,z+1)]
            faces.append((depth+.9,pts,shade(color,1.08)))
        nx=x+(1 if cx>0 else -1)
        if (nx,y,z) not in blocks:
            xp=x+1 if cx>0 else x
            pts=[proj(xp,y,z),proj(xp,y+1,z),proj(xp,y+1,z+1),proj(xp,y,z+1)]
            faces.append((depth+.6,pts,shade(color,.78)))
        nz=z+(1 if cz>0 else -1)
        if (x,y,nz) not in blocks:
            zp=z+1 if cz>0 else z
            pts=[proj(x,y,zp),proj(x,y+1,zp),proj(x+1,y+1,zp),proj(x+1,y,zp)]
            faces.append((depth+.7,pts,shade(color,.92)))
    pts=[p for _,poly,_ in faces for p in poly]
    minx,maxx=min(p[0] for p in pts),max(p[0] for p in pts)
    miny,maxy=min(p[1] for p in pts),max(p[1] for p in pts)
    margin=25*SCALE; titleh=28*SCALE
    im=Image.new('RGB',(int(maxx-minx+2*margin),int(maxy-miny+2*margin+titleh)),(221,137,198))
    d=ImageDraw.Draw(im); ox=margin-minx; oy=margin-miny+titleh
    for _,poly,color in sorted(faces,key=lambda v:v[0]):
        q=[(int(a+ox),int(b+oy)) for a,b in poly]
        d.polygon(q,fill=color,outline=(45,35,45),width=SCALE)
    try: font=ImageFont.truetype('DejaVuSans-Bold.ttf',11*SCALE)
    except OSError: font=ImageFont.load_default()
    d.text((im.width//2,8*SCALE),label,anchor='ma',fill=(42,31,92),font=font)
    return im.resize((im.width//SCALE,im.height//SCALE),Image.Resampling.LANCZOS)


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--module',default='build_torre')
    parser.add_argument('--output',default='24-torre-v5-turnaround-voxel-real.png')
    parser.add_argument('--title',default='TORRE V5 — TURNAROUND DO GERADOR REAL')
    args=parser.parse_args()
    tower=importlib.import_module(args.module)
    tower.world.clear(); tower.build_tower(0,0,1)
    blocks={p:b for p,b in tower.world.items() if b!=tower.AR}
    colors={tower.W:(252,252,252),tower.R:(196,67,60),tower.G:(65,89,32),
            tower.M:(101,62,36),tower.Y:(244,185,66),tower.GR:(104,166,79)}
    colors.update(getattr(tower, 'EXTRA_COLORS', {}))
    thin_blocks=set(getattr(tower, 'THIN_BLOCKS', set()))
    views=[render(blocks,colors,thin_blocks,1,-1,'FRONTAL NE'),render(blocks,colors,thin_blocks,-1,-1,'FRONTAL NO'),
           render(blocks,colors,thin_blocks,1,1,'TRASEIRA SE'),render(blocks,colors,thin_blocks,-1,1,'TRASEIRA SO')]
    cellw=max(v.width for v in views); cellh=max(v.height for v in views)
    sheet=Image.new('RGB',(cellw*2,cellh*2+32),(240,222,236))
    for i,v in enumerate(views):
        x=(i%2)*cellw+(cellw-v.width)//2; y=(i//2)*cellh+32+(cellh-v.height)//2
        sheet.paste(v,(x,y))
    d=ImageDraw.Draw(sheet)
    try: font=ImageFont.truetype('DejaVuSans-Bold.ttf',16)
    except OSError: font=ImageFont.load_default()
    d.text((sheet.width//2,8),args.title,anchor='ma',fill=(42,31,92),font=font)
    out=Path(__file__).with_name(args.output); sheet.save(out)
    print(out, sheet.size)

if __name__=='__main__': main()
