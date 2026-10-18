package hu.crusoe.standalone;

import android.content.Context;
import android.graphics.*;
import android.view.*;
import java.io.*;
import java.util.*;

/** GPU-backed native Android scene renderer. World coordinates are tile units. */
public final class GameView extends View {
    public static final int TITLE=0,WORLD=1,MAP=2,HARVEST=3;
    private static final float TILE=48;
    private static final int INK=0xff102e2e,GOLD=0xffedc37e,WHITE=0xfff6efd9;
    private static final int[] PALETTE={0xffe6d798,0xff6e9d41,0xffb77d4a,0xff777b3b,0xff78d8c2,0xff33bcd3,0xff1789bf,0xff73715a,0xffb5a26c,0xff3e732b,0xff8a603b,0xff2c502a,0xff237b85,0xff1a607a,0xff10436c,0xff4b4030};
    public final GameActivity activity;
    public final ItemArt itemArt;
    public int screen=TITLE;
    public long lastFrame;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Rect source=new Rect();
    private final RectF dest=new RectF();
    private final Bitmap terrain,objects,robinson,title,map,fauna;
    private HarvestGame shownHarvest;
    private String harvestResult;
    private float harvestTop,harvestScale;
    private int hookPointer=-1;
    private final Paint[] terrainPaints=new Paint[16];
    private float logicalHeight=1280,scale=1,cameraX,cameraY,joyX,joyY,joyOriginX,joyOriginY;
    private int joyPointer=-1,tapPointer=-1;
    private float tapStartX,tapStartY,tapX,tapY;
    private float saveSeconds;
    private final Typeface serif=Typeface.create("serif",Typeface.BOLD);
    private final Typeface sans=Typeface.create("sans-serif-medium",Typeface.NORMAL);

