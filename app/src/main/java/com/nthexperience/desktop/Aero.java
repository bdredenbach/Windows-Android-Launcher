package com.nthexperience.desktop;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

final class Aero {
    static final int INK=0xff18232c;
    private static Typeface regular,semibold;
    static int dp(Context c,float value){return Math.round(value*c.getResources().getDisplayMetrics().density);}
    static Typeface face(Context c){
        if(regular==null){try{regular=Typeface.createFromAsset(c.getAssets(),"fonts/selawk.ttf");}catch(RuntimeException e){regular=Typeface.create("sans-serif",Typeface.NORMAL);}}
        return regular;
    }
    static Typeface strong(Context c){
        if(semibold==null){try{semibold=Typeface.createFromAsset(c.getAssets(),"fonts/selawksb.ttf");}catch(RuntimeException e){semibold=Typeface.create(face(c),Typeface.BOLD);}}
        return semibold;
    }
    static GradientDrawable panel(Context c,int top,int bottom,int radius,int stroke){
        GradientDrawable shape=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{top,bottom});shape.setCornerRadius(dp(c,radius));if(stroke!=0)shape.setStroke(dp(c,1),stroke);return shape;
    }
    static GradientDrawable gloss(Context c,int[] colors,float radius,int stroke){
        GradientDrawable shape=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,colors);shape.setCornerRadius(dp(c,radius));if(stroke!=0)shape.setStroke(dp(c,1),stroke);return shape;
    }
    static StateListDrawable selection(Context c){
        StateListDrawable states=new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed},gloss(c,new int[]{0xfff0faff,0xffd6eeff,0xffb6defa,0xffd9f1ff},3,0xff83b4d2));
        states.addState(new int[]{android.R.attr.state_focused},panel(c,0xffedf8ff,0xffd4eaff,3,0xff97bcd3));
        states.addState(new int[]{},panel(c,0x00ffffff,0x00ffffff,3,0));return states;
    }
    static TextView text(Context c,String value,float size,int color){TextView v=new TextView(c);v.setText(value);v.setTextSize(size);v.setTypeface(face(c));v.setTextColor(color);v.setGravity(Gravity.CENTER_VERTICAL);v.setIncludeFontPadding(false);return v;}
    static TextView button(Context c,String label,Runnable click){
        TextView v=text(c,label,13,INK);v.setGravity(Gravity.CENTER);v.setMinHeight(dp(c,44));v.setPadding(dp(c,10),dp(c,5),dp(c,10),dp(c,5));
        v.setBackground(new RippleDrawable(ColorStateList.valueOf(0x4468c7f4),gloss(c,new int[]{0xfffdfefe,0xffe9eef1,0xffd7dfe4,0xffeef2f5},3,0xff81929e),null));v.setOnClickListener(view->click.run());return v;
    }
    static LinearLayout column(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.VERTICAL);return l;}
    static void bold(TextView v){v.setTypeface(strong(v.getContext()));}
    static void pad(View v,int value){int p=dp(v.getContext(),value);v.setPadding(p,p,p,p);}
    private Aero(){}
}
