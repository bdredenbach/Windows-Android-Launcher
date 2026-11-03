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
    static long capacity(Context context){return Math.max(100,Math.min(2048,context.getSharedPreferences("desktop",0).getInt("recycle_capacity_mb",250)))*1024L*1024;}
    private static File root(Context context)throws IOException{File root=new File(context.getFilesDir(),"recycle-bin");if(!root.isDirectory()&&!root.mkdirs())throw new IOException("Recycle Bin storage is unavailable. Original kept.");return root;}
    static synchronized List<JSONObject> list(Context context)throws IOException{
        List<JSONObject> items=new ArrayList<>();Set<String> manifests=new HashSet<>();File[] files=root(context).listFiles((dir,name)->name.endsWith(".json")||name.endsWith(".json.bak"));if(files!=null)for(File file:files)manifests.add(file.getName().replace(".json.bak",".json"));for(String file:manifests)try{JSONObject item=new JSONObject(new String(new AtomicFile(new File(root(context),file)).readFully(),StandardCharsets.UTF_8));String id=item.getString("id");UUID.fromString(id);if(file.equals(id+".json")&&(item.has("shortcut")||new File(root(context),id+".data").isFile()))items.add(item);}catch(Exception ignored){}
        items.sort((a,b)->Long.compare(b.optLong("date"),a.optLong("date")));return items;
    }
    private static void metadata(Context context,JSONObject item)throws Exception{AtomicFile file=new AtomicFile(new File(root(context),item.getString("id")+".json"));FileOutputStream out=null;try{out=file.startWrite();out.write(item.toString().getBytes(StandardCharsets.UTF_8));file.finishWrite(out);}catch(Exception ex){if(out!=null)file.failWrite(out);throw ex;}}
    static void trashShortcut(Context context, DesktopStore store, String source, String shortcut, String name) {
        if ("@recycle".equals(shortcut)) return;
        List<String> members = ExplorerClipboard.members(store, source);
        int position = members.indexOf(shortcut);
        if (position < 0) return;
        try {
            recordShortcut(context, source, shortcut, name, position);
            members.remove(position);
            ExplorerClipboard.members(store, source, members);
        } catch (Exception ex) {
            android.widget.Toast.makeText(context, ex.getMessage(), android.widget.Toast.LENGTH_LONG).show();
        }
    }

    private static synchronized void recordShortcut(Context context, String source, String shortcut,
                                                    String name, int position) throws Exception {
        if (list(context).size() >= 200) throw new IOException("Recycle Bin is full. Shortcut kept.");
        String id = UUID.randomUUID().toString();
        metadata(context, new JSONObject().put("id", id).put("name", name)
                .put("shortcut", shortcut).put("source", source).put("position", position)
                .put("location", source.equals("quick") ? "Quick Launch" : source.equals("managers")
                        ? "File Managers" : source.startsWith("contents:") ? "App folder" : "Desktop")
                .put("date", System.currentTimeMillis()).put("deleted", true));
    }

    private static void restoreShortcut(Context context, JSONObject item) throws Exception {
        DesktopStore store = new DesktopStore(context);
        String source = item.getString("source");
        if (source.startsWith("contents:") && !store.isFolder(source.substring(9))) source = "desktop";
        String shortcut = item.getString("shortcut");
        if ((shortcut.startsWith("folder:") && !store.isFolder(shortcut))
                || (FolderLinks.id(shortcut) && FolderLinks.get(store, shortcut) == null))
            throw new IOException("The original shortcut target is unavailable.");
        List<String> members = ExplorerClipboard.members(store, source);
        if (!members.contains(shortcut)) {
            int limit = source.equals("quick") ? DesktopStore.MAX_QUICK
                    : source.startsWith("contents:") ? DesktopStore.MAX_ITEMS : 512;
            if (members.size() >= limit) throw new IOException("The original shortcut location is full.");
            members.add(Math.min(members.size(), item.getInt("position")), shortcut);
            ExplorerClipboard.members(store, source, members);
        }
        remove(context, item);
    }

    static MessageDigest digest(){try{return MessageDigest.getInstance("SHA-256");}catch(NoSuchAlgorithmException ex){throw new AssertionError(ex);}}
    private static String hash(InputStream in,OutputStream out,long limit,DocumentOps.Cancel cancel)throws IOException{MessageDigest digest=digest();byte[] buffer=new byte[65536];long total=0;int n;while((n=in.read(buffer))!=-1){cancel.check();total+=n;if(total>limit)throw new IOException("This item exceeds the Recycle Bin limit. Original kept.");if(out!=null)out.write(buffer,0,n);digest.update(buffer,0,n);}return hex(digest.digest());}
    private static String hex(byte[] digest){StringBuilder hex=new StringBuilder();for(byte b:digest)hex.append(String.format(Locale.ROOT,"%02x",b&255));return hex.toString();}
    static synchronized void trash(Context context,DocumentOps.Ref source,DocumentOps.Cancel cancel)throws Exception{trash(context,source,cancel,null);}
    static synchronized void trash(Context context,DocumentOps.Ref source,DocumentOps.Cancel cancel,String location)throws Exception{
        DocumentFolders.Item before=DocumentOps.stat(context,source.uri,cancel);
        if(!DocumentOps.name(before.name).equals(before.name))throw new IOException("This name cannot be restored safely. Original kept.");
        if(source.parent==null||!DocumentOps.writable(context,source.uri,DocumentsContract.Document.FLAG_SUPPORTS_DELETE,before)||!DocumentOps.writable(context,source.parent,DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE,DocumentOps.stat(context,source.parent,cancel)))throw new IOException("This location cannot use Recycle Bin. Original kept.");
        long used=0;File[] stored=root(context).listFiles((dir,name)->name.endsWith(".data"));if(stored!=null)for(File file:stored)used+=file.length();long available=Math.min(FILE_LIMIT,Math.min(capacity(context)-used,root(context).getUsableSpace()-5*1024*1024));if(list(context).size()>=200)throw new IOException("Recycle Bin holds up to 200 items. Empty it before moving more. Original kept.");if(available<1||(!before.directory()&&before.size>available))throw new IOException("Recycle Bin is full or this file exceeds its 100 MB limit. Original kept. Empty the bin or explicitly delete permanently.");
        String id=UUID.randomUUID().toString();File copy=new File(root(context),id+".data");boolean recorded=false;
        try{
            String checksum;
            if(before.directory()){
                try(FileOutputStream out=new FileOutputStream(copy)){
                    checksum=hex(RecycleFolder.archive(context,source.uri,unsyncedClose(out),available,cancel));
                    out.getFD().sync();
                }
            }else try(InputStream in=read(context,source.uri,cancel);FileOutputStream out=new FileOutputStream(copy)){
                checksum=hash(in,out,available,cancel);out.getFD().sync();
            }
            if(!before.directory()&&before.size>=0&&copy.length()!=before.size)throw changedSource();
            try(InputStream in=new FileInputStream(copy)){
                if(!checksum.equals(hash(in,null,available,cancel)))throw new IOException("Recovery copy verification failed. Original kept.");
            }
            DocumentFolders.Item now=DocumentOps.stat(context,source.uri,cancel);
            if(now.size!=before.size||now.modified!=before.modified||!now.name.equals(before.name))throw changedSource();
            if(before.directory()){
                if(!checksum.equals(hex(RecycleFolder.archive(context,source.uri,discard(),available,cancel))))throw changedSource();
            }else try(InputStream in=read(context,source.uri,cancel)){
                if(!checksum.equals(hash(in,null,available,cancel)))throw changedSource();
            }
            JSONObject item=new JSONObject().put("id",id).put("name",before.name).put("mime",before.mime).put("uri",source.uri.toString()).put("parent",source.parent.toString()).put("date",System.currentTimeMillis()).put("size",copy.length()).put("hash",checksum).put("deleted",false).put("location",location==null?DocumentOps.stat(context,source.parent,cancel).name:location);
            metadata(context,item);recorded=true;cancel.check();
            if(!DocumentsContract.deleteDocument(context.getContentResolver(),source.uri))throw new IOException("Original could not be deleted. A recovery copy was kept in Recycle Bin.");
            item.put("deleted",true);metadata(context,item);
        }finally{if(!recorded)copy.delete();}
    }
    private static IOException changedSource(){return new IOException("Source changed while reading. Original kept.");}
    private static OutputStream discard(){return new OutputStream(){
        @Override public void write(int value){}
        @Override public void write(byte[] bytes,int offset,int length){}
    };}
    private static OutputStream unsyncedClose(FileOutputStream file){return new FilterOutputStream(file){
        @Override public void write(byte[] bytes,int offset,int length)throws IOException{out.write(bytes,offset,length);}
        @Override public void close()throws IOException{flush();}
    };}
    static InputStream read(Context context,Uri uri,DocumentOps.Cancel cancel)throws IOException{
        ParcelFileDescriptor file=context.getContentResolver().openFileDescriptor(uri,"r",cancel.signal);if(file==null)throw new IOException("File unavailable");try{cancel.track(file);}catch(RuntimeException ex){file.close();throw ex;}return new ParcelFileDescriptor.AutoCloseInputStream(file){@Override public void close()throws IOException{cancel.untrack(file);super.close();}};
    }
    static OutputStream write(Context context,Uri uri,DocumentOps.Cancel cancel)throws IOException{
        ParcelFileDescriptor file=context.getContentResolver().openFileDescriptor(uri,"w",cancel.signal);if(file==null)throw new IOException("Destination unavailable");try{cancel.track(file);}catch(RuntimeException ex){file.close();throw ex;}return new ParcelFileDescriptor.AutoCloseOutputStream(file){@Override public void close()throws IOException{cancel.untrack(file);super.close();}};
    }
    static synchronized Uri restore(Context context,JSONObject item,DocumentOps.Cancel cancel)throws Exception{
        if(item.has("shortcut")){restoreShortcut(context,item);return null;}
        File copy=new File(root(context),item.getString("id")+".data");String checksum=item.getString("hash");try(InputStream in=new FileInputStream(copy)){if(!checksum.equals(hash(in,null,FILE_LIMIT,cancel)))throw new IOException("The recovery copy is damaged. It has been kept.");}
        Uri parent=Uri.parse(item.getString("parent"));DocumentFolders.Item parentInfo=DocumentOps.stat(context,parent,cancel);if(!DocumentOps.writable(context,parent,DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE,parentInfo))throw new IOException("Reconnect the original folder in Computer, then try Restore again.");
        String name=DocumentOps.available(context,parent,item.getString("name"),true,cancel);Uri destination=DocumentsContract.createDocument(context.getContentResolver(),parent,item.getString("mime"),name);if(destination==null)throw new IOException("The original folder could not create the restored file.");
        boolean verified=false;try{
            boolean folder=DocumentsContract.Document.MIME_TYPE_DIR.equals(item.getString("mime"));
            String restored;
            if(folder){
                RecycleFolder.extract(context,copy,destination,cancel);
                restored=hex(RecycleFolder.archive(context,destination,discard(),FILE_LIMIT,cancel));
            }else{
                try(InputStream in=new FileInputStream(copy);OutputStream out=write(context,destination,cancel)){
                    hash(in,out,FILE_LIMIT,cancel);out.flush();
                }
                try(InputStream in=read(context,destination,cancel)){restored=hash(in,null,FILE_LIMIT,cancel);}
            }
            if(!checksum.equals(restored))throw new IOException("Restored item could not be verified. Recovery copy kept.");
            verified=true;remove(context,item);return destination;
        }finally{if(!verified)try{DocumentsContract.deleteDocument(context.getContentResolver(),destination);}catch(Exception ignored){}}
    }
    static synchronized void remove(Context context,JSONObject item)throws Exception{File root=root(context);File payload=new File(root,item.getString("id")+".data");if(payload.exists()&&!payload.delete())throw new IOException("The recovery file could not be removed.");new AtomicFile(new File(root,item.getString("id")+".json")).delete();}
    static synchronized void empty(Context context)throws Exception{for(JSONObject item:list(context))remove(context,item);File[] remaining=root(context).listFiles();if(remaining!=null)for(File file:remaining)if(file.isFile()&&!file.delete())throw new IOException("Some recovery data could not be removed.");}
}
