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
    private void touch(GameActivity a,int action,float x,float y){
        long now=android.os.SystemClock.uptimeMillis();MotionEvent event=MotionEvent.obtain(now,now,action,x,y,0);
        a.gameView.onTouchEvent(event);event.recycle();
    }
    private void tap(GameActivity a,float x,float y){touch(a,MotionEvent.ACTION_DOWN,x,y);touch(a,MotionEvent.ACTION_UP,x,y);}
    private void tick(GameActivity a,int count){for(int n=0;n<count;n++){a.gameView.lastFrame=System.nanoTime()-50_000_000;render(a,720,1280);}}
    private void multi(GameActivity a,int action,int[] ids,float[] xs,float[] ys){
        MotionEvent.PointerProperties[] properties=new MotionEvent.PointerProperties[ids.length];MotionEvent.PointerCoords[] coords=new MotionEvent.PointerCoords[ids.length];
        for(int i=0;i<ids.length;i++){properties[i]=new MotionEvent.PointerProperties();properties[i].id=ids[i];properties[i].toolType=MotionEvent.TOOL_TYPE_FINGER;coords[i]=new MotionEvent.PointerCoords();coords[i].x=xs[i];coords[i].y=ys[i];coords[i].pressure=1;coords[i].size=1;}
        long now=android.os.SystemClock.uptimeMillis();MotionEvent event=MotionEvent.obtain(now,now,action,ids.length,properties,coords,0,0,1,1,0,0,InputDevice.SOURCE_TOUCHSCREEN,0);a.gameView.onTouchEvent(event);event.recycle();
    }
    private android.widget.Button button(android.view.View view,String text){
        if(view instanceof android.widget.Button b&&text.equals(b.getText().toString()))return b;
        if(view instanceof android.view.ViewGroup group)for(int i=0;i<group.getChildCount();i++){android.widget.Button found=button(group.getChildAt(i),text);if(found!=null)return found;}
        return null;
    }
    private void shore(GameEngine e,int terrain){
        for(int yy=1;yy<179;yy++)for(int xx=1;xx<179;xx++)if(e.passable(xx,yy)){
            e.x=xx+.5f;e.y=yy+.5f;if(e.nearWater(terrain))return;
        }throw new AssertionError("Missing original shore");
    }
    private Bitmap dialogImage(android.app.AlertDialog dialog){
        View view=dialog.getWindow().getDecorView();
        view.measure(View.MeasureSpec.makeMeasureSpec(720,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1150,View.MeasureSpec.AT_MOST));
        int height=view.getMeasuredHeight();view.layout(0,0,720,height);
        Bitmap image=Bitmap.createBitmap(720,height,Bitmap.Config.ARGB_8888);view.draw(new Canvas(image));return image;
    }
    private boolean containsWebView(View view){
        if(view instanceof android.webkit.WebView)return true;
        if(view instanceof android.view.ViewGroup group)for(int i=0;i<group.getChildCount();i++)if(containsWebView(group.getChildAt(i)))return true;
        return false;
    }
    @Test public void originalKnifeIsBesideOriginalCrateAndCanBeFoundFromJournal()throws Exception {
        GameActivity a=create();a.start(false);GameEngine e=a.engine;
        assertEquals(7.5f,e.x,0);assertEquals(147.5f,e.y,0);
        int knife=e.findObject(76);assertEquals(165*180+11,knife);
        assertEquals(68,a.data.layers[GameData.DECOR][knife-1]&255);
        a.journal();android.app.AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
        android.widget.Button find=button(dialog.getWindow().getDecorView(),"Út a késhez");assertNotNull(find);find.performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertFalse(a.paused);assertFalse(e.route.isEmpty());assertEquals(knife,e.selected);
        int limit=20000;while(!e.route.isEmpty()&&limit-->0)e.update(.05f,0,0);assertTrue(limit>0);assertTrue(e.near(knife,1.8f));
        a.gameView.centerCamera();screenshot("native-knife-crate",render(a,720,1280));
        e.interact();assertEquals(1,e.inventory[76]);assertEquals(-1,e.objectAt(knife));
        assertEquals(68,a.data.layers[GameData.DECOR][knife-1]&255); // Picking up the knife leaves its neighboring crate.
    }
    @Test public void shellsFoodAndKnifeHaveDifferentActualRenderedIcons()throws Exception {
        GameActivity a=create();int[] ids={1,64,65,66,67,68,70,71,72,73,74,75,76,77,0,24,25,26,27,28,30,31,32,33};
        java.util.ArrayList<Bitmap> icons=new java.util.ArrayList<>();
        Bitmap gallery=Bitmap.createBitmap(720,((ids.length+3)/4)*190,Bitmap.Config.ARGB_8888);Canvas sheet=new Canvas(gallery);sheet.drawColor(0xff102e2e);
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(0xfff2dfb9);p.setTextSize(17);
        for(int n=0;n<ids.length;n++){
            Bitmap icon=Bitmap.createBitmap(128,128,Bitmap.Config.ARGB_8888);a.gameView.itemArt.draw(new Canvas(icon),ids[n],64,128,128,128);
            int opaque=0;for(int y=0;y<128;y++)for(int x=0;x<128;x++)if(Color.alpha(icon.getPixel(x,y))>128)opaque++;
            assertTrue("Empty icon for "+ids[n],opaque>250);
            for(Bitmap previous:icons)assertFalse("Two different items share the same rendered icon: "+ids[n],icon.sameAs(previous));icons.add(icon);
            sheet.drawBitmap(icon,n%4*180+26,n/4*190+10,null);
            String name=a.data.items[ids[n]].name;sheet.drawText(name,n%4*180+9,n/4*190+166,p);
        }
        screenshot("native-item-icons",gallery);
    }
    @Test public void nativeBackpackAndCraftingShowIconsAndCraftFromRecipeDetails()throws Exception {
        GameActivity a=create();a.start(false);a.engine.inventory[76]=1;a.engine.inventory[3]=1;a.engine.inventory[8]=1;a.engine.inventory[6]=1;a.engine.inventory[67]=2;
        a.inventory();android.app.AlertDialog bag=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
        View content=bag.getWindow().getDecorView();assertFalse(containsWebView(content));assertNotNull(content.findViewWithTag("icon:67"));
        screenshot("native-backpack",dialogImage(bag));bag.dismiss();Shadows.shadowOf(Looper.getMainLooper()).idle();
        a.crafting();android.app.AlertDialog recipes=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
        content=recipes.getWindow().getDecorView();assertFalse(containsWebView(content));assertNotNull(content.findViewWithTag("icon:5"));
        screenshot("native-crafting",dialogImage(recipes));content.findViewWithTag("recipe:5").performClick();
        android.app.AlertDialog detail=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();content=detail.getWindow().getDecorView();
        assertNotNull(content.findViewWithTag("icon:3"));assertNotNull(content.findViewWithTag("icon:8"));assertNotNull(content.findViewWithTag("icon:6"));
        android.widget.Button create=button(content,"Elkészítés");assertNotNull(create);assertTrue(create.isEnabled());screenshot("native-recipe",dialogImage(detail));create.performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();assertEquals(1,a.engine.inventory[5]);assertEquals(1,a.engine.inventory[76]);assertEquals(0,a.engine.inventory[3]);
        org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog().dismiss();Shadows.shadowOf(Looper.getMainLooper()).idle();assertFalse(a.paused);
    }
    @Test public void coconutActionUsesTwoFingerStickAndFireWithoutReleaseShots()throws Exception {
        GameActivity a=create();a.start(false);a.engine.inventory[3]=2;render(a,720,1280);
        tap(a,580,1110);android.app.AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
        android.widget.Button use=button(dialog.getWindow().getDecorView(),"Kókusz leverése · kő");assertNotNull(use);assertTrue(use.isEnabled());use.performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertEquals(GameView.HARVEST,a.gameView.screen);assertEquals(1,a.engine.inventory[3]);render(a,720,1280);HarvestGame g=a.engine.harvest;
        assertEquals(HarvestGame.COCONUT,g.tool);float angle=g.angle,power=g.power;
        multi(a,MotionEvent.ACTION_DOWN,new int[]{7},new float[]{87},new float[]{1086});tick(a,5);
        assertTrue(g.angle<angle);assertTrue(g.power>power);assertEquals(0,g.attempts);
        screenshot("native-coconut-joystick",render(a,720,1280));
        multi(a,MotionEvent.ACTION_POINTER_DOWN|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),new int[]{7,12},new float[]{87,500},new float[]{1086,1110});
        multi(a,MotionEvent.ACTION_POINTER_UP|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),new int[]{7,12},new float[]{87,500},new float[]{1086,1110});
        assertTrue(g.flying);assertEquals(1,g.attempts);
        multi(a,MotionEvent.ACTION_UP,new int[]{7},new float[]{87},new float[]{1086});assertEquals(1,g.attempts);
        while(g.flying&&!g.finished)g.update(.1f);
        angle=g.angle;power=g.power;tick(a,3);assertEquals(angle,g.angle,0);assertEquals(power,g.power,0);
        touch(a,MotionEvent.ACTION_DOWN,500,1110);touch(a,MotionEvent.ACTION_CANCEL,500,1110);touch(a,MotionEvent.ACTION_UP,500,1110);assertEquals(1,g.attempts);
        touch(a,MotionEvent.ACTION_DOWN,186,1127);a.onPause();a.onResume();tick(a,3);assertEquals(angle,g.angle,0);
        a.onBackPressed();assertEquals(GameView.WORLD,a.gameView.screen);assertEquals(1,a.engine.inventory[3]);
    }
    @Test public void rodJoystickMovesHooksAndReelsWithSeparateButton()throws Exception {
        GameActivity a=create();a.start(false);shore(a.engine,5);a.engine.inventory[14]=1;a.engine.inventory[90]=1;
        assertEquals("",a.engine.startHarvest(14,-1,42));a.gameView.screen=GameView.HARVEST;render(a,720,1280);HarvestGame g=a.engine.harvest;
        float x=g.hookX,y=g.hookY;touch(a,MotionEvent.ACTION_DOWN,169,1168);tick(a,8);touch(a,MotionEvent.ACTION_UP,169,1168);
        assertTrue(g.hookX>x+40);assertTrue(g.hookY>y+40);assertFalse(g.hooked);assertFalse(g.finished);
        g.fishX[0]=g.hookX;g.fishY[0]=g.hookY;g.fishSpeed[0]=0;tap(a,500,1110);assertTrue(g.hooked);
        screenshot("native-fishing-joystick",render(a,720,1280));int reward=g.reward;
        touch(a,MotionEvent.ACTION_DOWN,128,1069);tick(a,20);touch(a,MotionEvent.ACTION_UP,128,1069);
        assertTrue(g.won);assertEquals(1,a.engine.inventory[reward]);assertEquals(0,a.engine.inventory[90]);
        tap(a,500,1110);assertEquals(GameView.WORLD,a.gameView.screen);a.start(true);assertEquals(1,a.engine.inventory[reward]);
    }
    @Test public void actionSheetLaunchesRodAndTouchCatchesReelsSavesExactlyOneFish()throws Exception {
        GameActivity a=create();a.start(false);shore(a.engine,5);
        a.engine.inventory[14]=1;a.engine.inventory[90]=2;render(a,720,1280);
        touch(a,MotionEvent.ACTION_DOWN,580,1110);touch(a,MotionEvent.ACTION_UP,580,1110);
        android.app.AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
        android.widget.Button use=button(dialog.getWindow().getDecorView(),a.data.items[14].name);
        assertNotNull(use);assertTrue(use.isEnabled());use.performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertFalse(a.paused);assertEquals(GameView.HARVEST,a.gameView.screen);assertEquals(1,a.engine.inventory[90]);
        render(a,720,1280);HarvestGame g=a.engine.harvest;int reward=g.reward;
        screenshot("native-fishing",render(a,720,1280));
        touch(a,MotionEvent.ACTION_DOWN,g.fishX[0],205+1.25f*g.fishY[0]);
        touch(a,MotionEvent.ACTION_UP,g.fishX[0],205+1.25f*g.fishY[0]);assertTrue(g.hooked);
        touch(a,MotionEvent.ACTION_DOWN,g.hookX,205+1.25f*g.hookY);
        touch(a,MotionEvent.ACTION_MOVE,360,245);touch(a,MotionEvent.ACTION_UP,360,245);
        render(a,720,1280);assertTrue(g.won);assertEquals(1,a.engine.inventory[reward]);
        render(a,720,1280);assertEquals(1,a.engine.inventory[reward]);assertNull(a.engine.harvest);
        tap(a,360,1110);assertEquals(GameView.WORLD,a.gameView.screen);
        a.start(true);assertEquals(1,a.engine.inventory[reward]);assertEquals(1,a.engine.inventory[90]);
    }
    @Test public void netAndHuntingRenderAndBackCancelsWithoutRefundOrWorldMovement()throws Exception {
        GameActivity a=create();a.start(false);shore(a.engine,4);
        a.engine.inventory[83]=1;a.engine.inventory[26]=1;
        assertEquals("",a.engine.startHarvest(83,-1,42));a.gameView.screen=GameView.HARVEST;
        screenshot("native-net",render(a,720,1280));float x=a.engine.x,y=a.engine.y;
        tap(a,360,1110);assertTrue(a.engine.harvest.dropping);
        a.paused=true;float time=a.engine.harvest.elapsed;
        a.gameView.lastFrame=System.nanoTime()-90_000_000;render(a,720,1280);
        assertEquals(time,a.engine.harvest.elapsed,0);a.paused=false;
        a.onBackPressed();assertEquals(GameView.WORLD,a.gameView.screen);assertNull(a.engine.harvest);assertEquals(0,a.engine.inventory[26]);
        assertEquals(x,a.engine.x,0);assertEquals(y,a.engine.y,0);
        int target=a.engine.findObject(4);a.engine.x=target%180+.5f;a.engine.y=target/180+1.5f;
        a.engine.inventory[81]=1;a.engine.inventory[45]=1;
        assertEquals("",a.engine.startHarvest(81,target,9));a.gameView.screen=GameView.HARVEST;
        screenshot("native-hunting",render(a,720,1280));
        float beforeAngle=a.engine.harvest.angle;touch(a,MotionEvent.ACTION_DOWN,186,1127);tick(a,6);touch(a,MotionEvent.ACTION_UP,186,1127);assertTrue(a.engine.harvest.angle>beforeAngle);
        tap(a,500,1110);assertTrue(a.engine.harvest.flying);assertEquals(1,a.engine.harvest.attempts);
        a.onBackPressed();assertNull(a.engine.harvest);assertEquals(4,a.engine.objectAt(target));
    }
}
