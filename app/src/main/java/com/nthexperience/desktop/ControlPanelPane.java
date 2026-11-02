package com.nthexperience.desktop;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Original Vista-inspired blue-to-green task pane with quiet diagonal highlights. */
final class ControlPanelPane extends Drawable {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);private int alpha=255;
    @Override public void draw(Canvas canvas){Rect r=getBounds();int layer=canvas.saveLayerAlpha(new RectF(r),alpha);paint.setShader(new LinearGradient(r.left,r.top,r.left,r.bottom,0xff426c9b,0xff65ac6c,Shader.TileMode.CLAMP));canvas.drawRect(r,paint);paint.setShader(null);Path path=new Path();path.moveTo(r.left,r.bottom-r.height()*.28f);path.lineTo(r.right,r.bottom-r.height()*.48f);path.lineTo(r.right,r.bottom-r.height()*.38f);path.lineTo(r.left,r.bottom-r.height()*.15f);path.close();paint.setColor(0x14e5faff);canvas.drawPath(path,paint);paint.setColor(0x12ffffff);canvas.drawLine(r.left,r.bottom-r.height()*.16f,r.right,r.bottom-r.height()*.4f,paint);paint.setColor(0xff91a8bb);canvas.drawLine(r.right-1,r.top,r.right-1,r.bottom,paint);canvas.restoreToCount(layer);}
    @Override public void setAlpha(int value){alpha=value;invalidateSelf();}@Override public void setColorFilter(ColorFilter value){paint.setColorFilter(value);}@Override public int getOpacity(){return PixelFormat.OPAQUE;}
}
