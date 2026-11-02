package com.nthexperience.desktop;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.view.*;
import android.widget.*;
import org.json.JSONObject;
import java.util.*;

/** Vista-style task controls for our windows; Android apps are observed session entries. */
final class TaskManagerPanel extends LinearLayout {
    static final class Task {
        final String id,title,status;final boolean external;
        Task(String id,String title,String status,boolean external){this.id=id;this.title=title;this.status=status;this.external=external;}
    }
    interface Host {List<Task> tasks();View icon(String id);void switchTo(String id);void end(String id);void appInfo(String id);void newTask();void close();}
    private final Host host;private final LinearLayout content,tabs;private final TextView status,selection,switchTo,end;
    private List<Task> tasks=new ArrayList<>();private String selected="",tab="Applications";private boolean active=true,paused;
    private DesktopContextMenu menu;private HistoryGraph cpuGraph,memoryGraph;private TextView cpuText,memoryText,detail;
    private long lastWall,lastCpu;private int samples;private final float[] cpuHistory=new float[60],memoryHistory=new float[60];
    private final Runnable tick=new Runnable(){public void run(){if(!sampling())return;refresh();postDelayed(this,2000);}};
    TaskManagerPanel(Context c,Host host){
        super(c);this.host=host;setOrientation(VERTICAL);setContentDescription("Task Manager content");
        LinearLayout bar=new LinearLayout(c);bar.setBackgroundColor(0xfff3f4f5);
        for(String label:new String[]{"File","View"}){TextView link=Aero.text(c,label,12,Aero.INK);link.setGravity(Gravity.CENTER);link.setFocusable(true);link.setBackground(Aero.selection(c));link.setOnClickListener(v->menu(link,label));bar.addView(link,new LayoutParams(Aero.dp(c,48),Aero.dp(c,30)));}addView(bar);
        tabs=new LinearLayout(c);tabs.setPadding(Aero.dp(c,7),Aero.dp(c,6),Aero.dp(c,7),0);addView(tabs);
        content=Aero.column(c);Aero.pad(content,8);addView(content,new LayoutParams(-1,0,1));
        selection=Aero.text(c,"Select a task",11,0xff466079);selection.setPadding(Aero.dp(c,9),0,Aero.dp(c,9),0);addView(selection,new LayoutParams(-1,Aero.dp(c,27)));
        LinearLayout buttons=new LinearLayout(c);buttons.setPadding(Aero.dp(c,7),0,Aero.dp(c,7),Aero.dp(c,7));
        TextView newTask=Aero.button(c,"New Task…",host::newTask);switchTo=Aero.button(c,"Switch To",this::switchSelected);end=Aero.button(c,"End Task",this::endSelected);
        for(TextView button:new TextView[]{newTask,switchTo,end}){button.setTextSize(12);button.setMinWidth(0);LayoutParams p=new LayoutParams(0,Aero.dp(c,36),1);p.setMargins(Aero.dp(c,2),0,Aero.dp(c,2),0);buttons.addView(button,p);}addView(buttons);
        status=Aero.text(c,"",10,0xff374d5e);status.setPadding(Aero.dp(c,7),0,Aero.dp(c,5),0);status.setBackground(Aero.panel(c,0xfff5f6f7,0xffe4e8ed,0,0xffb7c1ca));addView(status,new LayoutParams(-1,Aero.dp(c,26)));
        render();
    }
    private void menu(View anchor,String label){
        dismissMenu();menu=new DesktopContextMenu(getContext(),"Task Manager "+label+" menu");
        if(label.equals("File")){menu.item("New Task…","programs",host::newTask);menu.separator();menu.item("Exit Task Manager","close",host::close);}
        else{menu.item("Refresh Now","refresh",this::refresh);menu.checked("Pause updates",paused,()->{paused=!paused;sync();});}
        ContextMenus.centered(anchor,(x,y)->ContextMenus.show(menu,anchor,x,y));
    }
    private void render(){
        tabs.removeAllViews();for(String name:new String[]{"Applications","Performance"}){TextView button=Aero.text(getContext(),name,12,Aero.INK);button.setGravity(Gravity.CENTER);button.setFocusable(true);button.setBackground(Aero.panel(getContext(),name.equals(tab)?0xffffffff:0xffe3eaf1,name.equals(tab)?0xfffbfcfd:0xffd2dce6,2,0xffa5b4c2));button.setContentDescription("Task Manager "+name);button.setOnClickListener(v->{tab=name;render();});tabs.addView(button,new LayoutParams(0,Aero.dp(getContext(),33),1));}
        content.removeAllViews();if(tab.equals("Applications"))renderTasks();else renderPerformance();updateSelection();
    }
    private void renderTasks(){
        tasks=host.tasks();if(tasks.stream().noneMatch(t->t.id.equals(selected)))selected="";
        LinearLayout header=new LinearLayout(getContext());header.setBackgroundColor(0xffedf1f5);header.addView(text("Task",12),new LayoutParams(0,Aero.dp(getContext(),28),1));header.addView(text("Status",12),new LayoutParams(Aero.dp(getContext(),105),Aero.dp(getContext(),28)));content.addView(header);
        ScrollView scroll=new ScrollView(getContext());LinearLayout rows=Aero.column(getContext());rows.setBackgroundColor(0xffffffff);scroll.addView(rows);content.addView(scroll,new LayoutParams(-1,0,1));
        for(Task task:tasks){LinearLayout row=new LinearLayout(getContext());row.setGravity(Gravity.CENTER_VERTICAL);row.setFocusable(true);row.setContentDescription("Task: "+task.title);row.setBackground(task.id.equals(selected)?Aero.selection(getContext()):Aero.panel(getContext(),0xffffffff,0xffffffff,0,0));row.setSelected(task.id.equals(selected));View icon=host.icon(task.id);row.addView(icon,new LayoutParams(Aero.dp(getContext(),22),Aero.dp(getContext(),22)));TextView title=text(task.title,12);title.setSingleLine();title.setEllipsize(android.text.TextUtils.TruncateAt.END);row.addView(title,new LayoutParams(0,-1,1));TextView state=text(task.status,10);state.setMaxLines(2);row.addView(state,new LayoutParams(Aero.dp(getContext(),105),-1));row.setOnClickListener(v->{selected=task.id;render();});ContextMenus.bind(row,(x,y)->{selected=task.id;updateSelection();dismissMenu();menu=new DesktopContextMenu(getContext(),"Task commands");menu.item("Switch To","programs",this::switchSelected);menu.item(task.external?"App Info…":"End Task",task.external?"control":"close",this::endSelected);ContextMenus.show(menu,row,x,y);});rows.addView(row,new LayoutParams(-1,Aero.dp(getContext(),42)));}
        if(tasks.isEmpty()){TextView empty=text("No other Nth windows are open.",12);rows.addView(empty,new LayoutParams(-1,Aero.dp(getContext(),54)));}
        TextView hint=text("Android apps are recently opened entries. Use App Info to manage them.",10);hint.setTypeface(Aero.face(getContext()),android.graphics.Typeface.ITALIC);content.addView(hint,new LayoutParams(-1,-2));status.setText(tasks.size()+" tasks  •  "+(paused?"Updates paused":"Updates every 2 seconds"));
    }
    private TextView text(String value,int size){TextView t=Aero.text(getContext(),value,size,Aero.INK);t.setGravity(Gravity.CENTER_VERTICAL);t.setPadding(Aero.dp(getContext(),6),Aero.dp(getContext(),3),Aero.dp(getContext(),4),Aero.dp(getContext(),3));return t;}
    private void renderPerformance(){
        ScrollView scroll=new ScrollView(getContext());LinearLayout body=Aero.column(getContext());scroll.addView(body);content.addView(scroll,new LayoutParams(-1,-1));
        cpuText=text("Nth Desktop CPU — sampling…",12);body.addView(cpuText);cpuGraph=new HistoryGraph(getContext(),cpuHistory);cpuGraph.setContentDescription("Nth Desktop CPU history");body.addView(cpuGraph,new LayoutParams(-1,Aero.dp(getContext(),85)));
        memoryText=text("Physical Memory — sampling…",12);body.addView(memoryText);memoryGraph=new HistoryGraph(getContext(),memoryHistory);memoryGraph.setContentDescription("Device physical memory history");body.addView(memoryGraph,new LayoutParams(-1,Aero.dp(getContext(),85)));detail=text("",11);body.addView(detail);sample();
    }
    private Task selected(){for(Task task:tasks)if(task.id.equals(selected))return task;return null;}
    private void updateSelection(){Task t=selected();boolean enabled=tab.equals("Applications")&&t!=null;switchTo.setEnabled(enabled);end.setEnabled(enabled);switchTo.setAlpha(enabled?1:.4f);end.setAlpha(enabled?1:.4f);end.setText(t!=null&&t.external?"App Info…":"End Task");selection.setText(tab.equals("Performance")?"CPU measures Nth Desktop; memory covers the device.":t==null?"Select a task":t.title);}
    private void switchSelected(){Task t=selected();if(t!=null)host.switchTo(t.id);}
    private void endSelected(){Task t=selected();if(t==null)return;if(t.external)host.appInfo(t.id);else host.end(t.id);refresh();}
    void tasksChanged(){if(tab.equals("Applications"))refresh();}
    void refresh(){if(tab.equals("Applications")){List<Task> next=host.tasks();boolean same=next.size()==tasks.size();if(same)for(int i=0;i<next.size();i++){Task a=next.get(i),b=tasks.get(i);if(!a.id.equals(b.id)||!a.title.equals(b.title)||!a.status.equals(b.status)){same=false;break;}}if(!same)render();}else sample();}
    private void sample(){
        long now=SystemClock.elapsedRealtime(),cpu=android.os.Process.getElapsedCpuTime();float percent=lastWall>0&&now>lastWall?Math.max(0,100f*(cpu-lastCpu)/(now-lastWall)):0;lastWall=now;lastCpu=cpu;
        ActivityManager.MemoryInfo info=new ActivityManager.MemoryInfo();ActivityManager manager=getContext().getSystemService(ActivityManager.class);if(manager!=null)manager.getMemoryInfo(info);float used=info.totalMem>0?100f*(info.totalMem-info.availMem)/info.totalMem:0;
        System.arraycopy(cpuHistory,1,cpuHistory,0,59);System.arraycopy(memoryHistory,1,memoryHistory,0,59);cpuHistory[59]=Math.min(100,percent);memoryHistory[59]=Math.max(0,Math.min(100,used));samples++;
        if(cpuText!=null){cpuText.setText(String.format(Locale.getDefault(),"Nth Desktop CPU: %.1f%% (one core)",percent));memoryText.setText(String.format(Locale.getDefault(),"Physical Memory: %.0f%% used",used));Runtime runtime=Runtime.getRuntime();detail.setText("Device: "+mb(info.availMem)+" available / "+mb(info.totalMem)+" total\nNth Java heap: "+mb(runtime.totalMemory()-runtime.freeMemory())+" used / "+mb(runtime.maxMemory())+" limit\nDevice uptime: "+(now/3600000)+"h "+(now/60000%60)+"m");cpuGraph.invalidate();memoryGraph.invalidate();status.setText(paused?"Updates paused":"Live readings  •  2 second interval");}
    }
    private String mb(long bytes){return android.text.format.Formatter.formatShortFileSize(getContext(),Math.max(0,bytes));}
    private boolean sampling(){return active&&!paused&&isAttachedToWindow()&&isShown()&&getWindowVisibility()==VISIBLE;}
    private void sync(){if(status!=null&&paused)status.setText("Updates paused");removeCallbacks(tick);lastWall=0;lastCpu=0;if(sampling())post(tick);}
    void active(boolean value){active=value;if(!value)dismissMenu();sync();}
    int sampleCount(){return samples;}
    JSONObject state(){JSONObject state=new JSONObject();GadgetLayout.put(state,"tab",tab);GadgetLayout.put(state,"selected",selected);GadgetLayout.put(state,"paused",paused);return state;}
    void restore(JSONObject state){if(state==null)return;tab=state.optString("tab").equals("Performance")?"Performance":"Applications";selected=state.optString("selected");paused=state.optBoolean("paused");render();sync();}
    void close(){active=false;removeCallbacks(tick);dismissMenu();}
    private void dismissMenu(){if(menu!=null){menu.dismiss();menu=null;}}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();sync();}
    @Override protected void onVisibilityChanged(View v,int visibility){super.onVisibilityChanged(v,visibility);if(content!=null)sync();}
    @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);if(content!=null)sync();}
    @Override protected void onDetachedFromWindow(){removeCallbacks(tick);dismissMenu();super.onDetachedFromWindow();}
    private static final class HistoryGraph extends View {
        private final float[] values;private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        HistoryGraph(Context c,float[] values){super(c);this.values=values;}
        @Override protected void onDraw(Canvas c){c.drawColor(0xff071a0b);p.setColor(0xff174627);p.setStrokeWidth(1);for(int x=0;x<getWidth();x+=Math.max(1,getWidth()/12))c.drawLine(x,0,x,getHeight(),p);for(int y=0;y<getHeight();y+=Math.max(1,getHeight()/5))c.drawLine(0,y,getWidth(),y,p);p.setColor(0xff57e86c);p.setStrokeWidth(Aero.dp(getContext(),1));for(int i=1;i<values.length;i++)c.drawLine((i-1)*getWidth()/59f,getHeight()*(1-values[i-1]/100),i*getWidth()/59f,getHeight()*(1-values[i]/100),p);}
    }
}
