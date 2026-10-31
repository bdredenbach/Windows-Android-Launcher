package com.nthexperience.desktop;

import android.app.ActivityManager;
import android.content.*;
import android.graphics.*;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class GadgetSidebar extends LinearLayout {
    interface Actions {void gadgetAction(String id);void gallery();void sidebarChanged();void readingsChanged();}
    private final DesktopStore store;
    private final Actions actions;
    final WeatherClient weather;
    private final LinearLayout gadgets;
    private final Map<String,GadgetView> faces=new LinkedHashMap<>();
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService photos=Executors.newSingleThreadExecutor();
    private Bitmap photo;
    private boolean active,closed;
    private String currentPhoto="";
    private long lastPhotoTime,lastMinute=-1;
    private int photoIndex;
    private final Runnable tick=new Runnable(){@Override public void run(){
        if(!active||closed||getVisibility()!=VISIBLE)return;
        long now=System.currentTimeMillis();GadgetView clock=faces.get("clock");if(clock!=null)clock.invalidate();
        long minute=now/60000;if(minute!=lastMinute){lastMinute=minute;refreshReadings();weather.refresh(false);for(GadgetView v:faces.values())if(!v.kind.equals("clock"))v.invalidate();}
        if(faces.containsKey("photo")&&now-lastPhotoTime>20000){lastPhotoTime=now;loadNextPhoto();}
        boolean seconds=clock!=null&&store.prefs.getBoolean("clock_seconds",true);main.postDelayed(this,seconds?1000-now%1000:60000-now%60000);
    }};
    GadgetSidebar(Context c,DesktopStore store,AeroWallpaper wallpaper,Actions actions){
        super(c);this.store=store;this.actions=actions;setOrientation(VERTICAL);setBackground(new AeroGlass(wallpaper,AeroGlass.SIDEBAR));
        weather=new WeatherClient(store,this::refresh);setContentDescription("Desktop gadget sidebar");
        LinearLayout header=new LinearLayout(c);header.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);
        TextView plus=Aero.text(c,"＋",23,0xffedf6ff);plus.setGravity(Gravity.CENTER);plus.setContentDescription("Add gadgets");plus.setOnClickListener(v->actions.gallery());header.addView(plus,new LayoutParams(Aero.dp(c,48),Aero.dp(c,36)));
        TextView hide=Aero.text(c,"›",25,0xffd5e5ee);hide.setGravity(Gravity.CENTER);hide.setContentDescription("Hide gadget sidebar");hide.setOnClickListener(v->{store.prefs.edit().putBoolean("sidebar_visible",false).apply();visibilityFromStore();actions.sidebarChanged();});header.addView(hide,new LayoutParams(Aero.dp(c,36),Aero.dp(c,36)));addView(header,new LayoutParams(-1,Aero.dp(c,36)));
        ScrollView scroll=new ScrollView(c);scroll.setVerticalScrollBarEnabled(false);scroll.setClipToPadding(false);gadgets=Aero.column(c);gadgets.setGravity(Gravity.CENTER_HORIZONTAL);gadgets.setPadding(Aero.dp(c,2),Aero.dp(c,1),Aero.dp(c,2),Aero.dp(c,16));scroll.addView(gadgets);addView(scroll,new LayoutParams(-1,0,1));rebuild();visibilityFromStore();
    }
    void rebuild(){
        gadgets.removeAllViews();faces.clear();
        for(String id:store.gadgets()){
            if(!Arrays.asList(GadgetView.IDS).contains(id))continue;
            GadgetView face=new GadgetView(getContext(),id,store,weather);face.photo=photo;face.setOnClickListener(v->actions.gadgetAction(id));face.setOnLongClickListener(v->face.startDragAndDrop(ClipData.newPlainText("gadget",id),new View.DragShadowBuilder(face),"gadget:"+id,0));face.setOnTouchListener((v,event)->{if(event.getActionMasked()==MotionEvent.ACTION_DOWN&&event.isButtonPressed(MotionEvent.BUTTON_SECONDARY)){menu(face,id);return true;}return false;});
            face.setOnDragListener((v,event)->{if(!(event.getLocalState() instanceof String)||!((String)event.getLocalState()).startsWith("gadget:"))return false;
                if(event.getAction()==DragEvent.ACTION_DRAG_ENTERED)v.setAlpha(.5f);if(event.getAction()==DragEvent.ACTION_DRAG_EXITED||event.getAction()==DragEvent.ACTION_DRAG_ENDED)v.setAlpha(1);
                if(event.getAction()==DragEvent.ACTION_DROP){String moving=((String)event.getLocalState()).substring(7);store.gadgets(LauncherState.move(store.gadgets(),moving,id));main.post(this::rebuild);}return true;});
            LayoutParams lp=new LayoutParams(Aero.dp(getContext(),132),Aero.dp(getContext(),GadgetView.height(id)));lp.bottomMargin=Aero.dp(getContext(),9);gadgets.addView(face,lp);faces.put(id,face);
        }
        if(faces.isEmpty()){TextView empty=Aero.text(getContext(),"Add a gadget\nto your Sidebar",13,0xffe8f2f7);empty.setGravity(Gravity.CENTER);Aero.pad(empty,14);empty.setOnClickListener(v->actions.gallery());gadgets.addView(empty);}
        refreshReadings();weather.active(active&&getVisibility()==VISIBLE&&faces.containsKey("weather"));restartTick();
    }
    void refresh(){for(GadgetView v:faces.values())v.invalidate();actions.readingsChanged();}
    void setActive(boolean value){active=value;weather.active(value&&getVisibility()==VISIBLE&&faces.containsKey("weather"));restartTick();}
    private void restartTick(){main.removeCallbacks(tick);if(active&&getVisibility()==VISIBLE)main.post(tick);}
    void visibilityFromStore(){setVisibility(store.prefs.getBoolean("sidebar_visible",true)?VISIBLE:GONE);weather.active(active&&getVisibility()==VISIBLE&&faces.containsKey("weather"));restartTick();}
    void reveal(){store.prefs.edit().putBoolean("sidebar_visible",true).apply();visibilityFromStore();actions.sidebarChanged();}
    boolean ticking(){return active&&getVisibility()==VISIBLE;}
    void menu(GadgetView face,String id){
        PopupMenu menu=new PopupMenu(getContext(),face);menu.getMenu().add("Open "+GadgetView.title(id)).setOnMenuItemClickListener(v->{actions.gadgetAction(id);return true;});
        menu.getMenu().add("Drag to rearrange").setOnMenuItemClickListener(v->{face.startDragAndDrop(ClipData.newPlainText("gadget",id),new View.DragShadowBuilder(face),"gadget:"+id,0);return true;});
        List<String> ids=store.gadgets();int index=ids.indexOf(id);
        if(index>0)menu.getMenu().add("Move up").setOnMenuItemClickListener(v->{Collections.swap(ids,index,index-1);store.gadgets(ids);rebuild();return true;});
        if(index>=0&&index<ids.size()-1)menu.getMenu().add("Move down").setOnMenuItemClickListener(v->{Collections.swap(ids,index,index+1);store.gadgets(ids);rebuild();return true;});
        menu.getMenu().add("Close gadget").setOnMenuItemClickListener(v->{ids.remove(id);store.gadgets(ids);rebuild();return true;});menu.show();
    }
    private void refreshReadings(){
        Intent battery=getContext().registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));int level=battery==null?-1:battery.getIntExtra(BatteryManager.EXTRA_LEVEL,-1),scale=battery==null?-1:battery.getIntExtra(BatteryManager.EXTRA_SCALE,-1);
        int percent=level<0||scale<=0?-1:Math.round(level*100f/scale);int used=-1;
        ActivityManager manager=(ActivityManager)getContext().getSystemService(Context.ACTIVITY_SERVICE);if(manager!=null){ActivityManager.MemoryInfo info=new ActivityManager.MemoryInfo();manager.getMemoryInfo(info);if(info.totalMem>0)used=Math.round((info.totalMem-info.availMem)*100f/info.totalMem);}
        for(GadgetView face:faces.values()){face.battery=percent;face.memory=used;}
    }
    void photosChanged(){currentPhoto="";photoIndex=0;loadNextPhoto();}
    private void loadNextPhoto(){
        List<String> uris=store.photos();if(uris.isEmpty())return;String uri=uris.get(photoIndex++%uris.size());if(uri.equals(currentPhoto))return;currentPhoto=uri;
        photos.execute(()->{Bitmap decoded=null;try{
            BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;try(InputStream in=getContext().getContentResolver().openInputStream(Uri.parse(uri))){BitmapFactory.decodeStream(in,null,options);}options.inSampleSize=1;while(options.outWidth/options.inSampleSize>900||options.outHeight/options.inSampleSize>900)options.inSampleSize*=2;options.inJustDecodeBounds=false;
            try(InputStream in=getContext().getContentResolver().openInputStream(Uri.parse(uri))){decoded=BitmapFactory.decodeStream(in,null,options);}
            if(decoded!=null){try(InputStream in=getContext().getContentResolver().openInputStream(Uri.parse(uri))){int orientation=new ExifInterface(in).getAttributeInt(ExifInterface.TAG_ORIENTATION,1);Matrix m=new Matrix();if(orientation==3)m.postRotate(180);if(orientation==6)m.postRotate(90);if(orientation==8)m.postRotate(270);if(orientation==2)m.postScale(-1,1);if(orientation==4)m.postScale(1,-1);if(orientation==5){m.postRotate(90);m.postScale(-1,1);}if(orientation==7){m.postRotate(270);m.postScale(-1,1);}if(!m.isIdentity()){Bitmap rotated=Bitmap.createBitmap(decoded,0,0,decoded.getWidth(),decoded.getHeight(),m,true);decoded=rotated;}}}
        }catch(IOException|RuntimeException ignored){}
            Bitmap result=decoded;main.post(()->{if(closed||!currentPhoto.equals(uri))return;photo=result;GadgetView face=faces.get("photo");if(face!=null){face.photo=result;face.invalidate();}});
        });
    }
    void close(){closed=true;active=false;main.removeCallbacksAndMessages(null);weather.close();photos.shutdownNow();}
}
