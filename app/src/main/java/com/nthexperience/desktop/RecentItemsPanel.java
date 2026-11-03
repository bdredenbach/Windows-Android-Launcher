package com.nthexperience.desktop;

import android.content.Context;
import android.widget.*;
import org.json.*;
import java.util.Date;
import java.util.function.Consumer;

final class RecentItemsPanel extends LinearLayout {
    private final DesktopStore store;private final Consumer<String> open;
    RecentItemsPanel(Context context,DesktopStore store,Consumer<String> open){super(context);this.store=store;this.open=open;setOrientation(VERTICAL);refresh();}
    void refresh(){removeAllViews();LinearLayout body=Aero.column(getContext());Aero.pad(body,12);body.addView(Aero.text(getContext(),"Recent Items",20,Aero.INK));body.addView(Aero.text(getContext(),"Documents, pictures and folders opened in Nth. Access still follows Android permissions. Browser history remains in Internet; Favorites are searchable from Start.",12,0xff496171));
        CheckBox enabled=Aero.check(getContext(),"Remember recent documents and folders",store.prefs.getBoolean("recent_items_enabled",true));enabled.setOnCheckedChangeListener((button,value)->store.prefs.edit().putBoolean("recent_items_enabled",value).apply());body.addView(enabled);
        body.addView(Aero.button(getContext(),"Clear Recent Items",()->{store.prefs.edit().remove("recent_files").apply();refresh();}));
        JSONArray items=RecentFiles.list(store);if(items.length()==0)body.addView(Aero.text(getContext(),"No recent items",14,Aero.INK));for(int i=0;i<items.length();i++){JSONObject item=items.optJSONObject(i);if(item==null)continue;String uri=item.optString("uri"),name=item.optString("name");TextView row=Aero.button(getContext(),name,()->open.accept("recent-file:"+uri));row.setGravity(android.view.Gravity.CENTER_VERTICAL|android.view.Gravity.START);row.setSingleLine(false);body.addView(row);body.addView(Aero.text(getContext(),(item.optString("kind").equals("folder")?"Folder":"Document")+" • "+java.text.DateFormat.getDateTimeInstance().format(new Date(item.optLong("opened"))),11,0xff496171));}
        ScrollView scroll=new ScrollView(getContext());scroll.addView(body);addView(scroll,new LayoutParams(-1,-1));
    }
}
