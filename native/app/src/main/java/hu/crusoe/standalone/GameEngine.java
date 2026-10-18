package hu.crusoe.standalone;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.CRC32;

/** Original native game rules; no Java ME classes, bytecode loader or VM. */
public final class GameEngine {
    public final GameData data;
    public final int[] inventory=new int[120];
    public final byte[] objects;
    public final BitSet seen=new BitSet(13);
    public final ArrayDeque<Integer> route=new ArrayDeque<>();
    public float x,y,hour=8,health=100,food=100,water=100,energy=100,animation;
    public int day=1,direction=0,selected=-1;
    public boolean walking;
    public String notice="A hullámok partra sodortak. Keresd meg a roncsból szétszóródott felszerelést.";
    public int noticeRevision;

    public GameEngine(GameData data){
        this.data=data;objects=data.layers[GameData.OBJECTS].clone();
        int start=findWalkable(10,147);x=start%180+.5f;y=start/180+.5f;
        visit();
    }
    public void say(String text){notice=text;noticeRevision++;}
    public int objectAt(int index){return (objects[index]&255)-1;}
    public int findObject(int item){for(int i=0;i<objects.length;i++)if(objectAt(i)==item)return i;return -1;}
    private int findWalkable(int sx,int sy){
        for(int r=0;r<180;r++)for(int dy=-r;dy<=r;dy++)for(int dx=-r;dx<=r;dx++){
            int tx=sx+dx,ty=sy+dy;if(passable(tx,ty))return ty*180+tx;
        }
        throw new IllegalStateException("No usable starting tile");
    }
    public boolean passable(int tx,int ty){
        if(tx<0||ty<0||tx>=180||ty>=180)return false;
        int t=data.tile(0,tx,ty);if(t==5||t==6||t==12||t==13||t==14)return false;
        int i=ty*180+tx,r=data.layers[4][i]&255;
        boolean rock=r>=50||r==0||r==10||r==11||r==12||r==18||r==20||r==22||r==23||r==24||r==30||r==31||r==32||r==38||r>=41&&r<=47;
        return rock&&data.layers[2][i]==0&&objects[i]==0;
    }
    public void stop(){route.clear();walking=false;}
    public void update(float delta,float joyX,float joyY){
        float dt=Math.max(0,Math.min(.1f,delta));walking=false;
        if(dt==0)return; // A fresh display frame must not cancel a pending route.
        float dx=joyX,dy=joyY;
        if(Math.hypot(dx,dy)>.12){route.clear();}
        else if(!route.isEmpty()){
            while(!route.isEmpty()){
                int next=route.peek();dx=next%180+.5f-x;dy=next/180+.5f-y;
                if(Math.hypot(dx,dy)>=.015)break;
                x=next%180+.5f;y=next/180+.5f;route.remove();dx=dy=0;
            }
        }else{dx=dy=0;}
        float length=(float)Math.hypot(dx,dy);
        if(length>.01){
            float step=Math.min(length,(energy>0?2.8f:1.4f)*dt);
            dx=dx/length*step;dy=dy/length*step;
            if(passable((int)(x+dx),(int)y)){x+=dx;walking|=dx!=0;}
            if(passable((int)x,(int)(y+dy))){y+=dy;walking|=dy!=0;}
            if(!walking)route.clear();
            if(Math.abs(dx)>Math.abs(dy))direction=dx<0?1:2;else direction=dy<0?3:0;
        }
        if(walking)animation+=dt*9;
        hour+=dt/18f;if(hour>=24){hour-=24;day++;}
        food=Math.max(0,food-dt*.035f);water=Math.max(0,water-dt*.05f);
        energy=Math.max(0,energy-dt*(walking?.025f:.005f));
        if(food<=0||water<=0)health=Math.max(0,health-dt*.05f);
        visit();
    }
    private void visit(){
        int code=data.tile(4,(int)x,(int)y);
        for(int i=0;i<data.events.length;i++)if(data.events[i].code==code&&!seen.get(i)){
            seen.set(i);say(data.events[i].title);break;
        }
    }
    public boolean navigate(int tx,int ty){
        route.clear();if(tx<0||ty<0||tx>=180||ty>=180)return false;
        boolean[] goals=new boolean[32400];int target=ty*180+tx;
        if(passable(tx,ty))goals[target]=true;
        else {for(int[] v:new int[][]{{1,0},{-1,0},{0,1},{0,-1}})if(passable(tx+v[0],ty+v[1]))goals[(ty+v[1])*180+tx+v[0]]=true;}
        int start=(int)y*180+(int)x;
        int[] previous=new int[32400],queue=new int[32400];Arrays.fill(previous,-1);
        int head=0,tail=0,found=-1;queue[tail++]=start;previous[start]=start;
        while(head<tail){
            int i=queue[head++];if(goals[i]){found=i;break;}
            int cx=i%180,cy=i/180;
            for(int n=0;n<4;n++){
                int nx=cx+(n==0?1:n==1?-1:0),ny=cy+(n==2?1:n==3?-1:0);
                int next=ny*180+nx;
                if(passable(nx,ny)&&previous[next]<0){previous[next]=i;queue[tail++]=next;}
            }
        }
        if(found<0){say("Innen nincs szabad út oda.");return false;}
        for(int i=found;i!=start;i=previous[i])route.addFirst(i);
        return true;
    }
    public boolean near(int index,float distance){return index>=0&&Math.hypot(index%180+.5-x,index/180+.5-y)<=distance;}
    public int nearest(){
        int best=-1;double distance=1.8;
        for(int yy=Math.max(0,(int)y-2);yy<=Math.min(179,(int)y+2);yy++)for(int xx=Math.max(0,(int)x-2);xx<=Math.min(179,(int)x+2);xx++){
            int i=yy*180+xx;if(objects[i]==0)continue;
            double d=Math.hypot(xx+.5-x,yy+.5-y);if(d<distance){distance=d;best=i;}
        }
        return best;
    }
    public String interact(){
        int index=selected>=0&&objects[selected]!=0?selected:nearest();
        if(index<0)return "Nincs elérhető tárgy a közelben.";
        if(!near(index,1.8f))return "Menj közelebb: "+data.items[objectAt(index)].name;
        int id=objectAt(index);
        if(id==17||id==9||id==10)return rest(index);
        if(id==18||id==84)return "Itt építhetsz: "+data.items[id==18?19:94].name+". Nyisd meg a Készítés menüt.";
        if(id==19||id==94)return "A hajózás ebben a próbaváltozatban még nem érhető el.";
        GameData.Item item=data.items[id];
        int tool=item.attributes[0];
        if(tool<0)return "Ehhez külön művelet kell, amely ebben a próbaváltozatban még nem érhető el.";
        if(tool>0&&inventory[tool]==0)return "Szükséges eszköz: "+data.items[tool].name;
        objects[index]=0;inventory[id]++;energy=Math.max(0,energy+Math.min(0,item.attributes[6]));
        selected=-1;return "+1 "+item.name;
    }
    private String rest(int index){
        hour+=8;if(hour>=24){hour-=24;day++;}energy=100;
        food=Math.max(0,food-8);water=Math.max(0,water-12);return "Megpihentél a táborban. Az erőd helyreállt.";
    }
    public boolean contextAvailable(int context){
        if(context<0)return true;
        if((context==9||context==10)&&inventory[context]>0)return true;
        for(int yy=Math.max(0,(int)y-2);yy<=Math.min(179,(int)y+2);yy++)for(int xx=Math.max(0,(int)x-2);xx<=Math.min(179,(int)x+2);xx++){
            int i=yy*180+xx;if(objectAt(i)==context&&near(i,2.1f))return true;
        }
        return false;
    }
    private int availableTool(int id){
        int amount=inventory[id];
        for(int yy=Math.max(0,(int)y-2);yy<=Math.min(179,(int)y+2);yy++)for(int xx=Math.max(0,(int)x-2);xx<=Math.min(179,(int)x+2);xx++){
            int i=yy*180+xx;if(objectAt(i)==id&&near(i,2.1f))amount++;
        }
        return amount;
    }
    public String missing(GameData.Recipe recipe){
        ArrayList<String> parts=new ArrayList<>();
        if(!contextAvailable(recipe.context))parts.add("Hely: "+data.items[recipe.context].name);
        // Reserve consumables even when an ingredient also serves as a tool.
        int[] consumed=new int[120];for(int[] p:recipe.ingredients)consumed[p[0]]+=p[1];
        for(int id=0;id<120;id++)if(inventory[id]<consumed[id])parts.add((consumed[id]-inventory[id])+" × "+data.items[id].name);
        for(int[] p:recipe.tools)if(availableTool(p[0])-consumed[p[0]]<p[1])parts.add("Eszköz: "+data.items[p[0]].name);
        return String.join(", ",parts);
    }
    public String craft(int id){
        GameData.Recipe r=data.recipe(id);if(r==null)return "Ehhez nincs recept.";
        String need=missing(r);if(!need.isEmpty())return "Hiányzik: "+need;
        int buildAt=-1;
        if(id==9||id==10||id==19||id==94||id==15||id==16||id==34||id==50||id==60||id==82||id==85||id==88||id==117){
            if(id==19||id==94){
                for(int i=0;i<objects.length;i++)if(objectAt(i)==r.context&&near(i,2.1f)){buildAt=i;break;}
            }else{
                for(int yy=Math.max(0,(int)y-1);yy<=Math.min(179,(int)y+1);yy++)for(int xx=Math.max(0,(int)x-1);xx<=Math.min(179,(int)x+1);xx++){
                    if(xx==(int)x&&yy==(int)y)continue;
                    if(passable(xx,yy))buildAt=yy*180+xx;
                }
            }
            if(buildAt<0)return "Nincs szabad építési hely a közelben.";
        }
        for(int[] p:r.ingredients)inventory[p[0]]-=p[1];
        if(buildAt>=0)objects[buildAt]=(byte)(id+1);else inventory[id]++;
        return "Elkészült: "+data.items[id].name;
    }
    public String eat(int id){
        if(id<0||id>=120||inventory[id]<=0)return "Nincs nálad ilyen tárgy.";
        GameData.Item item=data.items[id];
        if(item.attributes[1]<0)return "Ez nem fogyasztható.";
        int tool=item.attributes[1];if(tool>0&&availableTool(tool)==0)return "Szükséges: "+data.items[tool].name;
        inventory[id]--;food=clamp(food+Math.max(0,item.attributes[3]));water=clamp(water+Math.max(0,item.attributes[4]));
        health=clamp(health+item.attributes[6]);return "Elfogyasztottad: "+item.name;
    }
    private static float clamp(float n){return Math.max(0,Math.min(100,n));}
    public void save(File file)throws IOException {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
        out.writeInt(0x43535631);out.writeUTF(data.sourceHash);out.writeFloat(x);out.writeFloat(y);out.writeFloat(hour);
        out.writeInt(day);for(float v:new float[]{health,food,water,energy})out.writeFloat(v);
        for(int q:inventory)out.writeInt(q);out.write(objects);out.writeLong(seen.toLongArray().length==0?0:seen.toLongArray()[0]);out.flush();
        byte[] payload=bytes.toByteArray();CRC32 crc=new CRC32();crc.update(payload);
        File temp=new File(file.getPath()+".tmp");
        try(DataOutputStream target=new DataOutputStream(new FileOutputStream(temp))){target.write(payload);target.writeLong(crc.getValue());}
        Files.move(temp.toPath(),file.toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
    }
    public void load(File file)throws IOException {
        byte[] bytes=Files.readAllBytes(file.toPath());if(bytes.length<100||bytes.length>100000)throw new IOException("Invalid save size");
        int length=bytes.length-8;CRC32 crc=new CRC32();crc.update(bytes,0,length);
        if(new DataInputStream(new ByteArrayInputStream(bytes,length,8)).readLong()!=crc.getValue())throw new IOException("Save checksum mismatch");
        DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes,0,length));
        if(in.readInt()!=0x43535631||!in.readUTF().equals(data.sourceHash))throw new IOException("Unsupported save");
        float nx=in.readFloat(),ny=in.readFloat(),nh=in.readFloat();int nd=in.readInt();
        float[] stats=new float[4];for(int i=0;i<4;i++){stats[i]=in.readFloat();if(!Float.isFinite(stats[i])||stats[i]<0||stats[i]>100)throw new IOException("Invalid stats");}
        int[] inv=new int[120];for(int i=0;i<120;i++){inv[i]=in.readInt();if(inv[i]<0||inv[i]>1000000)throw new IOException("Invalid item amount");}
        byte[] world=new byte[32400];in.readFully(world);long flags=in.readLong();
        if(in.read()!=-1||!Float.isFinite(nx)||!Float.isFinite(ny)||!Float.isFinite(nh)||nx<0||ny<0||nx>=180||ny>=180||nh<0||nh>=24||nd<1)throw new IOException("Invalid saved position/time");
        for(byte b:world)if((b&255)>120)throw new IOException("Invalid world item");
        // Apply only after the complete save passed validation.
        x=nx;y=ny;hour=nh;day=nd;health=stats[0];food=stats[1];water=stats[2];energy=stats[3];
        System.arraycopy(inv,0,inventory,0,120);System.arraycopy(world,0,objects,0,32400);
        seen.clear();seen.or(BitSet.valueOf(new long[]{flags}));selected=-1;stop();say("A mentést betöltöttem.");
    }
}
