package hu.crusoe.standalone;

import java.util.Random;

/** Independent touch-game simulation. Coordinates are 720 x 640 logical units. */
public final class HarvestGame {
    public static final int COCONUT=3, ROD=14, NET=83, BOW=81;
    public final int tool,targetTile,targetItem,reward;
    public final float[] fishX=new float[7],fishY=new float[7],fishSpeed=new float[7];
    public final boolean[] caught=new boolean[7];
    public float hookX=360,hookY=70,netY=-40,animalX=460,animalY=370;
    public float arrowX=54,arrowY=580,arrowVX,arrowVY,angle=70,power=40,elapsed;
    public final float[] coconutX=new float[6],coconutY=new float[6],coconutFall=new float[6],coconutAge=new float[6];
    public final int[] coconutHits=new int[6];
    private final boolean[] hitThisShot=new boolean[6];
    public int attempts,total;
    public boolean hooked,dropping,flying,finished,won;
    private final Random random;
    private final int[] species={40,56,57,58};

    public HarvestGame(int tool,int tile,int item,long seed){
        this.tool=tool;targetTile=tile;targetItem=item;random=new Random(seed);
        reward=tool==ROD?species[random.nextInt(4)]:tool==NET?new int[]{59,60,2}[random.nextInt(3)]:tool==COCONUT?1:item;
        resetFish();
        for(int i=0;i<6;i++){coconutX[i]=240+i*70+random.nextFloat()*12-6;coconutY[i]=180+(i%2)*45+random.nextFloat()*24;}
    }
    public boolean isBallistic(){return tool==BOW||tool==COCONUT;}
    /** Continuous original-style directional input; releasing the stick retains the aim. */
    public void steer(float horizontal,float vertical,float delta){
        if(finished||!Float.isFinite(horizontal)||!Float.isFinite(vertical)||!Float.isFinite(delta)||delta<=0)return;
        float length=(float)Math.hypot(horizontal,vertical);if(length<.12f)return;
        float norm=Math.max(1,length),dt=Math.min(.1f,delta);horizontal/=norm;vertical/=norm;
        if(isBallistic())aim(angle+horizontal*50*dt,power-vertical*36*dt);
        else if(tool==ROD)moveHook(hookX+horizontal*250*dt,hookY+vertical*250*dt);
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
        }else if(isBallistic()&&!flying&&attempts<10&&!(tool==COCONUT&&total>=5)){
            attempts++;flying=true;arrowX=54;arrowY=580;
            java.util.Arrays.fill(hitThisShot,false);
            double radians=Math.toRadians(angle);float speed=330+power*5;
            arrowVX=(float)Math.cos(radians)*speed;arrowVY=-(float)Math.sin(radians)*speed;
        }
    }
    public void aim(float degrees,float strength){
        if(!Float.isFinite(degrees)||!Float.isFinite(strength)||flying)return;
        if(finished)return;
        angle=Math.max(10,Math.min(85,degrees));power=Math.max(40,Math.min(100,strength));
    }
    public void update(float delta){
        if(finished||!Float.isFinite(delta)||delta<=0)return;
        float remaining=Math.min(delta,.1f);
        while(remaining>0&&!finished){float dt=Math.min(.01f,remaining);remaining-=dt;step(dt);}
    }
    private void step(float dt){
        elapsed+=dt;
        if(isBallistic()){
            if(tool==BOW)animalX=460+(float)Math.sin(elapsed*.7)*95;
            else updateCoconuts(dt);
            if(flying){
                arrowVY+=600*dt;arrowX+=arrowVX*dt;arrowY+=arrowVY*dt;
                if(tool==BOW&&arrowVY>0&&Math.abs(arrowX-animalX)<42&&Math.abs(arrowY-animalY)<35){finished=won=true;}
                else if(tool==COCONUT)for(int i=0;i<6;i++){
                    if(coconutHits[i]<2&&!hitThisShot[i]&&Math.hypot(arrowX-coconutX[i],arrowY-coconutY[i])<23){
                        hitThisShot[i]=true;coconutAge[i]=0;if(++coconutHits[i]==2)total++;
                    }
                }
                if(arrowY>640||arrowX>760){flying=false;if(attempts>=10&&!(tool==COCONUT&&total>=5))finished=true;}
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
    private void updateCoconuts(float dt){
        boolean falling=false;
        for(int i=0;i<6;i++){
            if(coconutHits[i]==1){coconutAge[i]+=dt;if(coconutAge[i]>11){coconutHits[i]=0;coconutAge[i]=0;}}
            if(coconutHits[i]==2){coconutFall[i]=Math.min(580-coconutY[i],coconutFall[i]+230*dt);if(coconutY[i]+coconutFall[i]<580)falling=true;}
        }
        if(total>=5&&!falling){finished=won=true;}
    }
}
