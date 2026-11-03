package com.nthexperience.desktop;

import android.net.Uri;
import android.provider.DocumentsContract;
import org.json.*;
import java.util.*;

/** Desktop shortcuts to permission-scoped real folders; never grants storage access. */
final class FolderLinks {
    static boolean id(String id){if(id==null||!id.startsWith("location:"))return false;try{UUID.fromString(id.substring(9));return true;}catch(IllegalArgumentException ex){return false;}}
    static JSONObject list(DesktopStore store){try{return new JSONObject(store.prefs.getString("folder_links","{}"));}catch(JSONException ex){return new JSONObject();}}
    static JSONObject get(DesktopStore store,String id){return list(store).optJSONObject(id);}
    static String add(DesktopStore store,String page,String title)throws JSONException{validatePage(page);JSONObject links=list(store);Iterator<String> keys=links.keys();while(keys.hasNext()){String key=keys.next();if(page.equals(links.getJSONObject(key).optString("page"))){List<String> desktop=store.desktop();if(!desktop.contains(key)){desktop.add(key);store.desktop(desktop);}return key;}}if(links.length()>=64)throw new JSONException("Up to 64 folder links are supported.");String id="location:"+UUID.randomUUID();links.put(id,new JSONObject().put("page",page).put("title",title.substring(0,Math.min(240,title.length()))));store.prefs.edit().putString("folder_links",links.toString()).apply();List<String> desktop=store.desktop();desktop.add(id);store.desktop(desktop);return id;}
    static void validate(String text)throws JSONException{if(text.length()>512000)throw new JSONException("Too many folder links");JSONObject links=new JSONObject(text);if(links.length()>64)throw new JSONException("Too many folder links");Iterator<String> keys=links.keys();while(keys.hasNext()){String key=keys.next();if(!id(key))throw new JSONException("Invalid folder link ID");JSONObject link=links.getJSONObject(key);if(link.getString("title").length()>240)throw new JSONException("Invalid folder title");validatePage(link.getString("page"));}}
    static void validatePage(String page)throws JSONException{if(!page.startsWith("folder:")||page.length()>24000)throw new JSONException("Invalid folder path");JSONObject path=DocumentFolders.path(page);Uri tree=Uri.parse(path.getString("tree"));if(!"content".equals(tree.getScheme())||!DocumentsContract.isTreeUri(tree))throw new JSONException("Invalid folder access reference");JSONArray crumbs=path.getJSONArray("crumbs");if(crumbs.length()<1||crumbs.length()>64)throw new JSONException("Invalid folder path depth");for(int i=0;i<crumbs.length();i++){JSONObject crumb=crumbs.getJSONObject(i);if(crumb.getString("id").length()>8192||crumb.getString("name").length()>512)throw new JSONException("Invalid folder path segment");}}
}
