package com.nthexperience.desktop;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.pdf.PdfRenderer;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.*;
import android.provider.*;
import org.json.*;
import java.io.*;
import java.security.MessageDigest;
import java.util.zip.ZipInputStream;

/** Runs as the test APK UID: opens and decodes real files delivered by Nth. */
public final class FileViewActivity extends Activity {
    @Override public void onCreate(Bundle state){super.onCreate(state);JSONObject report=new JSONObject();try{
        Intent intent=getIntent();Uri uri=intent.getData();String name="";try(android.database.Cursor cursor=getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(cursor!=null&&cursor.moveToFirst())name=cursor.getString(0);}
        byte[] bytes;try(InputStream in=getContentResolver().openInputStream(uri);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1){out.write(buffer,0,n);if(out.size()>4*1024*1024)throw new IOException("Fixture too large");}bytes=out.toByteArray();}
        String mime=intent.getType();boolean decoded=false;
        if(mime.startsWith("image/")){Bitmap bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length);decoded=bitmap!=null&&bitmap.getWidth()==80&&bitmap.getHeight()==60;if(bitmap!=null)bitmap.recycle();}
        else if(mime.equals("application/pdf")){try(ParcelFileDescriptor descriptor=getContentResolver().openFileDescriptor(uri,"r");PdfRenderer renderer=new PdfRenderer(descriptor)){decoded=renderer.getPageCount()==1;}}
        else if(mime.startsWith("audio/")||mime.startsWith("video/")){MediaMetadataRetriever media=new MediaMetadataRetriever();try{media.setDataSource(this,uri);decoded=Long.parseLong(media.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION))>=500;}finally{media.release();}}
        else if(mime.equals("application/zip")){try(ZipInputStream zip=new ZipInputStream(new ByteArrayInputStream(bytes))){decoded=zip.getNextEntry()!=null&&zip.read()!=-1;}}
        else if(mime.equals("application/json"))decoded=new JSONObject(new String(bytes,java.nio.charset.StandardCharsets.UTF_8)).getBoolean("nth");
        else if(mime.equals("text/plain"))decoded=new String(bytes,java.nio.charset.StandardCharsets.UTF_8).contains("café");
        boolean writeDenied=false,unselectedDenied=false;try(ParcelFileDescriptor ignored=getContentResolver().openFileDescriptor(uri,"w")){}catch(Exception expected){writeDenied=true;}
        String id=DocumentsContract.getDocumentId(uri),parent=id.substring(0,id.lastIndexOf('/')+1);Uri unselected=DocumentsContract.buildDocumentUriUsingTree(uri,parent+"Unselected.bin");try(InputStream ignored=getContentResolver().openInputStream(unselected)){}catch(Exception expected){unselectedDenied=true;}
        StringBuilder digest=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(bytes))digest.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
        report.put("name",name).put("mime",mime).put("decoded",decoded).put("sha256",digest).put("writeDenied",writeDenied).put("unselectedDenied",unselectedDenied).put("flags",intent.getFlags());
    }catch(Exception ex){try{report.put("error",ex.toString());}catch(JSONException ignored){}}try(FileOutputStream out=openFileOutput("file-open-result.json",MODE_PRIVATE)){out.write(report.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}catch(Exception ex){android.util.Log.e("NthFileViewer","Report failed",ex);}finish();}
}
