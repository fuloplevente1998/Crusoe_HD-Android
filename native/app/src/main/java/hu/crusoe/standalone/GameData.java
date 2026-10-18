package hu.crusoe.standalone;

import java.io.*;
import java.util.*;

/** Plain game data, independent of Android and of every Java ME API. */
public final class GameData {
    public final int width, height;
    public final String sourceHash;
    public final byte[][] layers = new byte[5][];
    public final Item[] items;
    public final Recipe[] recipes;
    public final Event[] events;
    public static final int TERRAIN=0, BORDERS=1, DECOR=2, OBJECTS=3, ROCKS=4;
    public static final class Item {
        public String name, english, description;
        public final int[] attributes=new int[8];
        public int clothingSlot, category;
    }
    public static final class Recipe {
        public int item, context;
        public int[][] ingredients, tools;
    }
    public static final class Event { public String title, text; public int code; }

    public GameData(InputStream source) throws IOException {
        DataInputStream in=new DataInputStream(new BufferedInputStream(source));
        if(in.readInt()!=0x43525332)throw new IOException("Unsupported world format");
        sourceHash=in.readUTF(); width=in.readInt(); height=in.readInt();
        if(width!=180||height!=180)throw new IOException("Unexpected map size");
        for(int i=0;i<layers.length;i++){layers[i]=new byte[width*height];in.readFully(layers[i]);}
        int count=in.readInt();if(count!=120)throw new IOException("Expected 120 items");
        items=new Item[count];
        for(int i=0;i<count;i++){
            Item item=new Item();items[i]=item;
            item.name=in.readUTF();item.english=in.readUTF();item.description=in.readUTF();
            for(int a=0;a<8;a++)item.attributes[a]=in.readInt();
            item.clothingSlot=in.readInt();item.category=in.readInt();
        }
        count=in.readInt();if(count!=45)throw new IOException("Expected 45 recipes");
        recipes=new Recipe[count];
        for(int i=0;i<count;i++){
            Recipe r=new Recipe();recipes[i]=r;r.item=in.readInt();r.context=in.readInt();
            r.ingredients=readPairs(in);r.tools=readPairs(in);
        }
        count=in.readInt();if(count!=13)throw new IOException("Expected 13 tutorial events");
        events=new Event[count];
        for(int i=0;i<count;i++){Event e=new Event();e.title=in.readUTF();e.text=in.readUTF();e.code=in.readInt();events[i]=e;}
        if(in.read()!=-1)throw new IOException("Unexpected trailing world data");
    }
    private static int[][] readPairs(DataInputStream in)throws IOException {
        int n=in.readInt();if(n<0||n>16)throw new IOException("Invalid recipe group");
        int[][] result=new int[n][2];
        for(int[] pair:result){pair[0]=in.readInt();pair[1]=in.readInt();if(pair[0]<0||pair[0]>=120||pair[1]<=0)throw new IOException("Invalid recipe item");}
        return result;
    }
    public int tile(int layer,int x,int y){return layers[layer][y*width+x]&255;}
    public Recipe recipe(int item){for(Recipe r:recipes)if(r.item==item)return r;return null;}
}
