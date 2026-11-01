package com.nthexperience.desktop;

import android.os.*;
import org.json.*;
import java.util.*;

final class GadgetData {
    static JSONObject all(DesktopStore store){try{return new JSONObject(store.prefs.getString("gadget_data","{}"));}catch(JSONException ex){return new JSONObject();}}
    static JSONObject portable(DesktopStore store){JSONObject data=all(store);for(String kind:new String[]{"timer","stopwatch"}){JSONObject value=data.optJSONObject(kind);if(value!=null){GadgetLayout.put(value,"boot",-1);GadgetLayout.put(value,"start_elapsed",0);}}return data;}
    static JSONObject get(DesktopStore store,String kind){JSONObject data=all(store).optJSONObject(kind);return data==null?new JSONObject():data;}
    static void save(DesktopStore store,String kind,JSONObject data){JSONObject all=all(store);GadgetLayout.put(all,kind,data);store.prefs.edit().putString("gadget_data",all.toString()).apply();}
    static long delta(JSONObject data,String wall,String elapsed,long nowWall,long nowElapsed,int boot){return Math.max(0,data.optInt("boot",-2)==boot&&boot>=0?nowElapsed-data.optLong(elapsed,nowElapsed):nowWall-data.optLong(wall,nowWall));}
    static long stopwatch(JSONObject data,long wall,long elapsed,int boot){return Math.min(315360000000L,data.optLong("elapsed",0)+(data.optBoolean("running")?delta(data,"start_wall","start_elapsed",wall,elapsed,boot):0));}
    static long remaining(JSONObject data,long wall,long elapsed,int boot){return Math.max(0,data.optLong("remaining",data.optLong("duration",300000))-(data.optBoolean("running")?delta(data,"start_wall","start_elapsed",wall,elapsed,boot):0));}
    static long value(DesktopStore store,String kind){JSONObject data=get(store,kind);int boot=StartupExperience.boot(store.context);return kind.equals("timer")?remaining(data,System.currentTimeMillis(),SystemClock.elapsedRealtime(),boot):stopwatch(data,System.currentTimeMillis(),SystemClock.elapsedRealtime(),boot);}
    static void start(JSONObject data,int boot){GadgetLayout.put(data,"running",true);GadgetLayout.put(data,"boot",boot);GadgetLayout.put(data,"start_wall",System.currentTimeMillis());GadgetLayout.put(data,"start_elapsed",SystemClock.elapsedRealtime());}
    static String duration(long millis,boolean fractions){long seconds=Math.max(0,millis)/1000,hours=seconds/3600;String result=hours>0?String.format(Locale.ROOT,"%d:%02d:%02d",hours,seconds/60%60,seconds%60):String.format(Locale.ROOT,"%02d:%02d",seconds/60,seconds%60);return fractions?result+String.format(Locale.ROOT,".%02d",millis/10%100):result;}
    static void validate(JSONObject all)throws JSONException {
        if(all.length()>8)throw new JSONException("Too many gadget settings");Iterator<String> names=all.keys();while(names.hasNext()){String kind=names.next();if(!GadgetCatalog.extra(kind))throw new JSONException("Unknown gadget settings");JSONObject data=all.getJSONObject(kind);if(data.length()>16)throw new JSONException("Too many gadget fields");
            if(kind.equals("worldclock"))for(String key:new String[]{"zone1","zone2"})if(data.has(key)&&!Arrays.asList(TimeZone.getAvailableIDs()).contains(data.getString(key)))throw new JSONException("Invalid time zone");
            if(kind.equals("checklist")){JSONArray tasks=data.optJSONArray("tasks");if(tasks!=null){if(tasks.length()>50)throw new JSONException("Too many tasks");for(int i=0;i<tasks.length();i++){JSONObject task=tasks.getJSONObject(i);Object text=task.get("text");if(!(text instanceof String)||((String)text).trim().isEmpty()||((String)text).length()>180||!(task.get("done") instanceof Boolean))throw new JSONException("Invalid checklist item");}}}
            if(kind.equals("timer")||kind.equals("stopwatch")){for(String key:new String[]{"duration","remaining","elapsed","start_wall","start_elapsed"})if(data.has(key)){Object value=data.get(key);if(!(value instanceof Number)||!Double.isFinite(((Number)value).doubleValue())||((Number)value).doubleValue()!=((Number)value).longValue()||((Number)value).longValue()<0||((Number)value).longValue()>10000000000000L)throw new JSONException("Invalid timer value");}if(data.has("duration")&&(data.getLong("duration")<1000||data.getLong("duration")>86400000))throw new JSONException("Invalid countdown duration");if(data.has("running")&&!(data.get("running") instanceof Boolean))throw new JSONException("Invalid timer state");JSONArray laps=data.optJSONArray("laps");if(laps!=null){if(laps.length()>20)throw new JSONException("Too many laps");for(int i=0;i<laps.length();i++)if(!(laps.get(i) instanceof Number)||laps.getDouble(i)<0||laps.getDouble(i)>315360000000L)throw new JSONException("Invalid lap");}}
            if(kind.equals("calculator")){if(data.optString("expression").length()>120||data.optString("result").length()>80)throw new JSONException("Calculator text too long");}
            if(kind.equals("converter")){int category=data.optInt("category",0);if(category<0||category>=GadgetMath.UNITS.length||data.optInt("from",0)<0||data.optInt("from",0)>=GadgetMath.UNITS[category].length||data.optInt("to",1)<0||data.optInt("to",1)>=GadgetMath.UNITS[category].length||data.optString("input").length()>64)throw new JSONException("Invalid converter settings");}
        }
    }
}
