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
        float energy=e.energy; e.update(0,0,0);check(e.energy==energy,"Zero-time frame cannot advance survival state");
        file.delete();System.out.println("Engine checks passed: "+passed);
    }
}
