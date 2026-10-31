package com.nthexperience.desktop;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class DesktopStore {
    final SharedPreferences prefs;
    DesktopStore(Context context) { prefs = context.getSharedPreferences("desktop", Context.MODE_PRIVATE); }
    List<String> desktop() {
        return list("shortcuts", Arrays.asList("@computer", "@browser", "@files", "@notes", "@appearance"));
    }
    List<String> recents() { return list("recents", new ArrayList<>()); }
    private List<String> list(String key, List<String> fallback) {
        try {
            String saved = prefs.getString(key, null);
            if (saved == null) return new ArrayList<>(fallback);
            JSONArray array = new JSONArray(saved);
            List<String> result = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) result.add(array.getString(i));
            return LauncherState.normalize(result);
        } catch (JSONException | ClassCastException ex) { return new ArrayList<>(fallback); }
    }
    void desktop(List<String> ids) { save("shortcuts", ids); }
    void launched(String id) { save("recents", LauncherState.recent(recents(), id)); }
    private void save(String key, List<String> ids) {
        prefs.edit().putString(key, new JSONArray(LauncherState.normalize(ids)).toString()).apply();
    }
    boolean pinned(String id) { return desktop().contains(id); }
    void toggle(String id) {
        List<String> values = desktop();
        if (values.contains(id)) values.remove(id); else values.add(id);
        desktop(values);
    }
}
