package hu.crusoe.standalone;

import java.util.Random;

/** Independent touch-game simulation. Coordinates are 720 x 640 logical units. */
public final class HarvestGame {
    public static final int ROD=14, NET=83, BOW=81;
    public final int tool,targetTile,targetItem,reward;
    public final float[] fishX=new float[7],fishY=new float[7],fishSpeed=new float[7];
    public final boolean[] caught=new boolean[7];
    public float hookX=360,hookY=70,netY=-40,animalX=460,animalY=370;
    public float arrowX=54,arrowY=580,arrowVX,arrowVY,angle=55,power=65,elapsed;
    public int attempts,total;
    public boolean hooked,dropping,flying,finished,won;
    private final Random random;
    private final int[] species={40,56,57,58};

    public HarvestGame(int tool,int tile,int item,long seed){
        this.tool=tool;targetTile=tile;targetItem=item;random=new Random(seed);
        reward=tool==ROD?species[random.nextInt(4)]:tool==NET?new int[]{59,60,2}[random.nextInt(3)]:item;
        resetFish();
    }
    private void resetFish(){
        for(int i=0;i<7;i++){
            fishX[i]=60+random.nextFloat()*600;fishY[i]=130+i*64;
            fishSpeed[i]=(i%2==0?1:-1)*(65+random.nextFloat()*65);caught[i]=false;
        }
    }
    public void moveHook(float x,float y){
        if(finished||tool!=ROD||!Float.isFinite(x)||!Float.isFinite(y))return;
        hookX=Math.max(40,Math.min(680,x));hookY=Math.max(35,Math.min(595,y));
        if(hooked&&hookY<=65){finished=won=true;}
    }
    public void action(){
        if(finished)return;
        if(tool==ROD){
            if(hooked)return;
            // The rod has one fish, as in the original hook-and-reel game.
            if(Math.abs(hookX-fishX[0])<42&&Math.abs(hookY-fishY[0])<32)hooked=true;
            else finished=true;
        }else if(tool==NET){
            if(dropping)return;attempts++;netY=-40;dropping=true;
        }else if(tool==BOW&&!flying){
            attempts++;flying=true;arrowX=54;arrowY=580;
            double radians=Math.toRadians(angle);float speed=330+power*5;
            arrowVX=(float)Math.cos(radians)*speed;arrowVY=-(float)Math.sin(radians)*speed;
        }
    }
    public void aim(float degrees,float strength){
        if(!Float.isFinite(degrees)||!Float.isFinite(strength)||flying)return;
        angle=Math.max(10,Math.min(85,degrees));power=Math.max(10,Math.min(100,strength));
    }
    public void update(float delta){
        if(finished||!Float.isFinite(delta)||delta<=0)return;
        float remaining=Math.min(delta,.1f);
        while(remaining>0&&!finished){float dt=Math.min(.01f,remaining);remaining-=dt;step(dt);}
    }
    private void step(float dt){
        elapsed+=dt;
        if(tool==BOW){
            animalX=460+(float)Math.sin(elapsed*.7)*95;
            if(flying){
                arrowVY+=600*dt;arrowX+=arrowVX*dt;arrowY+=arrowVY*dt;
                if(arrowVY>0&&Math.abs(arrowX-animalX)<42&&Math.abs(arrowY-animalY)<35){finished=won=true;}
                else if(arrowY>640||arrowX>760){flying=false;if(attempts>=10)finished=true;}
            }
            return;
        }
        int count=tool==NET?7:1;
        for(int i=0;i<count;i++)if(!caught[i]&&!(hooked&&i==0)){
            fishX[i]+=fishSpeed[i]*dt;
            if(fishX[i]<35){fishX[i]=35;fishSpeed[i]=Math.abs(fishSpeed[i]);}
            if(fishX[i]>685){fishX[i]=685;fishSpeed[i]=-Math.abs(fishSpeed[i]);}
        }
        if(tool==ROD){if(elapsed>=45)finished=true;return;}
        if(dropping){
            netY+=190*dt;
            for(int i=0;i<7;i++)if(!caught[i]&&Math.abs(fishX[i]-360)<100&&Math.abs(fishY[i]-netY)<27){caught[i]=true;total++;}
            if(netY>620){dropping=false;
                if(total>=10){finished=won=true;}else if(attempts>=5)finished=true;else resetFish();
            }
        }
    }
}
