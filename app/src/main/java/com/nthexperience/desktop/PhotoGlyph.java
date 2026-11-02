package com.nthexperience.desktop;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Original toolbar artwork for the glass-era picture viewer. */
final class PhotoGlyph extends View {
    private String kind;private final Paint paint=new Paint(3);
    PhotoGlyph(Context context,String kind,String description,Runnable click){super(context);this.kind=kind;setContentDescription(description);setFocusable(true);setClickable(true);setBackground(Aero.selection(context));setOnClickListener(v->click.run());}
    void kind(String value){kind=value;invalidate();}
    protected void onDraw(Canvas c){super.onDraw(c);int save=c.save();c.translate(getWidth()/2f,getHeight()/2f);float size=Math.min(getWidth(),getHeight())/44f;c.scale(size,size);paint.setColor(0xff32516b);paint.setAlpha(isEnabled()?255:65);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);
        if(kind.equals("play")||kind.equals("pause")){paint.setStyle(Paint.Style.FILL);paint.setShader(new LinearGradient(0,-17,0,17,new int[]{0xffc9f4ff,0xff56b1ed,0xff1166bd,0xff3f9bd5},null,Shader.TileMode.CLAMP));c.drawCircle(0,0,17,paint);paint.setShader(null);paint.setColor(0xff285b86);paint.setStyle(Paint.Style.STROKE);c.drawCircle(0,0,17,paint);paint.setStyle(Paint.Style.FILL);paint.setColor(Color.WHITE);if(kind.equals("pause")){c.drawRect(-6,-7,-2,7,paint);c.drawRect(2,-7,6,7,paint);}else{Path triangle=new Path();triangle.moveTo(-4,-8);triangle.lineTo(8,0);triangle.lineTo(-4,8);triangle.close();c.drawPath(triangle,paint);}}
        else if(kind.equals("previous")||kind.equals("next")){int sign=kind.equals("next")?1:-1;c.scale(sign,1);Path p=new Path();p.moveTo(-6,-8);p.lineTo(4,0);p.lineTo(-6,8);p.close();paint.setStyle(Paint.Style.FILL);c.drawPath(p,paint);c.drawRect(7,-8,9,8,paint);}
        else if(kind.startsWith("rotate")){if(kind.equals("rotateLeft"))c.scale(-1,1);c.drawArc(-10,-9,10,11,140,270,false,paint);c.drawLine(7,-9,8,-1,paint);c.drawLine(8,-1,0,-2,paint);}
        else if(kind.equals("fit")){c.drawRect(-11,-9,11,9,paint);c.drawRect(-6,-5,6,5,paint);}
        else{c.drawCircle(-2,-2,8,paint);c.drawLine(4,4,11,11,paint);c.drawLine(-6,-2,2,-2,paint);if(kind.equals("zoomIn"))c.drawLine(-2,-6,-2,2,paint);}
        c.restoreToCount(save);
    }
}
