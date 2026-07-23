"""
Gera a TORRE do Check-In Station como arquivo .schematic (WorldEdit format)
Baseado em Meshy v2 + confirmação hexagonal
Dimensões: 15 × 15 × 38 (com margem de 1 bloco em X/Z)

Paleta MC vanilla 1.7.10 (sem depender de mod, pra papai testar direto):
  - branco = stained clay meta 0        (id 159:0)
  - vermelho = stained clay meta 14     (id 159:14)
  - verde = stained clay meta 13        (id 159:13)
  - amarelo = glowstone                 (id 89) → medalhão centro c/ luz
  - marrom = stained clay meta 12       (id 159:12)
  - grama = grass                       (id 2)

Uso:
    python3 build_torre.py
    (gera torre.schematic no diretório atual)

Depois em MC:
    1. Criar mundo superflat
    2. Instalar WorldEdit + carregar torre.schematic
    3. //paste
"""

import sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import struct, gzip, io

# ============== BLOCOS ==============
W  = (159, 0)   # branco
R  = (159, 14)  # vermelho
G  = (159, 13)  # verde
M  = (159, 12)  # marrom
Y  = (89, 0)    # glowstone (amarelo brilhante)
LG = (159, 8)   # cinza claro - base octogonal
GR = (2, 0)     # grama
AR = (0, 0)     # ar

# ============== MUNDO ESPARSO ==============
world = {}  # (x, y, z) -> (id, meta)

def sb(x, y, z, block):
    world[(x, y, z)] = block

def fill(x0, y0, z0, x1, y1, z1, block):
    xa, xb = sorted((x0, x1))
    ya, yb = sorted((y0, y1))
    za, zb = sorted((z0, z1))
    for x in range(xa, xb+1):
        for y in range(ya, yb+1):
            for z in range(za, zb+1):
                sb(x, y, z, block)

# ============== HEXÁGONO ==============
def hex_shell(cx, cz, y, face_width, block):
    """
    Desenha a shell (perímetro) de um hexágono horizontal em Y=y,
    centrado em (cx, cz), com face_width blocos na face norte.
    Hexágono orientado com 2 lados horizontais (norte/sul) e 4 chamfers.

    face_width = largura da face plana (norte e sul). Deve ser ímpar.
    Chamfer: cada lado inclinado tem (face_width // 2) blocos oblíquos.
    """
    half = face_width // 2
    # face NORTE (z = -half - chamfer_offset). Aproxima com stair steps.
    # geometria: pra face_width=7, half=3:
    #   linha z=-3: X de -3 a +3 (7 blocos, face norte)
    #   linha z=-2: X de -4 a +4 (9 blocos, chamfers começando)
    #   linha z=-1: X de -4 a +4
    #   linha z= 0: X de -4 a +4  (linha central, 9 blocos)
    #   linha z=+1: X de -4 a +4
    #   linha z=+2: X de -4 a +4
    #   linha z=+3: X de -3 a +3 (face sul)
    # total: 7×7 bbox

    # simplificação: pra cada linha Z, calcula largura X
    total_depth = face_width  # 7 pra face=7
    depth_half = total_depth // 2

    for dz in range(-depth_half, depth_half + 1):
        # calcula largura pra esta linha
        # distância do centro em Z
        dist = abs(dz)
        # se dist == depth_half → face norte/sul (face_width blocos)
        # se dist < depth_half → linha do meio, mais larga em 2 por linha? não.
        # hexágono regular real: largura constante no centro, estreita nas pontas
        # simplificação: hex "gordo" com máx largura = face_width+2, mín = face_width
        if dist == depth_half:
            w = face_width
        else:
            # linhas do meio ficam 2 blocos mais largas
            w = face_width + 2
        wh = w // 2
        # só as bordas (shell)
        # borda de trás (dz na face)
        if dist == depth_half:
            for dx in range(-wh, wh + 1):
                sb(cx + dx, y, cz + dz, block)
        else:
            # só extremos X (paredes laterais)
            sb(cx - wh, y, cz + dz, block)
            sb(cx + wh, y, cz + dz, block)
            # se estiver na linha limite (dist == depth_half - 1), preenche chamfer
            if dist == depth_half - 1:
                # blocos do chamfer que fazem transição
                sb(cx - wh + 1, y, cz + dz, block)
                sb(cx + wh - 1, y, cz + dz, block)

def hex_fill(cx, cz, y, face_width, block):
    """Preenche o hexágono todo (sólido)."""
    depth_half = face_width // 2
    for dz in range(-depth_half, depth_half + 1):
        dist = abs(dz)
        if dist == depth_half:
            w = face_width
        else:
            w = face_width + 2
        wh = w // 2
        for dx in range(-wh, wh + 1):
            sb(cx + dx, y, cz + dz, block)

