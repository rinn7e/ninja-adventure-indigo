#!/usr/bin/env python3
"""Imports what the Indigo port needs from the reference Godot project (assets/NinjaAdventure Godot V4).

Pure Python (standard library only). Run from the repository root:

    python3 ninja/tools/import_godot.py

It does two things:

1. Copies the images, music and font the game uses into ninja/assets/ (and ninja/fonts/), with
   flat, unique file names (Indigo's asset list is flat).
2. Reads the village map (content/map/map_village.tscn) and its tileset (content/map/tileset.tres)
   and writes ninja/src/ninja/common/constant/VillageTiles.scala: every tile of every layer, the
   tileset's per-tile data the game needs (collision polygon, y-sort origin, z-index), and the
   destroyable props placed as scene tiles.

Godot's TileMap `tile_data` stores each tile as three ints:
    [ (y << 16) | (x & 0xFFFF),  source | (atlasX << 16),  atlasY | (alternative << 16) ]
(x and y are signed 16-bit, so y is the arithmetic `value >> 16`).
"""

import re
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
GODOT = ROOT / "assets" / "NinjaAdventure Godot V4"
ASSETS = ROOT / "ninja" / "assets"
FONTS = ROOT / "ninja" / "fonts"
OUT_SCALA = ROOT / "ninja" / "src" / "ninja" / "common" / "constant" / "VillageTiles.scala"

# Godot file (relative to the Godot project) -> name in ninja/assets.
ASSET_FILES = {
    "content/map/tileset_village_abandoned.png": "tileset_village_abandoned.png",
    "content/map/tileset_floor.png": "tileset_floor.png",
    "content/map/tileset_interior_floor.png": "tileset_interior_floor.png",
    "content/map/tileset_animated.png": "tileset_animated.png",
    "content/map/tileset_wall_simple.png": "tileset_wall_simple.png",
    "content/character/ninja_blue/sprite.png": "ninja_blue.png",
    "content/character/samurai_green/samurai_green.png": "samurai_green.png",
    "content/character/samurai_blue/sprite.png": "samurai_blue.png",
    "content/character/pig/pig.png": "pig.png",
    "content/character/Shadow.png": "shadow.png",
    "content/destroyable/grass.png": "grass.png",
    "content/destroyable/pot.png": "pot.png",
    "content/destroyable/crate.png": "crate.png",
    "content/particle/particle_grass.png": "particle_grass.png",
    "content/particle/particle_pot.png": "particle_pot.png",
    "content/particle/particle_rock.png": "particle_rock.png",
    "content/ui/heart.png": "heart.png",
    "system/environment/fx/rain.png": "fx_rain.png",
    "system/environment/fx/cloud.png": "fx_cloud.png",
    "system/environment/fx/leaf.png": "fx_leaf.png",
    "system/environment/fx/fog.png": "fx_fog.png",
    "system/environment/fx/raylight.png": "fx_raylight.png",
    "audio/music/theme_dream.ogg": "music_dream.ogg",
    "audio/music/theme_swamp.ogg": "music_swamp.ogg",
}
FONT_FILES = {"theme/font_normal.ttf": "font_normal.ttf"}

LAYER_NAMES = {0: "wall", 1: "wall2", 2: "floorDetail", 3: "floor"}


def copy_assets():
    ASSETS.mkdir(parents=True, exist_ok=True)
    FONTS.mkdir(parents=True, exist_ok=True)
    for src, name in ASSET_FILES.items():
        shutil.copyfile(GODOT / src, ASSETS / name)
    for src, name in FONT_FILES.items():
        shutil.copyfile(GODOT / src, FONTS / name)
    print(f"copied {len(ASSET_FILES)} assets and {len(FONT_FILES)} font(s)")


def decode_cell(value):
    """Low 16 bits: x, high 16 bits: y, both signed."""
    x = ((value & 0xFFFF) ^ 0x8000) - 0x8000
    y = value >> 16
    return x, y


def read_map():
    text = (GODOT / "content/map/map_village.tscn").read_text()
    layers = {}
    props = []
    for m in re.finditer(r"layer_(\d)/tile_data = PackedInt32Array\(([^)]*)\)", text):
        nums = [int(v) for v in m.group(2).split(",") if v.strip()]
        tiles = []
        for i in range(0, len(nums), 3):
            x, y = decode_cell(nums[i])
            source, atlas_x = nums[i + 1] & 0xFFFF, nums[i + 1] >> 16
            atlas_y, alternative = nums[i + 2] & 0xFFFF, nums[i + 2] >> 16
            if source == 5:  # scene collection: destroyable props, the alternative is the scene id
                props.append((x, y, alternative))
            else:
                tiles.append((x, y, source, atlas_x, atlas_y))
        layers[int(m.group(1))] = sorted(tiles, key=lambda t: (t[1], t[0]))
    origin = re.search(r'\[node name="Tilemap" parent="\." index="1"\]\nposition = Vector2\(([-\d.]+), ([-\d.]+)\)', text)
    return layers, sorted(props), (int(float(origin.group(1))), int(float(origin.group(2))))


