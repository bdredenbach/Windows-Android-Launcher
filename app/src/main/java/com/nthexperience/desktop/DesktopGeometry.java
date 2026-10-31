package com.nthexperience.desktop;

/** Geometry in available-space fractions, independent of Android and screen density. */
public final class DesktopGeometry {
    private DesktopGeometry(){}
    public static float clamp(float value,float low,float high){return Float.isFinite(value)?Math.max(low,Math.min(high,value)):low;}
    public static float fraction(float position,float available){return available<=0?0:clamp(position/available,0,1);}
    public static float snap(float position,float grid){return grid>0?Math.round(position/grid)*grid:position;}
    public static int[] gadget(int viewportW,int viewportH,float baseW,float baseH,float gutter,float scale,float x,float y){
        int vw=Math.max(1,viewportW),vh=Math.max(1,viewportH);
        float s=clamp(scale,.65f,1.8f);
        s=Math.min(s,Math.min(Math.max(1,vw-gutter)/baseW,vh/baseH));
        int w=Math.max(1,Math.min(vw,Math.round(baseW*s+gutter))),h=Math.max(1,Math.min(vh,Math.round(baseH*s)));
        return new int[]{Math.round(clamp(x,0,1)*(vw-w)),Math.round(clamp(y,0,1)*(vh-h)),w,h};
    }
}
