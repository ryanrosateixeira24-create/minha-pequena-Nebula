#!/usr/bin/env python3
"""Render isométrico exato dos voxels produzidos por build_torre.py."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import build_torre as tower

SCALE = 2
SX, SY, SZ = 12 * SCALE, 6 * SCALE, 12 * SCALE
MARGIN = 70 * SCALE

COLORS = {
    tower.W:  (252, 252, 252),
    tower.R:  (196, 67, 60),
    tower.G:  (65, 89, 32),
    tower.M:  (101, 62, 36),
    tower.Y:  (244, 185, 66),
    tower.GR: (104, 166, 79),
}


def shade(rgb, factor):
    return tuple(max(0, min(255, int(c * factor))) for c in rgb)


def project(x, y, z):
    return ((x - z) * SX, (x + z) * SY - y * SZ)


def polygon_for_face(x, y, z, face):
    if face == "top":
        pts = [(x, y + 1, z), (x + 1, y + 1, z),
               (x + 1, y + 1, z + 1), (x, y + 1, z + 1)]
    elif face == "east":
        pts = [(x + 1, y, z), (x + 1, y + 1, z),
               (x + 1, y + 1, z + 1), (x + 1, y, z + 1)]
    else:  # south
        pts = [(x, y, z + 1), (x, y + 1, z + 1),
               (x + 1, y + 1, z + 1), (x + 1, y, z + 1)]
    return [project(*p) for p in pts]


def main():
    tower.world.clear()
    tower.build_tower(0, 0, 1)
    blocks = {p: b for p, b in tower.world.items() if b != tower.AR}

    faces = []
    for (x, y, z), block in blocks.items():
        base = COLORS.get(block, (210, 80, 210))
        if (x, y + 1, z) not in blocks:
            faces.append((x + z + y + 0.9, polygon_for_face(x, y, z, "top"), shade(base, 1.08)))
        if (x + 1, y, z) not in blocks:
            faces.append((x + z + y + 0.6, polygon_for_face(x, y, z, "east"), shade(base, 0.77)))
        if (x, y, z + 1) not in blocks:
            faces.append((x + z + y + 0.7, polygon_for_face(x, y, z, "south"), shade(base, 0.91)))

    all_points = [p for _, poly, _ in faces for p in poly]
    min_x = min(p[0] for p in all_points)
    max_x = max(p[0] for p in all_points)
    min_y = min(p[1] for p in all_points)
    max_y = max(p[1] for p in all_points)
    width = int(max_x - min_x + MARGIN * 2)
    height = int(max_y - min_y + MARGIN * 2 + 55 * SCALE)
    offset_x = MARGIN - min_x
    offset_y = MARGIN - min_y + 35 * SCALE

    image = Image.new("RGB", (width, height), (221, 137, 198))
    draw = ImageDraw.Draw(image)
    faces.sort(key=lambda item: item[0])
    for _, poly, color in faces:
        shifted = [(int(px + offset_x), int(py + offset_y)) for px, py in poly]
        draw.polygon(shifted, fill=color, outline=(48, 38, 48), width=max(1, SCALE))

    structure = {p: b for p, b in blocks.items() if b != tower.GR}
    xs = [p[0] for p in structure]
    ys = [p[1] for p in structure]
    zs = [p[2] for p in structure]
    title = "TORRE V5 — VOXELS REAIS DO GERADOR"
    subtitle = (f"estrutura {max(xs)-min(xs)+1}×{max(zs)-min(zs)+1}×{max(ys)-min(ys)+1} "
                f"| {len(structure)} blocos | base declarada 14, footprint real 17×15")
    try:
        font_title = ImageFont.truetype("DejaVuSans-Bold.ttf", 17 * SCALE)
        font_sub = ImageFont.truetype("DejaVuSans.ttf", 10 * SCALE)
    except OSError:
        font_title = font_sub = ImageFont.load_default()
    draw.text((width // 2, 14 * SCALE), title, anchor="ma", fill=(42, 31, 92), font=font_title)
    draw.text((width // 2, 36 * SCALE), subtitle, anchor="ma", fill=(82, 55, 76), font=font_sub)

    image = image.resize((width // SCALE, height // SCALE), Image.Resampling.LANCZOS)
    out = Path(__file__).with_name("23-torre-v5-voxel-real-preview.png")
    image.save(out)
    print(out)
    print(f"faces={len(faces)} canvas={image.width}x{image.height}")


if __name__ == "__main__":
    main()
