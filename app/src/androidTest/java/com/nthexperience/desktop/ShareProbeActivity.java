package com.nthexperience.desktop;

import android.app.Activity;
import android.content.*;
import android.net.Uri;
import android.os.*;
import org.json.*;
import java.io.*;
import java.security.*;

/** A different app UID verifies real Send to grants and bytes. Test APK only. */
public final class ShareProbeActivity extends Activity {
    @Override public void onCreate(Bundle state){super.onCreate(state);JSONObject result=new JSONObject();try{ClipData clip=getIntent().getClipData();if(clip==null)throw new IOException("No selected URIs");result.put("count",clip.getItemCount());JSONArray hashes=new JSONArray();boolean denied=true;for(int i=0;i<clip.getItemCount();i++){Uri uri=clip.getItemAt(i).getUri();MessageDigest hash=MessageDigest.getInstance("SHA-256");try(InputStream input=getContentResolver().openInputStream(uri)){byte[] bytes=new byte[4096];int n;while((n=input.read(bytes))!=-1)hash.update(bytes,0,n);}StringBuilder hex=new StringBuilder();for(byte value:hash.digest())hex.append(String.format(java.util.Locale.ROOT,"%02x",value&255));hashes.put(hex.toString());try(ParcelFileDescriptor descriptor=getContentResolver().openFileDescriptor(uri,"w")){denied=false;}catch(SecurityException expected){}}result.put("hashes",hashes).put("writeDenied",denied);}catch(Exception failure){try{result.put("error",failure.toString());}catch(JSONException ignored){}}try(FileOutputStream output=openFileOutput("share-result.json",0)){output.write(result.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}catch(IOException ignored){}finish();}
}
