package com.nthexperience.desktop;

import android.content.Context;
import android.graphics.*;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import org.json.JSONObject;

/** Fit-relative zoom and bounded panning; rotation is a non-destructive viewing transform. */
final class PhotoCanvas extends View {
    private Bitmap bitmap;
    private final Paint paint=new Paint(3);
    private final Matrix matrix=new Matrix();
    private final ScaleGestureDetector pinch;
    private final GestureDetector gestures;
    private float zoom=1,panX,panY,lastX,lastY;
    private int turn;
    private boolean scaling;
    private JSONObject pending;
    private Runnable changed=()->{};
    PhotoCanvas(Context context){super(context);setContentDescription("Photo viewing area");setFocusable(true);setBackgroundColor(0xffe9edf0);
        pinch=new ScaleGestureDetector(context,new ScaleGestureDetector.SimpleOnScaleGestureListener(){public boolean onScale(ScaleGestureDetector detector){scaling=true;setZoom(zoom*detector.getScaleFactor());return true;}});
        gestures=new GestureDetector(context,new GestureDetector.SimpleOnGestureListener(){public boolean onDown(MotionEvent e){return true;}public boolean onDoubleTap(MotionEvent e){setZoom(zoom>1.01f?1:2);return true;}});
    }
    void changed(Runnable listener){changed=listener;}
    void image(Bitmap value){bitmap=value;invalidate();}
    boolean ready(){return bitmap!=null&&!bitmap.isRecycled();}
    int pixelWidth(){return ready()?bitmap.getWidth():0;}
    float zoom(){return zoom;}
    int rotation(){return turn*90;}
    void fit(){zoom=1;panX=panY=0;invalidate();changed.run();}
    void zoomBy(float amount){setZoom(zoom*amount);}
    private void setZoom(float value){zoom=Math.max(1,Math.min(8,value));clamp();invalidate();changed.run();}
    void rotate(int direction){turn=(turn+direction+4)%4;fit();}
    JSONObject state(){JSONObject out=new JSONObject();GadgetLayout.put(out,"zoom",zoom);GadgetLayout.put(out,"rotation",turn);GadgetLayout.put(out,"x",getWidth()==0?0:panX/getWidth());GadgetLayout.put(out,"y",getHeight()==0?0:panY/getHeight());return out;}
    void restore(JSONObject value){if(value!=null&&(getWidth()==0||getHeight()==0)){pending=value;return;}pending=null;if(value==null){turn=0;fit();return;}zoom=(float)Math.max(1,Math.min(8,value.optDouble("zoom",1)));turn=Math.floorMod(value.optInt("rotation"),4);panX=(float)value.optDouble("x",0)*getWidth();panY=(float)value.optDouble("y",0)*getHeight();clamp();invalidate();changed.run();}
    private float fitScale(){if(!ready())return 1;float width=turn%2==0?bitmap.getWidth():bitmap.getHeight(),height=turn%2==0?bitmap.getHeight():bitmap.getWidth();return Math.min(Math.max(1,getWidth()-Aero.dp(getContext(),18))/width,Math.max(1,getHeight()-Aero.dp(getContext(),18))/height);}
    private void clamp(){if(!ready())return;float width=(turn%2==0?bitmap.getWidth():bitmap.getHeight())*fitScale()*zoom,height=(turn%2==0?bitmap.getHeight():bitmap.getWidth())*fitScale()*zoom;float x=Math.max(0,(width-getWidth())/2),y=Math.max(0,(height-getHeight())/2);panX=Math.max(-x,Math.min(x,panX));panY=Math.max(-y,Math.min(y,panY));}
    @Override protected void onSizeChanged(int w,int h,int oldW,int oldH){if(oldW>0)panX*=w/(float)oldW;if(oldH>0)panY*=h/(float)oldH;clamp();if(pending!=null)restore(pending);}
    @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);if(!ready())return;matrix.reset();matrix.postTranslate(-bitmap.getWidth()/2f,-bitmap.getHeight()/2f);matrix.postRotate(turn*90);float scale=fitScale()*zoom;matrix.postScale(scale,scale);matrix.postTranslate(getWidth()/2f+panX,getHeight()/2f+panY);canvas.drawBitmap(bitmap,matrix,paint);}
    @Override public boolean onTouchEvent(MotionEvent event){if(!ready())return super.onTouchEvent(event);pinch.onTouchEvent(event);gestures.onTouchEvent(event);int action=event.getActionMasked();if(action==MotionEvent.ACTION_DOWN){lastX=event.getX();lastY=event.getY();scaling=false;getParent().requestDisallowInterceptTouchEvent(true);}else if(action==MotionEvent.ACTION_MOVE){if(!pinch.isInProgress()&&!scaling&&event.getPointerCount()==1&&zoom>1){panX+=event.getX()-lastX;panY+=event.getY()-lastY;clamp();invalidate();}lastX=event.getX();lastY=event.getY();}else if(action==MotionEvent.ACTION_UP){performClick();getParent().requestDisallowInterceptTouchEvent(false);}else if(action==MotionEvent.ACTION_CANCEL)getParent().requestDisallowInterceptTouchEvent(false);return true;}
    @Override public boolean performClick(){super.performClick();return true;}
    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(info);info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);}
    @Override public boolean performAccessibilityAction(int action,android.os.Bundle arguments){if(action==AccessibilityNodeInfo.ACTION_SCROLL_FORWARD){zoomBy(1.25f);return true;}if(action==AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD){zoomBy(.8f);return true;}return super.performAccessibilityAction(action,arguments);}
}
