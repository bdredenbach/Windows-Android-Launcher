package com.nthexperience.desktop;
import java.util.*;
/** Stable family groups. Never changes the underlying windows or their order. */
final class TaskbarGrouping {
    static List<List<String>> groups(List<String> ids,Set<String> explorer,String mode,int available,int button){
        boolean combine=mode.equals("Always")||mode.equals("When full")&&ids.size()*button>Math.max(1,available);List<List<String>> result=new ArrayList<>();List<String> folders=null;
        for(String id:ids){if(combine&&explorer.contains(id)){if(folders==null){folders=new ArrayList<>();result.add(folders);}folders.add(id);}else result.add(new ArrayList<>(Collections.singletonList(id)));}return result;
    }
}
