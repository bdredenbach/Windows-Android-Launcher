package com.nthexperience.desktop;

import android.view.*;
import android.widget.*;
import org.json.*;

/** Small transient UI state. Note contents already live in SharedPreferences. */
final class ToolViewState {
    static JSONObject capture(View root){JSONObject result=new JSONObject();capture(root,"",result);return result;}
    private static void capture(View view,String path,JSONObject result){
        JSONObject item=new JSONObject();
        if(view instanceof EditText){EditText text=(EditText)view;String description=String.valueOf(text.getContentDescription());
            if(!description.equals("Notepad text")&&!description.equals("Sticky note text"))GadgetLayout.put(item,"text",text.getText().toString().substring(0,Math.min(2000,text.length())));
            GadgetLayout.put(item,"start",text.getSelectionStart());GadgetLayout.put(item,"end",text.getSelectionEnd());GadgetLayout.put(item,"focus",text.hasFocus());
        }
        if(view instanceof MonthCalendarView){MonthCalendarView calendar=(MonthCalendarView)view;GadgetLayout.put(item,"month",calendar.monthTime());GadgetLayout.put(item,"selected",calendar.selected().getTime());}
        if(view instanceof ScrollView){GadgetLayout.put(item,"scrollX",view.getScrollX());GadgetLayout.put(item,"scrollY",view.getScrollY());}
        if(item.length()>0)GadgetLayout.put(result,path,item);
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)capture(group.getChildAt(i),path+"/"+i,result);}
    }
    static void restore(View root,JSONObject state){if(state!=null)restore(root,"",state);}
    private static void restore(View view,String path,JSONObject state){
        JSONObject item=state.optJSONObject(path);
        if(item!=null){
            if(view instanceof EditText){EditText text=(EditText)view;if(item.has("text"))text.setText(item.optString("text"));int length=text.length();text.setSelection(Math.max(0,Math.min(length,item.optInt("start",length))),Math.max(0,Math.min(length,item.optInt("end",length))));if(item.optBoolean("focus")&&view.isShown())text.requestFocus();}
            if(view instanceof MonthCalendarView)((MonthCalendarView)view).restore(item.optLong("month"),item.optLong("selected"));
            if(view instanceof ScrollView)view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener(){public void onGlobalLayout(){view.getViewTreeObserver().removeOnGlobalLayoutListener(this);view.scrollTo(item.optInt("scrollX"),item.optInt("scrollY"));}});
        }
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)restore(group.getChildAt(i),path+"/"+i,state);}
    }
}
