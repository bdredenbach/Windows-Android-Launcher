package com.nthexperience.desktop;

import android.content.Context;
import android.graphics.Color;
import android.text.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;

/** Shared Vista navigation chrome, with touch-sized controls and compact-phone navigation. */
abstract class VistaShell extends LinearLayout {
    interface Actions {void open(String id);void action(String id);}
    final Actions actions;
    final DesktopStore store;
    final LinearLayout commands,side;
    final FrameLayout content;
    final EditText search;
    private final ScrollView sideScroll;
    final TextView address,status,back,forward,up,folders;
    private final ArrayList<String> history=new ArrayList<>();
    private int historyIndex=-1;
    private boolean restoring,ready,compact;
    private int pendingPosition=-1,pendingOffset;
    private boolean restoreFocus;
    VistaShell(Context c,DesktopStore store,Actions actions,String hint){
        super(c);this.store=store;this.actions=actions;setOrientation(VERTICAL);setBackgroundColor(0xfff7f9fb);setFocusableInTouchMode(true);
        LinearLayout location=new LinearLayout(c);location.setGravity(Gravity.CENTER_VERTICAL);location.setPadding(dp(4),dp(3),dp(4),dp(3));location.setBackground(Aero.panel(c,0xfff0f7fc,0xffc9dae8,0,0xff9baebe));
        back=navButton("‹","Back",()->goBack());forward=navButton("›","Forward",()->goForward());up=navButton("↑","Up one level",this::goUp);location.addView(back);location.addView(forward);location.addView(up);
        address=Aero.text(c,"",12,0xff203e54);address.setSingleLine(true);address.setEllipsize(TextUtils.TruncateAt.MIDDLE);address.setPadding(dp(8),0,dp(4),0);address.setBackground(Aero.panel(c,0xffffffff,0xfff3f9fd,1,0xff8ea6b8));address.setContentDescription("Address bar");location.addView(address,new LayoutParams(0,dp(32),1));addView(location,new LayoutParams(-1,dp(40)));
        LinearLayout toolbar=new LinearLayout(c);toolbar.setGravity(Gravity.CENTER_VERTICAL);toolbar.setPadding(dp(4),0,dp(5),0);toolbar.setBackground(Aero.panel(c,0xfffdfefe,0xffe5edf4,0,0xffb5c4d0));
        folders=command("Folders",this::showFolders);folders.setContentDescription("Navigation folders");toolbar.addView(folders,new LayoutParams(-2,dp(36)));
        commands=new LinearLayout(c);commands.setGravity(Gravity.CENTER_VERTICAL);toolbar.addView(commands,new LayoutParams(-2,dp(36)));
        search=new EditText(c);search.setTypeface(Aero.face(c));search.setSingleLine(true);search.setTextSize(12);search.setTextColor(Aero.INK);search.setHintTextColor(0xff70818e);search.setHint(hint);search.setContentDescription(hint);search.setPadding(dp(7),0,dp(5),0);search.setBackground(Aero.panel(c,0xffffffff,0xfffafcfd,1,0xff9aafbf));LayoutParams query=new LayoutParams(0,dp(30),1);query.setMargins(dp(5),0,0,0);toolbar.addView(search,query);addView(toolbar,new LayoutParams(-1,dp(38)));
        LinearLayout area=new LinearLayout(c);side=new LinearLayout(c);side.setOrientation(VERTICAL);side.setPadding(dp(10),dp(12),dp(8),dp(8));side.setBackground(Aero.panel(c,0xffeff5fa,0xffdbe7f1,0,0xffbacbd8));sideScroll=new ScrollView(c);sideScroll.setFillViewport(true);sideScroll.setVerticalScrollBarEnabled(false);sideScroll.addView(side);area.addView(sideScroll,new LayoutParams(dp(136),-1));content=new FrameLayout(c);content.setBackgroundColor(Color.WHITE);area.addView(content,new LayoutParams(0,-1,1));addView(area,new LayoutParams(-1,0,1));
        status=Aero.text(c,"",11,0xff425d71);status.setPadding(dp(9),0,dp(6),0);status.setSingleLine(true);status.setEllipsize(TextUtils.TruncateAt.END);status.setBackground(Aero.panel(c,0xffedf4fa,0xffdce8f2,0,0xffb1c3d1));addView(status,new LayoutParams(-1,dp(28)));
        addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{status.setVisibility(b-t<dp(250)?GONE:VISIBLE);boolean next=r-l<dp(490);if(!ready||next!=compact){compact=next;sideScroll.setVisibility(compact?GONE:VISIBLE);folders.setVisibility(compact?VISIBLE:GONE);if(ready)render();}});
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int n,int f){}public void afterTextChanged(Editable e){}public void onTextChanged(CharSequence s,int start,int before,int count){if(ready&&!restoring)render();}});
    }
    final void start(String page){history.add(page);historyIndex=0;ready=true;render();updateNavigation();}
    final String page(){return historyIndex>=0?history.get(historyIndex):"home";}
    final String query(){return search.getText().toString().trim().toLowerCase(Locale.getDefault());}
    final boolean compact(){return compact;}
    final void navigate(String page){if(page.equals(page())&&query().isEmpty())return;while(history.size()>historyIndex+1)history.remove(history.size()-1);history.add(page);if(history.size()>16)history.remove(0);historyIndex=history.size()-1;resetSearch();pendingPosition=-1;render();updateNavigation();}
    final boolean goBack(){if(historyIndex<=0)return false;historyIndex--;resetSearch();render();updateNavigation();return true;}
    final java.util.List<String> historyPages(){return new java.util.ArrayList<>(history);}
    final void visitHistory(int index){if(index<0||index>=history.size())return;historyIndex=index;resetSearch();render();updateNavigation();}
    private void goForward(){if(historyIndex+1>=history.size())return;historyIndex++;resetSearch();render();updateNavigation();}
    private void resetSearch(){restoring=true;search.setText("");search.clearFocus();requestFocus();restoring=false;}
    final void updateNavigation(){back.setEnabled(historyIndex>0);back.setAlpha(historyIndex>0?1:.35f);forward.setEnabled(historyIndex+1<history.size());forward.setAlpha(historyIndex+1<history.size()?1:.35f);}
    abstract void render();
    abstract void showFolders();
    abstract void goUp();
    void refreshApps(){}
    void close(){}
    JSONObject extraState(){return new JSONObject();}
    void restoreExtra(JSONObject value){}
    final JSONObject state(){
        JSONObject result=new JSONObject();JSONArray saved=new JSONArray();int size=0,start=0;for(int i=historyIndex;i>=0;i--){size+=history.get(i).length();if(size>24000){start=i+1;break;}}
        for(int i=start;i<history.size()&&size<48000;i++){saved.put(history.get(i));if(i>historyIndex)size+=history.get(i).length();}
        GadgetLayout.put(result,"history",saved);GadgetLayout.put(result,"index",Math.max(0,historyIndex-start));GadgetLayout.put(result,"query",search.getText().toString());GadgetLayout.put(result,"cursor",search.getSelectionStart());GadgetLayout.put(result,"focus",search.hasFocus());GadgetLayout.put(result,"extra",extraState());
        if(content.getChildCount()>0){View view=content.getChildAt(0);if(view instanceof ListView){ListView list=(ListView)view;GadgetLayout.put(result,"position",list.getFirstVisiblePosition());GadgetLayout.put(result,"offset",list.getChildCount()>0?list.getChildAt(0).getTop():0);}else if(view instanceof ScrollView){GadgetLayout.put(result,"position",0);GadgetLayout.put(result,"offset",view.getScrollY());}}
        return result;
    }
    final void restore(JSONObject value){
        if(value==null)return;restoring=true;JSONArray saved=value.optJSONArray("history");if(saved!=null&&saved.length()>0){history.clear();for(int i=0;i<Math.min(16,saved.length());i++)history.add(saved.optString(i,"home"));historyIndex=Math.max(0,Math.min(history.size()-1,value.optInt("index")));}
        restoreExtra(value.optJSONObject("extra"));search.setText(value.optString("query"));search.setSelection(Math.max(0,Math.min(search.length(),value.optInt("cursor"))));restoreFocus=value.optBoolean("focus");pendingPosition=value.optInt("position",0);pendingOffset=value.optInt("offset");restoring=false;render();updateNavigation();if(restoreFocus)search.requestFocus();
    }
    final void body(View view){content.removeAllViews();content.addView(view,new FrameLayout.LayoutParams(-1,-1));if(pendingPosition>=0){final int position=pendingPosition,offset=pendingOffset;view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener(){public void onGlobalLayout(){view.getViewTreeObserver().removeOnGlobalLayoutListener(this);if(view instanceof ListView)((ListView)view).setSelectionFromTop(position,offset);else view.scrollTo(0,offset);}});pendingPosition=-1;}}
    final void loading(View view){content.removeAllViews();content.addView(view,new FrameLayout.LayoutParams(-1,-1));}
    final ScrollView scrolling(LinearLayout column){ScrollView scroll=new ScrollView(getContext());scroll.setFillViewport(true);scroll.addView(column,new ScrollView.LayoutParams(-1,-2));return scroll;}
    final LinearLayout column(){LinearLayout value=Aero.column(getContext());value.setPadding(dp(14),dp(10),dp(14),dp(16));return value;}
    final void heading(LinearLayout parent,String title,String description){TextView h=Aero.text(getContext(),title,18,0xff326421);h.setPadding(0,dp(3),0,dp(5));parent.addView(h);if(!description.isEmpty()){TextView sub=Aero.text(getContext(),description,12,0xff586976);sub.setPadding(0,0,0,dp(13));parent.addView(sub);}}
    final TextView link(LinearLayout parent,String title,Runnable action){TextView v=Aero.text(getContext(),title,13,0xff075cac);v.setMinHeight(dp(38));v.setBackground(Aero.selection(getContext()));v.setOnClickListener(w->action.run());parent.addView(v,new LayoutParams(-1,-2));return v;}
    final void task(LinearLayout parent,String icon,String title,String description,Runnable action){
        LinearLayout row=new LinearLayout(getContext());row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(5),dp(8),dp(5),dp(8));row.setBackground(Aero.selection(getContext()));AeroIcon image=new AeroIcon(getContext(),icon);row.addView(image,new LayoutParams(dp(42),dp(42)));LinearLayout text=Aero.column(getContext());text.setPadding(dp(11),0,0,0);TextView name=Aero.text(getContext(),title,14,0xff075cac);text.addView(name);if(!description.isEmpty()){TextView sub=Aero.text(getContext(),description,11,0xff596c79);sub.setPadding(0,dp(3),0,0);text.addView(sub);}row.addView(text,new LayoutParams(0,-2,1));row.setContentDescription(title);row.setFocusable(true);row.setOnClickListener(v->action.run());parent.addView(row,new LayoutParams(-1,-2));
    }
    final void sideLink(String text,Runnable action){TextView v=Aero.text(getContext(),text,12,0xff145e99);v.setPadding(0,dp(7),0,dp(7));v.setBackground(Aero.selection(getContext()));v.setOnClickListener(w->action.run());side.addView(v,new LayoutParams(-1,-2));}
    final TextView command(String text,Runnable action){TextView v=Aero.text(getContext(),text,11,0xff1e4059);v.setGravity(Gravity.CENTER);v.setPadding(dp(6),0,dp(6),0);v.setBackground(Aero.selection(getContext()));v.setOnClickListener(w->action.run());return v;}
    final int dp(float value){return Aero.dp(getContext(),value);}
    private TextView navButton(String text,String description,Runnable action){TextView button=command(text,action);button.setTextSize(24);button.setContentDescription(description);button.setLayoutParams(new LayoutParams(dp(32),dp(34)));return button;}
}
