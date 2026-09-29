"""Reads the Godot 3 version of the demo (assets/NinjaAdventure Godot V3, MIT, (c) 2020 Emilio
Coppola): its TileSets and TileMaps, and the objects placed in its scenes. Used by import_godot.py.

Godot 3 differs from Godot 4 here:

- A TileSet tile has a texture region; "atlas" and "autotile" tiles split it into subtiles of
  `autotile/tile_size` (which can be bigger than a 16px cell: trees are 64x48), picked by the cell's
  autotile coordinate. Collision shapes are per subtile, in the subtile's texture coordinates.
- A TileMap's `tile_data` is three ints per cell: `(y << 16) | (x & 0xFFFF)`, the tile id with the
  flip flags in bits 29-31 (flip h, flip v, transpose), and the autotile coordinate
  `(ay << 16) | ax`.
- `cell_tile_origin` places a tile bigger than its cell: 0 top-left, 1 centred on the cell, 2 its
  bottom-left on the cell's bottom-left. With `cell_y_sort`, each cell sorts by its origin point
  (top, centre or bottom of the cell).
"""

import re
from pathlib import Path


def ext_resources(text):
    return {int(i): p for p, i in re.findall(r'\[ext_resource path="res://([^"]+)" type="\w+" id=(\d+)\]', text)}


def _shapes(text):
    out = {}
    pattern = r'\[sub_resource type="ConvexPolygonShape2D" id=(\d+)\]\s*points = PoolVector2Array\(([^)]*)\)'
    for i, pts in re.findall(pattern, text):
        v = [float(x) for x in pts.replace(" ", "").split(",") if x]
        out[int(i)] = list(zip(v[0::2], v[1::2]))
    return out


def numbers(s):
    """The numbers inside `Name( ... )`, e.g. Vector2( 1, 2 ) -> (1.0, 2.0)."""
    inner = s[s.index("(") + 1 :] if "(" in s else s
    return tuple(float(x) for x in re.findall(r"-?\d+(?:\.\d+)?", inner))


def parse_tileset(text, start_marker="[resource]"):
    """id -> {texture, region, mode, size, spacing, shapes{(ax, ay): [polygon]}, z, tex_offset}."""
    exts = ext_resources(text)
    shapes = _shapes(text)
    body = text[text.index(start_marker) :] if start_marker in text else text
    tiles = {}
    for m in re.finditer(r"^(\d+)/(\w+(?:/\w+)?) = (.*?)(?=^\d+/|\Z|^\[)", body, re.S | re.M):
        tid, key, val = int(m.group(1)), m.group(2), m.group(3).strip()
        t = tiles.setdefault(tid, {"shapes": {}, "z": 0, "tex_offset": (0, 0), "spacing": 0, "mode": 0, "size": None})
        if key == "texture":
            t["texture"] = exts[int(re.search(r"ExtResource\( (\d+) \)", val).group(1))]
        elif key == "region":
            t["region"] = numbers(val)
        elif key == "tile_mode":
            t["mode"] = int(val)
        elif key == "autotile/tile_size":
            t["size"] = numbers(val)
        elif key == "autotile/spacing":
            t["spacing"] = int(val)
        elif key == "z_index":
            t["z"] = int(val)
        elif key == "tex_offset":
            t["tex_offset"] = numbers(val)
        elif key == "shapes":
            pattern = (
                r'"autotile_coord": Vector2\( ([-\d.]+), ([-\d.]+) \).*?"shape": SubResource\( (\d+) \)'
                r'.*?"shape_transform": Transform2D\(([^)]*)\)'
            )
            for sm in re.finditer(pattern, val, re.S):
                tr = numbers("(" + sm.group(4))
                pts = [(x + tr[4], y + tr[5]) for x, y in shapes[int(sm.group(3))]]
                coord = (int(float(sm.group(1))), int(float(sm.group(2))))
                t["shapes"].setdefault(coord, []).append(pts)
    return tiles


def tileset_for(root, scene_text, ref):
    """The TileSet a TileMap uses: an external .tres, or one embedded in the scene."""
    m = re.match(r"(Ext|Sub)Resource\( (\d+) \)", ref)
    if m.group(1) == "Ext":
        return parse_tileset((root / ext_resources(scene_text)[int(m.group(2))]).read_text())
    marker = '[sub_resource type="TileSet" id=%s]' % m.group(2)
    start = scene_text.index(marker)
    end = scene_text.find("\n[", start + 1)
    return parse_tileset(scene_text[: end if end > 0 else len(scene_text)], start_marker=marker)


