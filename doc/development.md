# Development notes

Things that are easy to trip over when working on this project, and how a change is checked.

## Build gotchas

- The template's gotchas apply (stale barrels: `./mill clean ninja`; `-Werror`), see its
  [Commands](https://github.com/rinn7e/indigo-game-starter-template#commands). A stale barrel
  can also show up as "Not found" for something that exists.
- **Nested asset lists.** mill-indigo's `listAssets` gives each subfolder of `ninja/assets/` its
  own nested object with its own asset set, so `GameAssets.assets` adds
  `Assets.assets.generated.assetSetRelativeTo(baseUrl)` explicitly. A missing asset shows up as a
  black screen and "Failed to find texture ref" in the console.
- **The font sheet is committed.** `embedFont` writes `ninja/assets/generated/NormalFont.png`, but
  on a clean clone the asset list is generated before it, so the PNG is tracked (and the folder
  keeps a `.gitkeep`, since `embedFont` needs it to exist). The font's own space is 1px wide;
  `GameAssets.fontInfo` widens it.
- **Names Indigo already uses.** Our camera type is `CameraGrid` (Indigo has `Camera`); silence
  is `SceneAudio.Mute`.
- **No `indigo-physics`.** It only has box and circle colliders, and 83 of the tileset's 227
  collision polygons aren't rectangles, so collision is our own polygon move-and-slide
  (`common/util/Collision.scala`).

## Checking a change

```bash
./mill __.reformat
./mill ninja.test
./mill ninja.indigoBuild
python3 -m http.server 8787 --directory out/ninja/indigoBuild.dest
```

Restart the server after each build: the build replaces the output folder.

Then play it in a real browser window (with a GPU, so the FPS counter means something) and check
what the change touches:

- the title, and entering the world (the music starts);
- walking, sliding along walls and trees, the followers and the patrolling guard;
- breaking grass, pots and crates;
- the camera sliding to the next screen;
- the swamp (west of the start): rain with splashes, fog, leaves, swamp music, colour grading;
- a house door (north of the start) and the fade;
- combat: the lance, the monsters north-east of the start, getting hurt, dying and reviving;
- the letterbox at several window sizes, and a live resize;
- no errors in the console, and about 60 FPS.

## Faithful quirks

Some things look like bugs but match the originals:

- The village map has no walls along its edges: walking off the top leads into black, as in the
  Godot 4 demo.
- The Godot 3 monsters always play their "down" walk, whichever way they move.
