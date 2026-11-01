package com.nthexperience.desktop;

import java.util.*;

/** Launcher-local clipboard. It never leaks file grants into Android's public clipboard. */
final class ExplorerClipboard {
    static volatile List<DocumentOps.Ref> files=Collections.emptyList();static volatile List<String> shortcuts=Collections.emptyList();static volatile String shortcutSource="";static volatile boolean cut;
    static synchronized void files(List<DocumentOps.Ref> values,boolean moving){files=new ArrayList<>(values);shortcuts=Collections.emptyList();shortcutSource="";cut=moving;}
    static synchronized void shortcuts(List<String> values,String source,boolean moving){shortcuts=new ArrayList<>(values);shortcutSource=source;files=Collections.emptyList();cut=moving;}
    static synchronized void clear(){files=Collections.emptyList();shortcuts=Collections.emptyList();shortcutSource="";cut=false;}
    static synchronized void completedCut(List<DocumentOps.Ref> original){if(files==original)clear();}
    static List<String> members(DesktopStore store,String target){return target.equals("desktop")?store.desktop():target.equals("quick")?store.quickLaunch():target.equals("managers")?store.fileManagers():target.startsWith("contents:")?store.folderItems(target.substring(9)):new ArrayList<>();}
    static void members(DesktopStore store,String target,List<String> ids){if(target.equals("desktop"))store.desktop(ids);else if(target.equals("quick"))store.quickLaunch(ids);else if(target.equals("managers"))store.fileManagers(ids);else if(target.startsWith("contents:"))store.folderItems(target.substring(9),ids);}
    static void paste(DesktopStore store,String target){
        if(shortcuts.isEmpty())throw new IllegalArgumentException("Copy or cut a shortcut first.");if(target.startsWith("contents:")&&!store.isFolder(target.substring(9)))throw new IllegalArgumentException("This app folder was removed.");
        List<String> destination=members(store,target),source=members(store,shortcutSource);int max=target.equals("quick")?DesktopStore.MAX_QUICK:target.startsWith("contents:")?DesktopStore.MAX_ITEMS:512;
        for(String id:shortcuts){if(target.startsWith("contents:")&&id.startsWith("folder:"))throw new IllegalArgumentException("App folders cannot contain other app folders.");if(cut&&!source.contains(id))throw new IllegalArgumentException("A cut shortcut is no longer in its original folder.");if(!destination.contains(id))destination.add(id);}if(destination.size()>max)throw new IllegalArgumentException("This destination has no room for these shortcuts.");
        members(store,target,destination);if(cut&&!shortcutSource.equals(target)){source.removeAll(shortcuts);members(store,shortcutSource,source);}if(cut)clear();
    }
}
