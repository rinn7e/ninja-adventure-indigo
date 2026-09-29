#!/usr/bin/env python3
"""Imports what the Indigo port needs from the reference Godot project (assets/NinjaAdventure Godot V4).

The Godot project isn't in this repository (it has no licence): download it from
https://github.com/pixel-boy/NinjaAdventure into assets/NinjaAdventure Godot V4/ first. The
combat assets come from the older Godot 3 version (MIT), expected in
assets/NinjaAdventure Godot V3/.

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

import godot3

ROOT = Path(__file__).resolve().parents[2]
GODOT = ROOT / "assets" / "NinjaAdventure Godot V4"
# The older Godot 3 version (MIT, (c) 2020 Emilio Coppola), for the combat that V4 lacks.
GODOT3 = ROOT / "assets" / "NinjaAdventure Godot V3"
ASSETS = ROOT / "ninja" / "assets"
FONTS = ROOT / "ninja" / "fonts"
OUT_SCALA = ROOT / "ninja" / "src" / "ninja" / "common" / "constant" / "VillageTiles.scala"
OUT_NORTH = ROOT / "ninja" / "src" / "ninja" / "common" / "constant" / "NorthVillageTiles.scala"

# --- The Godot 3 world, joined north of the V4 village -------------------------------------------
#
# Its three places, with their offsets inside the Godot 3 world (World.tscn).
NORTH_SCENES = [
    ("World/Maps/Village.tscn", ".", (0, 0)),
    ("World/Maps/Interior.tscn", ".", (342, -555)),  # Godot: (342.228, -555.006)
    ("World/World.tscn", "YSort/Dungeon", (380, -960)),
]
# Where the Godot 3 world goes in ours: north of the V4 village, lined up with the camera grid
# (a Godot 3 screen, 320x176 from (0, 0), lands exactly on one of ours: our screens start at
# (-136, -72) modulo (320, 176)). Its southern tree line then runs along the V4 village's open
# north edge, and its lake lies east of the V4 village.
NORTH_OFFSET = (-456, -1128)
# The Godot 3 village's path runs south up to its tree line (y 848-879) at x 704-767; below the
# tree line is an unfinished strip leading off the map, like the V4 village's open north edge.
# Opening the tree line there joins the two villages.
PASSAGE = (704, 768)
TREE_LINE = (848, 880)  # rows 848 and 864
# The V4 house interior sits north of the V4 village, where the Godot 3 village now is: it moves
# two screens east (its teleporter and zone move with it, in Village.scala).
V4_INTERIOR_TOP = -20  # V4 rows at or above this are the interior
V4_INTERIOR_SHIFT = 40  # tiles


# Godot file (relative to the Godot project) -> name in ninja/assets.
ASSET_FILES = {
    "content/map/tileset_village_abandoned.png": "tileset_village_abandoned.png",
    "content/map/tileset_floor.png": "tileset_floor.png",
    "content/map/tileset_interior_floor.png": "tileset_interior_floor.png",
    "content/map/tileset_animated.png": "tileset_animated.png",
    "content/map/tileset_wall_simple.png": "tileset_wall_simple.png",
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
    "system/environment/fx/rain_on_floor.png": "fx_rain_on_floor.png",
    "system/environment/fx/snow.png": "fx_snow.png",
    "system/environment/fx/cloud.png": "fx_cloud.png",
    "system/environment/fx/leaf.png": "fx_leaf.png",
    "system/environment/fx/fog.png": "fx_fog.png",
    "system/environment/fx/raylight.png": "fx_raylight.png",
    "audio/music/theme_dream.ogg": "music_dream.ogg",
    "audio/music/theme_swamp.ogg": "music_swamp.ogg",
}
# Godot 3 file (relative to that project) -> name in ninja/assets.
ASSET_FILES_V3 = {
    "World/Actor/Monster/Bamboo.png": "monster_bamboo.png",
    "Hud/LifeBarMiniUnder.png": "life_bar_mini_under.png",
    "Hud/LifeBarMiniProgress.png": "life_bar_mini_progress.png",
    "World/Actor/Weapon/Sprite/Lance.png": "weapon_lance.png",
    "World/Actor/Weapon/Fx.png": "fx_impact.png",
    "Hud/Tuto.png": "tutorial.png",
    "World/Actor/Monster/SndDeath.wav": "snd_hit.wav",
    "World/Ld/DestroyableItem/SndGrass.wav": "snd_grass.wav",
    "World/Particle/Spark.png": "fx_spark.png",
    "World/FX/Smoke/SpriteSheet.png": "fx_smoke.png",
    "Resource/Music/23 - Road.ogg": "music_road.ogg",
    "Resource/Music/18 - Aquatic.ogg": "music_aquatic.ogg",
    "Resource/Music/13 - Mystical.ogg": "music_mystical.ogg",
    "Resource/Music/16 - Melancholia.ogg": "music_melancholia.ogg",
    # Villagers (sprite sheet and face portrait) and the dialogue box.
    "World/Actor/Npc/Samurai/SpriteSheet.png": "npc_samurai.png",
    "World/Actor/Npc/Samurai/Faceset.png": "face_samurai.png",
    "World/Actor/Npc/RedNinja/SpriteSheet.png": "npc_red_ninja.png",
    "World/Actor/Npc/RedNinja/Faceset.png": "face_red_ninja.png",
    "World/Actor/Npc/OldWoman/SpriteSheet.png": "npc_old_woman.png",
    "World/Actor/Npc/OldWoman/Faceset.png": "face_old_woman.png",
    "World/Actor/Npc/Warrior/SpriteSheet.png": "npc_warrior.png",
    "World/Actor/Npc/Warrior/Faceset.png": "face_warrior.png",
    "World/Actor/Npc/Dog/SpriteSheet.png": "npc_dog.png",
    "World/Actor/Npc/Dog/Faceset.png": "face_dog.png",
    "World/Actor/Npc/Monk2/SpriteSheet.png": "npc_monk.png",
    "World/Actor/Npc/Monk2/Faceset.png": "face_monk.png",
    "Hud/DialogBox/NinePathRect/DialogBoxFaceset.png": "dialog_box.png",
    "Hud/DialogBox/Arrow.png": "dialog_arrow.png",
    "Hud/DialogBox/DialogInfo.png": "dialog_info.png",
    "World/Ld/DestroyableItem/Plant/Sprite.png": "plant.png",
    # The player: Godot 3's green-hooded ninja (the pack's NinjaGreen, as in its screenshots),
    # rather than Godot 4's NinjaBlue.
    "World/Actor/Player/GreenNinja/SpriteSheet.png": "ninja_green.png",
}
FONT_FILES = {"theme/font_normal.ttf": "font_normal.ttf"}

LAYER_NAMES = {0: "wall", 1: "wall2", 2: "floorDetail", 3: "floor"}


def copy_assets():
    ASSETS.mkdir(parents=True, exist_ok=True)
    FONTS.mkdir(parents=True, exist_ok=True)
    for src, name in ASSET_FILES.items():
        shutil.copyfile(GODOT / src, ASSETS / name)
    for src, name in ASSET_FILES_V3.items():
        shutil.copyfile(GODOT3 / src, ASSETS / name)
    for src, name in FONT_FILES.items():
        shutil.copyfile(GODOT / src, FONTS / name)
    print(f"copied {len(ASSET_FILES) + len(ASSET_FILES_V3)} assets and {len(FONT_FILES)} font(s)")


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
                if y <= V4_INTERIOR_TOP:
                    x += V4_INTERIOR_SHIFT
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


def north_village(v4_layers, v4_origin):
    """The Godot 3 world's tiles, moved north of the V4 village and joined to it, plus walls along
    every edge of the combined floor that opens onto nothing."""
    tiles = godot3.sprites(GODOT3, NORTH_SCENES)

    def is_floor(t):
        return t["sort"] is None and t["z"] <= 0

    def in_passage(t):
        x, y = t["dest"]
        w, h = t["src"][2], t["src"][3]
        return x < PASSAGE[1] and x + w > PASSAGE[0] and y < TREE_LINE[1] and y + h > TREE_LINE[0]

    # Open the tree line: remove its trees in the passage and lay the path's floor through it.
    removed = [t for t in tiles if not is_floor(t) and in_passage(t)]
    tiles = [t for t in tiles if t not in removed]
    # Lay floor wherever a removed tree stood on none, copying the nearest floor tile above it.
    floors = {t["dest"]: t for t in tiles if is_floor(t) and t["src"][2:] == (16, 16)}
    for r in removed:
        x0, y0 = r["dest"]
        for x in range(x0 - x0 % 16, x0 + r["src"][2], 16):
            for y in range(y0 - y0 % 16, max(y0 + r["src"][3], TREE_LINE[1] + 16), 16):
                if (x, y) in floors:
                    continue
                source = next((floors[(x, yy)] for yy in range(y - 16, y - 16 * 12, -16) if (x, yy) in floors), None)
                if source:
                    floors[(x, y)] = dict(source, dest=(x, y), polys=[])
                    tiles.append(floors[(x, y)])

    ox, oy = NORTH_OFFSET
    moved = []
    for t in tiles:
        x, y = t["dest"]
        moved.append(
            dict(
                t,
                dest=(x + ox, y + oy),
                sort=None if t["sort"] is None else t["sort"] + oy,
                polys=[[(px + ox, py + oy) for px, py in poly] for poly in t["polys"]],
            )
        )

    # Walls along the void: on the V4 village's 16px grid, every empty cell next to floor.
    vx, vy = v4_origin

    def cell_of(x, y):
        return ((x - vx) // 16, (y - vy) // 16)

    covered = {(c[0], c[1]) for cells in v4_layers.values() for c in cells}
    for t in moved:
        if is_floor(t):
            x, y = t["dest"]
            for px in range(x, x + t["src"][2], 8):
                for py in range(y, y + t["src"][3], 8):
                    covered.add(cell_of(px + 4, py + 4))
    edges = set()
    for cx, cy in covered:
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                n = (cx + dx, cy + dy)
                if n not in covered:
                    edges.add(n)
    walls = [
        [(vx + cx * 16, vy + cy * 16), (vx + cx * 16 + 16, vy + cy * 16), (vx + cx * 16 + 16, vy + cy * 16 + 16), (vx + cx * 16, vy + cy * 16 + 16)]
        for cx, cy in sorted(edges)
    ]
    return moved, walls, len(removed)


def write_north(tiles, walls, removed):
    textures = sorted({t["texture"] for t in tiles})
    source = {tex: i for i, tex in enumerate(textures)}

    def asset(tex):
        return "v3_" + re.sub(r"(?<!^)(?=[A-Z])", "_", Path(tex).stem).lower() + ".png"

    for tex in textures:
        shutil.copyfile(GODOT3 / tex, ASSETS / asset(tex))

    def layer(t):
        if t["sort"] is not None:
            return 1
        return 2 if t["z"] > 0 else 0

    rows = [
        (source[t["texture"]], *t["src"], *t["dest"], t["flags"], layer(t), t["sort"] if t["sort"] is not None else 0)
        for t in tiles
    ]
    polys = [p for t in tiles for p in t["polys"]] + walls

    def chunked(rows):
        """A string literal can't exceed 64 KB (Scala.js), so rows are joined at run time."""
        chunks, current = [], []
        for row in rows:
            current.append(row)
            if sum(len(r) + 1 for r in current) > 20000:
                chunks.append(";".join(current))
                current = []
        if current:
            chunks.append(";".join(current))
        return "List(\n" + ",\n".join(f'      "{c}"' for c in chunks) + '\n    ).mkString(";")'

    lines = [
        "package ninja.common.constant",
        "",
        "import indigo.*",
        "",
        "// DO NOT EDIT: generated by ninja/tools/import_godot.py from the Godot 3 version of the demo",
        "// (MIT, (c) 2020 Emilio Coppola): World/Maps/Village.tscn, World/Maps/Interior.tscn and",
        "// World/World.tscn's dungeon, moved north of the V4 village and joined to it.",
        "",
        "/** The Godot 3 world's raw data, decoded by `ninja.common.util.TileMap`. Rows are `;`-separated,",
        "  * fields `,`-separated. World coordinates.",
        "  */",
        "object NorthVillageTiles:",
        "",
        "  /** Tileset textures by source id. */",
        "  val sources: Map[Int, AssetName] =",
        "    Map(",
        ",\n".join(f'      {i} -> AssetName("{asset(tex)}")' for tex, i in source.items()),
        "    )",
        "",
        "  /** Tiles, in Godot's drawing order: source, srcX, srcY, width, height, x, y, flags (1 flip h,",
        "    * 2 flip v), layer (0 under the characters, 1 y-sorted with them, 2 over them), sort y.",
        "    */",
        "  val tiles: String =",
        "    " + chunked([",".join(str(v) for v in r) for r in rows]),
        "",
        "  /** Collision polygons (x, y pairs): the tiles' shapes, then walls along the void. */",
        "  val solids: String =",
        "    " + chunked([",".join(fmt(v) for pt in p for v in pt) for p in polys]),
        "",
    ]
    OUT_NORTH.write_text("\n".join(lines))
    print(f"wrote {OUT_NORTH.relative_to(ROOT)}: {len(rows)} tiles from {len(textures)} textures, {len(polys)} polygons ({len(walls)} void walls), {removed} tree tiles opened")


def main():
    copy_assets()
    layers, props, origin = read_map()
    textures, info = read_tileset()
    write_scala(layers, props, origin, textures, info)
    tiles, walls, removed = north_village(layers, origin)
    write_north(tiles, walls, removed)


if __name__ == "__main__":
    main()
