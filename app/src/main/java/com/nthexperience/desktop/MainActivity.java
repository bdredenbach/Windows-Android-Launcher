package com.nthexperience.desktop;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.role.RoleManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity implements StartMenu.Actions {
    private DesktopStore store;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Map<String,AppCatalog.Entry> entries=new LinkedHashMap<>();
    private final Map<String,DesktopWindow> windows=new LinkedHashMap<>();
    private FrameLayout root,workspace;
    private GridLayout desktop;
    private LinearLayout shell,taskItems;
    private StartMenu startMenu;
    private TextView clock;
    private AeroWallpaper wallpaper;
    private boolean destroyed,scanPending,scanAgain,receiverRegistered;
    private int scanGeneration,columns;
    private OnBackInvokedCallback backCallback;
    private final BroadcastReceiver packages=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){scanApps();}};
    private final BroadcastReceiver timeChanged=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){updateClock();}};
    private final Runnable clockTick=new Runnable(){public void run(){updateClock();main.postDelayed(this,60000-System.currentTimeMillis()%60000);}};

    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);store=new DesktopStore(this);
        getWindow().setStatusBarColor(Color.TRANSPARENT);getWindow().setNavigationBarColor(0xff0a263b);
        if(Build.VERSION.SDK_INT>=30)getWindow().setDecorFitsSystemWindows(false);
        else getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        buildDesktop();
        if(Build.VERSION.SDK_INT>=33){backCallback=this::handleBack;getOnBackInvokedDispatcher().registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT,backCallback);}
        IntentFilter packageFilter=new IntentFilter();packageFilter.addAction(Intent.ACTION_PACKAGE_ADDED);packageFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);packageFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);packageFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);packageFilter.addDataScheme("package");
        // These receivers accept only Android's protected package/time broadcasts.
        if(Build.VERSION.SDK_INT>=33)registerReceiver(packages,packageFilter,Context.RECEIVER_EXPORTED);else registerReceiver(packages,packageFilter);
        IntentFilter timeFilter=new IntentFilter();timeFilter.addAction(Intent.ACTION_TIME_CHANGED);timeFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);timeFilter.addAction(Intent.ACTION_LOCALE_CHANGED);
        if(Build.VERSION.SDK_INT>=33)registerReceiver(timeChanged,timeFilter,Context.RECEIVER_EXPORTED);else registerReceiver(timeChanged,timeFilter);
        receiverRegistered=true;scanApps();
    }
    private void buildDesktop(){
        root=new FrameLayout(this);wallpaper=new AeroWallpaper(this,store.prefs.getInt("wallpaper",0));root.addView(wallpaper,new FrameLayout.LayoutParams(-1,-1));
        shell=Aero.column(this);root.addView(shell,new FrameLayout.LayoutParams(-1,-1));
        shell.setOnApplyWindowInsetsListener((v,insets)->{
            int left,top,right,bottom;
            if(Build.VERSION.SDK_INT>=30){android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());android.graphics.Insets ime=insets.getInsets(WindowInsets.Type.ime());left=bars.left;top=bars.top;right=bars.right;bottom=Math.max(bars.bottom,ime.bottom);}
            else{left=insets.getSystemWindowInsetLeft();top=insets.getSystemWindowInsetTop();right=insets.getSystemWindowInsetRight();bottom=insets.getSystemWindowInsetBottom();}
            shell.setPadding(left,top,right,bottom);return insets;
        });
        workspace=new FrameLayout(this);shell.addView(workspace,new LinearLayout.LayoutParams(-1,0,1));
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);scroll.setPadding(dp(7),dp(10),dp(7),dp(12));scroll.setVerticalScrollBarEnabled(false);
        desktop=new GridLayout(this);desktop.setAlignmentMode(GridLayout.ALIGN_BOUNDS);scroll.addView(desktop,new ScrollView.LayoutParams(-1,-2));workspace.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
        GestureDetector gestures=new GestureDetector(this,new GestureDetector.SimpleOnGestureListener(){
            public boolean onDown(MotionEvent e){return true;}
            public boolean onFling(MotionEvent a,MotionEvent b,float vx,float vy){if(a!=null&&a.getY()-b.getY()>dp(80)&&Math.abs(vy)>300){showStart(true);return true;}return false;}
            public void onLongPress(MotionEvent e){desktopOptions();}
            public boolean onSingleTapUp(MotionEvent e){startMenu.hide();return true;}
        });
        desktop.setOnTouchListener((v,event)->gestures.onTouchEvent(event));
        workspace.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{
            int next=Math.max(2,Math.min(9,(r-l-dp(14))/dp(88)));
            if(next!=columns){columns=next;renderDesktop();}
            for(DesktopWindow tool:windows.values())tool.fitToHost();
            if(startMenu!=null){startMenu.availableHeight(b-t);FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)startMenu.getLayoutParams();int h=Math.min(dp(610),Math.max(1,b-t-dp(8)));int w=Math.min(dp(390),Math.max(1,r-l-dp(12)));if(lp.height!=h||lp.width!=w){lp.height=h;lp.width=w;startMenu.setLayoutParams(lp);}}
        });
        buildTaskbar();
        startMenu=new StartMenu(this,this);startMenu.setFocusableInTouchMode(true);startMenu.setVisibility(View.GONE);
        FrameLayout.LayoutParams menuParams=new FrameLayout.LayoutParams(dp(370),dp(570),Gravity.BOTTOM|Gravity.START);menuParams.setMargins(dp(6),dp(4),dp(6),dp(4));workspace.addView(startMenu,menuParams);
        setContentView(root);shell.requestApplyInsets();renderDesktop();
    }
    private void buildTaskbar(){
        LinearLayout taskbar=new LinearLayout(this);taskbar.setGravity(Gravity.CENTER_VERTICAL);taskbar.setPadding(dp(4),0,dp(4),0);
        taskbar.setBackground(Aero.panel(this,0xc4628caa,0xf30b243b,0,0x88c6e9f9));taskbar.setElevation(dp(5));
        AeroIcon orb=new AeroIcon(this,"orb");orb.setContentDescription("Start menu");orb.setFocusable(true);orb.setOnClickListener(v->{if(startMenu.getVisibility()==View.VISIBLE)startMenu.hide();else showStart(false);});
        taskbar.addView(orb,new LinearLayout.LayoutParams(dp(56),dp(56)));
        HorizontalScrollView strip=new HorizontalScrollView(this);strip.setHorizontalScrollBarEnabled(false);strip.setFillViewport(true);
        taskItems=new LinearLayout(this);taskItems.setGravity(Gravity.CENTER_VERTICAL);strip.addView(taskItems,new HorizontalScrollView.LayoutParams(-2,-1));taskbar.addView(strip,new LinearLayout.LayoutParams(0,dp(54),1));
        clock=Aero.text(this,"",11,Color.WHITE);clock.setGravity(Gravity.CENTER);clock.setPadding(dp(8),0,dp(8),0);clock.setMinWidth(dp(82));clock.setOnClickListener(v->calendar());clock.setContentDescription("Clock and calendar");
        taskbar.addView(clock,new LinearLayout.LayoutParams(-2,dp(56)));
        TextView peek=Aero.text(this,"",10,Color.WHITE);peek.setBackground(Aero.panel(this,0x55c5e6f5,0x33819cb9,0,0x889fc1d5));peek.setContentDescription("Show desktop");peek.setOnClickListener(v->showDesktop());
        taskbar.addView(peek,new LinearLayout.LayoutParams(dp(18),dp(56)));shell.addView(taskbar,new LinearLayout.LayoutParams(-1,dp(60)));renderTaskbar();
    }
    private void scanApps(){
        if(destroyed)return;if(scanPending){scanAgain=true;return;}scanPending=true;int generation=++scanGeneration;
        worker.execute(()->{
            List<AppCatalog.Entry> apps;
            try{apps=AppCatalog.load(getApplicationContext());}catch(RuntimeException e){apps=new ArrayList<>();}
            List<AppCatalog.Entry> result=apps;
            main.post(()->{if(destroyed||generation!=scanGeneration)return;scanPending=false;entries.clear();for(AppCatalog.Entry e:result)entries.put(e.id,e);startMenu.entries(result);renderDesktop();renderTaskbar();if(scanAgain){scanAgain=false;scanApps();}});
        });
    }
    private void renderDesktop(){
        if(desktop==null)return;desktop.removeAllViews();desktop.setColumnCount(Math.max(2,columns));
        List<String> ids=store.desktop();
        for(String id:ids){
            LinearLayout tile=Aero.column(this);tile.setGravity(Gravity.CENTER);tile.setPadding(dp(2),dp(6),dp(2),dp(4));
            View icon=icon(id,52);tile.addView(icon,new LinearLayout.LayoutParams(dp(52),dp(52)));
            TextView label=Aero.text(this,label(id),12,Color.WHITE);label.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);label.setMaxLines(2);label.setEllipsize(TextUtils.TruncateAt.END);label.setShadowLayer(dp(2),0,dp(1),0xff122435);
            tile.addView(label,new LinearLayout.LayoutParams(-1,-2));
            tile.setBackground(Aero.panel(this,0x08ffffff,0x00ffffff,4,0));tile.setContentDescription(label(id)+". Hold and drag to swap shortcut positions. Desktop options in Start menu.");
            tile.setOnClickListener(v->open(id));tile.setFocusable(true);
            tile.setOnLongClickListener(v->{ClipData data=ClipData.newPlainText("desktop-shortcut",id);return v.startDragAndDrop(data,new View.DragShadowBuilder(v),id,0);});
            tile.setOnDragListener((v,event)->{
                boolean internal=event.getLocalState() instanceof String;
                switch(event.getAction()){
                    case DragEvent.ACTION_DRAG_STARTED:return internal;
                    case DragEvent.ACTION_DRAG_ENTERED:if(internal)v.setAlpha(.55f);return internal;
                    case DragEvent.ACTION_DRAG_EXITED:case DragEvent.ACTION_DRAG_ENDED:v.setAlpha(1);return internal;
                    case DragEvent.ACTION_DROP:
                        if(!internal)return false;store.desktop(LauncherState.move(store.desktop(),(String)event.getLocalState(),id));desktop.post(this::renderDesktop);return true;
                    default:return internal;
                }
            });
            GridLayout.LayoutParams lp=new GridLayout.LayoutParams();lp.width=0;lp.height=dp(108);lp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);tile.setLayoutParams(lp);desktop.addView(tile);
        }
    }
    private void renderTaskbar(){
        if(taskItems==null)return;taskItems.removeAllViews();
        for(Map.Entry<String,DesktopWindow> item:windows.entrySet()){
            if(item.getValue().getParent()==null)continue;
            TextView b=Aero.text(this,label(item.getKey()),12,Color.WHITE);b.setGravity(Gravity.CENTER);b.setPadding(dp(12),0,dp(12),0);b.setBackground(Aero.panel(this,0x887db2d0,0x443c728f,4,0x66b1dcf3));b.setOnClickListener(v->{startMenu.hide();item.getValue().restore();});
            taskItems.addView(b,new LinearLayout.LayoutParams(-2,dp(46)));
        }
        for(String id:store.recents()){
            if(!entries.containsKey(id))continue;FrameLayout slot=new FrameLayout(this);View icon=icon(id,30);
            FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(dp(30),dp(30),Gravity.CENTER);slot.addView(icon,lp);slot.setContentDescription("Recently launched: "+label(id));slot.setOnClickListener(v->open(id));slot.setFocusable(true);
            taskItems.addView(slot,new LinearLayout.LayoutParams(dp(48),dp(48)));
        }
    }
    private View icon(String id,int size){
        AppCatalog.Entry entry=entries.get(id);
        if(entry!=null){ImageView image=new ImageView(this);image.setImageDrawable(entry.icon);image.setScaleType(ImageView.ScaleType.FIT_CENTER);image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);return image;}
        return new AeroIcon(this,id.startsWith("@")?id.substring(1):"apps");
    }
    private String label(String id){
        switch(id){case "@calendar":return "Calendar";case "@computer":return "Computer";case "@browser":return "Internet";case "@files":return "Files";case "@notes":return "Notepad";case "@appearance":return "Personalize";default:AppCatalog.Entry app=entries.get(id);return app==null?"Unavailable app":app.label;}
    }
    @Override public void open(String id){
        startMenu.hide();
        switch(id){
            case "@computer":computer();return;
            case "@notes":notes();return;
            case "@appearance":appearance();return;
            case "@browser":launchSystem(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com")));return;
            case "@files":launchSystem(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*"),true);return;
        }
        ComponentName component=ComponentName.unflattenFromString(id);
        if(component==null){toast("This shortcut is unavailable. Hold its Start menu entry to remove it.");return;}
        try{
            Intent launch=Intent.makeMainActivity(component).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(launch);store.launched(id);renderTaskbar();
        }catch(ActivityNotFoundException|SecurityException e){
            new AlertDialog.Builder(this).setTitle("App unavailable").setMessage("This app may have been removed or disabled.").setPositiveButton("Remove shortcut",(d,w)->{List<String> ids=store.desktop();ids.remove(id);store.desktop(ids);renderDesktop();}).setNegativeButton("Keep",null).show();scanApps();
        }
    }
    @Override public void options(View anchor,String id){
        PopupMenu menu=new PopupMenu(this,anchor);menu.getMenu().add(store.pinned(id)?"Remove from desktop":"Add to desktop").setOnMenuItemClickListener(item->{store.toggle(id);renderDesktop();return true;});
        ComponentName component=ComponentName.unflattenFromString(id);if(component!=null)menu.getMenu().add("App info").setOnMenuItemClickListener(item->{launchSystem(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+component.getPackageName())));return true;});menu.show();
    }
    private void desktopOptions(){
        new AlertDialog.Builder(this).setTitle("Desktop").setItems(new String[]{"All programs","Personalize","Manage desktop shortcuts","Make default home"},(dialog,which)->{if(which==0)showStart(false);if(which==1)appearance();if(which==2)manageShortcuts();if(which==3)home();}).show();
    }
    private void manageShortcuts(){
        List<String> ids=store.desktop();String[] labels=new String[ids.size()];for(int i=0;i<ids.size();i++)labels[i]=label(ids.get(i));
        new AlertDialog.Builder(this).setTitle("Remove a desktop shortcut").setItems(labels,(d,index)->{List<String> current=store.desktop();current.remove(ids.get(index));store.desktop(current);renderDesktop();}).setNegativeButton("Done",null).show();
    }
    private void showStart(boolean search){startMenu.show(search);}
    private void showDesktop(){((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(workspace.getWindowToken(),0);startMenu.hide();for(DesktopWindow window:windows.values())window.setVisibility(View.GONE);renderTaskbar();}
    private void window(String id,String title,View body){
        DesktopWindow current=windows.get(id);if(current!=null&&current.getParent()!=null){current.restore();return;}
        DesktopWindow next=new DesktopWindow(this,workspace,title,body,this::renderTaskbar);windows.put(id,next);
        if(workspace.getWidth()>0){next.open();renderTaskbar();}else workspace.post(()->{if(!destroyed&&next.getParent()==null){next.open();renderTaskbar();}});
    }
    private void notes(){
        LinearLayout body=Aero.column(this);
        TextView hint=Aero.text(this,"  Saved automatically on this device",12,0xff567082);hint.setPadding(dp(4),dp(8),dp(4),dp(8));body.addView(hint);
        EditText editor=new EditText(this);editor.setTextSize(16);editor.setTextColor(Aero.INK);editor.setGravity(Gravity.TOP|Gravity.START);editor.setBackgroundColor(0xfffcfdf7);editor.setPadding(dp(14),dp(12),dp(14),dp(12));editor.setHint("A thought worth keeping…");editor.setContentDescription("Notepad text");editor.setText(store.prefs.getString("note",""));
        editor.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){}public void onTextChanged(CharSequence s,int a,int b,int c){store.prefs.edit().putString("note",s.toString()).apply();}public void afterTextChanged(Editable e){}});
        body.addView(editor,new LinearLayout.LayoutParams(-1,0,1));window("@notes","Notepad",body);
    }
    private void computer(){
        LinearLayout content=Aero.column(this);Aero.pad(content,18);
        TextView title=Aero.text(this,"Welcome to Nth Desktop",23,Aero.INK);Aero.bold(title);content.addView(title);
        paragraph(content,"A Vista-inspired home for your Android.\nVersion 0.1.0 · First test build");
        paragraph(content,"Tap the N orb to find your installed apps. Hold a program to add it to the desktop. Hold and drag desktop shortcuts to swap their positions.");
        paragraph(content,"The narrow strip at the far right of the taskbar shows your desktop. Minimized built-in tools stay in the taskbar; Android apps open normally.");
        paragraph(content,isHome()?"Nth Desktop is your default home app.":"Try the desktop first, then choose it as your home app when you're ready.");
        addAction(content,isHome()?"Home app settings":"Make Nth Desktop my home",this::home);
        addAction(content,"Personalize desktop",this::appearance);
        addAction(content,"Manage desktop shortcuts",this::manageShortcuts);
        addAction(content,"Android settings",this::settings);
        paragraph(content,"To switch back: Android Settings → Apps → Default apps → Home app.\n\nOriginal code and artwork by Nth Experience. No Microsoft code, wallpapers or icons are bundled.");
        ScrollView scroll=new ScrollView(this);scroll.addView(content);window("@computer","Computer",scroll);
    }
    private void appearance(){
        LinearLayout content=Aero.column(this);Aero.pad(content,16);TextView title=Aero.text(this,"Make yourself at home",22,Aero.INK);Aero.bold(title);content.addView(title);paragraph(content,"Original aurora wallpapers");
        String[] names={"Lagoon · blue and sea glass","Twilight · violet and rose","Meadow · sage and gold"};
        for(int i=0;i<names.length;i++){
            final int theme=i;FrameLayout preview=new FrameLayout(this);preview.addView(new AeroWallpaper(this,i),new FrameLayout.LayoutParams(-1,-1));
            TextView label=Aero.text(this,names[i],14,Color.WHITE);label.setBackgroundColor(0x44062233);label.setGravity(Gravity.CENTER);FrameLayout.LayoutParams caption=new FrameLayout.LayoutParams(-1,dp(42),Gravity.BOTTOM);preview.addView(label,caption);
            preview.setContentDescription("Use "+names[i]+" wallpaper");preview.setOnClickListener(v->{store.prefs.edit().putInt("wallpaper",theme).apply();wallpaper.theme(theme);toast("Wallpaper changed");});preview.setFocusable(true);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(105));lp.bottomMargin=dp(12);content.addView(preview,lp);
        }
        addAction(content,"Manage desktop shortcuts",this::manageShortcuts);
        addAction(content,"Restore original desktop shortcuts",()->{new AlertDialog.Builder(this).setTitle("Restore shortcuts?").setMessage("This resets desktop shortcuts and their order. Your note and installed apps are kept.").setPositiveButton("Restore",(d,w)->{store.prefs.edit().remove("shortcuts").apply();renderDesktop();}).setNegativeButton("Cancel",null).show();});
        ScrollView scroll=new ScrollView(this);scroll.addView(content);window("@appearance","Personalization",scroll);
    }
    private void calendar(){
        startMenu.hide();CalendarView calendar=new CalendarView(this);calendar.setMinDate(0);LinearLayout content=Aero.column(this);
        TextView today=Aero.text(this,new SimpleDateFormat("EEEE, MMMM d",Locale.getDefault()).format(new Date()),18,Aero.INK);Aero.pad(today,14);content.addView(today);content.addView(calendar,new LinearLayout.LayoutParams(-1,0,1));window("@calendar","Clock and calendar",content);
    }
    @Override public void home(){
        startMenu.hide();
        if(Build.VERSION.SDK_INT>=29){RoleManager roles=getSystemService(RoleManager.class);if(roles!=null&&roles.isRoleAvailable(RoleManager.ROLE_HOME)&&!roles.isRoleHeld(RoleManager.ROLE_HOME)){try{startActivityForResult(roles.createRequestRoleIntent(RoleManager.ROLE_HOME),12);return;}catch(ActivityNotFoundException ignored){}}}
        launchSystem(new Intent(Settings.ACTION_HOME_SETTINGS));
    }
    private boolean isHome(){
        Intent home=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);android.content.pm.ResolveInfo result=getPackageManager().resolveActivity(home,PackageManager.MATCH_DEFAULT_ONLY);return result!=null&&getPackageName().equals(result.activityInfo.packageName);
    }
    @Override public void settings(){launchSystem(new Intent(Settings.ACTION_SETTINGS));}
    private void launchSystem(Intent intent){launchSystem(intent,false);}
    private void launchSystem(Intent intent,boolean result){
        try{if(result)startActivityForResult(intent,11);else startActivity(intent);}catch(ActivityNotFoundException|SecurityException ex){toast("No app is available for that action on this device.");}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==11&&result==RESULT_OK&&data!=null&&data.getData()!=null){
            Uri uri=data.getData();Intent view=new Intent(Intent.ACTION_VIEW).setDataAndType(uri,getContentResolver().getType(uri)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);launchSystem(view);
        }
    }
    private void paragraph(LinearLayout parent,String text){TextView p=Aero.text(this,text,14,0xff476071);p.setLineSpacing(dp(3),1);p.setPadding(0,dp(12),0,dp(12));parent.addView(p);}
    private void addAction(LinearLayout parent,String label,Runnable action){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(8);parent.addView(Aero.button(this,label,action),lp);}
    private int dp(int value){return Aero.dp(this,value);}
    private void toast(String message){Toast.makeText(this,message,Toast.LENGTH_SHORT).show();}
    private void updateClock(){if(clock!=null){Date now=new Date();clock.setText(DateFormat.getTimeFormat(this).format(now)+"\n"+new SimpleDateFormat("MMM d",Locale.getDefault()).format(now));}}
    private void handleBack(){
        if(startMenu.getVisibility()==View.VISIBLE){startMenu.hide();return;}
        for(int i=workspace.getChildCount()-1;i>=0;i--){View v=workspace.getChildAt(i);if(v instanceof DesktopWindow&&v.getVisibility()==View.VISIBLE){v.setVisibility(View.GONE);((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(),0);renderTaskbar();return;}}
    }
    @Override public void onBackPressed(){handleBack();}
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);if(startMenu!=null)startMenu.hide();}
    @Override protected void onResume(){super.onResume();main.removeCallbacks(clockTick);main.post(clockTick);if(store!=null)scanApps();}
    @Override protected void onPause(){super.onPause();main.removeCallbacks(clockTick);if(startMenu!=null)startMenu.hide();}
    @Override protected void onDestroy(){destroyed=true;main.removeCallbacksAndMessages(null);worker.shutdownNow();if(receiverRegistered){unregisterReceiver(packages);unregisterReceiver(timeChanged);}if(Build.VERSION.SDK_INT>=33&&backCallback!=null)getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(backCallback);super.onDestroy();}
}