def hex_pilares(cx, cz, y0, y1, face_width, block):
    """Coloca pilares nas 6 quinas do hexágono, do Y0 ao Y1."""
    depth_half = face_width // 2
    wh_face = face_width // 2       # X dos extremos da face norte/sul
    wh_mid = (face_width + 2) // 2  # X dos extremos das laterais

    # 6 vértices do hexágono:
    vertices = [
        (-wh_face, -depth_half),   # NO
        ( wh_face, -depth_half),   # NE
        (-wh_mid,   0),            # O
        ( wh_mid,   0),            # L
        (-wh_face,  depth_half),   # SO
        ( wh_face,  depth_half),   # SE
    ]
    for dx, dz in vertices:
        for y in range(y0, y1 + 1):
            sb(cx + dx, y, cz + dz, block)

# ============== TORRE ==============
def build_tower(cx, cz, y_base):
    """Constrói a torre completa a partir de (cx, y_base, cz).
       cx, cz = centro horizontal. y_base = Y do chão (bloco 0 fica em Y=y_base)."""

    # === BASE CINZA OCTOGONAL (nova V8.7.1) - 3 blocos de espessura, face 19 ===
    # Esquececida na V8.7 aprovada, adicionada agora
    for dy in range(0, 3):
        hex_fill(cx, cz, y_base - 2 - dy, 19, LG)

    # === CHÃO DE GRAMA embaixo (base 20x20) - agora abaixo da base cinza ===
    for dx in range(-12, 13):
        for dz in range(-12, 13):
            sb(cx + dx, y_base - 5, cz + dz, GR)

    # === PLINTH Y=0-1 (face 15, sólido) ===
    hex_fill(cx, cz, y_base, 15, W)
    hex_fill(cx, cz, y_base + 1, 15, W)
    # borda vermelha topo do plinth
    hex_shell(cx, cz, y_base + 1, 15, R)  # sobrescreve só a shell com R

    # === BEIRAL PLINTH Y=2 ===
    hex_fill(cx, cz, y_base + 2, 15, W)

    # === BASE HEXAGONAL Y=3-7 (face 12, oca com pilares) ===
    for y in range(y_base + 3, y_base + 8):
        hex_shell(cx, cz, y, 13, W)  # paredes
    # 6 pilares nas quinas
    hex_pilares(cx, cz, y_base + 3, y_base + 7, 13, R)

    # PORTA CENTRAL na face norte (dz negativo)
    # posição: dx = -1 a +1 (3 blocos larg), dy = 0 a 3 (4 alt), dz = -6 (face norte)
    depth_half = 13 // 2
    porta_z = cz - depth_half
    for dx in range(-1, 2):
        for dy in range(0, 4):
            sb(cx + dx, y_base + 3 + dy, porta_z, AR)  # esvazia
    # moldura vermelha em volta
    for dy in range(0, 5):
        sb(cx - 2, y_base + 3 + dy, porta_z, R)
        sb(cx + 2, y_base + 3 + dy, porta_z, R)
    for dx in range(-2, 3):
        sb(cx + dx, y_base + 7, porta_z, R)
    # arco escalonado (2 blocos acima do topo horizontal)
    sb(cx - 1, y_base + 8, porta_z, R)
    sb(cx + 1, y_base + 8, porta_z, R)
    sb(cx, y_base + 8, porta_z, R)
    sb(cx, y_base + 9, porta_z, R)
    # LANTERNA dentro (glowstone)
    sb(cx, y_base + 3, cz, Y)  # centro
    sb(cx, y_base + 4, cz, Y)

    # === COLAR Y=8 (face 11, R) ===
    hex_shell(cx, cz, y_base + 8, 11, R)

    # === SAIA Y=9-12 (4 degraus 15→13→11→9) ===
    hex_fill(cx, cz, y_base + 9,  15, G)
    hex_fill(cx, cz, y_base + 10, 13, G)
    hex_fill(cx, cz, y_base + 11, 11, G)
    hex_fill(cx, cz, y_base + 12, 9, G)

    # === COLAR BRANCO Y=13-14 (face 9) ===
    hex_shell(cx, cz, y_base + 13, 9, W)
    hex_shell(cx, cz, y_base + 14, 9, W)

    # === EIXO Y=15-27 (face 7, 4 andares de 3 blocos) ===
    for y in range(y_base + 15, y_base + 28):
        hex_shell(cx, cz, y, 7, W)
    # 6 pilares R do eixo
    hex_pilares(cx, cz, y_base + 15, y_base + 27, 7, R)

    # medalhões nos 4 andares (Y=15, 18, 21, 24 — meio de cada andar de 3 blocos)
    # cada medalhão: 2×2 vermelho c/ centro 1×1 glowstone, na FACE NORTE
    depth_half_eixo = 7 // 2
    face_z = cz - depth_half_eixo
    for andar_y in [y_base + 16, y_base + 19, y_base + 22, y_base + 25]:
        # medalhão 2 larg × 2 alt
        for dx in range(-1, 1):
            for dy in range(0, 2):
                sb(cx + dx, andar_y + dy, face_z, R)
        # centro amarelo (glowstone) 1×1 dentro
        sb(cx, andar_y, face_z, Y)
        # anel R no topo do andar (1 bloco acima do medalhão superior)
        for dx in range(-3, 4):
            sb(cx + dx, andar_y + 2, face_z, R)

    # === BEIRAL TOPO Y=28-30 (face 9) ===
    hex_fill(cx, cz, y_base + 28, 9, W)
    hex_fill(cx, cz, y_base + 29, 9, W)
    hex_fill(cx, cz, y_base + 30, 9, W)

    # === TELHADO CÔNICO Y=31-34 (9→7→5→3) ===
    hex_fill(cx, cz, y_base + 31, 9, G)
    hex_fill(cx, cz, y_base + 32, 7, G)
    hex_fill(cx, cz, y_base + 33, 5, G)
    hex_fill(cx, cz, y_base + 34, 3, G)

    # === PINÁCULO Y=35-37 ===
    sb(cx, y_base + 35, cz, W)
    sb(cx, y_base + 36, cz, R)
    sb(cx, y_base + 37, cz, W)


