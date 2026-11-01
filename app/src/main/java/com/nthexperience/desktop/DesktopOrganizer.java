package com.nthexperience.desktop;

import android.content.Context;
import android.text.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;
import java.util.function.Supplier;

/** Ordinary launcher folders, shortcut names and Quick Launch, in native Vista chrome. */
final class DesktopOrganizer extends VistaShell {
    interface Host {String label(String id);View icon(String id,int size);void changed();void open(String id);}
    private final Host host;
    private final Supplier<List<AppCatalog.Entry>> apps;
    private ListView currentList;
    private EditText nameInput;
    private String draft;
    DesktopOrganizer(Context context,DesktopStore store,Actions actions,Host host,Supplier<List<AppCatalog.Entry>> apps,String initial){
        super(context,store,actions,"Search shortcuts");this.host=host;this.apps=apps;
        commands.addView(command("New folder",()->{draft=null;navigate("new");}),new LayoutParams(-2,dp(36)));
        commands.addView(command("Add programs",()->navigate("add:"+target())),new LayoutParams(-2,dp(36)));start(initial);
    }
    private String target(){String page=page();if(page.startsWith("add:"))return page.substring(4);return page.startsWith("contents:")||page.equals("quick")?page:"desktop";}
    private String folderId(String page){return page.startsWith("contents:")?page.substring(9):null;}
    private List<String> members(String target){String id=folderId(target);return id!=null?store.folderItems(id):target.equals("quick")?store.quickLaunch():store.desktop();}
    private void members(String target,List<String> values){String id=folderId(target);if(id!=null)store.folderItems(id,values);else if(target.equals("quick"))store.quickLaunch(values);else store.desktop(values);}
    @Override void render(){
        currentList=null;nameInput=null;side.removeAllViews();sideLink("Desktop shortcuts",()->navigate("desktop"));sideLink("Quick Launch",()->navigate("quick"));sideLink("New folder",()->{draft=null;navigate("new");});sideLink("Backup and Restore",()->host.open("@backup"));
        JSONObject folders=store.folders();Iterator<String> keys=folders.keys();while(keys.hasNext()){String id=keys.next();sideLink(host.label(id),()->navigate("contents:"+id));}
        String page=page();boolean editing=page.equals("new")||page.startsWith("rename:")||page.startsWith("dissolve:");search.setEnabled(!editing);commands.getChildAt(1).setVisibility(editing?GONE:VISIBLE);
        if(page.equals("new")||page.startsWith("rename:")){editor(page);return;}
        if(page.startsWith("dissolve:")){dissolve(page.substring(9));return;}
        if(page.startsWith("add:")){picker(page.substring(4));return;}
        String folder=folderId(page);address.setText(page.equals("quick")?"Quick Launch":folder==null?"Desktop shortcuts":host.label(folder));
        if(folder!=null&&!store.isFolder(folder)){LinearLayout column=column();heading(column,"Folder removed","Its shortcuts are available on the desktop.");link(column,"Desktop shortcuts",()->navigate("desktop"));body(scrolling(column));return;}
        List<String> all=members(page),shown=new ArrayList<>();for(String id:all)if(host.label(id).toLowerCase(Locale.getDefault()).contains(query()))shown.add(id);
        LinearLayout column=column();heading(column,page.equals("quick")?"Customize Quick Launch":folder==null?"Arrange your desktop":host.label(folder),folder!=null?"Open a shortcut, or use its menu to rename, reorder or move it.":page.equals("quick")?"Choose up to 12 shortcuts for the taskbar. Hold a taskbar icon for its options.":"Drop a shortcut on the center of another to create a folder. Drop near its edge to swap positions.");
        if(folder!=null){link(column,"Rename folder",()->rename(folder));link(column,"Dissolve folder…",()->navigate("dissolve:"+folder));}
        ListView list=list(column);list.setAdapter(new BaseAdapter(){public int getCount(){return shown.size();}public Object getItem(int i){return shown.get(i);}public long getItemId(int i){return i;}public View getView(int i,View recycled,ViewGroup parent){String id=shown.get(i);LinearLayout row=row(id);TextView more=Aero.text(getContext(),"⋮",23,Aero.INK);more.setGravity(Gravity.CENTER);more.setContentDescription("Options for "+host.label(id));more.setOnClickListener(v->options(v,id,page));row.addView(more,new LayoutParams(dp(42),dp(52)));return row;}});
        list.setOnItemClickListener((parent,view,position,id)->{int at=position-list.getHeaderViewsCount();if(at>=0&&at<shown.size())host.open(shown.get(at));});
        list.setOnItemLongClickListener((parent,view,position,id)->{int at=position-list.getHeaderViewsCount();if(at<0||at>=shown.size())return false;options(view,shown.get(at),page);return true;});
        status.setText(all.size()+" shortcut"+(all.size()==1?"":"s")+(page.equals("quick")?"  •  maximum 12":""));body(list);
    }
    private void picker(String target){
        String folder=folderId(target);if(folder!=null&&!store.isFolder(folder)){navigate("desktop");return;}address.setText("Add programs  ›  "+(folder!=null?host.label(folder):target.equals("quick")?"Quick Launch":"Desktop"));
        List<String> available=new ArrayList<>(Arrays.asList("@computer","@browser","@files","@notes","@appearance","@control","@gadgets","@calendar","@sticky","@filemanagers","@backup"));
        for(AppCatalog.Entry app:apps.get())if(!available.contains(app.id))available.add(app.id);
        if(folder==null){Iterator<String> keys=store.folders().keys();while(keys.hasNext())available.add(keys.next());}
        available.sort((a,b)->java.text.Collator.getInstance().compare(host.label(a),host.label(b)));available.removeIf(id->!host.label(id).toLowerCase(Locale.getDefault()).contains(query()));
        LinearLayout column=column();heading(column,"Choose shortcuts","Checked items appear in "+(folder!=null?host.label(folder):target.equals("quick")?"Quick Launch":"your desktop")+".");link(column,"Done",()->navigate(target));ListView list=list(column);List<String> chosen=members(target);
        list.setAdapter(new BaseAdapter(){public int getCount(){return available.size();}public Object getItem(int i){return available.get(i);}public long getItemId(int i){return i;}public View getView(int i,View recycled,ViewGroup parent){String id=available.get(i);LinearLayout row=row(id);CheckBox box=new CheckBox(getContext());box.setFocusable(false);box.setChecked(chosen.contains(id));box.setContentDescription("Include "+host.label(id));box.setOnCheckedChangeListener((v,checked)->select(target,id,checked));row.addView(box,new LayoutParams(dp(48),dp(52)));return row;}});
        list.setOnItemClickListener((parent,view,position,id)->{int at=position-list.getHeaderViewsCount();if(at>=0&&at<available.size()){String item=available.get(at);select(target,item,!members(target).contains(item));}});status.setText(chosen.size()+" selected");body(list);
    }
    private void select(String target,String id,boolean include){List<String> current=members(target);if(include==current.contains(id))return;if(include){int max=target.equals("quick")?DesktopStore.MAX_QUICK:target.startsWith("contents:")?DesktopStore.MAX_ITEMS:512;if(current.size()>=max){Toast.makeText(getContext(),"This list holds up to "+max+" shortcuts",Toast.LENGTH_SHORT).show();refreshApps();return;}current.add(id);}else current.remove(id);members(target,current);host.changed();}
    private ListView list(LinearLayout header){ListView list=new ListView(getContext());list.setDividerHeight(0);list.setSelector(Aero.selection(getContext()));list.setContentDescription("Organized shortcuts");list.addHeaderView(header,null,false);currentList=list;return list;}
    private LinearLayout row(String id){LinearLayout row=new LinearLayout(getContext());row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(10),dp(3),dp(4),dp(3));row.addView(host.icon(id,30),new LayoutParams(dp(30),dp(30)));LinearLayout text=Aero.column(getContext());text.setPadding(dp(11),dp(6),0,dp(6));TextView name=Aero.text(getContext(),host.label(id),13,Aero.INK);name.setMaxLines(2);text.addView(name);if(store.isFolder(id))text.addView(Aero.text(getContext(),store.folderItems(id).size()+" shortcuts",10,0xff687c8c));row.addView(text,new LayoutParams(0,dp(52),1));row.setContentDescription("Shortcut: "+host.label(id));return row;}
    private void options(View anchor,String id,String target){
        PopupMenu menu=new PopupMenu(getContext(),anchor);menu.getMenu().add("Open").setOnMenuItemClickListener(v->{host.open(id);return true;});menu.getMenu().add("Rename").setOnMenuItemClickListener(v->{rename(id);return true;});
        List<String> current=members(target);int position=current.indexOf(id);
        menu.getMenu().add("Move up").setEnabled(position>0).setOnMenuItemClickListener(v->{shift(target,id,-1);return true;});menu.getMenu().add("Move down").setEnabled(position>=0&&position<current.size()-1).setOnMenuItemClickListener(v->{shift(target,id,1);return true;});
        if(target.startsWith("contents:"))menu.getMenu().add("Move to desktop").setOnMenuItemClickListener(v->{List<String> desktop=store.desktop();if(!desktop.contains(id))desktop.add(id);store.desktop(desktop);List<String> items=members(target);items.remove(id);members(target,items);host.changed();return true;});
        if(!target.equals("quick"))menu.getMenu().add("Add to Quick Launch").setOnMenuItemClickListener(v->{select("quick",id,true);return true;});
        menu.getMenu().add(target.equals("quick")?"Remove from Quick Launch":target.equals("desktop")?"Remove from desktop":"Remove from folder").setOnMenuItemClickListener(v->{select(target,id,false);return true;});
        if(store.isFolder(id))menu.getMenu().add("Dissolve folder…").setOnMenuItemClickListener(v->{navigate("dissolve:"+id);return true;});else menu.getMenu().add("Restore original name").setOnMenuItemClickListener(v->{store.originalName(id);host.changed();return true;});menu.show();
    }
    private void shift(String target,String id,int direction){List<String> current=members(target);store.shift(current,id,direction);members(target,current);host.changed();}
    void rename(String id){draft=null;navigate("rename:"+id);}
    private void editor(String page){
        boolean creating=page.equals("new");String id=creating?null:page.substring(7);LinearLayout column=column();address.setText(creating?"New folder":"Rename shortcut");heading(column,creating?"Create a desktop folder":"Choose a name",creating?"Keep related apps together in a folder.":"The new name is used throughout your launcher.");
        nameInput=new EditText(getContext());nameInput.setTypeface(Aero.face(getContext()));nameInput.setSingleLine(true);nameInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(40)});nameInput.setContentDescription(creating||store.isFolder(id)?"Folder name":"Shortcut name");nameInput.setText(draft!=null?draft:creating?"New folder":host.label(id));nameInput.setSelectAllOnFocus(true);column.addView(nameInput,new LayoutParams(-1,dp(48)));nameInput.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){}public void onTextChanged(CharSequence s,int a,int b,int c){draft=s.toString();}public void afterTextChanged(Editable e){}});
        TextView save=Aero.button(getContext(),creating?"Create folder":"Save name",()->{String name=DesktopStore.cleanName(nameInput.getText().toString());if(name.isEmpty()){nameInput.setError("Enter a name");return;}try{String result=id;if(creating)result=store.createFolder(name,Collections.emptyList());else store.rename(id,name);draft=null;((android.view.inputmethod.InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(nameInput.getWindowToken(),0);host.changed();navigate(creating||store.isFolder(id)?"contents:"+result:"desktop");}catch(IllegalArgumentException error){nameInput.setError(error.getMessage());}});save.setContentDescription(creating?"Create folder":"Save shortcut name");column.addView(save,new LayoutParams(-1,dp(44)));status.setText("Names can contain up to 40 characters");body(scrolling(column));
    }
    private void dissolve(String id){LinearLayout column=column();address.setText("Dissolve folder");heading(column,"Dissolve "+host.label(id)+"?","Its "+store.folderItems(id).size()+" shortcuts will return to the desktop. Your installed apps are kept.");TextView confirm=Aero.button(getContext(),"Move shortcuts to desktop",()->{store.dissolve(id);host.changed();navigate("desktop");});confirm.setContentDescription("Confirm dissolve folder");column.addView(confirm,new LayoutParams(-1,dp(46)));link(column,"Keep folder",()->navigate("contents:"+id));status.setText("Review before changing the folder");body(scrolling(column));}
    @Override void refreshApps(){int position=currentList==null?0:currentList.getFirstVisiblePosition(),offset=currentList==null||currentList.getChildCount()==0?0:currentList.getChildAt(0).getTop();render();if(currentList!=null)currentList.setSelectionFromTop(position,offset);}
    @Override void showFolders(){PopupMenu menu=new PopupMenu(getContext(),folders);for(String page:Arrays.asList("desktop","quick"))menu.getMenu().add(page.equals("quick")?"Quick Launch":"Desktop shortcuts").setOnMenuItemClickListener(v->{navigate(page);return true;});Iterator<String> keys=store.folders().keys();while(keys.hasNext()){String id=keys.next();menu.getMenu().add(host.label(id)).setOnMenuItemClickListener(v->{navigate("contents:"+id);return true;});}menu.show();}
    @Override void goUp(){navigate("desktop");}
    @Override JSONObject extraState(){JSONObject saved=new JSONObject();if(draft!=null)GadgetLayout.put(saved,"nameDraft",draft);return saved;}
    @Override void restoreExtra(JSONObject saved){draft=saved!=null&&saved.has("nameDraft")?saved.optString("nameDraft"):null;}
}
