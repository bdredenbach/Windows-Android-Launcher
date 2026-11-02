package com.nthexperience.desktop;

import android.app.Activity;
import android.content.*;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.*;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.text.Collator;
import java.util.*;
import java.util.concurrent.*;

/** Vista-inspired still-picture viewer. Sources are read-only; collection access comes from Explorer. */
final class PhotoGallery extends LinearLayout {
    interface Host {void openPicture();void pictures();void back(JSONObject source,String window);void caption(String title);}
    private static final class Entry {final Uri uri;final String name,mime;final long size;Entry(Uri uri,String name,String mime,long size){this.uri=uri;this.name=name;this.mime=mime;this.size=size;}}
    private final Activity activity;private final Host host;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private final FrameLayout stage;final PhotoCanvas canvas;
    private final TextView status,folderButton;
    private final PhotoGlyph previous,next,play;
    private final ArrayList<Entry> entries=new ArrayList<>();
    private Uri current;private String name="",mime="image/*",sourceWindow="@files";
    private long size=-1;private int width,height,generation,index;
    private boolean closed,loading,playing,foreground=true,paused,limited;
    private JSONObject source=new JSONObject();private CancellationSignal cancellation;
    private DesktopContextMenu menu;
    private final Runnable advance=new Runnable(){public void run(){if(!playing||closed||!foreground||!isShown())return;if(!loading){if(entries.size()<2){stop();return;}step(index+1>=entries.size()?-index:1);}main.postDelayed(this,4000);}};
    PhotoGallery(Activity activity,Host host){super(activity);this.activity=activity;this.host=host;setOrientation(VERTICAL);setFocusableInTouchMode(true);
        LinearLayout command=new LinearLayout(activity);command.setGravity(Gravity.CENTER_VERTICAL);command.setPadding(dp(5),0,dp(5),0);command.setBackground(Aero.gloss(activity,new int[]{0xfff5faff,0xffe1ecf5,0xffcbdce9,0xffe9f1f8},0,0xffa4b7c7));
        TextView file=command("File ▾","Photo File menu",v->fileMenu(v));command.addView(file);TextView view=command("View ▾","Photo View menu",v->viewMenu(v));command.addView(view);folderButton=command("Back to folder","Back to photo folder",v->back());command.addView(folderButton,new LayoutParams(0,dp(38),1));addView(command,new LayoutParams(-1,dp(38)));
        stage=new FrameLayout(activity);canvas=new PhotoCanvas(activity);canvas.changed(this::update);stage.addView(canvas,new FrameLayout.LayoutParams(-1,-1));addView(stage,new LayoutParams(-1,0,1));
        LinearLayout controls=new LinearLayout(activity);controls.setGravity(Gravity.CENTER);controls.setBackground(Aero.gloss(activity,new int[]{0xfffbfdff,0xffe0e8ef,0xffc7d6e2,0xffeff5fa},0,0xffa2b5c6));
        previous=control(controls,"previous","Previous picture",()->step(-1),42);play=control(controls,"play","Play slideshow",this::toggleSlideshow,48);next=control(controls,"next","Next picture",()->step(1),42);
        control(controls,"rotateLeft","Rotate picture left (view only)",()->{stop();canvas.rotate(-1);},42);control(controls,"rotateRight","Rotate picture right (view only)",()->{stop();canvas.rotate(1);},42);
        control(controls,"zoomOut","Zoom picture out",()->{stop();canvas.zoomBy(.8f);},42);control(controls,"fit","Fit picture to window",()->{stop();canvas.fit();},42);control(controls,"zoomIn","Zoom picture in",()->{stop();canvas.zoomBy(1.25f);},42);
        HorizontalScrollView scroller=new HorizontalScrollView(activity);scroller.setHorizontalScrollBarEnabled(false);scroller.setFillViewport(true);scroller.addView(controls,new HorizontalScrollView.LayoutParams(-1,-1));addView(scroller,new LayoutParams(-1,dp(46)));
        status=Aero.text(activity,"Photo Gallery",11,0xff3d5366);status.setSingleLine();status.setEllipsize(android.text.TextUtils.TruncateAt.END);status.setPadding(dp(8),0,dp(6),0);status.setBackground(Aero.panel(activity,0xfff8fbfe,0xffe1eaf2,0,0xffb8c8d4));addView(status,new LayoutParams(-1,dp(24)));empty();update();
    }
    private int dp(int n){return Aero.dp(activity,n);}
    private TextView command(String title,String description,java.util.function.Consumer<View> click){TextView button=Aero.text(activity,title,13,Aero.INK);button.setBackground(Aero.selection(activity));button.setGravity(Gravity.CENTER);button.setPadding(dp(10),0,dp(10),0);button.setContentDescription(description);button.setFocusable(true);button.setOnClickListener(click::accept);button.setLayoutParams(new LayoutParams(-2,dp(38)));return button;}
    private PhotoGlyph control(LinearLayout row,String kind,String label,Runnable action,int width){PhotoGlyph button=new PhotoGlyph(activity,kind,label,action);row.addView(button,new LayoutParams(dp(width),dp(44)));return button;}
    boolean ready(){return !loading&&canvas.ready();}
    int count(){return entries.size();}int position(){return index;}boolean playing(){return playing;}Uri uri(){return current;}
    private void empty(){overlay("Windows Photo Gallery","Open a picture from Explorer, or choose one here.",true);}
    void open(Uri uri,String display,String type,JSONObject context,String originWindow){open(uri,display,type,context,originWindow,null);}
    private void open(Uri uri,String display,String type,JSONObject context,String originWindow,JSONObject transform){
        if(closed||uri==null)return;if(!"content".equals(uri.getScheme())){overlay("Cannot open picture","Choose a readable picture in Explorer.",true);return;}
        stop();current=uri;name=display==null||display.isEmpty()?"Picture":display;mime=FileLaunch.mime(name,type);source=context==null?new JSONObject():context;sourceWindow=originWindow==null?"@files":originWindow;entries.clear();entries.add(new Entry(uri,name,mime,-1));index=0;limited=false;load(transform,true);
    }
    private void load(JSONObject transform,boolean folder){
        if(current==null||closed)return;if(cancellation!=null)cancellation.cancel();cancellation=new CancellationSignal();CancellationSignal signal=cancellation;int request=++generation;Uri target=current;JSONObject context=source;String oldName=name;
        loading=true;width=height=0;size=-1;canvas.image(null);overlay("Opening picture…","",false);host.caption(name+" — Photo Gallery");update();
        worker.execute(()->{
            PhotoImage image=null;String error=null,display=oldName;long bytes=-1;
            try{try(android.database.Cursor cursor=activity.getContentResolver().query(target,new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE},null,null,null,signal)){if(cursor!=null&&cursor.moveToFirst()){String value=cursor.getString(0);if(value!=null&&!value.isEmpty())display=value;bytes=cursor.isNull(1)?-1:cursor.getLong(1);}}catch(IllegalArgumentException ignored){}image=PhotoImage.read(activity,target,signal);}
            catch(Exception|OutOfMemoryError failure){error=failure instanceof SecurityException?"Access to this picture was removed. Reconnect its folder in Explorer, then open the picture again.":"The picture may be damaged, unsupported, moved, or no longer readable. Try Open with another app.";}
            PhotoImage result=image;String message=error,foundName=display;long foundSize=bytes;
            main.post(()->{if(closed||request!=generation||signal.isCanceled()){if(result!=null)result.bitmap.recycle();return;}loading=false;name=foundName;size=foundSize;if(result==null){stop();canvas.image(null);overlay("Unable to display this picture",message,false);}else{width=result.width;height=result.height;stage.removeAllViews();stage.addView(canvas,new FrameLayout.LayoutParams(-1,-1));canvas.image(result.bitmap);canvas.setContentDescription("Photo: "+name);canvas.restore(transform);}host.caption(name+" — Photo Gallery");update();});
            if(folder&&!signal.isCanceled()&&!closed&&context.optString("page").startsWith("folder:"))readFolder(context,request,signal);
        });
    }
    private void readFolder(JSONObject context,int request,CancellationSignal signal){
        try{String page=context.getString("page");DocumentFolders.Listing listing=DocumentFolders.read(activity,page,signal);ArrayList<DocumentFolders.Item> pictures=new ArrayList<>();String query=context.optString("query").toLowerCase(Locale.ROOT);for(DocumentFolders.Item item:listing.items)if(!item.directory()&&(item.flags&DocumentsContract.Document.FLAG_VIRTUAL_DOCUMENT)==0&&PhotoImage.picture(item.name,item.mime)&&item.name.toLowerCase(Locale.ROOT).contains(query))pictures.add(item);
            Collator collator=Collator.getInstance();String sort=context.optString("sort","Name");boolean descending=context.optBoolean("descending");pictures.sort((a,b)->{int n=sort.equals("Size")?Long.compare(a.size,b.size):sort.equals("Date modified")?Long.compare(a.modified,b.modified):sort.equals("Type")?collator.compare(a.mime,b.mime):collator.compare(a.name,b.name);if(n==0)n=collator.compare(a.name,b.name);return descending?-n:n;});
            ArrayList<Entry> found=new ArrayList<>();for(DocumentFolders.Item item:pictures)found.add(new Entry(DocumentFolders.documentUri(page,item.id),item.name,FileLaunch.mime(item.name,item.mime),item.size));main.post(()->{if(closed||request!=generation||signal.isCanceled())return;int at=-1;for(int i=0;i<found.size();i++)if(found.get(i).uri.equals(current)){at=i;break;}if(at>=0){entries.clear();entries.addAll(found);index=at;limited=listing.limited;update();}});
        }catch(Exception ignored){/* A single-file grant still opens the selected picture without enumerating its neighbors. */}
    }
    void step(int amount){int at=index+amount;if(closed||at<0||at>=entries.size()||at==index)return;index=at;Entry entry=entries.get(index);current=entry.uri;name=entry.name;mime=entry.mime;load(null,false);}
    private void toggleSlideshow(){if(playing){stop();return;}if(entries.size()<2||!ready())return;playing=true;update();main.removeCallbacks(advance);main.postDelayed(advance,4000);}
    private void stop(){playing=false;main.removeCallbacks(advance);if(play!=null){play.kind("play");play.setContentDescription("Play slideshow");}if(status!=null)update();}
    private void update(){if(status==null)return;previous.setEnabled(index>0);next.setEnabled(index+1<entries.size());play.setEnabled(entries.size()>1&&ready());play.kind(playing?"pause":"play");play.setContentDescription(playing?"Pause slideshow":"Play slideshow");folderButton.setEnabled(!source.optString("page").isEmpty());String zoom=canvas.zoom()<=1.01f?"Fit":String.format(Locale.getDefault(),"%.2g×",canvas.zoom());status.setText(current==null?"Photo Gallery":(loading?"Opening…":(index+1)+" of "+entries.size()+(limited?" (folder listing limited)":"")+"  |  "+(width>0?width+" × "+height+"  |  ":"")+zoom+(playing?"  |  Slideshow":"")));}
    private void overlay(String title,String message,boolean choose){stage.removeAllViews();stage.addView(canvas,new FrameLayout.LayoutParams(-1,-1));LinearLayout column=Aero.column(activity);column.setGravity(Gravity.CENTER);column.setPadding(dp(20),dp(12),dp(20),dp(12));TextView heading=Aero.text(activity,title,19,0xff315b79);heading.setGravity(Gravity.CENTER);column.addView(heading);TextView detail=Aero.text(activity,message,14,0xff5f7382);detail.setGravity(Gravity.CENTER);detail.setPadding(0,dp(12),0,dp(12));column.addView(detail);if(choose){column.addView(Aero.button(activity,"Open a picture…",host::openPicture));column.addView(Aero.button(activity,"Browse Pictures",host::pictures));}ScrollView scroll=new ScrollView(activity);scroll.setFillViewport(true);scroll.addView(column);stage.addView(scroll,new FrameLayout.LayoutParams(-1,-1));}
    boolean back(){if(playing){stop();return true;}host.back(source,sourceWindow);return true;}
    private DesktopContextMenu menu(String title){dismissMenu();menu=new DesktopContextMenu(activity,title);return menu;}
    private void show(DesktopContextMenu menu,View anchor){ContextMenus.centered(anchor,(x,y)->ContextMenus.show(menu,anchor,x,y));}
    private void fileMenu(View anchor){DesktopContextMenu menu=menu("Photo File menu");menu.item("Open a picture…","pictures",host::openPicture);menu.item("Back to folder","files",!source.optString("page").isEmpty(),this::back);menu.separator();menu.item("Open with…","programs",current!=null,()->FileLaunch.open(activity,current,name,mime,true));menu.item("Share picture…","share",current!=null,this::share);menu.item("Picture properties","information",current!=null,this::properties);show(menu,anchor);}
    private void viewMenu(View anchor){DesktopContextMenu menu=menu("Photo View menu");menu.item("Fit to window","pictures",ready(),canvas::fit);menu.item("Zoom in","pictures",ready(),()->canvas.zoomBy(1.25f));menu.item("Zoom out","pictures",ready(),()->canvas.zoomBy(.8f));menu.separator();menu.item(playing?"Pause slideshow":"Play slideshow","pictures",ready()&&entries.size()>1,this::toggleSlideshow);menu.item("Previous picture","back",index>0,()->step(-1));menu.item("Next picture","forward",index+1<entries.size(),()->step(1));show(menu,anchor);}
    private void properties(){String text=name+"\n\nType: "+mime+"\n"+(size>=0?"Size: "+android.text.format.Formatter.formatFileSize(activity,size)+"\n":"")+(width>0?"Dimensions: "+width+" × "+height+" pixels\n":"")+"View rotation: "+canvas.rotation()+"°\n\nRotation and zoom change this view only. Large pictures are resized for viewing. Animated images show their first frame.";Aero.dialog(activity).setTitle("Picture properties").setMessage(text).setPositiveButton("OK",null).show();}
    private void share(){try{Intent intent=new Intent(Intent.ACTION_SEND).setType(mime).putExtra(Intent.EXTRA_STREAM,current).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);intent.setClipData(ClipData.newRawUri(name,current));activity.startActivity(Intent.createChooser(intent,"Share picture"));}catch(RuntimeException failure){Aero.dialog(activity).setTitle("Cannot share picture").setMessage("Read access to this picture is unavailable. Open it again from Explorer.").setPositiveButton("OK",null).show();}}
    JSONObject state(){JSONObject value=new JSONObject();if(current!=null){GadgetLayout.put(value,"uri",current.toString());GadgetLayout.put(value,"name",name);GadgetLayout.put(value,"mime",mime);GadgetLayout.put(value,"source",source);GadgetLayout.put(value,"window",sourceWindow);GadgetLayout.put(value,"view",canvas.state());}return value;}
    void restore(JSONObject value){if(value==null||!value.has("uri"))return;String uri=value.optString("uri");if(uri.length()>8192)return;open(Uri.parse(uri),value.optString("name"),value.optString("mime"),value.optJSONObject("source"),value.optString("window","@files"),value.optJSONObject("view"));}
    void active(boolean value){if(closed)return;foreground=value;if(!value){paused=true;stop();dismissMenu();}else if(paused&&current!=null){paused=false;Uri target=current;int request=generation;worker.execute(()->{try(android.content.res.AssetFileDescriptor file=activity.getContentResolver().openAssetFileDescriptor(target,"r")){if(file==null)throw new java.io.FileNotFoundException();}catch(Exception failure){main.post(()->{if(closed||generation!=request)return;canvas.image(null);overlay("Picture access was removed","Reconnect its folder in Explorer and open the picture again.",false);update();});}});}}
    @Override protected void onVisibilityChanged(View changed,int visibility){super.onVisibilityChanged(changed,visibility);if(main!=null){if(!isShown()){paused=true;stop();dismissMenu();}else if(!closed)active(foreground);}}
    private void dismissMenu(){if(menu!=null){menu.dismiss();menu=null;}}
    void close(){if(closed)return;closed=true;stop();dismissMenu();if(cancellation!=null)cancellation.cancel();generation++;main.removeCallbacksAndMessages(null);worker.shutdownNow();canvas.image(null);}
}
