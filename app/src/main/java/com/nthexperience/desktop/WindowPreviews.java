package com.nthexperience.desktop;

import android.app.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;

/** On-demand in-memory snapshots of Nth windows. No screen recording permission. */
final class WindowPreviews {
    interface Host {java.util.List<DesktopWindow> windows();java.util.List<String> external();String label(String id);void open(String id);View wallpaper();void showDesktop();}
    private final Activity activity;private final Host host;private final Handler main=new Handler(Looper.getMainLooper());
    private Dialog dialog;private PopupWindow hover;private Runnable pending;
    WindowPreviews(Activity activity,Host host){this.activity=activity;this.host=host;}
    static boolean allowed(DesktopWindow window){return !(window.body instanceof BrowserPanel)||!((BrowserPanel)window.body).hasPrivateTabs();}
    private Bitmap snapshot(DesktopWindow window){
        return allowed(window)?capture(window,280,170):null;
    }
    private Bitmap capture(View source,int maxWidth,int maxHeight){
        if(source.getWidth()<1||source.getHeight()<1)return null;
        float scale=Math.min(1,Math.min((float)maxWidth/source.getWidth(),(float)maxHeight/source.getHeight()));
        Bitmap bitmap=null;
        try{
            bitmap=Bitmap.createBitmap(Math.max(1,Math.round(source.getWidth()*scale)),
                    Math.max(1,Math.round(source.getHeight()*scale)),Bitmap.Config.ARGB_8888);
            Canvas canvas=new Canvas(bitmap);
            canvas.scale(scale,scale);
            source.draw(canvas);
            return bitmap;
        }catch(RuntimeException|OutOfMemoryError ex){
            if(bitmap!=null)bitmap.recycle();
            return null;
        }
    }
    void showGroup(java.util.List<DesktopWindow> windows){show(windows,"Folder windows",false);}
    void show(){show(host.windows(),"Switch Windows",true);}
    void flip(){
        hideKeyboard();
        dismiss();
        List<DesktopWindow> windows=new ArrayList<>(host.windows());
        Collections.reverse(windows);
        List<String> apps=host.external();
        Bitmap desktop=capture(host.wallpaper(),900,900);
        FlipStack stack=new FlipStack(activity,windows,apps,host,
                window->allowed(window)?capture(window,540,460):null,desktop,this::dismiss);
        Dialog view=new Dialog(activity);
        view.requestWindowFeature(Window.FEATURE_NO_TITLE);
        view.setContentView(stack);
        view.setTitle("Flip 3D");
        dialog=view;
        view.setOnDismissListener(d->{if(dialog==view)dialog=null;});
        view.setOnKeyListener((d,key,event)->{
            if(event.getAction()!=KeyEvent.ACTION_DOWN)return false;
            if(key==KeyEvent.KEYCODE_TAB&&event.isMetaPressed()){
                stack.step(event.isShiftPressed()?-1:1);
                return true;
            }
            if(key==KeyEvent.KEYCODE_ESCAPE){dismiss();return true;}
            return false;
        });
        Window chrome=view.getWindow();
        chrome.setBackgroundDrawableResource(android.R.color.transparent);
        chrome.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        view.show();
        chrome.setLayout(-1,-1);
    }
    private void hideKeyboard(){
        ((android.view.inputmethod.InputMethodManager)activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(activity.getWindow().getDecorView().getWindowToken(),0);
    }
    private void show(java.util.List<DesktopWindow> windows,String title,boolean external){hideKeyboard();dismiss();LinearLayout body=Aero.column(activity);Aero.pad(body,10);java.util.List<Bitmap> images=new ArrayList<>();
        body.addView(Aero.text(activity,"Tap a window to switch. Previews are snapshots taken when this view opens. InPrivate windows have no preview.",12,0xff496171));
        for(DesktopWindow window:windows){
            LinearLayout card=Aero.column(activity);card.setPadding(0,Aero.dp(activity,10),0,Aero.dp(activity,10));card.setFocusable(true);card.setContentDescription("Switch to "+window.caption());card.setBackground(Aero.selection(activity));
            card.addView(Aero.text(activity,window.caption()+(window.getVisibility()!=View.VISIBLE||window.minimizing()?" — Minimized":""),15,Aero.INK));Bitmap bitmap=snapshot(window);
            if(bitmap!=null){images.add(bitmap);ImageView preview=new ImageView(activity);preview.setImageBitmap(bitmap);preview.setScaleType(ImageView.ScaleType.FIT_CENTER);preview.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);card.addView(preview,new LinearLayout.LayoutParams(-1,Aero.dp(activity,145)));}
            else card.addView(Aero.text(activity,allowed(window)?"Preview unavailable":"InPrivate — preview hidden",12,0xff496171));
            card.setOnClickListener(v->{dismiss();window.restore();});body.addView(card);
        }
        if(external)for(String id:host.external())body.addView(Aero.button(activity,"Open Android app: "+host.label(id),()->{dismiss();host.open(id);}));
        if(windows.isEmpty()&&(!external||host.external().isEmpty()))body.addView(Aero.text(activity,"No open windows",16,Aero.INK));
        ScrollView scroll=new ScrollView(activity);scroll.addView(body);AlertDialog view=Aero.dialog(activity).setTitle(title).setView(scroll).setNegativeButton("Close",null).create();dialog=view;view.setOnDismissListener(d->{images.clear();if(dialog==view)dialog=null;});view.show();
        int height=Math.round(activity.getResources().getDisplayMetrics().heightPixels*.75f);dialog.getWindow().setLayout(Math.min(Aero.dp(activity,400),activity.getResources().getDisplayMetrics().widthPixels-Aero.dp(activity,24)),height);
    }
    void bind(View button,DesktopWindow window){button.setOnHoverListener((v,event)->{
        if(!event.isFromSource(InputDevice.SOURCE_MOUSE)&&!event.isFromSource(InputDevice.SOURCE_STYLUS))return false;
        if(event.getActionMasked()==MotionEvent.ACTION_HOVER_ENTER){hideHover();pending=()->{if(!button.isAttachedToWindow()||!activity.hasWindowFocus()||window.getParent()==null)return;LinearLayout content=Aero.column(activity);content.setContentDescription("Window preview for "+window.caption());Aero.pad(content,8);content.setBackground(Aero.panel(activity,0xffedf8ff,0xffc8dfec,3,0xff607f92));content.addView(Aero.text(activity,window.caption(),13,Aero.INK));Bitmap image=snapshot(window);if(image!=null){ImageView view=new ImageView(activity);view.setImageBitmap(image);view.setScaleType(ImageView.ScaleType.FIT_CENTER);content.addView(view,new LinearLayout.LayoutParams(-1,Aero.dp(activity,150)));}else content.addView(Aero.text(activity,allowed(window)?"Preview unavailable":"InPrivate — preview hidden",12,Aero.INK));hover=new PopupWindow(content,Aero.dp(activity,260),Aero.dp(activity,190),false);hover.setClippingEnabled(true);hover.showAsDropDown(button,0,-button.getHeight()-Aero.dp(activity,195));};main.postDelayed(pending,450);}
        if(event.getActionMasked()==MotionEvent.ACTION_HOVER_EXIT)hideHover();return false;});}
    private void hideHover(){if(pending!=null)main.removeCallbacks(pending);pending=null;if(hover!=null){hover.dismiss();hover=null;}}
    boolean dismiss(){boolean was=dialog!=null||hover!=null;hideHover();if(dialog!=null){Dialog old=dialog;dialog=null;old.dismiss();}return was;}
}
