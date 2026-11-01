package com.nthexperience.desktop;

/** Playback identity is separate from desktop backups and from Activity/process identity. */
public final class StartupPolicy {
    public static boolean shouldPlay(int mode,boolean seen,int lastBoot,int currentBoot){return mode==0?!seen:mode==1&&(!seen||(currentBoot>=0&&currentBoot!=lastBoot));}
    private StartupPolicy(){}
}
