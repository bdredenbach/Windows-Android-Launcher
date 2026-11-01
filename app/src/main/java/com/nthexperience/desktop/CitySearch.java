package com.nthexperience.desktop;

import android.os.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/** Manual, bounded Photon/OSM searches; independent of app discovery and backup workers. */
final class CitySearch {
    static final class Place {final String label;final double latitude,longitude;Place(String label,double lat,double lon){this.label=label;latitude=lat;longitude=lon;}}
    interface Callback {void done(List<Place> places,String error);}
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private final LinkedHashMap<String,List<Place>> cache=new LinkedHashMap<>();
    private volatile HttpURLConnection connection;
    private volatile int generation;private boolean closed;
    void search(String query,Callback callback){
        cancel();int request=generation;String key=query.trim().toLowerCase(Locale.ROOT);if(cache.containsKey(key)){callback.done(cache.get(key),"");return;}
        main.postDelayed(()->{if(!closed&&request==generation){cancel();callback.done(Collections.emptyList(),"City search took too long. Check your connection and try again.");}},15000);
        worker.execute(()->{if(closed||request!=generation)return;List<Place> places=Collections.emptyList();String error="";HttpURLConnection current=null;try{
            current=(HttpURLConnection)new URL("https://photon.komoot.io/api/?q="+URLEncoder.encode(query,"UTF-8")+"&limit=5&lang=en").openConnection();connection=current;current.setRequestProperty("User-Agent",WeatherClient.AGENT);current.setConnectTimeout(6000);current.setReadTimeout(7000);if(current.getResponseCode()!=200)throw new IOException();places=parse(read(current.getInputStream(),400000));if(places.isEmpty())error="No matching places. Try a city with its state or country.";
        }catch(IOException|JSONException|RuntimeException ex){error="City search is unavailable. Check your connection, or use your location.";}finally{if(current!=null)current.disconnect();if(connection==current)connection=null;}
            List<Place> found=places;String message=error;main.post(()->{if(closed||request!=generation)return;main.removeCallbacksAndMessages(null);if(!found.isEmpty()){cache.put(key,found);while(cache.size()>20)cache.remove(cache.keySet().iterator().next());}callback.done(found,message);});
        });
    }
    static List<Place> parse(String text)throws JSONException {JSONArray features=new JSONObject(text).getJSONArray("features");List<Place> result=new ArrayList<>();for(int i=0;i<Math.min(features.length(),10)&&result.size()<5;i++){JSONObject feature=features.getJSONObject(i),properties=feature.getJSONObject("properties");JSONArray xy=feature.getJSONObject("geometry").getJSONArray("coordinates");double lon=xy.getDouble(0),lat=xy.getDouble(1);if(!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180)continue;LinkedHashSet<String> parts=new LinkedHashSet<>();for(String key:new String[]{"name","city","state","country"}){String part=properties.optString(key,"").trim();if(!part.isEmpty())parts.add(part);}String label=String.join(", ",parts);if(!label.isEmpty())result.add(new Place(label.substring(0,Math.min(240,label.length())),lat,lon));}return result;}
    static String read(InputStream stream,int max)throws IOException {try(InputStream in=stream;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] bytes=new byte[8192];int n;while((n=in.read(bytes))!=-1){if(out.size()+n>max)throw new IOException("Response too large");out.write(bytes,0,n);}return out.toString(StandardCharsets.UTF_8.name());}}
    void cancel(){generation++;main.removeCallbacksAndMessages(null);HttpURLConnection old=connection;if(old!=null)old.disconnect();}
    void close(){closed=true;cancel();worker.shutdownNow();}
}
