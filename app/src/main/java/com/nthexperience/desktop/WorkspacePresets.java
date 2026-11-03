package com.nthexperience.desktop;

import org.json.*;
import java.util.*;

/** Small named layouts. Documents are references; notes remain in their normal store. */
final class WorkspacePresets {
    static final int LIMIT=6;
    static JSONArray list(DesktopStore store){try{return validate(store.prefs.getString("workspace_presets","[]"));}catch(JSONException ex){return new JSONArray();}}
    static JSONArray validate(String text)throws JSONException{
        if(text.length()>220000)throw new JSONException("Workspace presets are too large");JSONArray list=new JSONArray(text);if(list.length()>LIMIT)throw new JSONException("Too many workspace presets");
        Set<String> names=new HashSet<>();for(int i=0;i<list.length();i++){JSONObject item=list.getJSONObject(i);String name=item.getString("name");if(name.trim().isEmpty()||name.length()>60||!names.add(name.toLowerCase(Locale.ROOT)))throw new JSONException("Invalid workspace name");JSONObject session=item.getJSONObject("session");JSONArray tools=session.getJSONArray("tools");if(tools.length()>32)throw new JSONException("Too many workspace windows");for(int n=0;n<tools.length();n++){JSONObject tool=tools.getJSONObject(n);if(tool.getString("id").length()>512||!tool.getString("id").startsWith("@")&&!tool.getString("id").startsWith("folder:")&&!FolderLinks.id(tool.getString("id")))throw new JSONException("Invalid workspace tool");JSONObject geometry=tool.optJSONObject("geometry");if(geometry!=null){Iterator<String> keys=geometry.keys();while(keys.hasNext()){Object value=geometry.get(keys.next());if(value instanceof Number&&!Double.isFinite(((Number)value).doubleValue()))throw new JSONException("Invalid workspace geometry");}}}}
        return list;
    }
    static void save(DesktopStore store,String name,JSONObject session,boolean sidebar,String orientation)throws JSONException{
        name=name.trim();JSONArray result=new JSONArray(),old=list(store);for(int i=0;i<old.length();i++)if(!old.getJSONObject(i).getString("name").equalsIgnoreCase(name))result.put(old.get(i));if(result.length()>=LIMIT)throw new JSONException("Delete a preset before adding another. Up to six are supported.");
        JSONObject clean=new JSONObject(session.toString());clean.remove("start");clean.remove("desktopHidden");JSONArray tools=clean.getJSONArray("tools"),kept=new JSONArray();for(int i=0;i<tools.length();i++){JSONObject tool=tools.getJSONObject(i);if(!Arrays.asList("@backup","@recycle","@workspaces","@recent").contains(tool.optString("id")))kept.put(tool);}clean.put("tools",kept);
        result.put(new JSONObject().put("name",name).put("session",clean).put("sidebar",sidebar).put("orientation",orientation));validate(result.toString());store.prefs.edit().putString("workspace_presets",result.toString()).apply();
    }
    static void remove(DesktopStore store,String name){JSONArray old=list(store),result=new JSONArray();for(int i=0;i<old.length();i++)if(!old.optJSONObject(i).optString("name").equals(name))result.put(old.opt(i));store.prefs.edit().putString("workspace_presets",result.toString()).apply();}
}
