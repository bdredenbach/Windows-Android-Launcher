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
    DesktopWindow(Context context, FrameLayout host, String title, View body, Runnable changed) {
        super(context); this.host=host; this.changed=changed;
        setOrientation(VERTICAL); setElevation(Aero.dp(context,16));
        setBackground(Aero.panel(context,0xeea6d3e6,0xf0e7eff5,9,0x99ffffff));
        setPadding(Aero.dp(context,5),Aero.dp(context,4),Aero.dp(context,5),Aero.dp(context,5));
        LinearLayout bar=new LinearLayout(context); bar.setGravity(Gravity.CENTER_VERTICAL);
        TextView label=Aero.text(context,title,15,Aero.INK); Aero.bold(label); label.setPadding(Aero.dp(context,8),0,0,0);
        bar.addView(label,new LayoutParams(0,Aero.dp(context,48),1));
        TextView minimize=chrome("—",()->{ hideKeyboard();setVisibility(GONE); changed.run(); });
        maximizeButton=chrome("□",this::toggleMaximize);
        TextView close=chrome("×",()->{ hideKeyboard();host.removeView(this); changed.run(); });
        close.setTextColor(0xff963a35);
        bar.addView(minimize); bar.addView(maximizeButton); bar.addView(close);
        addView(bar,new LayoutParams(-1,Aero.dp(context,48)));
        body.setBackgroundColor(0xfff6f9fb); addView(body,new LayoutParams(-1,0,1));
        label.setOnTouchListener((v,e)->{
            if(e.getActionMasked()==MotionEvent.ACTION_DOWN){ bringToFront(); downX=e.getRawX(); downY=e.getRawY(); originX=getX(); originY=getY(); return true; }
            if(e.getActionMasked()==MotionEvent.ACTION_MOVE){
                if(!maximized){ setX(clamp(originX+e.getRawX()-downX,0,Math.max(0,host.getWidth()-getWidth()))); setY(clamp(originY+e.getRawY()-downY,0,Math.max(0,host.getHeight()-getHeight()))); }
                return true;
            }
            if(e.getActionMasked()==MotionEvent.ACTION_UP){ v.performClick(); return true; }
            return true;
        });
        label.setContentDescription(title+". Drag to move window.");
    }
    private TextView chrome(String symbol,Runnable action){
        TextView view=Aero.button(getContext(),symbol,action);
        view.setTextSize(19); view.setLayoutParams(new LayoutParams(Aero.dp(getContext(),48),Aero.dp(getContext(),44)));
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
        lp.width=maximized?host.getWidth():Math.max(1,Math.min(normalW,host.getWidth()-margin*2));
        lp.height=maximized?host.getHeight():Math.max(1,Math.min(normalH,host.getHeight()-margin*2));
        setX(clamp(getX(),0,Math.max(0,host.getWidth()-lp.width)));
        setY(clamp(getY(),0,Math.max(0,host.getHeight()-lp.height)));
        setLayoutParams(lp);
    }
    private void toggleMaximize(){
        FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)getLayoutParams();
        if(!maximized){ normalX=Math.round(getX());normalY=Math.round(getY());normalW=getWidth();normalH=getHeight();lp.width=host.getWidth();lp.height=host.getHeight();setX(0);setY(0); }
        else {lp.width=normalW;lp.height=normalH;setX(normalX);setY(normalY);}
        maximized=!maximized;maximizeButton.setText(maximized?"❐":"□");setLayoutParams(lp);fitToHost();
    }
    private float clamp(float value,float low,float high){return Math.max(low,Math.min(high,value));}
}
