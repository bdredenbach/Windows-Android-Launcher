package com.nthexperience.desktop;

import android.app.NotificationManager;
import android.content.Context;
import android.media.*;
import android.os.*;
import android.widget.*;

/** Original brief synthesized cues, opt-in, respecting silent/DND and system volume. */
final class DesktopSounds {
    private static long last;private static AudioTrack track;
    static void play(Context context,String event){
        DesktopStore store=new DesktopStore(context);AudioManager audio=context.getSystemService(AudioManager.class);NotificationManager notices=context.getSystemService(NotificationManager.class);
        if(!store.prefs.getBoolean("desktop_sounds",false)||!store.prefs.getBoolean("sound_"+event,true)||audio==null||audio.getRingerMode()!=AudioManager.RINGER_MODE_NORMAL||audio.getStreamVolume(AudioManager.STREAM_SYSTEM)==0||audio.isMusicActive()||notices!=null&&notices.getCurrentInterruptionFilter()!=NotificationManager.INTERRUPTION_FILTER_ALL||SystemClock.uptimeMillis()-last<180)return;
        last=SystemClock.uptimeMillis();if(track!=null)return;
        int rate=16000;short[] pcm=new short[rate/6];double a=event.equals("error")?330:event.equals("notice")?660:523.25,b=event.equals("error")?277.18:event.equals("notice")?880:783.99;
        for(int i=0;i<pcm.length;i++){double t=i/(double)rate,envelope=Math.sin(Math.PI*i/pcm.length);pcm[i]=(short)(5500*envelope*(Math.sin(2*Math.PI*a*t)+.45*Math.sin(2*Math.PI*b*t)));}
        try{AudioTrack sound=new AudioTrack.Builder().setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).setAudioFormat(new AudioFormat.Builder().setSampleRate(rate).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()).setTransferMode(AudioTrack.MODE_STATIC).setBufferSizeInBytes(pcm.length*2).build();track=sound;sound.write(pcm,0,pcm.length);sound.setVolume(store.prefs.getInt("desktop_sound_volume",30)/100f);sound.play();new Handler(Looper.getMainLooper()).postDelayed(()->{if(track==sound)track=null;try{sound.release();}catch(RuntimeException ignored){}},230);}catch(RuntimeException ignored){stop();}
    }
    static void stop(){if(track!=null){try{track.release();}catch(RuntimeException ignored){}track=null;}}
    static void settings(Context context,LinearLayout body){DesktopStore store=new DesktopStore(context);for(String[] option:new String[][]{{"desktop_sounds","Enable desktop sound effects"},{"sound_complete","File operation completed"},{"sound_error","Errors"},{"sound_notice","Desktop notifications"}}){CheckBox check=Aero.check(context,option[1],store.prefs.getBoolean(option[0],!option[0].equals("desktop_sounds")));check.setOnCheckedChangeListener((button,value)->store.prefs.edit().putBoolean(option[0],value).apply());body.addView(check);}SeekBar volume=new SeekBar(context);volume.setContentDescription("Desktop sound volume");volume.setMax(100);volume.setProgress(store.prefs.getInt("desktop_sound_volume",30));volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){play(context,"complete");}public void onProgressChanged(SeekBar b,int value,boolean user){if(user)store.prefs.edit().putInt("desktop_sound_volume",value).apply();}});body.addView(volume);body.addView(Aero.text(context,"Original short cues. Silent mode, Do Not Disturb and system volume are respected.",12,0xff496171));}
}
