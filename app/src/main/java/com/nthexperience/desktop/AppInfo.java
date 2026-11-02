package com.nthexperience.desktop;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

/** Installed package information is the single source for user-facing build labels. */
final class AppInfo {
    static String version(Context context){try{String name=context.getPackageManager().getPackageInfo(context.getPackageName(),0).versionName;return name==null?"Unknown":name;}catch(PackageManager.NameNotFoundException ex){return "Unknown";}}
    static long build(Context context){try{PackageInfo info=context.getPackageManager().getPackageInfo(context.getPackageName(),0);return Build.VERSION.SDK_INT>=28?info.getLongVersionCode():info.versionCode;}catch(PackageManager.NameNotFoundException ex){return 0;}}
    private AppInfo(){}
}
