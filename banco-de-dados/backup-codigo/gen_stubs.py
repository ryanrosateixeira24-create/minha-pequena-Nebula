#!/usr/bin/env python3
"""
Gera stubs Java mínimos a partir das referências externas do output de `javap -v`
sobre o jar original. Objetivo: providenciar um classpath sintético suficiente
para compilar novas classes do mod que usam os mesmos SRG names (func_*, field_*).

Assume que já rodou: javap -v -p -c <cada.class> > analysis/all_javap.txt
"""

import os
import re
import sys
from collections import defaultdict

# -------------- helpers --------------

def default_value(t):
    if t == "void":    return ""
    if t == "boolean": return "false"
    if t in ("byte", "char", "short", "int"): return "0"
    if t == "long":    return "0L"
    if t == "float":   return "0f"
    if t == "double":  return "0.0"
    return "null"

def parse_type(desc, i):
    c = desc[i]
    if c == 'V': return ("void", i + 1)
    if c == 'Z': return ("boolean", i + 1)
    if c == 'B': return ("byte", i + 1)
    if c == 'C': return ("char", i + 1)
    if c == 'S': return ("short", i + 1)
    if c == 'I': return ("int", i + 1)
    if c == 'J': return ("long", i + 1)
    if c == 'F': return ("float", i + 1)
    if c == 'D': return ("double", i + 1)
    if c == 'L':
        e = desc.index(';', i)
        cls = desc[i + 1:e].replace('/', '.').replace('$', '.')  # inner-class: $ -> .
        return (cls, e + 1)
    if c == '[':
        t, ni = parse_type(desc, i + 1)
        return (t + "[]", ni)
    raise ValueError("bad desc %r at %d" % (desc, i))

def parse_method_desc(desc):
    """Returns (params: list[str], ret: str)"""
    assert desc.startswith("(")
    i = 1
    params = []
    while desc[i] != ')':
        t, ni = parse_type(desc, i)
        params.append(t); i = ni
    ret, _ = parse_type(desc, i + 1)
    return params, ret

def parse_field_desc(desc):
    t, _ = parse_type(desc, 0)
    return t  # already has $ -> . via parse_type

# -------------- coleta --------------

INPUT = "analysis/all_javap.txt"
OUT_DIR = "stubs"

# For each external class, collect referenced methods (name, desc) and fields (name, desc).
methods = defaultdict(set)   # cls -> set of (name, desc)
fields  = defaultdict(set)   # cls -> set of (name, desc)
static_methods = set()       # (cls, name, desc)
static_fields  = set()       # (cls, name, desc)
seen_classes   = set()       # every class name we saw

# From constant-pool lines
CP_MEMBER = re.compile(
    r'#\d+ = (Methodref|Fieldref|InterfaceMethodref)\s+#\d+\.#\d+\s+//\s+(\S+)\.(\S+):(\S+)'
)
CP_CLASS = re.compile(r'#\d+ = Class\s+#\d+\s+//\s+(\S+)')

# From bytecode lines
BC_METHOD = re.compile(
    r'\b(invokestatic|invokevirtual|invokespecial|invokeinterface)\s+#\d+\s*//\s*(?:Interface)?Method\s+(\S+)\.(\S+):(\S+)'
)
BC_FIELD = re.compile(
    r'\b(getstatic|putstatic|getfield|putfield)\s+#\d+\s*//\s*Field\s+(\S+)\.(\S+):(\S+)'
)

interface_method_classes = set()

def is_own(cls):
    return cls.startswith("com/voiddim/")

def is_java(cls):
    return cls.startswith("java/") or cls.startswith("javax/") or cls == "org/apache/logging/log4j/Logger"

def note_class(cls):
    if cls.startswith('['): return
    if is_own(cls) or is_java(cls): return
    seen_classes.add(cls)

with open(INPUT) as f:
    for line in f:
        m = CP_MEMBER.search(line)
        if m:
            kind, cls, name, desc = m.group(1), m.group(2), m.group(3), m.group(4)
            if is_own(cls) or is_java(cls): continue
            note_class(cls)
            if kind == "Fieldref":
                fields[cls].add((name, desc))
            else:
                methods[cls].add((name, desc))
                if kind == "InterfaceMethodref":
                    interface_method_classes.add(cls)
            continue
        m = CP_CLASS.search(line)
        if m:
            note_class(m.group(1))
            continue
        m = BC_METHOD.search(line)
        if m:
            op, cls, name, desc = m.group(1), m.group(2), m.group(3), m.group(4)
            if not is_own(cls) and not is_java(cls) and op == "invokestatic":
                static_methods.add((cls, name, desc))
            continue
        m = BC_FIELD.search(line)
        if m:
            op, cls, name, desc = m.group(1), m.group(2), m.group(3), m.group(4)
            if not is_own(cls) and not is_java(cls) and op in ("getstatic", "putstatic"):
                static_fields.add((cls, name, desc))
            continue

