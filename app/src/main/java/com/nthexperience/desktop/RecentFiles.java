package com.nthexperience.desktop;

import android.net.Uri;
import org.json.*;

/** Bounded on-device document history; these entries never grant additional access. */
final class RecentFiles {
    static JSONArray list(DesktopStore store){try{return new JSONArray(store.prefs.getString("recent_files","[]"));}catch(JSONException ex){return new JSONArray();}}
    static JSONObject get(DesktopStore store,String uri){JSONArray items=list(store);for(int i=0;i<items.length();i++){JSONObject item=items.optJSONObject(i);if(item!=null&&uri.equals(item.optString("uri")))return item;}return null;}
    static void opened(DesktopStore store,Uri uri,String mime,String name){if(!store.prefs.getBoolean("recent_items_enabled",true)||uri==null||!"content".equals(uri.getScheme()))return;JSONArray items=new JSONArray();JSONObject item=new JSONObject();GadgetLayout.put(item,"uri",uri.toString());GadgetLayout.put(item,"mime",FileLaunch.mime(name,mime));GadgetLayout.put(item,"name",name==null||name.isEmpty()?"Document":name);GadgetLayout.put(item,"opened",System.currentTimeMillis());items.put(item);JSONArray old=list(store);for(int i=0;i<old.length()&&items.length()<20;i++){JSONObject previous=old.optJSONObject(i);if(previous!=null&&!uri.toString().equals(previous.optString("uri")))items.put(previous);}store.prefs.edit().putString("recent_files",items.toString()).apply();}
    static void folder(DesktopStore store,String page,String name){
        if(!store.prefs.getBoolean("recent_items_enabled",true)||!page.startsWith("folder:")||page.length()>24000)return;
        JSONArray next=new JSONArray(),old=list(store);JSONObject item=new JSONObject();GadgetLayout.put(item,"uri",page);GadgetLayout.put(item,"page",page);GadgetLayout.put(item,"name",name);GadgetLayout.put(item,"kind","folder");GadgetLayout.put(item,"opened",System.currentTimeMillis());next.put(item);
        for(int i=0;i<old.length()&&next.length()<20;i++){JSONObject previous=old.optJSONObject(i);if(previous!=null&&!page.equals(previous.optString("uri")))next.put(previous);}store.prefs.edit().putString("recent_files",next.toString()).apply();
    }

}
