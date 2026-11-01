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
import org.json.*;

public final class MainActivity extends Activity implements StartMenu.Actions,GadgetSidebar.Actions {
    private DesktopStore store;
    private String layoutKey;
    private JSONObject pausedSession;
    private String pendingFolderWindow="@files";
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Map<String,AppCatalog.Entry> entries=new LinkedHashMap<>();
    private final Map<String,DesktopWindow> windows=new LinkedHashMap<>();
    private FrameLayout root,workspace;
    private GridLayout desktop;
    private ScrollView desktopScroll;
    private GadgetSidebar sidebar;
    private DesktopContextMenu desktopMenu;
    private int desktopRows,desktopCutoutLeft,desktopCutoutRight;
    private android.location.LocationListener locationListener;
    private android.os.CancellationSignal locationCancellation;
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
        super.onCreate(saved);store=new DesktopStore(this);layoutKey=getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE?"landscape":"portrait";
        if(saved!=null)pendingFolderWindow=saved.getString("folder_window","@files");
        store.migrateDesktop();
        getWindow().setStatusBarColor(Color.TRANSPARENT);getWindow().setNavigationBarColor(0xff0a263b);
        if(Build.VERSION.SDK_INT>=28){WindowManager.LayoutParams attributes=getWindow().getAttributes();attributes.layoutInDisplayCutoutMode=Build.VERSION.SDK_INT>=30?WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS:WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;getWindow().setAttributes(attributes);}
        if(Build.VERSION.SDK_INT>=30)getWindow().setDecorFitsSystemWindows(false);
        else getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        buildDesktop();applySystemChrome();
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
        receiverRegistered=true;scanApps();
    }
    private void buildDesktop(){
        root=new FrameLayout(this);wallpaper=new AeroWallpaper(this,store.prefs.getInt("wallpaper",0));root.addView(wallpaper,new FrameLayout.LayoutParams(-1,-1));
        shell=Aero.column(this);root.addView(shell,new FrameLayout.LayoutParams(-1,-1));
        shell.setOnApplyWindowInsetsListener((v,insets)->{
            int left,top,right,bottom;
            if(Build.VERSION.SDK_INT>=30){android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars());android.graphics.Insets cutout=insets.getInsets(WindowInsets.Type.displayCutout());android.graphics.Insets ime=insets.getInsets(WindowInsets.Type.ime());desktopCutoutLeft=Math.max(0,cutout.left-bars.left);desktopCutoutRight=Math.max(0,cutout.right-bars.right);left=bars.left;top=Math.max(bars.top,cutout.top);right=bars.right;bottom=Math.max(Math.max(bars.bottom,cutout.bottom),ime.bottom);}
            else{left=insets.getSystemWindowInsetLeft();top=insets.getSystemWindowInsetTop();right=insets.getSystemWindowInsetRight();bottom=insets.getSystemWindowInsetBottom();}
            shell.setPadding(left,top,right,bottom);if(workspace!=null)workspace.post(this::layoutWorkspace);return insets;
        });
        workspace=new FrameLayout(this);shell.addView(workspace,new LinearLayout.LayoutParams(-1,0,1));
        ScrollView scroll=new ScrollView(this);desktopScroll=scroll;scroll.setFillViewport(true);scroll.setClipToPadding(false);scroll.setPadding(dp(7),dp(10),dp(7),dp(12));scroll.setVerticalScrollBarEnabled(false);
        desktop=new GridLayout(this);desktop.setOrientation(GridLayout.VERTICAL);desktop.setAlignmentMode(GridLayout.ALIGN_BOUNDS);scroll.addView(desktop,new ScrollView.LayoutParams(-1,-2));workspace.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
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
        if(destroyed||workspace==null||workspace.getWidth()==0)return;int width=workspace.getWidth(),height=workspace.getHeight();
            int reserved=sidebar.getVisibility()==View.VISIBLE?dp(144):desktopCutoutRight;
            FrameLayout.LayoutParams desktopParams=(FrameLayout.LayoutParams)desktopScroll.getLayoutParams();
            if(desktopParams.rightMargin!=reserved||desktopParams.leftMargin!=desktopCutoutLeft){desktopParams.rightMargin=reserved;desktopParams.leftMargin=desktopCutoutLeft;desktopScroll.setLayoutParams(desktopParams);}
            int next=Math.max(1,Math.min(9,(width-reserved-desktopCutoutLeft-dp(12))/dp(82)));
            int rows=Math.max(1,(height-dp(16))/dp(94));
            if(next!=columns||rows!=desktopRows){columns=next;desktopRows=rows;renderDesktop();}
            sidebar.workspaceSize(width,height);
            for(DesktopWindow tool:windows.values())tool.fitToHost();
            if(startMenu!=null){startMenu.availableHeight(height);FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)startMenu.getLayoutParams();int h=Math.min(dp(540),Math.max(1,height-dp(8)));int w=Math.min(dp(420),Math.max(1,width-dp(12)));if(lp.height!=h||lp.width!=w){lp.height=h;lp.width=w;startMenu.setLayoutParams(lp);}}
    }
    private void buildTaskbar(){
        shell.setClipChildren(false);shell.setClipToPadding(false);
        LinearLayout taskbar=new LinearLayout(this);taskbar.setContentDescription("Desktop taskbar");taskbar.setGravity(Gravity.CENTER_VERTICAL);taskbar.setClipChildren(false);taskbar.setPadding(dp(3),0,dp(3),0);taskbar.setBackground(new AeroGlass(wallpaper,AeroGlass.TASKBAR));taskbar.setElevation(dp(5));
        AeroIcon orb=new AeroIcon(this,"orb");orb.setContentDescription("Start menu");orb.setFocusable(true);orb.setOnClickListener(v->{if(startMenu.getVisibility()==View.VISIBLE)startMenu.hide();else showStart(false);});taskbar.addView(orb,new LinearLayout.LayoutParams(dp(53),dp(53)));
        HorizontalScrollView strip=new HorizontalScrollView(this);strip.setHorizontalScrollBarEnabled(false);strip.setFillViewport(true);taskItems=new LinearLayout(this);taskItems.setGravity(Gravity.CENTER_VERTICAL);strip.addView(taskItems,new HorizontalScrollView.LayoutParams(-2,-1));FrameLayout taskClip=new FrameLayout(this);taskClip.setClipChildren(true);taskClip.addView(strip,new FrameLayout.LayoutParams(-1,-1));taskbar.addView(taskClip,new LinearLayout.LayoutParams(0,dp(44),1));
        clock=Aero.text(this,"",10,Color.WHITE);clock.setGravity(Gravity.CENTER);clock.setPadding(dp(5),0,dp(5),0);clock.setMinWidth(dp(67));clock.setOnClickListener(v->calendar());clock.setContentDescription("Clock and calendar");taskbar.addView(clock,new LinearLayout.LayoutParams(-2,dp(44)));
        TextView peek=Aero.text(this,"",10,Color.WHITE);peek.setBackground(Aero.gloss(this,new int[]{0x668eaec0,0x554c697f,0x66314353},0,0x667f98ab));peek.setContentDescription("Show desktop");peek.setOnClickListener(v->showDesktop());taskbar.addView(peek,new LinearLayout.LayoutParams(dp(12),dp(44)));shell.addView(taskbar,new LinearLayout.LayoutParams(-1,dp(46)));renderTaskbar();
    }
    private void scanApps(){
        if(destroyed)return;if(scanPending){scanAgain=true;return;}scanPending=true;int generation=++scanGeneration;
        worker.execute(()->{
            List<AppCatalog.Entry> apps;
            try{apps=AppCatalog.load(getApplicationContext());}catch(RuntimeException e){apps=new ArrayList<>();}
            List<AppCatalog.Entry> result=apps;
            main.post(()->{if(destroyed||generation!=scanGeneration)return;scanPending=false;entries.clear();for(AppCatalog.Entry e:result)entries.put(e.id,e);startMenu.entries(result);renderDesktop();renderTaskbar();for(DesktopWindow tool:windows.values())if(tool.getParent()!=null&&tool.body instanceof VistaShell)((VistaShell)tool.body).refreshApps();if(scanAgain){scanAgain=false;scanApps();}});
        });
    }
    private void renderDesktop(){
        if(desktop==null)return;desktop.removeAllViews();List<String> ids=store.desktop();int cols=Math.max(1,columns);int rows=Math.max(Math.max(1,desktopRows),(ids.size()+cols-1)/cols);desktop.setRowCount(rows);desktop.setColumnCount(cols);
        for(int index=0;index<ids.size();index++){
            String id=ids.get(index);LinearLayout tile=Aero.column(this);tile.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);tile.setPadding(dp(2),dp(6),dp(2),dp(3));View icon=icon(id,46);tile.addView(icon,new LinearLayout.LayoutParams(dp(46),dp(46)));
            TextView label=Aero.text(this,label(id),11,Color.WHITE);label.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);label.setMaxLines(2);label.setEllipsize(TextUtils.TruncateAt.END);label.setShadowLayer(dp(3),0,dp(1),0xff000000);tile.addView(label,new LinearLayout.LayoutParams(-1,-2));
            android.graphics.drawable.StateListDrawable background=new android.graphics.drawable.StateListDrawable();background.addState(new int[]{android.R.attr.state_pressed},Aero.panel(this,0x44d0edff,0x2270add0,3,0x779fcde4));background.addState(new int[]{},Aero.panel(this,0x00ffffff,0x00ffffff,3,0));tile.setBackground(background);tile.setContentDescription(label(id)+". Hold and drag to swap shortcut positions.");tile.setOnClickListener(v->open(id));tile.setFocusable(true);tile.setOnLongClickListener(v->!store.prefs.getBoolean("layout_locked",false)&&v.startDragAndDrop(ClipData.newPlainText("desktop-shortcut",id),new View.DragShadowBuilder(v),id,0));
            tile.setOnDragListener((v,event)->{boolean own=event.getLocalState() instanceof String&&!((String)event.getLocalState()).startsWith("gadget:");switch(event.getAction()){
                case DragEvent.ACTION_DRAG_STARTED:return own;case DragEvent.ACTION_DRAG_ENTERED:if(own)v.setAlpha(.5f);return own;
                case DragEvent.ACTION_DRAG_EXITED:case DragEvent.ACTION_DRAG_ENDED:v.setAlpha(1);return own;
                case DragEvent.ACTION_DROP:if(!own||store.prefs.getBoolean("layout_locked",false))return false;store.desktop(LauncherState.move(store.desktop(),(String)event.getLocalState(),id));desktop.post(this::renderDesktop);return true;default:return own;}});
            GridLayout.LayoutParams lp=new GridLayout.LayoutParams(GridLayout.spec(index%rows),GridLayout.spec(index/rows));lp.width=dp(80);lp.height=dp(94);desktop.addView(tile,lp);
        }
    }
    private void renderTaskbar(){
        if(taskItems==null)return;taskItems.removeAllViews();
        for(String id:new String[]{"@browser","@files","@notes"}){FrameLayout quick=new FrameLayout(this);FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(dp(24),dp(24),Gravity.CENTER);quick.addView(icon(id,24),lp);quick.setContentDescription("Open "+label(id));quick.setOnClickListener(v->open(id));taskItems.addView(quick,new LinearLayout.LayoutParams(dp(36),dp(42)));}
        View divider=new View(this);divider.setBackgroundColor(0x778fa4b0);LinearLayout.LayoutParams rule=new LinearLayout.LayoutParams(dp(1),dp(27));rule.setMargins(dp(4),0,dp(5),0);taskItems.addView(divider,rule);
        for(Map.Entry<String,DesktopWindow> item:windows.entrySet()){
            if(item.getValue().getParent()==null)continue;TextView b=Aero.text(this,label(item.getKey()),11,Color.WHITE);b.setGravity(Gravity.CENTER);b.setSingleLine(true);b.setEllipsize(TextUtils.TruncateAt.END);b.setPadding(dp(10),0,dp(10),0);b.setBackground(Aero.gloss(this,new int[]{0xbb7a98aa,0x997b9bb1,0x88445e74,0x99657d8f},3,0x99b4cad7));b.setContentDescription("Restore "+label(item.getKey()));b.setOnClickListener(v->{startMenu.hide();item.getValue().restore();});LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(100),dp(35));lp.rightMargin=dp(3);taskItems.addView(b,lp);
        }
        for(String id:store.recents()){if(!entries.containsKey(id))continue;FrameLayout slot=new FrameLayout(this);slot.addView(icon(id,25),new FrameLayout.LayoutParams(dp(25),dp(25),Gravity.CENTER));slot.setContentDescription("Recently launched: "+label(id));slot.setOnClickListener(v->open(id));taskItems.addView(slot,new LinearLayout.LayoutParams(dp(36),dp(42)));}
    }
    private View icon(String id,int size){
        AppCatalog.Entry entry=entries.get(id);
        if(entry!=null){ImageView image=new ImageView(this);image.setImageDrawable(entry.icon);image.setScaleType(ImageView.ScaleType.FIT_CENTER);image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);return image;}
        return new AeroIcon(this,id.equals("@filemanagers")?"folderapps":id.startsWith("@")?id.substring(1):"apps");
    }
    private String label(String id){
        switch(id){case "@control":return "Control Panel";case "@system":return "System";case "@filemanagers":return "File Managers";case "@weather":return "Weather";case "@clock":return "Clock";case "@gadgets":return "Gadget Gallery";case "@sticky":return "Notes";case "@calendar":return "Calendar";case "@computer":return "Computer";case "@browser":return "Internet";case "@files":return "Files";case "@notes":return "Notepad";case "@appearance":return "Personalize";default:AppCatalog.Entry app=entries.get(id);return app==null?"Unavailable app":app.label;}
    }
    @Override public void open(String id){
        startMenu.hide();
        switch(id){
            case "@control":controlPanel("@control","Control Panel","home");return;
            case "@system":controlPanel("@system","System","system");return;
            case "@filemanagers":explorer("@filemanagers","managers");return;
            case "@gadgets":gadgetGallery();return;
            case "@clock":clockOptions();return;
            case "@weather":weatherSettings();return;
            case "@sticky":stickyNote();return;
            case "@calendar":calendar();return;
            case "@pictures":pickDocument("pictures");return;
            case "@music":pickDocument("music");return;
            case "@computer":computer();return;
            case "@notes":notes();return;
            case "@appearance":appearance();return;
            case "@browser":launchSystem(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com")));return;
            case "@files":explorer("@files","home");return;
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
    private void desktopOptions(float x,float y){
        if(desktopMenu!=null)desktopMenu.dismiss();startMenu.hide();desktopMenu=new DesktopContextMenu(this);
        desktopMenu.item("All programs","programs",()->showStart(false));desktopMenu.item("Refresh","refresh",()->{scanApps();sidebar.refresh();});desktopMenu.separator();
        desktopMenu.item("Computer","computer",this::computer);desktopMenu.item("Control Panel","control",()->open("@control"));desktopMenu.item("Gadgets","clock",this::gadgetGallery);desktopMenu.separator();
        desktopMenu.item("Manage desktop shortcuts","computer",this::manageShortcuts);desktopMenu.item("Personalize","appearance",this::appearance);
        if(!isHome()){desktopMenu.separator();desktopMenu.item("Make default home","orb",this::home);}
        desktopMenu.show(root,x,y,shell.getPaddingLeft(),shell.getPaddingTop(),shell.getPaddingRight(),shell.getPaddingBottom());
    }
    private void manageShortcuts(){
        List<String> ids=store.desktop();String[] labels=new String[ids.size()];for(int i=0;i<ids.size();i++)labels[i]=label(ids.get(i));
        new AlertDialog.Builder(this).setTitle("Remove a desktop shortcut").setItems(labels,(d,index)->{List<String> current=store.desktop();current.remove(ids.get(index));store.desktop(current);renderDesktop();}).setNegativeButton("Done",null).show();
    }
    private void showStart(boolean search){startMenu.show(search);}
    private void showDesktop(){((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(workspace.getWindowToken(),0);startMenu.hide();for(DesktopWindow window:windows.values())window.setVisibility(View.GONE);renderTaskbar();}
    private void window(String id,String title,View body){
        DesktopWindow current=windows.get(id);if(current!=null&&current.getParent()!=null){if(body instanceof VistaShell)((VistaShell)body).close();current.restore();return;}
        DesktopWindow next=new DesktopWindow(this,workspace,title,body,wallpaper,this::renderTaskbar,store,id,layoutKey);windows.put(id,next);
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
    private void computer(){explorer("@computer","computer");}
    private void appearance(){controlPanel("@appearance","Personalization","personalization");}
    private void controlPanel(String id,String title,String page){if(restoreTool(id))return;window(id,title,new VistaControlPanel(this,store,wallpaper,vistaActions,page));}
    private void explorer(String id,String page){
        if(restoreTool(id))return;
        VistaExplorer.Files files=new VistaExplorer.Files(){
            public void chooseFolder(){chooseLinkedFolder(id);}public void pickFile(String kind){pickDocument(kind);}public void openDocument(Uri uri,String mime){viewDocument(uri,mime);}public void app(String app){MainActivity.this.open(app);}
        };
        window(id,id.equals("@computer")?"Computer":id.equals("@filemanagers")?"File Managers":"Files",new VistaExplorer(this,store,vistaActions,files,()->new ArrayList<>(entries.values()),page));
    }
    private void vistaAction(String id){
        switch(id){
            case "chrome":applySystemChrome();return;case "desktop":renderDesktop();return;case "gadgets":sidebar.setActive(true);sidebar.refresh();return;case "wallpaper":wallpaper.theme(store.prefs.getInt("wallpaper",0));return;
            case "profile":startMenu.refreshName();return;case "home":home();return;case "shortcuts":manageShortcuts();return;
            case "reset-icons":new AlertDialog.Builder(this).setTitle("Restore desktop icons?").setMessage("Restore the original desktop icons and their order? Your notes and apps are kept.").setPositiveButton("Restore",(d,w)->{store.prefs.edit().remove("shortcuts").apply();renderDesktop();}).setNegativeButton("Cancel",null).show();return;
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
    private void pickDocument(String kind){String mime=kind.equals("pictures")?"image/*":kind.equals("music")?"audio/*":kind.equals("videos")?"video/*":"*/*";launchSystem(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),true);}
    private void viewDocument(Uri uri,String mime){Intent view=new Intent(Intent.ACTION_VIEW).setDataAndType(uri,mime==null?"*/*":mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);view.setClipData(ClipData.newRawUri("Document",uri));launchSystem(view);}
    private void chooseLinkedFolder(String id){pendingFolderWindow=id;try{startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION),14);}catch(ActivityNotFoundException|SecurityException ex){toast("No folder picker is available on this device.");}}
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
        if(request==14&&result==RESULT_OK&&data!=null&&data.getData()!=null){
            Uri tree=data.getData();String target=pendingFolderWindow;int flags=data.getFlags()&Intent.FLAG_GRANT_READ_URI_PERMISSION;
            try{if(flags==0||!android.provider.DocumentsContract.isTreeUri(tree))throw new SecurityException();getContentResolver().takePersistableUriPermission(tree,flags);}catch(SecurityException ex){toast("This provider couldn't keep folder access. Please choose another folder.");return;}
            worker.execute(()->{String label;try{label=DocumentFolders.rootName(this,tree);}catch(RuntimeException ex){label="Linked folder";}String name=label;main.post(()->{if(destroyed)return;if(!DocumentFolders.add(store,tree,name)){try{getContentResolver().releasePersistableUriPermission(tree,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(SecurityException ignored){}toast("16 folders are already linked. Remove a folder link before adding another.");return;}afterDesktopReady(()->{explorer(target,target.equals("@computer")?"computer":target.equals("@filemanagers")?"managers":"home");DesktopWindow tool=windows.get(target);if(tool!=null&&tool.body instanceof VistaExplorer)((VistaExplorer)tool.body).folderAdded(tree.toString(),name);});});});return;
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
        if(desktopMenu!=null&&desktopMenu.showing()){desktopMenu.dismiss();return;}
        if(startMenu.getVisibility()==View.VISIBLE){startMenu.hide();return;}
        for(int i=workspace.getChildCount()-1;i>=0;i--){View v=workspace.getChildAt(i);if(v instanceof DesktopWindow&&v.getVisibility()==View.VISIBLE){DesktopWindow tool=(DesktopWindow)v;if(tool.body instanceof VistaShell&&((VistaShell)tool.body).goBack())return;v.setVisibility(View.GONE);((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(),0);renderTaskbar();return;}}
    }
    @Override public void onBackPressed(){handleBack();}
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);if(desktopMenu!=null)desktopMenu.dismiss();if(startMenu!=null){startMenu.hide();startMenu.refreshHome();}}
    @Override protected void onResume(){super.onResume();pausedSession=null;if(startMenu!=null)startMenu.refreshHome();main.removeCallbacks(clockTick);main.post(clockTick);if(store!=null)scanApps();if(sidebar!=null)sidebar.setActive(true);}
    @Override protected void onPause(){pausedSession=captureSession();if(desktopMenu!=null)desktopMenu.dismiss();super.onPause();main.removeCallbacks(clockTick);if(sidebar!=null)sidebar.setActive(false);cancelLocation();if(startMenu!=null)startMenu.hide();}
    @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);state.putString("folder_window",pendingFolderWindow);state.putString("desktop_session",(pausedSession==null?captureSession():pausedSession).toString());}
    private JSONObject captureSession(){
        JSONObject session=new JSONObject();JSONArray tools=new JSONArray();
        if(workspace==null)return session;
        for(int i=0;i<workspace.getChildCount();i++)if(workspace.getChildAt(i) instanceof DesktopWindow){
            DesktopWindow tool=(DesktopWindow)workspace.getChildAt(i);tool.saveGeometry();JSONObject item=new JSONObject();GadgetLayout.put(item,"id",tool.toolId);GadgetLayout.put(item,"visible",tool.getVisibility()==View.VISIBLE);GadgetLayout.put(item,"geometry",tool.geometry());GadgetLayout.put(item,"ui",ToolViewState.capture(tool));tools.put(item);
        }
        GadgetLayout.put(session,"tools",tools);GadgetLayout.put(session,"start",startMenu.state());GadgetLayout.put(session,"desktopScroll",desktopScroll.getScrollY());return session;
    }
    private void restoreSession(String saved){
        if(destroyed||saved==null)return;
        try{
            JSONObject session=new JSONObject(saved);JSONArray tools=session.optJSONArray("tools");
            if(tools!=null)for(int i=0;i<tools.length();i++){
                JSONObject item=tools.optJSONObject(i);if(item==null)continue;String id=item.optString("id");
                if(!Arrays.asList("@notes","@sticky","@calendar","@gadgets","@clock","@weather","@appearance","@computer","@control","@system","@files","@filemanagers").contains(id))continue;
                open(id);DesktopWindow tool=windows.get(id);if(tool==null)continue;tool.readGeometry(item.optJSONObject("geometry"));tool.setVisibility(item.optBoolean("visible",true)?View.VISIBLE:View.GONE);ToolViewState.restore(tool,item.optJSONObject("ui"));
            }
            desktopScroll.post(()->desktopScroll.scrollTo(0,session.optInt("desktopScroll")));startMenu.restore(session.optJSONObject("start"));renderTaskbar();
        }catch(JSONException ignored){}
    }
    @Override protected void onDestroy(){destroyed=true;main.removeCallbacksAndMessages(null);worker.shutdownNow();for(DesktopWindow tool:windows.values())if(tool.body instanceof VistaShell)((VistaShell)tool.body).close();if(sidebar!=null)sidebar.close();cancelLocation();if(receiverRegistered){unregisterReceiver(packages);unregisterReceiver(timeChanged);}if(Build.VERSION.SDK_INT>=33&&backCallback!=null)getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(backCallback);super.onDestroy();}
    private TextView weatherSummary;
    private void applySystemChrome(){
        boolean show=store.prefs.getBoolean("show_status_bar",false);
        if(Build.VERSION.SDK_INT>=30){WindowInsetsController controller=getWindow().getInsetsController();if(controller!=null){controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);if(show)controller.show(WindowInsets.Type.statusBars());else controller.hide(WindowInsets.Type.statusBars());}}
        else{int flags=View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;if(!show)flags|=View.SYSTEM_UI_FLAG_FULLSCREEN;getWindow().getDecorView().setSystemUiVisibility(flags);}
        if(shell!=null)shell.requestApplyInsets();
    }
    @Override public void sidebarChanged(){workspace.requestLayout();}
    @Override public void gallery(){gadgetGallery();}
    @Override public void readingsChanged(){updateWeatherSummary();}
    @Override public void gadgetAction(String id){
        startMenu.hide();
        switch(id){
            case "clock":clockOptions();break;case "calendar":calendar();break;case "weather":weatherSettings();break;
            case "notes":stickyNote();break;case "photo":choosePhotos();break;
            case "meters":new AlertDialog.Builder(this).setTitle("System Meter").setMessage("The left dial shows the device's battery level. The right dial shows the percentage of physical memory currently in use. These are live Android readings.").setPositiveButton("OK",null).show();break;
        }
    }
    private boolean restoreTool(String id){DesktopWindow current=windows.get(id);if(current!=null&&current.getParent()!=null){current.restore();return true;}return false;}
    private void gadgetGallery(){
        startMenu.hide();if(restoreTool("@gadgets"))return;
        LinearLayout content=Aero.column(this);Aero.pad(content,14);TextView heading=Aero.text(this,"Add gadgets to your desktop",18,0xff173a65);content.addView(heading);
        TextView hint=Aero.text(this,"Hold a gadget and drag it onto the desktop or Sidebar. Use ⋮ for sizes and docking; drag ◢ to resize.",12,0xff5c6c77);hint.setTypeface(android.graphics.Typeface.create(Aero.face(this),android.graphics.Typeface.ITALIC));hint.setPadding(0,dp(8),0,dp(10));content.addView(hint);
        GridLayout grid=new GridLayout(this);grid.setColumnCount(2);content.addView(grid);
        for(String id:GadgetView.IDS){
            LinearLayout cell=Aero.column(this);cell.setGravity(Gravity.CENTER);Aero.pad(cell,5);
            FrameLayout preview=new FrameLayout(this);preview.setBackground(Aero.panel(this,0xffd9e6ed,0xffb9cdd8,4,0xffa4b5c0));GadgetView face=new GadgetView(this,id,store,sidebar.weather);preview.addView(face,new FrameLayout.LayoutParams(dp(108),dp(108),Gravity.CENTER));preview.setOnClickListener(v->gadgetAction(id));cell.addView(preview,new LinearLayout.LayoutParams(-1,dp(118)));
            TextView title=Aero.text(this,GadgetView.title(id),13,Aero.INK);title.setGravity(Gravity.CENTER);cell.addView(title,new LinearLayout.LayoutParams(-1,dp(28)));
            TextView toggle=Aero.button(this,store.gadgets().contains(id)?"Remove":"Add",()->{});toggle.setContentDescription((store.gadgets().contains(id)?"Remove ":"Add ")+GadgetView.title(id)+" gadget");toggle.setOnClickListener(v->{List<String> ids=store.gadgets();if(ids.contains(id))ids.remove(id);else ids.add(id);store.gadgets(ids);sidebar.rebuild();sidebar.reveal();boolean added=ids.contains(id);toggle.setText(added?"Remove":"Add");toggle.setContentDescription((added?"Remove ":"Add ")+GadgetView.title(id)+" gadget");});cell.addView(toggle,new LinearLayout.LayoutParams(-1,dp(40)));
            GridLayout.LayoutParams lp=new GridLayout.LayoutParams();lp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);lp.width=0;lp.bottomMargin=dp(10);grid.addView(cell,lp);
        }
        addAction(content,"Show Sidebar",()->{sidebar.reveal();toast("Sidebar shown");});
        Switch lock=new Switch(this);lock.setTypeface(Aero.face(this));lock.setText("Lock desktop layout");lock.setContentDescription("Lock desktop layout");lock.setChecked(store.prefs.getBoolean("layout_locked",false));Aero.pad(lock,10);lock.setOnCheckedChangeListener((v,checked)->store.prefs.edit().putBoolean("layout_locked",checked).apply());content.addView(lock);
        Switch snap=new Switch(this);snap.setTypeface(Aero.face(this));snap.setText("Snap gadgets to grid");snap.setChecked(store.prefs.getBoolean("snap_gadgets",true));Aero.pad(snap,10);snap.setOnCheckedChangeListener((v,checked)->store.prefs.edit().putBoolean("snap_gadgets",checked).apply());content.addView(snap);
        paragraph(content,"Portrait and landscape save separate positions and sizes. Gadget content is shared between them.");
        ScrollView scroll=new ScrollView(this);scroll.addView(content);window("@gadgets","Desktop Gadget Gallery",scroll);
    }
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
        if(restoreTool("@weather"))return;LinearLayout content=Aero.column(this);Aero.pad(content,16);TextView heading=Aero.text(this,"Weather",22,0xff173a65);content.addView(heading);
        weatherSummary=Aero.text(this,"",14,Aero.INK);weatherSummary.setPadding(0,dp(10),0,dp(10));content.addView(weatherSummary);updateWeatherSummary();
        EditText city=new EditText(this);city.setSingleLine(true);city.setTypeface(Aero.face(this));city.setTextSize(14);city.setHint("City and state, or city and country");city.setContentDescription("Weather city search");content.addView(city,new LinearLayout.LayoutParams(-1,dp(48)));
        TextView status=Aero.text(this,"",12,0xff596e7a);content.addView(status);LinearLayout results=Aero.column(this);
        TextView search=Aero.button(this,"Search for a city",()->{});search.setOnClickListener(v->{String query=city.getText().toString().trim();if(query.isEmpty()){city.setError("Enter a city");return;}search.setEnabled(false);status.setText("Searching…");results.removeAllViews();worker.execute(()->{
            List<android.location.Address> addresses=null;try{if(android.location.Geocoder.isPresent())addresses=new android.location.Geocoder(this,Locale.getDefault()).getFromLocationName(query,5);}catch(java.io.IOException|RuntimeException ignored){}List<android.location.Address> found=addresses;
            main.post(()->{if(destroyed)return;search.setEnabled(true);if(found==null||found.isEmpty()){status.setText("City search is unavailable or found no match. Try a nearby town, or use your approximate location.");return;}status.setText("Choose your location:");for(android.location.Address address:found){String label=addressLabel(address,query);TextView result=Aero.button(this,label,()->{sidebar.weather.location(label,address.getLatitude(),address.getLongitude());results.removeAllViews();status.setText("Location saved.");});results.addView(result,new LinearLayout.LayoutParams(-1,-2));}});
        });});content.addView(search);content.addView(results);
        addAction(content,"Use my approximate location",this::locateWeather);
        Switch fahrenheit=new Switch(this);fahrenheit.setTypeface(Aero.face(this));fahrenheit.setText("Fahrenheit (°F)");fahrenheit.setChecked(store.prefs.getBoolean("weather_f",Locale.getDefault().getCountry().equals("US")));Aero.pad(fahrenheit,10);fahrenheit.setOnCheckedChangeListener((v,checked)->{store.prefs.edit().putBoolean("weather_f",checked).apply();sidebar.refresh();});content.addView(fahrenheit);
        addAction(content,"Refresh forecast",()->{sidebar.weather.refresh(true);updateWeatherSummary();});
        paragraph(content,"Forecasts are cached for offline viewing. Refresh follows the provider's update schedule.");
        TextView credit=Aero.text(this,"Weather: MET Norway · CC BY 4.0",12,0xff235e92);credit.setPadding(0,dp(8),0,dp(12));credit.setOnClickListener(v->launchSystem(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.met.no/en/free-meteorological-data/Licensing-and-crediting"))));content.addView(credit);
        ScrollView scroll=new ScrollView(this);scroll.addView(content);window("@weather","Weather settings",scroll);
    }
    private void updateWeatherSummary(){
        if(weatherSummary==null||sidebar==null)return;WeatherClient client=sidebar.weather;WeatherClient.Snapshot forecast=client.snapshot();String text=client.city();
        if(forecast!=null){boolean f=store.prefs.getBoolean("weather_f",Locale.getDefault().getCountry().equals("US"));text+="\n"+forecast.temperature(f)+(f?"°F":"°C")+" · "+forecast.condition()+"\n"+(client.stale()?"Cached forecast · ":"Updated · ")+DateFormat.getTimeFormat(this).format(new Date(client.updated()));}
        else text+=client.loading()?"\nLoading forecast…":client.configured()?"\nForecast unavailable. Try again when online.":"\nChoose a location to show your forecast.";weatherSummary.setText(text);
    }
    private String addressLabel(android.location.Address address,String fallback){String place=address.getLocality();if(place==null)place=address.getFeatureName();if(place==null)place=fallback;String region=address.getAdminArea(),country=address.getCountryName();if(region!=null&&!place.contains(region))place+=", "+region;if(country!=null&&!place.contains(country))place+=", "+country;return place;}
    private void locateWeather(){
        if(checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)!=android.content.pm.PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},42);return;}
        cancelLocation();android.location.LocationManager manager=(android.location.LocationManager)getSystemService(LOCATION_SERVICE);
        try{
            if(manager==null||!manager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)){new AlertDialog.Builder(this).setTitle("Location is unavailable").setMessage("Enable device location, or search for a city instead.").setPositiveButton("Location settings",(d,w)->launchSystem(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))).setNegativeButton("Cancel",null).show();return;}
            toast("Finding your approximate area…");
            if(Build.VERSION.SDK_INT>=30){android.os.CancellationSignal signal=new android.os.CancellationSignal();locationCancellation=signal;manager.getCurrentLocation(android.location.LocationManager.NETWORK_PROVIDER,signal,getMainExecutor(),location->{if(locationCancellation!=signal||destroyed)return;locationCancellation=null;if(location==null){toast("Couldn't get a location. Try city search.");return;}sidebar.weather.location("My area",location.getLatitude(),location.getLongitude());});main.postDelayed(()->{if(locationCancellation==signal){signal.cancel();locationCancellation=null;toast("Location timed out. Try city search.");}},15000);}
            else {locationListener=new android.location.LocationListener(){public void onLocationChanged(android.location.Location location){cancelLocation();sidebar.weather.location("My area",location.getLatitude(),location.getLongitude());}public void onProviderEnabled(String p){}public void onProviderDisabled(String p){}public void onStatusChanged(String p,int s,Bundle b){}};manager.requestSingleUpdate(android.location.LocationManager.NETWORK_PROVIDER,locationListener,Looper.getMainLooper());main.postDelayed(()->{if(locationListener!=null){cancelLocation();toast("Location timed out. Try city search.");}},15000);}
        }catch(SecurityException|IllegalArgumentException e){toast("Location is unavailable. Try city search.");}
    }
    private void cancelLocation(){if(locationCancellation!=null){locationCancellation.cancel();locationCancellation=null;}if(locationListener!=null){android.location.LocationManager manager=(android.location.LocationManager)getSystemService(LOCATION_SERVICE);try{if(manager!=null)manager.removeUpdates(locationListener);}catch(SecurityException ignored){}locationListener=null;}}
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] grants){super.onRequestPermissionsResult(request,permissions,grants);if(request==42){if(grants.length>0&&grants[0]==android.content.pm.PackageManager.PERMISSION_GRANTED)locateWeather();else toast("You can choose a city without granting location access.");}}


}
