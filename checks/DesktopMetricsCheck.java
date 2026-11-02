package com.nthexperience.desktop;
public final class DesktopMetricsCheck {
    private static void check(boolean ok){if(!ok)throw new AssertionError("Desktop grid geometry");}
    public static void main(String[] args){
        check(DesktopMetrics.cell(360,6,80,48)==60);
        check(DesktopMetrics.cell(244,10,80,48)==48);
        check(DesktopMetrics.cell(720,5,94,72)==144);
        check(DesktopMetrics.cell(360,0,80,48)==80);
        check(DesktopMetrics.cell(35,8,80,48)==35);
        check(DesktopMetrics.cell(0,0,80,48)==1);
        check(DesktopMetrics.icon(69,48,72,8,9,28)==35);
        check(DesktopMetrics.icon(46,80,94,8,9,28)==46);
        check(DesktopMetrics.icon(69,80,72,8,9,0)==63);
        System.out.println("PASS: desktop grid count, narrow-window bounds, row fitting and icon/label clearance");
    }
}
