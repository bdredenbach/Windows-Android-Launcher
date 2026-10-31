import com.nthexperience.desktop.DesktopGeometry;
import java.util.Arrays;

public class DesktopGeometryCheck {
    public static void main(String[] args){
        check(Arrays.equals(DesktopGeometry.gadget(400,700,132,128,24,1,0,1),new int[]{0,572,156,128}),"Edge placement remains completely visible");
        int[] wide=DesktopGeometry.gadget(700,300,132,128,24,1.8f,1,1);
        check(wide[0]+wide[2]==700&&wide[1]+wide[3]==300,"Landscape uses the full available area");
        int[] tiny=DesktopGeometry.gadget(80,50,132,142,24,1.8f,2,-1);
        check(tiny[0]>=0&&tiny[1]>=0&&tiny[0]+tiny[2]<=80&&tiny[1]+tiny[3]<=50,"Temporary small viewport cannot strand a gadget");
        check(DesktopGeometry.fraction(25,100)==.25f&&DesktopGeometry.fraction(10,0)==0,"Position normalization handles zero space");
        check(DesktopGeometry.fraction(-20,100)==0&&DesktopGeometry.fraction(140,100)==1,"Positions clamp on both edges");
        check(DesktopGeometry.snap(21,8)==24&&DesktopGeometry.snap(21,0)==21,"Optional grid snapping");
        check(DesktopGeometry.clamp(Float.NaN,0,1)==0&&DesktopGeometry.clamp(Float.POSITIVE_INFINITY,0,1)==0,"Corrupt geometry recovers");
        System.out.println("7 desktop geometry checks passed.");
    }
    static void check(boolean value,String description){if(!value)throw new AssertionError(description);}
}
