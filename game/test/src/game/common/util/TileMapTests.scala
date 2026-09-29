package game.common.util

import indigo.*
import game.common.constant.Village
import game.common.util.TileMap.*

class TileMapTests extends munit.FunSuite {

  test("every layer of the Godot map is decoded") {
    assertEquals(Village.tiles.wall.length, 379)
    assertEquals(Village.tiles.wall2.length, 262)
    assertEquals(Village.tiles.floorDetail.length, 10)
    assertEquals(Village.tiles.floor.length, 2889)
  }

  test("negative cells decode to the right row (x low 16 bits, y high 16 bits)") {
    assertEquals(decodeCells("-2,-3,0,0,7").head.cell, Point(-2, -3))
  }

  test("tiles keep their exact collision polygon") {
    val diagonal = Village.tiles.kinds((0, 2, 6)).solid.getOrElse(Batch.empty)
    assertEquals(diagonal.length, 4)
    assertEquals(diagonal.head, Vector2(-8, -3.955))
  }

  test("props come from the tileset's scene tiles: 46 grass, a crate and a pot") {
    assertEquals(Village.props.length, 48)
  }

  test("a cell's centre includes the TileMap's origin") {
    assertEquals(centreOf(Point(0, 0), Point(8, -5)), Vector2(16, 3))
  }
}
