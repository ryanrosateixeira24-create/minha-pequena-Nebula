"""
Gera um MUNDO Minecraft 1.7.10 completo (pasta 'TorreCheckIn/')
com a torre já construída. Papai só precisa:
1. Copiar a pasta pra .minecraft/saves/
2. Abrir MC 1.7.10 e carregar o mundo
3. Voar pra ver a torre (spawn na frente dela)

Formato Anvil (region files .mca).
Usa os módulos NBT do Claude adaptados.
"""

import sys, os, struct, gzip, io, zlib, time
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from build_torre import build_tower, world

# ========== ENCODER NBT ==========
TAG_END=0; TAG_BYTE=1; TAG_SHORT=2; TAG_INT=3; TAG_LONG=4; TAG_FLOAT=5; TAG_DOUBLE=6
TAG_BYTE_ARRAY=7; TAG_STRING=8; TAG_LIST=9; TAG_COMPOUND=10; TAG_INT_ARRAY=11

class Byte(int): pass
class Short(int): pass
class Int(int): pass
class Long(int): pass
class Float(float): pass
class Double(float): pass
class ByteArray(bytes): pass
class IntArray(list): pass
class TagList(list):
    def __init__(self, item_type, items):
        super().__init__(items)
        self.item_type = item_type

def write_string(buf, s):
    b = s.encode("utf-8")
    buf.write(struct.pack(">H", len(b)))
    buf.write(b)

def tag_type_of(v):
    if isinstance(v, Byte): return TAG_BYTE
    if isinstance(v, Short): return TAG_SHORT
    if isinstance(v, Int): return TAG_INT
    if isinstance(v, Long): return TAG_LONG
    if isinstance(v, Float): return TAG_FLOAT
    if isinstance(v, Double): return TAG_DOUBLE
    if isinstance(v, ByteArray): return TAG_BYTE_ARRAY
    if isinstance(v, IntArray): return TAG_INT_ARRAY
    if isinstance(v, str): return TAG_STRING
    if isinstance(v, TagList): return TAG_LIST
    if isinstance(v, dict): return TAG_COMPOUND
    raise TypeError(f"?? {type(v)}")

def write_payload(buf, v, t):
    if t == TAG_BYTE: buf.write(struct.pack(">b", v))
    elif t == TAG_SHORT: buf.write(struct.pack(">h", v))
    elif t == TAG_INT: buf.write(struct.pack(">i", v))
    elif t == TAG_LONG: buf.write(struct.pack(">q", v))
    elif t == TAG_FLOAT: buf.write(struct.pack(">f", v))
    elif t == TAG_DOUBLE: buf.write(struct.pack(">d", v))
    elif t == TAG_BYTE_ARRAY:
        buf.write(struct.pack(">i", len(v)))
        buf.write(v)
    elif t == TAG_INT_ARRAY:
        buf.write(struct.pack(">i", len(v)))
        for i in v:
            buf.write(struct.pack(">i", i))
    elif t == TAG_STRING: write_string(buf, v)
    elif t == TAG_LIST:
        buf.write(struct.pack(">b", v.item_type))
        buf.write(struct.pack(">i", len(v)))
        for item in v:
            write_payload(buf, item, v.item_type)
    elif t == TAG_COMPOUND:
        for name, val in v.items():
            tt = tag_type_of(val)
            buf.write(struct.pack(">b", tt))
            write_string(buf, name)
            write_payload(buf, val, tt)
        buf.write(struct.pack(">b", TAG_END))

def write_nbt(root_name, root_compound):
    buf = io.BytesIO()
    buf.write(struct.pack(">b", TAG_COMPOUND))
    write_string(buf, root_name)
    write_payload(buf, root_compound, TAG_COMPOUND)
    return buf.getvalue()

# ========== ANVIL CHUNK ==========
def chunk_to_nbt(cx, cz, chunk_blocks):
    """Converte dicionário {(x,y,z):(id,meta)} local do chunk (0-15) em NBT.
    Formato 1.7.10 Anvil: seções de 16x16x16 blocos."""
    sections = []
    for sy in range(16):
        # coleta blocos dessa seção
        y0, y1 = sy * 16, sy * 16 + 16
        block_data = bytearray(4096)  # 16*16*16
        data_arr = bytearray(2048)    # 4 bits por bloco = 4096/2
        has_any = False
        for lx in range(16):
            for ly in range(16):
                for lz in range(16):
                    wy = y0 + ly
                    key = (lx, wy, lz)
                    if key in chunk_blocks:
                        bid, meta = chunk_blocks[key]
                        if bid == 0: continue
                        idx = ly * 256 + lz * 16 + lx  # ordem YZX
                        block_data[idx] = bid & 0xFF
                        # data: 4 bits, dois blocos por byte
                        di = idx // 2
                        if idx % 2 == 0:
                            data_arr[di] = (data_arr[di] & 0xF0) | (meta & 0x0F)
                        else:
                            data_arr[di] = (data_arr[di] & 0x0F) | ((meta & 0x0F) << 4)
                        has_any = True
        if has_any:
            sections.append({
                "Y": Byte(sy),
                "Blocks": ByteArray(bytes(block_data)),
                "Data": ByteArray(bytes(data_arr)),
                "BlockLight": ByteArray(bytes(2048)),
                "SkyLight": ByteArray(b"\xff" * 2048),  # full sky
            })

    # biomes
    biomes = bytes([1] * 256)  # 1 = plains

    # HeightMap (16x16 ints)
    heightmap = []
    for lz in range(16):
        for lx in range(16):
            h = 0
            for wy in range(255, -1, -1):
                if (lx, wy, lz) in chunk_blocks:
                    bid = chunk_blocks[(lx, wy, lz)][0]
                    if bid != 0:
                        h = wy + 1
                        break
            heightmap.append(h)

    level = {
        "xPos": Int(cx),
        "zPos": Int(cz),
        "LastUpdate": Long(0),
        "LightPopulated": Byte(1),
        "TerrainPopulated": Byte(1),
        "InhabitedTime": Long(0),
        "Biomes": ByteArray(biomes),
        "HeightMap": IntArray(heightmap),
        "Sections": TagList(TAG_COMPOUND, sections),
        "Entities": TagList(TAG_COMPOUND, []),
        "TileEntities": TagList(TAG_COMPOUND, []),
    }
    root = {"Level": level}
    return write_nbt("", root)

