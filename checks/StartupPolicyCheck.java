import com.nthexperience.desktop.StartupPolicy;
public class StartupPolicyCheck {
    public static void main(String[] args){
        check(StartupPolicy.shouldPlay(0,false,-1,4),"First launch plays once");
        check(!StartupPolicy.shouldPlay(0,true,3,4),"First-only mode does not replay on reboot");
        check(StartupPolicy.shouldPlay(1,false,-1,4),"Reboot mode includes first launch");
        check(!StartupPolicy.shouldPlay(1,true,4,4),"Same boot suppresses Home, rotation and process restarts");
        check(StartupPolicy.shouldPlay(1,true,4,5),"A new boot is eligible");
        check(!StartupPolicy.shouldPlay(1,true,4,-1),"An unavailable boot counter cannot trigger repeat playback");
        check(!StartupPolicy.shouldPlay(2,false,-1,5),"Off never plays automatically");
        check(!StartupPolicy.shouldPlay(99,false,-1,5),"Unknown modes fail closed");
        System.out.println("8 startup policy checks passed.");
    }
    private static void check(boolean good,String message){if(!good)throw new AssertionError(message);}
}