# Also pick up types that show up ONLY in descriptors (not as their own CP class).
def normalize_typeref(t):
    """Java-style 'net.minecraft.block.Block.SoundType' -> internal 'net/minecraft/block/Block$SoundType'."""
    tc = t.replace("[]", "")
    if tc in ("void","boolean","byte","char","short","int","long","float","double"):
        return None
    # First transform: dots -> slashes
    parts = tc.split(".")
    # Heuristic: if a part starts with uppercase and it's not the last uppercase piece,
    # the following uppercase parts are inner classes.
    # Simpler: find first uppercase-starting component; everything from there is class-chain.
    class_start = None
    for i, p in enumerate(parts):
        if p and p[0].isupper():
            class_start = i
            break
    if class_start is None:
        return None  # weird, all lowercase; skip
    pkg = "/".join(parts[:class_start])
    class_chain = "$".join(parts[class_start:])
    return f"{pkg}/{class_chain}" if pkg else class_chain

def sweep_types():
    added = True
    while added:
        added = False
        for cls in list(methods.keys()) + list(fields.keys()):
            for (_, desc) in list(methods.get(cls, [])):
                params, ret = parse_method_desc(desc)
                for t in params + [ret]:
                    tc = normalize_typeref(t)
                    if not tc or is_java(tc) or is_own(tc): continue
                    if tc not in seen_classes:
                        seen_classes.add(tc); added = True
            for (_, desc) in list(fields.get(cls, [])):
                t = parse_field_desc(desc)
                tc = normalize_typeref(t)
                if not tc or is_java(tc) or is_own(tc): continue
                if tc not in seen_classes:
                    seen_classes.add(tc); added = True
sweep_types()  # extras are added after KNOWN_EXTENDS section below

# -------------- ordem/heurísticas de tipo --------------

# Classes que sabemos serem interfaces (via InterfaceMethodref + conhecidas)
KNOWN_INTERFACES = {
    "net/minecraft/util/IProgressUpdate",
    "net/minecraft/util/IChatComponent",
    "net/minecraft/world/IBlockAccess",
    "net/minecraft/world/chunk/IChunkProvider",
    "net/minecraft/util/IIcon",
    "net/minecraft/client/renderer/texture/IIconRegister",
    "net/minecraftforge/common/IExtendedEntityProperties",
    "cpw/mods/fml/client/registry/ISimpleBlockRenderingHandler",
    "cpw/mods/fml/relauncher/IFMLLoadingPlugin",
}

# Classes que APARECEM em InterfaceMethodref MAS NA VERDADE são abstract classes:
FORCE_CLASS = {
    "net/minecraftforge/client/IRenderHandler",  # é abstract class
    "net/minecraft/nbt/NBTBase",                 # é abstract class
}

# Superclass hints: para permitir bytecode que faz `checkcast` funcionar
KNOWN_EXTENDS = {
    "net/minecraft/entity/player/EntityPlayerMP":  "net.minecraft.entity.player.EntityPlayer",
    "net/minecraft/world/WorldServer":             "net.minecraft.world.World",
    "net/minecraft/entity/player/EntityPlayer":    "net.minecraft.entity.EntityLivingBase",
    "net/minecraft/entity/EntityLivingBase":       "net.minecraft.entity.Entity",
    "net/minecraft/client/multiplayer/WorldClient":"net.minecraft.world.World",
    "net/minecraftforge/event/entity/living/LivingDeathEvent": "net.minecraftforge.event.entity.living.LivingEvent",
    "net/minecraftforge/event/entity/living/LivingEvent":      "net.minecraftforge.event.entity.EntityEvent",
    "net/minecraftforge/event/entity/EntityEvent":             "net.minecraftforge.event.Event",
    "net/minecraftforge/event/entity/EntityEvent$EntityConstructing": "net.minecraftforge.event.entity.EntityEvent",
    "net/minecraftforge/event/entity/player/PlayerEvent$Clone":       "net.minecraftforge.event.entity.player.PlayerEvent",
    "cpw/mods/fml/common/gameevent/TickEvent$PlayerTickEvent":        "cpw.mods.fml.common.gameevent.TickEvent",
    "cpw/mods/fml/common/gameevent/PlayerEvent$PlayerRespawnEvent":   "cpw.mods.fml.common.gameevent.PlayerEvent",
    "net/minecraftforge/client/event/RenderPlayerEvent$Specials$Post":"net.minecraftforge.client.event.RenderPlayerEvent",
}

