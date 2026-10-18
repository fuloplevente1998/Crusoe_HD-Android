package javax.microedition.lcdui;

import android.graphics.Bitmap;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import javax.microedition.shell.AppClassLoader;
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
    private static WeakReference<AppClassLoader> cacheOwner = new WeakReference<>(null);
    private CrusoeHD() {}

    /** Only allocate high resolution frames if this MIDlet owns both distinctive files. */
    static boolean isEnabled() {
        try (InputStream terrain = AppClassLoader.getResourceAsStream(null, "/BIGislandFULL.byt");
             InputStream atlas = AppClassLoader.getResourceAsStream(null, "/data/BS3.png")) {
            return terrain != null && atlas != null;
        } catch (IOException | RuntimeException ignored) {
            return false;
        }
    }

    // Original source atlas size and exact clip rectangle identify the character.
    static Bitmap get(Image source, int sx, int sy, int sw, int sh) {
        if (source == null || source.getWidth() != 348 || source.getHeight() != 36) {
            return null;
        }
        // Most versions load "/data/BS3.png" by name, but the original game also
        // contains an optional byte-array loading route. In that route the name
        // is unavailable; compare a handful of opaque pixels of the original atlas.
        if (!"/data/BS3.png".equals(source.getCrusoeResourceName())
                && !matchesOriginalAtlas(source.getBitmap())) {
            return null;
        }
        String key = keyFor(sx, sy, sw, sh);
        if (key == null) return null;
        synchronized (CACHE) {
            // A missing override in the previous imported game must not suppress
            // the same frame after importing another resource package.
            AppClassLoader owner = AppClassLoader.getInstance();
            if (cacheOwner.get() != owner) {
                CACHE.clear();
                MISSING.clear();
                cacheOwner = new WeakReference<>(owner);
            }
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

    private static boolean matchesOriginalAtlas(Bitmap bitmap) {
        return bitmap != null
                && bitmap.getWidth() == 348 && bitmap.getHeight() == 36
                && bitmap.getPixel(5, 20) == 0xFFA25F19
                && bitmap.getPixel(25, 26) == 0xFF3A4664
                && bitmap.getPixel(85, 26) == 0xFF1E2534
                && bitmap.getPixel(205, 12) == 0xFFA25F19
                && bitmap.getPixel(325, 4) == 0xFFB80000;
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
