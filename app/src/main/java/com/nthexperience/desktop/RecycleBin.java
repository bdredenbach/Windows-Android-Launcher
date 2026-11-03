package com.nthexperience.desktop;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.util.AtomicFile;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/** Verified private recovery copies. Metadata is durable before deleting any source. */
final class RecycleBin {
    static final long LIMIT=250L*1024*1024,FILE_LIMIT=100L*1024*1024;
    private static File root(Context context)throws IOException{File root=new File(context.getFilesDir(),"recycle-bin");if(!root.isDirectory()&&!root.mkdirs())throw new IOException("Recycle Bin storage is unavailable. Original kept.");return root;}
    static synchronized List<JSONObject> list(Context context)throws IOException{
        List<JSONObject> items=new ArrayList<>();Set<String> manifests=new HashSet<>();File[] files=root(context).listFiles((dir,name)->name.endsWith(".json")||name.endsWith(".json.bak"));if(files!=null)for(File file:files)manifests.add(file.getName().replace(".json.bak",".json"));for(String file:manifests)try{JSONObject item=new JSONObject(new String(new AtomicFile(new File(root(context),file)).readFully(),StandardCharsets.UTF_8));String id=item.getString("id");UUID.fromString(id);if(file.equals(id+".json")&&new File(root(context),id+".data").isFile())items.add(item);}catch(Exception ignored){}
        items.sort((a,b)->Long.compare(b.optLong("date"),a.optLong("date")));return items;
    }
    private static void metadata(Context context,JSONObject item)throws Exception{AtomicFile file=new AtomicFile(new File(root(context),item.getString("id")+".json"));FileOutputStream out=null;try{out=file.startWrite();out.write(item.toString().getBytes(StandardCharsets.UTF_8));file.finishWrite(out);}catch(Exception ex){if(out!=null)file.failWrite(out);throw ex;}}
    private static MessageDigest digest(){try{return MessageDigest.getInstance("SHA-256");}catch(NoSuchAlgorithmException ex){throw new AssertionError(ex);}}
    private static String hash(InputStream in,OutputStream out,long limit,DocumentOps.Cancel cancel)throws IOException{MessageDigest digest=digest();byte[] buffer=new byte[65536];long total=0;int n;while((n=in.read(buffer))!=-1){cancel.check();total+=n;if(total>limit)throw new IOException("This item exceeds the Recycle Bin limit. Original kept.");if(out!=null)out.write(buffer,0,n);digest.update(buffer,0,n);}StringBuilder hex=new StringBuilder();for(byte b:digest.digest())hex.append(String.format(Locale.ROOT,"%02x",b&255));return hex.toString();}
    static synchronized void trash(Context context,DocumentOps.Ref source,DocumentOps.Cancel cancel)throws Exception{
        DocumentFolders.Item before=DocumentOps.stat(context,source.uri,cancel);
        if(before.directory()||source.parent==null||!DocumentOps.writable(context,source.uri,DocumentsContract.Document.FLAG_SUPPORTS_DELETE,before)||!DocumentOps.writable(context,source.parent,DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE,DocumentOps.stat(context,source.parent,cancel)))throw new IOException("This location cannot use Recycle Bin. Original kept.");
        long used=0;File[] stored=root(context).listFiles((dir,name)->name.endsWith(".data"));if(stored!=null)for(File file:stored)used+=file.length();long available=Math.min(FILE_LIMIT,Math.min(LIMIT-used,root(context).getUsableSpace()-5*1024*1024));if(stored!=null&&stored.length>=200)throw new IOException("Recycle Bin holds up to 200 files. Empty it before moving more. Original kept.");if(available<1||before.size>available)throw new IOException("Recycle Bin is full or this file exceeds its 100 MB limit. Original kept. Empty the bin or explicitly delete permanently.");
        String id=UUID.randomUUID().toString();File copy=new File(root(context),id+".data");boolean recorded=false;
        try{
            String checksum;try(InputStream in=read(context,source.uri,cancel);FileOutputStream out=new FileOutputStream(copy)){if(in==null)throw new IOException("File unavailable. Original kept.");checksum=hash(in,out,available,cancel);out.getFD().sync();}
            if(before.size>=0&&copy.length()!=before.size)throw new IOException("Source changed while reading. Original kept.");try(InputStream in=new FileInputStream(copy)){if(!checksum.equals(hash(in,null,available,cancel)))throw new IOException("Recovery copy verification failed. Original kept.");}
            DocumentFolders.Item now=DocumentOps.stat(context,source.uri,cancel);if(now.size!=before.size||now.modified!=before.modified||!now.name.equals(before.name))throw new IOException("Source changed while reading. Original kept.");
            // Verify the current source bytes too; providers may not update size/time reliably.
            try(InputStream in=read(context,source.uri,cancel)){if(in==null||!checksum.equals(hash(in,null,available,cancel)))throw new IOException("Source changed while reading. Original kept.");}
            JSONObject item=new JSONObject().put("id",id).put("name",before.name).put("mime",before.mime).put("uri",source.uri.toString()).put("parent",source.parent.toString()).put("date",System.currentTimeMillis()).put("size",copy.length()).put("hash",checksum).put("deleted",false);
            metadata(context,item);recorded=true;cancel.check();
            if(!DocumentsContract.deleteDocument(context.getContentResolver(),source.uri))throw new IOException("Original could not be deleted. A recovery copy was kept in Recycle Bin.");
            item.put("deleted",true);metadata(context,item);
        }finally{if(!recorded)copy.delete();}
    }
    private static InputStream read(Context context,Uri uri,DocumentOps.Cancel cancel)throws IOException{
        ParcelFileDescriptor file=context.getContentResolver().openFileDescriptor(uri,"r",cancel.signal);if(file==null)throw new IOException("File unavailable");try{cancel.track(file);}catch(RuntimeException ex){file.close();throw ex;}return new ParcelFileDescriptor.AutoCloseInputStream(file){@Override public void close()throws IOException{cancel.untrack(file);super.close();}};
    }
    private static OutputStream write(Context context,Uri uri,DocumentOps.Cancel cancel)throws IOException{
        ParcelFileDescriptor file=context.getContentResolver().openFileDescriptor(uri,"w",cancel.signal);if(file==null)throw new IOException("Destination unavailable");try{cancel.track(file);}catch(RuntimeException ex){file.close();throw ex;}return new ParcelFileDescriptor.AutoCloseOutputStream(file){@Override public void close()throws IOException{cancel.untrack(file);super.close();}};
    }
    static synchronized Uri restore(Context context,JSONObject item,DocumentOps.Cancel cancel)throws Exception{
        File copy=new File(root(context),item.getString("id")+".data");String checksum=item.getString("hash");try(InputStream in=new FileInputStream(copy)){if(!checksum.equals(hash(in,null,FILE_LIMIT,cancel)))throw new IOException("The recovery copy is damaged. It has been kept.");}
        Uri parent=Uri.parse(item.getString("parent"));DocumentFolders.Item parentInfo=DocumentOps.stat(context,parent,cancel);if(!DocumentOps.writable(context,parent,DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE,parentInfo))throw new IOException("Reconnect the original folder in Computer, then try Restore again.");
        String name=DocumentOps.available(context,parent,item.getString("name"),true,cancel);Uri destination=DocumentsContract.createDocument(context.getContentResolver(),parent,item.getString("mime"),name);if(destination==null)throw new IOException("The original folder could not create the restored file.");
        boolean verified=false;try{
            try(InputStream in=new FileInputStream(copy);OutputStream out=write(context,destination,cancel)){if(out==null)throw new IOException("The destination cannot be written.");hash(in,out,FILE_LIMIT,cancel);out.flush();}
            try(InputStream in=read(context,destination,cancel)){if(in==null||!checksum.equals(hash(in,null,FILE_LIMIT,cancel)))throw new IOException("Restored file could not be verified. Recovery copy kept.");}verified=true;remove(context,item);return destination;
        }finally{if(!verified)try{DocumentsContract.deleteDocument(context.getContentResolver(),destination);}catch(Exception ignored){}}
    }
    static synchronized void remove(Context context,JSONObject item)throws Exception{File root=root(context);File payload=new File(root,item.getString("id")+".data");if(payload.exists()&&!payload.delete())throw new IOException("The recovery file could not be removed.");new AtomicFile(new File(root,item.getString("id")+".json")).delete();}
    static synchronized void empty(Context context)throws Exception{for(JSONObject item:list(context))remove(context,item);File[] remaining=root(context).listFiles();if(remaining!=null)for(File file:remaining)if(file.isFile()&&!file.delete())throw new IOException("Some recovery data could not be removed.");}
}
