package hu.crusoe.standalone;

import android.app.*;
import android.os.Bundle;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

public final class GameActivity extends Activity {
    public GameData data;
    public GameEngine engine;
    public GameView gameView;
    public boolean paused;
    public String versionLabel="alpha";
    private int openDialogs;
    private boolean foreground=true;
    private File saveFile;
    private File checkpointFile;
    private final ArrayList<AlertDialog> sheets=new ArrayList<>();

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        try{versionLabel=getPackageManager().getPackageInfo(getPackageName(),0).versionName;}
        catch(android.content.pm.PackageManager.NameNotFoundException ignored){}
        try {data=new GameData(getAssets().open("world.pack"));}
        catch(IOException e){throw new IllegalStateException("A játék adatai nem tölthetők be",e);}
        saveFile=new File(getFilesDir(),"island-v1.save");
        checkpointFile=new File(getFilesDir(),"island-checkpoint-v1.save");
        engine=new GameEngine(data);gameView=new GameView(this);setContentView(gameView);
    }
    public boolean hasSave(){return saveFile.isFile();}
    public boolean hasCheckpoint(){return checkpointFile.isFile();}
    public void start(boolean resume){
        GameEngine next=new GameEngine(data);
        if(resume)try{next.load(saveFile);}catch(IOException e){message("A mentés sérült vagy nem kompatibilis. Új játékot indíthatsz.");return;}
        engine=next;gameView.screen=GameView.WORLD;gameView.centerCamera();paused=false;
        gameView.resetInput();
        if(!checkGameOver())save();
        gameView.invalidate();
    }
    public void save(){
        if(gameView.screen==GameView.TITLE)return;
        try{
            // A lethal action never replaces the last living checkpoint.
            if(!engine.isDead())engine.save(checkpointFile);
            engine.save(saveFile);
        }catch(IOException e){engine.say("A mentés nem sikerült. Ellenőrizd a szabad tárhelyet.");}
    }
    public boolean checkGameOver(){
        if(!engine.isDead())return false;
        if(gameView.screen==GameView.DEAD)return true;
        engine.stop();engine.harvest=null;gameView.resetInput();
        for(AlertDialog dialog:new ArrayList<>(sheets))dialog.dismiss();
        gameView.screen=GameView.DEAD;gameView.lastFrame=0;save();gameView.invalidate();return true;
    }
    public void restoreCheckpoint(){
        GameEngine next=new GameEngine(data);
        try{
            next.load(checkpointFile);
            if(next.isDead())throw new IOException("Checkpoint is not alive");
            next.save(saveFile);
        }catch(IOException e){message("Az utolsó élő mentés nem tölthető be. A jelenlegi mentésedet nem módosítottam.");return;}
        engine=next;engine.say("Az utolsó élő mentést visszatöltöttem.");gameView.screen=GameView.WORLD;
        gameView.centerCamera();gameView.resetInput();paused=!foreground||openDialogs>0;gameView.invalidate();
    }
    public void confirmNewGame(){
        new AlertDialog.Builder(this).setTitle("Új kaland")
            .setMessage("Újrakezded? Ez felülírja a mentett kalandot és az utolsó élő mentést is.")
            .setPositiveButton("Új kaland",(d,w)->start(false)).setNegativeButton("Mégsem",null).show();
    }
    public void returnToTitle(){save();gameView.resetInput();gameView.screen=GameView.TITLE;gameView.invalidate();}
    public void message(String text){Toast.makeText(this,text,Toast.LENGTH_LONG).show();}
    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    private LinearLayout list(){LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);list.setPadding(dp(18),dp(6),dp(18),dp(18));return list;}
    private TextView label(String text,int size,int color){TextView t=new TextView(this);t.setText(text);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,dp(6),0,dp(6));return t;}
    private AlertDialog sheet(String title,LinearLayout content){
        openDialogs++;paused=true;engine.stop();gameView.resetInput();
        ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(0xff102e2e);scroll.addView(content);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(title).setView(scroll).setNegativeButton("Vissza",null).create();
        sheets.add(dialog);
        dialog.setOnDismissListener(d->{sheets.remove(dialog);openDialogs=Math.max(0,openDialogs-1);paused=!foreground||openDialogs>0;gameView.lastFrame=0;gameView.invalidate();});dialog.show();return dialog;
    }
    private android.graphics.drawable.GradientDrawable panel(int color){
        android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();d.setColor(color);d.setCornerRadius(dp(13));return d;
    }
    private ImageView icon(int id,int size){
        ImageView image=new ImageView(this);image.setImageDrawable(gameView.itemArt.icon(id));
        image.setContentDescription(data.items[id].name);image.setTag("icon:"+id);
        image.setLayoutParams(new LinearLayout.LayoutParams(dp(size),dp(size)));return image;
    }
    private LinearLayout itemCard(int id,String subtitle,boolean ready){
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(dp(8),dp(8),dp(8),dp(10));card.setBackground(panel(ready?0xff294f43:0xff193b38));
        card.addView(icon(id,78));TextView title=label(data.items[id].name,16,0xfff5e9cf);title.setGravity(Gravity.CENTER);title.setMinLines(2);card.addView(title);
        TextView status=label(subtitle,13,ready?0xffb7d2a1:0xffd0ab88);status.setGravity(Gravity.CENTER);card.addView(status);
        card.setClickable(true);card.setFocusable(true);return card;
    }
    private void addCards(LinearLayout list,java.util.List<LinearLayout> cards){
        for(int i=0;i<cards.size();i+=2){
            LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
            for(int n=0;n<2;n++){
                LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,-1,1);params.setMargins(dp(4),dp(5),dp(4),dp(5));
                if(i+n<cards.size())row.addView(cards.get(i+n),params);else row.addView(new View(this),params);
            }list.addView(row,new LinearLayout.LayoutParams(-1,-2));
        }
    }
    public void inventory(){
        LinearLayout content=list();ArrayList<LinearLayout> cards=new ArrayList<>();
        for(int id=0;id<120;id++)if(engine.inventory[id]>0){
            final int item=id;LinearLayout card=itemCard(id,"Nálad: "+engine.inventory[id],true);card.setTag("inventory:"+id);
            card.setContentDescription(data.items[id].name+", "+engine.inventory[id]+" darab");
            card.setOnClickListener(v->itemDetails(item,card));cards.add(card);
        }
        if(cards.isEmpty()){content.addView(label("Még üres a tarisznyád.",20,Color.WHITE));content.addView(label("Sétálj a tárgyhoz, majd nyisd meg a Művelet menüt. A kés a parton lévő láda mellett hever.",16,Color.LTGRAY));}
        else addCards(content,cards);
        sheet("Tarisznya",content);
    }
    private void itemDetails(int id,LinearLayout card){
        LinearLayout content=list();content.addView(icon(id,106));
        TextView count=label("Nálad: "+engine.inventory[id],19,0xffefcf94);content.addView(count);
        content.addView(label(data.items[id].description,16,Color.LTGRAY));
        if(data.items[id].attributes[1]>=0){
            content.addView(label(engine.consumptionEffects(id),17,data.items[id].attributes[6]<0?0xffe5a18a:0xffb7d2a1));
            int tool=data.items[id].attributes[1];
            if(tool>0)content.addView(label("Szükséges: "+data.items[tool].name+" · az eszköz megmarad.",15,Color.LTGRAY));
            if(data.items[id].attributes[6]<0)content.addView(label("Vigyázat: ez a tárgy életet vesz el!",17,0xffe5a18a));
            String need=engine.consumptionRequirement(id);
            if(!need.isEmpty())content.addView(label(need,15,0xffe5a18a));
            Button eat=new Button(this);eat.setText("Fogyasztás");eat.setEnabled(need.isEmpty());content.addView(eat);
            eat.setOnClickListener(v->{String result=engine.eat(id);engine.say(result);message(result);save();count.setText("Nálad: "+engine.inventory[id]);eat.setEnabled(engine.consumptionRequirement(id).isEmpty());((TextView)card.getChildAt(2)).setText("Nálad: "+engine.inventory[id]);if(engine.inventory[id]==0)card.setVisibility(View.GONE);checkGameOver();});
        }
        sheet(data.items[id].name,content);
    }
    public void actions(){
        LinearLayout content=list();
        Button collect=new Button(this);collect.setText("Közeli tárgy használata / felvétele");content.addView(collect);
        int target=engine.selected>=0&&engine.objects[engine.selected]!=0?engine.selected:engine.nearest();
        final int animal=target;
        content.addView(label("Eszköz használata",20,Color.rgb(239,211,158)));
        AlertDialog dialog=sheet("Művelet",content);
        collect.setOnClickListener(v->{
            if(engine.restTarget()){
                new AlertDialog.Builder(this).setTitle("Pihenés a táborban").setMessage(engine.restPreview())
                    .setPositiveButton("Pihenés",(d,w)->{String result=engine.interact();engine.say(result);dialog.dismiss();save();checkGameOver();})
                    .setNegativeButton("Mégsem",null).show();
            }else{String result=engine.interact();engine.say(result);message(result);dialog.dismiss();save();checkGameOver();}
        });
        for(int tool:new int[]{14,83,81,HarvestGame.COCONUT}){
            final int targetTile=tool==HarvestGame.COCONUT?engine.nearestPalm():animal;
            String need=engine.harvestRequirement(tool,targetTile);
            Button use=new Button(this);use.setText(tool==HarvestGame.COCONUT?"Kókusz leverése · kő":data.items[tool].name);use.setEnabled(need.isEmpty());content.addView(use);
            if(!need.isEmpty())content.addView(label(need,14,Color.LTGRAY));
            else content.addView(label(tool==14?"1 giliszta · mozgasd a horgot az érintőkarral, akaszd meg a halat, majd húzd fel.":tool==83?"1 burgonya · 10 hal, legfeljebb 5 merítés.":tool==HarvestGame.COCONUT?"1 kő · 10 dobásból 5 kókusz. Két találat kell egy kókuszhoz.":"1 nyílcsomag · 10 lövés. Érintőkar: szög és erő.",14,Color.LTGRAY));
            use.setOnClickListener(v->{String error=engine.startHarvest(tool,targetTile,System.nanoTime());
                if(!error.isEmpty()){message(error);return;}
                dialog.dismiss();gameView.screen=GameView.HARVEST;gameView.lastFrame=0;gameView.resetInput();save();gameView.invalidate();
            });
        }
        content.addView(label("A csalit, követ vagy nyílcsomagot az induláskor használod el. Kilépéskor a minijáték megszakad.",14,Color.LTGRAY));
    }
    public void crafting(){showCrafting(false);}
    private AlertDialog currentCraft;
    private void showCrafting(boolean readyOnly){
        LinearLayout content=list();int readyCount=0;
        for(GameData.Recipe recipe:data.recipes)if(engine.missing(recipe).isEmpty())readyCount++;
        content.addView(label("Elkészíthető: "+readyCount+" / "+data.recipes.length,18,0xffefcf94));
        Button toggle=new Button(this);toggle.setText(readyOnly?"Összes recept megjelenítése":"Csak az elkészíthető receptek");content.addView(toggle);
        ArrayList<GameData.Recipe> recipes=new ArrayList<>(Arrays.asList(data.recipes));
        recipes.sort(Comparator.comparingInt(r->engine.missing(r).isEmpty()?0:1));
        ArrayList<LinearLayout> cards=new ArrayList<>();
        for(GameData.Recipe r:recipes){
            boolean ready=engine.missing(r).isEmpty();if(readyOnly&&!ready)continue;
            LinearLayout card=itemCard(r.item,ready?"Elkészíthető":"Hiányzó hozzávalók",ready);card.setTag("recipe:"+r.item);
            card.setContentDescription(data.items[r.item].name+", "+(ready?"elkészíthető":"hozzávalók hiányoznak"));
            card.setOnClickListener(v->recipeDetails(r,readyOnly));cards.add(card);
        }
        if(cards.isEmpty())content.addView(label("Még nincs elég alapanyagod vagy eszközöd. Az összes receptnél láthatod, mire van szükséged.",16,Color.WHITE));
        else addCards(content,cards);
        currentCraft=sheet("Készítés · 45 recept",content);
        toggle.setOnClickListener(v->{currentCraft.dismiss();showCrafting(!readyOnly);});
    }
    private void requirement(LinearLayout content,int id,int need,int have,String kind){
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(7),dp(5),dp(7),dp(5));
        row.addView(icon(id,48));LinearLayout description=new LinearLayout(this);description.setOrientation(LinearLayout.VERTICAL);description.setPadding(dp(9),0,0,0);
        description.addView(label(data.items[id].name,16,0xfff5e9cf));
        description.addView(label(kind+" · "+Math.max(0,have)+" / "+need,14,have>=need?0xffb7d2a1:0xffe5a18a));
        row.addView(description,new LinearLayout.LayoutParams(0,-2,1));content.addView(row);
    }
    private void recipeDetails(GameData.Recipe recipe,boolean readyOnly){
        LinearLayout content=list();content.addView(icon(recipe.item,106));
        boolean ready=engine.missing(recipe).isEmpty();
        content.addView(label(ready?"Minden megvan az elkészítéshez.":"Még szükséged van hozzávalókra vagy megfelelő helyre.",16,ready?0xffb7d2a1:0xffe5a18a));
        content.addView(label("Alapanyagok · felhasználódnak",18,0xffefcf94));
        int[] consumed=new int[120];for(int[] pair:recipe.ingredients)consumed[pair[0]]+=pair[1];
        for(int[] pair:recipe.ingredients)requirement(content,pair[0],pair[1],engine.inventory[pair[0]],"Nálad / szükséges");
        if(recipe.tools.length>0)content.addView(label("Eszközök · megmaradnak",18,0xffefcf94));
        for(int[] pair:recipe.tools)requirement(content,pair[0],pair[1],engine.toolAmount(pair[0])-consumed[pair[0]],"Elérhető / szükséges");
        if(recipe.context>=0){content.addView(label("Készítés helye",18,0xffefcf94));requirement(content,recipe.context,1,engine.contextAvailable(recipe.context)?1:0,"Elérhető / szükséges");}
        Button create=new Button(this);create.setText("Elkészítés");create.setEnabled(ready);content.addView(create);
        AlertDialog detail=sheet(data.items[recipe.item].name,content);
        create.setOnClickListener(v->{String result=engine.craft(recipe.item);engine.say(result);message(result);save();detail.dismiss();if(currentCraft!=null)currentCraft.dismiss();showCrafting(readyOnly);});
    }
    public void journal(){
        LinearLayout content=list();
        content.addView(label("A sziget feljegyzései",20,Color.rgb(239,211,158)));
        content.addView(label("Fedezd fel a partot, keresd össze a felszerelést, és építs menedéket. A távoli roncsok helyreállításához több eszközre és alapanyagra lesz szükséged.",16,Color.WHITE));
        for(int i=0;i<data.events.length;i++)if(engine.seen.get(i)){
            content.addView(label(data.events[i].title,19,Color.rgb(239,211,158)));
            content.addView(label(data.events[i].text,14,Color.LTGRAY));
        }
        int knife=engine.findObject(76);
        Button findKnife=null;
        if(engine.inventory[76]==0&&knife>=0){
            content.addView(label("Első eszköz: a kés",19,0xffefcf94));content.addView(icon(76,72));
            content.addView(label("A kezdőparton egy láda mellett hever. A part mentén haladva éred el.",16,Color.WHITE));
            findKnife=new Button(this);findKnife.setText("Út a késhez");content.addView(findKnife);
        }
        AlertDialog journal=sheet("Napló",content);
        if(findKnife!=null)findKnife.setOnClickListener(v->{boolean found=engine.navigate(knife%180,knife/180);if(found)engine.selected=knife;journal.dismiss();gameView.screen=GameView.WORLD;gameView.lastFrame=0;gameView.invalidate();});
    }
    public void pauseMenu(){
        openDialogs++;paused=true;engine.stop();gameView.resetInput();
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Pihenő")
            .setItems(new String[]{"Játék folytatása","Mentés","Vissza a főmenübe"},(d,item)->{
                if(item==1){save();engine.say("A kalandot elmentettem.");}
                if(item==2){save();gameView.screen=GameView.TITLE;}
            }).create();
        dialog.setOnDismissListener(d->{openDialogs=Math.max(0,openDialogs-1);paused=!foreground||openDialogs>0;gameView.lastFrame=0;gameView.invalidate();});dialog.show();
    }
    @Override public void onBackPressed(){if(gameView.screen==GameView.DEAD){returnToTitle();}else if(gameView.screen==GameView.HARVEST){gameView.closeHarvest();}else if(gameView.screen==GameView.MAP){gameView.screen=GameView.WORLD;gameView.invalidate();}else if(gameView.screen==GameView.WORLD)pauseMenu();else super.onBackPressed();}
    @Override protected void onPause(){super.onPause();foreground=false;paused=true;gameView.resetInput();
        if(engine.harvest!=null&&engine.harvest.finished){engine.say(engine.finishHarvest());gameView.screen=GameView.WORLD;}
        save();}
    @Override protected void onResume(){super.onResume();foreground=true;paused=openDialogs>0;if(gameView!=null){gameView.lastFrame=0;gameView.invalidate();}}
}
