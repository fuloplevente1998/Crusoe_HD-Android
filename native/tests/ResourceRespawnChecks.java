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
        check(!ResourceRespawn.containsStone(world),"Depleted world has no stones");
        int restored=ResourceRespawn.restoreNearestStone(original,world,10.5f,10.5f);
        check(restored==far,"Skip player's tile and occupied original stone site");
        check(world[occupied]==99&&world[other]==9,"Preserve crafted and unrelated objects");
        check(world[near]==0&&world[far]==4,"Restore one and only one natural stone");
        check(ResourceRespawn.containsStone(world),"Restored stone is collectible");
        int next=ResourceRespawn.restoreNearestStone(original,world,10.5f,10.5f);
        check(next==-1,"No more eligible stone deposits when remaining sites are protected");
        world[far]=0;
        restored=ResourceRespawn.restoreNearestStone(original,world,10.5f,10.5f);
        check(restored==far,"Collected stone can regrow at its original location");
        byte[] snapshot=world.clone();
        check(ResourceRespawn.restoreNearestStone(new byte[32400],world,10.5f,10.5f)==-1
                &&Arrays.equals(snapshot,world),"Never introduce invented spawn locations");
        boolean thrown=false;
        try{ResourceRespawn.containsStone(new byte[9]);}catch(IllegalArgumentException expected){thrown=true;}
        check(thrown,"Reject malformed world maps instead of corrupting saves");
        System.out.println("Stone recovery unit tests passed: "+passed);
    }
}
