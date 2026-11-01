package com.nthexperience.desktop;

import android.graphics.*;
import org.json.*;
import java.util.*;

/** Original individual gadget objects, drawn at any density without bitmap scaling artifacts. */
final class GadgetArtwork {
    private final Canvas c;private final GadgetView view;private final Paint p=new Paint(3);private final JSONObject data;
    private static final int INK=0xff293f49,CREAM=0xfff7f0db,WHITE=0xffedf7fd;
    private GadgetArtwork(Canvas c,GadgetView view){this.c=c;this.view=view;data=GadgetData.get(view.store,view.kind);}
    static void draw(Canvas c,GadgetView view){GadgetArtwork a=new GadgetArtwork(c,view);switch(view.kind){case "worldclock":a.world();break;case "feeds":a.paper();break;case "checklist":a.clipboard();break;case "timer":a.hourglass();break;case "stopwatch":a.chrono();break;case "calculator":a.calculator();break;case "converter":a.converter();break;case "storage":a.disk();break;case "network":a.network();break;case "compass":a.compass();break;case "puzzle":a.puzzle();}}
    private void fill(int color){p.setShader(null);p.setColor(color);p.setStyle(Paint.Style.FILL);}
    private void rect(float l,float t,float r,float b,int color){fill(color);c.drawRect(l,t,r,b,p);}
    private void line(float x,float y,float xx,float yy,int color,float width){fill(color);p.setStrokeWidth(width);c.drawLine(x,y,xx,yy,p);}
    private void plate(float l,float t,float r,float b,float radius,int top,int bottom){p.setStyle(Paint.Style.FILL);p.setShader(new LinearGradient(l,t,r,b,new int[]{top,bottom},null,Shader.TileMode.CLAMP));p.setShadowLayer(2,0,2,0x66000000);c.drawRoundRect(l,t,r,b,radius,radius,p);p.clearShadowLayer();p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setColor(0x88616d73);p.setStrokeWidth(.7f);c.drawRoundRect(l,t,r,b,radius,radius,p);p.setStyle(Paint.Style.FILL);}
    private void circle(float x,float y,float r,int color){fill(color);c.drawCircle(x,y,r,p);}
    private void text(String text,float x,float y,float size,int color,boolean bold){fill(color);p.setTypeface(bold?Aero.strong(view.getContext()):Aero.face(view.getContext()));p.setTextSize(size);p.setTextAlign(Paint.Align.CENTER);c.drawText(text,x,y,p);p.setTextAlign(Paint.Align.LEFT);}
    private String shortText(String value,int max){return value.length()>max?value.substring(0,max-1)+"…":value;}
    private void fitted(String value,float x,float y,float size,float width,int color){fill(color);p.setTypeface(Aero.strong(view.getContext()));p.setTextSize(size);float measured=p.measureText(value);text(value,x,y,measured>width?size*width/measured:size,color,true);}
    private void bolt(float x,float y){circle(x,y,2.2f,0xff65747a);circle(x-.4f,y-.5f,1.8f,0xffd8e2e7);line(x-1,y,x+1,y,0xff57656f,.6f);}
    private void world(){
        plate(3,5,129,144,8,0xff986c49,0xff322b29);plate(8,10,124,139,4,0xffcab083,0xff766449);
        for(int row=0;row<2;row++){
            int y=15+row*60;String zone=data.optString("zone"+(row+1),row==0?"Europe/London":"Asia/Tokyo");TimeZone tz=TimeZone.getTimeZone(zone);Calendar now=Calendar.getInstance(tz);int hour=now.get(Calendar.HOUR_OF_DAY);boolean night=hour<6||hour>=18;
            plate(12,y,120,y+55,3,night?0xff24344c:0xff5293ac,night?0xff101b29:0xffd8e1d2);
            // Fine longitude and latitude lines give each clock a travel-instrument face.
            for(int x=22;x<116;x+=18)line(x,y+4,x,y+51,0x225ce2ee,.6f);for(int yy=y+9;yy<y+50;yy+=12)line(16,yy,116,yy,0x225ce2ee,.6f);
            text(shortText(zone.substring(zone.lastIndexOf('/')+1).replace('_',' '),18),66,y+12,9,night?WHITE:0xffe9faff,true);
            java.text.SimpleDateFormat time=new java.text.SimpleDateFormat(android.text.format.DateFormat.is24HourFormat(view.getContext())?"HH:mm":"h:mm a",Locale.getDefault());time.setTimeZone(tz);text(time.format(now.getTime()),66,y+34,20,night?WHITE:0xff163b50,true);
            java.text.SimpleDateFormat date=new java.text.SimpleDateFormat("EEE, MMM d",Locale.getDefault());date.setTimeZone(tz);text(date.format(now.getTime()),66,y+47,8,night?0xffb4c7da:0xff294e5b,false);
        }text("WORLD TIME",66,136,6,0xffe9d6b3,true);
    }
    private void paper(){
        plate(9,7,128,152,1,0xffd3ccba,0xff969b96);plate(4,3,123,147,1,0xfffff9e8,0xffded7c3);
        text("THE DAILY FEED",64,22,13,0xff342f2b,true);line(12,28,115,28,0xff49413a,1.5f);text("RSS  •  HEADLINES  •  ATOM",64,38,6,0xff746351,false);line(12,43,115,43,0xff83745e,.7f);
        JSONArray items=FeedClient.items(view.store);
        if(items.length()==0){text("Your headlines here",64,70,11,INK,true);text("Choose an RSS / Atom feed",64,88,8,0xff756e5c,false);for(int yy=105;yy<136;yy+=7)line(15,yy,112,yy,0xffbfb7a5,.6f);}
        for(int i=0;i<Math.min(3,items.length());i++){JSONObject item=items.optJSONObject(i);if(item==null)continue;int y=56+i*28;String title=item.optString("title");text(shortText(title,23),64,y,9,INK,true);text(title.length()>22?shortText(title.substring(22),25):"Tap to read the article",64,y+11,7,0xff665c4a,false);line(13,y+18,114,y+18,0xffb3a993,.5f);}
        text("OPEN TO READ MORE",64,139,6,0xff786d5d,true);
    }
    private void clipboard(){
        plate(8,8,124,145,7,0xffae814a,0xff65401e);plate(15,15,117,136,1,0xfffffdf1,0xffe8e6d5);
        line(33,21,33,130,0xffdfa8a0,.7f);for(int y=45;y<131;y+=21)line(19,y,113,y,0xffc6d7d4,.6f);
        plate(42,3,90,19,3,0xffe9edf0,0xff7f9096);circle(66,9,2,0xff5f6868);text("MY CHECKLIST",67,34,9,0xff4d6654,true);
        JSONArray tasks=data.optJSONArray("tasks");int count=tasks==null?0:tasks.length(),done=0;
        for(int i=0;i<count;i++)if(tasks.optJSONObject(i)!=null&&tasks.optJSONObject(i).optBoolean("done"))done++;
        for(int i=0;i<Math.min(3,count);i++){JSONObject task=tasks.optJSONObject(i);if(task==null)continue;int y=61+i*21;rect(21,y-7,28,y,0xffd0dad0);if(task.optBoolean("done")){line(21,y-3,24,y,0xff4e8d56,1.5f);line(24,y,29,y-8,0xff4e8d56,1.5f);}text(shortText(task.optString("text"),14),73,y,8,0xff4b564a,false);}
        if(count==0)text("Tap to add your tasks",70,80,8,0xff71806c,false);text(done+" / "+count+" done",68,125,8,0xff496442,true);
        // Original pencil along the right edge of the clipboard.
        c.save();c.rotate(8,118,85);plate(116,49,122,124,1,0xfff5c258,0xffce902d);rect(116,49,122,57,0xffd79591);rect(116,58,122,61,0xffbdc6c4);c.restore();
    }
    private void hourglass(){
        plate(15,6,117,15,3,0xffd8b168,0xff745426);plate(15,115,117,126,3,0xffd4b069,0xff5b4222);
        for(int x:new int[]{23,109})plate(x,15,x+3,115,1,0xffcaa469,0xff795021);
        Path glass=new Path();glass.moveTo(31,16);glass.lineTo(101,16);glass.cubicTo(100,45,76,51,70,64);glass.cubicTo(76,80,100,85,101,115);glass.lineTo(31,115);glass.cubicTo(32,85,56,80,62,64);glass.cubicTo(56,51,32,45,31,16);glass.close();
        int save=c.save();c.clipPath(glass);p.setShader(new LinearGradient(30,0,102,0,new int[]{0xaaeffbff,0x33235f7f,0x88d8f6fa},null,Shader.TileMode.CLAMP));c.drawPath(glass,p);p.setShader(null);
        long value=GadgetData.value(view.store,"timer"),duration=Math.max(1,data.optLong("duration",300000));float fraction=Math.min(1,value/(float)duration);
        rect(30,52-32*fraction,102,54,0xffdbb158);Path sand=new Path();sand.moveTo(31,113);sand.lineTo(66,113-33*(1-fraction));sand.lineTo(101,113);sand.close();fill(0xffe6bc69);c.drawPath(sand,p);if(data.optBoolean("running")&&value>0)line(66,56,66,102,0xfff9de91,1);c.restoreToCount(save);
        fill(0xffbee6ed);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1);c.drawPath(glass,p);p.setStyle(Paint.Style.FILL);line(39,22,42,39,0xaaffffff,2);
        plate(28,129,104,151,3,0xff364c55,0xff152b35);fitted(GadgetData.duration(value,false),66,144,17,69,WHITE);
    }
    private void chrono(){
        plate(57,3,75,13,3,0xffe9edf0,0xff77808c);plate(53,10,79,18,3,0xffd5dde1,0xff686f7a);c.save();c.rotate(-35,27,22);plate(17,17,35,26,3,0xffd1dde4,0xff69788a);c.restore();
        p.setShader(new LinearGradient(0,16,0,139,new int[]{0xfff0f4f6,0xff85939a,0xffdce3e6,0xff586875},null,Shader.TileMode.CLAMP));c.drawCircle(66,77,59,p);p.setShader(null);circle(66,77,53,0xff0d2535);circle(66,77,48,0xff183c50);
        for(int i=0;i<60;i++){double a=i*Math.PI/30;float r=i%5==0?39:43;line(66+(float)Math.sin(a)*r,77-(float)Math.cos(a)*r,66+(float)Math.sin(a)*46,77-(float)Math.cos(a)*46,i%5==0?0xffe4e9eb:0xff829ba7,i%5==0?1.2f:.6f);}
        long value=GadgetData.value(view.store,"stopwatch");c.save();c.rotate(value%60000*.006f,66,77);line(66,85,66,38,0xffec9a53,1.5f);c.restore();circle(66,77,2.5f,0xffe4c49c);
        text("CHRONOGRAPH",66,59,6,0xffafcbd8,true);plate(26,89,106,109,2,0xff101d25,0xff263d47);fitted(GadgetData.duration(value,false),66,104,17,73,WHITE);text(data.optBoolean("running")?"RUNNING":"PAUSED",66,122,6,0xffbbcdd4,false);
    }
    private void calculator(){
        plate(12,4,120,154,10,0xff818f9b,0xff23313e);line(24,15,108,15,0x66ffffff,.8f);text("Nth  CALCULATOR",66,25,7,0xffe0e9ed,true);
        plate(21,33,111,65,3,0xff65766c,0xffd7e0c4);fitted(data.optString("result","0"),66,56,21,80,0xff2e4134);
        String[] keys={"7","8","9","÷","4","5","6","×","1","2","3","−","0",".","=","+"};
        for(int i=0;i<16;i++){int x=22+i%4*23,y=76+i/4*17;boolean operator=i%4==3||i==14;plate(x,y,x+18,y+13,2,operator?0xffe7ad63:0xfff3f3ed,operator?0xffa06a31:0xff929fa7);text(keys[i],x+9,y+10,8,operator?0xff4e3320:INK,true);}
        text("TAP TO CALCULATE",66,147,6,0xffbdcbd0,false);
    }
    private void converter(){
        plate(5,8,127,130,5,0xfff4edcf,0xffb89e5e);plate(9,12,123,126,3,0xffeee7ca,0xffd8d2b7);text("UNIT CONVERTER",66,29,10,0xff4e5c58,true);
        // Two original ruler scales cross behind the conversion display.
        c.save();c.rotate(-9,66,62);plate(12,46,120,70,1,0xfff5d78f,0xffb68f44);for(int i=0;i<21;i++){int x=16+i*5;line(x,47,x,i%5==0?58:53,0xff675933,.7f);}c.restore();
        plate(14,75,118,111,3,0xfff9fcf2,0xffbecdbb);String input=data.optString("input","1"),result="Choose units",from="";
        try{int category=data.optInt("category",0),a=data.optInt("from",0),b=data.optInt("to",1);result=GadgetMath.number(GadgetMath.convert(Double.parseDouble(input),category,a,b))+" "+GadgetMath.UNITS[category][b];from=input+" "+GadgetMath.UNITS[category][a];}catch(RuntimeException ignored){}
        fitted(shortText(from,24)+"  →",66,88,9,94,0xff5c6a59);fitted(shortText(result,26),66,104,16,94,0xff264e42);bolt(14,20);bolt(118,119);
    }
    private void disk(){
        plate(7,7,125,125,7,0xffdce2e5,0xff839199);plate(13,13,119,104,3,0xffa7b4ba,0xffdce5e6);
        p.setShader(new RadialGradient(48,40,42,new int[]{0xfff8fbfd,0xff9cadb9,0xffdae7ef,0xff687e8c},new float[]{0,.35f,.7f,1},Shader.TileMode.CLAMP));c.drawCircle(60,55,36,p);p.setShader(null);circle(60,55,11,0xff556e7b);circle(60,55,5,0xffd6e2e6);
        c.save();c.rotate(-28,101,72);plate(58,67,106,75,2,0xfff4f8fb,0xff7a8f9d);c.restore();circle(101,72,8,0xff829aa7);circle(101,72,3,0xffd4e1e6);
        long total=0,free=0;try{android.os.StatFs stat=new android.os.StatFs(android.os.Environment.getDataDirectory().getPath());total=stat.getTotalBytes();free=stat.getAvailableBytes();}catch(RuntimeException ignored){}
        for(int[] xy:new int[][]{{17,19},{115,19},{17,96},{115,96}})bolt(xy[0],xy[1]);plate(20,92,112,108,2,0xfff6f5e9,0xffd6dedb);text(total==0?"Unavailable":android.text.format.Formatter.formatFileSize(view.getContext(),free)+" free",66,103,9,INK,true);
        rect(18,113,106,118,0xff3d4f57);if(total>0)rect(18,113,18+88*(1-free/(float)total),118,0xff6dbec8);circle(115,116,2,0xff9af171);
    }
    private void network(){
        plate(4,5,128,131,7,0xff82979e,0xff293f48);plate(11,12,121,99,3,0xff0c252c,0xff123e40);text("NETWORK ACTIVITY",66,26,8,0xff94c2bb,true);
        for(int x=15;x<119;x+=13)line(x,33,x,91,0x335cb9a4,.5f);for(int y=35;y<95;y+=12)line(15,y,117,y,0x335cb9a4,.5f);
        GadgetHardware hw=view.hardware;
        if(hw!=null&&!hw.history.isEmpty()){double max=1024;for(double rate:hw.history)max=Math.max(max,rate);Path graph=new Path();int i=0;for(double rate:hw.history){float x=16+i++*100f/31,y=(float)(88-rate/max*49);if(i==1)graph.moveTo(x,y);else graph.lineTo(x,y);}p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.5f);p.setColor(0xff87f3b1);c.drawPath(graph,p);p.setStyle(Paint.Style.FILL);}
        text("↓ "+GadgetHardware.speed(hw==null?-1:hw.down),66,112,10,0xffb5f4ce,true);text("↑ "+GadgetHardware.speed(hw==null?-1:hw.up),66,125,9,0xffa7d8e8,false);
    }
    private void compass(){
        plate(35,3,97,15,4,0xffdeb96b,0xff755225);p.setShader(new LinearGradient(0,14,0,135,new int[]{0xfff1d999,0xff8b682d,0xffc6a56a},null,Shader.TileMode.CLAMP));c.drawCircle(66,74,59,p);p.setShader(null);circle(66,74,53,0xffece2bd);circle(66,74,48,0xfffff7dc);
        for(int i=0;i<72;i++){double a=i*Math.PI/36;line(66+(float)Math.sin(a)*43,74-(float)Math.cos(a)*43,66+(float)Math.sin(a)*(i%3==0?38:41),74-(float)Math.cos(a)*(i%3==0?38:41),0xff8c805c,.6f);}
        for(int i=0;i<4;i++){double a=i*Math.PI/2;text(new String[]{"N","E","S","W"}[i],66+(float)Math.sin(a)*31,78-(float)Math.cos(a)*31,11,i==0?0xff9e3f32:0xff696644,true);}
        float heading=view.hardware==null?Float.NaN:view.hardware.heading;if(Float.isFinite(heading)){c.save();c.rotate(-heading,66,74);Path needle=new Path();needle.moveTo(66,40);needle.lineTo(58,75);needle.lineTo(74,75);needle.close();fill(0xffb34635);c.drawPath(needle,p);needle.reset();needle.moveTo(66,108);needle.lineTo(58,75);needle.lineTo(74,75);needle.close();fill(0xff547a8b);c.drawPath(needle,p);c.restore();}circle(66,74,4,0xffbc9553);
        text(Float.isFinite(heading)?Math.round(heading)+"° magnetic":"Waiting for sensor",66,145,10,WHITE,true);
    }
    private void puzzle(){
        plate(4,4,128,152,7,0xffb58e5c,0xff573c28);plate(10,10,122,122,3,0xff483927,0xff746046);int[] board=TilePuzzle.board(data);
        int[] tops={0xffe0b15d,0xff94b894,0xff85b3c7,0xffb9a5c9},bottoms={0xff9d6432,0xff547b58,0xff44718e,0xff77628e};
        for(int i=0;i<16;i++)if(board[i]!=0){int x=13+i%4*27,y=13+i/4*27,color=(board[i]-1)/4;plate(x,y,x+25,y+25,3,tops[color],bottoms[color]);text(Integer.toString(board[i]),x+12.5f,y+18,13,0xfffffae9,true);line(x+3,y+2,x+22,y+2,0x88fff4d2,.7f);}
        text("TILE PUZZLE",66,135,9,0xfff0d8b1,true);text(data.optInt("moves",0)+" moves  •  Tap to play",66,146,7,0xffdcc49b,false);
    }
}