# ============== SCHEMATIC EXPORTER ==============
def write_schematic(filename, w, h, l):
    """Salva 'world' como arquivo .schematic (WorldEdit format).
    w = largura X, h = altura Y, l = comprimento Z.
    Origem em (0, 0, 0) do 'world'."""
    blocks = bytearray(w * h * l)
    data = bytearray(w * h * l)
    for (x, y, z), (bid, meta) in world.items():
        if 0 <= x < w and 0 <= y < h and 0 <= z < l:
            idx = (y * l + z) * w + x
            blocks[idx] = bid & 0xFF
            data[idx] = meta & 0x0F

    # Monta NBT
    def encode_string(s):
        b = s.encode("utf-8")
        return struct.pack(">H", len(b)) + b

    buf = io.BytesIO()
    # TAG_Compound "Schematic"
    buf.write(struct.pack(">b", 10))  # TAG_Compound
    buf.write(encode_string("Schematic"))

    def write_tag(name, tag_id, payload):
        buf.write(struct.pack(">b", tag_id))
        buf.write(encode_string(name))
        buf.write(payload)

    write_tag("Width", 2, struct.pack(">h", w))
    write_tag("Length", 2, struct.pack(">h", l))
    write_tag("Height", 2, struct.pack(">h", h))
    write_tag("Materials", 8, encode_string("Alpha"))
    write_tag("Blocks", 7, struct.pack(">i", len(blocks)) + bytes(blocks))
    write_tag("Data", 7, struct.pack(">i", len(data)) + bytes(data))
    # Entities e TileEntities: listas vazias
    buf.write(struct.pack(">b", 9))  # TAG_List
    buf.write(encode_string("Entities"))
    buf.write(struct.pack(">b", 0))  # tipo interno = TAG_End
    buf.write(struct.pack(">i", 0))  # tamanho 0
    buf.write(struct.pack(">b", 9))
    buf.write(encode_string("TileEntities"))
    buf.write(struct.pack(">b", 0))
    buf.write(struct.pack(">i", 0))
    buf.write(struct.pack(">b", 0))  # fim do Compound

    # Comprime com gzip (padrão .schematic)
    with gzip.open(filename, "wb") as f:
        f.write(buf.getvalue())
    print(f"✅ salvo: {filename} ({os.path.getsize(filename)} bytes)")


# ============== MAIN ==============
if __name__ == "__main__":
    # Centro da torre: (13, 13) dentro do schematic 27×44×27 para caber base cinza octogonal 19
    build_tower(cx=13, cz=13, y_base=5)

    w, h, l = 27, 44, 27
    out_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "torre.schematic")
    write_schematic(out_path, w, h, l)

    # Também salvar um resumo de blocos
    from collections import Counter
    tipos = Counter((b[0], b[1]) for b in world.values() if b[0] != 0)
    print(f"total blocos colocados: {sum(tipos.values())}")
    print("distribuição:", dict(tipos.most_common()))