    public GameView(GameActivity activity){
        super(activity);this.activity=activity;setFocusable(true);setKeepScreenOn(true);
        terrain=load("terrain.png");objects=load("objects.png");robinson=load("robinson.png");title=load("title.png");fauna=load("fauna.png");
        itemArt=new ItemArt(activity,objects,fauna);
        int[] pixels=new int[32400];for(int i=0;i<pixels.length;i++)pixels[i]=PALETTE[activity.data.layers[0][i]&15];
        map=Bitmap.createBitmap(pixels,180,180,Bitmap.Config.ARGB_8888);
        Bitmap mask=Bitmap.createBitmap(48,48,Bitmap.Config.ARGB_8888);
        Paint maskPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
        maskPaint.setShader(new RadialGradient(24,24,34,new int[]{0xbaffffff,0x00ffffff},new float[]{.2f,1},Shader.TileMode.CLAMP));
        new Canvas(mask).drawRect(0,0,48,48,maskPaint);
        for(int i=0;i<16;i++){
            int l=(i%4)*terrain.getWidth()/4,t=(i/4)*terrain.getHeight()/4;
            int r=((i%4)+1)*terrain.getWidth()/4,b=((i/4)+1)*terrain.getHeight()/4;
            Bitmap tile=Bitmap.createBitmap(terrain,l,t,r-l,b-t);
            BitmapShader detail=new BitmapShader(tile,Shader.TileMode.REPEAT,Shader.TileMode.REPEAT);
            Matrix matrix=new Matrix();matrix.setScale(.6f,.6f);detail.setLocalMatrix(matrix);
            BitmapShader fade=new BitmapShader(mask,Shader.TileMode.REPEAT,Shader.TileMode.REPEAT);
            Paint tp=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);tp.setAlpha(125);
            tp.setShader(new ComposeShader(detail,fade,PorterDuff.Mode.DST_IN));terrainPaints[i]=tp;
        }
        terrain.recycle(); // Cropped shader textures own their pixels; release the unused atlas.
        if(android.os.Build.VERSION.SDK_INT<28)setLayerType(View.LAYER_TYPE_SOFTWARE,null);
        setOnApplyWindowInsetsListener((v,insets)->{setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;});
    }
    private Bitmap load(String name){try(InputStream in=getContext().getAssets().open("art/"+name)){BitmapFactory.Options o=new BitmapFactory.Options();o.inScaled=false;return BitmapFactory.decodeStream(in,null,o);}catch(IOException e){throw new IllegalStateException(e);}}
    public void centerCamera(){cameraX=activity.engine.x*TILE;cameraY=activity.engine.y*TILE;lastFrame=0;}
    public void resetInput(){joyPointer=tapPointer=hookPointer=-1;joyX=joyY=0;}
    private void text(Canvas c,String s,float x,float y,float size,int color){p.setShader(null);p.setColor(color);p.setTypeface(sans);p.setTextSize(size);c.drawText(s,x,y,p);}
    private void centered(Canvas c,String s,float x,float y,float size,int color){p.setTypeface(sans);p.setTextSize(size);text(c,s,x-p.measureText(s)/2,y,size,color);}
    private void box(Canvas c,float l,float t,float r,float b,float radius,int color){p.setShader(null);p.setColor(color);dest.set(l,t,r,b);c.drawRoundRect(dest,radius,radius,p);}
    private void sprite(Canvas c,Bitmap atlas,int cell,float x,float y,float width,float height){
        sprite(c,atlas,cell,x,y,width,height,255);
    }
    private void sprite(Canvas c,Bitmap atlas,int cell,float x,float y,float width,float height,int alpha){
        source.set((cell%4)*atlas.getWidth()/4,(cell/4)*atlas.getHeight()/4,((cell%4)+1)*atlas.getWidth()/4,((cell/4)+1)*atlas.getHeight()/4);
        dest.set(x-width/2,y-height,width/2+x,y);p.setColor(Color.WHITE);p.setAlpha(alpha);p.setShader(null);c.drawBitmap(atlas,source,dest,p);p.setAlpha(255);
    }
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        int width=getWidth()-getPaddingLeft()-getPaddingRight(),height=getHeight()-getPaddingTop()-getPaddingBottom();if(width<=0||height<=0)return;
        scale=width/720f;logicalHeight=height/scale;
        canvas.save();canvas.translate(getPaddingLeft(),getPaddingTop());canvas.scale(scale,scale);canvas.clipRect(0,0,720,logicalHeight);
        if(screen==TITLE)drawTitle(canvas);else if(screen==MAP)drawMap(canvas);else if(screen==HARVEST)drawHarvest(canvas);else {
            long now=System.nanoTime();float dt=lastFrame==0?0:Math.min(.1f,(now-lastFrame)/1e9f);lastFrame=now;
            if(!activity.paused){activity.engine.update(dt,joyX,joyY);saveSeconds+=dt;if(saveSeconds>20){activity.save();saveSeconds=0;}}
            float mix=1-(float)Math.exp(-dt*9);cameraX+=(activity.engine.x*TILE-cameraX)*mix;cameraY+=(activity.engine.y*TILE-cameraY)*mix;
            drawWorld(canvas);drawHUD(canvas);
        }
        canvas.restore();if((screen==WORLD||screen==HARVEST)&&!activity.paused)postInvalidateOnAnimation();
    }
    private void drawTitle(Canvas c){
        float h=logicalHeight;
        p.setColor(Color.WHITE);float imageScale=Math.max(720f/title.getWidth(),h/title.getHeight());
        float w=title.getWidth()*imageScale;float left=(720-w)*.72f;dest.set(left,0,left+w,h);c.drawBitmap(title,null,dest,p);
        p.setShader(new LinearGradient(0,h*.18f,0,h,new int[]{0x00102e2e,0xe6102e2e,INK},new float[]{0,.65f,1},Shader.TileMode.CLAMP));c.drawRect(0,0,720,h,p);p.setShader(null);
        text(c,"AZ ISMERETLEN SZIGET",44,h*.49f,19,GOLD);
        p.setTypeface(serif);p.setTextSize(65);p.setColor(WHITE);c.drawText("ROBINSON",40,h*.49f+76,p);c.drawText("CRUSOE",40,h*.49f+146,p);
        text(c,"HAJÓTÖRÖTT",44,h*.49f+188,23,GOLD);
        text(c,"Egy sziget. Egy esély a túlélésre.",44,h*.49f+236,23,WHITE);
        float first=h-243;box(c,40,first,680,first+78,17,GOLD);centered(c,"ÚJ KALAND",360,first+49,25,INK);
        box(c,40,first+94,680,first+166,17,0x884b756c);centered(c,"FOLYTATÁS",360,first+139,24,activity.hasSave()?WHITE:0xff83978e);
        centered(c,"Fejlesztői változat · 0.6",360,h-27,18,0xffa6beb3);
    }
    private void drawWorld(Canvas c){
        GameEngine e=activity.engine;
        float ox=360-cameraX,oy=logicalHeight*.49f-cameraY;
        c.drawColor(0xff1a8caf);
        int left=Math.max(0,(int)Math.floor(-ox/TILE)),right=Math.min(179,(int)Math.ceil((720-ox)/TILE));
        int top=Math.max(0,(int)Math.floor(-oy/TILE)-3),bottom=Math.min(179,(int)Math.ceil((logicalHeight-oy)/TILE)+3);
        p.setColor(Color.WHITE);p.setShader(null);
        // Bilinear color-map boundaries plus faded, world-aligned detail textures:
        // no giant full-resolution island bitmap and no hard checkerboard seams.
        c.save();c.translate(ox,oy);
        dest.set(0,0,180*TILE,180*TILE);c.drawBitmap(map,null,dest,p);
        for(int yy=top;yy<=bottom;yy++)for(int xx=left;xx<=right;xx++){
            int cell=activity.data.tile(0,xx,yy)&15;
            c.drawRect(xx*TILE,yy*TILE,(xx+1)*TILE,(yy+1)*TILE,terrainPaints[cell]);
        }
        c.restore();
        // A single Y-ordered pass draws only visible tiles and the player.
        boolean playerDrawn=false;
        for(int yy=top;yy<=bottom;yy++){
            if(!playerDrawn&&yy+1>e.y){drawPlayer(c,ox,oy);playerDrawn=true;}
            for(int xx=Math.max(0,left-3);xx<=Math.min(179,right+3);xx++){
                int i=yy*180+xx;float px=ox+(xx+.5f)*TILE,py=oy+(yy+1)*TILE;
                int decoration=activity.data.layers[2][i]&255;
                if(decoration>0){boolean palm=decoration==5||decoration==13||decoration==18||decoration==21||decoration==22||decoration==49;
                    int cell=decoration==68||decoration==69?9:
                        decoration==50||decoration==51||decoration==52?12:
                        decoration==55||decoration==58||decoration==65?1:
                        decoration==56||decoration==57||decoration==62||decoration==63?2:
                        palm?0:decoration==35?8:4;
                    float size=palm?148:decoration==35?110:decoration==68||decoration==69?108:76;
                    float height=size*(palm?1.2f:1),heroX=ox+e.x*TILE,heroY=oy+e.y*TILE+4;
                    boolean coversPlayer=size>=100&&py>heroY&&py-height<heroY-24&&Math.abs(px-heroX)<size*.44f;
                    sprite(c,objects,cell,px,py,size,height,coversPlayer?90:255);
                }
                int rock=activity.data.layers[4][i]&255;
                if(rock>0&&rock<50&&!e.passable(xx,yy)&&decoration==0&&e.objects[i]==0)sprite(c,objects,1,px,py,65,60);
                int id=e.objectAt(i);
                if(id>=0){
                    float size=sizeFor(id);
                    if(e.selected==i){p.setColor(0x88fff0b7);c.drawOval(px-25,py-17,px+25,py+4,p);}
                    itemArt.draw(c,id,px,py,size,size);
                    if(id==17||id==18||id==84||id==76){String name=activity.data.items[id].name;float half=id==76?38:110;
                        box(c,px-half,py-size-31,px+half,py-size-1,10,0xdd102e2e);centered(c,name,px,py-size-10,17,GOLD);}
                }
            }
        }
        if(!playerDrawn)drawPlayer(c,ox,oy);
        // Night is a display effect only; its mechanics are not yet the original weather system.
        if(e.hour<6||e.hour>19){p.setColor(0x5513234a);c.drawRect(0,0,720,logicalHeight,p);}
        if(!e.route.isEmpty()){
            int target=e.route.peekLast();float px=ox+(target%180+.5f)*TILE,py=oy+(target/180+.5f)*TILE;
            p.setColor(GOLD);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);c.drawCircle(px,py,12,p);p.setStyle(Paint.Style.FILL);
        }
    }
    private void drawPlayer(Canvas c,float ox,float oy){
        GameEngine e=activity.engine;int frame=e.walking?((int)e.animation)%4:0;
        sprite(c,robinson,e.direction*4+frame,ox+e.x*TILE,oy+e.y*TILE+4,106,106);
    }
    private int faunaFor(int id){return switch(id){
        case 40->0;case 56->1;case 57->2;case 58->3;case 2->4;case 59->5;case 4->6;case 41->7;
        case 42->8;case 43->9;case 44->10;case 46->11;case 47->12;case 48,49->13;
        case 51,52,53,62,63->14;case 61->15;default->-1;
    };}
    private float sizeFor(int id){
        if(id==76)return 94;
        if(id>=64&&id<=75||id==90)return 72;
        if(id==0||id>=24&&id<=33||id>=77&&id<=80||id==86||id==89||id==96||id==97)return 82;
        if(id==18||id==19)return 285;if(id==9||id==10)return 195;if(id==17)return 122;if(id==38)return 145;
        if(id==84||id==94||id==93)return 150;if(id==6)return 115;if(id==16)return 120;
        if(id>=40&&id<=63)return 64;return 58;
    }
    private void drawHUD(Canvas c){
        GameEngine e=activity.engine;float h=logicalHeight;
        box(c,16,14,704,163,20,0xef102e2e);
        String zone=e.y>120?"DÉLI PART":e.x>115?"KELETI RENGETEG":"A SZIGET BELSEJE";
        text(c,zone,36,47,17,GOLD);text(c,e.day+". nap  ·  "+String.format(java.util.Locale.ROOT,"%02d:%02d",(int)e.hour,(int)((e.hour%1)*60)),36,82,24,WHITE);
        box(c,626,29,684,89,16,0xff33544b);centered(c,"Ⅱ",655,67,25,WHITE);
        String[] labels={"ÉLET","ÉLELEM","VÍZ","ERŐ"};float[] values={e.health,e.food,e.water,e.energy};int[] colors={0xffdd8c75,0xffd9b76b,0xff70c6d1,0xff9ab988};
        for(int i=0;i<4;i++){float x=36+i*168;text(c,labels[i],x,117,14,0xffced9ca);box(c,x,129,x+145,138,4,0xff2d5049);box(c,x,129,x+145*values[i]/100f,138,4,colors[i]);}
        int target=e.selected>=0&&e.objects[e.selected]!=0?e.selected:e.nearest();
        if(e.coconutPalm(e.selected))target=e.selected;
        if(target>=0){String name=e.coconutPalm(target)?"Kókuszpálma":activity.data.items[e.objectAt(target)].name;box(c,194,h-295,694,h-246,12,0xd9102e2e);centered(c,name,444,h-263,20,WHITE);}
        if(e.notice!=null&&!e.notice.isEmpty()){
            box(c,22,178,698,253,13,0xc9102e2e);wrapped(c,e.notice,38,205,644,22,WHITE,2);
        }
        // Joystick uses its own pointer; a second finger can press the action button.
        float jx=112,jy=h-161;box(c,27,jy-82,197,jy+82,70,0x55102e2e);p.setColor(0x88ebd4a1);c.drawCircle(jx,jy,58,p);
        p.setColor(0xffe8d4a8);c.drawCircle(jx+joyX*34,jy+joyY*34,28,p);
        box(c,480,h-217,694,h-123,24,GOLD);
        centered(c,"MŰVELET",587,h-163,22,INK);centered(c,"közeli tárgy",587,h-139,14,0xff3e5543);
        box(c,0,h-78,720,h,0,INK);
        String[] nav={"TARISZNYA","KÉSZÍTÉS","TÉRKÉP","NAPLÓ"};
        for(int i=0;i<4;i++){p.setColor(0xff466358);c.drawLine(i*180,h-62,i*180,h-18,p);centered(c,nav[i],i*180+90,h-32,23,i==2?GOLD:WHITE);}
    }
    private void wrapped(Canvas c,String text,float x,float y,float width,float size,int color,int maxLines){
        p.setTextSize(size);String line="";int lines=0;
        for(String word:text.split(" ")){
            String candidate=line.isEmpty()?word:line+" "+word;
            if(p.measureText(candidate)>width&&!line.isEmpty()){text(c,line,x,y,size,color);y+=size+6;lines++;line=word;if(lines>=maxLines)return;}else line=candidate;
        }
        if(lines<maxLines)text(c,line,x,y,size,color);
    }
    private void drawMap(Canvas c){
        c.drawColor(INK);float side=Math.min(672,logicalHeight-265),left=(720-side)/2,top=130;
        text(c,"A SZIGET",28,61,30,GOLD);text(c,"VISSZA",580,60,20,WHITE);
        dest.set(left,top,left+side,top+side);p.setColor(Color.WHITE);c.drawBitmap(map,null,dest,p);
        GameEngine e=activity.engine;
        for(int id:new int[]{17,18,84}){int i=e.findObject(id);if(i<0)continue;float px=left+(i%180+.5f)/180*side,py=top+(i/180+.5f)/180*side;
            p.setColor(GOLD);c.drawCircle(px,py,6,p);text(c,activity.data.items[id].name,Math.min(left+side-170,px+10),py-9,16,WHITE);
        }
        float px=left+e.x/180*side,py=top+e.y/180*side;p.setColor(WHITE);c.drawCircle(px,py,7,p);p.setColor(0xffcf785b);c.drawCircle(px,py,4,p);
        wrapped(c,"A fehér pont Robinson helye. Bökj a térképre: ha van szabad út, elindulsz a kiválasztott hely felé.",28,top+side+47,660,21,WHITE,3);
    }
    private void drawHarvest(Canvas c){
        HarvestGame active=activity.engine.harvest;
        if(active!=null&&active!=shownHarvest){shownHarvest=active;harvestResult=null;}
        HarvestGame g=shownHarvest;
        if(g==null){screen=WORLD;lastFrame=0;return;}
        long now=System.nanoTime();float dt=lastFrame==0?0:Math.min(.1f,(now-lastFrame)/1e9f);lastFrame=now;
        if(!activity.paused){g.steer(joyX,joyY,dt);g.update(dt);}
        if(g.finished&&harvestResult==null){resetInput();harvestResult=activity.engine.finishHarvest();activity.engine.say(harvestResult);activity.save();}
        boolean coconut=g.tool==HarvestGame.COCONUT,ballistic=g.isBallistic(),net=g.tool==HarvestGame.NET;
        c.drawColor(INK);text(c,coconut?"KÓKUSZDOBÁLÁS":ballistic?"VADÁSZAT":net?"HALÁSZAT HÁLÓVAL":"HORGÁSZAT",30,60,28,GOLD);
        text(c,"VISSZA",588,60,20,WHITE);
        String help=coconut?"Érintőkar: szög és erő. Két találat / kókusz. Cél: 5 kókusz, legfeljebb 10 dobás.":ballistic?"Érintőkar: szög és erő. A lefelé tartó nyíllal találd el az állatot. 10 lövésed van.":net?"Bökj a Merítés gombra. Fogj 10 halat legfeljebb 5 merítéssel!":"Érintőkar: horogmozgatás. A halnál nyomd meg a Megakasztás gombot, majd húzd fel a felszínre.";
        wrapped(c,help,30,106,655,22,WHITE,3);
        harvestTop=205;harvestScale=Math.max(320,Math.min(1040,logicalHeight-480))/640f;
        c.save();c.translate(0,harvestTop);c.scale(1,harvestScale);c.clipRect(20,0,700,640);
        p.setShader(new LinearGradient(0,0,0,640,ballistic?0xff94b878:0xff56c5cc,ballistic?0xff234c39:0xff113f6b,Shader.TileMode.CLAMP));c.drawRect(20,0,700,640,p);p.setShader(null);
        if(ballistic){
            for(int i=0;i<(coconut?3:5);i++)sprite(c,objects,0,coconut?260+i*180:70+i*150,coconut?625:310,coconut?260:180,coconut?540:260);
            box(c,20,584,700,640,0,0xff786441);
            if(!g.flying&&!g.finished){
                float reach=130+g.power*4;
                p.setColor(0x30edc37e);dest.set(54-reach,580-reach,54+reach,580+reach);c.drawArc(dest,-g.angle-12,24,true,p);
                double angle=Math.toRadians(g.angle);float speed=330+g.power*5;
                p.setColor(0x99f6efd9);
                for(float t=.12f;t<.9f;t+=.12f)c.drawCircle(54+(float)Math.cos(angle)*speed*t,580-(float)Math.sin(angle)*speed*t+300*t*t,3,p);
            }
            if(coconut){
                for(int i=0;i<6;i++){
                    float x=g.coconutX[i]+(g.coconutHits[i]==1?(float)Math.sin(g.elapsed*24)*3:0),y=g.coconutY[i]+g.coconutFall[i];
                    itemArt.draw(c,1,x,y+25,50,50);
                    if(g.coconutHits[i]==1){p.setColor(GOLD);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);c.drawCircle(x,y,27,p);p.setStyle(Paint.Style.FILL);centered(c,"1/2",x,y-34,17,WHITE);}
                }
            }else itemArt.draw(c,g.targetItem,g.animalX,g.animalY+35,135,120);
            sprite(c,robinson,8,54,625,106,106);
            if(!coconut){p.setColor(GOLD);p.setStrokeWidth(4);p.setStyle(Paint.Style.STROKE);c.drawArc(52,542,92,613,-70,140,false,p);c.drawLine(76,547,76,607,p);p.setStyle(Paint.Style.FILL);}
            float ax=g.flying?g.arrowX:54,ay=g.flying?g.arrowY:580;
            if(coconut){p.setColor(0xffb5b4a4);c.drawCircle(ax,ay,8,p);p.setColor(0xffe2d7ba);c.drawCircle(ax-2,ay-2,3,p);}
            else{
                float vx=g.flying?g.arrowVX:(float)Math.cos(Math.toRadians(g.angle))*60,vy=g.flying?g.arrowVY:-(float)Math.sin(Math.toRadians(g.angle))*60;
                float length=(float)Math.hypot(vx,vy);vx=vx/length*25;vy=vy/length*25;
                p.setColor(WHITE);p.setStrokeWidth(3);c.drawLine(ax-vx,ay-vy,ax+vx,ay+vy,p);c.drawCircle(ax+vx,ay+vy,4,p);
            }
        }else{
            p.setColor(0x447ce7dc);for(int i=0;i<9;i++)c.drawCircle(65+i*73,70+(i*83)%520,7,p);
            int count=net?7:1;
            for(int i=0;i<count;i++)if(!g.caught[i]){
                float fx=g.hooked?g.hookX:g.fishX[i],fy=g.hooked?g.hookY:g.fishY[i];
                itemArt.draw(c,net?(i%2==0?2:59):g.reward,fx,fy+31,100,76);
            }
            p.setColor(WHITE);p.setStrokeWidth(3);
            if(!net){
                c.drawLine(360,0,g.hookX,g.hookY,p);p.setStyle(Paint.Style.STROKE);c.drawArc(g.hookX-8,g.hookY-4,g.hookX+8,g.hookY+17,0,210,false,p);p.setStyle(Paint.Style.FILL);
                text(c,g.hooked?"KAPÁS! HÚZD FEL!":"FELSZÍN",35,43,22,GOLD);
            }else{
                float ny=g.dropping?g.netY:5;
                for(int i=0;i<9;i++)c.drawLine(260+i*25,ny-25,260+i*25,ny+25,p);
                for(int i=0;i<3;i++)c.drawLine(260,ny-25+i*25,460,ny-25+i*25,p);
            }
        }
        c.restore();
        String status=ballistic?"SZÖG "+(int)g.angle+"°   ERŐ "+(int)g.power+"%   "+g.attempts+"/10 "+(coconut?"dobás":"lövés"):net?g.total+" / 10 hal     "+g.attempts+" / 5 merítés":g.hooked?"Kapás! Mozgasd felfelé az érintőkart!":"Vezesd a horgot a halhoz.";
        centered(c,status,360,logicalHeight-258,21,WHITE);
        if(net){
            box(c,30,logicalHeight-210,690,logicalHeight-122,18,GOLD);centered(c,g.finished?"VISSZA A SZIGETRE":g.dropping?"MERÍTÉS…":"MERÍTÉS",360,logicalHeight-157,25,INK);
        }else{
            float jx=128,jy=logicalHeight-153;
            p.setColor(0xff274e47);c.drawCircle(jx,jy,76,p);p.setColor(0xff58776a);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);c.drawCircle(jx,jy,58,p);c.drawLine(jx-60,jy,jx+60,jy,p);c.drawLine(jx,jy-60,jx,jy+60,p);p.setStyle(Paint.Style.FILL);
            p.setColor(0xffe8d4a8);c.drawCircle(jx+joyX*34,jy+joyY*34,27,p);
            box(c,300,logicalHeight-210,694,logicalHeight-122,20,GOLD);
            String button=g.finished?"VISSZA A SZIGETRE":coconut?(g.flying?"A KŐ REPÜL…":"DOBÁS"):ballistic?(g.flying?"A NYÍL REPÜL…":"LÖVÉS"):g.hooked?"HÚZD FEL!":"MEGAKASZTÁS";
            centered(c,button,497,logicalHeight-157,23,INK);
            centered(c,ballistic?"↔ szög · ↕ erő":"↔ ↕ horog",128,logicalHeight-48,19,0xffb9cdbb);
        }
        centered(c,coconut?g.total+" / 5 kókusz · egy kókuszhoz két találat kell":net?"Egy burgonya egy teljes halászathoz.":ballistic?"Az érintőkar elengedése megtartja a célzást.":"A felszínhez húzott hal a tarisznyába kerül.",420,logicalHeight-83,16,0xffb9cdbb);
        if(g.finished){box(c,35,harvestTop+160,685,harvestTop+340,20,0xf2102e2e);centered(c,g.won?"SIKERES FOGÁS":"PRÓBÁLD ÚJRA",360,harvestTop+210,28,GOLD);wrapped(c,harvestResult,65,harvestTop+259,590,23,WHITE,3);}
    }
    public void closeHarvest(){
        if(activity.engine.harvest!=null){
            if(activity.engine.harvest.finished)activity.engine.say(activity.engine.finishHarvest());
            else activity.engine.cancelHarvest();
        }
        shownHarvest=null;harvestResult=null;screen=WORLD;lastFrame=0;resetInput();activity.save();invalidate();
    }
    private boolean harvestTouch(MotionEvent event,int action,int index,int pointer,float x,float y){
        if(activity.paused)return true;
        HarvestGame g=activity.engine.harvest!=null?activity.engine.harvest:shownHarvest;if(g==null)return true;
        boolean down=action==MotionEvent.ACTION_DOWN||action==MotionEvent.ACTION_POINTER_DOWN;
        boolean up=action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_POINTER_UP;
        boolean actionArea=y>=logicalHeight-210&&y<=logicalHeight-122&&(g.tool==HarvestGame.NET?x>=30&&x<=690:x>=300&&x<=694);
        if(g.finished){resetInput();if(up&&(actionArea||y<90&&x>570))closeHarvest();return true;}
        float gy=(y-harvestTop)/harvestScale;
        if(down){
            if(g.tool!=HarvestGame.NET&&Math.hypot(x-128,y-(logicalHeight-153))<=98&&joyPointer<0){
                joyPointer=pointer;joyOriginX=128;joyOriginY=logicalHeight-153;updateJoystick(x,y);
            }else if((actionArea||y<90&&x>570)&&tapPointer<0){tapPointer=pointer;tapStartX=x;tapStartY=y;}
            else if(g.tool==HarvestGame.ROD&&gy>=0&&gy<=640&&x>=20&&x<=700&&hookPointer<0){hookPointer=pointer;g.moveHook(x,gy);}
        }else if(action==MotionEvent.ACTION_MOVE){
            int j=event.findPointerIndex(joyPointer);if(j>=0)updateJoystick((event.getX(j)-getPaddingLeft())/scale,(event.getY(j)-getPaddingTop())/scale);
            int h=event.findPointerIndex(hookPointer);if(h>=0)g.moveHook((event.getX(h)-getPaddingLeft())/scale,((event.getY(h)-getPaddingTop())/scale-harvestTop)/harvestScale);
        }else if(up){
            if(pointer==joyPointer){joyPointer=-1;joyX=joyY=0;}
            else if(pointer==hookPointer){hookPointer=-1;g.action();}
            else if(pointer==tapPointer){tapPointer=-1;if(Math.hypot(x-tapStartX,y-tapStartY)<45){if(y<90&&x>570)closeHarvest();else if(actionArea)g.action();}}
        }
        invalidate();return true;
    }
    @Override public boolean onTouchEvent(android.view.MotionEvent event){
        int action=event.getActionMasked(),index=event.getActionIndex(),pointer=event.getPointerId(index);
        float x=(event.getX(index)-getPaddingLeft())/scale,y=(event.getY(index)-getPaddingTop())/scale;
        if(action==MotionEvent.ACTION_CANCEL){resetInput();return true;}
        if(screen==HARVEST)return harvestTouch(event,action,index,pointer,x,y);
        if(screen!=WORLD){
            if(action==MotionEvent.ACTION_UP){performClick();
                if(screen==TITLE){float first=logicalHeight-243;
                    if(y>=first&&y<=first+78){if(activity.hasSave())new android.app.AlertDialog.Builder(activity).setMessage("Új kalandot indítasz? A korábbi mentést az új játék következő mentése felülírja.").setPositiveButton("Új kaland",(d,w)->activity.start(false)).setNegativeButton("Mégsem",null).show();else activity.start(false);}
                    else if(y>=first+94&&y<=first+166&&activity.hasSave())activity.start(true);
                }else{
                    float side=Math.min(672,logicalHeight-265),left=(720-side)/2;
                    if(y>130&&y<130+side&&x>left&&x<left+side){int tx=(int)((x-left)/side*180),ty=(int)((y-130)/side*180);activity.engine.navigate(tx,ty);}
                    screen=WORLD;lastFrame=0;
                }invalidate();
            }return true;
        }
        if(activity.paused)return true;
        if(action==MotionEvent.ACTION_DOWN||action==MotionEvent.ACTION_POINTER_DOWN){
            if(x<230&&y>logicalHeight-270&&y<logicalHeight-80&&joyPointer<0){joyPointer=pointer;joyOriginX=112;joyOriginY=logicalHeight-161;updateJoystick(x,y);}
            else if(tapPointer<0){tapPointer=pointer;tapStartX=tapX=x;tapStartY=tapY=y;}
        }else if(action==MotionEvent.ACTION_MOVE){
            int j=event.findPointerIndex(joyPointer);if(j>=0)updateJoystick((event.getX(j)-getPaddingLeft())/scale,(event.getY(j)-getPaddingTop())/scale);
        }else if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_POINTER_UP){
            if(pointer==joyPointer){joyPointer=-1;joyX=joyY=0;}
            if(pointer==tapPointer){tapPointer=-1;if(Math.hypot(x-tapStartX,y-tapStartY)<40){performClick();tap(x,y);}}
        }
        invalidate();return true;
    }
    private void updateJoystick(float x,float y){float dx=x-joyOriginX,dy=y-joyOriginY,len=(float)Math.hypot(dx,dy);float divisor=Math.max(58,len);joyX=dx/divisor;joyY=dy/divisor;}
    private void tap(float x,float y){
        GameEngine e=activity.engine;float h=logicalHeight;
        if(y>h-78){e.stop();int tab=Math.min(3,(int)(x/180));if(tab==0)activity.inventory();else if(tab==1)activity.crafting();else if(tab==2){screen=MAP;resetInput();}else activity.journal();return;}
        if(x>=480&&y>=h-217&&y<=h-123){activity.actions();return;}
        if(x>620&&y<100){activity.pauseMenu();return;}
        if(y<260)return;
        float ox=360-cameraX,oy=h*.49f-cameraY;int tx=(int)Math.floor((x-ox)/TILE),ty=(int)Math.floor((y-oy)/TILE);
        if(tx<0||tx>=180||ty<0||ty>=180)return;
        int tile=ty*180+tx;e.selected=e.objects[tile]!=0||e.coconutPalm(tile)?tile:-1;e.navigate(tx,ty);
    }
    @Override public boolean performClick(){super.performClick();return true;}
}
