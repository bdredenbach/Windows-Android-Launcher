package com.nthexperience.desktop;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.GestureDetector;
import android.view.PointerIcon;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Arrays;
import java.util.Collections;
import org.json.*;

/** Floating windows are for our built-in tools; Android apps launch normally. */
final class DesktopWindow extends LinearLayout {
    private final FrameLayout host;
    final View body;
    private final Runnable changed;
    private final TextView maximizeButton;
    private final TextView titleLabel;
    private boolean maximized;
    private int normalW, normalH,safeTop,safeRight;
    private float positionX=.5f,positionY=.33f;
    private final DesktopStore store;
    private String geometryKey,captionKind="";
    final String toolId;
    private float downX, downY, originX, originY;
    private final AeroGlass glass;
    private final TextView minimizeButton,closeButton;
    private boolean active=true,minimizing;
    private int transition;
    private DesktopContextMenu systemMenu;
    private static final int LEFT=1, RIGHT=2, TOP=4, BOTTOM=8;
    private static final int MAX_SAVED_SIZE=2000;
    private final Rect resizeStart=new Rect(), resizeArea=new Rect(), resizeBounds=new Rect();
    private final Paint gripPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private int resizeCorner;
    private float resizeDownX,resizeDownY;
    DesktopWindow(Context context, FrameLayout host, String title, View body, AeroWallpaper wallpaper,Runnable changed,DesktopStore store,String toolId,String layoutKey) {
        super(context);this.body=body; this.host=host; this.changed=changed;this.store=store;this.toolId=toolId;geometryKey="window_"+layoutKey+"_"+toolId;
        setOrientation(VERTICAL); setElevation(Aero.dp(context,16));
        glass=new AeroGlass(wallpaper,AeroGlass.WINDOW);setBackground(glass);
        setPadding(Aero.dp(context,6),Aero.dp(context,2),Aero.dp(context,6),Aero.dp(context,6));
        LinearLayout bar=new LinearLayout(context); bar.setGravity(Gravity.CENTER_VERTICAL);
        String icon=FolderLinks.id(toolId)||toolId.startsWith("@explorer:")?"files":toolId.equals("@recycle")?"recycle":toolId.equals("@taskmanager")?"taskmanager":toolId.equals("@welcome")?"orb":(toolId.startsWith("folder:")||toolId.startsWith("@organizer:"))?"folderapps":toolId.equals("@backup")?"drive":toolId.equals("@organize")||toolId.equals("@quicklaunch")?"folderapps":toolId.equals("@control")?"control":toolId.equals("@appearance")?"appearance":toolId.equals("@files")?"files":toolId.equals("@filemanagers")?"folderapps":toolId.equals("@computer")||toolId.equals("@system")?"computer":toolId.equals("@notes")?"notes":toolId.equals("@photos")?"pictures":toolId.equals(MediaLibrary.PLAYER)?"musicfile":toolId.equals("@browser")?"browser":GadgetCatalog.toolIcon(toolId);LinearLayout.LayoutParams iconParams=new LinearLayout.LayoutParams(Aero.dp(context,32),Aero.dp(context,32));iconParams.leftMargin=Aero.dp(context,1);bar.addView(new AeroIcon(context,icon),iconParams);
        TextView label=Aero.text(context,title,13,0xff111b22);titleLabel=label;label.setSingleLine();label.setEllipsize(android.text.TextUtils.TruncateAt.END);label.setLayerType(View.LAYER_TYPE_SOFTWARE,null);label.setShadowLayer(Aero.dp(context,6),0,0,0xffffffff); label.setPadding(Aero.dp(context,8),0,0,0);
        bar.addView(label,new LayoutParams(0,Aero.dp(context,48),1));
        TextView minimize=chrome("—",this::minimize);minimizeButton=minimize;
        maximizeButton=chrome("□",this::toggleMaximize);
        TextView close=chrome("×",this::closeWindow);closeButton=close;
        close.setTextColor(Color.WHITE);
        close.setBackground(Aero.gloss(context,new int[]{0xffefb2a3,0xffcf6550,0xffa92d18,0xffc14f33},3,0xff64362f));
        bar.addView(minimize); bar.addView(maximizeButton); bar.addView(close);
        addView(bar,new LayoutParams(-1,Aero.dp(context,48)));
        body.setBackgroundColor(0xfff7f8f9); addView(body,new LayoutParams(-1,0,1));
        bindCaptionIcon(bar.getChildAt(0));
        GestureDetector captionGestures=new GestureDetector(context,new GestureDetector.SimpleOnGestureListener(){public boolean onDown(MotionEvent e){return true;}public boolean onDoubleTap(MotionEvent e){toggleMaximize();return true;}public void onLongPress(MotionEvent e){showSystemMenu(label,e.getRawX(),e.getRawY());}});
        label.setOnContextClickListener(v->{ContextMenus.centered(label,(x,y)->showSystemMenu(label,x,y));return true;});
        label.setOnTouchListener((v,e)->{
            if(e.getActionMasked()==MotionEvent.ACTION_DOWN&&e.isButtonPressed(MotionEvent.BUTTON_SECONDARY)){showSystemMenu(label,e.getRawX(),e.getRawY());return true;}
            captionGestures.onTouchEvent(e);
            if(e.getActionMasked()==MotionEvent.ACTION_DOWN){ settle();bringToFront();changed.run();downX=e.getRawX(); downY=e.getRawY(); originX=getX(); originY=getY(); return true; }
            if(e.getActionMasked()==MotionEvent.ACTION_MOVE){
                if(!maximized){ setX(clamp(originX+e.getRawX()-downX,0,Math.max(0,hostWidth()-getWidth()))); setY(clamp(originY+e.getRawY()-downY,safeTop,Math.max(safeTop,host.getHeight()-getHeight())));invalidate(); }
                return true;
            }
            if(e.getActionMasked()==MotionEvent.ACTION_UP){if(!maximized){positionX=DesktopGeometry.fraction(getX(),hostWidth()-getWidth());positionY=DesktopGeometry.fraction(getY()-safeTop,host.getHeight()-safeTop-getHeight());saveGeometry();}v.performClick();return true;}
            if(e.getActionMasked()==MotionEvent.ACTION_CANCEL){fitToHost();return true;}
            return true;
        });
        label.setContentDescription(title+". Drag to move window.");
    }
    private void bindCaptionIcon(View icon){icon.setPadding(Aero.dp(getContext(),6),Aero.dp(getContext(),6),Aero.dp(getContext(),6),Aero.dp(getContext(),6));icon.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);icon.setContentDescription("Window menu");icon.setFocusable(true);icon.setOnClickListener(v->ContextMenus.centered(icon,(x,y)->showSystemMenu(icon,x,y)));ContextMenus.bind(icon,(x,y)->showSystemMenu(icon,x,y));}
    boolean captionIcon(String kind){if(captionKind.equals(kind))return false;captionKind=kind;ViewGroup chrome=(ViewGroup)getChildAt(0);View old=chrome.getChildAt(0);ViewGroup.LayoutParams params=old.getLayoutParams();chrome.removeViewAt(0);View icon=new AeroIcon(getContext(),kind);bindCaptionIcon(icon);chrome.addView(icon,0,params);return true;}
    boolean maximized(){return maximized;}
    void maximize(){if(!maximized)toggleMaximize();restore();}
    void restoreDown(){settle();setVisibility(VISIBLE);setMaximized(false);fitToHost();saveGeometry();bringToFront();changed.run();}
    void closeWindow(){if(getParent()==null)return;dismissSystemMenu();settle();hideKeyboard();if(body instanceof VistaShell)((VistaShell)body).close();if(body instanceof WeatherPanel)((WeatherPanel)body).close();if(body instanceof BrowserPanel)((BrowserPanel)body).close();if(body instanceof PhotoGallery)((PhotoGallery)body).close();if(body instanceof TaskManagerPanel)((TaskManagerPanel)body).close();if(body instanceof RecyclePanel)((RecyclePanel)body).close();host.removeView(this);changed.run();}
    void showSystemMenu(View anchor,float x,float y){dismissSystemMenu();systemMenu=new DesktopContextMenu(getContext(),"Window commands");systemMenu.item("Restore","empty",maximized||getVisibility()!=VISIBLE||minimizing,()->{if(getVisibility()!=VISIBLE||minimizing)restore();else if(maximized)restoreDown();});systemMenu.item("Minimize","empty",getVisibility()==VISIBLE&&!minimizing,this::minimize);systemMenu.item("Maximize","empty",!maximized,this::maximize);systemMenu.separator();systemMenu.item("Close","close",this::closeWindow);ContextMenus.show(systemMenu,anchor,x,y);}
    void dismissSystemMenu(){if(systemMenu!=null){systemMenu.dismiss();systemMenu=null;}}
    void arrange(int x,int y,int width,int height){settle();setMaximized(false);normalW=Math.max(1,width);normalH=Math.max(1,height);positionX=DesktopGeometry.fraction(x,hostWidth()-normalW);positionY=DesktopGeometry.fraction(y,host.getHeight()-safeTop-normalH);fitToHost();saveGeometry();}
    String caption(){return titleLabel.getText().toString();}
    void caption(String title){titleLabel.setText(title);titleLabel.setContentDescription(title+". Drag to move window.");}
    boolean minimizing(){return minimizing;}
    boolean active(){return active;}
    void active(boolean value){active=value;glass.active(value);titleLabel.setTextColor(value?0xff111b22:0xff465563);titleLabel.setShadowLayer(Aero.dp(getContext(),value?6:2),0,0,value?0xffffffff:0x99ffffff);minimizeButton.setAlpha(value?1:.65f);maximizeButton.setAlpha(value?1:.65f);closeButton.setAlpha(value?1:.6f);}
    @Override public boolean dispatchTouchEvent(MotionEvent e){if(e.getActionMasked()==MotionEvent.ACTION_DOWN&&!minimizing){bringToFront();changed.run();}return super.dispatchTouchEvent(e);}
    private int cornerReach(){return Aero.dp(getContext(),22);}
    private int resizeCornerAt(float x,float y){
        if(maximized||minimizing)return 0;
        int reach=cornerReach();
        int side=x<reach?LEFT:x>=getWidth()-reach?RIGHT:0;
        if(side==0)return 0;
        if(y>=getHeight()-reach)return side|BOTTOM;
        // Keep the caption buttons' main touch targets clear.
        int border=Aero.dp(getContext(),6);
        if(y<reach&&(y<border||x<border||x>=getWidth()-border))return side|TOP;
        return 0;
    }
    @Override public PointerIcon onResolvePointerIcon(MotionEvent event,int pointerIndex){
        int corner=resizeCornerAt(event.getX(pointerIndex),event.getY(pointerIndex));
        if(corner==0)return super.onResolvePointerIcon(event,pointerIndex);
        boolean topLeftToBottomRight=corner==(TOP|LEFT)||corner==(BOTTOM|RIGHT);
        return PointerIcon.getSystemIcon(getContext(),topLeftToBottomRight
                ?PointerIcon.TYPE_TOP_LEFT_DIAGONAL_DOUBLE_ARROW:PointerIcon.TYPE_TOP_RIGHT_DIAGONAL_DOUBLE_ARROW);
    }
    @Override protected void onSizeChanged(int width,int height,int oldWidth,int oldHeight){
        super.onSizeChanged(width,height,oldWidth,oldHeight);
        updateCornerGestures(width,height);
    }
    private void updateCornerGestures(int width,int height){
        if(Build.VERSION.SDK_INT<29)return;
        int reach=cornerReach();
        if(maximized||width<2*reach||height<2*reach){
            setSystemGestureExclusionRects(Collections.emptyList());
            return;
        }
        // A floating window can touch the phone's Back gesture area.
        setSystemGestureExclusionRects(Arrays.asList(
                new Rect(0,0,reach,reach),new Rect(width-reach,0,width,reach),
                new Rect(0,height-reach,reach,height),new Rect(width-reach,height-reach,width,height)));
    }
    @Override protected void dispatchDraw(Canvas canvas){
        super.dispatchDraw(canvas);
        if(maximized)return;
        float density=getResources().getDisplayMetrics().density;
        gripPaint.setStrokeWidth(density);
        gripPaint.setColor(active?0x99586e80:0x77627480);
        float edge=4*density;
        float bottom=getHeight()-edge;
        for(int size=4;size<=10;size+=3){
            float length=size*density;
            canvas.drawLine(edge,bottom-length,edge+length,bottom,gripPaint);
            float right=getWidth()-edge;
            canvas.drawLine(right-length,bottom,right,bottom-length,gripPaint);
        }
    }
    @Override public boolean onInterceptTouchEvent(MotionEvent event){
        if(event.getActionMasked()!=MotionEvent.ACTION_DOWN||event.isButtonPressed(MotionEvent.BUTTON_SECONDARY))
            return super.onInterceptTouchEvent(event);
        int corner=resizeCornerAt(event.getX(),event.getY());
        if(corner==0)return super.onInterceptTouchEvent(event);

        dismissSystemMenu();
        settle();
        resizeCorner=corner;
        resizeDownX=event.getRawX();
        resizeDownY=event.getRawY();
        int x=Math.round(getX()),y=Math.round(getY());
        resizeStart.set(x,y,x+getWidth(),y+getHeight());
        resizeBounds.set(resizeStart);
        resizeArea.set(0,safeTop,hostWidth(),host.getHeight());
        return true;
    }
    @Override public boolean onTouchEvent(MotionEvent event){
        if(resizeCorner==0)return super.onTouchEvent(event);
        switch(event.getActionMasked()){
            case MotionEvent.ACTION_MOVE:
                dragCorner(event.getRawX(),event.getRawY());
                break;
            case MotionEvent.ACTION_UP:
                dragCorner(event.getRawX(),event.getRawY());
                finishResize();
                break;
            case MotionEvent.ACTION_POINTER_UP:
                if(event.getActionIndex()==0)finishResize();
                break;
            case MotionEvent.ACTION_CANCEL:
                cancelResize();
                break;
            default:break;
        }
        return true;
    }
    private void dragCorner(float rawX,float rawY){
        int dx=Math.round(rawX-resizeDownX),dy=Math.round(rawY-resizeDownY);
        int minWidth=Math.min(resizeStart.width(),Aero.dp(getContext(),220));
        int minHeight=Math.min(resizeStart.height(),Aero.dp(getContext(),180));
        int maxSize=Aero.dp(getContext(),MAX_SAVED_SIZE);
        if((resizeCorner&LEFT)!=0){
            resizeBounds.left=(int)clamp(resizeStart.left+dx,Math.max(resizeArea.left,resizeStart.right-maxSize),resizeStart.right-minWidth);
        }else{
            resizeBounds.right=(int)clamp(resizeStart.right+dx,resizeStart.left+minWidth,Math.min(resizeArea.right,resizeStart.left+maxSize));
        }
        if((resizeCorner&TOP)!=0){
            resizeBounds.top=(int)clamp(resizeStart.top+dy,Math.max(resizeArea.top,resizeStart.bottom-maxSize),resizeStart.bottom-minHeight);
        }else{
            resizeBounds.bottom=(int)clamp(resizeStart.bottom+dy,resizeStart.top+minHeight,Math.min(resizeArea.bottom,resizeStart.top+maxSize));
        }
        place(resizeBounds.left,resizeBounds.top,resizeBounds.width(),resizeBounds.height());
    }
    private void finishResize(){
        resizeCorner=0;
        if(resizeBounds.equals(resizeStart))return;
        normalW=resizeBounds.width();
        normalH=resizeBounds.height();
        positionX=DesktopGeometry.fraction(resizeBounds.left,hostWidth()-normalW);
        positionY=DesktopGeometry.fraction(resizeBounds.top-safeTop,host.getHeight()-safeTop-normalH);
        saveGeometry();
    }
    void cancelResize(){
        if(resizeCorner==0)return;
        resizeCorner=0;
        fitToHost();
    }
    private TextView chrome(String symbol,Runnable action){
        TextView view=Aero.button(getContext(),symbol,action);
        view.setTextSize(16);view.setMinHeight(0);view.setPadding(0,0,0,0);LayoutParams lp=new LayoutParams(Aero.dp(getContext(),44),Aero.dp(getContext(),44));lp.leftMargin=Aero.dp(getContext(),1);view.setLayoutParams(lp);
        view.setBackground(Aero.gloss(getContext(),new int[]{0xbbc4deeb,0xaa8daabd,0x99738c9e,0xaab0c9d6},3,0xff627b8c));
        view.setContentDescription(symbol.equals("×")?"Close window":symbol.equals("—")?"Minimize window":"Maximize or restore window");
        return view;
    }
    void open(){
        int margin=Aero.dp(getContext(),12);
        int width=Math.min(Aero.dp(getContext(),520),hostWidth()-margin*2);
        int height=Math.min(Aero.dp(getContext(),490),host.getHeight()-safeTop-margin*2);
        FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(Math.max(1,width),Math.max(1,height));
        normalW=width;normalH=height;host.addView(this,lp); setX((hostWidth()-width)/2f); setY(safeTop+Math.max(margin,(host.getHeight()-safeTop-height)/3f));
        if((toolId.equals("@browser")||toolId.equals("@photos")||toolId.equals(MediaLibrary.PLAYER))&&!store.prefs.contains(geometryKey))setMaximized(true);
        readGeometry(null);fitToHost();changed.run();
    }
    private void hideKeyboard(){
        ((android.view.inputmethod.InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(getWindowToken(),0);
        clearFocus();
    }
    void settle(){cancelResize();transition++;animate().withEndAction(null).cancel();minimizing=false;setAlpha(1);setScaleX(1);setScaleY(1);setPivotX(getWidth()/2f);setPivotY(getHeight()/2f);}
    void minimize(){dismissSystemMenu();hideKeyboard();settle();if(!Aero.motion(getContext())){setVisibility(GONE);changed.run();return;}minimizing=true;int token=++transition;setPivotX(getWidth()*.2f);setPivotY(host.getHeight()-getY()+Aero.dp(getContext(),20));changed.run();animate().scaleX(.15f).scaleY(.15f).alpha(0).setDuration(180).withEndAction(()->{if(token!=transition)return;setVisibility(GONE);settle();changed.run();}).start();}
    void restore(){boolean hidden=getVisibility()!=VISIBLE||minimizing;settle();setVisibility(VISIBLE);bringToFront();changed.run();if(hidden&&Aero.motion(getContext())){int token=++transition;setPivotX(getWidth()*.2f);setPivotY(host.getHeight()-getY()+Aero.dp(getContext(),20));setScaleX(.18f);setScaleY(.18f);setAlpha(0);animate().scaleX(1).scaleY(1).alpha(1).setDuration(190).withEndAction(()->{if(token==transition)settle();}).start();}}
    @Override protected void onDetachedFromWindow(){dismissSystemMenu();settle();super.onDetachedFromWindow();}
    private int hostWidth(){return Math.max(1,host.getWidth()-safeRight);}
    void safeRight(int inset){int next=Math.max(0,Math.min(inset,Math.max(0,host.getWidth()-Aero.dp(getContext(),180))));if(safeRight!=next){safeRight=next;fitToHost();}}
    void safeTop(int inset){safeTop=Math.max(0,inset);fitToHost();}
    void fitToHost(){
        if(getParent()==null)return;
        if(resizeCorner!=0){
            if(resizeArea.top==safeTop&&resizeArea.right==hostWidth()&&resizeArea.bottom==host.getHeight())return;
            resizeCorner=0;
        }
        int width=maximized?hostWidth():Math.max(1,Math.min(normalW,hostWidth()));
        int height=maximized?Math.max(1,host.getHeight()-safeTop):Math.max(1,Math.min(normalH,host.getHeight()-safeTop));
        float x=maximized?0:positionX*Math.max(0,hostWidth()-width);
        float y=safeTop+(maximized?0:positionY*Math.max(0,host.getHeight()-safeTop-height));
        place(x,y,width,height);
    }
    private void place(float x,float y,int width,int height){
        setX(x);
        setY(y);
        FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)getLayoutParams();
        // The host layout listener can call this while a child is already laid out.
        if(lp.width!=width||lp.height!=height){
            lp.width=width;
            lp.height=height;
            setLayoutParams(lp);
        }
        invalidate();
    }
    private void setMaximized(boolean value){
        if(maximized==value)return;
        maximized=value;
        maximizeButton.setText(value?"❐":"□");
        updateCornerGestures(getWidth(),getHeight());
    }
    private void toggleMaximize(){
        cancelResize();
        setMaximized(!maximized);fitToHost();saveGeometry();changed.run();
    }
    JSONObject geometry(){
        JSONObject result=new JSONObject();float density=getResources().getDisplayMetrics().density;
        GadgetLayout.put(result,"x",positionX);GadgetLayout.put(result,"y",positionY);GadgetLayout.put(result,"w",normalW/density);GadgetLayout.put(result,"h",normalH/density);GadgetLayout.put(result,"maximized",maximized);return result;
    }
    void saveGeometry(){store.prefs.edit().putString(geometryKey,geometry().toString()).apply();}
    void orientation(String key){saveGeometry();JSONObject previous=geometry();geometryKey="window_"+key+"_"+toolId;readGeometry(previous);}
    void readGeometry(JSONObject fallback){
        JSONObject value=fallback;String saved=store.prefs.getString(geometryKey,null);if(saved!=null)try{value=new JSONObject(saved);}catch(JSONException ignored){}
        if(value==null)return;
        applyGeometry(value,(fallback==null?value:fallback).optBoolean("maximized"));
    }
    void restoreGeometry(JSONObject value){if(value!=null)applyGeometry(value,value.optBoolean("maximized"));}
    private void applyGeometry(JSONObject value,boolean maximize){
        positionX=DesktopGeometry.clamp((float)value.optDouble("x",.5),0,1);positionY=DesktopGeometry.clamp((float)value.optDouble("y",.33),0,1);
        normalW=Aero.dp(getContext(),DesktopGeometry.clamp((float)value.optDouble("w",520),180,MAX_SAVED_SIZE));
        normalH=Aero.dp(getContext(),DesktopGeometry.clamp((float)value.optDouble("h",490),180,MAX_SAVED_SIZE));
        setMaximized(maximize);fitToHost();
    }
    private float clamp(float value,float low,float high){return Math.max(low,Math.min(high,value));}
}
