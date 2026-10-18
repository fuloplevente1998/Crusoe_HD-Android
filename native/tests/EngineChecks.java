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
        for(String version:new String[]{"031","050"}){
            File legacy=new File("artifacts/baseline-"+version+"/island-v1.save");
            if(legacy.isFile()){
                GameEngine restored=new GameEngine(data);restored.load(legacy);
                check(restored.inventory[76]==1&&restored.inventory[8]==7&&restored.health==73&&restored.food==62&&restored.water==51&&restored.energy==40&&restored.day==9&&restored.hour==14.5f&&restored.seen.get(3)&&restored.findObject(76)<0,"Actual "+version+" engine save loads with inventory, world, stats and journal intact");
            }
        }
    }
    static void aimAtCoconut(HarvestGame game,int index){
        double dx=game.coconutX[index]-54,height=580-game.coconutY[index];
        for(float angle=50;angle<=85;angle+=.5f){
            double radians=Math.toRadians(angle),denominator=Math.cos(radians)*Math.cos(radians)*(dx*Math.tan(radians)-height);
            if(denominator<=0)continue;
            double speed=Math.sqrt(300*dx*dx/denominator),power=(speed-330)/5;
            if(power>=40&&power<=100){game.aim(angle,(float)power);return;}
        }throw new AssertionError("No legal coconut trajectory");
    }
    static void throwAt(HarvestGame game,int coconut){
        aimAtCoconut(game,coconut);game.action();int limit=200;
        while(game.flying&&!game.finished&&limit-->0)game.update(.05f);
        check(limit>0,"Coconut projectile completes a legal flight");
    }
    static void joystickAndCoconuts()throws Exception {
        HarvestGame bow=new HarvestGame(81,0,4,7);float angle=bow.angle,power=bow.power;
        bow.steer(1,-1,.1f);check(bow.angle>angle&&bow.power>power,"Diagonal stick input changes both angle and power continuously");
        angle=bow.angle;power=bow.power;bow.steer(0,0,.1f);bow.steer(Float.NaN,1,.1f);
        check(bow.angle==angle&&bow.power==power,"Released stick retains aim and invalid input cannot corrupt it");
        bow.aim(-999,-999);check(bow.angle==10&&bow.power==40,"Original-style aim clamps angle and minimum power");
        bow.aim(999,999);check(bow.angle==85&&bow.power==100,"Aim cannot exceed maximum angle or power");
        bow.action();bow.steer(-1,1,.1f);check(bow.angle==85&&bow.power==100,"In-flight shot keeps its launch aim");
        HarvestGame rod=new HarvestGame(14,-1,-1,7);float hx=rod.hookX,hy=rod.hookY;
        rod.steer(1,1,.1f);check(rod.hookX>hx&&rod.hookY>hy,"Fishing stick moves the hook on both axes");
        GameEngine e=new GameEngine(data);int palm=e.nearestPalm();check(palm>=0&&e.coconutPalm(palm),"Original coconut-palm decoration is reachable from the starting beach");
        check(!e.startHarvest(3,palm,7).isEmpty()&&e.harvest==null,"Coconut game cannot start without a stone");
        e.inventory[3]=2;check(!e.startHarvest(3,-1,7).isEmpty()&&e.inventory[3]==2,"Invalid palm does not consume a stone");
        check(e.startHarvest(3,palm,7).isEmpty()&&e.inventory[3]==1,"Starting coconut game reserves one stone once");
        check(!e.startHarvest(3,palm,7).isEmpty()&&e.inventory[3]==1,"Repeated start cannot consume a second stone");
        HarvestGame game=e.harvest;throwAt(game,0);
        check(game.coconutHits[0]==1&&game.total==0,"A first real hit shakes a coconut without awarding it");
        throwAt(game,0);check(game.coconutHits[0]==2&&game.total>=1,"Two separate stone hits dislodge a coconut");
        check(e.inventory[1]==0&&!game.won,"One dislodged coconut does not meet the five-coconut goal");
        for(int index=1;index<5&&!game.finished;index++)while(game.coconutHits[index]<2&&!game.finished)throwAt(game,index);
        for(int t=0;t<50&&!game.finished;t++)game.update(.1f);
        check(game.won&&game.total>=5&&game.attempts<=10,"Ten legal throws can dislodge five coconuts and win");
        e.finishHarvest();check(e.inventory[1]==1&&e.inventory[3]==1&&e.coconutPalm(palm),"Coconut win grants the original single reward and preserves the palm");
        e.finishHarvest();check(e.inventory[1]==1,"Coconut result cannot award twice");
        game=new HarvestGame(3,palm,1,7);throwAt(game,0);
        for(int t=0;t<130;t++)game.update(.1f);
        check(game.coconutHits[0]==0,"Unconfirmed first hit resets after the original eleven-second interval");
        game=new HarvestGame(3,palm,1,7);game.aim(85,40);
        for(int shot=0;shot<10;shot++){game.action();game.action();while(game.flying&&!game.finished)game.update(.1f);}
        check(game.finished&&!game.won&&game.attempts==10,"Coconut game stops after ten misses and ignores duplicate fire taps");
    }
    static void stoneRespawnChecks()throws Exception {
        GameEngine depleted=new GameEngine(data);
        byte[] original=data.layers[GameData.OBJECTS];
        int initial=0;
        for(int i=0;i<depleted.objects.length;i++){
            if((original[i]&255)==4){initial++;depleted.objects[i]=0;}
        }
        check(initial==54,"Original world has exactly 54 stone source sites");
        depleted.inventory[3]=0;
        depleted.day=3;depleted.hour=12.5f;
        int[] originalInventory=depleted.inventory.clone();
        float x=depleted.x,y=depleted.y;
        File saved=Files.createTempFile("crusoe-exhausted-stones-",".dat").toFile();
        try {
            depleted.save(saved); // Existing v1 save layout, no migration/destruction.
            int isolated=5*180+44; // Original rock deposit on the separated northern map.
            check((original[isolated]&255)==4,"Remote stone source exists in original map");
            depleted.objects[isolated]=4;
            depleted.save(saved);
            GameEngine isolatedRecovery=new GameEngine(data);
            isolatedRecovery.load(saved);
            int isolatedCount=0;
            for(int i=0;i<isolatedRecovery.objects.length;i++)if(isolatedRecovery.objectAt(i)==3)isolatedCount++;
            check(isolatedCount==2,"Save with only a distant, inaccessible stone recovers one locally");
            depleted.objects[isolated]=0;
            depleted.save(saved);
            GameEngine recovered=new GameEngine(data);
            recovered.load(saved);
            int stones=0,source=-1;
            for(int i=0;i<recovered.objects.length;i++)if(recovered.objectAt(i)==3){stones++;source=i;}
            check(stones==1&&source>=0&&(original[source]&255)==4,
                  "Legacy depleted save restores exactly one original stone source");
            check(Arrays.equals(originalInventory,recovered.inventory)&&recovered.x==x&&recovered.y==y
                      &&recovered.day==3&&recovered.hour==12.5f,
                  "Rescue preserves inventory, player coordinates, game day and time");
            check(recovered.notice.contains("kő"),"Recovery informs the player about restored stone");
            recovered.save(saved);
            GameEngine repeated=new GameEngine(data);
            repeated.load(saved);
            int afterReload=0;
            for(int i=0;i<repeated.objects.length;i++)if(repeated.objectAt(i)==3)afterReload++;
            check(afterReload==1,"Second load does not duplicate a restored stone");
            repeated.hour=23.999f;
            int beforeDay=repeated.day;
            repeated.update(0.1f,0,0);
            int atMidnight=0;
            for(int i=0;i<repeated.objects.length;i++)if(repeated.objectAt(i)==3)atMidnight++;
            check(repeated.day==beforeDay+1&&atMidnight==1,
                  "Midnight changes the day but does not prematurely respawn stones");
            // Exhaust every renewable kind, not only stone, before 04:00.
            int[] renewableCodes={1,2,4,7,8,9,12,14,21,22,25,26,27,28,29,
                    31,32,33,34,36,39,40,65,66,67,68,69,71,72,73,74,75,
                    76,91,94,99};
            boolean[] renewable=new boolean[256];
            for(int code:renewableCodes)renewable[code]=true;
            int depletedSources=0,occupiedSource=-1,oneOff=-1,animal=-1,placeholder=-1;
            for(int i=0;i<repeated.objects.length;i++){
                int code=original[i]&255;
                if(renewable[code]){repeated.objects[i]=0;depletedSources++;}
                if(code==9&&occupiedSource<0)occupiedSource=i;
                if(code==77)oneOff=i;
                if(code==5&&animal<0)animal=i;
                if(code==96&&placeholder<0)placeholder=i;
            }
            check(depletedSources==1024,"Original map contains 1024 recurring deposits in 36 item categories");
            check(occupiedSource>=0&&oneOff>=0&&animal>=0&&placeholder>=0,
                  "Original world contains building, one-off, animal and placeholder regression sites");
            repeated.objects[occupiedSource]=(byte)100; // Crafted object protects its tile.
            repeated.objects[oneOff]=0;
            repeated.objects[animal]=0;
            repeated.objects[placeholder]=0;
            repeated.hour=3.999f;
            repeated.update(0.1f,0,0);
            int restoredNatural=0;
            for(int i=0;i<repeated.objects.length;i++)
                if(renewable[original[i]&255]&&repeated.objects[i]==original[i])
                    restoredNatural++;
            check(restoredNatural==1023,"At 04:00 all 36 original renewable types return except a protected building site");
            check(repeated.objects[occupiedSource]==(byte)100
                    &&repeated.objects[oneOff]==0
                    &&repeated.objects[animal]==0
                    &&repeated.objects[placeholder]==0,
                  "Daily map refresh preserves built structures, unique pickups, animals and placeholders");
            int atFour=0;
            for(int i=0;i<repeated.objects.length;i++)if(repeated.objectAt(i)==3)atFour++;
            check(atFour==54,"Original 04:00 refresh replenishes all 54 vacant stone source tiles");
            // Collect three stones and check another morning does not duplicate inventory.
            int spent=0;
            for(int i=0;i<repeated.objects.length&&spent<3;i++)
                if(repeated.objectAt(i)==3&&i!=(int)repeated.y*180+(int)repeated.x){
                    repeated.objects[i]=0;spent++;
                }
            check(spent==3,"Stone source depletion can be simulated without moving the player");
            repeated.hour=23.0f;
            int beforeRestDay=repeated.day;
            java.lang.reflect.Method rest=GameEngine.class.getDeclaredMethod("rest",int.class);
            rest.setAccessible(true);
            rest.invoke(repeated,0);
            int afterRest=0;
            for(int i=0;i<repeated.objects.length;i++)if(repeated.objectAt(i)==3)afterRest++;
            check(repeated.day==beforeRestDay+1&&afterRest==54,
                  "Sleeping across 04:00 replenishes stones through the same clock logic");
            // A second sleep stays within the new day; no early refresh is allowed.
            int removed=-1;
            for(int i=0;i<repeated.objects.length;i++)if(repeated.objectAt(i)==3){
                repeated.objects[i]=0;removed=i;break;
            }
            rest.invoke(repeated,0);
            check(removed>=0&&repeated.objects[removed]==0,
                  "Same-day rest before next 04:00 does not refresh resource sites");

        } finally {
            saved.delete();
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
        file.delete();stoneRespawnChecks();harvestChecks();joystickAndCoconuts();System.out.println("Engine checks passed: "+passed);
    }
}
