package com.nthexperience.desktop;

import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class StartMenu extends LinearLayout {
    interface Actions {
        void open(String id);
        void options(View anchor,String id);
        void home();
        void settings();
    }
    private final EditText search;
    private final TextView count;
    private final ListView list;
    private final LinearLayout profile,shortcuts,footer;
    private final Actions actions;
    private final List<AppCatalog.Entry> all=new ArrayList<>(), filtered=new ArrayList<>();
    private final ListAdapter adapter=new ListAdapter();
    StartMenu(Context c,Actions actions){
        super(c);this.actions=actions;
        setOrientation(VERTICAL);setElevation(Aero.dp(c,24));
        setBackground(Aero.panel(c,0xf043748e,0xf010293e,9,0x99d9f6ff));Aero.pad(this,8);
        profile=new LinearLayout(c);profile.setGravity(Gravity.CENTER_VERTICAL);Aero.pad(profile,8);
        AeroIcon avatar=new AeroIcon(c,"orb");profile.addView(avatar,new LayoutParams(Aero.dp(c,44),Aero.dp(c,44)));
        LinearLayout words=Aero.column(c); words.setPadding(Aero.dp(c,12),0,0,0);
        TextView heading=Aero.text(c,"Nth Desktop",19,Color.WHITE);Aero.bold(heading);words.addView(heading);
        words.addView(Aero.text(c,"A little nostalgia. Your Android.",11,0xffd4e6ee));profile.addView(words);addView(profile);
        search=new EditText(c);search.setSingleLine(true);search.setTextSize(15);search.setTextColor(Aero.INK);search.setHintTextColor(0xff647b88);
        search.setHint("Search programs"); search.setContentDescription("Search installed apps");
        search.setPadding(Aero.dp(c,12),0,Aero.dp(c,12),0);search.setBackground(Aero.panel(c,0xffffffff,0xffedf5f8,4,0xffa3c5d4));
        addView(search,new LayoutParams(-1,Aero.dp(c,48)));
        shortcuts=new LinearLayout(c);shortcuts.setPadding(0,Aero.dp(c,8),0,Aero.dp(c,6));
        for(String[] item:new String[][]{{"Files","@files"},{"Notepad","@notes"},{"Personalize","@appearance"}}){
            TextView b=Aero.button(c,item[0],()->actions.open(item[1]));shortcuts.addView(b,new LayoutParams(0,Aero.dp(c,48),1));
        }
        addView(shortcuts);count=Aero.text(c,"Loading programs…",12,0xffd5e6ee);count.setPadding(Aero.dp(c,8),Aero.dp(c,4),0,Aero.dp(c,7));addView(count);
        list=new ListView(c);list.setBackground(Aero.panel(c,0xfffbfdff,0xffe8f0f6,4,0));
        list.setDividerHeight(0);list.setAdapter(adapter);list.setClipToPadding(false);list.setContentDescription("Installed programs");
        list.setOnItemClickListener((parent,view,position,id)->actions.open(filtered.get(position).id));
        list.setOnItemLongClickListener((parent,view,position,id)->{actions.options(view,filtered.get(position).id);return true;});
        FrameLayout programs=new FrameLayout(c);programs.addView(list,new FrameLayout.LayoutParams(-1,-1));
        TextView empty=Aero.text(c,"No matching programs",15,Aero.INK);empty.setGravity(Gravity.CENTER);empty.setVisibility(GONE);
        programs.addView(empty,new FrameLayout.LayoutParams(-1,-1));list.setEmptyView(empty);
        addView(programs,new LayoutParams(-1,0,1));
        footer=new LinearLayout(c);footer.setPadding(0,Aero.dp(c,8),0,0);
        footer.addView(Aero.button(c,"Make home",actions::home),new LayoutParams(0,Aero.dp(c,48),1));
        footer.addView(Aero.button(c,"Android settings",actions::settings),new LayoutParams(0,Aero.dp(c,48),1));addView(footer);
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int n,int f){} public void onTextChanged(CharSequence s,int start,int before,int n){filter();}public void afterTextChanged(Editable e){}});
    }
    void availableHeight(int height){
        boolean compact=height<Aero.dp(getContext(),420);
        profile.setVisibility(compact?GONE:VISIBLE);
        shortcuts.setVisibility(compact?GONE:VISIBLE);
        footer.setVisibility(height<Aero.dp(getContext(),280)?GONE:VISIBLE);
    }
    void entries(List<AppCatalog.Entry> values){all.clear();all.addAll(values);filter();}
    void show(boolean keyboard){
        setVisibility(VISIBLE);bringToFront();search.setText("");
        if(keyboard){search.requestFocus();search.post(()->((InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(search,InputMethodManager.SHOW_IMPLICIT));}
        else {search.clearFocus();requestFocus();}
    }
    void hide(){
        ((InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(getWindowToken(),0);
        search.clearFocus();setVisibility(GONE);
    }
    private void filter(){
        String query=search.getText().toString().trim().toLowerCase(Locale.getDefault());filtered.clear();
        for(AppCatalog.Entry entry:all)if(entry.label.toLowerCase(Locale.getDefault()).contains(query))filtered.add(entry);
        count.setText(filtered.size()+" programs  ·  Hold an app for desktop options");adapter.notifyDataSetChanged();
    }
    private final class ListAdapter extends BaseAdapter{
        public int getCount(){return filtered.size();}public Object getItem(int position){return filtered.get(position);}public long getItemId(int position){return position;}
        public View getView(int position,View recycled,android.view.ViewGroup parent){
            LinearLayout row;
            if(recycled instanceof LinearLayout){row=(LinearLayout)recycled;}
            else {row=new LinearLayout(getContext());row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(Aero.dp(getContext(),12),Aero.dp(getContext(),7),Aero.dp(getContext(),12),Aero.dp(getContext(),7));
                ImageView icon=new ImageView(getContext());icon.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);row.addView(icon,new LayoutParams(Aero.dp(getContext(),36),Aero.dp(getContext(),36)));
                TextView name=Aero.text(getContext(),"",15,Aero.INK);name.setPadding(Aero.dp(getContext(),12),0,0,0);row.addView(name,new LayoutParams(0,Aero.dp(getContext(),42),1));}
            AppCatalog.Entry entry=filtered.get(position);((ImageView)row.getChildAt(0)).setImageDrawable(entry.icon);((TextView)row.getChildAt(1)).setText(entry.label);return row;
        }
    }
}