# Extra classes we know we need (superclass chain) that might not be referenced directly.
EXTRA_CLASSES = {
    "net/minecraftforge/event/entity/living/LivingEvent",
    "net/minecraftforge/event/entity/EntityEvent",
    "net/minecraftforge/event/Event",
    "net/minecraftforge/event/entity/player/PlayerEvent",
    "cpw/mods/fml/common/gameevent/PlayerEvent",  # different package from forge event
}
for c in EXTRA_CLASSES:
    seen_classes.add(c)

# Se aparecer só como InterfaceMethodref → interface. Se aparecer nos dois → assume class.
def is_interface(cls):
    if cls in FORCE_CLASS: return False
    if cls in KNOWN_INTERFACES: return True
    return cls in interface_method_classes and cls not in [c for (c,_,_) in static_methods]

# -------------- emissão --------------

os.makedirs(OUT_DIR, exist_ok=True)

# Colect inner classes to emit within their outer.
inner_of = defaultdict(list)  # outer -> [full_inner_names]
for cls in list(seen_classes):
    if '$' in cls:
        outer = cls.rsplit('$', 1)[0]
        # If the outer isn't in seen_classes, add it so it gets a file.
        if outer not in seen_classes and not is_own(outer) and not is_java(outer):
            seen_classes.add(outer)
        inner_of[outer].append(cls)

def java_pkg(cls):
    parts = cls.split("/")
    return "/".join(parts[:-1]), parts[-1]

def emit_member_lines(cls, mset, fset, is_iface, indent="    "):
    lines = []
    # Emit fields.
    for (name, desc) in sorted(fset):
        t = parse_field_desc(desc)
        is_static = (cls, name, desc) in static_fields
        if is_iface:
            # In interfaces, fields must be public static final with initializer.
            lines.append(f"{indent}public static final {t} {name} = {default_value(t)};")
        elif is_static:
            lines.append(f"{indent}public static {t} {name};")
        else:
            lines.append(f"{indent}public {t} {name};")

    # Emit methods.
    seen_signatures = set()  # avoid duplicates
    # Always add a no-arg constructor for classes (so `new Foo()` compiles).
    if not is_iface:
        seen_signatures.add(("<init>", "()V"))
        cls_short = cls.rsplit("/", 1)[-1].split("$")[-1]
        lines.append(f"{indent}public {cls_short}() {{}}")
    for (name, desc) in sorted(mset):
        # javap prints <init>/<clinit> with literal quotes: `"<init>"`, `"<clinit>"`.
        clean_name = name.strip('"')
        if clean_name == "<clinit>": continue
        if (clean_name, desc) in seen_signatures: continue
        seen_signatures.add((clean_name, desc))
        params, ret = parse_method_desc(desc)
        is_static = (cls, clean_name, desc) in static_methods
        args_str = ", ".join(f"{p} p{i}" for i, p in enumerate(params))
        if clean_name == "<init>":
            if is_iface: continue
            cls_short = cls.rsplit("/", 1)[-1].split("$")[-1]
            # Skip no-arg ctor already emitted above.
            if desc == "()V": continue
            lines.append(f"{indent}public {cls_short}({args_str}) {{}}")
        else:
            if is_iface:
                lines.append(f"{indent}public {ret} {clean_name}({args_str});")
            else:
                stat = " static" if is_static else ""
                ret_stmt = "" if ret == "void" else f" return {default_value(ret)};"
                lines.append(f"{indent}public{stat} {ret} {clean_name}({args_str}) {{{ret_stmt} }}")
    return lines

# Filter: only emit "outer" classes; inner classes are emitted inside them.
# The seen_classes set may contain either "pkg/Outer" or "pkg/Outer$Inner" — we
# need to ensure inner-class entries are collected into inner_of[outer], not
# emitted as separate files.

# Also, sweep_types may have introduced Outer.Inner as "pkg/Outer.Inner" (with .).
# Normalize: any name with a '.' after the last '/' is an inner class.
def is_inner_name(cls):
    tail = cls.rsplit('/', 1)[-1]
    return '.' in tail or '$' in tail

