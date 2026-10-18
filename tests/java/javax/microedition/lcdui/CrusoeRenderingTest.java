package javax.microedition.lcdui;

import static org.junit.Assert.*;
import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import javax.microedition.shell.AppClassLoader;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import crusoe.tests.JavaMeRobolectricRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

/** Pixel-level regressions against actual patched runtime classes; synthetic art only. */
@RunWith(JavaMeRobolectricRunner.class)
@Config(manifest = Config.NONE, sdk = 35, application = Application.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class CrusoeRenderingTest {
    private File resources;

    @Before public void resources() throws Exception {
        resources = Files.createTempDirectory("crusoe-synthetic-").toFile();
        new File(resources, "data").mkdirs();
        Files.write(new File(resources, "BIGislandFULL.byt").toPath(), new byte[]{1});
        Files.write(new File(resources, "data/BS3.png").toPath(), new byte[]{1});
        Field old = AppClassLoader.class.getDeclaredField("oldResDir");
        old.setAccessible(true);
        old.set(null, resources);
    }

    @Test public void hdPixelsSurviveRepeatedGraphicsReset() {
        Image image = Image.createCrusoeRenderTarget(32, 24, 0);
        assertEquals(32, image.getWidth());
        assertEquals(128, image.getBitmap().getWidth());
        Graphics graphics = image.getSingleGraphics();
        for (int i = 0; i < 10; i++) {
            graphics.reset(0, 0, 32, 24);
            graphics.setColor(0xff0000);
            graphics.fillRect(4, 5, 2, 3);
        }
        assertEquals(Color.RED, image.getBitmap().getPixel(16, 20));
        assertEquals(Color.RED, image.getBitmap().getPixel(23, 31));
        assertEquals(0, image.getBitmap().getPixel(24, 32));
        assertEquals(0, image.getBitmap().getPixel(15, 20));
    }

    @Test public void partialFlushUsesScaledSourceCoordinates() {
        Image source = Image.createCrusoeRenderTarget(32, 24, 0);
        Graphics graphics = source.getSingleGraphics();
        graphics.setColor(0xff0000);
        graphics.fillRect(0, 0, 4, 4);
        graphics.setColor(0x0000ff);
        graphics.fillRect(8, 4, 6, 5);
        Image target = Image.createCrusoeRenderTarget(32, 24, 0);
        target.getSingleGraphics().flush(source, 8, 4, 6, 5);
        assertEquals(Color.BLUE, target.getBitmap().getPixel(32, 16));
        assertEquals(Color.BLUE, target.getBitmap().getPixel(55, 35));
        assertEquals(0, target.getBitmap().getPixel(31, 16));
        assertEquals(0, target.getBitmap().getPixel(0, 0));
    }

    @Test public void copyingResizedBufferDoesNotSqueezeUnusedPixels() {
        Image source = Image.createCrusoeRenderTarget(32, 24, Color.RED);
        source.setSize(16, 12);
        Graphics graphics = source.getSingleGraphics();
        graphics.setColor(0x00ff00);
        graphics.fillRect(0, 0, 16, 12);
        Image target = Image.createCrusoeRenderTarget(16, 12, 0);
        source.copyTo(target);
        assertEquals(Color.GREEN, target.getBitmap().getPixel(63, 47));
        assertEquals(64, source.getCrusoeNativeBounds().width());
        assertEquals(48, source.getCrusoeNativeBounds().height());
    }

    @Test public void drawingHdImageKeepsItsLogicalSizeAndAnchor() {
        Image source = Image.createCrusoeRenderTarget(8, 6, Color.RED);
        Image target = Image.createCrusoeRenderTarget(32, 24, 0);
        target.getSingleGraphics().drawImage(source, 12, 10, Graphics.RIGHT | Graphics.BOTTOM);
        assertEquals(Color.RED, target.getBitmap().getPixel(16, 16));
        assertEquals(Color.RED, target.getBitmap().getPixel(47, 39));
        assertEquals(0, target.getBitmap().getPixel(48, 40));
    }

    @Test public void optionalSquareArtKeepsAspectAndFootpoint() throws Exception {
        new File(resources, "crusoe_hd").mkdirs();
        Bitmap art = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
        art.eraseColor(Color.BLUE);
        try (FileOutputStream out = new FileOutputStream(new File(resources, "crusoe_hd/BS03_000.png"))) {
            art.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
        Image atlas = Image.createImage(348, 36, Color.RED);
        Field path = Image.class.getDeclaredField("crusoeResourceName");
        path.setAccessible(true);
        path.set(atlas, "/data/BS3.png");
        Image target = Image.createCrusoeRenderTarget(80, 80, 0);
        target.getSingleGraphics().drawRegion(atlas, 244, 0, 19, 34, 0, 40, 50,
                Graphics.HCENTER | Graphics.BOTTOM);
        assertEquals(Color.BLUE, target.getBitmap().getPixel(92, 64));
        assertEquals(Color.BLUE, target.getBitmap().getPixel(227, 199));
        assertEquals(0, target.getBitmap().getPixel(228, 199));
        assertEquals(0, target.getBitmap().getPixel(160, 200));
    }

    @Test public void otherGamesAndOversizedViewportsUseStandardResolution() throws Exception {
        assertEquals(1, Image.createCrusoeRenderTarget(361, 640, 0).getCrusoeScale());
        Files.delete(new File(resources, "BIGislandFULL.byt").toPath());
        Image image = Image.createCrusoeRenderTarget(32, 24, 0);
        assertEquals(1, image.getCrusoeScale());
        assertEquals(32, image.getBitmap().getWidth());
    }
}
