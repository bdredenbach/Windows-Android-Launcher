package com.nthexperience.desktop;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.view.*;
import android.widget.*;

/** Desktop preferences with a live icon preview and independent orientation grids. */
final class DesktopSettings extends LinearLayout {
    private final DesktopStore store;private final Runnable changed;
    private final LinearLayout preview;private TextView grid;
    DesktopSettings(Context c,DesktopStore store,Runnable changed){
        super(c);this.store=store;this.changed=changed;setOrientation(VERTICAL);
        section("Desktop layout");check("Snap desktop apps to grid","snap_desktop",true);check("Snap gadgets to grid","snap_gadgets",true);check("Lock desktop layout","layout_locked",false);
        section("Icon layout");HorizontalScrollView scroll=new HorizontalScrollView(c);scroll.setHorizontalScrollBarEnabled(false);scroll.setBackground(Aero.panel(c,0xff1b557e,0xff16394c,2,0xff7293a7));preview=new LinearLayout(c);preview.setGravity(Gravity.CENTER_VERTICAL);preview.setPadding(dp(8),dp(12),dp(8),dp(8));scroll.addView(preview);addView(scroll,new LayoutParams(-1,dp(142)));
        slider(this,"Icon size","desktop_icon_percent",60,150,store.iconPercent(),"%",true);
        check("Show desktop icon labels","desktop_labels",true);slider(this,"Label size","desktop_label_size",10,16,store.labelSize(),"",true);
        grid=Aero.button(c,"",this::gridDialog);grid.setContentDescription("Choose desktop grid");addView(grid,new LayoutParams(-1,dp(44)));gridLabel();hint("Portrait and landscape keep separate grids. The grid uses the space beside the Sidebar.");
        slider(this,"Horizontal spacing","desktop_spacing_x",0,24,store.spacingX(),"",true);slider(this,"Vertical spacing","desktop_spacing_y",0,24,store.spacingY(),"",true);
        TextView reset=Aero.button(c,"Restore default icon layout",()->{android.content.SharedPreferences.Editor e=store.prefs.edit();for(String key:new String[]{"desktop_icon_size","desktop_icon_percent","desktop_labels","desktop_label_size","desktop_spacing_x","desktop_spacing_y","desktop_columns_portrait","desktop_rows_portrait","desktop_columns_landscape","desktop_rows_landscape"})e.remove(key);e.apply();changed.run();View parent=(View)getParent();while(parent!=null&&!(parent instanceof VistaControlPanel))parent=parent.getParent() instanceof View?(View)parent.getParent():null;if(parent instanceof VistaControlPanel)((VistaControlPanel)parent).render();});addView(reset,new LayoutParams(-1,dp(44)));updatePreview();
    }
    private String orientation(){return getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE?"landscape":"portrait";}
    private void gridLabel(){String o=orientation();int columns=store.gridColumns(o),rows=store.gridRows(o);grid.setText("Desktop grid: "+(columns==0&&rows==0?"Automatic":columns+" × "+rows)+" · "+(o.equals("portrait")?"Portrait":"Landscape"));}
    private void gridDialog(){
        String o=orientation();Context c=getContext();int width=c instanceof MainActivity?((MainActivity)c).desktopGridWidth():getResources().getDisplayMetrics().widthPixels;int height=c instanceof MainActivity?((MainActivity)c).desktopGridHeight():getResources().getDisplayMetrics().heightPixels;
        int maxColumns=Math.max(1,Math.min(10,width/Math.max(1,dp(48+store.spacingX())))),maxRows=Math.max(1,Math.min(16,height/Math.max(1,dp(store.iconLabels()?Math.max(72,store.labelSize()*2.6f+29+store.spacingY()):48+store.spacingY()))));
        LinearLayout panel=Aero.column(c);panel.setPadding(dp(14),dp(8),dp(14),dp(8));CheckBox automatic=Aero.check(c,"Automatic grid",store.gridColumns(o)==0&&store.gridRows(o)==0);panel.addView(automatic);
        int columns=Math.max(1,Math.min(maxColumns,store.gridColumns(o)==0?width/Math.max(1,dp(store.tileWidth())):store.gridColumns(o))),rows=Math.max(1,Math.min(maxRows,store.gridRows(o)==0?height/Math.max(1,dp(store.tileHeight())):store.gridRows(o)));
        TextView columnLabel=Aero.text(c,"Columns: "+columns,13,Aero.INK),rowLabel=Aero.text(c,"Rows: "+rows,13,Aero.INK);panel.addView(columnLabel);SeekBar columnSlider=countSlider(panel,"Desktop grid columns",columns,maxColumns,columnLabel,"Columns");panel.addView(rowLabel);SeekBar rowSlider=countSlider(panel,"Desktop grid rows",rows,maxRows,rowLabel,"Rows");
        Runnable enabled=()->{columnSlider.setEnabled(!automatic.isChecked());rowSlider.setEnabled(!automatic.isChecked());columnLabel.setAlpha(automatic.isChecked()?.5f:1);rowLabel.setAlpha(automatic.isChecked()?.5f:1);};automatic.setOnCheckedChangeListener((v,b)->enabled.run());enabled.run();
        TextView note=Aero.text(c,"Icons scale to fit. More shortcuts remain available by scrolling.",12,0xff586976);note.setTypeface(Aero.face(c),android.graphics.Typeface.ITALIC);panel.addView(note);
        Aero.dialog(c).setTitle((o.equals("portrait")?"Portrait":"Landscape")+" desktop grid").setView(panel).setNegativeButton("Cancel",null).setPositiveButton("Apply",(d,w)->{store.prefs.edit().putInt("desktop_columns_"+o,automatic.isChecked()?0:columnSlider.getProgress()+1).putInt("desktop_rows_"+o,automatic.isChecked()?0:rowSlider.getProgress()+1).apply();changed.run();gridLabel();updatePreview();}).show();
    }
    private SeekBar countSlider(LinearLayout parent,String description,int value,int max,TextView label,String title){SeekBar b=new SeekBar(getContext());b.setMax(max-1);b.setProgress(value-1);b.setContentDescription(description);b.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}public void onProgressChanged(SeekBar s,int p,boolean user){label.setText(title+": "+(p+1));}});parent.addView(b,new LayoutParams(-1,dp(42)));return b;}
    private void section(String title){TextView v=Aero.text(getContext(),title,14,0xff326421);v.setPadding(0,dp(12),0,dp(7));addView(v);}
    private void hint(String text){TextView v=Aero.text(getContext(),text,12,0xff586976);v.setTypeface(Aero.face(getContext()),android.graphics.Typeface.ITALIC);v.setPadding(0,dp(5),0,dp(8));addView(v);}
    private void check(String title,String key,boolean fallback){CheckBox box=Aero.check(getContext(),title,store.prefs.getBoolean(key,fallback));box.setOnCheckedChangeListener((v,b)->{store.prefs.edit().putBoolean(key,b).apply();changed.run();if(preview!=null)updatePreview();});addView(box);}
    private void slider(LinearLayout parent,String title,String key,int min,int max,int value,String suffix,boolean refresh){TextView label=Aero.text(getContext(),title+": "+value+suffix,13,Aero.INK);label.setPadding(0,dp(9),0,0);parent.addView(label);SeekBar bar=new SeekBar(getContext());bar.setMax(max-min);bar.setProgress(value-min);bar.setContentDescription(title);bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}public void onProgressChanged(SeekBar b,int p,boolean user){if(!user)return;int next=p+min;store.prefs.edit().putInt(key,next).apply();label.setText(title+": "+next+suffix);if(refresh){changed.run();updatePreview();}}});parent.addView(bar,new LayoutParams(-1,dp(42)));}
    private void updatePreview(){preview.removeAllViews();String[][] samples={{"computer","Computer"},{"folderapps","Folder"},{"notes","Notepad"}};for(String[] sample:samples){LinearLayout cell=Aero.column(getContext());cell.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);int size=store.iconSize();cell.addView(new AeroIcon(getContext(),sample[0]),new LayoutParams(dp(size),dp(size)));if(store.iconLabels()){TextView label=Aero.text(getContext(),sample[1],store.labelSize(),Color.WHITE);label.setGravity(Gravity.CENTER);label.setShadowLayer(dp(2),0,dp(1),Color.BLACK);cell.addView(label);}preview.addView(cell,new LayoutParams(dp(store.tileWidth()),-2));}}
    private int dp(float n){return Aero.dp(getContext(),n);}
}
