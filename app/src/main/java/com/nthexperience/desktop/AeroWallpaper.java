package com.nthexperience.desktop;

import android.content.Context;
import android.graphics.*;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;

/** Original, resolution-independent aurora artwork. No Microsoft assets. */
final class AeroWallpaper extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private int theme;
    private static Bitmap artwork;
    private final java.util.List<java.lang.ref.WeakReference<AeroGlass>> surfaces=new java.util.ArrayList<>();
    private Bitmap softened;
    private int softWidth,softHeight,softTheme=-1;
    private final int[] ownLocation=new int[2],targetLocation=new int[2];
    AeroWallpaper(Context context, int theme) { super(context); this.theme = theme;
        if(artwork==null){try{artwork=BitmapFactory.decodeStream(context.getAssets().open("wallpapers/aero-aurora.webp"));}catch(java.io.IOException ignored){}} setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO); }
    void theme(int value) { theme = value; softened=null; invalidate();for(java.util.Iterator<java.lang.ref.WeakReference<AeroGlass>> i=surfaces.iterator();i.hasNext();){AeroGlass glass=i.next().get();if(glass==null)i.remove();else glass.invalidateSelf();} }
    void register(AeroGlass glass){surfaces.add(new java.lang.ref.WeakReference<>(glass));}
    @Override protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        if(artwork!=null){
            drawArtwork(c,w,h);return;
        }
        int[][] colors = {{0xff08263f,0xff156d82,0xff79bcb1}, {0xff141c41,0xff49427c,0xffbc8cae}, {0xff102c30,0xff347e6c,0xffc4d8aa}};
        int[] palette = colors[Math.floorMod(theme, colors.length)];
        paint.setShader(new LinearGradient(0, 0, w*.8f, h, palette, null, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, paint);
        paint.setShader(new RadialGradient(w*.88f, h*.47f, h*.66f, new int[]{0x887ae8de,0x0024b6cc}, null, Shader.TileMode.CLAMP));
        c.drawRect(0,0,w,h,paint);
        path.reset(); path.moveTo(-w*.3f,h*.82f);
        path.cubicTo(w*.1f,h*.16f,w*.9f,h*.84f,w*1.3f,h*.14f);
        path.cubicTo(w*.75f,h*.98f,w*.27f,h*.45f,-w*.3f,h*1.1f); path.close();
        paint.setShader(new LinearGradient(0,h*.25f,w,h, new int[]{0x002ae3ca,0x6684f6bf,0x08c4f9d9},null,Shader.TileMode.CLAMP));
        c.drawPath(path,paint);
        path.reset(); path.moveTo(-w*.1f,h*.91f);
        path.cubicTo(w*.26f,h*.5f,w*.8f,h*.86f,w*1.2f,h*.36f);
        path.cubicTo(w*.78f,h*1.02f,w*.27f,h*.72f,-w*.1f,h*1.07f); path.close();
        paint.setShader(new LinearGradient(0,h*.5f,w,h,new int[]{0x001c8faf,0x668fdcdf,0x00c4f9d9},null,Shader.TileMode.CLAMP));
        c.drawPath(path,paint);
        paint.setShader(null); paint.setColor(0x11ffffff); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(1.5f);
        path.reset(); path.moveTo(-w*.2f,h*.9f); path.cubicTo(w*.22f,h*.38f,w*.88f,h*.9f,w*1.2f,h*.27f);
        c.drawPath(path,paint); paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x08ffffff);
        for(int i=0;i<24;i++) { float x=((i*167+41)%997)/997f*w, y=((i*107+73)%997)/997f*h; c.drawCircle(x,y,1+(i%3),paint); }
    }
    private void drawArtwork(Canvas c,float w,float h){
        float scale=Math.max(w/artwork.getWidth(),h/artwork.getHeight());
        float rw=artwork.getWidth()*scale,rh=artwork.getHeight()*scale;
        paint.setShader(null);paint.setFilterBitmap(true);
        if(theme==1){float[] m={.65f,0,.45f,0,10, 0,.5f,0,0,0, .35f,0,1,0,15, 0,0,0,1,0};paint.setColorFilter(new ColorMatrixColorFilter(m));}
        else if(theme==2){float[] m={.55f,.25f,0,0,0, .05f,.9f,0,0,0, 0,.25f,.5f,0,0, 0,0,0,1,0};paint.setColorFilter(new ColorMatrixColorFilter(m));}
        else paint.setColorFilter(null);
        c.drawBitmap(artwork,null,new RectF((w-rw)/2,(h-rh)/2,(w+rw)/2,(h+rh)/2),paint);paint.setColorFilter(null);
    }
    void drawGlass(Canvas canvas,View target){
        if(getWidth()<1||getHeight()<1)return;
        if(softened==null||softWidth!=getWidth()||softHeight!=getHeight()||softTheme!=theme){
            softWidth=getWidth();softHeight=getHeight();softTheme=theme;
            int w=Math.max(1,Math.round(getWidth()*96f/getHeight()));softened=Bitmap.createBitmap(w,96,Bitmap.Config.ARGB_8888);
            Canvas small=new Canvas(softened);small.scale(w/(float)getWidth(),96f/getHeight());onDraw(small);
        }
        getLocationOnScreen(ownLocation);target.getLocationOnScreen(targetLocation);
        float x=ownLocation[0]-targetLocation[0],y=ownLocation[1]-targetLocation[1];
        paint.setShader(null);paint.setAlpha(255);paint.setFilterBitmap(true);canvas.drawBitmap(softened,null,new RectF(x,y,x+getWidth(),y+getHeight()),paint);
    }

}
