package com.nthexperience.desktop;

import java.util.*;

/** The gallery, faces, window captions and backup validation share one catalog. */
final class GadgetCatalog {
    static final String[] IDS={"clock","calendar","weather","notes","photo","meters","worldclock","feeds","checklist","timer","stopwatch","calculator","converter","storage"};
    static final String[][] ITEMS={
        {"clock","Clock","Time","An analog clock with ivory and black glass faces."},
        {"calendar","Calendar","Time","Today’s date and a browsable month calendar."},
        {"weather","Weather","Information","Cached local forecasts with optional Follow my location."},
        {"notes","Notes","Productivity","A sticky note that saves as you type."},
        {"photo","Photo Frame","Photos","Your pictures in a rotating photo frame."},
        {"meters","System Meter","Device","Real battery and physical memory readings."},
        {"worldclock","World Clock","Time","Two independently chosen time zones with local dates."},
        {"feeds","Feed Headlines","Information","Headlines from an RSS or Atom feed you choose."},
        {"checklist","Checklist","Productivity","A saved to-do checklist of tasks, with checkboxes and progress."},
        {"timer","Countdown","Time","A persistent countdown timer with pause, resume and reset."},
        {"stopwatch","Stopwatch","Time","Elapsed time, pause and saved laps."},
        {"calculator","Calculator","Productivity","A working calculator with parentheses and arithmetic."},
        {"converter","Unit Converter","Productivity","Length, mass, temperature, speed, area, volume, data and time."},
        {"storage","Storage Meter","Device","Real internal storage and disk usage, available space and system settings."}};
    static boolean valid(String id){return Arrays.asList(IDS).contains(id);}
    static boolean extra(String id){return valid(id)&&Arrays.asList(IDS).indexOf(id)>=6;}
    static String title(String id){for(String[] row:ITEMS)if(row[0].equals(id))return row[1];return "Gadget";}
    static String description(String id){for(String[] row:ITEMS)if(row[0].equals(id))return row[3];return "";}
    static String icon(String id){return id.equals("photo")?"pictures":id.equals("meters")?"power":id.equals("storage")?"drive":id;}
    static String toolIcon(String tool){switch(tool){case "@sticky":return "sticky";case "@calendar":return "calendar";case "@weather":return "weather";case "@gadgets":return "gadgets";case "@clock":return "clock";default:return tool.startsWith("@gadget:")?icon(tool.substring(8)):"programs";}}
}
