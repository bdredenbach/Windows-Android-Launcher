package com.nthexperience.desktop;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.CancellationSignal;
import android.provider.DocumentsContract;
import org.json.*;
import java.text.Collator;
import java.util.*;

/** Read-only access to folders explicitly granted by Android's document picker. */
final class DocumentFolders {
    static final class Item {
        final String id,name,mime;final long size,modified;
        Item(String id,String name,String mime,long size,long modified){this.id=id;this.name=name;this.mime=mime;this.size=size;this.modified=modified;}
        boolean directory(){return DocumentsContract.Document.MIME_TYPE_DIR.equals(mime);}
    }
    static final class Listing {final List<Item> items;final boolean limited;Listing(List<Item> items,boolean limited){this.items=items;this.limited=limited;}}
    static JSONArray links(DesktopStore store){try{return new JSONArray(store.prefs.getString("linked_folders","[]"));}catch(JSONException|ClassCastException ex){return new JSONArray();}}
    static void add(DesktopStore store,Uri uri,String name){JSONArray saved=links(store),result=new JSONArray();JSONObject folder=new JSONObject();GadgetLayout.put(folder,"uri",uri.toString());GadgetLayout.put(folder,"name",name);result.put(folder);for(int i=0;i<saved.length()&&result.length()<16;i++){JSONObject item=saved.optJSONObject(i);if(item!=null&&!uri.toString().equals(item.optString("uri")))result.put(item);}store.prefs.edit().putString("linked_folders",result.toString()).apply();}
    static void remove(Context context,DesktopStore store,String uri){JSONArray saved=links(store),result=new JSONArray();for(int i=0;i<saved.length();i++){JSONObject item=saved.optJSONObject(i);if(item!=null&&!uri.equals(item.optString("uri")))result.put(item);}store.prefs.edit().putString("linked_folders",result.toString()).apply();try{context.getContentResolver().releasePersistableUriPermission(Uri.parse(uri),Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(SecurityException ignored){}}
    static String rootPage(String tree,String name){JSONObject page=new JSONObject(),crumb=new JSONObject();GadgetLayout.put(page,"tree",tree);GadgetLayout.put(crumb,"id",DocumentsContract.getTreeDocumentId(Uri.parse(tree)));GadgetLayout.put(crumb,"name",name);GadgetLayout.put(page,"crumbs",new JSONArray().put(crumb));return "folder:"+page;}
    static JSONObject path(String page) throws JSONException {return new JSONObject(page.substring(7));}
    static String childPage(String page,Item item) throws JSONException {JSONObject path=path(page),crumb=new JSONObject();GadgetLayout.put(crumb,"id",item.id);GadgetLayout.put(crumb,"name",item.name);path.getJSONArray("crumbs").put(crumb);return "folder:"+path;}
    static Uri documentUri(String page,String id) throws JSONException {return DocumentsContract.buildDocumentUriUsingTree(Uri.parse(path(page).getString("tree")),id);}
    static String rootName(Context context,Uri tree){Uri root=DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));try(Cursor cursor=context.getContentResolver().query(root,new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME},null,null,null)){if(cursor!=null&&cursor.moveToFirst())return cursor.getString(0);}return "Linked folder";}
    static Listing read(Context context,String page,CancellationSignal signal) throws JSONException {
        JSONObject path=path(page);JSONArray crumbs=path.getJSONArray("crumbs");String id=crumbs.getJSONObject(crumbs.length()-1).getString("id");Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(Uri.parse(path.getString("tree")),id);
        String[] projection={DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE,DocumentsContract.Document.COLUMN_SIZE,DocumentsContract.Document.COLUMN_LAST_MODIFIED};List<Item> items=new ArrayList<>();boolean limited=false;
        try(Cursor cursor=context.getContentResolver().query(children,projection,null,null,null,signal)){
            if(cursor==null)throw new IllegalStateException("Folder unavailable");
            while(cursor.moveToNext()){signal.throwIfCanceled();if(items.size()>=2000){limited=true;break;}String childId=cursor.getString(0),name=cursor.getString(1),mime=cursor.getString(2);if(childId==null)continue;items.add(new Item(childId,name==null?"Unnamed item":name,mime==null?"application/octet-stream":mime,cursor.isNull(3)?-1:cursor.getLong(3),cursor.isNull(4)?0:cursor.getLong(4)));}
        }
        Collator collator=Collator.getInstance();items.sort((a,b)->{if(a.directory()!=b.directory())return a.directory()?-1:1;return collator.compare(a.name,b.name);});return new Listing(items,limited);
    }
}
