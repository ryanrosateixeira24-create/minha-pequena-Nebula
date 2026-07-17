"""Torre Check-In v7 — melhoria da direção v9 escolhida pelo Pai.

Octógono por quadrado chanfrado, ~65 blocos, quatro módulos de 9 blocos,
branco dominante, medalhões pequenos e telhados com nervuras apoiadas.
"""
from pathlib import Path
from collections import Counter
import build_torre as legacy

W = (159, 0)    # white clay
R = (159, 14)   # red clay
G = (159, 13)   # green clay
M = (159, 12)   # brown clay
Y = (95, 3)     # light blue glass / center of medallion
L = (89, 0)     # warm light
Q = (155, 0)    # quartz ribs
GR = (35, 4)    # yellow wool test ground
AR = (0, 0)
EXTRA_COLORS = {Q: (230, 230, 226), L: (255, 205, 82)}
world = {}


def sb(x, y, z, block):
    world[(x, y, z)] = block


def chamfered_square(half, chamfer):
    cells = set()
    straight = half - chamfer
    for x in range(-half, half + 1):
        for z in range(-half, half + 1):
            if abs(x) > straight and abs(z) > straight:
                continue
            cells.add((x, z))
    return cells


def edge_cells(cells):
    return {p for p in cells if any((p[0] + dx, p[1] + dz) not in cells
                                    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)))}


def profile_vertices(half, chamfer):
    straight = half - chamfer
    return [(-straight, -half), (straight, -half),
            (half, -straight), (half, straight),
            (straight, half), (-straight, half),
            (-half, straight), (-half, -straight)]


def fill_profile(cx, cz, y, half, chamfer, block):
    for x, z in chamfered_square(half, chamfer):
        sb(cx + x, y, cz + z, block)


def shell_profile(cx, cz, y, half, chamfer, block):
    for x, z in edge_cells(chamfered_square(half, chamfer)):
        sb(cx + x, y, cz + z, block)


def ribs(cx, cz, y, half, chamfer, block=Q):
    """Blocos sempre apoiados em vértices reais do perfil; nunca flutuam no bbox."""
    for x, z in profile_vertices(half, chamfer):
        sb(cx + x, y, cz + z, block)


def vertical_ribs(cx, cz, y0, y1, half, chamfer, block):
    for x, z in profile_vertices(half, chamfer):
        for y in range(y0, y1 + 1):
            sb(cx + x, y, cz + z, block)


def stamp_small_medallions(cx, cz, y, half):
    """Cruz 3×3 pequena nas quatro faces principais; diagonais ficam leves."""
    # faces norte/sul — largura varia em X
    for z in (-half, half):
        sb(cx, y, cz + z, Y)
        for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1)):
            sb(cx + dx, y + dy, cz + z, R)
    # faces oeste/leste — largura varia em Z
    for x in (-half, half):
        sb(cx + x, y, cz, Y)
        for dz, dy in ((-1, 0), (1, 0), (0, -1), (0, 1)):
            sb(cx + x, y + dy, cz + dz, R)


def stamp_arch_z(cx, cz, y0, z_face, light=True, open_center=False):
    for dy in range(0, 5):
        sb(cx - 2, y0 + dy, z_face, R)
        sb(cx + 2, y0 + dy, z_face, R)
    for dx in range(-1, 2):
        sb(cx + dx, y0 + 5, z_face, R)
    sb(cx, y0 + 6, z_face, R)
    for dx in range(-1, 2):
        for dy in range(0, 5):
            sb(cx + dx, y0 + dy, z_face, AR if open_center else W)
    if light and not open_center:
        sb(cx, y0 + 2, z_face, L)
        sb(cx, y0 + 3, z_face, L)


def stamp_arch_x(cx, cz, y0, x_face, light=True):
    for dy in range(0, 5):
        sb(x_face, y0 + dy, cz - 2, R)
        sb(x_face, y0 + dy, cz + 2, R)
    for dz in range(-1, 2):
        sb(x_face, y0 + 5, cz + dz, R)
    sb(x_face, y0 + 6, cz, R)
    if light:
        sb(x_face, y0 + 2, cz, L)
        sb(x_face, y0 + 3, cz, L)


