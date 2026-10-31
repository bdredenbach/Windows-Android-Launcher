package com.nthexperience.desktop;

import android.app.Activity;
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
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){
        Bundle result=new Bundle();String previous=null;String oldSticky=getTargetContext().getSharedPreferences("desktop",0).getString("gadget_note","");String oldGadgets=getTargetContext().getSharedPreferences("desktop",0).getString("gadgets",null);progress("runner started");
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
            result.putString("stream","PASS: app discovery, Start search/All Programs, autosave, window controls, wallpapers, gadget gallery, sticky notes, clock settings, weather parsing and configuration.\n");
            restoreGadgets(oldGadgets);finish(Activity.RESULT_OK,result);
        } catch(Throwable failure){
            if(previous!=null)getTargetContext().getSharedPreferences("desktop",0).edit().putString("note",previous).commit();
            getTargetContext().getSharedPreferences("desktop",0).edit().putString("gadget_note",oldSticky).commit();restoreGadgets(oldGadgets);
            result.putString("stream","FAIL: "+failure+"\n");finish(Activity.RESULT_CANCELED,result);
        }
    }
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
