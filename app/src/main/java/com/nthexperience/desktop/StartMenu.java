package com.nthexperience.desktop;

import android.content.Context;
import android.graphics.Color;
import android.text.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.util.*;

/** Vista's white program pane, dark navigation pane and bottom Start Search field. */
final class StartMenu extends LinearLayout {
    interface Actions{void open(String id);void options(View anchor,String id);void home();void settings();boolean isHome();}
    private final EditText search;
    private final ListView list;
    private final Actions actions;
    private final DesktopStore store;
    private final LinearLayout profile,navigation;
    private final TextView programs,home;
    private final List<AppCatalog.Entry> all=new ArrayList<>();
    private final List<StartSearch.Result> filtered=new ArrayList<>();
    private final ListAdapter adapter=new ListAdapter();
    private boolean allPrograms;
    private View heldOption;
    private int pendingListPosition=-1;
    StartMenu(Context c,Actions actions,DesktopStore store,AeroWallpaper wallpaper){
        super(c);this.actions=actions;this.store=store;setOrientation(HORIZONTAL);setElevation(Aero.dp(c,22));setPadding(Aero.dp(c,7),Aero.dp(c,7),Aero.dp(c,6),Aero.dp(c,7));setBackground(new AeroGlass(wallpaper,AeroGlass.START));
        LinearLayout left=Aero.column(c);left.setBackground(Aero.panel(c,0xfffdfefe,0xfff6f9fb,4,0xff8b9eaa));
        FrameLayout content=new FrameLayout(c);list=new ListView(c);list.setDividerHeight(0);list.setSelector(Aero.selection(c));list.setAdapter(adapter);list.setContentDescription("Installed programs. Hold an app for desktop options.");
        list.setOnItemClickListener((parent,view,position,id)->actions.open(filtered.get(position).id));list.setOnItemLongClickListener((parent,view,position,id)->{if(!filtered.get(position).shortcut)return false;actions.options(view,filtered.get(position).id);return true;});content.addView(list,new FrameLayout.LayoutParams(-1,-1));
        TextView empty=Aero.text(c,"No matching results",12,0xff4b6170);empty.setGravity(Gravity.CENTER);empty.setVisibility(GONE);content.addView(empty,new FrameLayout.LayoutParams(-1,-1));list.setEmptyView(empty);left.addView(content,new LayoutParams(-1,0,1));
        programs=Aero.text(c,"All Programs  ›",12,Aero.INK);programs.setPadding(Aero.dp(c,10),0,Aero.dp(c,6),0);programs.setBackground(Aero.selection(c));programs.setOnClickListener(v->{allPrograms=!allPrograms;programs.setText(allPrograms?"‹  Back":"All Programs  ›");filter();});left.addView(programs,new LayoutParams(-1,Aero.dp(c,38)));
        FrameLayout searchBox=new FrameLayout(c);searchBox.setPadding(Aero.dp(c,6),Aero.dp(c,4),Aero.dp(c,6),Aero.dp(c,5));searchBox.setBackgroundColor(0xffe5eaf0);
        search=new EditText(c);search.setTypeface(Aero.face(c));search.setSingleLine(true);search.setTextSize(12);search.setTextColor(Aero.INK);search.setHintTextColor(0xff657480);search.setHint("Start search");search.setContentDescription("Search apps, settings, notes and linked files");search.setPadding(Aero.dp(c,7),0,Aero.dp(c,22),0);search.setBackground(Aero.panel(c,0xffffffff,0xfff5f9fc,1,0xff728798));searchBox.addView(search,new FrameLayout.LayoutParams(-1,-1));left.addView(searchBox,new LayoutParams(-1,Aero.dp(c,44)));
        addView(left,new LayoutParams(0,-1,.61f));
        LinearLayout right=Aero.column(c);right.setPadding(Aero.dp(c,7),0,0,0);addView(right,new LayoutParams(0,-1,.39f));
        profile=Aero.column(c);profile.setGravity(Gravity.CENTER);View avatar=ProfilePortrait.view(c,store);avatar.setContentDescription("Change profile picture");avatar.setFocusable(true);avatar.setOnClickListener(v->actions.open("setting:page:accounts"));profile.addView(avatar,new LayoutParams(Aero.dp(c,49),Aero.dp(c,49)));TextView name=Aero.text(c,store.prefs.getString("desktop_name","Nth Desktop"),13,Color.WHITE);name.setGravity(Gravity.CENTER);name.setShadowLayer(2,0,1,0xff000000);profile.addView(name,new LayoutParams(-1,Aero.dp(c,27)));right.addView(profile,new LayoutParams(-1,Aero.dp(c,86)));
        navigation=Aero.column(c);String[][] items={{"Documents","@documents"},{"Pictures","@pictures"},{"Music","@music"},{"Computer","@computer"},{"Control Panel","@control"},{"Gadgets","@gadgets"},{"Welcome Center","@welcome"}};
        for(String[] item:items){TextView nav=Aero.text(c,item[0],12,Color.WHITE);nav.setSingleLine(true);nav.setEllipsize(TextUtils.TruncateAt.END);nav.setPadding(Aero.dp(c,7),0,0,0);nav.setShadowLayer(2,0,1,0xff000000);nav.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x5571b6dc),Aero.panel(c,0x00ffffff,0x00ffffff,3,0),null));nav.setOnClickListener(v->actions.open(item[1]));nav.setContentDescription(item[0]);hoverOption(nav,true);navigation.addView(nav,new LayoutParams(-1,Aero.dp(c,43)));}
        ScrollView navScroll=new ScrollView(c);navScroll.setVerticalScrollBarEnabled(false);navScroll.addView(navigation);right.addView(navScroll,new LayoutParams(-1,0,1));
        home=Aero.text(c,"⌂  Make home",11,Color.WHITE);home.setGravity(Gravity.CENTER);home.setBackground(Aero.gloss(c,new int[]{0xff5a735c,0xff37543d,0xff293f30,0xff3f5b45},3,0xff9cae9a));home.setOnClickListener(v->actions.home());right.addView(home,new LayoutParams(-1,Aero.dp(c,35)));
        hoverOption(programs,false);hoverOption(home,true);
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int n,int f){}public void onTextChanged(CharSequence s,int start,int before,int n){filter();}public void afterTextChanged(Editable e){}});
    }
    private void hoverOption(View view,boolean dark){
        android.graphics.drawable.Drawable normal=view.getBackground();android.graphics.drawable.StateListDrawable states=new android.graphics.drawable.StateListDrawable();
        for(int state:new int[]{android.R.attr.state_pressed,android.R.attr.state_hovered,android.R.attr.state_focused,android.R.attr.state_selected})states.addState(new int[]{state},Aero.gloss(getContext(),dark?new int[]{0xddbed9e8,0xcc76a3bf,0xcc446c88,0xcc80a3bb}:new int[]{0xfff5fbff,0xffe4f1fc,0xffcce5f7,0xffe7f4fd},3,dark?0xffb4cad7:0xff80aed0));
        states.addState(new int[]{},normal==null?Aero.panel(getContext(),0x00ffffff,0x00ffffff,3,0):normal);view.setBackground(states);view.setFocusable(true);
        view.setOnLongClickListener(v->{clearHeldOption();heldOption=v;v.setSelected(true);v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);return true;});
        view.setOnTouchListener((v,e)->{if(e.getActionMasked()==MotionEvent.ACTION_DOWN||e.getActionMasked()==MotionEvent.ACTION_CANCEL)clearHeldOption();return false;});
        view.setOnHoverListener((v,e)->{if(e.getActionMasked()==MotionEvent.ACTION_HOVER_ENTER)clearHeldOption();return false;});view.setOnFocusChangeListener((v,focused)->{if(focused)clearHeldOption();});
    }
    private void clearHeldOption(){if(heldOption!=null){heldOption.setSelected(false);heldOption=null;}}
    void availableHeight(int height){boolean compact=height<Aero.dp(getContext(),420);profile.setVisibility(compact?GONE:VISIBLE);}
    void entries(List<AppCatalog.Entry> values){all.clear();all.addAll(values);filter();if(pendingListPosition>=0&&!values.isEmpty()){list.setSelection(pendingListPosition);pendingListPosition=-1;}}
    void refreshName(){((TextView)profile.getChildAt(1)).setText(store.prefs.getString("desktop_name","Nth Desktop"));View avatar=ProfilePortrait.view(getContext(),store);avatar.setContentDescription("Change profile picture");avatar.setFocusable(true);avatar.setOnClickListener(v->actions.open("setting:page:accounts"));profile.removeViewAt(0);profile.addView(avatar,0,new LayoutParams(Aero.dp(getContext(),49),Aero.dp(getContext(),49)));}
    void refreshHome(){home.setVisibility(actions.isHome()?GONE:VISIBLE);}
    void show(boolean keyboard){clearHeldOption();refreshHome();refreshName();setVisibility(VISIBLE);bringToFront();allPrograms=false;programs.setText("All Programs  ›");search.setText("");filter();if(Aero.motion(getContext())){setAlpha(0);animate().alpha(1).setDuration(120).start();}else setAlpha(1);if(keyboard){search.requestFocus();search.post(()->((InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(search,InputMethodManager.SHOW_IMPLICIT));}else{search.clearFocus();requestFocus();}}
    void hide(){clearHeldOption();animate().cancel();((InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(getWindowToken(),0);search.clearFocus();setVisibility(GONE);}
    org.json.JSONObject state(){org.json.JSONObject result=new org.json.JSONObject();GadgetLayout.put(result,"visible",getVisibility()==VISIBLE);GadgetLayout.put(result,"all",allPrograms);GadgetLayout.put(result,"position",list.getFirstVisiblePosition());GadgetLayout.put(result,"ui",ToolViewState.capture(this));return result;}
    void restore(org.json.JSONObject state){if(state==null||!state.optBoolean("visible"))return;refreshHome();setVisibility(VISIBLE);setAlpha(1);bringToFront();allPrograms=state.optBoolean("all");programs.setText(allPrograms?"‹  Back":"All Programs  ›");ToolViewState.restore(this,state.optJSONObject("ui"));filter();pendingListPosition=state.optInt("position");list.setSelection(pendingListPosition);}
    private void filter(){
        String query=search.getText().toString().trim();filtered.clear();
        if(!query.isEmpty()||allPrograms)filtered.addAll(StartSearch.find(store,all,query));
        else{List<AppCatalog.Entry> choices=new ArrayList<>();for(String id:store.recents())for(AppCatalog.Entry app:all)if(app.id.equals(id)&&!choices.contains(app))choices.add(app);for(AppCatalog.Entry app:all)if(!choices.contains(app)&&choices.size()<8)choices.add(app);for(AppCatalog.Entry app:choices)filtered.add(new StartSearch.Result(app.id,store.name(app.id,app.label),"Programs","programs",app.icon,true));}
        adapter.notifyDataSetChanged();
    }
    private final class ListAdapter extends BaseAdapter{
        public int getCount(){return filtered.size();}public Object getItem(int p){return filtered.get(p);}public long getItemId(int p){return p;}
        public View getView(int pos,View recycled,android.view.ViewGroup parent){
            LinearLayout row;if(recycled instanceof LinearLayout)row=(LinearLayout)recycled;else{row=new LinearLayout(getContext());row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(Aero.dp(getContext(),7),Aero.dp(getContext(),3),Aero.dp(getContext(),4),Aero.dp(getContext(),3));ImageView icon=new ImageView(getContext());icon.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);row.addView(icon,new LayoutParams(Aero.dp(getContext(),30),Aero.dp(getContext(),30)));TextView name=Aero.text(getContext(),"",12,Aero.INK);name.setMaxLines(3);name.setEllipsize(TextUtils.TruncateAt.END);name.setPadding(Aero.dp(getContext(),8),0,0,0);row.addView(name,new LayoutParams(0,Aero.dp(getContext(),50),1));}
            StartSearch.Result e=filtered.get(pos);ImageView image=(ImageView)row.getChildAt(0);if(e.icon!=null)image.setImageDrawable(e.icon);else image.setImageDrawable(null);image.setVisibility(e.icon==null?GONE:VISIBLE);TextView name=(TextView)row.getChildAt(1);name.setText(e.title+(search.getText().toString().trim().isEmpty()?"":"\n"+e.group));row.setContentDescription("Open "+e.group+": "+e.title);return row;
        }
    }
}