def build_tower(cx, cz, ground_y=1):
    world.clear()
    # Solo amarelo só para a prancha de teste.
    for x in range(cx - 11, cx + 12):
        for z in range(cz - 11, cz + 12):
            sb(x, ground_y - 1, z, GR)

    # Plinth 17×17 chanfrado.
    fill_profile(cx, cz, ground_y - 1, 8, 4, W)

    # BASE: 7 blocos, octogonal, uma porta + três arcadas iluminadas.
    base_half, base_chamfer = 7, 3
    base_cells = chamfered_square(base_half, base_chamfer)
    for y in range(ground_y, ground_y + 7):
        for x, z in edge_cells(base_cells):
            sb(cx + x, y, cz + z, W)
    fill_profile(cx, cz, ground_y, base_half, base_chamfer, Q)
    vertical_ribs(cx, cz, ground_y, ground_y + 6,
                  base_half, base_chamfer, R)
    stamp_arch_z(cx, cz, ground_y, cz - base_half, light=False, open_center=True)
    stamp_arch_z(cx, cz, ground_y, cz + base_half, light=True, open_center=False)
    stamp_arch_x(cx, cz, ground_y, cx - base_half, light=True)
    stamp_arch_x(cx, cz, ground_y, cx + base_half, light=True)

    # SAIA: seis camadas verdes, nervuras brancas apoiadas, colar branco.
    skirt_y = ground_y + 7
    skirt_profiles = [(7, 3), (7, 3), (6, 3), (6, 3), (5, 2), (4, 2)]
    for i, (half, chamfer) in enumerate(skirt_profiles):
        fill_profile(cx, cz, skirt_y + i, half, chamfer, G)
        ribs(cx, cz, skirt_y + i, half, chamfer, Q)
    collar_y = skirt_y + len(skirt_profiles)
    fill_profile(cx, cz, collar_y, 4, 2, Q)

    # EIXO: quatro módulos de 9 blocos = 36; branco domina.
    shaft_y = collar_y + 1
    shaft_half, shaft_chamfer = 4, 2
    shaft_cells = chamfered_square(shaft_half, shaft_chamfer)
    shaft_edge = edge_cells(shaft_cells)
    for section in range(4):
        y0 = shaft_y + section * 9
        for y in range(y0, y0 + 8):
            for x, z in shaft_edge:
                sb(cx + x, y, cz + z, W)
        # nervuras claras nos oito cantos; cintas vermelhas ficam finas.
        vertical_ribs(cx, cz, y0, y0 + 7,
                      shaft_half, shaft_chamfer, Q)
        stamp_small_medallions(cx, cz, y0 + 4, shaft_half)
        shell_profile(cx, cz, y0 + 8,
                      shaft_half, shaft_chamfer, R)
    shaft_top = shaft_y + 36

    # TOP0: beiral branco fino e telhado verde alto com nervuras reais.
    fill_profile(cx, cz, shaft_top, 7, 3, Q)
    roof_profiles = [(6, 2), (6, 2), (5, 2), (4, 2), (4, 2),
                     (3, 1), (2, 1), (2, 1), (1, 0), (0, 0)]
    roof_y = shaft_top + 1
    for i, (half, chamfer) in enumerate(roof_profiles):
        fill_profile(cx, cz, roof_y + i, half, chamfer, G)
        if half > 0:
            ribs(cx, cz, roof_y + i, half, chamfer, Q)

    spire_y = roof_y + len(roof_profiles)
    sb(cx, spire_y, cz, R)
    sb(cx, spire_y + 1, cz, R)
    sb(cx, spire_y + 2, cz, Q)
    sb(cx, spire_y + 3, cz, Q)
    return spire_y + 3


def main():
    top = build_tower(12, 12, 1)
    legacy.world = world
    out = Path(__file__).with_name('torre-v7.schematic')
    legacy.write_schematic(str(out), 25, max(70, top + 2), 25)
    structure = [b for b in world.values() if b not in (AR, GR)]
    print('topY=', top, 'estrutura=', len(structure))
    print('distribuição=', dict(Counter(structure).most_common()))


if __name__ == '__main__':
    main()