def read_tileset():
    """Per (source, atlasX, atlasY): collision polygon, y-sort origin, z-index, animation frames."""
    text = (GODOT / "content/map/tileset.tres").read_text()
    ext = dict(re.findall(r'\[ext_resource type="Texture2D" [^\]]*path="res://([^"]+)" id="([^"]+)"\]', text))
    ext = {v: k for k, v in ext.items()}
    sub_to_source = {sub: int(n) for n, sub in re.findall(r'sources/(\d+) = SubResource\("([^"]+)"\)', text)}
    textures, info = {}, {}
    for block in re.split(r'\n(?=\[sub_resource )', text):
        head = re.match(r'\[sub_resource type="TileSetAtlasSource" id="([^"]+)"\]', block)
        if not head:
            continue
        source = sub_to_source[head.group(1)]
        textures[source] = Path(ext[re.search(r'texture = ExtResource\("([^"]+)"\)', block).group(1)]).name

        def tile(ax, ay):
            return info.setdefault((source, int(ax), int(ay)), {"polygon": None, "ysort": 0, "z": 0, "frames": 1})

        # Every tile in this tileset has at most one collision polygon (polygon_0).
        for ax, ay, pts in re.findall(r'(\d+):(\d+)/0/physics_layer_0/polygon_0/points = PackedVector2Array\(([^)]*)\)', block):
            tile(ax, ay)["polygon"] = [round(float(v), 3) for v in pts.split(",")]
        for ax, ay, v in re.findall(r'(\d+):(\d+)/0/y_sort_origin = (-?\d+)', block):
            tile(ax, ay)["ysort"] = int(v)
        for ax, ay, v in re.findall(r'(\d+):(\d+)/0/z_index = (-?\d+)', block):
            tile(ax, ay)["z"] = int(v)
        for ax, ay in re.findall(r'(\d+):(\d+)/animation_frame_\d+/duration', block):
            tile(ax, ay)["frames"] += 1
    for key, t in info.items():  # the count above includes frame 0 once more
        if t["frames"] > 1:
            t["frames"] -= 1
    return textures, info


def fmt(value):
    return str(int(value)) if value == int(value) else str(value)


def encode(rows):
    return ";".join(",".join(str(v) for v in row) for row in rows)


def write_scala(layers, props, origin, textures, info):
    used = {(s, ax, ay) for tiles in layers.values() for (_, _, s, ax, ay) in tiles}
    tile_rows = []
    for key in sorted(used):
        t = info.get(key, {"polygon": None, "ysort": 0, "z": 0, "frames": 1})
        polygon = t["polygon"] or []
        tile_rows.append([*key, t["ysort"], t["z"], t["frames"], *[fmt(v) for v in polygon]])

    def s(value):
        return '"' + value + '"'

    lines = [
        "package ninja.common.constant",
        "",
        "import indigo.*",
        "",
        "// DO NOT EDIT: generated by ninja/tools/import_godot.py from the reference Godot project's",
        "// content/map/map_village.tscn and content/map/tileset.tres.",
        "",
        "/** The village map's raw data, decoded by `ninja.common.util.TileMap`. Rows are `;`-separated,",
        "  * fields `,`-separated.",
        "  */",
        "object VillageTiles:",
        "",
        "  /** Where the Godot TileMap node sits in the world; a cell's centre is `origin + cell * 16 + 8`. */",
        f"  val origin: Point = Point({origin[0]}, {origin[1]})",
        "",
        "  /** Tileset textures by Godot source id, as asset names. */",
        "  val sources: Map[Int, AssetName] =",
        "    Map(",
    ]
    lines += [f'      {k} -> AssetName("{v}"),' for k, v in sorted(textures.items())]
    lines[-1] = lines[-1].rstrip(",")
    lines += ["    )", ""]
    for n, name in LAYER_NAMES.items():
        lines += [f"  /** Layer `{n}` ({name}): cellX, cellY, source, atlasX, atlasY. */",
                  f"  val {name}: String =", f"    {s(encode(layers.get(n, [])))}", ""]
    lines += [
        "  /** Per tile: source, atlasX, atlasY, ySortOrigin, zIndex, animation frames, then its collision",
        "    * polygon's points (x, y pairs relative to the tile centre), if it has one.",
        "    */",
        "  val tiles: String =",
        f"    {s(encode(tile_rows))}",
        "",
        "  /** Destroyable props placed as scene tiles: cellX, cellY, scene (1 crate, 2 grass, 3 pot). */",
        "  val props: String =",
        f"    {s(encode(props))}",
        "",
    ]
    OUT_SCALA.parent.mkdir(parents=True, exist_ok=True)
    OUT_SCALA.write_text("\n".join(lines))
    counts = {LAYER_NAMES[n]: len(t) for n, t in layers.items()}
    print(f"wrote {OUT_SCALA.relative_to(ROOT)}: tiles {counts}, {len(tile_rows)} tile kinds, {len(props)} props")


def main():
    copy_assets()
    layers, props, origin = read_map()
    textures, info = read_tileset()
    write_scala(layers, props, origin, textures, info)


if __name__ == "__main__":
    main()
