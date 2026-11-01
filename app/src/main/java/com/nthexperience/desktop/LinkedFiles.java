package com.nthexperience.desktop;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import org.json.*;
import java.util.*;

/** Explicit Start-search bookmarks. It never scans storage or reads document contents. */
final class LinkedFiles {
    static final int MAX=50;
    static JSONArray list(DesktopStore store){try{return new JSONArray(store.prefs.getString("linked_files","[]"));}catch(JSONException ex){return new JSONArray();}}
    static JSONObject describe(Context c,Uri uri)throws JSONException {
        String name="Linked file",mime=c.getContentResolver().getType(uri);
        try(Cursor cursor=c.getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(cursor!=null&&cursor.moveToFirst()&&!cursor.isNull(0))name=cursor.getString(0);}
        return new JSONObject().put("uri",uri.toString()).put("name",name.substring(0,Math.min(512,name.length()))).put("mime",mime==null?"application/octet-stream":mime);
    }
    static boolean add(DesktopStore store,JSONObject file){JSONArray values=list(store);for(int i=0;i<values.length();i++)if(values.optJSONObject(i)!=null&&values.optJSONObject(i).optString("uri").equals(file.optString("uri")))return true;if(values.length()>=MAX)return false;values.put(file);store.prefs.edit().putString("linked_files",values.toString()).apply();return true;}
    static void remove(DesktopStore store,String uri){JSONArray values=list(store);for(int i=values.length()-1;i>=0;i--)if(values.optJSONObject(i)!=null&&uri.equals(values.optJSONObject(i).optString("uri")))values.remove(i);store.prefs.edit().putString("linked_files",values.toString()).apply();}
    static JSONObject get(DesktopStore store,String uri){JSONArray values=list(store);for(int i=0;i<values.length();i++){JSONObject file=values.optJSONObject(i);if(file!=null&&uri.equals(file.optString("uri")))return file;}return null;}
    static void validate(JSONArray values)throws JSONException {
        if(values.length()>MAX)throw new JSONException("Too many linked files");Set<String> seen=new HashSet<>();
        for(int i=0;i<values.length();i++){JSONObject f=values.getJSONObject(i);String uri=f.getString("uri"),name=f.getString("name"),mime=f.getString("mime");if(!"content".equals(Uri.parse(uri).getScheme())||uri.length()>8192||name.isEmpty()||name.length()>512||mime.length()>256||!seen.add(uri))throw new JSONException("Invalid linked file");}
    }
}
