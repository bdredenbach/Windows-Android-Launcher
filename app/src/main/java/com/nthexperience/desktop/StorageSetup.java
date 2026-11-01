package com.nthexperience.desktop;

import android.app.*;
import android.content.*;
import android.os.Build;

/** A one-time optional setup after the launcher intro; its state is device-local, not backed up. */
final class StorageSetup {
    private final MainActivity activity;private final SharedPreferences prefs;private AlertDialog dialog;
    StorageSetup(MainActivity activity){this.activity=activity;prefs=activity.getSharedPreferences(StorageAccess.PREFS,0);}
    boolean showing(){return dialog!=null&&dialog.isShowing();}
    void show(boolean force){if(showing()||activity.isFinishing()||activity.isDestroyed())return;if(!force&&(prefs.getBoolean("seen",false)||StorageAccess.has(activity))){if(StorageAccess.has(activity))prefs.edit().putBoolean("seen",true).apply();return;}
        String detail=StorageAccess.has(activity)?"Device file browsing is enabled. Computer opens Internal storage, Documents, Pictures, Music, Videos and Downloads inside Nth Explorer.":"Browse and manage your shared device files in Vista-style windows. "+(Build.VERSION.SDK_INT>=30?"Android will ask you to allow ‘All files access’.":"Android will ask for storage permission.");
        detail+="\n\nThis is optional. Files stay on your device. Android protects system files and other apps’ private data. You can connect individual folders instead, or change access later in Control Panel → File access.";
        dialog=Aero.dialog(activity).setTitle("Set up Nth Explorer").setMessage(detail).setPositiveButton(StorageAccess.has(activity)?"Review access":"Enable file browsing",(d,w)->{seen();if(StorageAccess.has(activity))try{activity.startActivity(StorageAccess.settingsIntent(activity));}catch(ActivityNotFoundException|SecurityException ex){StorageAccess.request(activity);}else StorageAccess.request(activity);}).setNeutralButton("Choose folders",(d,w)->{seen();activity.storageFolderSetup();}).setNegativeButton("Later",(d,w)->seen()).create();dialog.setOnCancelListener(d->seen());dialog.setOnDismissListener(d->dialog=null);dialog.setOnShowListener(d->{Aero.styleDialog(activity,dialog);android.view.Window window=dialog.getWindow();if(window!=null){int available=activity.getWindow().getDecorView().getWidth();if(available<=0)available=activity.getResources().getDisplayMetrics().widthPixels;window.setLayout(Math.max(1,Math.min(Aero.dp(activity,420),available-Aero.dp(activity,32))),android.view.ViewGroup.LayoutParams.WRAP_CONTENT);}});dialog.show();
    }
    private void seen(){prefs.edit().putBoolean("seen",true).apply();}
    void close(){if(dialog!=null){dialog.setOnDismissListener(null);dialog.dismiss();dialog=null;}}
}
