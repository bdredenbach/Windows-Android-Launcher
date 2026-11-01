package com.nthexperience.desktop;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.text.SimpleDateFormat;
import java.util.*;

/** Live, resolution-independent gadget faces; none of the displayed readings are decorative. */
final class GadgetView extends View {
    static final String[] IDS=GadgetCatalog.IDS;
    final String kind;
    final DesktopStore store;
    final WeatherClient weather;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Path path=new Path();
    private final RectF box=new RectF();
    Bitmap photo;
    GadgetHardware hardware;
    int battery=-1,memory=-1;
    GadgetView(Context c,String kind,DesktopStore store,WeatherClient weather){super(c);this.kind=kind;this.store=store;this.weather=weather;setLayerType(View.LAYER_TYPE_SOFTWARE,null);setContentDescription(title(kind)+" gadget");setFocusable(true);}
    static String title(String id){return GadgetCatalog.title(id);}
    static int height(String id){switch(id){case "clock":return 128;case "calendar":return 142;case "weather":return 116;case "notes":return 138;case "photo":return 112;case "worldclock":return 150;case "feeds":return 156;case "checklist":return 150;case "timer":return 154;case "stopwatch":return 152;case "calculator":return 158;case "converter":return 136;case "storage":return 132;case "network":return 136;case "compass":return 152;case "puzzle":return 156;default:return 112;}}
    @Override protected void onDraw(Canvas c){
        float scale=Math.min(getWidth()/132f,getHeight()/(float)height(kind));c.save();c.translate((getWidth()-132*scale)/2,0);c.scale(scale,scale);p.setTypeface(Aero.face(getContext()));p.setStyle(Paint.Style.FILL);p.setShader(null);p.clearShadowLayer();p.setAlpha(255);
        switch(kind){case "clock":clock(c);break;case "calendar":calendar(c);break;case "weather":weather(c);break;case "notes":notes(c);break;case "photo":photo(c);break;case "meters":meters(c);break;default:extra(c);}
        c.restore();
    }
    private void extra(Canvas c){GadgetArtwork.draw(c,this);}
    private void clock(Canvas c){
        int style=store.prefs.getInt("clock_style",0);boolean dark=style==1;
        p.setShadowLayer(4,1,3,0xaa000000);gradient(0,3,0,125,new int[]{0xfff1f2ee,0xff53575a,0xffcfd3d4,0xff2b3034});c.drawCircle(66,63,59,p);p.clearShadowLayer();
        p.setShader(new RadialGradient(48,30,75,new int[]{0xfff6f8f9,0xff7c8b93,0xff343a40},null,Shader.TileMode.CLAMP));c.drawCircle(66,63,56,p);p.setShader(null);
        p.setColor(0xff1f252b);c.drawCircle(66,63,51,p);
        p.setShader(new RadialGradient(62,46,61,dark?new int[]{0xff47515b,0xff13191e}:new int[]{0xfffffff8,0xffe5e0cd},null,Shader.TileMode.CLAMP));c.drawCircle(66,63,49,p);p.setShader(null);
        c.save();c.translate(66,63);p.setStrokeWidth(1);
        for(int i=0;i<60;i++){p.setColor(dark?0xff9aa9b2:0xff5b584e);float a=(float)Math.toRadians(i*6);float inner=i%5==0?41:45;c.drawLine((float)Math.sin(a)*inner,-(float)Math.cos(a)*inner,(float)Math.sin(a)*47,-(float)Math.cos(a)*47,p);}
        for(int i=1;i<=12;i++){float a=(float)Math.toRadians(i*30);text(c,String.valueOf(i),(float)Math.sin(a)*33,-(float)Math.cos(a)*33+4,11,dark?0xffe3e9ec:0xff282825,true);}
        Calendar now=Calendar.getInstance();float minute=now.get(Calendar.MINUTE)+now.get(Calendar.SECOND)/60f;float hour=now.get(Calendar.HOUR)+minute/60f;
        hand(c,hour*30,25,3.4f,dark?0xffedf4f8:0xff27282b);hand(c,minute*6,37,2.3f,dark?0xffedf4f8:0xff27282b);
        if(store.prefs.getBoolean("clock_seconds",true))hand(c,now.get(Calendar.SECOND)*6,40,1,0xff9f3027);
        p.setColor(0xffd4c8aa);c.drawCircle(0,0,3,p);p.setColor(0xff272727);c.drawCircle(0,0,1.4f,p);c.restore();
        p.setShader(new LinearGradient(0,7,0,61,0x65ffffff,0x00ffffff,Shader.TileMode.CLAMP));c.drawOval(19,11,111,57,p);p.setShader(null);
    }
    private void hand(Canvas c,float degrees,float length,float width,int color){c.save();c.rotate(degrees);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeWidth(width);p.setColor(color);c.drawLine(0,6,0,-length,p);p.setStrokeCap(Paint.Cap.BUTT);c.restore();}
    private void calendar(Canvas c){
        round(c,8,4,124,137,4,new int[]{0xfff5f2e9,0xffddd9cd},0x88000000);
        round(c,8,4,124,37,4,new int[]{0xffe88b22,0xffc34f09},0xff713314);
        p.setColor(0xffe66c13);c.drawRect(9,27,123,39,p);text(c,new SimpleDateFormat("EEEE",Locale.getDefault()).format(new Date()),66,26,14,Color.WHITE,true);
        Calendar now=Calendar.getInstance();text(c,String.valueOf(now.get(Calendar.DAY_OF_MONTH)),66,99,66,0xff232323,true);
        text(c,new SimpleDateFormat("MMMM yyyy",Locale.getDefault()).format(new Date()),66,122,12,0xff625a4c,false);
        p.setColor(0x55ffffff);c.drawLine(10,6,122,6,p);p.setColor(0x555d4a35);c.drawLine(12,133,120,133,p);
    }
    private void weather(Canvas c){
        WeatherClient.Snapshot s=weather.snapshot();
        round(c,5,4,127,110,5,new int[]{0xffefa735,0xffbf7a1c,0xff955915},0xfff1c66f);
        p.setColor(0x2affffff);c.drawRoundRect(7,6,125,49,4,4,p);
        text(c,shorten(weather.city(),18),66,23,12,Color.WHITE,true);
        boolean cloud=s!=null&&(s.symbol.contains("cloud")||s.symbol.contains("rain")||s.symbol.contains("snow")||s.symbol.contains("fog"));
        sun(c,34,53,16,s!=null&&s.symbol.contains("night"));if(cloud)cloud(c,28,53);
        if(s==null){text(c,"—",94,66,31,Color.WHITE,true);text(c,weather.loading()?"Loading forecast…":weather.configured()?"Forecast unavailable":"Tap to set location",66,87,11,0xfffff4d9,false);}
        else {boolean f=store.prefs.getBoolean("weather_f",Locale.getDefault().getCountry().equals("US"));text(c,s.temperature(f)+"°",94,66,31,Color.WHITE,true);text(c,s.condition(),66,87,12,Color.WHITE,false);}
        String footer=s==null?"Weather · MET Norway":(weather.stale()?"Cached · ":"Updated · ")+android.text.format.DateFormat.getTimeFormat(getContext()).format(new Date(weather.updated()));
        text(c,footer,66,102,8,0xffffe4b7,false);
    }
    private void sun(Canvas c,float x,float y,float r,boolean night){
        if(night){p.setColor(0xffffedaf);c.drawCircle(x,y,r,p);p.setColor(0xffbe7a1c);c.drawCircle(x+7,y-6,r-2,p);return;}
        p.setColor(0xfffbe8a0);p.setStrokeWidth(2);for(int i=0;i<12;i++){double a=i*Math.PI/6;c.drawLine(x+(float)Math.cos(a)*(r+3),y+(float)Math.sin(a)*(r+3),x+(float)Math.cos(a)*(r+7),y+(float)Math.sin(a)*(r+7),p);}
        p.setShader(new RadialGradient(x-3,y-4,r,new int[]{0xfffff4bf,0xffffbd25},null,Shader.TileMode.CLAMP));c.drawCircle(x,y,r,p);p.setShader(null);
    }
    private void cloud(Canvas c,float x,float y){p.setShadowLayer(1,0,1,0x88595046);p.setShader(new LinearGradient(0,y-12,0,y+14,0xffffffff,0xffc7cbd0,Shader.TileMode.CLAMP));c.drawRoundRect(x-8,y+2,x+33,y+19,8,8,p);c.drawCircle(x+4,y+2,10,p);c.drawCircle(x+19,y,14,p);p.clearShadowLayer();p.setShader(null);}
    private void notes(Canvas c){
        p.setShadowLayer(4,1,3,0x99000000);gradient(0,3,0,132,new int[]{0xffffef9d,0xffffdf68});path.reset();path.moveTo(8,4);path.lineTo(123,4);path.lineTo(123,115);path.lineTo(104,133);path.lineTo(8,133);path.close();c.drawPath(path,p);p.clearShadowLayer();p.setShader(null);
        p.setColor(0xffdfb848);path.reset();path.moveTo(104,115);path.lineTo(123,115);path.lineTo(104,133);path.close();c.drawPath(path,p);
        p.setColor(0x559c761a);p.setStrokeWidth(.5f);for(int y=34;y<119;y+=18)c.drawLine(15,y,115,y,p);
        String note=store.prefs.getString("gadget_note","");if(note.isEmpty())note="A little note…\nTap to write.";
        p.setColor(0xff675321);p.setTextSize(12);p.setTypeface(Aero.face(getContext()));int y=28;
        for(String line:wrap(note,15)){if(y>115)break;c.drawText(line,16,y,p);y+=18;}
    }
    private void photo(Canvas c){
        round(c,3,6,129,104,5,new int[]{0xfff2e5c9,0xffa78549,0xffd7c19b,0xff70592f},0xff3a3021);
        round(c,10,13,122,97,1,new int[]{0xff342d22,0xff191918},0xfffaefd5);
        if(photo!=null){box.set(14,17,118,93);int save=c.save();c.clipRect(box);float scale=Math.max(box.width()/photo.getWidth(),box.height()/photo.getHeight());float w=photo.getWidth()*scale,h=photo.getHeight()*scale;p.setColor(Color.WHITE);c.drawBitmap(photo,null,new RectF(box.centerX()-w/2,box.centerY()-h/2,box.centerX()+w/2,box.centerY()+h/2),p);c.restoreToCount(save);}
        else {gradient(0,17,0,93,new int[]{0xff6dabc2,0xffdae0c1});c.drawRect(14,17,118,93,p);p.setShader(null);p.setColor(0xff426557);path.reset();path.moveTo(14,78);path.lineTo(45,46);path.lineTo(75,76);path.lineTo(94,56);path.lineTo(118,83);path.lineTo(118,93);path.lineTo(14,93);path.close();c.drawPath(path,p);text(c,"Add a photo",66,87,11,Color.WHITE,true);}
        p.setColor(0x22ffffff);c.drawRect(14,17,118,46,p);
    }
    private void meters(Canvas c){
        gauge(c,41,48,35,battery,"BATTERY");gauge(c,98,53,27,memory,"MEMORY");
        text(c,"Battery & memory",66,94,10,0xfff7fbff,true);
    }
    private void gauge(Canvas c,float x,float y,float r,int value,String label){
        p.setShadowLayer(3,0,2,0x88000000);p.setShader(new LinearGradient(0,y-r,0,y+r,new int[]{0xffefefed,0xff747a80,0xffc8cbce},null,Shader.TileMode.CLAMP));c.drawCircle(x,y,r,p);p.clearShadowLayer();p.setShader(null);p.setColor(0xffdadad3);c.drawCircle(x,y,r-3,p);p.setColor(0xfff4f2e9);c.drawCircle(x,y,r-6,p);
        c.save();c.translate(x,y);p.setStrokeWidth(1);for(int i=0;i<=10;i++){c.save();c.rotate(-140+i*28);p.setColor(i>8?0xffa34e2c:0xff697278);c.drawLine(0,-r+8,0,-r+12,p);c.restore();}
        if(value>=0)hand(c,-140+value*2.8f,r-12,1.5f,0xffa8402e);p.setColor(0xff5c6164);c.drawCircle(0,0,2,p);text(c,value<0?"—":value+"%",0,17,r>30?11:9,0xff303b40,true);text(c,label,0,-8,r>30?6:5,0xff686b68,false);c.restore();
    }
    private void round(Canvas c,float l,float t,float r,float b,float radius,int[] colors,int border){p.setShadowLayer(3,0,2,0x66000000);gradient(0,t,0,b,colors);c.drawRoundRect(l,t,r,b,radius,radius,p);p.clearShadowLayer();p.setShader(null);p.setColor(border);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(.8f);c.drawRoundRect(l,t,r,b,radius,radius,p);p.setStyle(Paint.Style.FILL);}
    private void gradient(float x,float y,float ex,float ey,int[] colors){p.setShader(new LinearGradient(x,y,ex,ey,colors,null,Shader.TileMode.CLAMP));}
    private void text(Canvas c,String value,float x,float y,float size,int color,boolean bold){p.setShader(null);p.setColor(color);p.setTypeface(bold?Aero.strong(getContext()):Aero.face(getContext()));p.setTextSize(size);p.setTextAlign(Paint.Align.CENTER);c.drawText(value,x,y,p);p.setTextAlign(Paint.Align.LEFT);}
    private static String shorten(String value,int count){return value.length()>count?value.substring(0,count-1)+"…":value;}
    private static List<String> wrap(String value,int width){List<String> result=new ArrayList<>();for(String raw:value.split("\n",-1)){while(raw.length()>width){int split=raw.lastIndexOf(' ',width);if(split<1)split=width;result.add(raw.substring(0,split));raw=raw.substring(split).trim();}result.add(raw);}return result;}
}