for cls in list(seen_classes):
    if is_inner_name(cls):
        # Normalize to $-form for consistency
        norm = cls.replace('.', '$')
        seen_classes.discard(cls)
        seen_classes.add(norm)
        # Also make sure it's registered in inner_of.
        outer = norm.rsplit('$', 1)[0]
        if outer not in inner_of or norm not in inner_of[outer]:
            inner_of[outer].append(norm)
        if outer not in seen_classes and not is_own(outer) and not is_java(outer):
            seen_classes.add(outer)

generated = 0
for cls in sorted(seen_classes):
    if is_inner_name(cls):
        continue  # emitted inside outer
    pkg, name = java_pkg(cls)
    if not pkg:
        # unnamed package: skip
        continue
    outpath = os.path.join(OUT_DIR, pkg)
    os.makedirs(outpath, exist_ok=True)

    iface = is_interface(cls)
    kw = "interface" if iface else "class"
    ext = ""
    if not iface and cls in KNOWN_EXTENDS:
        ext = " extends " + KNOWN_EXTENDS[cls]

    lines = []
    lines.append(f"package {pkg.replace('/', '.')};")
    lines.append("")
    lines.append("/** AUTO-GENERATED STUB - compile time only. */")
    # Suppress ALL warnings so we don't drown in noise.
    lines.append("@SuppressWarnings({\"all\"})")
    lines.append(f"public {kw} {name}{ext} {{")

    lines.extend(emit_member_lines(cls, methods.get(cls, set()), fields.get(cls, set()), iface))

    # Inner classes
    for inner_cls in sorted(inner_of.get(cls, [])):
        inner_name = inner_cls.split('$', 1)[1]
        inner_iface = is_interface(inner_cls)
        inner_kw = "interface" if inner_iface else "class"
        lines.append(f"    public static {inner_kw} {inner_name} {{")
        lines.extend(emit_member_lines(inner_cls,
                                       methods.get(inner_cls, set()),
                                       fields.get(inner_cls, set()),
                                       inner_iface,
                                       indent="        "))
        lines.append("    }")

    lines.append("}")

    with open(os.path.join(outpath, name + ".java"), "w") as fh:
        fh.write("\n".join(lines))
    generated += 1

# ---------- Manual patches ----------
# Add missing members that our NEW code uses but the OLD jar didn't reference.

def append_to_stub(path, extra_lines):
    """Insert extra lines just before the last '}' of the file."""
    with open(path) as f:
        content = f.read()
    # Find last '}' and insert before it.
    idx = content.rfind('}')
    if idx == -1: return
    new_content = content[:idx] + "\n" + "\n".join(extra_lines) + "\n" + content[idx:]
    with open(path, 'w') as f:
        f.write(new_content)

# GL11: extra constants used by CloudRenderHandler.renderInventoryBlock.
append_to_stub(f"{OUT_DIR}/org/lwjgl/opengl/GL11.java", [
    "    public static final int GL_CULL_FACE = 2884;",
    "    public static final int GL_TEXTURE_2D = 3553;",
    "    public static final int GL_LIGHTING = 2896;",
    "    public static final int GL_BLEND = 3042;",
    "    public static final int GL_ALPHA_TEST = 3008;",
    "    public static final int GL_FOG = 2912;",
    "    public static final int GL_COMPILE = 4864;",
    "    public static final int GL_TRIANGLE_STRIP = 5;",
    "    public static boolean glIsEnabled(int cap) { return false; }",
])

# ISimpleBlockRenderingHandler: the actual interface methods.
with open(f"{OUT_DIR}/cpw/mods/fml/client/registry/ISimpleBlockRenderingHandler.java", "w") as f:
    f.write("""package cpw.mods.fml.client.registry;

/** AUTO-GENERATED STUB - compile time only. */
@SuppressWarnings({"all"})
public interface ISimpleBlockRenderingHandler {
    void renderInventoryBlock(net.minecraft.block.Block block, int metadata, int modelId,
                              net.minecraft.client.renderer.RenderBlocks renderer);
    boolean renderWorldBlock(net.minecraft.world.IBlockAccess world, int x, int y, int z,
                             net.minecraft.block.Block block, int modelId,
                             net.minecraft.client.renderer.RenderBlocks renderer);
    boolean shouldRender3DInInventory(int modelId);
    int getRenderId();
}
""")

print(f"Generated {generated} stub files in {OUT_DIR}/")
