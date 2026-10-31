package com.nthexperience.desktop;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.View;

/** Samples the softened wallpaper beneath the chrome, keeping foreground text sharp. */
final class AeroGlass extends Drawable {
    static final int WINDOW=0,START=1,TASKBAR=2,SIDEBAR=3;
    private final AeroWallpaper wallpaper;
    private final int style;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final RectF rect=new RectF();
    private final Path clip=new Path();
    AeroGlass(AeroWallpaper wallpaper,int style){this.wallpaper=wallpaper;this.style=style;wallpaper.register(this);}
    @Override public void draw(Canvas c){
        rect.set(getBounds());float d=getCallback() instanceof View?((View)getCallback()).getResources().getDisplayMetrics().density:1;
        float radius=(style==TASKBAR||style==SIDEBAR)?0:6*d;
        clip.reset();clip.addRoundRect(rect,radius,radius,Path.Direction.CW);int save=c.save();c.clipPath(clip);
        if(getCallback() instanceof View)wallpaper.drawGlass(c,(View)getCallback());
        int[] colors;
        if(style==WINDOW)colors=new int[]{0xb2bedbe8,0x88b2d3e4,0x9b7f9db1,0xccc0d9e7};
        else if(style==START)colors=new int[]{0xe147555e,0xdc263b49,0xe0162936,0xe82a414e};
        else if(style==TASKBAR)colors=new int[]{0xf343535b,0xe72b3e4a,0xf012222c,0xee0c1922};
        else colors=new int[]{0x33212b33,0x4423313e,0x55202a34,0x55202a34};
        p.setShader(new LinearGradient(0,rect.top,0,rect.bottom,colors,null,Shader.TileMode.CLAMP));p.setAlpha(255);c.drawRect(rect,p);p.setShader(null);
        if(style!=SIDEBAR){
            p.setShader(new LinearGradient(0,0,rect.width(),rect.height(),new int[]{0x00ffffff,0x20ffffff,0x00ffffff,0x10ffffff},new float[]{0,.23f,.45f,1},Shader.TileMode.CLAMP));
            Path beam=new Path();beam.moveTo(rect.width()*.05f,0);beam.lineTo(rect.width()*.35f,0);beam.lineTo(rect.width()*.78f,rect.height());beam.lineTo(rect.width()*.48f,rect.height());beam.close();c.drawPath(beam,p);p.setShader(null);
        }
        c.restoreToCount(save);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(d);p.setColor(style==WINDOW?0xc5f3fbff:0x90b8ced9);c.drawRoundRect(rect.left+d*.5f,rect.top+d*.5f,rect.right-d*.5f,rect.bottom-d*.5f,radius,radius,p);
        p.setColor(0x55000000);c.drawLine(rect.left,rect.bottom-d,rect.right,rect.bottom-d,p);p.setStyle(Paint.Style.FILL);
    }
    @Override public void setAlpha(int alpha){p.setAlpha(alpha);}
    @Override public void setColorFilter(ColorFilter filter){p.setColorFilter(filter);}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
