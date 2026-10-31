package com.nthexperience.desktop;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** A small original icon family drawn with Android primitives. */
final class AeroIcon extends View {
    private final String kind;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    AeroIcon(Context c, String kind) { super(c); this.kind = kind; setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO); }
    @Override protected void onDraw(Canvas canvas) {
        float s = Math.min(getWidth(),getHeight())/64f;
        canvas.save(); canvas.translate((getWidth()-64*s)/2,(getHeight()-64*s)/2); canvas.scale(s,s);
        p.setShader(null); p.setStyle(Paint.Style.FILL);
        if (kind.equals("orb")) {
            p.setShader(new RadialGradient(24,16,45,new int[]{0xff75e1de,0xff2189b3,0xff092e61},null,Shader.TileMode.CLAMP));
            canvas.drawCircle(32,32,29,p); p.setShader(null);
            p.setColor(0xaaffffff); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1); canvas.drawCircle(32,32,28,p); p.setStyle(Paint.Style.FILL);
            p.setColor(0x55ffffff); canvas.drawOval(13,8,51,28,p);
            p.setColor(Color.WHITE); p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD)); p.setTextSize(34); canvas.drawText("N",20,46,p);
        } else if (kind.equals("computer")) {
            rounded(canvas,7,9,57,45,4,0xffc5d9e2,0xff7297ab);
            rounded(canvas,11,13,53,39,1,0xff4bb7ce,0xff133d6b);
            p.setColor(0xffbfd1dc); canvas.drawRect(27,45,37,53,p); rounded(canvas,18,52,46,56,2,0xffe3eaf0,0xff88a5b7);
            p.setColor(0x559ef5f0); canvas.drawOval(13,15,48,28,p);
        } else if(kind.equals("files")) {
            rounded(canvas,5,14,33,26,3,0xffffe3a0,0xffd59432);
            rounded(canvas,5,21,58,53,4,0xffffde79,0xffd9a13a);
            rounded(canvas,8,25,59,53,4,0xffffe9a1,0xffe3b247);
        } else if(kind.equals("browser")) {
            p.setShader(new LinearGradient(0,7,0,57,0xff85e0ea,0xff1671b0,Shader.TileMode.CLAMP)); canvas.drawCircle(32,31,24,p); p.setShader(null);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2); p.setColor(0xbbe4ffff); canvas.drawOval(20,7,44,55,p); canvas.drawOval(8,22,56,40,p); canvas.drawLine(9,31,55,31,p); p.setStyle(Paint.Style.FILL);
            p.setColor(0xffe8bc54); canvas.save(); canvas.rotate(-26,32,31); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(4); canvas.drawOval(1,22,63,43,p); p.setStyle(Paint.Style.FILL); canvas.restore();
        } else if(kind.equals("notes")) {
            canvas.save(); canvas.rotate(-7,32,32); rounded(canvas,13,7,53,57,3,0xffffffff,0xffd4e5ec);
            p.setColor(0xff4c8eaa); p.setStrokeWidth(2);
            for(int y=21;y<50;y+=7) canvas.drawLine(20,y,45,y,p);
            p.setColor(0xff86bfcf); canvas.drawRect(13,8,53,16,p); canvas.restore();
        } else if(kind.equals("appearance")) {
            rounded(canvas,8,9,55,48,4,0xffd4e3ec,0xff7f9daf);
            rounded(canvas,12,13,51,42,1,0xff71cfbf,0xff2b668c);
            p.setColor(0xfff0c96e); canvas.drawCircle(45,43,13,p);
            p.setColor(0xffffe1a2); canvas.drawCircle(42,39,7,p);
        } else {
            for(int i=0;i<4;i++) rounded(canvas,10+(i%2)*25,10+(i/2)*25,29+(i%2)*25,29+(i/2)*25,4,0xffa9ddec,0xff3b8fba);
        }
        canvas.restore();
    }
    private void rounded(Canvas c,float l,float t,float r,float b,int radius,int top,int bottom) {
        p.setShader(new LinearGradient(0,t,0,b,top,bottom,Shader.TileMode.CLAMP)); c.drawRoundRect(l,t,r,b,radius,radius,p); p.setShader(null);
    }
}
