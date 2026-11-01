package com.nthexperience.desktop;

import android.content.Context;
import android.hardware.*;
import android.net.TrafficStats;
import android.os.*;
import android.view.*;
import java.util.*;

/** Foreground-only samples; never substitutes invented readings for unavailable hardware. */
final class GadgetHardware implements SensorEventListener {
    private final Context context;
    private final Runnable changed;
    private final SensorManager sensors;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private boolean active,network,compass,haveGravity,haveMagnetic;
    private long time,rx=-1,tx=-1;
    long received=-1,sent=-1;
    double down=-1,up=-1;
    float heading=Float.NaN;
    String compassStatus="Waiting for compass";
    final ArrayDeque<Double> history=new ArrayDeque<>();
    private final float[] gravity=new float[3],magnetic=new float[3],matrix=new float[9];
    private final Runnable sample=new Runnable(){public void run(){if(!active||!network)return;sampleTraffic(TrafficStats.getTotalRxBytes(),TrafficStats.getTotalTxBytes(),SystemClock.elapsedRealtime());changed.run();handler.postDelayed(this,1000);}};
    GadgetHardware(Context context,Runnable changed){this.context=context;this.changed=changed;sensors=(SensorManager)context.getSystemService(Context.SENSOR_SERVICE);}
    void sampleTraffic(long nextRx,long nextTx,long now){
        received=nextRx;sent=nextTx;
        down=rate(rx,nextRx,now-time);up=rate(tx,nextTx,now-time);
        if(down>=0){history.addLast(down);while(history.size()>32)history.removeFirst();}
        rx=nextRx;tx=nextTx;time=now;
    }
    static double rate(long before,long after,long millis){return before<0||after<before||millis<=0?-1:(after-before)*1000d/millis;}
    static String speed(double value){return value<0?"—":value>=1048576?String.format(Locale.getDefault(),"%.1f MiB/s",value/1048576):String.format(Locale.getDefault(),"%.1f KiB/s",value/1024);}
    void active(boolean value,boolean network,boolean compass){
        if(active==value&&this.network==network&&this.compass==compass)return;
        handler.removeCallbacksAndMessages(null);if(sensors!=null)sensors.unregisterListener(this);
        active=value;this.network=network;this.compass=compass;rx=tx=-1;down=up=-1;time=0;haveGravity=haveMagnetic=false;heading=Float.NaN;
        if(!value)return;
        if(network)handler.post(sample);
        if(compass){
            compassStatus="Waiting for compass";Sensor vector=sensors==null?null:sensors.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            boolean registered=false;
            if(vector!=null)registered=sensors.registerListener(this,vector,SensorManager.SENSOR_DELAY_UI);
            else if(sensors!=null){Sensor a=sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),m=sensors.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);if(a!=null&&m!=null){boolean first=sensors.registerListener(this,a,SensorManager.SENSOR_DELAY_UI),second=sensors.registerListener(this,m,SensorManager.SENSOR_DELAY_UI);registered=first&&second;if(!registered)sensors.unregisterListener(this);}}
            if(!registered)compassStatus="Compass sensor unavailable";
            changed.run();
        }
    }
    @Override public void onSensorChanged(SensorEvent event){
        if(!active||!compass)return;
        if(event.sensor.getType()==Sensor.TYPE_ROTATION_VECTOR)SensorManager.getRotationMatrixFromVector(matrix,event.values);
        else {if(event.sensor.getType()==Sensor.TYPE_ACCELEROMETER){System.arraycopy(event.values,0,gravity,0,3);haveGravity=true;}else if(event.sensor.getType()==Sensor.TYPE_MAGNETIC_FIELD){System.arraycopy(event.values,0,magnetic,0,3);haveMagnetic=true;}if(!haveGravity||!haveMagnetic||!SensorManager.getRotationMatrix(matrix,null,gravity,magnetic))return;}
        WindowManager manager=(WindowManager)context.getSystemService(Context.WINDOW_SERVICE);int rotation=manager.getDefaultDisplay().getRotation();
        heading=bearing(matrix,rotation);if(!Float.isFinite(heading))return;compassStatus=event.accuracy<=SensorManager.SENSOR_STATUS_ACCURACY_LOW?"Low accuracy · move in a figure eight":"Magnetic north · hold phone flat";changed.run();
    }
    static float bearing(float[] matrix,int rotation){
        int x=rotation==Surface.ROTATION_90?SensorManager.AXIS_Y:rotation==Surface.ROTATION_180?SensorManager.AXIS_MINUS_X:rotation==Surface.ROTATION_270?SensorManager.AXIS_MINUS_Y:SensorManager.AXIS_X;
        int y=rotation==Surface.ROTATION_90?SensorManager.AXIS_MINUS_X:rotation==Surface.ROTATION_180?SensorManager.AXIS_MINUS_Y:rotation==Surface.ROTATION_270?SensorManager.AXIS_X:SensorManager.AXIS_Y;
        float[] remapped=new float[9],angles=new float[3];if(!SensorManager.remapCoordinateSystem(matrix,x,y,remapped))return Float.NaN;SensorManager.getOrientation(remapped,angles);return (float)((Math.toDegrees(angles[0])+360)%360);
    }
    @Override public void onAccuracyChanged(Sensor sensor,int accuracy){}
    void close(){active(false,false,false);}
    boolean running(){return active;}
}
