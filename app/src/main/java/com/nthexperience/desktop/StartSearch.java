package com.nthexperience.desktop;

import android.content.ComponentName;
import android.graphics.drawable.Drawable;
import org.json.*;
import java.util.*;

final class StartSearch {
    static final class Result {
        final String id,title,group,kind;final Drawable icon;final boolean shortcut;
        Result(String id,String title,String group,String kind,Drawable icon,boolean shortcut){this.id=id;this.title=title;this.group=group;this.kind=kind;this.icon=icon;this.shortcut=shortcut;}
    }
    static List<Result> find(DesktopStore store,List<AppCatalog.Entry> apps,String query){
        String q=query.trim().toLowerCase(Locale.ROOT);List<Result> result=new ArrayList<>();
        for(AppCatalog.Entry app:apps){String title=store.name(app.id,app.label);if((title+" "+app.label).toLowerCase(Locale.ROOT).contains(q))result.add(new Result(app.id,title,"Programs","programs",app.icon,true));}
        if(q.isEmpty())return result;
        VistaControlPanel.searchItems(q,result);
        for(String[] note:new String[][]{{"note","Notepad","@notes"},{"gadget_note","Sticky note","@sticky"}}){String text=store.prefs.getString(note[0],"");if((note[1]+" "+text).toLowerCase(Locale.ROOT).contains(q))result.add(new Result(note[2],note[1],"Notes","notes",null,false));}
        JSONArray files=LinkedFiles.list(store);for(int i=0;i<files.length();i++){JSONObject file=files.optJSONObject(i);if(file!=null&&file.optString("name").toLowerCase(Locale.ROOT).contains(q))result.add(new Result("search-file:"+file.optString("uri"),file.optString("name"),"Linked files","documentfile",null,false));}
        return result;
    }
}
