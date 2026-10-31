package com.nthexperience.desktop;

import android.app.Activity;
import android.app.Application;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import java.util.concurrent.*;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.ComponentName;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import java.util.function.Predicate;

/** Dependency-free on-device smoke test. Run on a disposable emulator. */
public final class LauncherSmoke extends Instrumentation {
    private MainActivity activity;
    private View root;
    private final BlockingQueue<MainActivity> resumed=new LinkedBlockingQueue<>();
    private final Application.ActivityLifecycleCallbacks lifecycle=new Application.ActivityLifecycleCallbacks(){
        public void onActivityResumed(Activity a){if(a instanceof MainActivity)resumed.offer((MainActivity)a);}
        public void onActivityCreated(Activity a,Bundle b){}public void onActivityStarted(Activity a){}public void onActivityPaused(Activity a){}public void onActivityStopped(Activity a){}public void onActivitySaveInstanceState(Activity a,Bundle b){}public void onActivityDestroyed(Activity a){}
    };
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){
        Bundle result=new Bundle();String previous=null;String oldSticky=getTargetContext().getSharedPreferences("desktop",0).getString("gadget_note","");String oldGadgets=getTargetContext().getSharedPreferences("desktop",0).getString("gadgets",null);progress("runner started");
        ((Application)getTargetContext().getApplicationContext()).registerActivityLifecycleCallbacks(lifecycle);
        try {
            Intent launch=new Intent(Intent.ACTION_MAIN).setComponent(new ComponentName(getTargetContext(),MainActivity.class)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            progress("starting launcher activity");activity=(MainActivity)startActivitySync(launch);progress("activity started; waiting for UI idle");waitForIdleSync();root=activity.getWindow().getDecorView();
            check(find(v->"Start menu".equals(v.getContentDescription()))!=null,"Desktop has a Start button");snapshot("desktop.png");
            ui(()->find(v->"Start menu".equals(v.getContentDescription())).performClick());
            check(find(v->"Clock gadget".equals(v.getContentDescription()))!=null,"Clock gadget is visible");
            ListView list=(ListView)find(v->v instanceof ListView);
            for(int i=0;i<40&&list.getCount()==0;i++){Thread.sleep(250);waitForIdleSync();}
            check(list.getCount()>0,"Installed apps loaded");progress("app discovery passed");
            EditText search=(EditText)find(v->v instanceof EditText&&"Search installed apps".equals(v.getContentDescription()));
            ui(()->search.setText("__no_such_installed_program__"));check(list.getCount()==0,"Search filters unmatched apps");
            ui(()->search.setText(""));check(list.getCount()>0,"Clearing search restores apps");
            int recentCount=list.getCount();ui(()->find(v->v instanceof TextView&&"All Programs  ›".contentEquals(((TextView)v).getText())).performClick());check(list.getCount()>=recentCount,"All Programs expands the list");snapshot("start-menu.png");
            progress("opening Notepad");ui(()->activity.open("@notes"));
            EditText editor=(EditText)find(v->v instanceof EditText&&"Notepad text".equals(v.getContentDescription()));
            check(editor!=null,"Notepad opens");previous=editor.getText().toString();
            ui(()->editor.setText("Nth Desktop smoke test"));snapshot("notepad.png");
            ui(()->find(v->"Minimize window".equals(v.getContentDescription())).performClick());
            check(find(v->v instanceof EditText&&"Notepad text".equals(v.getContentDescription()))==null,"Minimize hides the editor");
            ui(()->find(v->v instanceof TextView&&v.isClickable()&&"Notepad".contentEquals(((TextView)v).getText())).performClick());
            check(find(v->v instanceof EditText&&"Notepad text".equals(v.getContentDescription()))!=null,"Taskbar restores Notepad");
            ui(()->find(v->"Maximize or restore window".equals(v.getContentDescription())).performClick());
            ui(()->find(v->"Maximize or restore window".equals(v.getContentDescription())).performClick());
            ui(()->find(v->"Close window".equals(v.getContentDescription())).performClick());
            ui(()->activity.open("@notes"));
            EditText reopened=(EditText)find(v->v instanceof EditText&&"Notepad text".equals(v.getContentDescription()));
            check("Nth Desktop smoke test".contentEquals(reopened.getText()),"Note survives closing and reopening its window");
            String original=previous;ui(()->reopened.setText(original));
            ui(()->find(v->"Close window".equals(v.getContentDescription())).performClick());
            progress("note and window checks passed");ui(()->activity.open("@appearance"));
            check(find(v->"Use Twilight · violet and rose wallpaper".equals(v.getContentDescription()))!=null,"Personalization opens");
            ui(()->find(v->"Close window".equals(v.getContentDescription())).performClick());
            ui(()->activity.open("@gadgets"));
            check(find(v->"Add Notes gadget".equals(v.getContentDescription()))!=null,"Gadget gallery offers Notes");
            ui(()->find(v->"Add Notes gadget".equals(v.getContentDescription())).performClick());
            check(new DesktopStore(getTargetContext()).gadgets().contains("notes"),"Adding a gadget is persisted");snapshot("gadget-gallery.png");
            ui(()->find(v->"Close window".equals(v.getContentDescription())).performClick());
            ui(()->activity.open("@sticky"));EditText sticky=(EditText)find(v->"Sticky note text".equals(v.getContentDescription()));
            check(sticky!=null,"Sticky Notes opens");ui(()->sticky.setText("Remember the little things."));
            ui(()->find(v->"Close window".equals(v.getContentDescription())).performClick());
            check("Remember the little things.".equals(new DesktopStore(getTargetContext()).prefs.getString("gadget_note","")),"Sticky note saves independently");
            ui(()->new DesktopStore(getTargetContext()).prefs.edit().putString("gadget_note",oldSticky).apply());
            ui(()->activity.sidebarChanged());
            WeatherClient.Snapshot forecast=WeatherClient.Snapshot.parse("{\"properties\":{\"timeseries\":[{\"time\":\"2026-10-31T12:00:00Z\",\"data\":{\"instant\":{\"details\":{\"air_temperature\":25}},\"next_1_hours\":{\"summary\":{\"symbol_code\":\"partlycloudy_day\"}}}}]}}");
            check(forecast.temperature(false)==25&&forecast.temperature(true)==77,"Weather unit conversion");check("Partly cloudy".equals(forecast.condition()),"Weather condition labels");
            check("12.345".equals(WeatherClient.coordinate(12.34567)),"Weather coordinates use limited precision");
            ui(()->activity.gadgetAction("weather"));check(find(v->"Weather city search".equals(v.getContentDescription()))!=null,"Weather can be configured without location permission");
            ui(()->find(v->"Close window".equals(v.getContentDescription())).performClick());
            ui(()->activity.gadgetAction("clock"));check(find(v->v instanceof android.widget.Switch)!=null,"Clock settings opens");
            ui(()->find(v->"Close window".equals(v.getContentDescription())).performClick());
            snapshot("gadgets-desktop.png");
            ui(()->activity.gadgetAction("calendar"));snapshot("calendar.png");ui(()->find(v->"Close window".equals(v.getContentDescription())).performClick());
            layoutChecks();
            result.putString("stream","PASS: landscape, gadget drag/dock/resize, independent saved layouts, layout lock, rotation and activity recreation with window/cursor state; app discovery, Start search/All Programs, autosave, window controls, wallpapers, gadget gallery, sticky notes, clock settings, weather parsing and configuration.\n");
            restoreGadgets(oldGadgets);((Application)getTargetContext().getApplicationContext()).unregisterActivityLifecycleCallbacks(lifecycle);finish(Activity.RESULT_OK,result);
        } catch(Throwable failure){
            if(previous!=null)getTargetContext().getSharedPreferences("desktop",0).edit().putString("note",previous).commit();
            getTargetContext().getSharedPreferences("desktop",0).edit().putString("gadget_note",oldSticky).commit();restoreGadgets(oldGadgets);
            failure.printStackTrace();try{snapshot("failure.png");}catch(Exception ignored){}
            ((Application)getTargetContext().getApplicationContext()).unregisterActivityLifecycleCallbacks(lifecycle);result.putString("stream","FAIL: "+failure+"\n");finish(Activity.RESULT_CANCELED,result);
        }
    }
    private void layoutChecks() throws Exception {
        progress("dragging Clock out of the Sidebar");
        View clock=find(v->"Clock gadget".equals(v.getContentDescription()));check(clock!=null,"Clock available for drag");
        int[] rootLocation=new int[2];root.getLocationOnScreen(rootLocation);
        drag(clock,rootLocation[0]+Math.round(root.getWidth()*.32f),rootLocation[1]+Math.round(root.getHeight()*.28f));
        check(find(v->"Clock floating gadget".equals(v.getContentDescription()))!=null,"Long drag detaches a gadget");
        GadgetLayout portrait=new GadgetLayout(new DesktopStore(getTargetContext()),"portrait");check(!portrait.docked("clock"),"Detached position saved");
        View grip=find(v->"Resize Clock gadget".equals(v.getContentDescription()));check(grip!=null,"Floating gadget has resize grip");
        int[] loc=new int[2];grip.getLocationOnScreen(loc);gesture(grip,loc[0]+grip.getWidth()/2f+35*activity.getResources().getDisplayMetrics().density,loc[1]+grip.getHeight()/2f+35*activity.getResources().getDisplayMetrics().density,false);
        portrait=new GadgetLayout(new DesktopStore(getTargetContext()),"portrait");check(portrait.scale("clock")>1.1f,"Corner grip resizes and persists");String portraitSaved=new DesktopStore(getTargetContext()).prefs.getString("gadget_layout_portrait","");snapshot("floating-gadgets.png");
        ui(()->activity.open("@notes"));EditText editor=(EditText)find(v->"Notepad text".equals(v.getContentDescription()));String oldNote=editor.getText().toString();ui(()->{editor.setText("Landscape remembers my place.");editor.requestFocus();editor.setSelection(9);});
        progress("rotating with Notepad open");rotate(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,Configuration.ORIENTATION_LANDSCAPE);
        check(root.getWidth()>root.getHeight(),"Landscape uses a wide desktop");EditText wideEditor=(EditText)find(v->"Notepad text".equals(v.getContentDescription()));check(wideEditor!=null&&"Landscape remembers my place.".contentEquals(wideEditor.getText()),"Open note survives rotation");check(wideEditor.getSelectionStart()==9,"Cursor survives rotation");snapshot("landscape-notepad.png");
        ui(()->find(v->"Minimize window".equals(v.getContentDescription())).performClick());snapshot("landscape.png");
        ui(()->activity.open("@calendar"));View wideCalendar=find(v->v instanceof MonthCalendarView);check(wideCalendar!=null&&wideCalendar.getHeight()>=Aero.dp(activity,240),"Landscape calendar keeps readable rows in a scrollable grid");snapshot("landscape-calendar.png");android.widget.ScrollView calendarScroll=(android.widget.ScrollView)((View)wideCalendar.getParent()).getParent();int[] calendarLocation=new int[2];calendarScroll.getLocationOnScreen(calendarLocation);gesture(calendarScroll,calendarLocation[0]+calendarScroll.getWidth()/2f,calendarLocation[1]+Aero.dp(activity,12),false);check(calendarScroll.getScrollY()>0,"Landscape calendar scrolls to its lower rows and controls");
        int calendarOffset=calendarScroll.getScrollY();ui(()->((MonthCalendarView)wideCalendar).offset(1));String calendarMonth=((MonthCalendarView)wideCalendar).title();MainActivity beforeCalendarRecreation=activity;resumed.clear();ui(()->activity.recreate());awaitReplacement(beforeCalendarRecreation,Configuration.ORIENTATION_LANDSCAPE);
        MonthCalendarView recreatedCalendar=(MonthCalendarView)find(v->v instanceof MonthCalendarView);android.widget.ScrollView recreatedScroll=(android.widget.ScrollView)((View)recreatedCalendar.getParent()).getParent();check(calendarMonth.equals(recreatedCalendar.title()),"Calendar month survives recreation");check(Math.abs(calendarOffset-recreatedScroll.getScrollY())<=1,"Calendar scroll position survives recreation after layout");ui(()->find(v->"Close window".equals(v.getContentDescription())).performClick());
        ui(()->find(v->"Start menu".equals(v.getContentDescription())).performClick());check(find(v->v instanceof TextView&&"Gadgets".contentEquals(((TextView)v).getText()))!=null,"Landscape navigation remains reachable");snapshot("landscape-start.png");
        EditText search=(EditText)find(v->"Search installed apps".equals(v.getContentDescription()));ui(()->search.setText("settings"));
        rotate(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,Configuration.ORIENTATION_PORTRAIT);
        EditText restoredSearch=(EditText)find(v->"Search installed apps".equals(v.getContentDescription()));check(restoredSearch!=null&&"settings".contentEquals(restoredSearch.getText()),"Open Start query survives rotation");
        ui(()->find(v->"Start menu".equals(v.getContentDescription())).performClick());
        check(find(v->"Notepad text".equals(v.getContentDescription()))==null,"Minimized state survives rotation");
        check(portraitSaved.equals(new DesktopStore(getTargetContext()).prefs.getString("gadget_layout_portrait","")),"Portrait layout is unchanged by landscape fitting");
        progress("checking docking, layout lock and recreation");
        View floatingClock=find(v->"Clock gadget".equals(v.getContentDescription()));View dock=find(v->"Desktop gadget sidebar".equals(v.getContentDescription()));int[] dockLoc=new int[2];dock.getLocationOnScreen(dockLoc);drag(floatingClock,dockLoc[0]+dock.getWidth()/2,dockLoc[1]+Math.min(dock.getHeight()-20,40));
        check(find(v->"Clock docked gadget".equals(v.getContentDescription()))!=null,"Dropping on Sidebar docks a gadget");
        check(!new GadgetLayout(new DesktopStore(getTargetContext()),"landscape").docked("clock"),"Docking in portrait leaves landscape placement independent");
        ui(()->new DesktopStore(getTargetContext()).prefs.edit().putBoolean("layout_locked",true).apply());
        View locked=find(v->"Clock gadget".equals(v.getContentDescription()));drag(locked,root.getWidth()*.3f,root.getHeight()*.25f);check(new GadgetLayout(new DesktopStore(getTargetContext()),"portrait").docked("clock"),"Lock prevents gadget moves");
        ui(()->new DesktopStore(getTargetContext()).prefs.edit().putBoolean("layout_locked",false).apply());
        ui(()->activity.open("@notes"));EditText before=(EditText)find(v->"Notepad text".equals(v.getContentDescription()));ui(()->{before.requestFocus();before.setSelection(4);});
        MainActivity previous=activity;resumed.clear();ui(()->activity.recreate());awaitReplacement(previous,Configuration.ORIENTATION_PORTRAIT);
        EditText recreated=(EditText)find(v->"Notepad text".equals(v.getContentDescription()));check(recreated!=null&&recreated.getSelectionStart()==4,"Activity recreation restores window and cursor");ui(()->recreated.setText(oldNote));
        ui(()->find(v->"Close window".equals(v.getContentDescription())).performClick());snapshot("restored-portrait.png");
    }
    private void rotate(int requested,int expected) throws Exception {MainActivity previous=activity;resumed.clear();ui(()->activity.setRequestedOrientation(requested));awaitReplacement(previous,expected);}
    private void awaitReplacement(MainActivity previous,int orientation) throws Exception {
        long deadline=SystemClock.uptimeMillis()+15000;MainActivity next=null;
        while(SystemClock.uptimeMillis()<deadline){MainActivity candidate=resumed.poll(1,TimeUnit.SECONDS);if(candidate!=null&&candidate!=previous&&candidate.getResources().getConfiguration().orientation==orientation){next=candidate;break;}}
        check(next!=null,"Activity recreated in requested orientation");activity=next;waitForIdleSync();Thread.sleep(300);waitForIdleSync();root=activity.getWindow().getDecorView();
    }
    private void drag(View source,float x,float y) throws Exception {gesture(source,x,y,true);}
    private void gesture(View source,float x,float y,boolean hold) throws Exception {
        check(source!=null,"Gesture source exists");int[] location=new int[2];source.getLocationOnScreen(location);float startX=location[0]+source.getWidth()/2f,startY=location[1]+source.getHeight()/2f;long down=SystemClock.uptimeMillis();inject(down,MotionEvent.ACTION_DOWN,startX,startY);Thread.sleep(hold?700:40);
        for(int i=1;i<=12;i++){inject(down,MotionEvent.ACTION_MOVE,startX+(x-startX)*i/12,startY+(y-startY)*i/12);Thread.sleep(20);}inject(down,MotionEvent.ACTION_UP,x,y);Thread.sleep(300);waitForIdleSync();
    }
    private void inject(long down,int action,float x,float y){MotionEvent event=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,x,y,0);event.setSource(InputDevice.SOURCE_TOUCHSCREEN);getUiAutomation().injectInputEvent(event,true);event.recycle();}
    private void restoreGadgets(String value){android.content.SharedPreferences.Editor e=getTargetContext().getSharedPreferences("desktop",0).edit();if(value==null)e.remove("gadgets");else e.putString("gadgets",value);e.commit();}
    private void snapshot(String name) throws java.io.IOException {
        try{Thread.sleep(250);}catch(InterruptedException interrupted){Thread.currentThread().interrupt();throw new java.io.IOException(interrupted);}
        waitForIdleSync();
        android.graphics.Bitmap bitmap=getUiAutomation().takeScreenshot();
        if(bitmap==null)throw new java.io.IOException("Screenshot unavailable");
        java.io.File output=new java.io.File(getTargetContext().getFilesDir(),name);
        try(java.io.FileOutputStream stream=new java.io.FileOutputStream(output)){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,stream);}finally{bitmap.recycle();}
    }
    private void progress(String message){Bundle status=new Bundle();status.putString("stream","CHECK: "+message+"\n");sendStatus(1,status);}
    private void ui(Runnable action){runOnMainSync(action);waitForIdleSync();}
    private View find(Predicate<View> match){return find(root,match);}
    private View find(View view,Predicate<View> match){
        if(!view.isShown())return null;
        if(match.test(view))return view;
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){View found=find(group.getChildAt(i),match);if(found!=null)return found;}}
        return null;
    }
    private void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
