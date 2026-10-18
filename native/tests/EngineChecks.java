import hu.crusoe.standalone.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public final class EngineChecks {
    static GameData data;
    static int passed;
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);passed++;System.out.println("PASS "+message);}
    static void approach(GameEngine e,int tile){
        check(e.navigate(tile%180,tile/180),"A reachable route exists to tile "+tile);
        int limit=20000;while(!e.route.isEmpty()&&limit-->0)e.update(.05f,0,0);
        check(limit>0&&e.near(tile,1.8f),"Following the route reaches an interaction position");
    }
    static int reachable(GameEngine e,int item){
        ArrayList<Integer> candidates=new ArrayList<>();
        for(int i=0;i<e.objects.length;i++)if(e.objectAt(i)==item)candidates.add(i);
        candidates.sort(Comparator.comparingDouble(i->Math.hypot(i%180+.5-e.x,i/180+.5-e.y)));
        for(int tile:candidates)if(e.navigate(tile%180,tile/180))return tile;
        throw new AssertionError("No reachable original item: "+item);
    }
    static void shore(GameEngine e,int terrain){
        for(int y=1;y<179;y++)for(int x=1;x<179;x++)if(e.passable(x,y)){
            e.x=x+.5f;e.y=y+.5f;if(e.nearWater(terrain))return;
        }throw new AssertionError("No original shore for terrain "+terrain);
    }
    static void harvestChecks()throws Exception {
        GameEngine e=new GameEngine(data);shore(e,5);
        check(e.nearWater(5),"Rod uses original deep-water terrain 5");
        check(!e.startHarvest(14,-1,1).isEmpty()&&e.harvest==null,"Missing rod cannot start fishing");
        e.inventory[14]=1;
        check(!e.startHarvest(14,-1,1).isEmpty(),"Rod requires original worm bait");
        e.inventory[90]=2;
        check(e.startHarvest(14,-1,1).isEmpty()&&e.inventory[90]==1,"Starting a rod session reserves exactly one worm");
        check(!e.startHarvest(14,-1,2).isEmpty()&&e.inventory[90]==1,"Repeated start cannot spend extra bait or replace the active game");
        HarvestGame g=e.harvest;int reward=g.reward;
        check(reward==40||reward==56||reward==57||reward==58,"Rod chooses only an original deep-water fish");
        g.moveHook(g.fishX[0],g.fishY[0]);g.action();
        check(g.hooked&&!g.finished&&e.inventory[reward]==0,"Hooking a fish requires reeling before any reward");
        g.moveHook(360,40);e.finishHarvest();
        check(e.inventory[reward]==1&&e.inventory[14]==1,"Reeling to the surface awards one fish and preserves the rod");
        e.finishHarvest();check(e.inventory[reward]==1,"Repeated result settlement cannot duplicate a reward");
        e.startHarvest(14,-1,2);e.cancelHarvest();
        check(e.inventory[90]==0&&e.harvest==null,"Cancelling keeps bait spent and leaves no active session");
        e.inventory[90]=1;e.x=10.5f;e.y=147.5f;
        if(!e.nearWater(5))check(!e.startHarvest(14,-1,3).isEmpty()&&e.inventory[90]==1,"Inland rod use fails without consuming bait");
        e.inventory[83]=1;e.inventory[26]=2;shore(e,4);
        check(e.startHarvest(83,-1,4).isEmpty()&&e.inventory[26]==1,"Net uses original shallow water 4 and one potato");
        g=e.harvest;for(int n=0;n<5;n++){g.action();g.action();while(g.dropping)g.update(.1f);if(g.finished)break;}
        check(g.finished&&g.attempts<=5,"Net session terminates within five drops; duplicate taps do not add attempts");
        check(g.won==(g.total>=10),"Net victory requires at least ten caught fish");
        e.finishHarvest();
        // A controlled interception verifies capture along the moving net, including reward settlement.
        e.startHarvest(83,-1,5);g=e.harvest;reward=g.reward;
        for(int n=0;n<2;n++){
            for(int i=0;i<7;i++){g.fishX[i]=360;g.fishSpeed[i]=0;}
            g.action();while(g.dropping)g.update(.1f);
        }
        check(g.won&&g.total>=10&&g.attempts==2,"Net catches fish along its descent and can win before the fifth drop");
        int count=e.inventory[reward];e.finishHarvest();
        check(e.inventory[reward]==count+1&&e.inventory[83]==1,"Successful net awards one original result and retains its tool");
        int target=e.findObject(4);e.x=target%180+.5f;e.y=target/180+1.5f;e.selected=target;
        int old=e.inventory[4];e.interact();
        check(e.inventory[4]==old&&e.objectAt(target)==4,"Animals cannot be collected as ordinary objects");
        e.inventory[81]=1;
        check(!e.startHarvest(81,target,7).isEmpty(),"Bow requires an arrow bundle");
        e.inventory[45]=2;e.x=0;e.y=0;
        check(!e.startHarvest(81,target,7).isEmpty()&&e.inventory[45]==2,"Distant hunting is rejected atomically");
        e.x=target%180+.5f;e.y=target/180+1.5f;
        check(e.startHarvest(81,target,7).isEmpty()&&e.inventory[45]==1,"Hunting reserves one bundle for up to ten shots");
        e.cancelHarvest();
        // Find an actual ballistic hit using legal aim settings, rather than setting a victory flag.
        HarvestGame success=null;
        outer:for(int angle=10;angle<=85;angle+=5)for(int power=10;power<=100;power+=5){
            g=new HarvestGame(81,target,4,9);g.aim(angle,power);g.action();
            for(int t=0;t<120&&g.flying&&!g.finished;t++)g.update(.05f);
            if(g.won){success=g;break outer;}
        }
        check(success!=null&&success.arrowVY>0,"A legal ballistic shot can hit the animal only while descending");
        e.startHarvest(81,target,9);g=e.harvest;g.aim(success.angle,success.power);g.action();
        while(!g.finished&&g.flying)g.update(.05f);e.finishHarvest();
        check(e.inventory[4]==old+1&&e.objectAt(target)==-1&&e.inventory[81]==1,"Successful hunt removes the original animal once and retains the bow");
        g=new HarvestGame(81,target,4,9);g.aim(85,10);
        for(int n=0;n<10;n++){g.action();g.action();while(g.flying&&!g.finished)g.update(.1f);}
        check(g.finished&&!g.won&&g.attempts==10,"Hunting stops after ten misses without duplicate-tap shots");
        File legacy=new File("artifacts/baseline-031/island-v1.save");
        if(legacy.isFile()){
            GameEngine restored=new GameEngine(data);restored.load(legacy);
            check(restored.inventory[76]==1&&restored.inventory[8]==7&&restored.health==73&&restored.food==62&&restored.water==51&&restored.energy==40&&restored.day==9&&restored.hour==14.5f&&restored.seen.get(3)&&restored.findObject(76)<0,"Actual 0.3.1 engine save loads with inventory, world, stats and journal intact");
        }
    }
    public static void main(String[] args)throws Exception {
        data=new GameData(new FileInputStream(args[0]));
        check(data.items.length==120&&data.recipes.length==45,"Complete original item and recipe catalog");
        check(data.sourceHash.equals("f7066eacb94da5e624d488a9b0e058b1f1b70f8c3cb39c6a37a8b8e8e68c74a8"),"Expected original source fingerprint");
        GameEngine e=new GameEngine(data);
        check(e.passable((int)e.x,(int)e.y),"Spawn is on a walkable tile");
        check(!e.passable(-1,4)&&!e.passable(180,4),"Map bounds block movement");
        int knife=e.findObject(76);String far=e.interact();
        check(e.inventory[76]==0,"Distant knife cannot be gathered");
        approach(e,knife);e.selected=knife;
        check(e.interact().contains(data.items[76].name)&&e.inventory[76]==1&&e.objectAt(knife)==-1,"Original knife is picked up once");
        e.selected=knife;e.interact();check(e.inventory[76]==1,"Repeated action does not duplicate the picked-up tool");
        int branch=reachable(e,8),stone=reachable(e,3),vine=reachable(e,6);
        for(int tile:new int[]{branch,stone,vine}){approach(e,tile);e.selected=tile;e.interact();}
        check(e.inventory[8]>0&&e.inventory[3]>0&&e.inventory[6]>0,"Original nearby materials can be collected using the knife");
        check(e.missing(data.recipe(5)).isEmpty(),"Axe's original ingredient requirements are satisfied");
        check(e.craft(5).contains("Elkészült")&&e.inventory[5]==1&&e.inventory[76]==1,"Axe crafting consumes materials and retains the knife");
        int[] unchanged=e.inventory.clone();e.craft(19);check(Arrays.equals(unchanged,e.inventory),"Failed pirate-ship crafting is atomic");
        for(GameData.Recipe r:data.recipes){
            GameEngine fresh=new GameEngine(data);
            if(r.context>=0){
                for(int[] p:r.ingredients)fresh.inventory[p[0]]+=p[1];
                for(int[] p:r.tools)fresh.inventory[p[0]]+=p[1];
                check(fresh.missing(r).contains("Hely:"),"Recipe "+r.item+" enforces original context "+r.context);
            }
        }
        // Verify 45 recipes individually, including building placement and tool retention.
        for(GameData.Recipe r:data.recipes){
            GameEngine g=new GameEngine(data);
            Arrays.fill(g.objects,(byte)0);
            g.x=10.5f;g.y=147.5f;
            // Find a patch with free neighboring ground for placeable crafted objects.
            outer:for(int yy=1;yy<179;yy++)for(int xx=1;xx<179;xx++)if(g.passable(xx,yy)&&g.passable(xx+1,yy)&&g.passable(xx,yy+1)){g.x=xx+.5f;g.y=yy+.5f;break outer;}
            if(r.context>=0){int at=(int)g.y*180+(int)g.x+1;g.objects[at]=(byte)(r.context+1);}
            for(int[] p:r.ingredients)g.inventory[p[0]]+=p[1];
            for(int[] p:r.tools)g.inventory[p[0]]+=p[1];
            check(g.missing(r).isEmpty(),"Recipe "+r.item+" accepts its exact original materials/tools/context");
            String result=g.craft(r.item);
            check(result.startsWith("Elkészült"),"Recipe "+r.item+" completes: "+result);
            for(int[] p:r.tools)check(g.inventory[p[0]]>=p[1],"Recipe "+r.item+" retains tool "+p[0]);
            check(g.inventory[r.item]>0||g.findObject(r.item)>=0,"Recipe "+r.item+" produces its result");
        }
        File file=Files.createTempFile("crusoe-native-save-",".dat").toFile();e.health=52;e.food=44;e.water=33;e.energy=71;e.day=3;e.hour=12.5f;e.save(file);
        GameEngine restored=new GameEngine(data);restored.load(file);
        check(restored.x==e.x&&restored.y==e.y&&Arrays.equals(e.inventory,restored.inventory)&&Arrays.equals(e.objects,restored.objects)&&e.seen.equals(restored.seen)&&e.health==restored.health&&e.day==restored.day,"Save/load preserves coordinates, stats, inventory, world and events");
        byte[] bytes=Files.readAllBytes(file.toPath());bytes[200]^=1;Files.write(file.toPath(),bytes);
        int[] before=restored.inventory.clone();float x=restored.x;
        try{restored.load(file);throw new AssertionError("Corrupt save accepted");}catch(IOException expected){check(restored.x==x&&Arrays.equals(before,restored.inventory),"Corrupt save is rejected without partial state changes");}
        float energy=e.energy;
        int near=reachable(e,8);e.navigate(near%180,near/180);int pending=e.route.size();
        e.update(0,0,0);check(e.energy==energy,"Zero-time frame cannot advance survival state");
        check(pending>0&&e.route.size()==pending,"First zero-time display frame preserves the requested path");
        file.delete();harvestChecks();System.out.println("Engine checks passed: "+passed);
    }
}
