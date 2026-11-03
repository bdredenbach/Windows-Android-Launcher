package com.nthexperience.desktop;

import android.content.Context;
import android.os.*;
import android.widget.*;
import org.json.JSONObject;
import java.util.*;
import java.util.concurrent.*;

final class RecyclePanel extends LinearLayout {
    private final ExecutorService worker=Executors.newSingleThreadExecutor();private final Handler main=new Handler(Looper.getMainLooper());
    private final Runnable changed;private final DocumentOps.Cancel cancel=new DocumentOps.Cancel();private boolean closed,busy;
    RecyclePanel(Context context,Runnable changed){super(context);this.changed=changed;setOrientation(VERTICAL);refresh();}
    private void refresh(){run(()->RecycleBin.list(getContext()),items->{removeAllViews();LinearLayout body=Aero.column(getContext());Aero.pad(body,12);body.addView(Aero.text(getContext(),"Recycle Bin",20,Aero.INK));body.addView(Aero.text(getContext(),"Restore to the original folder. Existing files are kept; restored copies receive a new name. Up to 250 MB total, 100 MB per file. Folder deletions remain permanent.",12,0xff496171));
        TextView empty=Aero.button(getContext(),"Empty Recycle Bin…",()->Aero.dialog(getContext()).setTitle("Empty Recycle Bin?").setMessage("Permanently remove all recovery copies? This cannot be undone.").setPositiveButton("Empty",(d,w)->operation(()->{RecycleBin.empty(getContext());return "Recycle Bin emptied";})).setNegativeButton("Cancel",null).show());empty.setEnabled(!items.isEmpty());body.addView(empty);
        if(items.isEmpty())body.addView(Aero.text(getContext(),"Recycle Bin is empty",15,Aero.INK));
        for(JSONObject item:items){body.addView(Aero.text(getContext(),item.optString("name"),15,Aero.INK));body.addView(Aero.text(getContext(),android.text.format.Formatter.formatFileSize(getContext(),item.optLong("size"))+" • "+java.text.DateFormat.getDateTimeInstance().format(new Date(item.optLong("date")))+(item.optBoolean("deleted")?"":" • Recovery copy; original deletion unconfirmed"),12,0xff496171));TextView restore=Aero.button(getContext(),"Restore "+item.optString("name"),()->operation(()->{RecycleBin.restore(getContext(),item,cancel);return "File restored";}));body.addView(restore);body.addView(Aero.button(getContext(),"Delete permanently…",()->Aero.dialog(getContext()).setTitle("Delete recovery copy?").setMessage(item.optString("name")).setPositiveButton("Delete",(d,w)->operation(()->{RecycleBin.remove(getContext(),item);return "Recovery copy removed";})).setNegativeButton("Cancel",null).show()));}
        ScrollView scroll=new ScrollView(getContext());scroll.addView(body);addView(scroll,new LayoutParams(-1,-1));});}
    interface Job<T>{T run()throws Exception;}
    private <T> void run(Job<T> job,java.util.function.Consumer<T> done){if(busy||closed)return;busy=true;worker.execute(()->{try{T result=job.run();main.post(()->{busy=false;if(!closed)done.accept(result);});}catch(Exception ex){main.post(()->{busy=false;if(!closed){Toast.makeText(getContext(),ex.getMessage()==null?"Operation unavailable":ex.getMessage(),Toast.LENGTH_LONG).show();DesktopSounds.play(getContext(),"error");}});}});}
    private void operation(Job<String> job){run(job,text->{Toast.makeText(getContext(),text,Toast.LENGTH_LONG).show();DesktopSounds.play(getContext(),"complete");changed.run();refresh();});}
    void close(){closed=true;cancel.cancel();worker.shutdown();main.removeCallbacksAndMessages(null);}
}
