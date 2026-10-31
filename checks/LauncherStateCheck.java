import com.nthexperience.desktop.LauncherState;
import java.util.List;

public class LauncherStateCheck {
    public static void main(String[] args) {
        check(LauncherState.move(List.of("a","b","c"),"a","c").equals(List.of("c","b","a")),"Dragging onto a shortcut swaps both occupied slots");
        check(LauncherState.move(List.of("a","b"),"removed","b").equals(List.of("a","b")),"Stale drag does not remove shortcuts");
        check(LauncherState.recent(List.of("a","b","c","d"),"c").equals(List.of("c","a","b","d")),"Relaunch promotes rather than duplicates an app");
        check(LauncherState.recent(List.of("a","b","c","d"),"e").equals(List.of("e","a","b","c")),"Recent launches stay bounded");
        check(LauncherState.normalize(java.util.Arrays.asList("a",null," ","a","b")).equals(List.of("a","b")),"Damaged saved shortcuts recover without blank or duplicate entries");
        System.out.println("5 launcher behavior checks passed.");
    }
    static void check(boolean value,String description){if(!value)throw new AssertionError(description);}
}
