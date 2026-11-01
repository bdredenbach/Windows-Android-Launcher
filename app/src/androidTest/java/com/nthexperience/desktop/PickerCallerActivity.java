package com.nthexperience.desktop;

import android.app.Activity;
import android.content.*;
import android.net.Uri;
import android.os.*;
import org.json.*;
import java.io.*;
import java.util.*;

/** Runs as the test APK's own UID, distinct from both launcher and fixture provider. */
public final class PickerCallerActivity extends Activity {
    private String token;
    @Override public void onCreate(Bundle state){super.onCreate(state);token=getIntent().getStringExtra("token");Intent request=new Intent(getIntent().getBooleanExtra("explicit",false)?PickerSpec.ACTION:Intent.ACTION_GET_CONTENT).setType("text/plain").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_ALLOW_MULTIPLE,getIntent().getBooleanExtra("multiple",false));if(getIntent().getBooleanExtra("explicit",false))request.setComponent(new ComponentName("com.nthexperience.desktop","com.nthexperience.desktop.ExplorerPickerActivity"));else request.setPackage("com.nthexperience.desktop");startActivityForResult(request,1);}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);try{JSONObject report=new JSONObject().put("token",token).put("result",result).put("uid",android.os.Process.myUid());JSONArray values=new JSONArray(),content=new JSONArray();List<Uri> uris=new ArrayList<>();if(data!=null&&data.getClipData()!=null)for(int i=0;i<data.getClipData().getItemCount();i++)uris.add(data.getClipData().getItemAt(i).getUri());else if(data!=null&&data.getData()!=null)uris.add(data.getData());boolean read=true,writeDenied=true;for(Uri uri:uris){values.put(uri.toString());try(InputStream in=getContentResolver().openInputStream(uri)){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] bytes=new byte[4096];int n;while((n=in.read(bytes))!=-1&&out.size()<65536)out.write(bytes,0,n);content.put(out.toString("UTF-8"));}catch(Exception ex){read=false;}try(ParcelFileDescriptor ignored=getContentResolver().openFileDescriptor(uri,"w")){writeDenied=false;}catch(Exception expected){}}
        boolean denied=false;Uri tree=android.provider.DocumentsContract.buildTreeDocumentUri("com.nthexperience.desktop.fixture.documents","root:"),unselected=android.provider.DocumentsContract.buildDocumentUriUsingTree(tree,"root:Unselected.txt");try(InputStream in=getContentResolver().openInputStream(unselected)){}catch(Exception expected){denied=true;}report.put("uris",values).put("contents",content).put("read",read).put("writeDenied",writeDenied).put("unselectedDenied",denied);try(FileOutputStream out=openFileOutput("picker-result.json",MODE_PRIVATE)){out.write(report.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    }catch(Exception ex){android.util.Log.e("NthPickerClient","Test result failed",ex);}finish();}
}
