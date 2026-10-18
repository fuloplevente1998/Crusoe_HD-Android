import hu.crusoe.standalone.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Survival regressions against the original item data and saved native state. */
public final class SurvivalChecks {
    static int passed;
    static void check(boolean ok,String description){
        if(!ok)throw new AssertionError(description);
        passed++;System.out.println("PASS "+description);
    }
    public static void main(String[] args)throws Exception {
        GameData data=new GameData(new FileInputStream(args[0]));
        GameEngine e=new GameEngine(data);Arrays.fill(e.objects,(byte)0);
        e.food=20;e.water=40;e.health=50;e.inventory[0]=2;
        check(e.eat(0).startsWith("Elfogyasztottad"),"Mango can be consumed");
        check(e.food==23&&e.water==55&&e.health==58&&e.inventory[0]==1,
              "Original mango gives 3 food, 15 water and 8 health exactly once");
        check(e.consumptionEffects(0).equals("Élelem +3 · Víz +15 · Élet +8"),
              "Food preview uses the original item effects");
        e.inventory[1]=1;
        check(e.eat(1).startsWith("Szükséges:")&&e.inventory[1]==1,
              "Coconut cannot be consumed without its required stone");
        e.inventory[3]=1;e.eat(1);
        check(e.inventory[1]==0&&e.inventory[3]==1&&e.food==26&&e.water==80&&e.health==66,
              "Opening coconut retains its stone and applies nutrition once");
        e.inventory[66]=1;
        check(!e.consumptionRequirement(66).isEmpty(),"Oyster needs a knife before consumption");
        e.inventory[76]=1;e.eat(66);
        check(e.inventory[76]==1&&e.inventory[66]==0&&e.health==96,
              "Knife is retained after opening an oyster");
        e.inventory[0]=1;e.food=e.water=e.health=99;e.eat(0);
        check(e.food==100&&e.water==100&&e.health==100,"Nutrition never overflows full survival bars");
        e.inventory[31]=1;e.health=40;e.food=e.water=50;
        check(e.eat(31).startsWith("Szükséges:")&&e.health==40&&e.inventory[31]==1,
              "Red mushroom cannot bypass its original fire requirement");
        int fire=(int)e.y*180+(int)e.x+1;e.objects[fire]=17;
        e.route.add(fire);e.eat(31);
        check(e.isDead()&&e.health==0&&e.inventory[31]==0,"Original -50 health mushroom is lethal at 40 life");
        check(e.food==50&&e.water==50,"Original -1 nutrition sentinel does not subtract food or water");
        check(e.objects[fire]==17&&e.route.isEmpty(),"Lethal consumption keeps the fire and stops movement");
        int[] inventory=e.inventory.clone();byte[] world=e.objects.clone();float x=e.x,y=e.y,hour=e.hour;
        e.inventory[0]=1;inventory=e.inventory.clone();
        check(e.eat(0).equals(GameEngine.DEAD_MESSAGE)&&e.health==0,
              "A dead character cannot revive by eating");
        e.selected=fire;e.interact();e.craft(5);e.navigate(7,147);e.update(.1f,1,0);
        check(Arrays.equals(inventory,e.inventory)&&Arrays.equals(world,e.objects)
                &&e.x==x&&e.y==y&&e.hour==hour&&e.route.isEmpty(),
              "Death freezes movement, clock, collecting, crafting and inventory");
        check(!e.harvestRequirement(HarvestGame.COCONUT,0).isEmpty(),"Death blocks starting minigames");
        e.harvest=new HarvestGame(HarvestGame.COCONUT,0,1,1);
        e.harvest.finished=true;e.harvest.won=true;e.finishHarvest();
        check(e.harvest==null&&Arrays.equals(inventory,e.inventory),"Death cannot award a pending harvest prize");
        File save=Files.createTempFile("crusoe-survival-",".save").toFile();
        try{
            e.save(save);GameEngine loaded=new GameEngine(data);loaded.load(save);
            check(loaded.isDead()&&loaded.health==0&&Arrays.equals(e.inventory,loaded.inventory),
                  "Existing save-v1 format persists death without a new schema");
            check(Arrays.equals(e.objects,loaded.objects),"Loading a dead save does not respawn a rescue stone");
        }finally{save.delete();}
        GameEngine starved=new GameEngine(data);starved.food=0;starved.health=.001f;
        starved.update(.1f,0,0);
        check(starved.isDead(),"Starvation damage enters the same death state");
        GameEngine warning=new GameEngine(data);warning.water=10;
        check(warning.survivalWarning().startsWith("Kevés a víz"),"Critical water warning starts at ten percent");
        warning.health=10;
        check(warning.survivalWarning().startsWith("Veszélyesen kevés élet"),"Critical life warning takes priority");
        warning.water=11;warning.food=7;
        check(warning.restPreview().contains("egyél és igyál"),"Rest preview warns when supplies run out during sleep");
        System.out.println("Survival checks passed: "+passed);
    }
}
