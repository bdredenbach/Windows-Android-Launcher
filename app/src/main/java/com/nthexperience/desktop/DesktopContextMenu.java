package com.nthexperience.desktop;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.StateListDrawable;
import android.view.*;
import android.widget.*;

/** A pointer-anchored Vista menu, shared by desktop long-press and mouse right-click. */
final class DesktopContextMenu {
    private final Context context;
    private final LinearLayout items;
    private final PopupWindow popup;
    DesktopContextMenu(Context context){
        this.context=context;items=Aero.column(context);items.setContentDescription("Desktop context menu");
        ScrollView scroll=new ScrollView(context);scroll.setFillViewport(false);scroll.setBackground(Aero.panel(context,0xfffafafa,0xfff0f0f0,1,0xff777777));scroll.setPadding(dp(2),dp(2),dp(2),dp(2));scroll.addView(items);
        popup=new PopupWindow(scroll,dp(238),-2,true);popup.setBackgroundDrawable(Aero.panel(context,0xfff8f8f8,0xffeeeeee,1,0xff777777));popup.setOutsideTouchable(true);popup.setElevation(dp(8));popup.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
    }
    void item(String title,String icon,Runnable action){
        LinearLayout row=new LinearLayout(context);row.setGravity(Gravity.CENTER_VERTICAL);row.setFocusable(true);row.setContentDescription(title);
        StateListDrawable hover=new StateListDrawable();for(int state:new int[]{android.R.attr.state_pressed,android.R.attr.state_focused,android.R.attr.state_hovered})hover.addState(new int[]{state},Aero.panel(context,0xffedf7ff,0xffcce5f7,2,0xff80aed0));hover.addState(new int[]{},Aero.panel(context,0x00ffffff,0x00ffffff,0,0));row.setBackground(hover);
        FrameLayout gutter=new FrameLayout(context);gutter.setBackground(Aero.panel(context,0xfff0f2f4,0xffe8ecef,0,0));View image;
        if(icon.equals("refresh")){TextView glyph=Aero.text(context,"↻",20,0xff4b86ab);glyph.setGravity(Gravity.CENTER);image=glyph;}else image=new AeroIcon(context,icon);
        gutter.addView(image,new FrameLayout.LayoutParams(dp(20),dp(20),Gravity.CENTER));row.addView(gutter,new LinearLayout.LayoutParams(dp(31),-1));
        TextView label=Aero.text(context,title,13,0xff151a1f);label.setPadding(dp(9),0,dp(12),0);row.addView(label,new LinearLayout.LayoutParams(0,-1,1));row.setOnClickListener(v->{dismiss();action.run();});items.addView(row,new LinearLayout.LayoutParams(-1,dp(36)));
    }
    void separator(){LinearLayout row=new LinearLayout(context);row.setPadding(dp(34),dp(3),dp(5),dp(3));View line=new View(context);line.setBackgroundColor(0xffc6cbcf);row.addView(line,new LinearLayout.LayoutParams(-1,dp(1)));items.addView(row,new LinearLayout.LayoutParams(-1,dp(7)));}
    void show(View root,float screenX,float screenY,int leftInset,int topInset,int rightInset,int bottomInset){
        int[] location=new int[2];root.getLocationOnScreen(location);int left=location[0]+leftInset+dp(3),top=location[1]+topInset+dp(3),right=location[0]+root.getWidth()-rightInset-dp(3),bottom=location[1]+root.getHeight()-bottomInset-dp(3);
        int width=Math.min(dp(238),Math.max(1,right-left));popup.getContentView().measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(Math.max(1,bottom-top),View.MeasureSpec.AT_MOST));int height=Math.min(popup.getContentView().getMeasuredHeight(),Math.max(1,bottom-top));popup.setWidth(width);popup.setHeight(height);
        int x=Math.max(left,Math.min(Math.round(screenX),right-width)),y=Math.max(top,Math.min(Math.round(screenY),bottom-height));popup.showAtLocation(root,Gravity.TOP|Gravity.LEFT,x,y);
    }
    boolean showing(){return popup.isShowing();}
    void dismiss(){popup.dismiss();}
    private int dp(int value){return Aero.dp(context,value);}
}
