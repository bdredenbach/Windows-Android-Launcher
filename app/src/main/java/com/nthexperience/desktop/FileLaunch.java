package com.nthexperience.desktop;

import android.app.Activity;
import android.content.*;
import android.net.Uri;
import android.provider.Settings;
import android.webkit.MimeTypeMap;
import java.util.Locale;

/** One read-only file handoff for Explorer, bookmarks and Open with. */
final class FileLaunch {
    static final String APK="application/vnd.android.package-archive";
    static String mime(String name,String provided){
        String type=provided==null?"":provided.trim().toLowerCase(Locale.ROOT);
        String lower=name==null?"":name.toLowerCase(Locale.ROOT);
        if(lower.endsWith(".apk"))return APK;
        if(!type.isEmpty()&&!type.equals("*/*")&&!type.equals("application/octet-stream")&&!type.equals("binary/octet-stream"))return type;
        int dot=lower.lastIndexOf('.');String guessed=dot<0?null:MimeTypeMap.getSingleton().getMimeTypeFromExtension(lower.substring(dot+1));
        return guessed==null?"application/octet-stream":guessed;
    }
    static Intent intent(Uri uri,String name,String provided){
        Intent intent=new Intent(Intent.ACTION_VIEW).setDataAndType(uri,mime(name,provided)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setClipData(ClipData.newRawUri(name==null||name.isEmpty()?"Document":name,uri));return intent;
    }
    static void open(Activity activity,Uri uri,String name,String provided,boolean chooser){
        String type=mime(name,provided);
        if(!chooser&&activity instanceof MainActivity&&type.startsWith("image/")){((MainActivity)activity).openPhoto(uri,name,type,null,"@files");return;}
        MediaLibrary.Entry media=chooser?null:MediaLibrary.entry(uri,name,provided);
        if(media!=null&&activity instanceof MainActivity){((MainActivity)activity).openMedia(media);return;}
        try{Intent intent=intent(uri,name,provided);activity.startActivity(chooser?Intent.createChooser(intent,"Open with"):intent);}
        catch(ActivityNotFoundException ex){Aero.dialog(activity).setTitle("Cannot open file").setMessage(type.equals(APK)?"No Android package installer is available on this device.":"No installed app can open this file type ("+type+"). Install an app that supports it, then try again.").setPositiveButton("OK",null).show();}
        catch(SecurityException ex){android.app.AlertDialog.Builder dialog=Aero.dialog(activity).setTitle("Cannot open file").setMessage(type.equals(APK)?"Android blocked this installation request. Review whether Nth Desktop is allowed to install unknown apps, then tap the APK again.":"Android could not share read access to this file. Reconnect its folder or allow file browsing, then try again.").setNegativeButton("Close",null);if(type.equals(APK))dialog.setPositiveButton("Installation settings",(d,w)->{try{activity.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+activity.getPackageName())));}catch(ActivityNotFoundException|SecurityException ignored){}});dialog.show();}
    }
    private FileLaunch(){}
}
