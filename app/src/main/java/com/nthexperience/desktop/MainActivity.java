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
import android.provider.DocumentsContract;
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
import org.json.*;

public final class MainActivity extends Activity implements StartMenu.Actions,GadgetSidebar.Actions {
    private DesktopStore store;
    private String layoutKey;
    private JSONObject pausedSession;
    private String pendingFolderWindow="@files";
    private DesktopBackup.Plan restorePlan;
    private String backupMessage="";
    private boolean backupBusy,restoringBackup;
    private FrameLayout restoreCover;
    private StartupExperience startup;
    private StorageSetup storageSetup;private boolean restoreStorageSetup;private boolean storageAllowed;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Map<String,AppCatalog.Entry> entries=new LinkedHashMap<>();
    private final Map<String,DesktopWindow> windows=new LinkedHashMap<>();
    private FrameLayout root,workspace,videoOverlay;
    private int previousShellAccessibility;
    private DesktopIconGrid desktop;
    private ScrollView desktopScroll;
    private GadgetSidebar sidebar;
    private DesktopContextMenu desktopMenu;
    private int desktopRows,desktopCutoutLeft,desktopCutoutRight,windowTopInset;
    private WeatherLocation weatherLocation;
    private LinearLayout shell,taskItems;
    private StartMenu startMenu;
    private SystemTray tray;
    private TextView clock;
    private AeroWallpaper wallpaper;
    private boolean destroyed,scanPending,scanAgain,receiverRegistered;
    private int scanGeneration,columns;
    private OnBackInvokedCallback backCallback;
    private final BroadcastReceiver packages=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){scanApps();}};
    private final BroadcastReceiver timeChanged=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){updateClock();}};
    private final Runnable desktopRender=()->{if(!destroyed)renderDesktop();};
    private final Runnable clockTick=new Runnable(){public void run(){updateClock();main.postDelayed(this,60000-System.currentTimeMillis()%60000);}};

    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);store=new DesktopStore(this);storageSetup=new StorageSetup(this);storageAllowed=StorageAccess.has(this);restoreStorageSetup=saved!=null&&saved.getBoolean("storage_setup_open");layoutKey=getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE?"landscape":"portrait";
        if(saved!=null)pendingFolderWindow=saved.getString("folder_window","@files");
        store.migrateDesktop();
        getWindow().setStatusBarColor(Color.TRANSPARENT);getWindow().setNavigationBarColor(0xff0a263b);
        if(Build.VERSION.SDK_INT>=28){WindowManager.LayoutParams attributes=getWindow().getAttributes();attributes.layoutInDisplayCutoutMode=Build.VERSION.SDK_INT>=30?WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS:WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;getWindow().setAttributes(attributes);}
        if(Build.VERSION.SDK_INT>=30)getWindow().setDecorFitsSystemWindows(false);
        else getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        buildDesktop();weatherLocation=new WeatherLocation(this,store,sidebar.weather,()->store.gadgets().contains("weather"),this::updateWeatherSummary);applySystemChrome();String backupStatus=getIntent().getStringExtra("backup_status");if(backupStatus!=null){getIntent().removeExtra("backup_status");main.post(()->toast(backupStatus));}
        handleRequestedTool();
        if(saved!=null&&saved.containsKey("desktop_session")){
            String session=saved.getString("desktop_session");
            workspace.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener(){public void onGlobalLayout(){if(workspace.getWidth()<1||workspace.getHeight()<1)return;workspace.getViewTreeObserver().removeOnGlobalLayoutListener(this);restoreSession(session);}});
        }
        if(Build.VERSION.SDK_INT>=33){backCallback=this::handleBack;getOnBackInvokedDispatcher().registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT,backCallback);}
        IntentFilter packageFilter=new IntentFilter();packageFilter.addAction(Intent.ACTION_PACKAGE_ADDED);packageFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);packageFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);packageFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);packageFilter.addDataScheme("package");
        // These receivers accept only Android's protected package/time broadcasts.
        if(Build.VERSION.SDK_INT>=33)registerReceiver(packages,packageFilter,Context.RECEIVER_EXPORTED);else registerReceiver(packages,packageFilter);
        IntentFilter timeFilter=new IntentFilter();timeFilter.addAction(Intent.ACTION_TIME_CHANGED);timeFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);timeFilter.addAction(Intent.ACTION_LOCALE_CHANGED);
        if(Build.VERSION.SDK_INT>=33)registerReceiver(timeChanged,timeFilter,Context.RECEIVER_EXPORTED);else registerReceiver(timeChanged,timeFilter);
        receiverRegistered=true;scanApps();loadRestorePreview();startup=new StartupExperience(this,root,store,saved!=null,()->open("@welcome"));
    }
    private void buildDesktop(){
        root=new FrameLayout(this);wallpaper=new AeroWallpaper(this,store.prefs.getInt("wallpaper",0));root.addView(wallpaper,new FrameLayout.LayoutParams(-1,-1));
        shell=Aero.column(this);root.addView(shell,new FrameLayout.LayoutParams(-1,-1));
        shell.setOnApplyWindowInsetsListener((v,insets)->{
            int left,top,right,bottom;
            if(Build.VERSION.SDK_INT>=30){android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars());android.graphics.Insets cutout=insets.getInsets(WindowInsets.Type.displayCutout());android.graphics.Insets ime=insets.getInsets(WindowInsets.Type.ime());desktopCutoutLeft=Math.max(0,cutout.left-bars.left);desktopCutoutRight=Math.max(0,cutout.right-bars.right);left=bars.left;top=store.prefs.getBoolean("show_status_bar",false)?Math.max(bars.top,cutout.top):bars.top;windowTopInset=Math.max(0,cutout.top-top);right=bars.right;bottom=Math.max(Math.max(bars.bottom,cutout.bottom),ime.bottom);}
            else{left=insets.getSystemWindowInsetLeft();top=insets.getSystemWindowInsetTop();right=insets.getSystemWindowInsetRight();bottom=insets.getSystemWindowInsetBottom();windowTopInset=0;if(Build.VERSION.SDK_INT>=28&&insets.getDisplayCutout()!=null&&!store.prefs.getBoolean("show_status_bar",false)){int safe=insets.getDisplayCutout().getSafeInsetTop();top=Math.max(0,top-safe);windowTopInset=safe;}}
            shell.setPadding(left,top,right,bottom);if(workspace!=null)workspace.post(this::layoutWorkspace);return insets;
        });
        workspace=new FrameLayout(this);shell.addView(workspace,new LinearLayout.LayoutParams(-1,0,1));
        ScrollView scroll=new ScrollView(this);desktopScroll=scroll;scroll.setFillViewport(true);scroll.setClipToPadding(false);scroll.setPadding(dp(7),dp(10),dp(7),dp(12));scroll.setVerticalScrollBarEnabled(false);
        // Keep the icon sheet in one cached layer when side insets move during a 180-degree turn.
        desktop=new DesktopIconGrid(this,store,layoutKey);scroll.addView(desktop,new ScrollView.LayoutParams(-1,-2));workspace.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
        desktop.setOnDragListener((v,event)->{if(!(event.getLocalState() instanceof String)||((String)event.getLocalState()).startsWith("gadget:")||store.prefs.getBoolean("layout_locked",false))return false;if(event.getAction()==DragEvent.ACTION_DROP){desktop.place((String)event.getLocalState(),event.getX()-dp(store.tileWidth())/2f,event.getY()-dp(store.tileHeight())/2f);return true;}return true;});
        sidebar=new GadgetSidebar(this,store,wallpaper,this,workspace,layoutKey);FrameLayout.LayoutParams sideParams=new FrameLayout.LayoutParams(dp(140),-1,Gravity.END);workspace.addView(sidebar,sideParams);workspace.addView(sidebar.floatingHost,new FrameLayout.LayoutParams(-1,-1));
        GestureDetector gestures=new GestureDetector(this,new GestureDetector.SimpleOnGestureListener(){
            public boolean onDown(MotionEvent e){return true;}
            public boolean onFling(MotionEvent a,MotionEvent b,float vx,float vy){if(a!=null&&a.getY()-b.getY()>dp(80)&&Math.abs(vy)>300){showStart(true);return true;}return false;}
            public void onLongPress(MotionEvent e){desktopOptions(e.getRawX(),e.getRawY());}
            public boolean onSingleTapUp(MotionEvent e){startMenu.hide();return true;}
        });
        desktop.setContentDescription("Desktop workspace");
        desktop.setOnTouchListener((v,event)->{if(event.getActionMasked()==MotionEvent.ACTION_DOWN&&event.isButtonPressed(MotionEvent.BUTTON_SECONDARY)){desktopOptions(event.getRawX(),event.getRawY());return true;}return gestures.onTouchEvent(event);});
        desktop.setOnGenericMotionListener((v,event)->{if(event.getActionMasked()==MotionEvent.ACTION_BUTTON_PRESS&&event.isButtonPressed(MotionEvent.BUTTON_SECONDARY)){desktopOptions(event.getRawX(),event.getRawY());return true;}return false;});
        workspace.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->layoutWorkspace());
        buildTaskbar();
        startMenu=new StartMenu(this,this,store,wallpaper);startMenu.setFocusableInTouchMode(true);startMenu.setVisibility(View.GONE);
        FrameLayout.LayoutParams menuParams=new FrameLayout.LayoutParams(dp(390),dp(520),Gravity.BOTTOM|Gravity.START);menuParams.setMargins(dp(6),dp(4),dp(6),dp(4));workspace.addView(startMenu,menuParams);
        setContentView(root);shell.requestApplyInsets();renderDesktop();
    }
    private void layoutWorkspace(){
        if(destroyed||workspace==null||workspace.getWidth()==0)return;int width=workspace.getWidth(),height=workspace.getHeight();if(tray!=null)tray.bounds(desktopCutoutLeft,desktopCutoutRight,windowTopInset);
            int reserved=sidebar.getVisibility()==View.VISIBLE?dp(144):0;
            FrameLayout.LayoutParams desktopParams=(FrameLayout.LayoutParams)desktopScroll.getLayoutParams();
            if(desktopParams.rightMargin!=reserved||desktopParams.leftMargin!=0){desktopParams.rightMargin=reserved;desktopParams.leftMargin=0;desktopScroll.setLayoutParams(desktopParams);}
            WindowInsets current=root.getRootWindowInsets();java.util.List<android.graphics.Rect> holes=new ArrayList<>();
            if(current!=null&&Build.VERSION.SDK_INT>=28&&current.getDisplayCutout()!=null)holes.addAll(current.getDisplayCutout().getBoundingRects());
            desktop.configure(dp(store.tileWidth()),dp(store.tileHeight()),holes);desktop.viewport(Math.max(1,height-desktopScroll.getPaddingTop()-desktopScroll.getPaddingBottom()));
            int sidebarTop=0;int[] screen=new int[2];workspace.getLocationOnScreen(screen);int sideLeft=screen[0]+width-dp(140),sideRight=screen[0]+width;
            for(android.graphics.Rect hole:holes)if(hole.right>sideLeft&&hole.left<sideRight&&hole.bottom>screen[1]&&hole.top<screen[1]+windowTopInset)sidebarTop=Math.max(sidebarTop,Math.min(windowTopInset,hole.bottom-screen[1]+dp(2)));
            FrameLayout.LayoutParams sideParams=(FrameLayout.LayoutParams)sidebar.getLayoutParams();if(sideParams.topMargin!=sidebarTop){sideParams.topMargin=sidebarTop;sidebar.setLayoutParams(sideParams);}
            FrameLayout.LayoutParams floatingParams=(FrameLayout.LayoutParams)sidebar.floatingHost.getLayoutParams();if(floatingParams.topMargin!=windowTopInset){floatingParams.topMargin=windowTopInset;sidebar.floatingHost.setLayoutParams(floatingParams);}
            sidebar.workspaceSize(width,Math.max(1,height-windowTopInset),Math.max(1,height-sidebarTop),windowTopInset);
            for(DesktopWindow tool:windows.values()){tool.safeTop(windowTopInset);tool.safeRight(sidebarInset());}
            if(startMenu!=null){startMenu.availableHeight(height-windowTopInset);FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)startMenu.getLayoutParams();int h=Math.min(dp(540),Math.max(1,height-windowTopInset-dp(8)));int w=Math.min(dp(420),Math.max(1,width-dp(12)));if(lp.height!=h||lp.width!=w){lp.height=h;lp.width=w;startMenu.setLayoutParams(lp);}}
    }
    private void buildTaskbar(){
        shell.setClipChildren(false);shell.setClipToPadding(false);
        LinearLayout taskbar=new LinearLayout(this);taskbar.setContentDescription("Desktop taskbar");taskbar.setGravity(Gravity.CENTER_VERTICAL);taskbar.setClipChildren(false);taskbar.setPadding(dp(3),0,dp(3),0);taskbar.setBackground(new AeroGlass(wallpaper,AeroGlass.TASKBAR));taskbar.setElevation(dp(5));
        AeroIcon orb=new AeroIcon(this,"orb");orb.setContentDescription("Start menu");orb.setFocusable(true);orb.setOnClickListener(v->{if(startMenu.getVisibility()==View.VISIBLE)startMenu.hide();else showStart(false);});ContextMenus.bind(orb,(x,y)->startOrbMenu(orb,x,y));taskbar.addView(orb,new LinearLayout.LayoutParams(dp(53),dp(53)));
        HorizontalScrollView strip=new HorizontalScrollView(this);strip.setHorizontalScrollBarEnabled(false);strip.setFillViewport(true);taskItems=new LinearLayout(this);taskItems.setGravity(Gravity.CENTER_VERTICAL);strip.addView(taskItems,new HorizontalScrollView.LayoutParams(-2,-1));FrameLayout taskClip=new FrameLayout(this);taskClip.setClipChildren(true);taskClip.addView(strip,new FrameLayout.LayoutParams(-1,-1));taskbar.addView(taskClip,new LinearLayout.LayoutParams(0,dp(44),1));
        tray=new SystemTray(this,workspace,wallpaper,new SystemTray.Host(){public void action(String id){vistaAction(id);}public void open(String id){MainActivity.this.open(id);}public void opening(){startMenu.hide();((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(root.getWindowToken(),0);}});taskbar.addView(tray,new LinearLayout.LayoutParams(-2,dp(44)));
        clock=Aero.text(this,"",10,Color.WHITE);clock.setGravity(Gravity.CENTER);clock.setPadding(dp(5),0,dp(5),0);clock.setMinWidth(dp(67));clock.setOnClickListener(v->tray.show("calendar"));clock.setContentDescription("Clock and calendar");taskbar.addView(clock,new LinearLayout.LayoutParams(-2,dp(44)));
        TextView peek=Aero.text(this,"",10,Color.WHITE);peek.setBackground(Aero.gloss(this,new int[]{0x668eaec0,0x554c697f,0x66314353},0,0x667f98ab));peek.setContentDescription("Show desktop");peek.setOnClickListener(v->showDesktop());taskbar.addView(peek,new LinearLayout.LayoutParams(dp(12),dp(44)));shell.addView(taskbar,new LinearLayout.LayoutParams(-1,dp(46)));renderTaskbar();
    }
    private void scanApps(){
        if(destroyed)return;if(scanPending){scanAgain=true;return;}scanPending=true;int generation=++scanGeneration;
        worker.execute(()->{
            List<AppCatalog.Entry> apps;
            try{apps=AppCatalog.load(getApplicationContext());}catch(RuntimeException e){apps=new ArrayList<>();}
            List<AppCatalog.Entry> result=apps;ExternalTasks.refresh(getApplicationContext(),store,result);
            main.post(()->{if(destroyed||restoringBackup||generation!=scanGeneration)return;scanPending=false;entries.clear();for(AppCatalog.Entry e:result)entries.put(e.id,e);startMenu.entries(result);renderDesktop();renderTaskbar();for(DesktopWindow tool:windows.values())if(tool.getParent()!=null&&tool.body instanceof VistaShell)((VistaShell)tool.body).refreshApps();if(scanAgain){scanAgain=false;scanApps();}});
        });
    }
    private void renderDesktop(){
        if(desktop==null)return;desktop.removeAllViews();List<String> ids=store.desktop();desktop.configure(dp(store.tileWidth()),dp(store.tileHeight()),java.util.Collections.emptyList());
        for(int index=0;index<ids.size();index++){
            String id=ids.get(index);LinearLayout tile=Aero.column(this);tile.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);tile.setPadding(dp(2),dp(6),dp(2),dp(3));int iconSize=store.iconSize();View icon=icon(id,iconSize);tile.addView(icon,new LinearLayout.LayoutParams(dp(iconSize),dp(iconSize)));
            TextView label=Aero.text(this,label(id),store.iconSize()>46?12:11,Color.WHITE);label.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);label.setMaxLines(2);label.setEllipsize(TextUtils.TruncateAt.END);label.setShadowLayer(dp(3),0,dp(1),0xff000000);tile.addView(label,new LinearLayout.LayoutParams(-1,-2));
            android.graphics.drawable.StateListDrawable background=new android.graphics.drawable.StateListDrawable();background.addState(new int[]{android.R.attr.state_pressed},Aero.panel(this,0x44d0edff,0x2270add0,3,0x779fcde4));background.addState(new int[]{},Aero.panel(this,0x00ffffff,0x00ffffff,3,0));tile.setBackground(background);tile.setContentDescription(label(id)+". Hold and drag onto an icon to group, or near its edge to swap positions.");tile.setOnClickListener(v->open(id));tile.setFocusable(true);ContextMenus.desktopIcon(tile,(x,y)->shortcutMenu(tile,id,"desktop",x,y),()->{if(!store.prefs.getBoolean("layout_locked",false))tile.startDragAndDrop(ClipData.newPlainText("desktop-shortcut",id),new View.DragShadowBuilder(tile),id,0);});
            tile.setOnDragListener((v,event)->{boolean own=event.getLocalState() instanceof String&&!((String)event.getLocalState()).startsWith("gadget:");switch(event.getAction()){
                case DragEvent.ACTION_DRAG_STARTED:return own;case DragEvent.ACTION_DRAG_ENTERED:if(own)v.setAlpha(.5f);return own;
                case DragEvent.ACTION_DRAG_EXITED:case DragEvent.ACTION_DRAG_ENDED:v.setAlpha(1);return own;
                case DragEvent.ACTION_DROP:if(!own||store.prefs.getBoolean("layout_locked",false))return false;String moving=(String)event.getLocalState();boolean group=event.getX()>v.getWidth()*.18f&&event.getX()<v.getWidth()*.82f&&event.getY()>v.getHeight()*.1f&&event.getY()<v.getHeight()*.8f;try{String folder=store.drop(moving,id,group);if(!group)desktop.swapPositions(moving,id);else if(folder!=null&&!folder.equals(id))desktop.place(folder,v.getX(),v.getY());desktop.post(()->{desktopChanged();if(folder!=null)open(folder);});}catch(IllegalArgumentException error){toast(error.getMessage());}return true;default:return own;}});
            tile.setTag(id);ViewGroup.LayoutParams lp=new ViewGroup.LayoutParams(dp(store.tileWidth()),dp(store.tileHeight()));desktop.addView(tile,lp);
        }
        desktop.post(this::layoutWorkspace);
    }
    private void renderTaskbar(){
        if(taskItems==null)return;DesktopWindow weatherWindow=windows.get("@weather"),feedWindow=windows.get("@gadget:feeds"),networkWindow=windows.get("@gadget:network"),compassWindow=windows.get("@gadget:compass");if(sidebar!=null)sidebar.details(weatherWindow!=null&&weatherWindow.getParent()!=null&&weatherWindow.getVisibility()==View.VISIBLE,feedWindow!=null&&feedWindow.getParent()!=null&&feedWindow.getVisibility()==View.VISIBLE,networkWindow!=null&&networkWindow.getParent()!=null&&networkWindow.getVisibility()==View.VISIBLE,compassWindow!=null&&compassWindow.getParent()!=null&&compassWindow.getVisibility()==View.VISIBLE);DesktopWindow front=null;for(int i=workspace.getChildCount()-1;i>=0;i--){View child=workspace.getChildAt(i);if(child instanceof DesktopWindow&&child.getVisibility()==View.VISIBLE&&!((DesktopWindow)child).minimizing()){front=(DesktopWindow)child;break;}}for(DesktopWindow window:windows.values())if(window.getParent()!=null){window.safeRight(sidebarInset());window.active(window==front);}taskItems.removeAllViews();
        for(String id:store.quickLaunch()){FrameLayout quick=new FrameLayout(this);FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(dp(24),dp(24),Gravity.CENTER);quick.addView(icon(id,24),lp);quick.setContentDescription("Open "+label(id));quick.setOnClickListener(v->open(id));ContextMenus.bind(quick,(x,y)->shortcutMenu(quick,id,"quick",x,y));taskItems.addView(quick,new LinearLayout.LayoutParams(dp(36),dp(42)));}
        View divider=new View(this);divider.setBackgroundColor(0x778fa4b0);LinearLayout.LayoutParams rule=new LinearLayout.LayoutParams(dp(1),dp(27));rule.setMargins(dp(4),0,dp(5),0);taskItems.addView(divider,rule);
        for(Map.Entry<String,DesktopWindow> item:windows.entrySet()){
            if(item.getValue().getParent()==null)continue;String title=item.getValue().body instanceof VistaExplorer?item.getValue().caption():label(item.getKey());LinearLayout b=taskButton(item.getKey(),title);b.setBackground(Aero.gloss(this,item.getValue().active()?new int[]{0xddbed9e8,0xcc76a3bf,0xcc446c88,0xcc80a3bb}:new int[]{0x887b93a3,0x77536e82,0x77405b6f,0x77597589},3,0x99b4cad7));b.setContentDescription((item.getValue().active()?"Minimize ":"Restore ")+title);b.setOnClickListener(v->{startMenu.hide();DesktopWindow tool=item.getValue();if(tool.getVisibility()==View.VISIBLE&&!tool.minimizing()&&tool.active())tool.minimize();else tool.restore();});LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(100),dp(35));lp.rightMargin=dp(3);taskItems.addView(b,lp);
        }
        if(store.prefs.getBoolean("taskbar_apps",true))for(String id:ExternalTasks.list()){
            if(!entries.containsKey(id))continue;LinearLayout button=taskButton(id);button.setContentDescription("Switch to "+label(id));button.setBackground(Aero.gloss(this,new int[]{0x997b93a3,0x88536e82,0x88405b6f,0x88597589},3,0x99b4cad7));button.setOnClickListener(v->open(id));ContextMenus.bind(button,(x,y)->{DesktopContextMenu menu=new DesktopContextMenu(this,"Android app taskbar menu");menu.item("Switch to "+label(id),"programs",()->open(id));menu.item("Remove taskbar button","close",()->{ExternalTasks.remove(id);renderTaskbar();});menu.item("Taskbar Properties","control",()->open("setting:page:taskbar"));ContextMenus.show(menu,button,x,y);});LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(118),dp(35));lp.rightMargin=dp(3);taskItems.addView(button,lp);
        }
        if(sidebar!=null&&store.prefs.getBoolean("sidebar_always_top",false))sidebar.raise();
    }
    private int sidebarInset(){return sidebar!=null&&sidebar.getVisibility()==View.VISIBLE&&store.prefs.getBoolean("sidebar_always_top",false)?sidebar.getWidth():0;}
    private LinearLayout taskButton(String id){return taskButton(id,label(id));}
    private LinearLayout taskButton(String id,String title){LinearLayout button=new LinearLayout(this);button.setGravity(Gravity.CENTER_VERTICAL);button.setPadding(dp(6),0,dp(5),0);button.setFocusable(true);DesktopWindow window=windows.get(id);View image=window!=null&&window.body instanceof VistaExplorer&&((VistaExplorer)window.body).page().startsWith("folder:")?new AeroIcon(this,"files"):icon(id,20);button.addView(image,new LinearLayout.LayoutParams(dp(20),dp(20)));TextView name=Aero.text(this,title,11,Color.WHITE);name.setSingleLine(true);name.setEllipsize(TextUtils.TruncateAt.END);name.setPadding(dp(5),0,0,0);button.addView(name,new LinearLayout.LayoutParams(0,-1,1));name.setGravity(Gravity.CENTER_VERTICAL);return button;}
    private View icon(String id,int size){
        AppCatalog.Entry entry=entries.get(id);
        if(entry!=null){ImageView image=new ImageView(this);image.setImageDrawable(entry.icon);image.setScaleType(ImageView.ScaleType.FIT_CENTER);image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);return image;}
        return new AeroIcon(this,id.startsWith("@gadget:")?GadgetCatalog.icon(id.substring(8)):store.isFolder(id)||id.equals("@filemanagers")||id.equals("@organize")?"folderapps":id.equals("@welcome")?"orb":id.equals("@backup")?"drive":id.equals("@quicklaunch")?"programs":id.equals("@photos")?"pictures":id.startsWith("@")?id.substring(1):"apps");
    }
    private String label(String id){return store.name(id,defaultLabel(id));}
    private String defaultLabel(String id){
        if(id.startsWith("@gadget:")&&GadgetCatalog.extra(id.substring(8)))return GadgetCatalog.title(id.substring(8));
        switch(id){case "@welcome":return "Welcome Center";case "@organize":return "Desktop Organization";case "@quicklaunch":return "Quick Launch";case "@backup":return "Backup and Restore";case "@control":return "Control Panel";case "@system":return "System";case "@filemanagers":return "File Managers";case "@weather":return "Weather";case "@clock":return "Clock";case "@gadgets":return "Gadget Gallery";case "@sticky":return "Notes";case "@calendar":return "Calendar";case "@computer":return "Computer";case "@browser":return BrowserPanel.NAME;case "@photos":return "Photo Gallery";case "@files":return "Files";case "@documents":return "Documents";case "@notes":return "Notepad";case "@appearance":return "Personalize";default:AppCatalog.Entry app=entries.get(id);return app==null?"Unavailable app":app.label;}
    }
    @Override public void open(String id){
        startMenu.hide();if(tray!=null)tray.dismiss();
        if(id.startsWith("setting:")){String destination=id.substring(8);if(destination.startsWith("page:")){open("@control");DesktopWindow control=windows.get("@control");if(control!=null&&control.body instanceof VistaControlPanel)((VistaControlPanel)control.body).navigate(destination.substring(5));}else if(destination.startsWith("open:"))open(destination.substring(5));else vistaAction(destination);return;}
        if(id.startsWith("start-search:")){explorer("@files","search-all");DesktopWindow folder=windows.get("@files");if(folder!=null&&folder.body instanceof VistaExplorer){VistaExplorer view=(VistaExplorer)folder.body;view.navigate("search-all");view.search.setText(Uri.decode(id.substring(13)));}return;}
        if(id.startsWith("web-search:")){open("@browser");DesktopWindow browser=windows.get("@browser");if(browser!=null&&browser.body instanceof BrowserPanel)((BrowserPanel)browser.body).navigate("https://www.bing.com/search?q="+Uri.encode(Uri.decode(id.substring(11))));return;}
        if(id.startsWith("recent-file:")){JSONObject file=RecentFiles.get(store,id.substring(12));if(file!=null)viewDocument(Uri.parse(file.optString("uri")),file.optString("mime"),file.optString("name"),false);return;}
        if(id.startsWith("search-file:")){JSONObject file=LinkedFiles.get(store,id.substring(12));if(file==null){toast("This file link is unavailable. Choose it again in Computer.");return;}viewDocument(Uri.parse(file.optString("uri")),file.optString("mime"));return;}
        if(store.isFolder(id)){organizer(id,"contents:"+id);return;}
        switch(id){
            case "@welcome":if(!restoreTool(id))window(id,"Welcome Center",new WelcomeCenter(this,store,vistaActions));return;
            case "@organize":organizer("@organize","desktop");return;
            case "@quicklaunch":organizer("@quicklaunch","quick");return;
            case "@backup":backupPanel(false);return;
            case "@control":controlPanel("@control","Control Panel","home");return;
            case "@system":controlPanel("@system","System","system");return;
            case "@filemanagers":explorer("@filemanagers","managers");return;
            case "@gadgets":gadgetGallery();return;
            case "@clock":clockOptions();return;
            case "@weather":weatherSettings();return;
            case "@sticky":stickyNote();return;
            case "@calendar":calendar();return;
            case "@documents":explorer("@files","home");DesktopWindow documents=windows.get("@files");if(documents!=null&&documents.body instanceof VistaExplorer)((VistaExplorer)documents.body).openFavorite("documents");return;
            case "@pictures":explorer("@files","home");DesktopWindow pictures=windows.get("@files");if(pictures!=null&&pictures.body instanceof VistaExplorer)((VistaExplorer)pictures.body).openDevice("pictures");return;
            case "@music":explorer("@files","home");DesktopWindow music=windows.get("@files");if(music!=null&&music.body instanceof VistaExplorer)((VistaExplorer)music.body).openFavorite("music");return;
            case "@computer":computer();return;
            case "@notes":notes();return;
            case "@appearance":appearance();return;
            case "@photos":photoGallery();return;
            case "@browser":if(!restoreTool(id))try{window(id,BrowserPanel.NAME,new BrowserPanel(this));}catch(RuntimeException|LinkageError ex){Aero.dialog(this).setTitle("Browser engine unavailable").setMessage("Install or enable Android System WebView, then reopen Nth Internet Explorer.").setPositiveButton("OK",null).show();}return;
            case "@files":explorer("@files","home");return;
        }
        if(id.startsWith("@gadget:")&&GadgetCatalog.extra(id.substring(8))){gadgetPanel(id.substring(8));return;}
        ComponentName component=ComponentName.unflattenFromString(id);
        if(component==null){toast("This shortcut is unavailable. Hold its Start menu entry to remove it.");return;}
        try{
            Intent launch=Intent.makeMainActivity(component).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(launch);ExternalTasks.opened(component.flattenToString());store.launched(component.flattenToString());renderTaskbar();
        }catch(ActivityNotFoundException|SecurityException e){
            Aero.dialog(this).setTitle("App unavailable").setMessage("This app may have been removed or disabled.").setPositiveButton("Remove shortcut",(d,w)->{List<String> ids=store.desktop();ids.remove(id);store.desktop(ids);renderDesktop();}).setNegativeButton("Keep",null).show();scanApps();
        }
    }
    @Override public void options(View anchor,String id){ContextMenus.centered(anchor,(x,y)->shortcutMenu(anchor,id,"desktop",x,y));}
    private void shortcutMenu(View anchor,String id,String source,float x,float y){ShortcutMenus.show(this,anchor,x,y,store,id,source,new ShortcutMenus.Host(){public void open(String value){MainActivity.this.open(value);}public void rename(String value){organizer("@organize","rename:"+value);}public void changed(){desktopChanged();}});}
    private void desktopOptions(float x,float y){
        if(desktopMenu!=null)desktopMenu.dismiss();startMenu.hide();desktopMenu=new DesktopContextMenu(this);
        desktopMenu.item("View  ›","appearance",()->desktopViewMenu(x,y));desktopMenu.item("Sort by  ›","programs",()->desktopSortMenu(x,y));desktopMenu.item("New folder","folderapps",()->organizer("@organize","new"));desktopMenu.item("Paste","paste",!ExplorerClipboard.shortcuts.isEmpty(),()->{try{ExplorerClipboard.paste(store,"desktop");desktopChanged();}catch(IllegalArgumentException ex){toast(ex.getMessage());}});desktopMenu.separator();
        desktopMenu.item("All programs","programs",()->showStart(false));desktopMenu.item("Refresh","refresh",()->{scanApps();sidebar.refresh();});desktopMenu.separator();
        desktopMenu.item("Computer","computer",this::computer);desktopMenu.item("Control Panel","control",()->open("@control"));desktopMenu.item("Gadgets","clock",this::gadgetGallery);desktopMenu.separator();
        desktopMenu.item("Manage desktop shortcuts","computer",this::manageShortcuts);desktopMenu.item("Personalize","appearance",this::appearance);
        if(!isHome()){desktopMenu.separator();desktopMenu.item("Make default home","orb",this::home);}
        desktopMenu.show(root,x,y,shell.getPaddingLeft(),shell.getPaddingTop()+windowTopInset,shell.getPaddingRight(),shell.getPaddingBottom());
    }
    private void manageShortcuts(){organizer("@organize","desktop");}
    private void filesChanged(){if(destroyed)return;for(DesktopWindow tool:windows.values())if(tool.getParent()!=null){if(tool.body instanceof VistaExplorer)((VistaExplorer)tool.body).refreshFiles();else if(tool.body instanceof DesktopOrganizer)((DesktopOrganizer)tool.body).refreshApps();}renderDesktop();renderTaskbar();}
    private void desktopChanged(){
        layoutWorkspace();renderDesktop();renderTaskbar();
        for(Map.Entry<String,DesktopWindow> entry:new ArrayList<>(windows.entrySet())){DesktopWindow window=entry.getValue();if(entry.getKey().startsWith("folder:")&&!store.isFolder(entry.getKey())){if(window.body instanceof VistaShell)((VistaShell)window.body).close();workspace.removeView(window);windows.remove(entry.getKey());continue;}if(window.getParent()==null)continue;if(store.isFolder(entry.getKey()))window.caption(label(entry.getKey()));if(window.body instanceof DesktopOrganizer)((DesktopOrganizer)window.body).refreshApps();}
        renderTaskbar();
    }
    private void quickOptions(View anchor,String id){ContextMenus.centered(anchor,(x,y)->shortcutMenu(anchor,id,"quick",x,y));}
    private void desktopViewMenu(float x,float y){desktopMenu=new DesktopContextMenu(this);String[] names={"Small icons","Medium icons","Large icons"};for(int i=0;i<names.length;i++){final int value=i;desktopMenu.item((store.prefs.getInt("desktop_icon_size",1)==i?"✓  ":"")+names[i],"computer",()->{store.prefs.edit().putInt("desktop_icon_size",value).apply();desktopChanged();});}desktopMenu.show(root,x,y,shell.getPaddingLeft(),shell.getPaddingTop()+windowTopInset,shell.getPaddingRight(),shell.getPaddingBottom());}
    private void desktopSortMenu(float x,float y){desktopMenu=new DesktopContextMenu(this);for(String mode:Arrays.asList("Name","Type"))desktopMenu.item(mode,"programs",()->{List<String> ids=store.desktop();java.text.Collator collator=java.text.Collator.getInstance();ids.sort((a,b)->{if(mode.equals("Type")&&store.isFolder(a)!=store.isFolder(b))return store.isFolder(a)?-1:1;return collator.compare(label(a),label(b));});store.desktop(ids);desktop.clearPositions();desktopChanged();});desktopMenu.show(root,x,y,shell.getPaddingLeft(),shell.getPaddingTop()+windowTopInset,shell.getPaddingRight(),shell.getPaddingBottom());}
    private void startOrbMenu(View anchor,float x,float y){if(desktopMenu!=null)desktopMenu.dismiss();startMenu.hide();if(tray!=null)tray.dismiss();desktopMenu=new DesktopContextMenu(this,"Start button context menu");desktopMenu.item("Open","orb",()->showStart(false));desktopMenu.item("Explore","files",()->open("@files"));desktopMenu.item("Search","search",()->showStart(true));desktopMenu.separator();desktopMenu.item("Properties","control",()->open("setting:page:taskbar"));ContextMenus.show(desktopMenu,anchor,x,y);}
    private void showStart(boolean search){if(tray!=null)tray.dismiss();startMenu.show(search);}
    private void showDesktop(){if(tray!=null)tray.dismiss();((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(workspace.getWindowToken(),0);startMenu.hide();for(DesktopWindow window:windows.values()){window.settle();window.setVisibility(View.GONE);}renderTaskbar();}
    private void window(String id,String title,View body){
        DesktopWindow current=windows.get(id);if(current!=null&&current.getParent()!=null){if(body instanceof VistaShell)((VistaShell)body).close();current.restore();return;}
        DesktopWindow next=new DesktopWindow(this,workspace,title,body,wallpaper,this::renderTaskbar,store,id,layoutKey);next.safeTop(windowTopInset);next.safeRight(sidebarInset());windows.put(id,next);
        if(workspace.getWidth()>0){next.open();renderTaskbar();}else workspace.post(()->{if(!destroyed&&next.getParent()==null){next.open();renderTaskbar();}});
    }
    private void notes(){
        LinearLayout body=Aero.column(this);
        TextView hint=Aero.text(this,"  Saved automatically on this device",12,0xff567082);hint.setPadding(dp(4),dp(8),dp(4),dp(8));body.addView(hint);
        EditText editor=new EditText(this);editor.setTypeface(Aero.face(this));editor.setTextSize(15);editor.setTextColor(Aero.INK);editor.setGravity(Gravity.TOP|Gravity.START);editor.setBackgroundColor(0xfffcfdf7);editor.setPadding(dp(14),dp(12),dp(14),dp(12));editor.setHint("A thought worth keeping…");editor.setContentDescription("Notepad text");editor.setText(store.prefs.getString("note",""));
        editor.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){}public void onTextChanged(CharSequence s,int a,int b,int c){store.prefs.edit().putString("note",s.toString()).apply();}public void afterTextChanged(Editable e){}});
        body.addView(editor,new LinearLayout.LayoutParams(-1,0,1));window("@notes","Notepad",body);
    }
    private final VistaShell.Actions vistaActions=new VistaShell.Actions(){public void open(String id){MainActivity.this.open(id);}public void action(String id){vistaAction(id);}};
    private final DesktopOrganizer.Host organizerHost=new DesktopOrganizer.Host(){public String label(String id){return MainActivity.this.label(id);}public View icon(String id,int size){return MainActivity.this.icon(id,size);}public void changed(){desktopChanged();}public void open(String id){MainActivity.this.open(id);}};
    private void organizer(String id,String page){
        DesktopWindow existing=windows.get(id);if(existing!=null&&existing.getParent()!=null&&existing.body instanceof DesktopOrganizer){existing.restore();((DesktopOrganizer)existing.body).navigate(page);((DesktopOrganizer)existing.body).refreshApps();return;}
        window(id,label(id),new DesktopOrganizer(this,store,vistaActions,organizerHost,()->new ArrayList<>(entries.values()),page));
    }
    private final BackupPanel.Host backupHost=new BackupPanel.Host(){
        public DesktopBackup.Plan pending(){return restorePlan;}public String message(){return backupMessage;}public boolean busy(){return backupBusy;}
        public void exportBackup(){MainActivity.this.exportBackup();}public void importBackup(){MainActivity.this.importBackup();}public void restoreBackup(boolean undo){MainActivity.this.restoreBackup(undo);}public void discardPreview(){restorePlan=null;DesktopBackup.discard(MainActivity.this,DesktopBackup.PENDING);}
    };
    private void backupPanel(boolean preview){
        if(!restoreTool("@backup"))window("@backup","Backup and Restore",new BackupPanel(this,store,vistaActions,backupHost));
        DesktopWindow current=windows.get("@backup");if(current!=null&&current.body instanceof BackupPanel){if(preview)((BackupPanel)current.body).preview();else ((BackupPanel)current.body).refreshApps();}
    }
    private void backupStatus(String message){backupMessage=message;DesktopWindow current=windows.get("@backup");if(current!=null&&current.body instanceof BackupPanel)((BackupPanel)current.body).refreshApps();}
    private void loadRestorePreview(){
        if(!DesktopBackup.file(this,DesktopBackup.PENDING).exists())return;
        worker.execute(()->{try{DesktopBackup.Plan plan=DesktopBackup.staged(this,DesktopBackup.PENDING);main.post(()->{if(destroyed||restoringBackup)return;restorePlan=plan;backupStatus("A selected backup is ready to review.");});}catch(Exception error){DesktopBackup.discard(this,DesktopBackup.PENDING);}});
    }
    private void exportBackup(){
        if(backupBusy)return;backupBusy=true;backupStatus("Preparing your desktop backup…");
        final JSONObject snapshot;try{for(DesktopWindow tool:windows.values())if(tool.getParent()!=null)tool.saveGeometry();snapshot=DesktopBackup.snapshot(store);}catch(JSONException error){backupBusy=false;backupStatus("The backup could not be prepared.");return;}
        worker.execute(()->{try{String text=snapshot.toString(2);DesktopBackup.parse(text);DesktopBackup.stage(this,DesktopBackup.EXPORT,text);main.post(()->{if(destroyed)return;backupBusy=false;backupStatus("Choose where to save your backup.");Intent create=new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/json").putExtra(android.provider.DocumentsContract.EXTRA_INITIAL_URI,android.provider.DocumentsContract.buildDocumentUri("com.android.externalstorage.documents","primary:Documents")).putExtra(Intent.EXTRA_TITLE,"Nth-Desktop-"+new SimpleDateFormat("yyyy-MM-dd-HHmmss",Locale.US).format(new Date())+".nthdesktop.json");try{startActivityForResult(create,20);}catch(ActivityNotFoundException|SecurityException error){DesktopBackup.discard(this,DesktopBackup.EXPORT);backupStatus("No app can save the backup on this device.");}});}catch(Exception error){main.post(()->{if(destroyed)return;backupBusy=false;backupStatus("Backup could not be prepared: "+error.getMessage());});}});
    }
    private void importBackup(){
        if(backupBusy)return;restorePlan=null;DesktopBackup.discard(this,DesktopBackup.PENDING);Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*").putExtra(android.provider.DocumentsContract.EXTRA_INITIAL_URI,android.provider.DocumentsContract.buildDocumentUri("com.android.externalstorage.documents","primary:Documents")).putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"application/json","text/plain","application/octet-stream"}).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try{startActivityForResult(pick,21);}catch(ActivityNotFoundException|SecurityException error){backupStatus("No app can open a backup on this device.");}
    }
    private void backupResult(int request,int result,Intent data){
        if(result!=RESULT_OK||data==null||data.getData()==null){if(request==20)DesktopBackup.discard(this,DesktopBackup.EXPORT);return;}Uri uri=data.getData();backupBusy=true;backupStatus(request==20?"Saving backup…":"Reading backup…");
        worker.execute(()->{try{
            if(request==20){DesktopBackup.Plan plan=DesktopBackup.staged(this,DesktopBackup.EXPORT);try(java.io.OutputStream output=getContentResolver().openOutputStream(uri,"wt")){if(output==null)throw new java.io.IOException("The location is unavailable");output.write(plan.text.getBytes(java.nio.charset.StandardCharsets.UTF_8));output.flush();}DesktopBackup.discard(this,DesktopBackup.EXPORT);main.post(()->{if(destroyed)return;backupBusy=false;backupStatus("Backup saved. Your desktop is unchanged.");});}
            else{DesktopBackup.Plan plan=DesktopBackup.parse(DesktopBackup.read(getContentResolver().openInputStream(uri)));DesktopBackup.stage(this,DesktopBackup.PENDING,plan.text);main.post(()->{if(destroyed)return;backupBusy=false;restorePlan=plan;backupMessage="Review the contents before restoring.";backupPanel(true);});}
        }catch(Exception error){if(request==20)DesktopBackup.discard(this,DesktopBackup.EXPORT);main.post(()->{if(destroyed)return;backupBusy=false;backupStatus((request==20?"Backup could not be saved: ":"Backup was not opened: ")+error.getMessage());backupPanel(false);});}});
    }
    private void restoreBackup(boolean undo){
        if(backupBusy||(!undo&&restorePlan==null))return;backupBusy=true;restoringBackup=true;scanGeneration++;if(weatherLocation!=null)weatherLocation.pause();if(sidebar!=null)sidebar.close();
        restoreCover=new FrameLayout(this);restoreCover.setBackgroundColor(0xee0a263b);restoreCover.setClickable(true);restoreCover.setElevation(dp(100));TextView waiting=Aero.text(this,"Restoring your desktop…",18,Color.WHITE);waiting.setGravity(Gravity.CENTER);restoreCover.addView(waiting,new FrameLayout.LayoutParams(-1,-1));root.addView(restoreCover,new FrameLayout.LayoutParams(-1,-1));DesktopBackup.Plan selected=restorePlan;
        worker.execute(()->{String result;try{DesktopBackup.Plan plan=undo?DesktopBackup.staged(this,DesktopBackup.UNDO):selected;DesktopBackup.apply(this,store,plan,undo);result=undo?"Your previous desktop was recovered.":"Desktop restored. Undo last restore is available in Backup and Restore.";}catch(Exception error){result="Restore did not finish: "+error.getMessage();}String message=result;main.post(()->{if(destroyed)return;getIntent().putExtra("backup_status",message);recreate();});});
    }
    private void computer(){explorer("@computer","computer");}
    private void appearance(){controlPanel("@appearance","Personalization","personalization");}
    private void controlPanel(String id,String title,String page){if(restoreTool(id))return;window(id,title,new VistaControlPanel(this,store,wallpaper,vistaActions,page));}
    private void explorer(String id,String page){
        if(restoreTool(id))return;
        VistaExplorer.Files files=new VistaExplorer.Files(){
            public void caption(String title){DesktopWindow window=windows.get(id);if(window!=null&&window.getParent()!=null){VistaExplorer view=(VistaExplorer)window.body;String kind=view.page().startsWith("folder:")?"files":view.page().equals("search-all")?"search":id.equals("@computer")?"computer":id.equals("@filemanagers")?"folderapps":"files";boolean changed=window.captionIcon(kind);if(!window.caption().equals(title)){window.caption(title);changed=true;}if(changed)renderTaskbar();}}public void chooseFolder(){chooseLinkedFolder(id);}public void requestFolder(Uri initial){chooseLinkedFolder(id,initial);}public void requestDeviceAccess(){StorageAccess.request(MainActivity.this);}public void changed(){filesChanged();}public void linkFile(){chooseSearchFile();}public void pickFile(String kind){pickDocument(kind);}public void openDocument(Uri uri,String mime){viewDocument(uri,mime);}public void openDocument(Uri uri,String mime,String name){viewDocument(uri,mime,name,false);}public void openPicture(Uri uri,String mime,String name,JSONObject context){openPhoto(uri,name,mime,context,id);}public void openWith(Uri uri,String mime,String name){viewDocument(uri,mime,name,true);}public void app(String app){MainActivity.this.open(app);}
        };
        window(id,id.equals("@computer")?"Computer":id.equals("@filemanagers")?"File Managers":"Files",new VistaExplorer(this,store,vistaActions,files,()->new ArrayList<>(entries.values()),page));
    }
    private void vistaAction(String id){
        if(id.startsWith("rename-shortcut:")){organizer("@organize","rename:"+id.substring(16));return;}
        if(id.startsWith("gadget:")&&GadgetCatalog.valid(id.substring(7))){gadgetAction(id.substring(7));return;}
        switch(id){
            case "sidebar-show":sidebar.reveal();renderTaskbar();return;case "sidebar-hide":sidebar.visible(false);renderTaskbar();return;case "sidebar-apply":renderTaskbar();return;
            case "taskbar-refresh":renderTaskbar();return;case "taskbar-usage":ExternalTasks.resetObservation();if(store.prefs.getBoolean("taskbar_usage",false)&&!ExternalTasks.permitted(this))launchSystem(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));scanApps();renderTaskbar();return;
            case "show-desktop":for(DesktopWindow w:windows.values())if(w.getParent()!=null&&w.getVisibility()==View.VISIBLE)w.minimize();startMenu.hide();return;
            case "storage-access":storageSetup.show(true);return;case "glass":wallpaper.appearanceChanged();return;case "startup-preview":if(startup!=null)startup.preview();return;
            case "chrome":applySystemChrome();return;case "desktop":renderDesktop();return;case "gadgets":sidebar.setActive(true);sidebar.refresh();return;case "wallpaper":wallpaper.theme(store.prefs.getInt("wallpaper",0));return;
            case "profile":startMenu.refreshName();return;case "profile-pick":chooseProfilePicture();return;case "home":home();return;case "shortcuts":manageShortcuts();return;case "new-folder":organizer("@organize","new");return;
            case "reset-icons":Aero.dialog(this).setTitle("Restore desktop icons?").setMessage("Restore the original desktop icons and their order? Your notes and apps are kept.").setPositiveButton("Restore",(d,w)->{store.prefs.edit().remove("shortcuts").apply();renderDesktop();}).setNegativeButton("Cancel",null).show();return;
        }
        String action;
        switch(id){
            case "android:wifi":action=Settings.ACTION_WIFI_SETTINGS;break;case "android:network":action=Settings.ACTION_WIRELESS_SETTINGS;break;case "android:bluetooth":action=Settings.ACTION_BLUETOOTH_SETTINGS;break;
            case "android:sound":action=Settings.ACTION_SOUND_SETTINGS;break;case "android:display":action=Settings.ACTION_DISPLAY_SETTINGS;break;case "android:apps":action=Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS;break;
            case "android:defaults":action=Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS;break;case "android:security":action=Settings.ACTION_SECURITY_SETTINGS;break;case "android:privacy":action=Settings.ACTION_PRIVACY_SETTINGS;break;
            case "android:storage":action=Settings.ACTION_INTERNAL_STORAGE_SETTINGS;break;case "android:battery":action=Settings.ACTION_BATTERY_SAVER_SETTINGS;break;case "android:accounts":action=Settings.ACTION_SYNC_SETTINGS;break;
            case "android:accessibility":action=Settings.ACTION_ACCESSIBILITY_SETTINGS;break;case "android:date":action=Settings.ACTION_DATE_SETTINGS;break;case "android:language":action=Settings.ACTION_LOCALE_SETTINGS;break;case "android:device":action=Settings.ACTION_DEVICE_INFO_SETTINGS;break;
            default:action=Settings.ACTION_SETTINGS;
        }
        try{startActivity(new Intent(action));}catch(ActivityNotFoundException|SecurityException ex){launchSystem(new Intent(Settings.ACTION_SETTINGS));}
    }
    private void chooseProfilePicture(){chooseDocument("image/*",15);}
    private void chooseSearchFile(){if(LinkedFiles.list(store).length()>=LinkedFiles.MAX){toast("50 files are already linked. Remove a file link in Computer first.");return;}chooseDocument("*/*",16);}
    private void chooseDocument(String mime,int request){try{startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(mime).putExtra(android.provider.DocumentsContract.EXTRA_INITIAL_URI,android.provider.DocumentsContract.buildDocumentUri("com.android.externalstorage.documents",request==15?"primary:Pictures":"primary:Documents")).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION),request);}catch(ActivityNotFoundException|SecurityException ex){toast("No file picker is available on this device.");}}
    private void pickDocument(String kind){String mime=kind.equals("pictures")?"image/*":kind.equals("music")?"audio/*":kind.equals("videos")?"video/*":"*/*";launchSystem(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),true);}
    private PhotoGallery photoGallery(){
        DesktopWindow existing=windows.get("@photos");if(existing!=null&&existing.getParent()!=null&&existing.body instanceof PhotoGallery){existing.restore();return (PhotoGallery)existing.body;}
        PhotoGallery gallery=new PhotoGallery(this,new PhotoGallery.Host(){public void openPicture(){try{startActivityForResult(new Intent(MainActivity.this,ExplorerPickerActivity.class).setAction(PickerSpec.ACTION).setType("image/*"),304);}catch(RuntimeException ex){toast("The picture picker could not be opened.");}}public void pictures(){explorer("@files","home");DesktopWindow files=windows.get("@files");if(files!=null&&files.body instanceof VistaExplorer)((VistaExplorer)files.body).openDevice("pictures");}public void caption(String title){DesktopWindow window=windows.get("@photos");if(window!=null)window.caption(title);}public void back(JSONObject context,String origin){DesktopWindow photo=windows.get("@photos");if(photo!=null)photo.minimize();String page=context.optString("page");if(!page.startsWith("folder:"))return;String target=Arrays.asList("@computer","@filemanagers","@files").contains(origin)?origin:"@files";DesktopWindow previous=windows.get(target);if(previous==null||previous.getParent()==null)explorer(target,page);else{previous.restore();if(previous.body instanceof VistaExplorer&&!((VistaExplorer)previous.body).page().equals(page))((VistaExplorer)previous.body).navigate(page);}}});window("@photos","Photo Gallery",gallery);return gallery;
    }
    void openPhoto(Uri uri,String name,String mime,JSONObject source,String origin){RecentFiles.opened(store,uri,mime,name);photoGallery().open(uri,name,mime,source,origin);}
    private void viewDocument(Uri uri,String mime){viewDocument(uri,mime,null,false);}
    private void viewDocument(Uri uri,String mime,String name,boolean chooser){
        worker.execute(()->{String display=name;if(display==null||display.isEmpty())try(android.database.Cursor cursor=getContentResolver().query(uri,new String[]{android.provider.OpenableColumns.DISPLAY_NAME},null,null,null)){if(cursor!=null&&cursor.moveToFirst())display=cursor.getString(0);}catch(RuntimeException ignored){}String label=display;main.post(()->{if(!destroyed&&!isFinishing()){RecentFiles.opened(store,uri,mime,label);FileLaunch.open(this,uri,label,mime,chooser);}});});
    }
    private void chooseLinkedFolder(String id){chooseLinkedFolder(id,null);}
    private void chooseLinkedFolder(String id,Uri initial){pendingFolderWindow=id;try{startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).putExtra(DocumentsContract.EXTRA_INITIAL_URI,initial).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION),14);}catch(ActivityNotFoundException|SecurityException ex){toast("No folder picker is available on this device.");}}
    private void afterDesktopReady(Runnable action){if(workspace.isLaidOut()&&workspace.getWidth()>0){action.run();return;}workspace.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener(){public void onGlobalLayout(){workspace.getViewTreeObserver().removeOnGlobalLayoutListener(this);if(!destroyed)action.run();}});}
    private void calendar(){
        startMenu.hide();if(restoreTool("@calendar"))return;LinearLayout content=Aero.column(this);Aero.pad(content,12);
        TextView monthLabel=Aero.text(this,"",17,0xff173a65);monthLabel.setGravity(Gravity.CENTER);TextView date=Aero.text(this,"",12,0xff4a6071);date.setGravity(Gravity.CENTER);
        MonthCalendarView[] ref=new MonthCalendarView[1];MonthCalendarView calendar=new MonthCalendarView(this,()->{monthLabel.setText(ref[0].title());date.setText(java.text.DateFormat.getDateInstance(java.text.DateFormat.FULL,Locale.getDefault()).format(ref[0].selected()));});ref[0]=calendar;
        LinearLayout navigation=new LinearLayout(this);navigation.addView(Aero.button(this,"‹",()->calendar.offset(-1)),new LinearLayout.LayoutParams(dp(44),dp(42)));navigation.addView(monthLabel,new LinearLayout.LayoutParams(0,dp(42),1));navigation.addView(Aero.button(this,"›",()->calendar.offset(1)),new LinearLayout.LayoutParams(dp(44),dp(42)));content.addView(navigation);
        calendar.setMinimumHeight(dp(240));content.addView(calendar,new LinearLayout.LayoutParams(-1,dp(240)));content.addView(date,new LinearLayout.LayoutParams(-1,dp(37)));addAction(content,"Today",calendar::today);monthLabel.setText(calendar.title());date.setText(java.text.DateFormat.getDateInstance(java.text.DateFormat.FULL,Locale.getDefault()).format(calendar.selected()));ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.addView(content);window("@calendar","Calendar",scroll);
    }
    @Override public void home(){
        startMenu.hide();
        if(Build.VERSION.SDK_INT>=29){RoleManager roles=getSystemService(RoleManager.class);if(roles!=null&&roles.isRoleAvailable(RoleManager.ROLE_HOME)&&!roles.isRoleHeld(RoleManager.ROLE_HOME)){try{startActivityForResult(roles.createRequestRoleIntent(RoleManager.ROLE_HOME),12);return;}catch(ActivityNotFoundException ignored){}}}
        launchSystem(new Intent(Settings.ACTION_HOME_SETTINGS));
    }
    @Override public boolean isHome(){
        if(Build.VERSION.SDK_INT>=29){RoleManager roles=getSystemService(RoleManager.class);if(roles!=null&&roles.isRoleAvailable(RoleManager.ROLE_HOME))return roles.isRoleHeld(RoleManager.ROLE_HOME);}
        Intent home=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);android.content.pm.ResolveInfo result=getPackageManager().resolveActivity(home,PackageManager.MATCH_DEFAULT_ONLY);return result!=null&&getPackageName().equals(result.activityInfo.packageName);
    }
    @Override public void settings(){open("@control");}
    private void launchSystem(Intent intent){launchSystem(intent,false);}
    private void launchSystem(Intent intent,boolean result){
        try{if(result)startActivityForResult(intent,11);else startActivity(intent);}catch(ActivityNotFoundException|SecurityException ex){toast("No app is available for that action on this device.");}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==304){if(result==RESULT_OK&&data!=null&&data.getData()!=null)openPhoto(data.getData(),null,"image/*",null,"@files");return;}
        DesktopWindow browser=windows.get("@browser");if(browser!=null&&browser.getParent()!=null&&browser.body instanceof BrowserPanel&&((BrowserPanel)browser.body).result(request,result,data))return;
        if((request==15||request==16)&&result==RESULT_OK&&data!=null&&data.getData()!=null){
            Uri uri=data.getData();if(!"content".equals(uri.getScheme())){toast("Choose a file from the document picker.");return;}
            if(request==16)try{int flags=data.getFlags()&Intent.FLAG_GRANT_READ_URI_PERMISSION;if(flags==0)throw new SecurityException();getContentResolver().takePersistableUriPermission(uri,flags);}catch(SecurityException ex){toast("This provider cannot keep file access. Choose another file.");return;}
            worker.execute(()->{try{if(request==15){String portrait=ProfilePortrait.importPicture(getApplicationContext(),uri);store.prefs.edit().putString("profile_portrait",portrait).apply();}else{JSONObject file=LinkedFiles.describe(getApplicationContext(),uri);if(!LinkedFiles.add(store,file))throw new IllegalStateException("50 files are already linked");}main.post(()->{if(destroyed||restoringBackup)return;startMenu.refreshName();for(DesktopWindow tool:windows.values())if(tool.getParent()!=null&&tool.body instanceof VistaControlPanel)((VistaControlPanel)tool.body).render();else if(tool.getParent()!=null&&tool.body instanceof VistaExplorer)((VistaExplorer)tool.body).render();toast(request==15?"Profile picture updated":"File added to Start search");});}catch(Exception ex){main.post(()->{if(!destroyed)toast(request==15?"This picture could not be opened. Choose another picture.":"This file could not be linked. Try choosing it again.");});}});return;
        }
        if(request==20||request==21){backupResult(request,result,data);return;}
        if(request==14&&result==RESULT_OK&&data!=null&&data.getData()!=null){
            Uri tree=data.getData();String target=pendingFolderWindow;int flags=data.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            try{if((flags&Intent.FLAG_GRANT_READ_URI_PERMISSION)==0||!android.provider.DocumentsContract.isTreeUri(tree))throw new SecurityException();getContentResolver().takePersistableUriPermission(tree,flags);}catch(SecurityException ex){toast("This provider couldn't keep folder access. Please choose another folder.");return;}
            worker.execute(()->{String label;try{label=DocumentFolders.rootName(this,tree);}catch(RuntimeException ex){label="Linked folder";}String name=label;main.post(()->{if(destroyed||restoringBackup)return;if(!DocumentFolders.add(store,tree,name)){try{getContentResolver().releasePersistableUriPermission(tree,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(SecurityException ignored){}toast("16 folders are already linked. Remove a folder link before adding another.");return;}afterDesktopReady(()->{explorer(target,target.equals("@computer")?"computer":target.equals("@filemanagers")?"managers":"home");DesktopWindow tool=windows.get(target);if(tool!=null&&tool.body instanceof VistaExplorer)((VistaExplorer)tool.body).folderAdded(tree.toString(),name);});});});return;
        }
        if(request==13&&result==RESULT_OK&&data!=null){
            List<String> selected=new ArrayList<>();if(data.getClipData()!=null){for(int i=0;i<Math.min(12,data.getClipData().getItemCount());i++)rememberPhoto(data.getClipData().getItemAt(i).getUri(),selected);}else if(data.getData()!=null)rememberPhoto(data.getData(),selected);
            if(!selected.isEmpty()){store.photos(selected);List<String> widgets=store.gadgets();if(!widgets.contains("photo")){widgets.add("photo");store.gadgets(widgets);sidebar.rebuild();sidebar.reveal();}sidebar.photosChanged();toast(selected.size()==1?"Photo frame updated":"Slideshow updated");}return;
        }
        if(request==11&&result==RESULT_OK&&data!=null&&data.getData()!=null){
            Uri uri=data.getData();String mime;try{mime=getContentResolver().getType(uri);}catch(RuntimeException ex){mime=null;}viewDocument(uri,mime);
        }
    }
    private void paragraph(LinearLayout parent,String text){TextView p=Aero.text(this,text,14,0xff476071);p.setLineSpacing(dp(3),1);p.setPadding(0,dp(12),0,dp(12));parent.addView(p);}
    private void addAction(LinearLayout parent,String label,Runnable action){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(8);parent.addView(Aero.button(this,label,action),lp);}
    private int dp(int value){return Aero.dp(this,value);}
    private void toast(String message){Toast.makeText(this,message,Toast.LENGTH_SHORT).show();}
    private void updateClock(){if(clock!=null){Date now=new Date();clock.setText(DateFormat.getTimeFormat(this).format(now)+"\n"+new SimpleDateFormat("MMM d",Locale.getDefault()).format(now));}}
    private void handleBack(){
        if(restoringBackup)return;
        if(videoOverlay!=null){DesktopWindow browser=windows.get("@browser");if(browser!=null&&browser.body instanceof BrowserPanel){((BrowserPanel)browser.body).exitFullScreen();return;}}
        if(startup!=null&&startup.skip())return;
        if(tray!=null&&tray.dismiss())return;
        if(desktopMenu!=null&&desktopMenu.showing()){desktopMenu.dismiss();return;}
        if(startMenu.getVisibility()==View.VISIBLE){startMenu.hide();return;}
        for(int i=workspace.getChildCount()-1;i>=0;i--){View v=workspace.getChildAt(i);if(v instanceof DesktopWindow&&v.getVisibility()==View.VISIBLE){DesktopWindow tool=(DesktopWindow)v;if(tool.body instanceof PhotoGallery&&((PhotoGallery)tool.body).back())return;if(tool.body instanceof BrowserPanel&&((BrowserPanel)tool.body).goBack())return;if(tool.body instanceof VistaShell&&((VistaShell)tool.body).goBack())return;tool.minimize();((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(),0);renderTaskbar();return;}}
    }
    @Override public void onBackPressed(){handleBack();}
    private void handleRequestedTool(){String tool=getIntent().getStringExtra("open_tool");if(tool==null)return;getIntent().removeExtra("open_tool");if(Arrays.asList("@files","@computer","@control","@appearance","@system","@filemanagers","@notes","@gadgets","@backup","@browser","@photos").contains(tool))afterDesktopReady(()->open(tool));}
    @Override public void onWindowFocusChanged(boolean focused){super.onWindowFocusChanged(focused);if(focused&&store!=null&&!destroyed)applySystemChrome();if(focused&&startup!=null)root.post(startup::maybeShow);}
    void storageReady(){root.post(()->{if(!destroyed&&!restoringBackup&&hasWindowFocus()){boolean force=restoreStorageSetup;restoreStorageSetup=false;storageSetup.show(force);}});}
    void storageFolderSetup(){afterDesktopReady(()->{explorer("@computer","computer");chooseLinkedFolder("@computer");});}
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);DesktopWindow currentBrowser=windows.get("@browser");if(currentBrowser!=null&&currentBrowser.body instanceof BrowserPanel)((BrowserPanel)currentBrowser.body).exitFullScreen();handleRequestedTool();if(desktopMenu!=null)desktopMenu.dismiss();if(tray!=null)tray.dismiss();if(startMenu!=null){startMenu.hide();startMenu.refreshHome();}}
    @Override protected void onResume(){super.onResume();if(restoringBackup)return;boolean access=StorageAccess.has(this);if(storageAllowed!=access){storageAllowed=access;filesChanged();}pausedSession=null;if(startMenu!=null)startMenu.refreshHome();main.removeCallbacks(clockTick);main.post(clockTick);if(store!=null)scanApps();if(sidebar!=null)sidebar.setActive(true);if(startup!=null)startup.resume();if(tray!=null)tray.start();if(weatherLocation!=null)weatherLocation.resume();for(DesktopWindow tool:windows.values()){if(tool.body instanceof GadgetPanel)((GadgetPanel)tool.body).active(true);if(tool.body instanceof BrowserPanel)((BrowserPanel)tool.body).active(true);if(tool.body instanceof PhotoGallery)((PhotoGallery)tool.body).active(true);}}
    @Override protected void onPause(){for(DesktopWindow tool:windows.values()){if(tool.body instanceof PhotoGallery)((PhotoGallery)tool.body).active(false);if(tool.body instanceof BrowserPanel){((BrowserPanel)tool.body).save();((BrowserPanel)tool.body).active(false);}if(tool.body instanceof GadgetPanel)((GadgetPanel)tool.body).active(false);if(tool.body instanceof VistaExplorer)((VistaExplorer)tool.body).dismissMenus();}if(tray!=null)tray.stop();if(startup!=null)startup.pause();pausedSession=restoringBackup?null:captureSession();if(desktopMenu!=null)desktopMenu.dismiss();super.onPause();main.removeCallbacks(clockTick);if(sidebar!=null)sidebar.setActive(false);if(weatherLocation!=null)weatherLocation.pause();if(startMenu!=null)startMenu.hide();}
    @Override public void onConfigurationChanged(android.content.res.Configuration config){
        super.onConfigurationChanged(config);String next=config.orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE?"landscape":"portrait";
        if(!next.equals(layoutKey)){for(DesktopWindow tool:windows.values())if(tool.getParent()!=null){tool.settle();tool.orientation(next);}layoutKey=next;desktop.orientation(next);sidebar.orientation(next);}
        if(startup!=null){startup.pause();startup.resume();}if(desktopMenu!=null)desktopMenu.dismiss();if(tray!=null)tray.dismiss();pausedSession=null;applySystemChrome();workspace.post(()->{layoutWorkspace();renderTaskbar();});
    }
    @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);state.putBoolean("storage_setup_open",storageSetup!=null&&storageSetup.showing());state.putString("folder_window",pendingFolderWindow);if(!restoringBackup)state.putString("desktop_session",(pausedSession==null?captureSession():pausedSession).toString());}
    private JSONObject captureSession(){
        JSONObject session=new JSONObject();JSONArray tools=new JSONArray();
        if(workspace==null)return session;
        for(int i=0;i<workspace.getChildCount();i++)if(workspace.getChildAt(i) instanceof DesktopWindow){
            DesktopWindow tool=(DesktopWindow)workspace.getChildAt(i);tool.saveGeometry();JSONObject item=new JSONObject();GadgetLayout.put(item,"id",tool.toolId);GadgetLayout.put(item,"visible",tool.getVisibility()==View.VISIBLE&&!tool.minimizing());GadgetLayout.put(item,"geometry",tool.geometry());GadgetLayout.put(item,"ui",ToolViewState.capture(tool));tools.put(item);
        }
        GadgetLayout.put(session,"tools",tools);GadgetLayout.put(session,"start",startMenu.state());GadgetLayout.put(session,"desktopScroll",desktopScroll.getScrollY());return session;
    }
    private void restoreSession(String saved){
        if(destroyed||saved==null)return;
        try{
            JSONObject session=new JSONObject(saved);JSONArray tools=session.optJSONArray("tools");
            if(tools!=null)for(int i=0;i<tools.length();i++){
                JSONObject item=tools.optJSONObject(i);if(item==null)continue;String id=item.optString("id");
                if(!(id.startsWith("@gadget:")&&GadgetCatalog.extra(id.substring(8)))&&!Arrays.asList("@notes","@sticky","@calendar","@gadgets","@clock","@weather","@appearance","@computer","@control","@system","@files","@filemanagers","@organize","@quicklaunch","@backup","@welcome","@browser","@photos").contains(id)&&!store.isFolder(id))continue;
                open(id);DesktopWindow tool=windows.get(id);if(tool==null)continue;tool.readGeometry(item.optJSONObject("geometry"));tool.setVisibility(item.optBoolean("visible",true)?View.VISIBLE:View.GONE);ToolViewState.restore(tool,item.optJSONObject("ui"));
            }
            desktopScroll.post(()->desktopScroll.scrollTo(0,session.optInt("desktopScroll")));startMenu.restore(session.optJSONObject("start"));renderTaskbar();
        }catch(JSONException ignored){}
    }
    @Override protected void onDestroy(){destroyed=true;if(storageSetup!=null)storageSetup.close();if(weatherLocation!=null)weatherLocation.close();if(tray!=null)tray.stop();if(startup!=null)startup.close();main.removeCallbacksAndMessages(null);worker.shutdownNow();for(DesktopWindow tool:windows.values()){if(tool.body instanceof VistaShell)((VistaShell)tool.body).close();if(tool.body instanceof WeatherPanel)((WeatherPanel)tool.body).close();if(tool.body instanceof BrowserPanel)((BrowserPanel)tool.body).close();if(tool.body instanceof PhotoGallery)((PhotoGallery)tool.body).close();}if(sidebar!=null)sidebar.close();if(weatherLocation!=null)weatherLocation.pause();if(receiverRegistered){unregisterReceiver(packages);unregisterReceiver(timeChanged);}if(Build.VERSION.SDK_INT>=33&&backCallback!=null)getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(backCallback);super.onDestroy();}

    void enterBrowserFullscreen(View video){
        startMenu.hide();if(tray!=null)tray.dismiss();if(desktopMenu!=null)desktopMenu.dismiss();((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(root.getWindowToken(),0);
        if(video.getParent() instanceof ViewGroup)((ViewGroup)video.getParent()).removeView(video);videoOverlay=new FrameLayout(this);videoOverlay.setBackgroundColor(Color.BLACK);videoOverlay.setContentDescription("Fullscreen video");videoOverlay.setElevation(dp(64));videoOverlay.setFocusableInTouchMode(true);videoOverlay.setClickable(true);
        videoOverlay.setOnApplyWindowInsetsListener((v,insets)->{int l=0,t=0,r=0,b=0;if(Build.VERSION.SDK_INT>=28&&insets.getDisplayCutout()!=null){android.view.DisplayCutout cutout=insets.getDisplayCutout();l=cutout.getSafeInsetLeft();t=cutout.getSafeInsetTop();r=cutout.getSafeInsetRight();b=cutout.getSafeInsetBottom();}v.setPadding(l,t,r,b);return insets;});
        videoOverlay.addView(video,new FrameLayout.LayoutParams(-1,-1));previousShellAccessibility=shell.getImportantForAccessibility();shell.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);root.addView(videoOverlay,new FrameLayout.LayoutParams(-1,-1));applySystemChrome();videoOverlay.requestApplyInsets();video.requestFocus();
    }
    void exitBrowserFullscreen(View video){if(video.getParent() instanceof ViewGroup)((ViewGroup)video.getParent()).removeView(video);if(videoOverlay!=null){root.removeView(videoOverlay);videoOverlay=null;shell.setImportantForAccessibility(previousShellAccessibility);}applySystemChrome();}
    private void applySystemChrome(){
        boolean full=videoOverlay!=null,show=!full&&store.prefs.getBoolean("show_status_bar",false);
        if(Build.VERSION.SDK_INT>=30){WindowInsetsController controller=getWindow().getInsetsController();if(controller!=null){controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);if(full)controller.hide(WindowInsets.Type.systemBars());else{controller.show(WindowInsets.Type.navigationBars());if(show)controller.show(WindowInsets.Type.statusBars());else controller.hide(WindowInsets.Type.statusBars());}}}
        else{int flags=View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;if(!show)flags|=View.SYSTEM_UI_FLAG_FULLSCREEN;if(full)flags|=View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;getWindow().getDecorView().setSystemUiVisibility(flags);}
        if(shell!=null)shell.requestApplyInsets();
    }
    @Override public void sidebarChanged(){workspace.requestLayout();renderTaskbar();}
    @Override public void gallery(){gadgetGallery();}
    @Override public void readingsChanged(){updateWeatherSummary();for(DesktopWindow tool:windows.values())if(tool.body instanceof GadgetPanel&&tool.getParent()!=null)((GadgetPanel)tool.body).readings();}
    @Override public void gadgetAction(String id){
        startMenu.hide();
        switch(id){
            case "sidebar-properties":open("setting:page:sidebar");break;
            case "clock":clockOptions();break;case "calendar":calendar();break;case "weather":weatherSettings();break;
            default:if(GadgetCatalog.extra(id))gadgetPanel(id);break;
            case "notes":stickyNote();break;case "photo":choosePhotos();break;
            case "meters":Aero.dialog(this).setTitle("System Meter").setMessage("The left dial shows the device's battery level. The right dial shows the percentage of physical memory currently in use. These are live Android readings.").setPositiveButton("OK",null).show();break;
        }
    }
    private boolean restoreTool(String id){DesktopWindow current=windows.get(id);if(current!=null&&current.getParent()!=null){current.restore();return true;}return false;}
    private void gadgetGallery(){startMenu.hide();if(restoreTool("@gadgets"))return;window("@gadgets","Desktop Gadget Gallery",new GadgetGallery(this,store,sidebar.weather,new GadgetGallery.Host(){public void open(String id){MainActivity.this.open(id);}public void action(String id){vistaAction(id);}public void changed(){sidebar.rebuild();}public void reveal(){sidebar.reveal();}}));}
    private void gadgetPanel(String kind){if(!GadgetCatalog.extra(kind))return;String id="@gadget:"+kind;if(restoreTool(id))return;window(id,GadgetCatalog.title(kind),new GadgetPanel(this,store,kind,sidebar.feeds,sidebar.hardware,new GadgetPanel.Host(){public void open(String id){MainActivity.this.open(id);}public void action(String id){vistaAction(id);}public void changed(){sidebar.refresh();}public void feed(String url){List<String> enabled=store.gadgets();if(!enabled.contains("feeds")){enabled.add("feeds");store.gadgets(enabled);sidebar.rebuild();sidebar.reveal();}sidebar.feeds.configure(url);}public void refreshFeed(){sidebar.feeds.refresh(true);}public void article(String url){Uri uri=Uri.parse(url);if(Arrays.asList("http","https").contains(uri.getScheme()))launchSystem(new Intent(Intent.ACTION_VIEW,uri));}}));}
    private void clockOptions(){controlPanel("@clock","Clock settings","clock");}
    private void stickyNote(){
        if(restoreTool("@sticky"))return;LinearLayout body=Aero.column(this);EditText editor=new EditText(this);editor.setTypeface(Aero.face(this));editor.setTextSize(16);editor.setTextColor(0xff5c4a1f);editor.setGravity(Gravity.TOP|Gravity.START);editor.setBackgroundColor(0xffffe889);Aero.pad(editor,14);editor.setHint("A little note…");editor.setContentDescription("Sticky note text");editor.setText(store.prefs.getString("gadget_note",""));editor.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){}public void onTextChanged(CharSequence s,int a,int b,int c){store.prefs.edit().putString("gadget_note",s.toString()).apply();sidebar.refresh();}public void afterTextChanged(Editable e){}});body.addView(editor,new LinearLayout.LayoutParams(-1,-1));window("@sticky","Notes",body);
    }
    private void choosePhotos(){
        startMenu.hide();Intent choose=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("image/*").putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);try{startActivityForResult(choose,13);}catch(ActivityNotFoundException e){toast("No photo picker is available on this device.");}
    }
    private void rememberPhoto(Uri uri,List<String> selected){try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);if(!selected.contains(uri.toString()))selected.add(uri.toString());}catch(SecurityException ignored){}}
    private void weatherSettings(){
        List<String> widgets=store.gadgets();if(!widgets.contains("weather")){widgets.add("weather");store.gadgets(widgets);sidebar.rebuild();sidebar.reveal();}
        if(restoreTool("@weather"))return;
        WeatherPanel panel=new WeatherPanel(this,store,sidebar.weather,weatherLocation,new WeatherPanel.Host(){public void locate(){locateWeather();}public void settings(){launchSystem(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));}public void follow(boolean enabled){store.prefs.edit().putBoolean("weather_follow",enabled).apply();if(enabled)locateWeather();else weatherLocation.stopFollowing();updateWeatherSummary();}public void refresh(){sidebar.weather.refresh(true);sidebar.refresh();updateWeatherSummary();}});window("@weather","Weather settings",panel);
    }
    private void updateWeatherSummary(){DesktopWindow current=windows.get("@weather");if(current!=null&&current.body instanceof WeatherPanel)((WeatherPanel)current.body).refresh();}
    private void locateWeather(){if(weatherLocation==null)return;if(!weatherLocation.permitted()){requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},42);return;}weatherLocation.locate();}
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] grants){super.onRequestPermissionsResult(request,permissions,grants);DesktopWindow browser=windows.get("@browser");if(browser!=null&&browser.getParent()!=null&&browser.body instanceof BrowserPanel&&((BrowserPanel)browser.body).permissionResult(request))return;if(request==StorageAccess.REQUEST){boolean access=StorageAccess.has(this);storageAllowed=access;filesChanged();if(!access)toast("File access was not enabled. You can still connect individual folders.");return;}if(request==42){if(grants.length>0&&grants[0]==android.content.pm.PackageManager.PERMISSION_GRANTED)weatherLocation.locate();else weatherLocation.denied();}}
}
