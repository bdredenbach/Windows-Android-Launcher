package com.nthexperience.desktop;

import android.app.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;

/** On-demand in-memory snapshots of Nth windows. No screen recording permission. */
final class WindowPreviews {
    interface Host {java.util.List<DesktopWindow> windows();java.util.List<String> external();String label(String id);void open(String id);}
    private final Activity activity;private final Host host;private final Handler main=new Handler(Looper.getMainLooper());
    private AlertDialog dialog;private PopupWindow hover;private Runnable pending;
    WindowPreviews(Activity activity,Host host){this.activity=activity;this.host=host;}
    private boolean allowed(DesktopWindow window){return !(window.body instanceof BrowserPanel)||!((BrowserPanel)window.body).hasPrivateTabs();}
    private Bitmap snapshot(DesktopWindow window){
        if(!allowed(window)||window.getWidth()<1||window.getHeight()<1)return null;
        float scale=Math.min(1,Math.min(280f/window.getWidth(),170f/window.getHeight()));Bitmap bitmap=null;
        try{bitmap=Bitmap.createBitmap(Math.max(1,Math.round(window.getWidth()*scale)),Math.max(1,Math.round(window.getHeight()*scale)),Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);canvas.scale(scale,scale);window.draw(canvas);return bitmap;}catch(RuntimeException|OutOfMemoryError ex){if(bitmap!=null)bitmap.recycle();return null;}
    }
    void show(){((android.view.inputmethod.InputMethodManager)activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(activity.getWindow().getDecorView().getWindowToken(),0);dismiss();LinearLayout body=Aero.column(activity);Aero.pad(body,10);java.util.List<Bitmap> images=new ArrayList<>();
        body.addView(Aero.text(activity,"Tap a window to switch. Previews are snapshots taken when this view opens. InPrivate windows have no preview.",12,0xff496171));
        for(DesktopWindow window:host.windows()){
            LinearLayout card=Aero.column(activity);card.setPadding(0,Aero.dp(activity,10),0,Aero.dp(activity,10));card.setFocusable(true);card.setContentDescription("Switch to "+window.caption());card.setBackground(Aero.selection(activity));
            card.addView(Aero.text(activity,window.caption()+(window.getVisibility()!=View.VISIBLE||window.minimizing()?" — Minimized":""),15,Aero.INK));Bitmap bitmap=snapshot(window);
            if(bitmap!=null){images.add(bitmap);ImageView preview=new ImageView(activity);preview.setImageBitmap(bitmap);preview.setScaleType(ImageView.ScaleType.FIT_CENTER);preview.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);card.addView(preview,new LinearLayout.LayoutParams(-1,Aero.dp(activity,145)));}
            else card.addView(Aero.text(activity,allowed(window)?"Preview unavailable":"InPrivate — preview hidden",12,0xff496171));
            card.setOnClickListener(v->{dismiss();window.restore();});body.addView(card);
        }
        for(String id:host.external())body.addView(Aero.button(activity,"Open Android app: "+host.label(id),()->{dismiss();host.open(id);}));
        if(host.windows().isEmpty()&&host.external().isEmpty())body.addView(Aero.text(activity,"No open windows",16,Aero.INK));
        ScrollView scroll=new ScrollView(activity);scroll.addView(body);dialog=Aero.dialog(activity).setTitle("Switch Windows").setView(scroll).setNegativeButton("Close",null).create();dialog.setOnDismissListener(d->{images.clear();dialog=null;});dialog.show();
        int height=Math.round(activity.getResources().getDisplayMetrics().heightPixels*.75f);dialog.getWindow().setLayout(Math.min(Aero.dp(activity,400),activity.getResources().getDisplayMetrics().widthPixels-Aero.dp(activity,24)),height);
    }
    void bind(View button,DesktopWindow window){button.setOnHoverListener((v,event)->{
        if(event.getActionMasked()==MotionEvent.ACTION_HOVER_ENTER){hideHover();pending=()->{if(!button.isAttachedToWindow()||!activity.hasWindowFocus()||window.getParent()==null)return;LinearLayout content=Aero.column(activity);Aero.pad(content,8);content.setBackground(Aero.panel(activity,0xffedf8ff,0xffc8dfec,3,0xff607f92));content.addView(Aero.text(activity,window.caption(),13,Aero.INK));Bitmap image=snapshot(window);if(image!=null){ImageView view=new ImageView(activity);view.setImageBitmap(image);view.setScaleType(ImageView.ScaleType.FIT_CENTER);content.addView(view,new LinearLayout.LayoutParams(-1,Aero.dp(activity,150)));}else content.addView(Aero.text(activity,allowed(window)?"Preview unavailable":"InPrivate — preview hidden",12,Aero.INK));hover=new PopupWindow(content,Aero.dp(activity,260),Aero.dp(activity,190),false);hover.setClippingEnabled(true);hover.showAsDropDown(button,0,-button.getHeight()-Aero.dp(activity,195));};main.postDelayed(pending,450);}
        if(event.getActionMasked()==MotionEvent.ACTION_HOVER_EXIT)hideHover();return false;});}
    private void hideHover(){if(pending!=null)main.removeCallbacks(pending);pending=null;if(hover!=null){hover.dismiss();hover=null;}}
    boolean dismiss(){boolean was=dialog!=null||hover!=null;hideHover();if(dialog!=null){AlertDialog old=dialog;dialog=null;old.dismiss();}return was;}
}
