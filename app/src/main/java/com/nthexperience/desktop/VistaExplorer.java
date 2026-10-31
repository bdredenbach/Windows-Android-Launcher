package com.nthexperience.desktop;

import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.provider.DocumentsContract;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

final class VistaExplorer extends VistaShell {
    interface Files {void chooseFolder();void pickFile(String kind);void openDocument(Uri uri,String mime);void app(String id);}
    private final Files files;
    private final Supplier<List<AppCatalog.Entry>> apps;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private CancellationSignal cancellation;
    private String loadedPage="";
    private DocumentFolders.Listing listing;
    private boolean details,closed,failed;
    private int generation;
    VistaExplorer(Context c,DesktopStore store,Actions actions,Files files,Supplier<List<AppCatalog.Entry>> apps,String initial){
        super(c,store,actions,"Search this folder");this.files=files;this.apps=apps;details=store.prefs.getBoolean("explorer_details",false);
        commands.addView(command("Organize ▾",this::organize),new LayoutParams(-2,dp(36)));commands.addView(command("Views ▾",this::views),new LayoutParams(-2,dp(36)));address.setOnClickListener(v->breadcrumbs());start(initial);
    }
    @Override void render(){
        if(closed)return;side.removeAllViews();sideLink("Favorite Links",()->navigate("home"));sideLink("Documents",()->files.pickFile("documents"));sideLink("Pictures",()->files.pickFile("pictures"));sideLink("Music",()->files.pickFile("music"));sideLink("Computer",()->navigate("computer"));sideLink("File Managers",()->navigate("managers"));sideLink("Add a folder…",files::chooseFolder);
        String page=page();address.setText(title(page));status.setText("Nth Explorer");
        if(page.startsWith("folder:")){
            if(!loadedPage.equals(page)){load(page);return;}if(failed){error();return;}if(listing==null){loadingMessage();return;}fileList();return;
        }
        if(cancellation!=null)cancellation.cancel();loadedPage="";listing=null;
        if(page.equals("managers")||page.equals("add-apps")){appList(page.equals("add-apps"));return;}
        LinearLayout column=column();
        if(page.equals("computer")){
            heading(column,"Computer","Storage and folders on your Android device");task(column,"drive","Internal storage",storageSummary(),()->actions.action("android:storage"));
            try{StatFs disk=new StatFs(Environment.getDataDirectory().getPath());ProgressBar meter=new ProgressBar(getContext(),null,android.R.attr.progressBarStyleHorizontal);meter.setMax(100);meter.setProgress(Math.round(100*(1-disk.getAvailableBytes()/(float)disk.getTotalBytes())));meter.setProgressTintList(android.content.res.ColorStateList.valueOf(0xff2086bc));LayoutParams lp=new LayoutParams(-1,dp(9));lp.setMargins(dp(57),0,dp(9),dp(16));column.addView(meter,lp);}catch(RuntimeException ignored){}
            link(column,"System properties",()->actions.open("@system"));link(column,"Open Control Panel",()->actions.open("@control"));
        }else heading(column,"Your files and folders","Open a file, use a file manager, or link a folder to browse here.");
        String[][] favorites={{"documents","Documents","Open a document"},{"pictures","Pictures","Open a picture"},{"music","Music","Open an audio file"},{"videos","Videos","Open a video"},{"downloads","Downloads","Choose a downloaded file"}};
        for(String[] favorite:favorites)if(matches(favorite[1]))task(column,favorite[0],favorite[1],favorite[2],()->files.pickFile(favorite[0]));
        if(matches("File Managers"))task(column,"folderapps","File Managers","Your chosen file manager apps",()->navigate("managers"));
        heading(column,"Linked folders","Folders you choose can be browsed inside this window.");JSONArray links=DocumentFolders.links(store);int shown=0;
        for(int i=0;i<links.length();i++){JSONObject folder=links.optJSONObject(i);if(folder==null)continue;String uri=folder.optString("uri"),name=folder.optString("name","Linked folder");if(!matches(name))continue;shown++;int before=column.getChildCount();task(column,"files",name,"Browse folder",()->openLinked(uri,name));View row=column.getChildAt(before);row.setOnLongClickListener(v->{PopupMenu menu=new PopupMenu(getContext(),row);menu.getMenu().add("Remove folder link").setOnMenuItemClickListener(w->{DocumentFolders.remove(getContext(),store,uri);render();return true;});menu.show();return true;});}
        link(column,"Add a folder…",files::chooseFolder).setContentDescription("Link a folder");status.setText(shown+" linked folder"+(shown==1?"":"s")+"  •  File access stays under your control");body(scrolling(column));
    }
    private boolean matches(String text){return query().isEmpty()||text.toLowerCase(Locale.getDefault()).contains(query());}
    private String storageSummary(){try{StatFs disk=new StatFs(Environment.getDataDirectory().getPath());return android.text.format.Formatter.formatFileSize(getContext(),disk.getAvailableBytes())+" free of "+android.text.format.Formatter.formatFileSize(getContext(),disk.getTotalBytes());}catch(RuntimeException ex){return "View device storage in Android";}}
    private String title(String page){if(page.startsWith("folder:"))try{JSONArray crumbs=DocumentFolders.path(page).getJSONArray("crumbs");StringBuilder title=new StringBuilder("Computer");for(int i=0;i<crumbs.length();i++)title.append("  ›  ").append(crumbs.getJSONObject(i).optString("name"));return title.toString();}catch(JSONException ignored){}switch(page){case "computer":return "Computer";case "managers":return "Files  ›  File Managers";case "add-apps":return "File Managers  ›  Add apps";default:return "Files";}}
    void openLinked(String uri,String name){try{if(!DocumentsContract.isTreeUri(Uri.parse(uri)))throw new IllegalArgumentException();navigate(DocumentFolders.rootPage(uri,name));}catch(RuntimeException ex){Toast.makeText(getContext(),"Choose this folder again to restore access",Toast.LENGTH_SHORT).show();}}
    void folderAdded(String uri,String name){openLinked(uri,name);}
    private void load(String page){
        if(cancellation!=null)cancellation.cancel();loadedPage=page;listing=null;failed=false;cancellation=new CancellationSignal();CancellationSignal signal=cancellation;int request=++generation;loadingMessage();
        worker.execute(()->{DocumentFolders.Listing result=null;try{result=DocumentFolders.read(getContext(),page,signal);}catch(RuntimeException|JSONException ignored){}DocumentFolders.Listing loaded=result;main.post(()->{if(closed||signal.isCanceled()||request!=generation||!page.equals(page()))return;listing=loaded;failed=loaded==null;if(failed)error();else fileList();});});
    }
    private void loadingMessage(){LinearLayout column=column();heading(column,"Opening folder…","");ProgressBar progress=new ProgressBar(getContext());column.addView(progress,new LayoutParams(dp(36),dp(36)));loading(scrolling(column));}
    private void error(){LinearLayout column=column();heading(column,"This folder is unavailable","It may have moved, or access may have been removed.");link(column,"Choose a folder again…",files::chooseFolder);link(column,"Return to your folders",()->navigate("home"));status.setText("Folder unavailable");body(scrolling(column));}
    private void fileList(){
        ArrayList<DocumentFolders.Item> visible=new ArrayList<>();for(DocumentFolders.Item item:listing.items)if(matches(item.name))visible.add(item);
        status.setText((listing.limited?"First 2,000 items  •  ":"")+visible.size()+" item"+(visible.size()==1?"":"s")+(query().isEmpty()?"":" matching your search"));
        if(visible.isEmpty()){LinearLayout empty=column();heading(empty,query().isEmpty()?"This folder is empty":"No matching items",query().isEmpty()?"Choose another folder with the navigation arrows.":"Search checks names in the current folder.");body(scrolling(empty));return;}
        ListView list=new ListView(getContext());list.setDividerHeight(dp(1));list.setDivider(new android.graphics.drawable.ColorDrawable(0xffeef3f7));list.setSelector(Aero.selection(getContext()));list.setContentDescription("Folder contents");list.setFastScrollEnabled(visible.size()>100);list.setAdapter(new BaseAdapter(){
            public int getCount(){return visible.size();}public Object getItem(int p){return visible.get(p);}public long getItemId(int p){return p;}
            public View getView(int position,View recycled,ViewGroup parent){DocumentFolders.Item item=visible.get(position);String type=item.directory()?"File folder":fileType(item.mime);String meta=item.directory()?type:(item.size<0?type:android.text.format.Formatter.formatFileSize(getContext(),item.size)+"  •  "+type);if(details&&item.modified>0)meta+="  •  "+android.text.format.DateFormat.getDateFormat(getContext()).format(new Date(item.modified));return row(icon(item.mime,item.directory()),null,item.name,meta);}
        });list.setOnItemClickListener((parent,v,position,id)->{DocumentFolders.Item item=visible.get(position);if(item.directory())try{navigate(DocumentFolders.childPage(page(),item));}catch(JSONException ex){error();}else try{files.openDocument(DocumentFolders.documentUri(page(),item.id),item.mime);}catch(JSONException ex){error();}});
        list.setOnItemLongClickListener((parent,v,position,id)->{DocumentFolders.Item item=visible.get(position);new android.app.AlertDialog.Builder(getContext()).setTitle(item.name).setMessage((item.directory()?"File folder":fileType(item.mime))+(item.size<0?"":"\nSize: "+android.text.format.Formatter.formatFileSize(getContext(),item.size))+(item.modified<=0?"":"\nModified: "+java.text.DateFormat.getDateTimeInstance().format(new Date(item.modified)))).setPositiveButton("OK",null).show();return true;});body(list);
    }
    private View row(String kind,android.graphics.drawable.Drawable drawable,String name,String description){LinearLayout row=new LinearLayout(getContext());row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(11),dp(7),dp(9),dp(7));View icon;if(drawable==null)icon=new AeroIcon(getContext(),kind);else{ImageView image=new ImageView(getContext());image.setImageDrawable(drawable);image.setScaleType(ImageView.ScaleType.FIT_CENTER);icon=image;}row.addView(icon,new LayoutParams(dp(details?28:40),dp(details?28:40)));LinearLayout text=Aero.column(getContext());text.setPadding(dp(10),0,0,0);TextView title=Aero.text(getContext(),name,13,Aero.INK);title.setMaxLines(2);title.setEllipsize(android.text.TextUtils.TruncateAt.END);text.addView(title);TextView sub=Aero.text(getContext(),description,11,0xff6c7e8a);sub.setSingleLine(true);sub.setEllipsize(android.text.TextUtils.TruncateAt.END);sub.setPadding(0,dp(3),0,0);text.addView(sub);row.addView(text,new LayoutParams(0,-2,1));row.setContentDescription(name);return row;}
    private static String icon(String mime,boolean directory){return directory?"files":mime.startsWith("image/")?"pictures":mime.startsWith("audio/")?"music":mime.startsWith("video/")?"videos":"documents";}
    private static String fileType(String mime){if(mime.startsWith("image/"))return "Picture";if(mime.startsWith("audio/"))return "Audio file";if(mime.startsWith("video/"))return "Video";if(mime.startsWith("text/"))return "Text document";if(mime.equals("application/pdf"))return "PDF document";return "File";}
    private void appList(boolean adding){
        List<AppCatalog.Entry> source=apps.get(),visible=new ArrayList<>();List<String> selected=store.fileManagers();for(AppCatalog.Entry app:source)if((adding||selected.contains(app.id))&&matches(app.label))visible.add(app);
        LinearLayout column=column();heading(column,adding?"Add file manager apps":"File Managers",adding?"Choose apps to keep in this folder. Tap again to remove.":"Keep your preferred file managers together.");
        if(!adding){task(column,"files","System file picker","Browse files with Android",()->files.pickFile("documents"));link(column,"Add file manager apps…",()->navigate("add-apps")).setContentDescription("Add file manager apps");}
        ListView list=new ListView(getContext());list.setDividerHeight(0);list.setSelector(Aero.selection(getContext()));list.addHeaderView(column,null,false);
        if(adding){LinearLayout footer=column();if(visible.isEmpty())heading(footer,"No matching apps","Try clearing the search, or install your preferred file manager.");link(footer,"Done",()->navigate("managers"));list.addFooterView(footer,null,false);}
        list.setAdapter(new BaseAdapter(){public int getCount(){return visible.size();}public Object getItem(int p){return visible.get(p);}public long getItemId(int p){return p;}public View getView(int p,View recycled,ViewGroup parent){AppCatalog.Entry app=visible.get(p);View row=row("programs",app.icon,app.label,adding?(store.fileManagers().contains(app.id)?"✓  In File Managers":"Tap to add"):"Tap to open • Hold to remove");row.setContentDescription((adding?"Choose file manager app: ":"File manager app: ")+app.label);return row;}});
        list.setOnItemClickListener((parent,view,position,id)->{int index=position-list.getHeaderViewsCount();if(index<0||index>=visible.size())return;AppCatalog.Entry app=visible.get(index);if(adding){List<String> saved=store.fileManagers();if(saved.contains(app.id))saved.remove(app.id);else saved.add(app.id);store.fileManagers(saved);int first=list.getFirstVisiblePosition();render();if(content.getChildCount()>0&&content.getChildAt(0) instanceof ListView)((ListView)content.getChildAt(0)).setSelection(first);}else files.app(app.id);});
        list.setOnItemLongClickListener((parent,view,position,id)->{int index=position-list.getHeaderViewsCount();if(adding||index<0||index>=visible.size())return false;AppCatalog.Entry app=visible.get(index);PopupMenu menu=new PopupMenu(getContext(),view);menu.getMenu().add("Remove from File Managers").setOnMenuItemClickListener(v->{List<String> saved=store.fileManagers();saved.remove(app.id);store.fileManagers(saved);render();return true;});menu.show();return true;});
        status.setText(adding?selected.size()+" selected apps":visible.size()+" available file manager app"+(visible.size()==1?"":"s"));body(list);
    }
    private void organize(){PopupMenu menu=new PopupMenu(getContext(),commands);menu.getMenu().add("Add a folder…").setOnMenuItemClickListener(v->{files.chooseFolder();return true;});menu.getMenu().add("Add file manager apps…").setOnMenuItemClickListener(v->{navigate("add-apps");return true;});menu.getMenu().add("Refresh").setOnMenuItemClickListener(v->{loadedPage="";render();return true;});menu.getMenu().add("Open Control Panel").setOnMenuItemClickListener(v->{actions.open("@control");return true;});menu.show();}
    private void views(){PopupMenu menu=new PopupMenu(getContext(),commands);for(String label:new String[]{"Tiles","Details"})menu.getMenu().add(label).setOnMenuItemClickListener(v->{details=label.equals("Details");store.prefs.edit().putBoolean("explorer_details",details).apply();render();return true;});menu.show();}
    private void breadcrumbs(){PopupMenu menu=new PopupMenu(getContext(),address);menu.getMenu().add("Computer").setOnMenuItemClickListener(v->{navigate("computer");return true;});if(page().startsWith("folder:"))try{JSONObject path=DocumentFolders.path(page());JSONArray crumbs=path.getJSONArray("crumbs");for(int i=0;i<crumbs.length();i++){JSONArray prefix=new JSONArray();for(int j=0;j<=i;j++)prefix.put(crumbs.getJSONObject(j));JSONObject target=new JSONObject();GadgetLayout.put(target,"tree",path.getString("tree"));GadgetLayout.put(target,"crumbs",prefix);String destination="folder:"+target;menu.getMenu().add(crumbs.getJSONObject(i).optString("name")).setOnMenuItemClickListener(v->{navigate(destination);return true;});}}catch(JSONException ignored){}menu.show();}
    @Override void showFolders(){PopupMenu menu=new PopupMenu(getContext(),folders);for(String[] item:new String[][]{{"Files","home"},{"Computer","computer"},{"File Managers","managers"}})menu.getMenu().add(item[0]).setOnMenuItemClickListener(v->{navigate(item[1]);return true;});JSONArray links=DocumentFolders.links(store);for(int i=0;i<links.length();i++){JSONObject folder=links.optJSONObject(i);if(folder!=null)menu.getMenu().add(folder.optString("name")).setOnMenuItemClickListener(v->{openLinked(folder.optString("uri"),folder.optString("name"));return true;});}menu.show();}
    @Override void goUp(){if(page().startsWith("folder:"))try{JSONObject path=DocumentFolders.path(page());JSONArray crumbs=path.getJSONArray("crumbs");if(crumbs.length()>1){crumbs.remove(crumbs.length()-1);navigate("folder:"+path);return;}}catch(JSONException ignored){}navigate(page().equals("add-apps")?"managers":"home");}
    @Override void refreshApps(){if(page().equals("managers")||page().equals("add-apps"))render();}
    @Override JSONObject extraState(){JSONObject state=new JSONObject();GadgetLayout.put(state,"details",details);return state;}
    @Override void restoreExtra(JSONObject value){if(value!=null)details=value.optBoolean("details");}
    @Override void close(){closed=true;if(cancellation!=null)cancellation.cancel();main.removeCallbacksAndMessages(null);worker.shutdownNow();}
}
