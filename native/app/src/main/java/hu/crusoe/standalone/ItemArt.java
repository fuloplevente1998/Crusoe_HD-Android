package hu.crusoe.standalone;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import java.io.*;

/** One item identity across the world, backpack and crafting requirements. */
public final class ItemArt {
    private final Bitmap[] atlases;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Rect source=new Rect();
    private final RectF target=new RectF();
    private static final int[] SHEET=new int[120],CELL=new int[120];
    static {
        java.util.Arrays.fill(SHEET,-1);
        assign(0,new int[][]{{1,14},{3,1},{6,3},{7,13},{8,2},{9,6},{10,6},{11,12},{16,15},{17,11},{18,5},{19,7},{21,1},{35,12},{38,8},{84,12},{93,12},{94,7}});
        assign(1,new int[][]{{40,0},{56,1},{57,2},{58,3},{2,4},{59,5},{4,6},{41,7},{42,8},{43,9},{44,10},{46,11},{47,12},{48,13},{49,13},{51,14},{52,14},{53,14},{61,15},{62,14},{63,14}});
        assign(2,new int[][]{{64,0},{65,1},{66,2},{67,3},{68,4},{70,5},{71,6},{72,7},{73,8},{74,9},{75,10},{90,11},{0,12},{24,13},{25,14},{27,15}});
        assign(3,new int[][]{{76,0},{5,1},{77,2},{78,3},{79,4},{80,5},{22,6},{23,7},{86,8},{89,9},{96,10},{97,11},{13,12},{12,13},{54,13},{92,14},{98,15}});
        assign(4,new int[][]{{26,0},{28,1},{30,2},{31,3},{32,4},{33,5},{20,6},{35,7},{39,8},{36,9},{37,10},{29,11},{69,12},{91,13},{81,14},{45,14},{83,15}});
        for(int id:new int[]{10,45,84,94})SHEET[id]=-1;
    }
    private static void assign(int sheet,int[][] pairs){for(int[] pair:pairs){SHEET[pair[0]]=sheet;CELL[pair[0]]=pair[1];}}
    public ItemArt(Context context,Bitmap objects,Bitmap fauna){
        atlases=new Bitmap[]{objects,fauna,load(context,"shore"),load(context,"gear"),load(context,"supplies")};
    }
    private static Bitmap load(Context context,String name){
        try(InputStream in=context.getAssets().open("art/"+name+".png")){
            BitmapFactory.Options options=new BitmapFactory.Options();options.inScaled=false;
            Bitmap bitmap=BitmapFactory.decodeStream(in,null,options);if(bitmap==null)throw new IOException("Invalid image");return bitmap;
        }catch(IOException e){throw new IllegalStateException("Cannot load item art: "+name,e);}
    }
    public void draw(Canvas canvas,int id,float x,float bottom,float width,float height){
        if(id<0||id>=120)return;
        int sheet=SHEET[id];
        if(sheet>=0){
            Bitmap atlas=atlases[sheet];int cell=CELL[id];
            source.set(cell%4*atlas.getWidth()/4,cell/4*atlas.getHeight()/4,(cell%4+1)*atlas.getWidth()/4,(cell/4+1)*atlas.getHeight()/4);
            if(sheet==0&&id==16)source.top+=source.height()/7; // Exclude a neighboring atlas cell's leaf tips.
            if(sheet==1&&cell==2)source.bottom-=source.height()/9; // Keep the next row's goat horns out of the yellow-fish sprite.
            target.set(x-width/2,bottom-height,x+width/2,bottom);paint.setColor(Color.WHITE);paint.setShader(null);paint.setStyle(Paint.Style.FILL);
            canvas.drawBitmap(atlas,source,target,paint);
        }else{
            canvas.save();canvas.translate(x-width/2,bottom-height);canvas.scale(width/100,height/100);drawEquipment(canvas,id);canvas.restore();
        }
    }
    private void line(Canvas c,float x,float y,float xx,float yy,int color,float stroke){paint.setColor(color);paint.setStrokeWidth(stroke);paint.setStrokeCap(Paint.Cap.ROUND);c.drawLine(x,y,xx,yy,paint);}
    private void rect(Canvas c,float l,float t,float r,float b,int color){paint.setColor(color);paint.setStyle(Paint.Style.FILL);target.set(l,t,r,b);c.drawRoundRect(target,4,4,paint);}
    private void oval(Canvas c,float l,float t,float r,float b,int color){paint.setColor(color);paint.setStyle(Paint.Style.FILL);c.drawOval(l,t,r,b,paint);}
    private void drawEquipment(Canvas c,int id){
        final int wood=0xff98683c,light=0xffddb177,steel=0xffb9c8c4,cloth=0xffead8a8;
        if(id>=99&&id<=114){
            int color=new int[]{0xffc6b58b,0xff7d3841,0xffddd3b6,0xffa16a43}[id%4];
            if(id<=102){ // Jackets and shirts.
                rect(c,30,30,70,84,color);line(c,30,33,14,59,color,17);line(c,70,33,86,59,color,17);
                oval(c,40,22,60,38,0xff283e36);line(c,50,40,50,79,light,2);
                if(id!=99)for(int y=46;y<80;y+=10)oval(c,54,y,57,y+3,steel);
            }else if(id<=106){rect(c,27,22,73,47,color);rect(c,27,42,46,84,color);rect(c,54,42,73,84,color);line(c,30,30,70,30,wood,4);}
            else if(id<=110){rect(c,17,52,45,81,color);rect(c,55,39,83,68,color);rect(c,13,73,46,84,wood);rect(c,54,63,87,72,wood);}
            else {oval(c,12,57,88,77,color);rect(c,30,30,70,65,color);line(c,31,58,69,58,wood,7);}
            return;
        }
        switch(id){
            case 10:rect(c,23,40,77,84,wood);for(int i=0;i<5;i++)line(c,24,45+i*7,76,45+i*7,light,2);Path roof=new Path();roof.moveTo(13,43);roof.lineTo(50,16);roof.lineTo(87,43);roof.close();paint.setColor(0xff704b2e);c.drawPath(roof,paint);rect(c,42,53,59,84,0xff3c3326);break;
            case 45:for(int i=0;i<3;i++){line(c,26+i*13,80,44+i*13,20,wood,3);line(c,44+i*13,20,37+i*13,31,steel,5);line(c,27+i*13,75,19+i*13,68,cloth,4);}break;
            case 84:case 94:
                for(int i=0;i<(id==94?5:3);i++)line(c,20+i*14,73,30+i*12,47,wood,10);
                line(c,22,58,82,58,light,3);line(c,22,69,82,69,light,3);
                if(id==94){line(c,54,62,54,13,wood,4);Path sail=new Path();sail.moveTo(57,17);sail.lineTo(82,42);sail.lineTo(57,42);sail.close();paint.setColor(cloth);c.drawPath(sail,paint);}break;
            case 14:line(c,22,86,66,16,wood,5);line(c,66,16,86,70,steel,1.5f);line(c,86,70,79,79,steel,3);break;
            case 15:case 85:
                line(c,15,40,15,85,wood,7);line(c,85,40,85,85,wood,7);rect(c,15,45,85,72,wood);rect(c,20,40,80,60,cloth);rect(c,23,39,41,50,steel);if(id==85)rect(c,50,39,72,50,steel);break;
            case 34:case 88:case 117:
                rect(c,20,20,28,87,wood);rect(c,72,20,80,87,wood);rect(c,17,20,83,28,light);rect(c,17,76,83,84,light);
                if(id==88){oval(c,23,34,77,54,steel);rect(c,43,47,57,80,wood);oval(c,39,30,61,47,wood);}
                else {for(int i=0;i<7;i++)line(c,32+i*6,30,32+i*6,74,cloth,2);for(int i=0;i<(id==117?6:3);i++)line(c,27,40+i*6,74,40+i*6,id==117?0xff799c85:0xffae6d58,4);}
                break;
            case 50:case 87:
                oval(c,22,60,78,88,wood);rect(c,22,30,78,73,id==50?wood:0xffa56f49);oval(c,22,20,78,42,id==50?light:0xff64492f);
                if(id==50){line(c,23,42,77,42,steel,6);line(c,23,69,77,69,steel,6);}else {line(c,20,40,10,45,wood,5);line(c,80,40,90,45,wood,5);}break;
            case 60:rect(c,12,25,88,78,0xffa65c45);rect(c,22,34,78,69,cloth);line(c,25,51,74,51,0xff52877c,12);break;
            case 82:line(c,18,26,18,83,wood,6);line(c,82,26,82,83,wood,6);line(c,12,36,89,36,steel,4);oval(c,34,31,67,54,0xffa66036);break;
            case 55: // Distinct shark silhouette, rather than a branch or coconut.
                oval(c,13,38,80,68,0xff7399a4);Path fin=new Path();fin.moveTo(44,41);fin.lineTo(57,17);fin.lineTo(64,44);fin.close();paint.setColor(0xff527b89);c.drawPath(fin,paint);fin.reset();fin.moveTo(76,51);fin.lineTo(93,32);fin.lineTo(90,73);fin.close();c.drawPath(fin,paint);oval(c,24,46,28,50,0xff142f39);break;
            case 115:case 116:case 119:
                oval(c,23,20,77,84,id==116?cloth:0xffbaa088);for(int i=0;i<5;i++)line(c,30,31+i*10,70,42+i*7,id==116?0xffbaae86:0xffdfcdb6,2);break;
            case 118:rect(c,18,26,82,78,cloth);line(c,26,38,74,38,light,2);line(c,26,65,74,65,light,2);break;
            case 95:break; // Unused original placeholder is not a collectible.
            default:rect(c,25,24,75,78,wood);line(c,30,35,70,65,light,5);break;
        }
    }
    public Drawable icon(int id){return new Drawable(){
        @Override public void draw(Canvas canvas){Rect b=getBounds();ItemArt.this.draw(canvas,id,b.exactCenterX(),b.bottom,b.width(),b.height());}
        @Override public void setAlpha(int alpha){}
        @Override public void setColorFilter(ColorFilter filter){}
        @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    };}
}
