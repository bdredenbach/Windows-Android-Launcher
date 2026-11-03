package com.nthexperience.desktop;
import android.content.Context;
import android.graphics.*;
import android.view.View;
/** One matching two-line visual for both taskbar grips, with a wider transparent touch area. */
final class TaskbarSeparator extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    TaskbarSeparator(Context context){super(context);setFocusable(true);}
    @Override protected void onDraw(Canvas canvas){float x=getWidth()/2f,y=(getHeight()-Aero.dp(getContext(),24))/2f;paint.setStrokeWidth(Aero.dp(getContext(),1));paint.setColor(0x887e97a7);canvas.drawLine(x,y,x,y+Aero.dp(getContext(),24),paint);paint.setColor(0x55e1edf3);canvas.drawLine(x+Aero.dp(getContext(),1),y,x+Aero.dp(getContext(),1),y+Aero.dp(getContext(),24),paint);}
}