# ========== REGION FILE ==========
def write_region_file(path, chunks):
    """Escreve arquivo .mca (formato Anvil).
    chunks = dict {(cx, cz): nbt_bytes} onde 0 <= cx, cz < 32 (relativos à região)"""
    # Cabeçalho: 4096 bytes location + 4096 bytes timestamp
    locations = bytearray(4096)
    timestamps = bytearray(4096)
    sectors = []  # cada sector = 4096 bytes
    current_sector = 2  # após header

    for (cx, cz), nbt_data in chunks.items():
        # comprime com zlib
        compressed = zlib.compress(nbt_data)
        # cabeçalho do chunk: 4 bytes length + 1 byte compression type
        chunk_data = struct.pack(">I", len(compressed) + 1) + b'\x02' + compressed
        # padding pra múltiplo de 4096
        pad = (-len(chunk_data)) % 4096
        chunk_data += b'\x00' * pad
        n_sectors = len(chunk_data) // 4096
        # location: offset (3 bytes) + count (1 byte)
        idx = (cz * 32 + cx) * 4
        locations[idx:idx+3] = struct.pack(">I", current_sector)[1:]
        locations[idx+3] = n_sectors
        timestamps[idx:idx+4] = struct.pack(">I", int(time.time()))
        sectors.append(chunk_data)
        current_sector += n_sectors

    with open(path, "wb") as f:
        f.write(bytes(locations))
        f.write(bytes(timestamps))
        for s in sectors:
            f.write(s)
    print(f"  região salva: {path} ({os.path.getsize(path)} bytes)")

# ========== LEVEL.DAT ==========
def write_level_dat(path, spawn_x, spawn_y, spawn_z):
    data = {
        "version": Int(19133),  # Anvil 1.7.x
        "initialized": Byte(1),
        "LevelName": "Torre Check-In",
        "generatorName": "flat",
        "generatorVersion": Int(0),
        "generatorOptions": "3;minecraft:air;127;",  # void
        "RandomSeed": Long(0),
        "MapFeatures": Byte(0),
        "LastPlayed": Long(int(time.time() * 1000)),
        "SizeOnDisk": Long(0),
        "allowCommands": Byte(1),
        "hardcore": Byte(0),
        "GameType": Int(1),  # criativo
        "Difficulty": Byte(0),
        "DifficultyLocked": Byte(0),
        "Time": Long(6000),
        "DayTime": Long(6000),  # meio-dia
        "SpawnX": Int(spawn_x),
        "SpawnY": Int(spawn_y),
        "SpawnZ": Int(spawn_z),
        "raining": Byte(0),
        "thundering": Byte(0),
        "rainTime": Int(999999),
        "thunderTime": Int(999999),
        "clearWeatherTime": Int(999999),
    }
    root = {"Data": data}
    nbt_bytes = write_nbt("", root)
    with gzip.open(path, "wb") as f:
        f.write(nbt_bytes)
    print(f"  level.dat salvo: {path}")

# ========== MAIN ==========
def main():
    # Constrói torre centrada em (0, 0)
    build_tower(cx=0, cz=0, y_base=64)
    print(f"total blocos construídos: {len(world)}")

    # Agrupa por chunks (16x16 XZ)
    from collections import defaultdict
    chunks_data = defaultdict(dict)
    for (x, y, z), block in world.items():
        cx = x >> 4
        cz = z >> 4
        lx = x & 15
        lz = z & 15
        chunks_data[(cx, cz)][(lx, y, lz)] = block

    print(f"chunks tocados: {len(chunks_data)}")

    # Estrutura de saída
    out_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                           "TorreCheckIn")
    os.makedirs(os.path.join(out_dir, "region"), exist_ok=True)

    # Agrupa chunks em regiões (32x32 chunks por região)
    regions = defaultdict(dict)
    for (cx, cz), blocks in chunks_data.items():
        rx = cx >> 5
        rz = cz >> 5
        lcx = cx & 31
        lcz = cz & 31
        nbt = chunk_to_nbt(cx, cz, blocks)
        regions[(rx, rz)][(lcx, lcz)] = nbt

    for (rx, rz), chunks in regions.items():
        rpath = os.path.join(out_dir, "region", f"r.{rx}.{rz}.mca")
        write_region_file(rpath, chunks)

    # level.dat: spawn 20 blocos ao sul da torre, olhando pra ela
    write_level_dat(os.path.join(out_dir, "level.dat"),
                    spawn_x=0, spawn_y=65, spawn_z=-20)

    print(f"\n✅ MUNDO PRONTO em: {out_dir}")
    print(f"copia essa pasta pra .minecraft/saves/ e abre no MC 1.7.10")

if __name__ == "__main__":
    main()
