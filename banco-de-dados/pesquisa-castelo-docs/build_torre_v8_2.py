"""Torre Check-In v8.2 — leitura direta da referência enviada pelo Pai.

Medalhões circulares, nervuras escuras, eaves finos e finial com agulha.

Topo côncavo de pagode e saia inferior com beiral próprio e oito nervuras.

Deriva da v7 escolhida como grande avanço pelo Pai. Mantém o eixo branco e a
silhueta octogonal, ampliando presença sem recomeçar a arquitetura.
"""
from pathlib import Path
from collections import Counter
import build_torre as legacy
import build_torre_v7 as v7

W, R, G, M, Y, L, Q, GR, AR = v7.W, v7.R, v7.G, v7.M, v7.Y, v7.L, v7.Q, v7.GR, v7.AR
DG = (35, 13)      # lã verde escura: segunda textura de telha
DARK = (159, 10)   # argila roxa escura: cintas/ribs da referência
NEEDLE = (101, 0)  # barras de ferro: agulha fina do finial
EXTRA_COLORS = dict(v7.EXTRA_COLORS)
EXTRA_COLORS[DG] = (53, 76, 28)
EXTRA_COLORS[DARK] = (91, 48, 91)
EXTRA_COLORS[NEEDLE] = (196, 174, 194)
THIN_BLOCKS = {NEEDLE}
world = v7.world

# Helpers usam o mesmo dicionário `world` do módulo v7.
sb = v7.sb
chamfered_square = v7.chamfered_square
edge_cells = v7.edge_cells
profile_vertices = v7.profile_vertices
fill_profile = v7.fill_profile
shell_profile = v7.shell_profile
vertical_ribs = v7.vertical_ribs


def four_ribs(cx, cz, y, half, chamfer, block=Q):
    """Quatro linhas de cumeeira, simétricas a cada rotação de 90°."""
    vertices = profile_vertices(half, chamfer)
    for index in (0, 2, 4, 6):
        x, z = vertices[index]
        sb(cx + x, y, cz + z, block)


def all_eave_tips(cx, cz, y, half, chamfer, block=Q):
    for x, z in profile_vertices(half, chamfer):
        sb(cx + x, y, cz + z, block)


def stamp_round_medallions(cx, cz, y, half):
    """Disco 3×3 sem cantos: cinco pixels escuros em cada face principal."""
    for z in (-half, half):
        for dx, dy in ((0, 0), (-1, 0), (1, 0), (0, -1), (0, 1)):
            sb(cx + dx, y + dy, cz + z, DARK)
    for x in (-half, half):
        for dz, dy in ((0, 0), (-1, 0), (1, 0), (0, -1), (0, 1)):
            sb(cx + x, y + dy, cz + dz, DARK)


def stamp_tall_arch_z(cx, cz, y0, z_face, open_center=False):
    # Interior 5×7, moldura de 1 bloco e topo escalonado de 3 níveis.
    for x in range(-2, 3):
        for y in range(y0, y0 + 7):
            sb(cx + x, y, z_face, AR if open_center else W)
    for y in range(y0, y0 + 8):
        sb(cx - 3, y, z_face, R)
        sb(cx + 3, y, z_face, R)
    for x in range(-2, 3):
        sb(cx + x, y0 + 7, z_face, R)
    for x in range(-1, 2):
        sb(cx + x, y0 + 8, z_face, R)
    sb(cx, y0 + 9, z_face, R)
    if not open_center:
        sb(cx, y0 + 3, z_face, L)
        sb(cx, y0 + 4, z_face, L)


def stamp_tall_arch_x(cx, cz, y0, x_face):
    for z in range(-2, 3):
        for y in range(y0, y0 + 7):
            sb(x_face, y, cz + z, W)
    for y in range(y0, y0 + 8):
        sb(x_face, y, cz - 3, R)
        sb(x_face, y, cz + 3, R)
    for z in range(-2, 3):
        sb(x_face, y0 + 7, cz + z, R)
    for z in range(-1, 2):
        sb(x_face, y0 + 8, cz + z, R)
    sb(x_face, y0 + 9, cz, R)
    sb(x_face, y0 + 3, cz, L)
    sb(x_face, y0 + 4, cz, L)


