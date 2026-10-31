package com.nthexperience.desktop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Ordering rules shared by the desktop and recent-launch strip. No Android dependencies. */
public final class LauncherState {
    public static final int MAX_RECENTS = 4;
    public static List<String> normalize(List<String> values) {
        ArrayList<String> result = new ArrayList<>();
        for (String value : values) {
            if (value != null && !value.trim().isEmpty() && !result.contains(value)) result.add(value);
        }
        return result;
    }
    public static List<String> move(List<String> values, String moving, String target) {
        ArrayList<String> result = new ArrayList<>(normalize(values));
        int from = result.indexOf(moving), to = result.indexOf(target);
        if (from < 0 || to < 0 || from == to) return result;
        // Swap occupied slots, preserving all other desktop positions.
        Collections.swap(result, from, to);
        return result;
    }
    public static List<String> recent(List<String> values, String launched) {
        ArrayList<String> result = new ArrayList<>(normalize(values));
        result.remove(launched);
        result.add(0, launched);
        return new ArrayList<>(result.subList(0, Math.min(MAX_RECENTS, result.size())));
    }
    private LauncherState() {}
}
