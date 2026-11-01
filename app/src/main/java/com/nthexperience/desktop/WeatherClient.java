package com.nthexperience.desktop;

import android.os.Handler;
import android.os.Looper;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Locationforecast is cached and fetched only while this launcher is visible. */
final class WeatherClient {
    static final String AGENT="NthDesktop/0.10.0 (https://github.com/bdredenbach/Windows-Android-Launcher)";
    static final class Snapshot {
        final double celsius; final String symbol; final long forecastTime;
        Snapshot(double celsius,String symbol,long time){this.celsius=celsius;this.symbol=symbol;forecastTime=time;}
        static Snapshot parse(String json) throws JSONException {
            JSONObject point=new JSONObject(json).getJSONObject("properties").getJSONArray("timeseries").getJSONObject(0);
            JSONObject data=point.getJSONObject("data");double temp=data.getJSONObject("instant").getJSONObject("details").getDouble("air_temperature");
            if(!Double.isFinite(temp)||temp< -120||temp>80)throw new JSONException("Invalid temperature");
            JSONObject summary=data.optJSONObject("next_1_hours");if(summary==null)summary=data.optJSONObject("next_6_hours");
            String symbol=summary==null?"unknown":summary.getJSONObject("summary").optString("symbol_code","unknown");
            long time;try{time=Instant.parse(point.getString("time")).toEpochMilli();}catch(RuntimeException e){throw new JSONException("Invalid forecast time");}
            return new Snapshot(temp,symbol,time);
        }
        long temperature(boolean fahrenheit){return Math.round(fahrenheit?celsius*9/5+32:celsius);}
        String condition(){
            String s=symbol.toLowerCase(Locale.ROOT);
            if(s.contains("thunder"))return "Thunderstorms";
            if(s.contains("snow"))return "Snow";
            if(s.contains("sleet"))return "Sleet";
            if(s.contains("rain"))return s.contains("light")?"Light rain":"Rain";
            if(s.contains("fog"))return "Fog";
            if(s.contains("partlycloudy")||s.contains("fair"))return "Partly cloudy";
            if(s.contains("cloudy"))return "Cloudy";
            if(s.contains("clearsky"))return s.contains("night")?"Clear":"Sunny";
            return "Forecast";
        }
    }
    final DesktopStore store;
    private final Runnable changed;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private Snapshot snapshot;
    private boolean active,inFlight,closed,failed;
    private int generation;
    WeatherClient(DesktopStore store,Runnable changed){this.store=store;this.changed=changed;try{snapshot=Snapshot.parse(store.prefs.getString("weather_json",""));}catch(JSONException ignored){}}
    Snapshot snapshot(){return snapshot;}
    boolean configured(){return store.prefs.contains("weather_lat");}
    boolean loading(){return inFlight;}
    boolean stale(){return failed||System.currentTimeMillis()>store.prefs.getLong("weather_expires",0);}
    String city(){return store.prefs.getString("weather_city","Add a location");}
    long updated(){return store.prefs.getLong("weather_updated",0);}
    void active(boolean value){active=value;if(value)refresh(false);}
    void location(String label,double latitude,double longitude){
        generation++;snapshot=null;failed=false;
        store.prefs.edit().putString("weather_city",label).putString("weather_lat",coordinate(latitude)).putString("weather_lon",coordinate(longitude)).remove("weather_json").remove("weather_modified").remove("weather_expires").remove("weather_updated").remove("weather_retry_after").apply();changed.run();refresh(false);
    }
    static String coordinate(double value){return String.format(Locale.US,"%.3f",Math.floor(value*1000)/1000);}
    void refresh(boolean requested){
        if(closed||!active||inFlight||!configured())return;
        long now=System.currentTimeMillis();
        // Refresh never overrides the server's expiry, including the manual action.
        if(now<store.prefs.getLong("weather_expires",0)&&snapshot!=null){if(requested)changed.run();return;}
        if(now<store.prefs.getLong("weather_retry_after",0))return;
        inFlight=true;int requestGeneration=generation;String lat=store.prefs.getString("weather_lat","0"),lon=store.prefs.getString("weather_lon","0");String modified=store.prefs.getString("weather_modified","");changed.run();
        worker.execute(()->{
            HttpURLConnection connection=null;
            try{
                connection=(HttpURLConnection)new URL("https://api.met.no/weatherapi/locationforecast/2.0/compact?lat="+lat+"&lon="+lon).openConnection();
                connection.setRequestProperty("User-Agent",AGENT);if(!modified.isEmpty())connection.setRequestProperty("If-Modified-Since",modified);
                connection.setConnectTimeout(7000);connection.setReadTimeout(9000);
                int code=connection.getResponseCode();long received=System.currentTimeMillis();long expires=Math.max(received+60000,connection.getHeaderFieldDate("Expires",received+3600000));
                String last=connection.getHeaderField("Last-Modified");
                if(code==304&&snapshot!=null){main.post(()->complete(requestGeneration,null,null,expires,last));}
                else if(code==200||code==203){String json=read(connection.getInputStream());Snapshot parsed=Snapshot.parse(json);main.post(()->complete(requestGeneration,parsed,json,expires,last));}
                else throw new IOException("Weather service returned "+code);
            }catch(IOException|JSONException|RuntimeException e){main.post(()->{inFlight=false;if(closed)return;if(requestGeneration!=generation){refresh(false);return;}failed=true;store.prefs.edit().putLong("weather_retry_after",System.currentTimeMillis()+900000).apply();changed.run();});}
            finally{if(connection!=null)connection.disconnect();}
        });
    }
    private void complete(int request,Snapshot value,String json,long expires,String modified){
        inFlight=false;if(closed)return;if(request!=generation){refresh(false);return;}if(value!=null)snapshot=value;failed=false;
        android.content.SharedPreferences.Editor edit=store.prefs.edit().putLong("weather_expires",expires).putLong("weather_updated",System.currentTimeMillis()).remove("weather_retry_after");
        if(json!=null)edit.putString("weather_json",json);if(modified!=null)edit.putString("weather_modified",modified);edit.apply();changed.run();
    }
    private static String read(InputStream stream) throws IOException {
        try(InputStream in=stream;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] bytes=new byte[8192];int count,total=0;while((count=in.read(bytes))>=0){total+=count;if(total>2000000)throw new IOException("Response too large");out.write(bytes,0,count);}return out.toString(StandardCharsets.UTF_8.name());}
    }
    void close(){closed=true;active=false;worker.shutdownNow();main.removeCallbacksAndMessages(null);}
}
