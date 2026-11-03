package com.nthexperience.desktop;

import android.content.Context;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;

/** Virtual recovery folder over the verified bin, not a raw directory of internal payloads. */
final class RecyclePanel extends VistaShell {
    private final ExecutorService worker=Executors.newSingleThreadExecutor();private final Handler main=new Handler(Looper.getMainLooper());private final Runnable changed;private final DocumentOps.Cancel cancel=new DocumentOps.Cancel();
    private static final String[] COLUMNS={"Name","Original location","Date deleted","Size"};
    private static final String[] VIEWS={"Large icons","Medium icons","Small icons","List","Details","Tiles"};
    private static final float[] WEIGHTS={.35f,.27f,.24f,.14f};
    private List<JSONObject> items=new ArrayList<>();
    private String selected="",sort="Date deleted",view="Details";
    private boolean closed,busy,loaded,refreshPending,descending=true;
    private final TextView restoreButton,emptyButton;
    private final View classicMenu;
    private int viewWidth;

    RecyclePanel(Context context,DesktopStore store,Actions actions,Runnable changed){
        super(context,store,actions,"Search Recycle Bin");
        this.changed=changed;
        addCommand("Organize ▾",this::organize);
        addCommand("Views ▾",this::views);
        emptyButton=addCommand("Empty Recycle Bin",this::confirmEmpty);
        restoreButton=addCommand("Restore all items",()->{if(selected()==null)restoreAll();else restoreSelected();});
        classicMenu=explorerChrome(this::menu);
        classicMenu.setVisibility(GONE);
        useCompactExplorerToolbar();
        content.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{
            if(r-l!=viewWidth){viewWidth=r-l;if(!view.equals("Details"))render();}
        });
        setOnDragListener((v,event)->{
            if(!(event.getLocalState() instanceof FileDrag))return false;
            if(event.getAction()==DragEvent.ACTION_DROP){
                FileDrag drag=(FileDrag)event.getLocalState();
                main.post(()->drag.explorer.dropInRecycleBin(drag));
            }
            return true;
        });
        start("recycle");
        refresh();
    }
    private TextView addCommand(String label,Runnable action){
        TextView button=command(label,action);
        button.setMaxLines(2);
        button.setPadding(dp(2),0,dp(2),0);
        commands.addView(button,new LayoutParams(0,-1,label.length()>10?1.45f:1f));
        return button;
    }
    @Override void render(){
        if(closed)return;
        if(side.getChildCount()==0){
            side.addView(Aero.text(getContext(),"Favorite Links",12,0xff425d71));
            sideLink("Documents",()->actions.open("@documents"));
            sideLink("Pictures",()->actions.open("@photos"));
            sideLink("Computer",()->actions.open("@computer"));
            side.addView(Aero.text(getContext(),"Folders",12,0xff425d71));
            sideLink("Desktop",()->actions.action("show-desktop"));
        }
        address.setText("Recycle Bin");
        LinearLayout column=Aero.column(getContext());
        List<JSONObject> shown=new ArrayList<>();
        for(JSONObject item:items)if(query().isEmpty()||item.optString("name").toLowerCase(Locale.getDefault()).contains(query()))shown.add(item);
        if(loaded&&shown.stream().noneMatch(i->i.optString("id").equals(selected)))selected="";
        updateCommands();
        shown.sort((a,b)->{
            int result=sort.equals("Size")?Long.compare(a.optLong("size"),b.optLong("size"))
                    :sort.equals("Date deleted")?Long.compare(a.optLong("date"),b.optLong("date"))
                    :(sort.equals("Original location")?a.optString("location"):a.optString("name"))
                    .compareToIgnoreCase(sort.equals("Original location")?b.optString("location"):b.optString("name"));
            return descending?-result:result;
        });
        if(view.equals("Details")){
            LinearLayout header=new LinearLayout(getContext());
            for(int i=0;i<COLUMNS.length;i++){
                String name=COLUMNS[i];
                TextView label=command(name,()->sortBy(name));
                label.setTextSize(10);
                label.setMaxLines(2);
                label.setPadding(dp(2),0,dp(2),0);
                label.setContentDescription("Sort Recycle Bin by "+name);
                header.addView(label,new LayoutParams(0,dp(32),WEIGHTS[i]));
            }
            column.addView(header);
            for(JSONObject item:shown)column.addView(itemRow(item,true));
        }else{
            int minWidth=view.equals("Tiles")?150:view.equals("List")?125:view.equals("Large icons")?104:view.equals("Medium icons")?88:72;
            int columns=Math.max(1,viewWidth/dp(minWidth));
            LinearLayout row=null;
            for(int i=0;i<shown.size();i++){
                if(i%columns==0){row=new LinearLayout(getContext());column.addView(row);}
                row.addView(itemRow(shown.get(i),false),new LayoutParams(0,-2,1));
            }
            if(row!=null&&shown.size()%columns!=0)
                for(int i=shown.size()%columns;i<columns;i++)row.addView(new View(getContext()),new LayoutParams(0,1,1));
        }
        if(shown.isEmpty()){
            TextView message=Aero.text(getContext(),loaded?query().isEmpty()?"Recycle Bin is empty":"No matching recovery items":"Loading Recycle Bin…",13,Aero.INK);
            message.setPadding(dp(10),dp(10),dp(10),dp(10));
            column.addView(message);
        }
        status.setText(items.size()+" item"+(items.size()==1?"":"s")+" • "+android.text.format.Formatter.formatShortFileSize(getContext(),items.stream().mapToLong(i->i.optLong("size")).sum())+" / "+store.prefs.getInt("recycle_capacity_mb",250)+" MB"+(busy?" • Working…":""));
        body(scrolling(column));
    }
    private View itemRow(JSONObject item,boolean details){
        boolean gallery=!details&&view.endsWith("icons");
        LinearLayout row=new LinearLayout(getContext());
        row.setOrientation(gallery?VERTICAL:HORIZONTAL);
        row.setGravity(gallery?Gravity.CENTER:Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(gallery?76:42));
        row.setPadding(dp(3),dp(3),dp(3),dp(3));
        row.setFocusable(true);
        row.setContentDescription("Recycle item: "+item.optString("name"));
        row.setTag(item.optString("id"));
        if(item.optString("id").equals(selected))row.setBackground(Aero.panel(getContext(),0xffe4f3ff,0xffc8e3f5,1,0xff89b3cf));
        LinearLayout name=new LinearLayout(getContext());
        name.setOrientation(gallery?VERTICAL:HORIZONTAL);
        name.setGravity(gallery?Gravity.CENTER:Gravity.CENTER_VERTICAL);
        int size=details||view.equals("List")?17:view.equals("Large icons")?48:view.equals("Small icons")?23:33;
        View icon=itemIcon(item,size);
        icon.setTag("recycle-item-icon");
        icon.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        name.addView(icon,new LayoutParams(dp(size),dp(size)));
        TextView title=cell(item.optString("name"));
        if(gallery)title.setGravity(Gravity.CENTER);
        name.addView(title,new LayoutParams(gallery?-1:0,-2,gallery?0:1));
        row.addView(name,new LayoutParams(gallery?-1:0,-2,details?WEIGHTS[0]:gallery?0:1));
        if(details){
            String[] values={item.optString("location","Original folder"),
                    java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT,java.text.DateFormat.SHORT).format(new Date(item.optLong("date"))),
                    isFolder(item)||item.has("shortcut")?"":android.text.format.Formatter.formatShortFileSize(getContext(),item.optLong("size"))};
            for(int i=0;i<values.length;i++)row.addView(cell(values[i]),new LayoutParams(0,-2,WEIGHTS[i+1]));
        }
        row.setOnClickListener(v->selectItem(item));
        ContextMenus.bind(row,(x,y)->{
            DesktopContextMenu menu=new DesktopContextMenu(getContext(),"Recycle item commands");
            menu.item("Restore","recycle",!busy,()->restoreItem(item));
            menu.item("Delete permanently…","delete",!busy,()->confirmRemove(item));
            menu.item("Properties","control",()->itemProperties(item));
            selectItem(item);
            ContextMenus.show(menu,row,x,y);
        });
        return row;
    }
    private boolean isFolder(JSONObject item){
        return android.provider.DocumentsContract.Document.MIME_TYPE_DIR.equals(item.optString("mime"))||
                item.optString("shortcut").startsWith("folder:")||FolderLinks.id(item.optString("shortcut"));
    }
    private View itemIcon(JSONObject item,int size){
        if(item.has("shortcut")&&getContext() instanceof MainActivity)
            return ((MainActivity)getContext()).icon(item.optString("shortcut"),size);
        return new AeroIcon(getContext(),VistaExplorer.icon(item.optString("mime"),isFolder(item)));
    }
    private TextView cell(String value){
        TextView cell=Aero.text(getContext(),value,11,Aero.INK);
        cell.setMaxLines(2);
        cell.setEllipsize(android.text.TextUtils.TruncateAt.END);
        cell.setPadding(dp(3),dp(3),dp(2),dp(3));
        return cell;
    }
    private void selectItem(JSONObject item){
        View previous=content.findViewWithTag(selected);
        if(previous!=null)previous.setBackground(null);
        selected=item.optString("id");
        View current=content.findViewWithTag(selected);
        if(current!=null)current.setBackground(Aero.panel(getContext(),0xffe4f3ff,0xffc8e3f5,1,0xff89b3cf));
        updateCommands();
    }
    private void updateCommands(){
        restoreButton.setText(selected()==null?"Restore all items":"Restore this item");
        for(TextView button:new TextView[]{restoreButton,emptyButton}){
            button.setEnabled(!busy&&!items.isEmpty());
            button.setAlpha(button.isEnabled()?1f:.5f);
        }
    }
    private void sortBy(String name){descending=sort.equals(name)?!descending:name.equals("Date deleted");sort=name;render();}
    private JSONObject selected(){for(JSONObject item:items)if(item.optString("id").equals(selected))return item;return null;}
    private void restoreSelected(){JSONObject item=selected();if(item==null){Toast.makeText(getContext(),"Select a recovery file first.",Toast.LENGTH_SHORT).show();return;}restoreItem(item);}
    private void restoreItem(JSONObject item){operation(()->{RecycleBin.restore(getContext(),item,cancel);return "Item restored";});}
    private void restoreAll(){if(items.isEmpty()||busy)return;List<JSONObject> snapshot=new ArrayList<>(items);operation(()->{int done=0;for(JSONObject item:snapshot){RecycleBin.restore(getContext(),item,cancel);done++;}return done+" items restored";});}
    private void confirmRemove(JSONObject item){Aero.dialog(getContext()).setTitle("Delete recovery copy?").setMessage(item.optString("name")+". This cannot be undone.").setPositiveButton("Delete",(d,w)->operation(()->{RecycleBin.remove(getContext(),item);return "Recovery copy removed";})).setNegativeButton("Cancel",null).show();}
    private void confirmEmpty(){
        if(!busy)confirmEmpty(getContext(),()->operation(()->{RecycleBin.empty(getContext());return "Recycle Bin emptied";}));
    }
    static void confirmEmpty(Context context,Runnable empty){
        Aero.dialog(context).setTitle("Empty Recycle Bin?")
                .setMessage("Permanently remove all recovery copies? This cannot be undone.")
                .setPositiveButton("Empty",(d,w)->empty.run()).setNegativeButton("Cancel",null).show();
    }
    private void itemProperties(JSONObject item){Aero.dialog(getContext()).setTitle(item.optString("name")).setMessage("Original location: "+item.optString("location","Original folder")+"\nDeleted: "+java.text.DateFormat.getDateTimeInstance().format(new Date(item.optLong("date")))+"\nSize: "+android.text.format.Formatter.formatFileSize(getContext(),item.optLong("size"))+"\n"+(item.optBoolean("deleted")?"Original deletion confirmed":"Recovery copy; original deletion unconfirmed")+"\nRestore keeps both names if the destination already exists.").setPositiveButton("OK",null).show();}
    void properties(){LinearLayout body=column();body.addView(Aero.text(getContext(),"Maximum bin size (new deletions)",14,Aero.INK));Spinner capacity=new Spinner(getContext());String[] choices={"100 MB","250 MB","500 MB","1 GB","2 GB"};int[] values={100,250,500,1024,2048};capacity.setAdapter(new ArrayAdapter<>(getContext(),android.R.layout.simple_spinner_dropdown_item,choices));int current=store.prefs.getInt("recycle_capacity_mb",250);for(int i=0;i<values.length;i++)if(values[i]==current)capacity.setSelection(i);capacity.setContentDescription("Recycle Bin maximum size");body.addView(capacity);CheckBox icon=Aero.check(getContext(),"Show Recycle Bin on desktop",store.pinned("@recycle"));body.addView(icon);body.addView(Aero.text(getContext(),"Up to 100 MB per file or folder and 200 recovery items. Existing copies are kept when the limit is lowered. Items from locations without recovery support stay in place unless you explicitly choose permanent deletion.",12,Aero.INK));Aero.dialog(getContext()).setTitle("Recycle Bin Properties").setView(body).setPositiveButton("Apply",(d,w)->{store.prefs.edit().putInt("recycle_capacity_mb",values[capacity.getSelectedItemPosition()]).apply();if(icon.isChecked()!=store.pinned("@recycle"))store.toggle("@recycle");changed.run();render();}).setNegativeButton("Cancel",null).show();}
    private void show(DesktopContextMenu menu){ContextMenus.centered(commands,(x,y)->ContextMenus.show(menu,commands,x,y));}
    private void organize(){
        DesktopContextMenu menu=new DesktopContextMenu(getContext(),"Recycle Bin Organize");
        menu.submenu("Layout","appearance",this::layoutMenu);
        menu.separator();
        menu.item("Properties","control",this::properties);
        show(menu);
    }
    private void layoutMenu(){
        DesktopContextMenu menu=new DesktopContextMenu(getContext(),"Recycle Bin Layout");
        menu.checked("Menu bar",classicMenu.getVisibility()==VISIBLE,()->classicMenu.setVisibility(classicMenu.getVisibility()==VISIBLE?GONE:VISIBLE));
        menu.checked("Navigation pane",navigationPaneVisible(),this::toggleNavigationPane);
        show(menu);
    }
    private void views(){
        DesktopContextMenu menu=new DesktopContextMenu(getContext(),"Recycle Bin Views");
        for(String label:VIEWS)menu.checked(label,view.equals(label),()->{view=label;render();});
        show(menu);
    }
    private void menu(String label){
        DesktopContextMenu menu=new DesktopContextMenu(getContext(),"Recycle Bin "+label+" menu");
        JSONObject item=selected();
        switch(label){
            case "File":
                menu.item("Restore","recycle",item!=null&&!busy,()->restoreItem(item));
                menu.item("Delete permanently…","delete",item!=null&&!busy,()->confirmRemove(item));
                menu.item("Properties","control",()->{if(item==null)properties();else itemProperties(item);});
                menu.separator();
                menu.item("Close","close",this::closeWindow);
                break;
            case "Edit":
                menu.item("Clear selection","checklist",item!=null,()->{selected="";render();});
                break;
            case "View":
                menu.submenu("Views","appearance",this::views);
                for(String name:COLUMNS)menu.checked("Sort by "+name,sort.equals(name),()->sortBy(name));
                menu.item("Refresh","refresh",this::refresh);
                break;
            case "Tools":
                menu.item("Recycle Bin Properties","control",this::properties);
                break;
            case "Help":
                menu.item("Recycle Bin Help","control",()->Aero.dialog(getContext()).setTitle("Recycle Bin")
                        .setMessage("Select an item to restore it to its original location. Hold an item for individual commands. Empty Recycle Bin permanently removes every recovery item.")
                        .setPositiveButton("OK",null).show());
                break;
        }
        show(menu);
    }
    private void closeWindow(){
        for(View view=this;view.getParent() instanceof View;view=(View)view.getParent())
            if(view.getParent() instanceof DesktopWindow){((DesktopWindow)view.getParent()).closeWindow();return;}
    }
    interface Job<T>{T run()throws Exception;}
    private <T> void run(Job<T> job,java.util.function.Consumer<T> done){if(busy||closed)return;busy=true;updateCommands();worker.execute(()->{try{T result=job.run();main.post(()->{busy=false;if(!closed){done.accept(result);refreshIfPending();}});}catch(Exception ex){main.post(()->{busy=false;if(!closed){Toast.makeText(getContext(),ex.getMessage()==null?"Operation unavailable":ex.getMessage(),Toast.LENGTH_LONG).show();DesktopSounds.play(getContext(),"error");loaded=true;render();refreshIfPending();}});}});}
    private void operation(Job<String> job){run(job,text->{Toast.makeText(getContext(),text,Toast.LENGTH_LONG).show();DesktopSounds.play(getContext(),"complete");changed.run();refresh();});}
    private void refreshIfPending(){if(refreshPending&&!busy)refresh();}
    void refresh(){if(busy){refreshPending=true;return;}refreshPending=false;run(()->RecycleBin.list(getContext()),values->{items=values;loaded=true;render();});}
    @Override JSONObject extraState(){JSONObject value=new JSONObject();GadgetLayout.put(value,"sort",sort);GadgetLayout.put(value,"descending",descending);GadgetLayout.put(value,"selected",selected);GadgetLayout.put(value,"view",view);GadgetLayout.put(value,"menuBar",classicMenu.getVisibility()==VISIBLE);return value;}
    @Override void restoreExtra(JSONObject state){if(state!=null){sort=state.optString("sort","Date deleted");descending=state.optBoolean("descending",true);selected=state.optString("selected");String saved=state.optString("view");if(Arrays.asList(VIEWS).contains(saved))view=saved;classicMenu.setVisibility(state.optBoolean("menuBar")?VISIBLE:GONE);}}
    @Override void showFolders(){toggleNavigationPane();}
    @Override void goUp(){actions.open("@computer");}
    @Override void close(){if(closed)return;closed=true;cancel.cancel();worker.shutdown();main.removeCallbacksAndMessages(null);}
}
