"""Shortest walkable route between two world points, from the game's own collision data,
as key holds (100 px/s). Usage: python3 route.py x0 y0 x1 y1"""
import re, sys, heapq, math
from pathlib import Path
SRC = str(Path(__file__).resolve().parents[1] / "src" / "ninja" / "common" / "constant") + "/"
def strings(path, name):
    s = open(SRC + path).read()
    m = re.search(r'val %s: String =\s*(List\((.*?)\)\.mkString\(";"\)|"([^"]*)")' % name, s, re.S)
    return ";".join(re.findall(r'"([^"]*)"', m.group(2))) if m.group(2) else m.group(3)
polys = []
for row in strings("NorthVillageTiles.scala", "solids").split(";"):
    v = list(map(float, row.split(","))); polys.append(list(zip(v[0::2], v[1::2])))
# V4 tiles: cells with kinds that have polygons (centre-relative), origin (8,-5)
v4 = open(SRC + "VillageTiles.scala").read()
def v4s(name): return re.search(r'val %s: String =\s*"([^"]*)"' % name, v4).group(1)
kinds = {}
for r in v4s("tiles").split(";"):
    f = r.split(","); pts = list(map(float, f[6:]))
    if pts: kinds[(int(f[0]), int(f[1]), int(f[2]))] = list(zip(pts[0::2], pts[1::2]))
for layer in ("wall", "wall2", "floorDetail", "floor"):
    for r in v4s(layer).split(";"):
        x, y, s_, ax, ay = map(int, r.split(","))
        if (s_, ax, ay) in kinds:
            cx, cy = 8 + x * 16 + 8, -5 + y * 16 + 8
            polys.append([(cx + px, cy + py) for px, py in kinds[(s_, ax, ay)]])
boxes = [(min(p[0] for p in q), min(p[1] for p in q), max(p[0] for p in q), max(p[1] for p in q), q) for q in polys]
G = 4  # grid step
buckets = {}
for b in boxes:
    for bx in range(int(b[0] // 64), int(b[2] // 64) + 1):
        for by in range(int(b[1] // 64), int(b[3] // 64) + 1):
            buckets.setdefault((bx, by), []).append(b)
def seg_dist(px, py, ax, ay, bx, by):
    dx, dy = bx - ax, by - ay; l = dx * dx + dy * dy
    t = 0 if l == 0 else max(0, min(1, ((px - ax) * dx + (py - ay) * dy) / l))
    return math.hypot(px - ax - t * dx, py - ay - t * dy)
def inside(px, py, q):
    c = False
    for (x1, y1), (x2, y2) in zip(q, q[1:] + q[:1]):
        if (y1 > py) != (y2 > py) and px < (x2 - x1) * (py - y1) / (y2 - y1) + x1: c = not c
    return c
memo = {}
def free(x, y):
    if (x, y) in memo: return memo[(x, y)]
    cx, cy, r = x, y - 1, 7.0  # the player's body: r 7, 1px above the feet (+ margin)
    ok = True
    for b in buckets.get((int(cx // 64), int(cy // 64)), []) + buckets.get((int(cx // 64) + 1, int(cy // 64)), []) + buckets.get((int(cx // 64) - 1, int(cy // 64)), []) + buckets.get((int(cx // 64), int(cy // 64) + 1), []) + buckets.get((int(cx // 64), int(cy // 64) - 1), []):
        if cx + r < b[0] or cx - r > b[2] or cy + r < b[1] or cy - r > b[3]: continue
        q = b[4]
        if inside(cx, cy, q) or min(seg_dist(cx, cy, *a, *c) for a, c in zip(q, q[1:] + q[:1])) < r:
            ok = False; break
    memo[(x, y)] = ok
    return ok
x0, y0, x1, y1 = map(float, sys.argv[1:5])
s = (round(x0 / G) * G, round(y0 / G) * G); g = (round(x1 / G) * G, round(y1 / G) * G)
dist = {s: 0}; prev = {}; h = [(0, 0, s)]
while h:
    _, d, c = heapq.heappop(h)
    if c == g: break
    if d > dist[c]: continue
    for dx, dy in ((G, 0), (-G, 0), (0, G), (0, -G), (G, G), (G, -G), (-G, G), (-G, -G)):
        n = (c[0] + dx, c[1] + dy)
        step = 1.4142 if dx and dy else 1
        # a turn costs a little, so the route prefers long straight legs
        turn = 0.3 if c in prev and (c[0] - prev[c][0], c[1] - prev[c][1]) != (dx, dy) else 0
        nd = d + step + turn
        if (free(*n) or n == g) and nd < dist.get(n, 1e18):
            dist[n] = nd; prev[n] = c
            heapq.heappush(h, (nd + math.hypot(n[0] - g[0], n[1] - g[1]) / G, nd, n))
if g not in prev: print("NO ROUTE"); sys.exit(1)
path = [g]
while path[-1] != s: path.append(prev[path[-1]])
path.reverse()
segs = []
for a, b in zip(path, path[1:]):
    d = (b[0] - a[0], b[1] - a[1])
    if segs and segs[-1][0] == d: segs[-1][1] += G
    else: segs.append([d, G])
def keys(d):
    k = []
    if d[0] > 0: k.append("ArrowRight")
    if d[0] < 0: k.append("ArrowLeft")
    if d[1] > 0: k.append("ArrowDown")
    if d[1] < 0: k.append("ArrowUp")
    return "+".join(k)
# a leg of n steps covers n * |step| pixels at 100 px/s
print(" ".join(f"{keys(d)}:{int(n / G * math.hypot(*d) * 10)}" for d, n in segs))