def build_tower(cx, cz, ground_y=1):
    world.clear()
    # Plataforma só para a prancha.
    for x in range(cx - 13, cx + 14):
        for z in range(cz - 13, cz + 14):
            sb(x, ground_y - 1, z, GR)

    # Plinth 21×21 e base octogonal 19×19, dez blocos de altura.
    fill_profile(cx, cz, ground_y - 1, 10, 5, Q)
    base_half, base_chamfer = 9, 4
    cells = chamfered_square(base_half, base_chamfer)
    border = edge_cells(cells)
    for y in range(ground_y, ground_y + 10):
        for x, z in border:
            sb(cx + x, y, cz + z, W)
    fill_profile(cx, cz, ground_y, base_half, base_chamfer, Q)
    vertical_ribs(cx, cz, ground_y, ground_y + 9,
                  base_half, base_chamfer, R)
    stamp_tall_arch_z(cx, cz, ground_y, cz - base_half, open_center=True)
    stamp_tall_arch_z(cx, cz, ground_y, cz + base_half, open_center=False)
    stamp_tall_arch_x(cx, cz, ground_y, cx - base_half)
    stamp_tall_arch_x(cx, cz, ground_y, cx + base_half)

    # Saia inferior refeita como telhado, não como escada verde simples:
    # soffit branco próprio + eave verde + seis degraus rápidos + oito nervuras.
    skirt_y = ground_y + 10
    fill_profile(cx, cz, skirt_y, 10, 5, Q)  # underside/eave projetado
    green_profiles = [(9, 4), (8, 4), (7, 3), (6, 3),
                      (6, 2), (5, 2), (5, 2)]
    for i, (half, chamfer) in enumerate(green_profiles, start=1):
        y = skirt_y + i
        fill_profile(cx, cz, y, half, chamfer,
                     G if i % 2 else DG)
        # A referência usa nervuras finas escuras; o eave branco fica em uma camada só.
        all_eave_tips(cx, cz, y, half, chamfer, DARK)
    collar_y = skirt_y + 1 + len(green_profiles)
    fill_profile(cx, cz, collar_y, 5, 2, Q)

    # Eixo maior 11×11, quatro módulos de dez blocos.
    shaft_y = collar_y + 1
    shaft_half, shaft_chamfer = 5, 2
    shaft_edge = edge_cells(chamfered_square(shaft_half, shaft_chamfer))
    for section in range(4):
        y0 = shaft_y + section * 10
        for y in range(y0, y0 + 9):
            for x, z in shaft_edge:
                sb(cx + x, y, cz + z, W)
        vertical_ribs(cx, cz, y0, y0 + 8,
                      shaft_half, shaft_chamfer, DARK)
        stamp_round_medallions(cx, cz, y0 + 4, shaft_half)
        shell_profile(cx, cz, y0 + 9, shaft_half, shaft_chamfer, DARK)
    shaft_top = shaft_y + 40

    # Topo côncavo de pagode: abre no beiral e estreita forte logo no começo,
    # em vez da sequência repetida que transformava a v8 numa cúpula arredondada.
    fill_profile(cx, cz, shaft_top, 8, 4, Q)
    roof_y = shaft_top + 1
    roof_profiles = [(8, 4), (6, 3), (5, 2), (4, 2),
                     (4, 2), (3, 1), (3, 1), (2, 1),
                     (2, 1), (1, 0), (1, 0), (0, 0)]
    for i, (half, chamfer) in enumerate(roof_profiles):
        y = roof_y + i
        fill_profile(cx, cz, y, half, chamfer,
                     G if i % 2 == 0 else DG)
        if half > 0:
            four_ribs(cx, cz, y, half, chamfer, DARK)

    spire_y = roof_y + len(roof_profiles)
    # Esfera vermelha de cinco blocos + topo; acima, agulha fina de iron bars.
    for dx, dz in ((0, 0), (-1, 0), (1, 0), (0, -1), (0, 1)):
        sb(cx + dx, spire_y, cz + dz, R)
    sb(cx, spire_y + 1, cz, R)
    for i in range(2, 6):
        sb(cx, spire_y + i, cz, NEEDLE)
    return spire_y + 5


def main():
    top = build_tower(15, 15, 1)
    legacy.world = world
    out = Path(__file__).with_name('torre-v8.2.schematic')
    legacy.write_schematic(str(out), 31, max(85, top + 2), 31)
    structure = [b for b in world.values() if b not in (AR, GR)]
    print('topY=', top, 'estrutura=', len(structure))
    print('distribuição=', dict(Counter(structure).most_common()))


if __name__ == '__main__':
    main()
