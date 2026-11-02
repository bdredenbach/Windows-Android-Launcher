package com.nthexperience.desktop;

/** Pixel geometry shared by the desktop grid and the settings preview. */
final class DesktopMetrics {
    static int cell(int available,int requested,int automatic,int minimum){
        if(requested<=0)return Math.max(1,Math.min(Math.max(1,available),automatic));
        int count=Math.max(1,Math.min(requested,Math.max(1,available/Math.max(1,minimum))));
        return Math.max(1,available/count);
    }
    static int icon(int requested,int width,int height,int horizontalPadding,int verticalPadding,int labelHeight){
        return Math.max(1,Math.min(requested,Math.min(Math.max(1,width-horizontalPadding),Math.max(1,height-verticalPadding-labelHeight))));
    }
}
