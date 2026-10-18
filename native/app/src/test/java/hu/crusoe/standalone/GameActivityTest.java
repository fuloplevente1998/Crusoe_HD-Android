package hu.crusoe.standalone;

import static org.junit.Assert.*;
import android.app.Application;
import android.os.Looper;
import android.graphics.*;
import android.view.*;
import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,application=Application.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class GameActivityTest {
    private GameActivity create(){return Robolectric.buildActivity(GameActivity.class).setup().get();}
    private Bitmap render(GameActivity a,int width,int height){
        GameView v=a.gameView;
        v.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));
        v.layout(0,0,width,height);
        Bitmap bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);v.draw(new Canvas(bitmap));return bitmap;
    }
    private void screenshot(String name,Bitmap bitmap)throws Exception {
        File dir=new File(System.getProperty("crusoe.artifacts", "build/verification"));dir.mkdirs();
        try(FileOutputStream out=new FileOutputStream(new File(dir,name+".png"))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));}
    }
    @Test public void launcherOpensOwnTitleAndTouchStartsGame()throws Exception {
        GameActivity a=create();
        assertEquals(GameView.TITLE,a.gameView.screen);
        Bitmap title=render(a,720,1280);screenshot("native-title",title);
        assertTrue(title.getPixel(360,1080)!=0);
        long now=android.os.SystemClock.uptimeMillis();
        a.gameView.onTouchEvent(MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,360,1070,0));
        a.gameView.onTouchEvent(MotionEvent.obtain(now,now+20,MotionEvent.ACTION_UP,360,1070,0));
        assertEquals(GameView.WORLD,a.gameView.screen);
        assertEquals(45,a.data.recipes.length);
        screenshot("native-game",render(a,720,1280));
    }
    @Test public void portraitAtTwoPhysicalResolutionsRendersAndMapShowsOriginalIsland()throws Exception {
        GameActivity a=create();a.start(false);
        Bitmap full=render(a,1080,2400);screenshot("native-game-1080x2400",full);
        assertEquals(0xff102e2e,full.getPixel(500,2395));
        a.gameView.screen=GameView.MAP;screenshot("native-map",render(a,720,1280));
        assertEquals(32400,a.data.layers[0].length);
    }
    @Test public void joystickTouchMovesPlayerWithoutDesktopOrJavaMeRuntime()throws Exception {
        GameActivity a=create();a.start(false);render(a,720,1280);
        GameEngine e=a.engine;
        // Find two adjacent free tiles and use a real pointer gesture.
        outer:for(int y=1;y<179;y++)for(int x=1;x<177;x++)if(e.passable(x,y)&&e.passable(x+1,y)){e.x=x+.5f;e.y=y+.5f;break outer;}
        float before=e.x;
        long now=android.os.SystemClock.uptimeMillis();
        a.gameView.onTouchEvent(MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,164,1119,0));
        for(int n=0;n<8;n++){a.gameView.lastFrame=System.nanoTime()-50_000_000;render(a,720,1280);}
        a.gameView.onTouchEvent(MotionEvent.obtain(now,now+500,MotionEvent.ACTION_UP,164,1119,0));
        assertTrue(e.x>before+.2);
        assertFalse(a.getPackageManager().getPackageInfo(a.getPackageName(),0).requestedPermissions!=null);
    }
    @Test public void mapTapRouteSurvivesReturningToGameAndStartsWalking() {
        GameActivity a=create();a.start(false);GameEngine e=a.engine;
        outer:for(int yy=1;yy<179;yy++)for(int xx=1;xx<177;xx++)if(e.passable(xx,yy)&&e.passable(xx+1,yy)){e.x=xx+.5f;e.y=yy+.5f;break outer;}
        float before=e.x;int tx=(int)e.x+1,ty=(int)e.y;
        a.gameView.screen=GameView.MAP;render(a,720,1280);
        float mx=24+(tx+.5f)/180*672,my=130+(ty+.5f)/180*672;
        long now=android.os.SystemClock.uptimeMillis();
        a.gameView.onTouchEvent(MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,mx,my,0));
        a.gameView.onTouchEvent(MotionEvent.obtain(now,now+20,MotionEvent.ACTION_UP,mx,my,0));
        assertEquals(GameView.WORLD,a.gameView.screen);assertFalse(e.route.isEmpty());
        render(a,720,1280); // First frame has delta=0 after leaving the map.
        assertFalse(e.route.isEmpty());
        for(int n=0;n<8;n++){a.gameView.lastFrame=System.nanoTime()-50_000_000;render(a,720,1280);}
        assertTrue(e.x>before+.2);
    }
    @Test public void nativeSaveLoadsThroughContinueAction()throws Exception {
        GameActivity a=create();a.start(false);a.engine.inventory[76]=1;a.engine.food=63;a.save();
        assertTrue(a.hasSave());a.engine.inventory[76]=0;a.start(true);
        assertEquals(1,a.engine.inventory[76]);assertEquals(63,a.engine.food,.001);
    }
    @Test public void actualInventoryCraftingAndJournalOpenWithoutImportUi() {
        GameActivity a=create();a.start(false);
        a.inventory();assertTrue(a.paused);
        org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog().dismiss();Shadows.shadowOf(Looper.getMainLooper()).idle();assertFalse(a.paused);
        a.crafting();assertTrue(a.paused);
        org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog().dismiss();Shadows.shadowOf(Looper.getMainLooper()).idle();
        a.journal();assertTrue(a.paused);
        org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog().dismiss();Shadows.shadowOf(Looper.getMainLooper()).idle();assertFalse(a.paused);
    }
}