def tilemaps(text):
    """TileMap nodes: name, parent, tile_set, z, origin, y_sort, cells [(x, y, tile, flags, ax, ay)]."""
    out = []
    for m in re.finditer(r'\[node name="([^"]+)" type="TileMap" parent="([^"]*)"[^\]]*\]((?:(?!\n\[).)*)', text, re.S):
        body = m.group(3)

        def get(key, default=None):
            mm = re.search(r"^%s = (.*)$" % key, body, re.M)
            return mm.group(1) if mm else default

        tdm = re.search(r"tile_data = PoolIntArray\(([^)]*)\)", body)
        data = [int(x) for x in tdm.group(1).replace(" ", "").split(",") if x] if tdm else []
        cells = []
        for i in range(0, len(data), 3):
            c, t, a = data[i], data[i + 1], data[i + 2]
            x = ((c & 0xFFFF) ^ 0x8000) - 0x8000
            cells.append((x, c >> 16, t & 0x1FFFFFFF, (t >> 29) & 7, a & 0xFFFF, a >> 16))
        out.append(
            dict(
                name=m.group(1),
                parent=m.group(2),
                tile_set=get("tile_set"),
                z=int(get("z_index", "0")),
                origin=int(get("cell_tile_origin", "0")),
                y_sort=get("cell_y_sort", "false") == "true",
                cells=cells,
            )
        )
    return out


def _flip(point, size, flags):
    x, y = point
    w, h = size
    if flags & 4:
        x, y = y, x
    if flags & 1:
        x = w - x
    if flags & 2:
        y = h - y
    return (x, y)


def sprites(root, scenes):
    """Every tile drawn by the given (scene, TileMap parent, world offset) triples, in scene order:
    {texture, src (x, y, w, h), dest (x, y), flags, z, sort (y or None), polys [[(x, y)]]}.
    """
    out = []
    for path, parent, (ox, oy) in scenes:
        text = (root / path).read_text()
        for tm in tilemaps(text):
            if tm["parent"] != parent:
                continue
            tileset = tileset_for(root, text, tm["tile_set"])
            known = {(c[0], c[1]): c for c in tm["cells"] if c[2] in tileset}
            for x, y, tid, flags, ax, ay in tm["cells"]:
                if tid not in tileset:
                    # A deleted tile: Godot leaves a hole. In a floor layer, patch it with a
                    # neighbour's tile; elsewhere skip it, as Godot does.
                    near = next((known[n] for n in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)) if n in known), None)
                    if tm["y_sort"] or near is None:
                        continue
                    tid, flags, ax, ay = near[2], near[3], near[4], near[5]
                t = tileset[tid]
                rx, ry, rw, rh = t["region"]
                if t["mode"] == 0:
                    src = (rx, ry, rw, rh)
                    shapes = t["shapes"].get((0, 0), [])
                else:
                    w, h = t["size"]
                    sp = t["spacing"]
                    src = (rx + ax * (w + sp), ry + ay * (h + sp), w, h)
                    shapes = t["shapes"].get((ax, ay), [])
                w, h = (src[3], src[2]) if flags & 4 else (src[2], src[3])
                cx, cy = ox + x * 16, oy + y * 16
                tx, ty = t["tex_offset"]
                if tm["origin"] == 1:
                    dest, sort = (cx + 8 - w / 2 + tx, cy + 8 - h / 2 + ty), cy + 8
                elif tm["origin"] == 2:
                    dest, sort = (cx + tx, cy + 16 - h + ty), cy + 16
                else:
                    dest, sort = (cx + tx, cy + ty), cy
                polys = [
                    [(dest[0] + px, dest[1] + py) for px, py in (_flip(p, (src[2], src[3]), flags) for p in poly)]
                    for poly in shapes
                ]
                out.append(
                    dict(
                        texture=t["texture"],
                        src=tuple(int(v) for v in src),
                        dest=(int(dest[0]), int(dest[1])),
                        flags=flags,
                        z=tm["z"] + t["z"],
                        sort=int(sort) if tm["y_sort"] else None,
                        polys=polys,
                    )
                )
    return out


def nodes(text, parent_filter=None):
    """Instanced / typed nodes: [{name, parent, type or instance path, position, props{key: raw}}]."""
    exts = ext_resources(text)
    out = []
    for m in re.finditer(r'\[node name="([^"]+)"([^\]]*)\]((?:(?!\n\[).)*)', text, re.S):
        attrs, body = m.group(2), m.group(3)
        parent = re.search(r'parent="([^"]*)"', attrs)
        inst = re.search(r"instance=ExtResource\( (\d+) \)", attrs)
        typ = re.search(r'type="(\w+)"', attrs)
        props = dict(re.findall(r"^(\w+) = (.*)$", body, re.M))
        pos = numbers(props["position"]) if "position" in props else (0.0, 0.0)
        out.append(
            dict(
                name=m.group(1),
                parent=parent.group(1) if parent else None,
                kind=exts[int(inst.group(1))] if inst else (typ.group(1) if typ else None),
                position=pos,
                props=props,
            )
        )
    return [n for n in out if parent_filter is None or n["parent"] == parent_filter]
