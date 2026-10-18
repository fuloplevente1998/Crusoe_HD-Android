package javax.microedition.lcdui;

import android.graphics.Bitmap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Experimental HD sprite override for a separately imported MIDlet.
 * Does not bundle or redistribute any original game resources.
 * Other images always fall back to their original Java ME drawing path.
 */
final class CrusoeHD {
    static final int SCALE = 4; // first-pass experimental 4x framebuffer
    private static final Map<String, Bitmap> CACHE = new HashMap<>();
    private static final Set<String> MISSING = new HashSet<>();
    private CrusoeHD() {}

    // Original source atlas size and exact clip rectangle identify the character.
    static Bitmap get(Image source, int sx, int sy, int sw, int sh) {
        if (source == null || source.getWidth() != 348 || source.getHeight() != 36) {
            return null;
        }
        String key = keyFor(sx, sy, sw, sh);
        if (key == null) return null;
        synchronized (CACHE) {
            Bitmap hit = CACHE.get(key);
            if (hit != null) return hit;
            if (MISSING.contains(key)) return null;
            try {
                Bitmap image = Image.createImage("/crusoe_hd/" + key + ".png").getBitmap();
                if (image != null) {
                    CACHE.put(key, image);
                    return image;
                }
            } catch (java.io.IOException ignored) {
                // Missing optional modern asset: draw original sprite.
            }
            MISSING.add(key);
            return null;
        }
    }

    private static String keyFor(int sx, int sy, int sw, int sh) {
        if (sy != 0) return null;
        // [x, width, height] for 20 original animation regions in the 348x36 atlas.
        int[][] rects = {
            {244,19,34}, {129,18,35}, {0,19,36},   {94,17,35},
            {19,19,36},  {226,18,34}, {76,18,35},  {38,19,36},
            {111,18,35}, {57,19,36},  {147,17,35}, {296,18,34},
            {211,15,34}, {331,17,33}, {164,15,35}, {179,17,35},
            {263,18,34}, {281,15,34}, {314,17,33}, {196,15,35}
        };
        for (int i = 0; i < rects.length; i++) {
            int[] r = rects[i];
            if (sx == r[0] && sw == r[1] && sh == r[2]) {
                return String.format(java.util.Locale.ROOT, "BS03_%03d", i);
            }
        }
        return null;
    }
}
