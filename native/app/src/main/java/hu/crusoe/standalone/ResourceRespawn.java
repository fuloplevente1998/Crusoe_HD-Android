package hu.crusoe.standalone;

/**
 * Original 04:00 natural-resource renewal and P0 recovery for missing stones.
 *
 * The original Java ME code reloads the BIGislandObjects map at 04:00 and
 * filters dynamic objects. We additionally keep native-built structures and
 * one-off story items intact rather than resetting the whole mutable map.
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
     * The original game's data.ad.s() reloads BIGislandObjects at 04:00.
     * data.g.a(String,byte[]) applies the original data.y.a(int) filter.
     *
     * The native port additionally preserves one-time pickups and player-built
     * structures: only recurring natural resources are restored, and never
     * over a currently occupied tile or the player's current tile.
     */
    public static int restoreOriginalResources(byte[] originalObjects, byte[] currentObjects,
                                               float playerX, float playerY) {
        checkMap(originalObjects);
        checkMap(currentObjects);

        // Derive natural, repeatable sources from the verified original map:
        // do not recreate unique story equipment or placed one-off objects.
        int[] originalCounts = new int[256];
        for (byte tile : originalObjects) originalCounts[tile & 255]++;
        final int playerIndex = (int) playerY * MAP_WIDTH + (int) playerX;
        int restored = 0;
        for (int i = 0; i < MAP_TILES; i++) {
            int originalCode = originalObjects[i] & 255;
            if (i == playerIndex || currentObjects[i] != 0
                    || !isRecurringResource(originalCode, originalCounts)) continue;
            currentObjects[i] = originalObjects[i];
            restored++;
        }
        return restored;
    }

    /**
     * Original data.y.a(int) excludes animal/dynamic object codes.
     * The original map has 36 recurring natural item codes (1024 placements).
     * Map code 96 is a non-collectible placeholder, not a resource.
     */
    static boolean isRecurringResource(int mapCode, int[] sourceCounts) {
        if (mapCode <= 0 || mapCode >= sourceCounts.length
                || sourceCounts[mapCode] <= 1 || mapCode == 96) return false;
        return !excludedByOriginalLoader(mapCode);
    }

    static boolean excludedByOriginalLoader(int code) {
        return code == 3 || code == 5
                || (code >= 41 && code <= 45)
                || (code >= 47 && code <= 50)
                || (code >= 52 && code <= 54)
                || (code >= 56 && code <= 60)
                || (code >= 62 && code <= 64);
    }

    /** Is a stone currently adjacent to a player-reachable walking cell? */
    public static boolean hasApproachableStone(byte[] currentObjects, boolean[] reachable) {
        checkMap(currentObjects);
        checkReachability(reachable);
        for (int i = 0; i < MAP_TILES; i++) {
            if ((currentObjects[i] & 255) != STONE_MAP_VALUE) continue;
            int x = i % MAP_WIDTH, y = i / MAP_WIDTH;
            if ((x > 0 && reachable[i - 1])
                    || (x + 1 < MAP_WIDTH && reachable[i + 1])
                    || (y > 0 && reachable[i - MAP_WIDTH])
                    || (y + 1 < MAP_WIDTH && reachable[i + MAP_WIDTH]))
                return true;
        }
        return false;
    }

    /**
     * Restore at most one natural stone on an empty original stone location.
     *
     * The nearest reachable, empty original deposit is selected deterministically.
     * A tile with a crafted item, story object or any other object is preserved.
     * Never touch inventory or overwrite the tile under the player.
     *
     * @return restored tile index, or -1 if none was safely available
     */
    public static int restoreNearestStone(byte[] originalObjects, byte[] currentObjects,
                                           float playerX, float playerY, boolean[] reachable) {
        checkMap(originalObjects);
        checkMap(currentObjects);
        checkReachability(reachable);
        int playerTileX = (int) playerX;
        int playerTileY = (int) playerY;
        int best = -1;
        double distance = Double.POSITIVE_INFINITY;
        for (int i = 0; i < MAP_TILES; i++) {
            if (!reachable[i] || (originalObjects[i] & 255) != STONE_MAP_VALUE || currentObjects[i] != 0)
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

    private static void checkReachability(boolean[] reachable) {
        if (reachable == null || reachable.length != MAP_TILES)
            throw new IllegalArgumentException("Expected reachability map");
    }

    private static void checkMap(byte[] map) {
        if (map == null || map.length != MAP_TILES) {
            throw new IllegalArgumentException("Expected original 180x180 object map");
        }
    }
}
