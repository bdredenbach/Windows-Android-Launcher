package com.nthexperience.desktop;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.*;

/** Floating windows are for our built-in tools; Android apps launch normally. */
final class DesktopWindow extends LinearLayout {
    private final FrameLayout host;
    final View body;
    private final Runnable changed;
    private final TextView maximizeButton;
    private final TextView titleLabel;
    private boolean maximized;
    private int normalW, normalH,safeTop,safeRight;
    private float positionX=.5f,positionY=.33f;
    private final DesktopStore store;
    private String geometryKey,captionKind="";
    final String toolId;
    private float downX, downY, originX, originY;
    private final AeroGlass glass;
    private final TextView minimizeButton,closeButton;
    private boolean active=true,minimizing;
    private int transition;
    DesktopWindow(Context context, FrameLayout host, String title, View body, AeroWallpaper wallpaper,Runnable changed,DesktopStore store,String toolId,String layoutKey) {
        super(context);this.body=body; this.host=host; this.changed=changed;this.store=store;this.toolId=toolId;geometryKey="window_"+layoutKey+"_"+toolId;
        setOrientation(VERTICAL); setElevation(Aero.dp(context,16));
        glass=new AeroGlass(wallpaper,AeroGlass.WINDOW);setBackground(glass);
        setPadding(Aero.dp(context,6),Aero.dp(context,2),Aero.dp(context,6),Aero.dp(context,6));
        LinearLayout bar=new LinearLayout(context); bar.setGravity(Gravity.CENTER_VERTICAL);
        String icon=toolId.equals("@welcome")?"orb":toolId.startsWith("folder:")?"folderapps":toolId.equals("@backup")?"drive":toolId.equals("@organize")||toolId.equals("@quicklaunch")?"folderapps":toolId.equals("@control")?"control":toolId.equals("@appearance")?"appearance":toolId.equals("@files")?"files":toolId.equals("@filemanagers")?"folderapps":toolId.equals("@computer")||toolId.equals("@system")?"computer":toolId.equals("@notes")?"notes":toolId.equals("@photos")?"pictures":toolId.equals("@browser")?"browser":GadgetCatalog.toolIcon(toolId);LinearLayout.LayoutParams iconParams=new LinearLayout.LayoutParams(Aero.dp(context,18),Aero.dp(context,18));iconParams.leftMargin=Aero.dp(context,5);bar.addView(new AeroIcon(context,icon),iconParams);
        TextView label=Aero.text(context,title,13,0xff111b22);titleLabel=label;label.setSingleLine();label.setEllipsize(android.text.TextUtils.TruncateAt.END);label.setLayerType(View.LAYER_TYPE_SOFTWARE,null);label.setShadowLayer(Aero.dp(context,6),0,0,0xffffffff); label.setPadding(Aero.dp(context,8),0,0,0);
        bar.addView(label,new LayoutParams(0,Aero.dp(context,48),1));
        TextView minimize=chrome("—",this::minimize);minimizeButton=minimize;
        maximizeButton=chrome("□",this::toggleMaximize);
        TextView close=chrome("×",()->{ settle();hideKeyboard();if(body instanceof VistaShell)((VistaShell)body).close();if(body instanceof WeatherPanel)((WeatherPanel)body).close();if(body instanceof BrowserPanel)((BrowserPanel)body).close();if(body instanceof PhotoGallery)((PhotoGallery)body).close();host.removeView(this); changed.run(); });closeButton=close;
        close.setTextColor(Color.WHITE);
        close.setBackground(Aero.gloss(context,new int[]{0xffefb2a3,0xffcf6550,0xffa92d18,0xffc14f33},3,0xff64362f));
        bar.addView(minimize); bar.addView(maximizeButton); bar.addView(close);
        addView(bar,new LayoutParams(-1,Aero.dp(context,48)));
        body.setBackgroundColor(0xfff7f8f9); addView(body,new LayoutParams(-1,0,1));
        label.setOnTouchListener((v,e)->{
            if(e.getActionMasked()==MotionEvent.ACTION_DOWN){ settle();bringToFront();changed.run();downX=e.getRawX(); downY=e.getRawY(); originX=getX(); originY=getY(); return true; }
            if(e.getActionMasked()==MotionEvent.ACTION_MOVE){
                if(!maximized){ setX(clamp(originX+e.getRawX()-downX,0,Math.max(0,hostWidth()-getWidth()))); setY(clamp(originY+e.getRawY()-downY,safeTop,Math.max(safeTop,host.getHeight()-getHeight())));invalidate(); }
                return true;
            }
            if(e.getActionMasked()==MotionEvent.ACTION_UP){if(!maximized){positionX=DesktopGeometry.fraction(getX(),hostWidth()-getWidth());positionY=DesktopGeometry.fraction(getY()-safeTop,host.getHeight()-safeTop-getHeight());saveGeometry();}v.performClick();return true;}
            if(e.getActionMasked()==MotionEvent.ACTION_CANCEL){fitToHost();return true;}
            return true;
        });
        label.setContentDescription(title+". Drag to move window.");
    }
    boolean captionIcon(String kind){if(captionKind.equals(kind))return false;captionKind=kind;ViewGroup chrome=(ViewGroup)getChildAt(0);View old=chrome.getChildAt(0);ViewGroup.LayoutParams params=old.getLayoutParams();chrome.removeViewAt(0);chrome.addView(new AeroIcon(getContext(),kind),0,params);return true;}
    String caption(){return titleLabel.getText().toString();}
    void caption(String title){titleLabel.setText(title);titleLabel.setContentDescription(title+". Drag to move window.");}
    boolean minimizing(){return minimizing;}
    boolean active(){return active;}
    void active(boolean value){active=value;glass.active(value);titleLabel.setTextColor(value?0xff111b22:0xff465563);titleLabel.setShadowLayer(Aero.dp(getContext(),value?6:2),0,0,value?0xffffffff:0x99ffffff);minimizeButton.setAlpha(value?1:.65f);maximizeButton.setAlpha(value?1:.65f);closeButton.setAlpha(value?1:.6f);}
    @Override public boolean dispatchTouchEvent(MotionEvent e){if(e.getActionMasked()==MotionEvent.ACTION_DOWN&&!minimizing){bringToFront();changed.run();}return super.dispatchTouchEvent(e);}
    private TextView chrome(String symbol,Runnable action){
        TextView view=Aero.button(getContext(),symbol,action);
        view.setTextSize(16);view.setMinHeight(0);view.setPadding(0,0,0,0);LayoutParams lp=new LayoutParams(Aero.dp(getContext(),40),Aero.dp(getContext(),30));lp.leftMargin=Aero.dp(getContext(),1);view.setLayoutParams(lp);
        view.setBackground(Aero.gloss(getContext(),new int[]{0xbbc4deeb,0xaa8daabd,0x99738c9e,0xaab0c9d6},3,0xff627b8c));
        view.setContentDescription(symbol.equals("×")?"Close window":symbol.equals("—")?"Minimize window":"Maximize or restore window");
        return view;
    }
    void open(){
        int margin=Aero.dp(getContext(),12);
        int width=Math.min(Aero.dp(getContext(),520),hostWidth()-margin*2);
        int height=Math.min(Aero.dp(getContext(),490),host.getHeight()-safeTop-margin*2);
        FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(Math.max(1,width),Math.max(1,height));
        normalW=width;normalH=height;host.addView(this,lp); setX((hostWidth()-width)/2f); setY(safeTop+Math.max(margin,(host.getHeight()-safeTop-height)/3f));
        if((toolId.equals("@browser")||toolId.equals("@photos"))&&!store.prefs.contains(geometryKey))maximized=true;
        readGeometry(null);maximizeButton.setText(maximized?"❐":"□");fitToHost();changed.run();
    }
    private void hideKeyboard(){
        ((android.view.inputmethod.InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(getWindowToken(),0);
        clearFocus();
    }
    void settle(){transition++;animate().withEndAction(null).cancel();minimizing=false;setAlpha(1);setScaleX(1);setScaleY(1);setPivotX(getWidth()/2f);setPivotY(getHeight()/2f);}
    void minimize(){hideKeyboard();settle();if(!Aero.motion(getContext())){setVisibility(GONE);changed.run();return;}minimizing=true;int token=++transition;setPivotX(getWidth()*.2f);setPivotY(host.getHeight()-getY()+Aero.dp(getContext(),20));changed.run();animate().scaleX(.15f).scaleY(.15f).alpha(0).setDuration(180).withEndAction(()->{if(token!=transition)return;setVisibility(GONE);settle();changed.run();}).start();}
    void restore(){boolean hidden=getVisibility()!=VISIBLE||minimizing;settle();setVisibility(VISIBLE);bringToFront();changed.run();if(hidden&&Aero.motion(getContext())){int token=++transition;setPivotX(getWidth()*.2f);setPivotY(host.getHeight()-getY()+Aero.dp(getContext(),20));setScaleX(.18f);setScaleY(.18f);setAlpha(0);animate().scaleX(1).scaleY(1).alpha(1).setDuration(190).withEndAction(()->{if(token==transition)settle();}).start();}}
    @Override protected void onDetachedFromWindow(){settle();super.onDetachedFromWindow();}
    private int hostWidth(){return Math.max(1,host.getWidth()-safeRight);}
    void safeRight(int inset){int next=Math.max(0,Math.min(inset,Math.max(0,host.getWidth()-Aero.dp(getContext(),180))));if(safeRight!=next){safeRight=next;fitToHost();}}
    void safeTop(int inset){safeTop=Math.max(0,inset);fitToHost();}
    void fitToHost(){
        if(getParent()==null)return;
        FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)getLayoutParams();
        int margin=Aero.dp(getContext(),8);
        int width=maximized?hostWidth():Math.max(1,Math.min(normalW,hostWidth()-margin*2));
        int height=maximized?Math.max(1,host.getHeight()-safeTop):Math.max(1,Math.min(normalH,host.getHeight()-safeTop-margin*2));
        setX(maximized?0:positionX*Math.max(0,hostWidth()-width));
        setY(safeTop+(maximized?0:positionY*Math.max(0,host.getHeight()-safeTop-height)));
        // Called from the host layout listener. Reapplying identical parameters
        // would request another layout forever once any tool window is open.
        if(lp.width!=width||lp.height!=height){lp.width=width;lp.height=height;setLayoutParams(lp);}
    }
    private void toggleMaximize(){
        maximized=!maximized;maximizeButton.setText(maximized?"❐":"□");fitToHost();saveGeometry();changed.run();
    }
    JSONObject geometry(){
        JSONObject result=new JSONObject();float density=getResources().getDisplayMetrics().density;
        GadgetLayout.put(result,"x",positionX);GadgetLayout.put(result,"y",positionY);GadgetLayout.put(result,"w",normalW/density);GadgetLayout.put(result,"h",normalH/density);GadgetLayout.put(result,"maximized",maximized);return result;
    }
    void saveGeometry(){store.prefs.edit().putString(geometryKey,geometry().toString()).apply();}
    void orientation(String key){saveGeometry();JSONObject previous=geometry();geometryKey="window_"+key+"_"+toolId;readGeometry(previous);}
    void readGeometry(JSONObject fallback){
        JSONObject value=fallback;String saved=store.prefs.getString(geometryKey,null);if(saved!=null)try{value=new JSONObject(saved);}catch(JSONException ignored){}
        if(value==null)return;
        positionX=DesktopGeometry.clamp((float)value.optDouble("x",.5),0,1);positionY=DesktopGeometry.clamp((float)value.optDouble("y",.33),0,1);
        normalW=Aero.dp(getContext(),Math.round(DesktopGeometry.clamp((float)value.optDouble("w",520),180,900)));
        normalH=Aero.dp(getContext(),Math.round(DesktopGeometry.clamp((float)value.optDouble("h",490),180,900)));
        maximized=(fallback==null?value:fallback).optBoolean("maximized");maximizeButton.setText(maximized?"❐":"□");fitToHost();
    }
    private float clamp(float value,float low,float high){return Math.max(low,Math.min(high,value));}
}
