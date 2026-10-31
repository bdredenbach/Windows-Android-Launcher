package com.nthexperience.desktop;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Floating windows are for our built-in tools; Android apps launch normally. */
final class DesktopWindow extends LinearLayout {
    private final FrameLayout host;
    private final Runnable changed;
    private final TextView maximizeButton;
    private boolean maximized;
    private int normalX, normalY, normalW, normalH;
    private float downX, downY, originX, originY;
    DesktopWindow(Context context, FrameLayout host, String title, View body, AeroWallpaper wallpaper,Runnable changed) {
        super(context); this.host=host; this.changed=changed;
        setOrientation(VERTICAL); setElevation(Aero.dp(context,16));
        setBackground(new AeroGlass(wallpaper,AeroGlass.WINDOW));
        setPadding(Aero.dp(context,6),Aero.dp(context,2),Aero.dp(context,6),Aero.dp(context,6));
        LinearLayout bar=new LinearLayout(context); bar.setGravity(Gravity.CENTER_VERTICAL);
        TextView label=Aero.text(context,title,13,0xff111b22);label.setShadowLayer(Aero.dp(context,3),0,0,0xffffffff); label.setPadding(Aero.dp(context,8),0,0,0);
        bar.addView(label,new LayoutParams(0,Aero.dp(context,48),1));
        TextView minimize=chrome("—",()->{ hideKeyboard();setVisibility(GONE); changed.run(); });
        maximizeButton=chrome("□",this::toggleMaximize);
        TextView close=chrome("×",()->{ hideKeyboard();host.removeView(this); changed.run(); });
        close.setTextColor(Color.WHITE);
        close.setBackground(Aero.gloss(context,new int[]{0xffefb2a3,0xffcf6550,0xffa92d18,0xffc14f33},3,0xff64362f));
        bar.addView(minimize); bar.addView(maximizeButton); bar.addView(close);
        addView(bar,new LayoutParams(-1,Aero.dp(context,48)));
        body.setBackgroundColor(0xfff7f8f9); addView(body,new LayoutParams(-1,0,1));
        label.setOnTouchListener((v,e)->{
            if(e.getActionMasked()==MotionEvent.ACTION_DOWN){ bringToFront(); downX=e.getRawX(); downY=e.getRawY(); originX=getX(); originY=getY(); return true; }
            if(e.getActionMasked()==MotionEvent.ACTION_MOVE){
                if(!maximized){ setX(clamp(originX+e.getRawX()-downX,0,Math.max(0,host.getWidth()-getWidth()))); setY(clamp(originY+e.getRawY()-downY,0,Math.max(0,host.getHeight()-getHeight())));invalidate(); }
                return true;
            }
            if(e.getActionMasked()==MotionEvent.ACTION_UP){ v.performClick(); return true; }
            return true;
        });
        label.setContentDescription(title+". Drag to move window.");
    }
    private TextView chrome(String symbol,Runnable action){
        TextView view=Aero.button(getContext(),symbol,action);
        view.setTextSize(16);view.setMinHeight(0);view.setPadding(0,0,0,0);LayoutParams lp=new LayoutParams(Aero.dp(getContext(),40),Aero.dp(getContext(),30));lp.leftMargin=Aero.dp(getContext(),1);view.setLayoutParams(lp);
        view.setBackground(Aero.gloss(getContext(),new int[]{0xbbc4deeb,0xaa8daabd,0x99738c9e,0xaab0c9d6},3,0xff627b8c));
        view.setContentDescription(symbol.equals("×")?"Close window":symbol.equals("—")?"Minimize window":"Maximize or restore window");
        return view;
    }
    void open(){
        int margin=Aero.dp(getContext(),12);
        int width=Math.min(Aero.dp(getContext(),520),host.getWidth()-margin*2);
        int height=Math.min(Aero.dp(getContext(),490),host.getHeight()-margin*2);
        FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(Math.max(1,width),Math.max(1,height));
        normalW=width;normalH=height;host.addView(this,lp); setX((host.getWidth()-width)/2f); setY(Math.max(margin,(host.getHeight()-height)/3f));
        changed.run();
    }
    private void hideKeyboard(){
        ((android.view.inputmethod.InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(getWindowToken(),0);
        clearFocus();
    }
    void restore(){setVisibility(VISIBLE);bringToFront();changed.run();}
    void fitToHost(){
        if(getParent()==null)return;
        FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)getLayoutParams();
        int margin=Aero.dp(getContext(),8);
        int width=maximized?host.getWidth():Math.max(1,Math.min(normalW,host.getWidth()-margin*2));
        int height=maximized?host.getHeight():Math.max(1,Math.min(normalH,host.getHeight()-margin*2));
        setX(clamp(getX(),0,Math.max(0,host.getWidth()-width)));
        setY(clamp(getY(),0,Math.max(0,host.getHeight()-height)));
        // Called from the host layout listener. Reapplying identical parameters
        // would request another layout forever once any tool window is open.
        if(lp.width!=width||lp.height!=height){lp.width=width;lp.height=height;setLayoutParams(lp);}
    }
    private void toggleMaximize(){
        FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)getLayoutParams();
        if(!maximized){ normalX=Math.round(getX());normalY=Math.round(getY());normalW=getWidth();normalH=getHeight();lp.width=host.getWidth();lp.height=host.getHeight();setX(0);setY(0); }
        else {lp.width=normalW;lp.height=normalH;setX(normalX);setY(normalY);}
        maximized=!maximized;maximizeButton.setText(maximized?"❐":"□");setLayoutParams(lp);fitToHost();
    }
    private float clamp(float value,float low,float high){return Math.max(low,Math.min(high,value));}
}
