package com.nthexperience.desktop;

import android.content.Context;
import android.graphics.Rect;
import android.view.*;
import java.util.*;

/** Fixed desktop columns. A camera hole excludes only the cells it actually touches. */
final class DesktopIconGrid extends ViewGroup {
    private int tileWidth,tileHeight,viewportHeight,plannedX,plannedY;
    private final List<Rect> cutouts=new ArrayList<>();
    private final List<Rect> positions=new ArrayList<>();
    DesktopIconGrid(Context context){super(context);setWillNotDraw(false);setLayerType(LAYER_TYPE_SOFTWARE,null);}
    void configure(int width,int height,List<Rect> exclusions){
        if(tileWidth==width&&tileHeight==height&&cutouts.equals(exclusions))return;
        tileWidth=width;tileHeight=height;cutouts.clear();for(Rect r:exclusions)cutouts.add(new Rect(r));requestLayout();
    }
    void viewport(int height){if(viewportHeight!=height){viewportHeight=height;requestLayout();}}
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        int width=MeasureSpec.getSize(widthSpec),w=Math.max(1,tileWidth),h=Math.max(1,tileHeight);
        int columns=Math.max(1,width/w),rows=Math.max(1,Math.max(viewportHeight/h,(getChildCount()+columns-1)/columns));
        int[] origin=new int[2];getLocationOnScreen(origin);plannedX=origin[0];plannedY=origin[1];
        // More rows permit scrolling when camera exclusions leave too few cells.
        for(;;rows++){
            positions.clear();
            for(int column=0;column<columns&&positions.size()<getChildCount();column++)for(int row=0;row<rows&&positions.size()<getChildCount();row++){
                Rect cell=new Rect(column*w,row*h,(column+1)*w,(row+1)*h);
                Rect content=new Rect(cell);content.inset(Aero.dp(getContext(),7),Aero.dp(getContext(),3));content.offset(origin[0],origin[1]);
                boolean blocked=false;for(Rect hole:cutouts)if(Rect.intersects(content,hole)){blocked=true;break;}
                if(!blocked)positions.add(cell);
            }
            if(positions.size()==getChildCount())break;
        }
        for(int i=0;i<getChildCount();i++)getChildAt(i).measure(MeasureSpec.makeMeasureSpec(w,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(h,MeasureSpec.EXACTLY));
        setMeasuredDimension(width,resolveSize(Math.max(viewportHeight,rows*h),heightSpec));
    }
    @Override protected void onLayout(boolean changed,int l,int t,int r,int b){for(int i=0;i<getChildCount()&&i<positions.size();i++){Rect p=positions.get(i);getChildAt(i).layout(p.left,p.top,p.right,p.bottom);}int[] origin=new int[2];getLocationOnScreen(origin);if(origin[0]!=plannedX||origin[1]!=plannedY)post(this::requestLayout);}
    @Override protected LayoutParams generateDefaultLayoutParams(){return new LayoutParams(-2,-2);}
}
