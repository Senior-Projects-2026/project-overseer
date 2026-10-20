# Tile system and chunk rendering

Implementation notes for issue #28. These APIs provide terrain data for rendering,
collision detection and pathfinding.

## Classes and modules

| Class | Module | Responsibility |
| --- | --- | --- |
| `TileType` | `shared` | Terrain type and whether it permits walking |
| `Chunk` | `shared` | A 16 × 16 grid of terrain cells |
| `TileRenderer` | `core` | Draws visible terrain before the player |
| `VisibleTileBounds` | `core` | Calculates visible local cell ranges; internal to the renderer |
| `DemoWorld` | `core` | Creates the fixed client-side test map |

The data classes are in `com.projectoverseer.shared.world` and have no graphics
dependencies. Both client and server can use them. Rendering classes are in
`com.projectoverseer.world` and use LibGDX.

## Coordinates and size

- Each chunk contains **16 × 16 tiles**, or 256 cells.
- Local tile coordinates are integers from **0 to 15** on each axis.
- X increases to the right; Y increases upwards.
- A chunk's coordinates identify its position in the chunk grid.
- Its origin in global tile coordinates is `(chunkX * 16, chunkY * 16)`.
- The renderer draws each tile at **32 × 32 world units**. A chunk spans 512 × 512
  world units. Screen pixel size varies with the viewport and camera zoom.

For example, chunk `(1, 0)` contains global tile X coordinates `16..31`.
Chunk `(-1, 0)` contains global tile X coordinates `-16..-1`.

Convert global tile coordinates to a chunk and local cell with:

```java
int tileX = -1;
int tileY = 20;

int chunkX = Chunk.chunkCoordinate(tileX); // -1
int chunkY = Chunk.chunkCoordinate(tileY); // 1
int localX = Chunk.localCoordinate(tileX); // 15
int localY = Chunk.localCoordinate(tileY); // 4
```

These helpers use floor division/modulo, so they also work for negative positions.
To convert a world position to a global tile coordinate in client code:

```java
int tileX = (int) Math.floor(worldX / TileRenderer.TILE_SIZE);
int tileY = (int) Math.floor(worldY / TileRenderer.TILE_SIZE);
```

Use floor rather than casting the quotient directly: a position just below zero
belongs to tile `-1`. `TileRenderer` is a client class; server-side consumers should
agree on the same world-unit scale without depending on the renderer.

## Creating and modifying terrain

```java
Chunk chunk = new Chunk(0, 0, TileType.GRASS);
chunk.setTile(5, 3, TileType.ROCK);
TileType terrain = chunk.getTile(5, 3);
```

The constructor initializes all cells with the supplied type. `getTile`, `setTile`
and `isWalkable` take **local** coordinates. Out-of-range coordinates throw
`IndexOutOfBoundsException`; null tile types are rejected. Chunk coordinates whose
global origin overflows an integer are also rejected.

## Obstacles and walkability

An obstacle is currently a cell whose terrain type has `isWalkable() == false`.
There is no separate obstacle entity or collision shape in this implementation.

| Type | Walkable | Preview appearance |
| --- | --- | --- |
| `GRASS` | Yes | Green |
| `DIRT` | Yes | Brown |
| `WATER` | No | Blue |
| `ROCK` | No | Grey |

```java
chunk.setTile(5, 3, TileType.ROCK);
boolean canWalk = chunk.isWalkable(5, 3); // false

chunk.setTile(5, 3, TileType.DIRT);
boolean nowWalkable = chunk.isWalkable(5, 3); // true
```

For collision or A*, first locate the chunk containing the requested global tile,
then call `isWalkable` with its local coordinates. The caller manages the collection
of chunks and decides how to handle missing chunks; this API does not define them
as either walkable or blocked.

This flag describes terrain only. Entity collision, player size, diagonal movement
rules, dynamic obstacles and the pathfinding algorithm belong to their respective
tasks. The current player movement does not yet consult this flag, so the player
can walk across water and rock in the preview.

## Rendering and camera culling

`Main` updates the existing camera and draws terrain before the player:

```java
batch.setProjectionMatrix(camera.combined);
batch.begin();
tileRenderer.render(batch, chunks, camera);
player.render(batch);
batch.end();
```

The renderer uses the camera position, viewport dimensions and zoom to calculate
the visible rectangle. It skips chunks outside that rectangle and loops only over
the intersecting local cell ranges in each remaining chunk. Partially visible tiles
are included; cells merely touching the outside edge are excluded.

This calculation supports the game's **axis-aligned orthographic camera**. A rotated
or perspective camera would require adapting the visibility calculation.

Culling only avoids drawing off-screen cells. It does not remove terrain from
memory, load new chunks, restrict player movement or change how the camera follows
the player. The renderer still checks each supplied chunk for visibility.

The placeholder tile atlas is created once by `TileRenderer` and released by
`dispose()`. The caller owns the `SpriteBatch` and controls `begin()`/`end()`.

## Preview map and current limits

`DemoWorld` creates a fixed 3 × 3 arrangement of chunks, with chunk coordinates
`-1..1` on each axis. The complete map is **48 × 48 tiles**, spanning global tile
coordinates `-16..31`, or world positions `[-512, 1024)` on each axis.

It contains grass, crossing dirt paths, a water patch and a rock patch. The paths
cross chunk boundaries to make alignment easy to inspect. Tile colors and borders
are placeholders rather than final artwork.

The map is created locally on the client and is not synchronized with the server.
Beyond its edges, the dark background is visible and the player can continue moving.
Procedural terrain generation (#29), dynamic chunk loading (#30), collision handling
(#31) and A* (#39) are separate tasks.