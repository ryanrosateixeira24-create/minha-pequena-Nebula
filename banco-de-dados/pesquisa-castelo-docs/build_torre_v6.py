"""Torre Check-In v6 — perfil hexagonal real e detalhes em seis faces.

Preserva a torre v5. Gera `torre-v6.schematic` para Minecraft 1.7.10.
"""
from pathlib import Path
from collections import Counter
import build_torre as legacy

W, R, G, M, Y, GR, AR = legacy.W, legacy.R, legacy.G, legacy.M, legacy.Y, legacy.GR, legacy.AR
world = {}


def sb(x, y, z, block):
    world[(x, y, z)] = block


# width, depth e largura das faces norte/sul. Todos ímpares para centro exato.
PROFILES = {
    15: (15, 11, 7),
    13: (13, 9, 7),
    11: (11, 9, 5),
     9: (9, 7, 5),
     7: (7, 7, 3),
     5: (5, 5, 3),
     3: (3, 3, 1),
     1: (1, 1, 1),
}


def hex_mask(size):
    width, depth, front = PROFILES[size]
    max_half = width // 2
    depth_half = depth // 2
    front_half = front // 2
    points = set()
    if depth_half == 0:
        return {(0, 0)}
    gain = max_half - front_half
    for z in range(-depth_half, depth_half + 1):
        toward_center = depth_half - abs(z)
        half = front_half + int(round(gain * toward_center / float(depth_half)))
        for x in range(-half, half + 1):
            points.add((x, z))
    return points


def shell_points(size):
    mask = hex_mask(size)
    return {p for p in mask if any((p[0] + dx, p[1] + dz) not in mask
                                    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)))}


def hex_fill(cx, cz, y, size, block):
    for x, z in hex_mask(size):
        sb(cx + x, y, cz + z, block)


def hex_shell(cx, cz, y, size, block):
    for x, z in shell_points(size):
        sb(cx + x, y, cz + z, block)


def hex_vertices(size):
    width, depth, front = PROFILES[size]
    wh, dh, fh = width // 2, depth // 2, front // 2
    return [(-fh, -dh), (fh, -dh), (wh, 0),
            (fh, dh), (-fh, dh), (-wh, 0)]


def hex_pillars(cx, cz, y0, y1, size, block):
    for x, z in hex_vertices(size):
        for y in range(y0, y1 + 1):
            sb(cx + x, y, cz + z, block)


def side_segments(size):
    mask = hex_mask(size)
    width, depth, front = PROFILES[size]
    dh = depth // 2
    rows = {}
    for z in range(-dh, dh + 1):
        xs = sorted(x for x, zz in mask if zz == z)
        rows[z] = (xs[0], xs[-1])
    north = [(x, -dh) for x in range(rows[-dh][0], rows[-dh][1] + 1)]
    south = [(x, dh) for x in range(rows[dh][1], rows[dh][0] - 1, -1)]
    ne = [(rows[z][1], z) for z in range(-dh, 1)]
    se = [(rows[z][1], z) for z in range(0, dh + 1)]
    sw = [(rows[z][0], z) for z in range(dh, -1, -1)]
    nw = [(rows[z][0], z) for z in range(0, -dh - 1, -1)]
    return [north, ne, se, south, sw, nw]


def decorate_six_medallions(cx, cz, y, size):
    """Motivo R-Y-R centralizado em cada uma das seis faces."""
    used = set()
    for segment in side_segments(size):
        # Remove repetições de degraus e escolhe três posições em torno do centro.
        clean = []
        for point in segment:
            if not clean or point != clean[-1]:
                clean.append(point)
        middle = len(clean) // 2
        indices = sorted(set(max(0, min(len(clean) - 1, middle + offset))
                             for offset in (-1, 0, 1)))
        for index in indices:
            x, z = clean[index]
            key = (x, z)
            if key in used:
                continue
            used.add(key)
            sb(cx + x, y, cz + z, Y if index == middle else R)


def white_roof_tips(cx, cz, y, size):
    for x, z in hex_vertices(size):
        sb(cx + x, y, cz + z, W)


def build_tower(cx, cz, y_base):
    # Plataforma de teste, não faz parte do módulo final do palácio.
    for dx in range(-10, 11):
        for dz in range(-10, 11):
            sb(cx + dx, y_base - 1, cz + dz, GR)

    # Plinth e beiral: footprint máximo 15×11, mais próximo da base declarada 14.
    hex_fill(cx, cz, y_base + 0, 15, W)
    hex_fill(cx, cz, y_base + 1, 15, W)
    hex_shell(cx, cz, y_base + 1, 15, R)
    hex_fill(cx, cz, y_base + 2, 15, W)

    # Base oca com seis pilares e porta norte.
    for y in range(y_base + 3, y_base + 8):
        hex_shell(cx, cz, y, 13, W)
    hex_pillars(cx, cz, y_base + 3, y_base + 7, 13, R)
    north_z = cz - PROFILES[13][1] // 2
    for dx in range(-1, 2):
        for dy in range(0, 4):
            sb(cx + dx, y_base + 3 + dy, north_z, AR)
    for dy in range(0, 5):
        sb(cx - 2, y_base + 3 + dy, north_z, R)
        sb(cx + 2, y_base + 3 + dy, north_z, R)
    for dx in range(-2, 3):
        sb(cx + dx, y_base + 7, north_z, R)
    sb(cx, y_base + 3, cz, Y)
    sb(cx, y_base + 4, cz, Y)

    # Colar e telhado-saia em quatro degraus.
    hex_shell(cx, cz, y_base + 8, 11, R)
    for dy, size in enumerate((15, 13, 11, 9), start=9):
        hex_fill(cx, cz, y_base + dy, size, G)
        if size in (15, 13):
            white_roof_tips(cx, cz, y_base + dy, size)
    hex_shell(cx, cz, y_base + 13, 9, W)
    hex_shell(cx, cz, y_base + 14, 9, W)

    # Eixo hexagonal: quatro andares realmente legíveis em todas as faces.
    for y in range(y_base + 15, y_base + 28):
        hex_shell(cx, cz, y, 7, W)
    hex_pillars(cx, cz, y_base + 15, y_base + 27, 7, R)
    for floor_base in (y_base + 15, y_base + 18, y_base + 21, y_base + 24):
        decorate_six_medallions(cx, cz, floor_base + 1, 7)
        hex_shell(cx, cz, floor_base + 2, 7, R)
    hex_shell(cx, cz, y_base + 27, 7, W)

    # Beiral superior vazado e cone escalonado.
    hex_fill(cx, cz, y_base + 28, 9, W)
    hex_shell(cx, cz, y_base + 29, 9, W)
    hex_shell(cx, cz, y_base + 30, 9, W)
    for dy, size in enumerate((9, 7, 5, 3), start=31):
        hex_fill(cx, cz, y_base + dy, size, G)
        if dy == 31:
            white_roof_tips(cx, cz, y_base + dy, size)

    sb(cx, y_base + 35, cz, W)
    sb(cx, y_base + 36, cz, R)
    sb(cx, y_base + 37, cz, W)


def main():
    world.clear()
    build_tower(10, 10, 1)
    legacy.world = world
    out = Path(__file__).with_name("torre-v6.schematic")
    legacy.write_schematic(str(out), 21, 40, 21)
    non_air = [block for block in world.values() if block not in (AR, GR)]
    print("estrutura:", len(non_air), "blocos")
    print("distribuição:", dict(Counter(non_air).most_common()))


if __name__ == "__main__":
    main()
