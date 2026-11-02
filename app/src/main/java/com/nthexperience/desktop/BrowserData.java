package com.nthexperience.desktop;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.*;

/** Browser records live separately from launcher settings and desktop backups. */
final class BrowserData {
    final SharedPreferences prefs;
    BrowserData(Context context){prefs=context.getSharedPreferences("internet",Context.MODE_PRIVATE);}
    JSONArray list(String key){try{return new JSONArray(prefs.getString(key,"[]"));}catch(JSONException ex){return new JSONArray();}}
    void list(String key,JSONArray value){prefs.edit().putString(key,value.toString()).apply();}
    void remember(String key,String title,String url,int limit) {
        if(!BrowserUrls.web(url))return;
        JSONArray values=new JSONArray();JSONObject item=new JSONObject();
        GadgetLayout.put(item,"title",title==null||title.isEmpty()?url:title.substring(0,Math.min(200,title.length())));
        GadgetLayout.put(item,"url",url);GadgetLayout.put(item,"time",System.currentTimeMillis());values.put(item);
        JSONArray old=list(key);
        for(int i=0;i<old.length()&&values.length()<limit;i++){JSONObject previous=old.optJSONObject(i);if(previous!=null&&!url.equals(previous.optString("url")))values.put(previous);}
        list(key,values);
    }
    void remove(String key,int index){JSONArray old=list(key),result=new JSONArray();for(int i=0;i<old.length();i++)if(i!=index)result.put(old.opt(i));list(key,result);}
    String search(){return prefs.getString("search","Bing");}
}
