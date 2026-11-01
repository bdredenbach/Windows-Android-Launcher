package com.nthexperience.desktop;

import android.content.Context;
import android.graphics.Color;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;

final class VistaControlPanel extends VistaShell {
    private static final String[][] CATEGORIES={
        {"system","System and Maintenance","computer","View your device and storage"},
        {"security","Security","security","Privacy and device security"},
        {"network","Network and Internet","network","Connect to a network"},
        {"hardware","Hardware and Sound","hardware","Display, sound and Bluetooth"},
        {"programs","Programs","programs","Installed apps and defaults"},
        {"accounts","User Accounts","accounts","Your desktop name and accounts"},
        {"appearance","Appearance and Personalization","appearance","Colors, background and gadgets"},
        {"clock","Clock, Language, and Region","clock","Time, language and clock face"},
        {"accessibility","Ease of Access","accessibility","Make your device easier to use"}};
    // Category, title, icon, description, destination. Every row has a working destination.
    private static final String[][] ITEMS={
        {"system","Welcome Center","orb","Start here to set up your desktop","open:@welcome"},
        {"system","System","computer","View basic information about your device","page:system"},
        {"system","Backup and Restore","drive","Save or restore your desktop, folders and notes","open:@backup"},
        {"system","Desktop Organization","folderapps","Folders, shortcut names and ordering","open:@organize"},
        {"system","Storage","drive","Storage use and device cleanup · Android","android:storage"},
        {"system","Power Options","power","Battery and power saving · Android","android:battery"},
        {"security","Security settings","security","Screen lock and device security · Android","android:security"},
        {"security","Privacy settings","security","Review privacy controls · Android","android:privacy"},
        {"network","Wi-Fi","network","Choose a wireless network · Android","android:wifi"},
        {"network","Network connections","network","Internet and connection settings · Android","android:network"},
        {"hardware","Sound","sound","Volume and device sounds · Android","android:sound"},
        {"hardware","Display","computer","Brightness, display size and rotation · Android","android:display"},
        {"hardware","Bluetooth devices","hardware","Connect nearby devices · Android","android:bluetooth"},
        {"programs","Programs and Features","programs","Manage installed Android apps","android:apps"},
        {"programs","Default Programs","programs","Choose your default Android apps","android:defaults"},
        {"programs","File Managers","folderapps","Organize shortcuts to your file manager apps","open:@filemanagers"},
        {"accounts","Change your desktop name","accounts","Name and profile picture shown in the Start menu","page:accounts"},
        {"accounts","Device accounts","accounts","Manage accounts on your device · Android","android:accounts"},
        {"appearance","Personalization","appearance","Change the appearance of your desktop","page:personalization"},
        {"appearance","Desktop Background","pictures","Choose your aurora wallpaper","page:background"},
        {"appearance","Window Color and Appearance","color","Glass color, intensity, transparency and opacity","page:colors"},
        {"appearance","Startup and Sounds","orb","Startup intro, reboot, original chime and reduced motion","page:startup"},
        {"appearance","Quick Launch","programs","Choose and order taskbar shortcuts","open:@quicklaunch"},
        {"appearance","Taskbar and Start Menu","control","Status bar and desktop layout preferences","page:taskbar"},
        {"appearance","Desktop Icons","computer","Choose which built-in icons appear","page:icons"},
        {"appearance","Desktop Gadgets","clock","Add and arrange desktop gadgets","open:@gadgets"},
        {"clock","Date and Time","clock","Device date and time zone · Android","android:date"},
        {"clock","Clock appearance","clock","Clock face and second hand","page:clock"},
        {"clock","Regional and Language Options","network","Device language and region · Android","android:language"},
        {"accessibility","Ease of Access Center","accessibility","Accessibility services and display aids · Android","android:accessibility"}};
    static void searchItems(String query,List<StartSearch.Result> out){for(String[] item:ITEMS)if((item[1]+" "+item[3]).toLowerCase(Locale.ROOT).contains(query))out.add(new StartSearch.Result("setting:"+item[4],item[1],"Control Panel",item[2],null,false));}
    private final AeroWallpaper wallpaper;
    private boolean classic;
    private EditText nameInput;
    private String nameDraft;
    VistaControlPanel(Context c,DesktopStore store,AeroWallpaper wallpaper,Actions actions,String initial){
        super(c,store,actions,"Search Control Panel");this.wallpaper=wallpaper;classic=store.prefs.getBoolean("control_panel_classic",false);
        TextView view=command("View ▾",()->{PopupMenu menu=new PopupMenu(c,commands);for(String mode:new String[]{"Control Panel Home","Classic View"})menu.getMenu().add(mode).setOnMenuItemClickListener(item->{classic=mode.equals("Classic View");store.prefs.edit().putBoolean("control_panel_classic",classic).apply();navigate("home");render();return true;});menu.show();});commands.addView(view,new LayoutParams(-2,dp(36)));start(initial);
    }
    @Override void render(){
        nameInput=null;side.removeAllViews();sideLink("Control Panel Home",()->{classic=false;store.prefs.edit().putBoolean("control_panel_classic",false).apply();navigate("home");render();});sideLink("Classic View",()->{classic=true;store.prefs.edit().putBoolean("control_panel_classic",true).apply();navigate("home");render();});sideLink("Personalization",()->navigate("personalization"));sideLink("Computer",()->actions.open("@computer"));sideLink("Android settings",()->actions.action("android:settings"));
        LinearLayout column=column();String page=page();address.setText("Control Panel"+(page.equals("home")?"":"  ›  "+title(page)));status.setText("Nth Desktop  •  "+(page.equals("home")?(classic?"Classic View":"Control Panel Home"):title(page)));
        if(!query().isEmpty()){
            heading(column,"Search results","Settings matching “"+search.getText()+"”");int count=0;for(String[] item:ITEMS)if((item[1]+" "+item[3]).toLowerCase(Locale.getDefault()).contains(query())){item(column,item);count++;}if(count==0)heading(column,"No matching settings","Try a setting such as wallpaper, sound or Wi-Fi.");status.setText(count+" matching settings");
        }else if(page.equals("home")){
            heading(column,"Adjust your computer's settings",classic?"All Control Panel items":"Choose a category");
            GridLayout grid=new GridLayout(getContext());grid.setColumnCount(compact()?1:2);column.addView(grid,new LayoutParams(-1,-2));
            if(classic)for(String[] item:ITEMS){LinearLayout cell=Aero.column(getContext());task(cell,item[2],item[1],"",()->destination(item[4]));gridCell(grid,cell);}else for(String[] category:CATEGORIES){LinearLayout cell=Aero.column(getContext());task(cell,category[2],category[1],category[3],()->navigate("category:"+category[0]));gridCell(grid,cell);}
        }else if(page.startsWith("category:")){
            String key=page.substring(9);heading(column,title(page),"Choose a task");for(String[] item:ITEMS)if(item[0].equals(key))item(column,item);
        }else switch(page){
            case "personalization":heading(column,"Personalize appearance and sounds","Give your desktop a familiar Aero look.");for(String[] item:ITEMS)if(item[0].equals("appearance")&&!item[4].equals("page:personalization"))item(column,item);break;
            case "background":backgrounds(column);break;
            case "colors":colors(column);break;
            case "startup":startup(column);break;
            case "taskbar":heading(column,"Taskbar and Start Menu","Desktop behavior");check(column,"Show Android status bar","show_status_bar",false,()->actions.action("chrome"));check(column,"Lock desktop layout","layout_locked",false,()->{});check(column,"Snap gadgets to grid","snap_gadgets",true,()->{});task(column,"control","Choose default home app","Use Nth Desktop as your home",()->actions.action("home"));break;
            case "icons":heading(column,"Desktop Icon Settings","Choose the built-in icons shown on your desktop.");String[][] icons={{"@computer","Computer"},{"@files","Files"},{"@notes","Notepad"},{"@appearance","Personalize"},{"@control","Control Panel"},{"@filemanagers","File Managers"}};for(String[] icon:icons){CheckBox box=checkbox(icon[1],store.pinned(icon[0]));box.setOnCheckedChangeListener((v,value)->{if(value!=store.pinned(icon[0]))store.toggle(icon[0]);actions.action("desktop");});column.addView(box);}link(column,"Manage all desktop shortcuts",()->actions.action("shortcuts"));link(column,"Restore original desktop icons",()->actions.action("reset-icons"));break;
            case "accounts":accounts(column);break;
            case "system":system(column);break;
            case "clock":heading(column,"Clock appearance","Your device's local time and time zone");GadgetView preview=new GadgetView(getContext(),"clock",store,null);column.addView(preview,new LayoutParams(dp(132),dp(128)));LinearLayout faces=new LinearLayout(getContext());for(int i=0;i<2;i++){final int style=i;TextView button=Aero.button(getContext(),i==0?"Classic ivory":"Black glass",()->{store.prefs.edit().putInt("clock_style",style).apply();actions.action("gadgets");render();});faces.addView(button,new LayoutParams(0,dp(44),1));}column.addView(faces);check(column,"Show second hand","clock_seconds",true,()->actions.action("gadgets"));break;
            default:heading(column,"Control Panel","Choose Control Panel Home to view your settings.");break;
        }
        body(scrolling(column));
    }
    private void item(LinearLayout column,String[] item){task(column,item[2],item[1],item[3],()->destination(item[4]));}
    private void destination(String value){if(value.startsWith("page:"))navigate(value.substring(5));else if(value.startsWith("open:"))actions.open(value.substring(5));else actions.action(value);}
    private void gridCell(GridLayout grid,View cell){GridLayout.LayoutParams lp=new GridLayout.LayoutParams();lp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);lp.width=0;lp.setMargins(0,0,dp(8),dp(8));grid.addView(cell,lp);}
    private String title(String page){if(page.startsWith("category:"))for(String[] category:CATEGORIES)if(category[0].equals(page.substring(9)))return category[1];switch(page){case "personalization":return "Personalization";case "background":return "Desktop Background";case "colors":return "Window Color and Appearance";case "startup":return "Startup and Sounds";case "taskbar":return "Taskbar and Start Menu";case "icons":return "Desktop Icon Settings";case "accounts":return "User Accounts";case "system":return "System";case "clock":return "Clock";default:return "Control Panel";}}
    private void backgrounds(LinearLayout column){
        heading(column,"Choose a desktop background","Original aurora artwork • lossless WebP");String[] names={"Aero · green and blue aurora","Twilight · violet and rose","Meadow · sage and gold"};int current=store.prefs.getInt("wallpaper",0);
        for(int i=0;i<names.length;i++){final int selected=i;FrameLayout preview=new FrameLayout(getContext());preview.addView(new AeroWallpaper(getContext(),i),new FrameLayout.LayoutParams(-1,-1));TextView label=Aero.text(getContext(),(current==i?"✓  ":"")+names[i],13,Color.WHITE);label.setPadding(dp(9),0,dp(6),0);label.setBackgroundColor(0x880a253a);preview.addView(label,new FrameLayout.LayoutParams(-1,dp(35),Gravity.BOTTOM));preview.setBackground(Aero.panel(getContext(),0xffffffff,0xffffffff,2,current==i?0xff2882bc:0xffb5c4d0));preview.setPadding(dp(3),dp(3),dp(3),dp(3));preview.setContentDescription("Use "+names[i]+" wallpaper");preview.setOnClickListener(v->selectWallpaper(selected));preview.setFocusable(true);LayoutParams lp=new LayoutParams(-1,dp(105));lp.bottomMargin=dp(12);column.addView(preview,lp);}
    }
    private void colors(LinearLayout column){
        heading(column,"Change your window colors","Color your glass independently of the desktop background.");

        GridLayout choices=new GridLayout(getContext());choices.setColumnCount(4);int selected=store.prefs.getInt("aero_color",0);
        for(int i=0;i<AeroGlass.TINTS.length;i++){final int value=i;LinearLayout tile=Aero.column(getContext());tile.setGravity(Gravity.CENTER);tile.setPadding(dp(3),dp(5),dp(3),dp(5));View swatch=new View(getContext());swatch.setBackground(Aero.gloss(getContext(),new int[]{0xfff0f8ff,AeroGlass.TINTS[i],AeroGlass.TINTS[i],0xffd9edf8},4,selected==i?0xff286c98:0xff96aab8));tile.addView(swatch,new LayoutParams(dp(43),dp(34)));TextView name=Aero.text(getContext(),(selected==i?"✓ ":"")+AeroGlass.COLOR_NAMES[i],11,Aero.INK);name.setGravity(Gravity.CENTER);tile.addView(name,new LayoutParams(-1,dp(27)));tile.setContentDescription("Glass color: "+AeroGlass.COLOR_NAMES[i]);tile.setBackground(Aero.selection(getContext()));tile.setFocusable(true);tile.setOnClickListener(v->{store.prefs.edit().putInt("aero_color",value).apply();actions.action("glass");render();});GridLayout.LayoutParams cell=new GridLayout.LayoutParams();cell.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);cell.width=0;choices.addView(tile,cell);}column.addView(choices);
        CheckBox transparency=checkbox("Enable glass transparency",store.prefs.getBoolean("aero_transparency",true));column.addView(transparency);
        LinearLayout opacity=Aero.column(getContext());opacity.setVisibility(transparency.isChecked()?VISIBLE:GONE);percentSlider(opacity,"Glass opacity","aero_opacity",AeroGlass.DEFAULT_OPACITY);column.addView(opacity);
        transparency.setOnCheckedChangeListener((v,value)->{store.prefs.edit().putBoolean("aero_transparency",value).apply();opacity.setVisibility(value?VISIBLE:GONE);actions.action("glass");});
        percentSlider(column,"Color intensity","aero_intensity",45);
        link(column,"Restore default glass",()->{store.prefs.edit().remove("aero_color").remove("aero_intensity").remove("aero_transparency").remove("aero_opacity").apply();actions.action("glass");render();});link(column,"Choose a desktop background",()->navigate("background"));
    }
    private void percentSlider(LinearLayout column,String title,String key,int fallback){
        TextView label=Aero.text(getContext(),title+": "+store.prefs.getInt(key,fallback)+"%",13,Aero.INK);column.addView(label,new LayoutParams(-1,dp(34)));
        SeekBar slider=new SeekBar(getContext());slider.setMax(100);slider.setProgress(store.prefs.getInt(key,fallback));slider.setContentDescription(title);slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}public void onProgressChanged(SeekBar b,int value,boolean user){if(!user)return;store.prefs.edit().putInt(key,value).apply();label.setText(title+": "+value+"%");actions.action("glass");}});column.addView(slider,new LayoutParams(-1,dp(44)));
    }
    private void startup(LinearLayout column){
        heading(column,"Startup and Sounds","A short Nth pearl, a soft glow and a welcome home. Ordinary Home presses return straight to your desktop.");RadioGroup modes=new RadioGroup(getContext());String[] names={"First launch only","Once per phone reboot","Off"};for(int i=0;i<names.length;i++){RadioButton mode=new RadioButton(getContext());mode.setId(500+i);mode.setText(names[i]);mode.setContentDescription(names[i]);mode.setTypeface(Aero.face(getContext()));mode.setTextSize(13);mode.setTextColor(Aero.INK);mode.setButtonDrawable(new AeroCheck(getContext(),true));mode.setMinHeight(dp(44));modes.addView(mode,new RadioGroup.LayoutParams(-1,dp(44)));}modes.check(500+store.prefs.getInt("startup_mode",0));modes.setOnCheckedChangeListener((group,id)->store.prefs.edit().putInt("startup_mode",id-500).apply());column.addView(modes);
        check(column,"Play the original Nth startup chime","startup_sound",false,()->{});TextView sound=Aero.text(getContext(),"Sound follows your phone’s sound mode and stays quiet while music is playing.",12,0xff586976);sound.setPadding(0,dp(4),0,dp(10));column.addView(sound);check(column,"Reduce motion","reduce_motion",false,()->{});TextView reduced=Aero.text(getContext(),"Also follows Android’s animation setting. Reduced motion uses a brief, silent still welcome.",12,0xff586976);reduced.setPadding(0,dp(4),0,dp(12));column.addView(reduced);TextView preview=Aero.button(getContext(),"Preview startup",()->actions.action("startup-preview"));preview.setContentDescription("Preview startup");column.addView(preview,new LayoutParams(-1,dp(44)));link(column,"Open Welcome Center",()->actions.open("@welcome"));
    }
    private void selectWallpaper(int value){store.prefs.edit().putInt("wallpaper",value).apply();wallpaper.theme(value);actions.action("wallpaper");render();}
    private CheckBox checkbox(String title,boolean checked){return Aero.check(getContext(),title,checked);}
    private void check(LinearLayout column,String title,String key,boolean fallback,Runnable changed){CheckBox box=checkbox(title,store.prefs.getBoolean(key,fallback));box.setOnCheckedChangeListener((v,value)->{store.prefs.edit().putBoolean(key,value).apply();changed.run();});column.addView(box);}
    private void accounts(LinearLayout column){heading(column,"Change your desktop name","This name appears at the top of your Start menu.");nameInput=new EditText(getContext());nameInput.setSingleLine(true);nameInput.setTypeface(Aero.face(getContext()));nameInput.setTextSize(15);nameInput.setContentDescription("Desktop name");nameInput.setText(nameDraft==null?store.prefs.getString("desktop_name","Nth Desktop"):nameDraft);column.addView(nameInput,new LayoutParams(-1,dp(46)));column.addView(Aero.button(getContext(),"Save name",()->{String value=nameInput.getText().toString().trim();if(value.isEmpty()){nameInput.setError("Enter a name");return;}value=value.substring(0,Math.min(40,value.length()));store.prefs.edit().putString("desktop_name",value).apply();nameDraft=value;status.setText("Desktop name saved");actions.action("profile");}));section(column,"Your picture");column.addView(ProfilePortrait.view(getContext(),store),new LayoutParams(dp(74),dp(74)));link(column,"Choose a profile picture…",()->actions.action("profile-pick"));link(column,"Use the Nth picture",()->{store.prefs.edit().remove("profile_portrait").apply();actions.action("profile");render();});link(column,"Manage device accounts in Android",()->actions.action("android:accounts"));}
    private void system(LinearLayout column){
        heading(column,"View basic information about your device","");LinearLayout edition=new LinearLayout(getContext());edition.setGravity(Gravity.CENTER_VERTICAL);edition.addView(new AeroIcon(getContext(),"orb"),new LayoutParams(dp(58),dp(58)));TextView brand=Aero.text(getContext(),"Nth Desktop\nVista-inspired desktop for Android",16,0xff1e4e76);brand.setPadding(dp(12),0,0,0);edition.addView(brand,new LayoutParams(0,-2,1));column.addView(edition);section(column,"System");property(column,"Device",Build.MANUFACTURER+" "+Build.MODEL);property(column,"Android",Build.VERSION.RELEASE+" (API "+Build.VERSION.SDK_INT+")");android.app.ActivityManager manager=(android.app.ActivityManager)getContext().getSystemService(Context.ACTIVITY_SERVICE);if(manager!=null){android.app.ActivityManager.MemoryInfo memory=new android.app.ActivityManager.MemoryInfo();manager.getMemoryInfo(memory);property(column,"Usable memory",android.text.format.Formatter.formatFileSize(getContext(),memory.totalMem));}try{StatFs disk=new StatFs(Environment.getDataDirectory().getPath());property(column,"Internal storage",android.text.format.Formatter.formatFileSize(getContext(),disk.getAvailableBytes())+" free of "+android.text.format.Formatter.formatFileSize(getContext(),disk.getTotalBytes()));}catch(RuntimeException ignored){}property(column,"Desktop version","0.7.0");section(column,"Computer name");property(column,"Desktop name",store.prefs.getString("desktop_name","Nth Desktop"));link(column,"Change desktop name",()->navigate("accounts"));link(column,"Android device information",()->actions.action("android:device"));link(column,"Default home app",()->actions.action("home"));section(column,"Nth Experience");TextView credit=Aero.text(getContext(),"Original artwork and native desktop code. Selawik font by Microsoft, used under the SIL Open Font License.\n\nAndroid apps open in Android. System settings open the matching Android page.",11,0xff627483);column.addView(credit);
    }
    private void section(LinearLayout column,String title){TextView text=Aero.text(getContext(),title,14,0xff326421);text.setPadding(0,dp(17),0,dp(7));column.addView(text);View line=new View(getContext());line.setBackgroundColor(0xffd7e2ea);column.addView(line,new LayoutParams(-1,dp(1)));}
    private void property(LinearLayout column,String name,String value){TextView text=Aero.text(getContext(),name+":  "+value,12,0xff334c5d);text.setPadding(dp(4),dp(7),0,dp(3));column.addView(text);}
    @Override void showFolders(){PopupMenu menu=new PopupMenu(getContext(),folders);menu.getMenu().add("Control Panel Home").setOnMenuItemClickListener(v->{navigate("home");return true;});menu.getMenu().add("Classic View").setOnMenuItemClickListener(v->{classic=true;store.prefs.edit().putBoolean("control_panel_classic",true).apply();navigate("home");render();return true;});menu.getMenu().add("Personalization").setOnMenuItemClickListener(v->{navigate("personalization");return true;});for(String[] category:CATEGORIES)menu.getMenu().add(category[1]).setOnMenuItemClickListener(v->{navigate("category:"+category[0]);return true;});menu.show();}
    @Override void goUp(){navigate("home");}
    @Override JSONObject extraState(){JSONObject result=new JSONObject();GadgetLayout.put(result,"classic",classic);if(nameInput!=null)GadgetLayout.put(result,"nameDraft",nameInput.getText().toString());return result;}
    @Override void restoreExtra(JSONObject value){if(value!=null){classic=value.optBoolean("classic");nameDraft=value.optString("nameDraft",null);}}
}
