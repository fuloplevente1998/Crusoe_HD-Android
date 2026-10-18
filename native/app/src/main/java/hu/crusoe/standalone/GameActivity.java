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
    private int openDialogs;
    private boolean foreground=true;
    private File saveFile;

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        try {data=new GameData(getAssets().open("world.pack"));}
        catch(IOException e){throw new IllegalStateException("A játék adatai nem tölthetők be",e);}
        saveFile=new File(getFilesDir(),"island-v1.save");
        engine=new GameEngine(data);gameView=new GameView(this);setContentView(gameView);
    }
    public boolean hasSave(){return saveFile.isFile();}
    public void start(boolean resume){
        GameEngine next=new GameEngine(data);
        if(resume)try{next.load(saveFile);}catch(IOException e){message("A mentés sérült vagy nem kompatibilis. Új játékot indíthatsz.");return;}
        engine=next;gameView.screen=GameView.WORLD;gameView.centerCamera();paused=false;
        gameView.invalidate();
    }
    public void save(){
        if(gameView.screen==GameView.TITLE)return;
        try{engine.save(saveFile);}catch(IOException e){engine.say("A mentés nem sikerült. Ellenőrizd a szabad tárhelyet.");}
    }
    public void message(String text){Toast.makeText(this,text,Toast.LENGTH_LONG).show();}
    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    private LinearLayout list(){LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);list.setPadding(dp(18),dp(6),dp(18),dp(18));return list;}
    private TextView label(String text,int size,int color){TextView t=new TextView(this);t.setText(text);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,dp(6),0,dp(6));return t;}
    private AlertDialog sheet(String title,LinearLayout content){
        openDialogs++;paused=true;engine.stop();gameView.resetInput();
        ScrollView scroll=new ScrollView(this);scroll.addView(content);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(title).setView(scroll).setNegativeButton("Vissza",null).create();
        dialog.setOnDismissListener(d->{openDialogs=Math.max(0,openDialogs-1);paused=!foreground||openDialogs>0;gameView.lastFrame=0;gameView.invalidate();});dialog.show();return dialog;
    }
    private String pairText(int[][] pairs){
        ArrayList<String> values=new ArrayList<>();for(int[] p:pairs)values.add(p[1]+" × "+data.items[p[0]].name);return String.join(", ",values);
    }
    public void inventory(){
        LinearLayout content=list();int count=0;
        for(int id=0;id<120;id++)if(engine.inventory[id]>0){
            count++;final int item=id;GameData.Item d=data.items[id];
            content.addView(label(d.name+"  ×"+engine.inventory[id],19,Color.rgb(239,211,158)));
            Button button=new Button(this);button.setText(d.attributes[1]>=0?"Fogyasztás":"Megvizsgálás");
            button.setOnClickListener(v->{
                if(d.attributes[1]>=0){String result=engine.eat(item);engine.say(result);message(result);save();}
                else message(d.description);
            });content.addView(button);
        }
        if(count==0){content.addView(label("Még üres a tarisznyád.",20,Color.WHITE));content.addView(label("A tárgyhoz sétálva a Felvesz gombbal gyűjthetsz. Néhány növényhez késre, a rönkökhöz fejszére lesz szükséged.",16,Color.LTGRAY));}
        sheet("Tarisznya",content);
    }
    public void actions(){
        LinearLayout content=list();
        Button collect=new Button(this);collect.setText("Közeli tárgy használata / felvétele");content.addView(collect);
        int target=engine.selected>=0&&engine.objects[engine.selected]!=0?engine.selected:engine.nearest();
        final int animal=target;
        content.addView(label("Eszköz használata",20,Color.rgb(239,211,158)));
        AlertDialog dialog=sheet("Művelet",content);
        collect.setOnClickListener(v->{String result=engine.interact();engine.say(result);message(result);dialog.dismiss();save();});
        for(int tool:new int[]{14,83,81}){
            String need=engine.harvestRequirement(tool,animal);
            Button use=new Button(this);use.setText(data.items[tool].name);use.setEnabled(need.isEmpty());content.addView(use);
            if(!need.isEmpty())content.addView(label(need,14,Color.LTGRAY));
            else content.addView(label(tool==14?"1 giliszta · húzd a horgot a halra, engedd el, majd húzd a felszínre.":tool==83?"1 burgonya · 10 hal, legfeljebb 5 merítés.":"1 nyílcsomag · 10 lövés. A lefelé tartó nyíllal célozz.",14,Color.LTGRAY));
            use.setOnClickListener(v->{String error=engine.startHarvest(tool,animal,System.nanoTime());
                if(!error.isEmpty()){message(error);return;}
                dialog.dismiss();gameView.screen=GameView.HARVEST;gameView.lastFrame=0;gameView.resetInput();save();gameView.invalidate();
            });
        }
        content.addView(label("A csalit vagy nyílcsomagot az induláskor használod el. Kilépéskor a minijáték megszakad.",14,Color.LTGRAY));
    }
    public void crafting(){showCrafting(false);}
    private void showCrafting(boolean readyOnly){
        LinearLayout content=list();
        Button toggle=new Button(this);toggle.setText(readyOnly?"Összes recept megjelenítése":"Csak az elkészíthető receptek");content.addView(toggle);
        ArrayList<GameData.Recipe> recipes=new ArrayList<>(Arrays.asList(data.recipes));
        recipes.sort(Comparator.comparingInt(r->engine.missing(r).isEmpty()?0:1));
        int visible=0;
        for(GameData.Recipe r:recipes){
            String missing=engine.missing(r);if(readyOnly&&!missing.isEmpty())continue;visible++;
            TextView title=label(data.items[r.item].name,20,Color.rgb(239,211,158));content.addView(title);
            String details=pairText(r.ingredients);
            if(r.tools.length>0)details+="\nEszközök: "+pairText(r.tools);
            if(r.context>=0)details+="\nHely: "+data.items[r.context].name;
            content.addView(label(details,14,Color.LTGRAY));
            if(!missing.isEmpty())content.addView(label("Hiányzik: "+missing,13,Color.rgb(219,154,135)));
            Button create=new Button(this);create.setText("Elkészítés");create.setEnabled(missing.isEmpty());content.addView(create);
            create.setOnClickListener(v->{String result=engine.craft(r.item);engine.say(result);message(result);save();refreshCraftDialog();});
            View divider=new View(this);divider.setBackgroundColor(0x334e8d84);content.addView(divider,new LinearLayout.LayoutParams(-1,dp(1)));
        }
        if(visible==0)content.addView(label("Még nincs elég alapanyagod és eszközöd. Az összes receptnél láthatod, mire van szükséged.",16,Color.WHITE));
        currentCraft=sheet("Készítés · 45 recept",content);
        toggle.setOnClickListener(v->{currentCraft.dismiss();showCrafting(!readyOnly);});
    }
    private AlertDialog currentCraft;
    private void refreshCraftDialog(){if(currentCraft!=null){currentCraft.dismiss();showCrafting(false);}}
    public void journal(){
        LinearLayout content=list();
        content.addView(label("A sziget feljegyzései",20,Color.rgb(239,211,158)));
        content.addView(label("Fedezd fel a partot, keresd össze a felszerelést, és építs menedéket. A távoli roncsok helyreállításához több eszközre és alapanyagra lesz szükséged.",16,Color.WHITE));
        for(int i=0;i<data.events.length;i++)if(engine.seen.get(i)){
            content.addView(label(data.events[i].title,19,Color.rgb(239,211,158)));
            content.addView(label(data.events[i].text,14,Color.LTGRAY));
        }
        sheet("Napló",content);
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
    @Override public void onBackPressed(){if(gameView.screen==GameView.HARVEST){gameView.closeHarvest();}else if(gameView.screen==GameView.MAP){gameView.screen=GameView.WORLD;gameView.invalidate();}else if(gameView.screen==GameView.WORLD)pauseMenu();else super.onBackPressed();}
    @Override protected void onPause(){super.onPause();foreground=false;paused=true;gameView.resetInput();
        if(engine.harvest!=null&&engine.harvest.finished){engine.say(engine.finishHarvest());gameView.screen=GameView.WORLD;}
        save();}
    @Override protected void onResume(){super.onResume();foreground=true;paused=openDialogs>0;if(gameView!=null){gameView.lastFrame=0;gameView.invalidate();}}
}
