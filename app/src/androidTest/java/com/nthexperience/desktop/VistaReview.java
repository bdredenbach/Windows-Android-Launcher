package com.nthexperience.desktop;

import android.app.*;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.os.*;
import android.view.*;
import android.view.accessibility.*;
import android.graphics.*;
import org.json.*;
import java.io.*;
import java.util.function.Predicate;

/** Read the visible competitor UI without waiting for its continuously updating desktop to idle. */
public final class VistaReview extends Instrumentation {
    private final JSONArray observations=new JSONArray();
    private File directory;
    private static final String PACKAGE="rocks.gorjan.gokixp";
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){
        directory=new File(getTargetContext().getFilesDir(),"vista-review");directory.mkdirs();Bundle result=new Bundle();
        try{
            android.accessibilityservice.AccessibilityServiceInfo info=getUiAutomation().getServiceInfo();info.flags|=AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS|AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;getUiAutomation().setServiceInfo(info);
            launch();capture("03-vista-desktop");
            tapId("start_button",true);capture("04-vista-start-menu");
            if(tapId("all_programs",false))capture("05-all-programs");
            launch();
            for(String[] target:new String[][]{{"Computer","computer"},{"My Computer","my-computer"},{"Notepad","notepad"},{"Recycle Bin","recycle-bin"},{"Paint","paint"}}){
                boolean found=tap(n->target[0].contentEquals(n.getText()==null?"":n.getText()),false,false);
                if(!found){tapId("start_button",false);found=tap(n->target[0].contentEquals(n.getText()==null?"":n.getText()),false,false);}
                if(found){capture("06-"+target[1]);tapId("dialog_close_button",false);}
                launch();
            }
            AccessibilityNodeInfo root=root();Rect bounds=new Rect();root.getBoundsInScreen(bounds);gesture(bounds.left+bounds.width()*.52f,bounds.top+bounds.height()*.45f,true);capture("07-desktop-context-menu");
            if(tap(n->n.getText()!=null&&n.getText().toString().toLowerCase().contains("properties"),false,false))capture("08-display-properties");
            launch();shell("settings put system accelerometer_rotation 0");shell("settings put system user_rotation 1");Thread.sleep(1800);capture("09-vista-landscape");tapId("start_button",true);capture("10-landscape-start");
            result.putString("stream","PASS: Official Windows Launcher v2.1.6 installed; Vista desktop, Start, available tools and landscape captured.\n");finish(Activity.RESULT_OK,result);
        }catch(Throwable error){try{capture("failure");}catch(Exception ignored){}result.putString("stream","FAIL: "+error+"\n");error.printStackTrace();finish(Activity.RESULT_CANCELED,result);}
        finally{try{write("observations.json",observations.toString(2));}catch(Exception ignored){}}
    }
    private void launch() throws Exception{shell("am force-stop "+PACKAGE);shell("am start -n "+PACKAGE+"/.MainActivity");Thread.sleep(1600);for(int i=0;i<3;i++){if(!tap(n->n.getViewIdResourceName()!=null&&(n.getViewIdResourceName().endsWith("/permission_deny_button")||n.getViewIdResourceName().endsWith("/dialog_close_button")),false,false))break;}}
    private AccessibilityNodeInfo root() throws Exception{for(int i=0;i<35;i++){AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();if(root!=null)return root;Thread.sleep(120);}throw new IOException("No active UI root");}
    private AccessibilityNodeInfo find(AccessibilityNodeInfo node,Predicate<AccessibilityNodeInfo> match){if(node==null)return null;if(node.isVisibleToUser()&&match.test(node))return node;for(int i=0;i<node.getChildCount();i++){AccessibilityNodeInfo found=find(node.getChild(i),match);if(found!=null)return found;}return null;}
    private boolean tapId(String id,boolean required)throws Exception{return tap(n->n.getViewIdResourceName()!=null&&n.getViewIdResourceName().endsWith("/"+id),required,false);}
    private boolean tap(Predicate<AccessibilityNodeInfo> predicate,boolean required,boolean hold)throws Exception{AccessibilityNodeInfo node=find(root(),predicate);if(node==null){if(required)throw new IOException("Expected visible UI item missing");return false;}Rect bounds=new Rect();node.getBoundsInScreen(bounds);if(bounds.isEmpty())return false;gesture(bounds.exactCenterX(),bounds.exactCenterY(),hold);return true;}
    private void gesture(float x,float y,boolean hold)throws Exception{long down=SystemClock.uptimeMillis();inject(down,MotionEvent.ACTION_DOWN,x,y);Thread.sleep(hold?850:65);inject(down,MotionEvent.ACTION_UP,x,y);Thread.sleep(500);}
    private void inject(long down,int action,float x,float y){MotionEvent.PointerProperties pointer=new MotionEvent.PointerProperties();pointer.id=0;pointer.toolType=MotionEvent.TOOL_TYPE_FINGER;MotionEvent.PointerCoords coordinate=new MotionEvent.PointerCoords();coordinate.x=x;coordinate.y=y;coordinate.pressure=action==MotionEvent.ACTION_UP?0:1;coordinate.size=1;MotionEvent event=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,1,new MotionEvent.PointerProperties[]{pointer},new MotionEvent.PointerCoords[]{coordinate},0,0,1,1,0,0,InputDevice.SOURCE_TOUCHSCREEN,0);getUiAutomation().injectInputEvent(event,true);event.recycle();}
    private void capture(String name)throws Exception{Thread.sleep(250);Bitmap screenshot=getUiAutomation().takeScreenshot();if(screenshot==null)throw new IOException("No screenshot");try(FileOutputStream stream=new FileOutputStream(new File(directory,name+".png"))){screenshot.compress(Bitmap.CompressFormat.PNG,100,stream);}finally{screenshot.recycle();}JSONArray nodes=new JSONArray();dump(root(),nodes);JSONObject item=new JSONObject().put("capture",name).put("nodes",nodes);observations.put(item);write(name+".json",item.toString(2));}
    private void dump(AccessibilityNodeInfo node,JSONArray output)throws Exception{if(node==null)return;Rect bounds=new Rect();node.getBoundsInScreen(bounds);output.put(new JSONObject().put("text",node.getText()).put("description",node.getContentDescription()).put("id",node.getViewIdResourceName()).put("bounds",bounds.toShortString()).put("visible",node.isVisibleToUser()));for(int i=0;i<node.getChildCount();i++)dump(node.getChild(i),output);}
    private void write(String name,String text)throws IOException{try(FileOutputStream stream=new FileOutputStream(new File(directory,name))){stream.write(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));}}
    private String shell(String command)throws IOException{try(ParcelFileDescriptor descriptor=getUiAutomation().executeShellCommand(command);InputStream input=new ParcelFileDescriptor.AutoCloseInputStream(descriptor);ByteArrayOutputStream output=new ByteArrayOutputStream()){byte[] b=new byte[4096];int count;while((count=input.read(b))!=-1)output.write(b,0,count);return output.toString("UTF-8");}}
}
