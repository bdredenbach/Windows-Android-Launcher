package com.nthexperience.desktop;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Original artwork drawn for the familiar glass-era browser controls. */
final class BrowserGlyph extends View {
    private final String kind;
    private final Paint paint=new Paint(3);
    BrowserGlyph(Context context,String kind,String description,Runnable action){super(context);this.kind=kind;setContentDescription(description);setFocusable(true);setClickable(true);setOnClickListener(v->action.run());setBackground(Aero.gloss(context,new int[]{0x00ffffff,0x22ffffff,0x0087acbf,0x1187acbf},3,0x0087acbf));}
    protected void onDraw(Canvas canvas){super.onDraw(canvas);int save=canvas.save();canvas.translate(getWidth()/2f,getHeight()/2f);float scale=Math.min(getWidth(),getHeight())/40f;canvas.scale(scale,scale);paint.setAlpha(isEnabled()?255:80);paint.setStrokeWidth(2);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);paint.setStyle(Paint.Style.STROKE);paint.setColor(0xff436173);paint.setAlpha(isEnabled()?255:80);
        if(kind.equals("back")){paint.setStyle(Paint.Style.FILL);paint.setShader(new LinearGradient(0,-17,0,17,new int[]{0xffb4eaff,0xff3b9adb,0xff0861ad,0xff3699d1},null,Shader.TileMode.CLAMP));canvas.drawCircle(0,0,17,paint);paint.setShader(null);paint.setColor(0xff235678);paint.setStyle(Paint.Style.STROKE);canvas.drawCircle(0,0,17,paint);paint.setColor(Color.WHITE);paint.setStrokeWidth(3);canvas.drawLine(8,0,-8,0,paint);canvas.drawLine(-8,0,-1,-7,paint);canvas.drawLine(-8,0,-1,7,paint);
        }else if(kind.equals("forward")){canvas.drawLine(-7,0,7,0,paint);canvas.drawLine(7,0,1,-6,paint);canvas.drawLine(7,0,1,6,paint);
        }else if(kind.equals("reload")){canvas.drawArc(-8,-8,8,8,35,290,false,paint);canvas.drawLine(7,-7,8,0,paint);canvas.drawLine(8,0,2,-2,paint);
        }else if(kind.equals("stop")||kind.equals("close")){canvas.drawLine(-5,-5,5,5,paint);canvas.drawLine(-5,5,5,-5,paint);
        }else if(kind.equals("home")){Path p=new Path();p.moveTo(-10,-1);p.lineTo(0,-10);p.lineTo(10,-1);p.moveTo(-7,-3);p.lineTo(-7,9);p.lineTo(7,9);p.lineTo(7,-3);p.moveTo(-2,9);p.lineTo(-2,2);p.lineTo(2,2);p.lineTo(2,9);canvas.drawPath(p,paint);
        }else if(kind.equals("star")){Path p=new Path();for(int i=0;i<10;i++){double a=i*Math.PI/5-Math.PI/2;float radius=i%2==0?11:5;float x=(float)Math.cos(a)*radius,y=(float)Math.sin(a)*radius;if(i==0)p.moveTo(x,y);else p.lineTo(x,y);}p.close();paint.setStyle(Paint.Style.FILL);paint.setColor(0xffe7b849);canvas.drawPath(p,paint);paint.setStyle(Paint.Style.STROKE);paint.setColor(0xff9c792a);canvas.drawPath(p,paint);
        }else if(kind.equals("tools")){canvas.drawCircle(0,0,7,paint);canvas.drawCircle(0,0,2,paint);for(int i=0;i<8;i++){canvas.save();canvas.rotate(i*45);canvas.drawLine(0,-7,0,-11,paint);canvas.restore();}
        }else if(kind.equals("plus")){canvas.drawLine(-7,0,7,0,paint);canvas.drawLine(0,-7,0,7,paint);
        }else if(kind.equals("lock")){canvas.drawRoundRect(-6,-1,6,9,2,2,paint);canvas.drawArc(-4,-9,4,4,180,180,false,paint);
        }else {paint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));paint.setStyle(Paint.Style.FILL);paint.setTextSize(18);canvas.drawText("i",-2,6,paint);}canvas.restoreToCount(save);}
}
