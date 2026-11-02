package com.nthexperience.desktop;

import android.content.*;
import android.graphics.Color;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.json.*;

/** Offline gadgets: their faces and tools use the same saved values. */
final class GadgetExtras {
    static boolean supports(String kind){return Arrays.asList("daysuntil","dice","colors","textcounter").contains(kind);}
    static LocalDate target(JSONObject data){try{return LocalDate.parse(data.optString("date"));}catch(RuntimeException ex){return LocalDate.now();}}
    static long days(JSONObject data){return ChronoUnit.DAYS.between(LocalDate.now(),target(data));}
    static String dayLabel(JSONObject data){long days=days(data);return days==0?"Today":Math.abs(days)+(days<0?" days ago":" days to go");}
    static int words(String text){String value=text.trim();return value.isEmpty()?0:value.split("(?U)\\s+").length;}
    static String hex(JSONObject data){return String.format(Locale.ROOT,"#%06X",data.optInt("rgb",0x4286b4)&0xffffff);}
    static void validate(String kind,JSONObject data)throws JSONException{
        if(kind.equals("daysuntil")){if(data.optString("title").length()>80)throw new JSONException("Event name too long");if(data.has("date"))try{LocalDate date=LocalDate.parse(data.getString("date"));if(date.getYear()<1900||date.getYear()>2200)throw new IllegalArgumentException();}catch(RuntimeException ex){throw new JSONException("Invalid event date");}}
        if(kind.equals("colors")&&data.has("rgb")&&(!(data.get("rgb") instanceof Number)||data.getDouble("rgb")!=data.getInt("rgb")||data.getInt("rgb")<0||data.getInt("rgb")>0xffffff))throw new JSONException("Invalid RGB color");
        if(kind.equals("textcounter")&&data.optString("text").length()>20000)throw new JSONException("Text counter text too long");
        if(kind.equals("dice")){int count=data.optInt("count",2),sides=data.optInt("sides",6);if(count<1||count>6||!Arrays.asList(4,6,8,10,12,20,100).contains(sides))throw new JSONException("Invalid dice");JSONArray rolls=data.optJSONArray("rolls");if(rolls!=null){if(rolls.length()!=count)throw new JSONException("Invalid roll count");for(int i=0;i<rolls.length();i++)if(!(rolls.get(i) instanceof Number)||rolls.getDouble(i)!=rolls.getInt(i)||rolls.getInt(i)<1||rolls.getInt(i)>sides)throw new JSONException("Invalid die value");}}
    }
    static void build(Context context,DesktopStore store,String kind,LinearLayout column,Runnable changed){new GadgetExtras(context,store,kind,column,changed).build();}
    private final Context context;private final DesktopStore store;private final String kind;private final LinearLayout column;private final Runnable changed;
    private GadgetExtras(Context context,DesktopStore store,String kind,LinearLayout column,Runnable changed){this.context=context;this.store=store;this.kind=kind;this.column=column;this.changed=changed;}
    private int dp(int n){return Aero.dp(context,n);}
    private JSONObject data(){return GadgetData.get(store,kind);}
    private void save(JSONObject value){GadgetData.save(store,kind,value);changed.run();}
    private TextView label(String text,String description,int size){TextView view=Aero.text(context,text,size,Aero.INK);view.setContentDescription(description);view.setPadding(0,dp(6),0,dp(6));column.addView(view,new LinearLayout.LayoutParams(-1,-2));return view;}
    private EditText input(String description,String text,int limit){EditText view=new EditText(context);view.setTypeface(Aero.face(context));view.setTextSize(15);view.setText(text);view.setContentDescription(description);view.setFilters(new InputFilter[]{new InputFilter.LengthFilter(limit)});column.addView(view,new LinearLayout.LayoutParams(-1,-2));return view;}
    private void button(String text,Runnable action){TextView button=Aero.button(context,text,action);button.setContentDescription(text);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(44));p.topMargin=dp(6);column.addView(button,p);}
    private TextWatcher watch(Runnable action){return new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void onTextChanged(CharSequence s,int a,int b,int c){action.run();}public void afterTextChanged(Editable e){}};}
    private void build(){switch(kind){case "daysuntil":event();break;case "dice":dice();break;case "colors":colors();break;case "textcounter":text();break;}}
    private void event(){
        EditText name=input("Event name",data().optString("title","My next adventure"),80);name.setSingleLine();TextView date=label(target(data()).toString(),"Target event date",18);TextView remaining=label(dayLabel(data()),"Days until event",28);
        name.addTextChangedListener(watch(()->{JSONObject value=data();GadgetLayout.put(value,"title",name.getText().toString());save(value);}));
        button("Choose event date",()->{LocalDate current=target(data());DatePicker picker=new DatePicker(context);Calendar min=Calendar.getInstance();min.set(1900,0,1);picker.setMinDate(min.getTimeInMillis());Calendar max=Calendar.getInstance();max.set(2200,11,31);picker.setMaxDate(max.getTimeInMillis());picker.init(current.getYear(),current.getMonthValue()-1,current.getDayOfMonth(),null);Aero.dialog(context).setTitle("Choose event date").setView(picker).setPositiveButton("Save date",(d,w)->{picker.clearFocus();JSONObject value=data();GadgetLayout.put(value,"date",LocalDate.of(picker.getYear(),picker.getMonth()+1,picker.getDayOfMonth()).toString());save(value);date.setText(target(value).toString());remaining.setText(dayLabel(value));}).setNegativeButton("Cancel",null).show();});
        label("Calendar days in your device’s time zone. Past events show days elapsed.","Event date information",12);
    }
    private void dice(){
        EditText count=input("Number of dice",Integer.toString(data().optInt("count",2)),1);count.setInputType(InputType.TYPE_CLASS_NUMBER);EditText sides=input("Sides per die",Integer.toString(data().optInt("sides",6)),3);sides.setInputType(InputType.TYPE_CLASS_NUMBER);label("1–6 dice; 4, 6, 8, 10, 12, 20 or 100 sides.","Dice limits",12);TextView result=label(rollText(data()),"Dice result",22);
        button("Roll dice",()->{int n,s;try{n=Integer.parseInt(count.getText().toString());s=Integer.parseInt(sides.getText().toString());if(n<1||n>6||!Arrays.asList(4,6,8,10,12,20,100).contains(s))throw new NumberFormatException();}catch(NumberFormatException ex){sides.setError("Choose 1–6 dice and a supported number of sides");return;}JSONObject value=data();GadgetLayout.put(value,"count",n);GadgetLayout.put(value,"sides",s);JSONArray rolls=new JSONArray();Random random=new Random();for(int i=0;i<n;i++)rolls.put(1+random.nextInt(s));GadgetLayout.put(value,"rolls",rolls);save(value);result.setText(rollText(value));});
    }
    static String rollText(JSONObject data){JSONArray rolls=data.optJSONArray("rolls");if(rolls==null)return "Ready to roll";int sum=0;StringJoiner text=new StringJoiner(" + ");for(int i=0;i<rolls.length();i++){int roll=rolls.optInt(i);sum+=roll;text.add(Integer.toString(roll));}return text+" = "+sum;}
    private void colors(){
        View swatch=new View(context);column.addView(swatch,new LinearLayout.LayoutParams(-1,dp(90)));EditText hex=input("Hex color",hex(data()),7);hex.setSingleLine();TextView rgb=label("","RGB color",16);SeekBar[] sliders=new SeekBar[3];String[] names={"Red","Green","Blue"};boolean[] updating={false};
        Runnable draw=()->{int color=data().optInt("rgb",0x4286b4);swatch.setBackgroundColor(0xff000000|color);rgb.setText(String.format(Locale.ROOT,"R %d    G %d    B %d",color>>16&255,color>>8&255,color&255));updating[0]=true;for(int i=0;i<3;i++)if(sliders[i]!=null)sliders[i].setProgress(color>>(16-8*i)&255);updating[0]=false;};
        for(int i=0;i<3;i++){int channel=i;label(names[i],names[i]+" channel label",12);SeekBar slider=new SeekBar(context);sliders[i]=slider;slider.setMax(255);slider.setContentDescription(names[i]+" color channel");column.addView(slider,new LinearLayout.LayoutParams(-1,dp(40)));slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar v){}public void onStopTrackingTouch(SeekBar v){}public void onProgressChanged(SeekBar v,int progress,boolean user){if(!user||updating[0])return;int shift=16-channel*8;JSONObject value=data();int color=(value.optInt("rgb",0x4286b4)&~(255<<shift))|(progress<<shift);GadgetLayout.put(value,"rgb",color);save(value);hex.setText(hex(value));draw.run();}});}
        button("Apply hex color",()->{String text=hex.getText().toString().trim();if(!text.matches("#?[0-9a-fA-F]{6}")){hex.setError("Enter six hexadecimal digits, such as #4286B4");return;}JSONObject value=data();GadgetLayout.put(value,"rgb",Integer.parseInt(text.replace("#",""),16));save(value);hex.setText(hex(value));draw.run();});button("Copy color code",()->{ClipboardManager clipboard=(ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE);clipboard.setPrimaryClip(ClipData.newPlainText("Color",hex(data())));Toast.makeText(context,"Color code copied",Toast.LENGTH_SHORT).show();});draw.run();
    }
    private void text(){
        EditText editor=input("Text to count",data().optString("text"),20000);editor.setGravity(Gravity.TOP|Gravity.START);editor.setMinLines(5);TextView counts=label("","Text counts",18);Runnable update=()->{String text=editor.getText().toString();int count=words(text);counts.setText(count+" words\n"+text.codePointCount(0,text.length())+" characters\n"+(count==0?0:Math.max(1,(count+199)/200))+" min reading time");};editor.addTextChangedListener(watch(()->{JSONObject value=data();GadgetLayout.put(value,"text",editor.getText().toString());save(value);update.run();}));button("Clear counted text",()->editor.setText(""));label("Saved on this device. Reading time estimates 200 words per minute.","Text counter information",12);update.run();
    }
}
