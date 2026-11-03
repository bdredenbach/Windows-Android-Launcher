package com.nthexperience.desktop;

import android.app.Activity;
import android.content.*;
import android.net.Uri;
import android.provider.Settings;
import android.view.WindowManager;
import android.widget.*;

/** An explicit choice between a window override and Android's shared setting. */
final class BrightnessControl {
    private final Activity activity;private final DesktopStore store;
    BrightnessControl(Activity activity){this.activity=activity;store=new DesktopStore(activity);}
    boolean system(){return store.prefs.getBoolean("brightness_system",false)&&Settings.System.canWrite(activity);}
    void apply(){WindowManager.LayoutParams attrs=activity.getWindow().getAttributes();int level=store.prefs.getInt("brightness_window",-1);attrs.screenBrightness=system()||level<0?-1:Math.max(.02f,Math.min(1,level/100f));activity.getWindow().setAttributes(attrs);}
    int level(){if(!system())return store.prefs.getInt("brightness_window",-1);return Math.round(Settings.System.getInt(activity.getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,128)*100f/255);}
    void set(int value){value=Math.max(2,Math.min(100,value));if(system())try{Settings.System.putInt(activity.getContentResolver(),Settings.System.SCREEN_BRIGHTNESS_MODE,Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);if(!Settings.System.putInt(activity.getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,Math.round(value*255f/100)))throw new SecurityException();}catch(RuntimeException ex){Toast.makeText(activity,"Use Android Display settings to change phone brightness.",Toast.LENGTH_LONG).show();}else store.prefs.edit().putInt("brightness_window",value).apply();apply();}
    void content(LinearLayout body){
        TextView label=Aero.text(activity,"",13,Aero.INK);Runnable update=()->label.setText((system()?"Whole-phone brightness":"Nth Desktop brightness")+": "+(level()<0?"Follow phone":level()+"%"));update.run();body.addView(label);
        SeekBar slider=new SeekBar(activity);slider.setMax(98);slider.setProgress(Math.max(0,(level()<0?50:level())-2));slider.setContentDescription("Brightness");slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){DesktopSounds.play(activity,"complete");}public void onProgressChanged(SeekBar b,int value,boolean user){if(user){set(value+2);update.run();}}});body.addView(slider,new LinearLayout.LayoutParams(-1,Aero.dp(activity,48)));
        TextView info=Aero.text(activity,system()?"Moving the slider selects manual brightness for the phone.":"Applies while Nth Desktop is visible. Other apps follow your phone's setting.",12,0xff496171);body.addView(info);
        body.addView(Aero.button(activity,"Follow phone brightness",()->{store.prefs.edit().putBoolean("brightness_system",false).putInt("brightness_window",-1).apply();apply();body.removeAllViews();content(body);}));
        body.addView(Aero.button(activity,Settings.System.canWrite(activity)?"Use whole-phone control":"Enable whole-phone control…",()->{
            if(Settings.System.canWrite(activity)){store.prefs.edit().putBoolean("brightness_system",true).apply();apply();body.removeAllViews();content(body);return;}
            try{store.prefs.edit().putBoolean("brightness_system",true).apply();activity.startActivity(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,Uri.parse("package:"+activity.getPackageName())));}catch(ActivityNotFoundException|SecurityException ex){Toast.makeText(activity,"Open Android settings to allow modifying system settings.",Toast.LENGTH_LONG).show();}
        }));
        body.addView(Aero.button(activity,"Android Display settings",()->{try{activity.startActivity(new Intent(Settings.ACTION_DISPLAY_SETTINGS));}catch(ActivityNotFoundException ignored){}}));
    }
}
