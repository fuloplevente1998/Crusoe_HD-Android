package hu.crusoe.standalone;

/**
 * Provisional P0 recovery for naturally placed stones (item ID 3).
 *
 * Original Java ME regrowth cadence has not been established yet. Do not call
 * this the original respawn algorithm. We only reuse confirmed starting
 * locations and never overwrite player-placed structures or unique objects.
 */
public final class ResourceRespawn {
    public static final int STONE_ITEM_ID = 3;
    static final int STONE_MAP_VALUE = STONE_ITEM_ID + 1;
    private static final int MAP_WIDTH = 180;
    private static final int MAP_TILES = MAP_WIDTH * MAP_WIDTH;

    private ResourceRespawn() {}

    public static boolean containsStone(byte[] currentObjects) {
        checkMap(currentObjects);
        for (byte b : currentObjects) {
            if ((b & 255) == STONE_MAP_VALUE) return true;
        }
        return false;
    }

    /**
     * Restore at most one natural stone on an empty original stone location.
     *
     * The nearest currently empty original deposit is selected deterministically.
     * A tile with a crafted item, story object or any other object is preserved.
     * Never touch inventory or overwrite the tile under the player.
     *
     * @return restored tile index, or -1 if none was safely available
     */
    public static int restoreNearestStone(byte[] originalObjects, byte[] currentObjects,
                                           float playerX, float playerY) {
        checkMap(originalObjects);
        checkMap(currentObjects);
        int playerTileX = (int) playerX;
        int playerTileY = (int) playerY;
        int best = -1;
        double distance = Double.POSITIVE_INFINITY;
        for (int i = 0; i < MAP_TILES; i++) {
            if ((originalObjects[i] & 255) != STONE_MAP_VALUE || currentObjects[i] != 0)
                continue;
            int sx = i % MAP_WIDTH, sy = i / MAP_WIDTH;
            if (sx == playerTileX && sy == playerTileY) continue;
            double dx = sx + .5 - playerX;
            double dy = sy + .5 - playerY;
            double d = dx * dx + dy * dy;
            if (d < distance) {
                best = i;
                distance = d;
            }
        }
        if (best >= 0) currentObjects[best] = (byte) STONE_MAP_VALUE;
        return best;
    }

    private static void checkMap(byte[] map) {
        if (map == null || map.length != MAP_TILES) {
            throw new IllegalArgumentException("Expected original 180x180 object map");
        }
    }
}
