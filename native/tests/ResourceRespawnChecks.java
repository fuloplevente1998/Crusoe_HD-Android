import hu.crusoe.standalone.ResourceRespawn;
import java.util.Arrays;

public final class ResourceRespawnChecks {
    static int passed;
    static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        passed++;
        System.out.println("PASS " + message);
    }
    static int cell(int x,int y){return y*180+x;}
    public static void main(String[] args) {
        byte[] original=new byte[32400],world=new byte[32400];
        int near=cell(10,10),occupied=cell(11,10),far=cell(12,10),other=cell(50,50);
        original[near]=4;original[occupied]=4;original[far]=4;
        original[other]=9;
        world[occupied]=99;world[other]=9; // crafted object and unique item
        boolean[] reachable=new boolean[32400];Arrays.fill(reachable,true);
        check(!ResourceRespawn.containsStone(world),"Depleted world has no stones");
        int restored=ResourceRespawn.restoreNearestStone(original,world,10.5f,10.5f,reachable);
        check(restored==far,"Skip player's tile and occupied original stone site");
        check(world[occupied]==99&&world[other]==9,"Preserve crafted and unrelated objects");
        check(world[near]==0&&world[far]==4,"Restore one and only one natural stone");
        check(ResourceRespawn.containsStone(world),"Restored stone is collectible");
        check(ResourceRespawn.hasApproachableStone(world,reachable),
              "A stone beside a reachable walking tile is usable");
        boolean[] isolated=new boolean[32400];
        check(!ResourceRespawn.hasApproachableStone(world,isolated),
              "Global stone count does not prove local access");
        isolated[far-1]=true;
        check(ResourceRespawn.hasApproachableStone(world,isolated),
              "One reachable neighbor makes a stone approachable");
        int next=ResourceRespawn.restoreNearestStone(original,world,10.5f,10.5f,reachable);
        check(next==-1,"No more eligible stone deposits when remaining sites are protected");
        world[far]=0;reachable[far]=false;
        check(ResourceRespawn.restoreNearestStone(original,world,10.5f,10.5f,reachable)==-1,
              "Unreachable original stone must not be selected");
        reachable[far]=true;
        world[far]=0;
        restored=ResourceRespawn.restoreNearestStone(original,world,10.5f,10.5f,reachable);
        check(restored==far,"Collected stone can regrow at its original location");
        byte[] snapshot=world.clone();
        check(ResourceRespawn.restoreNearestStone(new byte[32400],world,10.5f,10.5f,reachable)==-1
                &&Arrays.equals(snapshot,world),"Never introduce invented spawn locations");
        world[far]=0;
        int morning=ResourceRespawn.restoreOriginalResources(original,world,10.5f,10.5f);
        check(morning==1&&world[far]==4&&world[near]==0,
              "04:00 refresh restores only empty original stone tiles and avoids player's cell");
        check(world[occupied]==99&&world[other]==9,
              "04:00 refresh never replaces crafted or story objects");
        check(ResourceRespawn.restoreOriginalResources(original,world,10.5f,10.5f)==0,
              "Repeated 04:00 refresh is idempotent with no inventory duplication");
        // Multiple original resources refresh; one-off, animal and placeholder
        // entries do not, even when the map is reloaded at 04:00.
        byte[] full=new byte[32400],current=new byte[32400];
        full[cell(1,1)]=9;full[cell(2,1)]=9; // two branch sources
        full[cell(3,1)]=7;full[cell(4,1)]=7; // vine
        full[cell(5,1)]=29;full[cell(6,1)]=29; // berries
        full[cell(7,1)]=91;full[cell(8,1)]=91; // earthworms
        full[cell(9,1)]=65;full[cell(10,1)]=65; // shells
        full[cell(11,1)]=77; // unique knife
        full[cell(12,1)]=5;full[cell(13,1)]=5; // dynamic animals
        full[cell(14,1)]=96;full[cell(15,1)]=96; // placeholders
        current[cell(2,1)]=99; // built item occupies one original branch tile
        int regrown=ResourceRespawn.restoreOriginalResources(full,current,80.5f,80.5f);
        check(regrown==9,"04:00 restores branches, vines, berries, worms and shells");
        check(current[cell(2,1)]==99&&current[cell(11,1)]==0,
              "Structures and unique inventory/story pickups remain untouched");
        check(current[cell(12,1)]==0&&current[cell(13,1)]==0
                &&current[cell(14,1)]==0,
              "The original animal filter and native placeholder exception remain intact");
        check(ResourceRespawn.restoreOriginalResources(full,current,80.5f,80.5f)==0,
              "Full natural-resource refresh cannot duplicate surviving items");
        current[cell(4,1)]=0;
        check(ResourceRespawn.restoreOriginalResources(full,current,4.5f,1.5f)==0,
              "Never spawn directly beneath the player");
        check(ResourceRespawn.restoreOriginalResources(full,current,80.5f,80.5f)==1
                &&current[cell(4,1)]==7,
              "Missed vine reappears on the next safe 04:00 refresh");
        boolean thrown=false;
        try{ResourceRespawn.containsStone(new byte[9]);}catch(IllegalArgumentException expected){thrown=true;}
        check(thrown,"Reject malformed world maps instead of corrupting saves");
        System.out.println("Stone recovery unit tests passed: "+passed);
    }
}
