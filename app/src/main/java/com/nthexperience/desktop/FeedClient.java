package com.nthexperience.desktop;

import android.net.Uri;
import android.os.*;
import android.text.Html;
import org.json.*;
import org.xmlpull.v1.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/** A small RSS/Atom reader. Only titles and article links are retained. */
final class FeedClient {
    private final DesktopStore store;private final Runnable changed;private final Handler main=new Handler(Looper.getMainLooper());private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private boolean active,closed,loading;private volatile HttpURLConnection connection;private volatile int generation;private String error="";
    FeedClient(DesktopStore store,Runnable changed){this.store=store;this.changed=changed;}
    static boolean validUrl(String value){try{Uri uri=Uri.parse(value);return value.length()<=2048&&"https".equalsIgnoreCase(uri.getScheme())&&uri.getHost()!=null&&!uri.getHost().isEmpty()&&uri.getUserInfo()==null;}catch(RuntimeException ex){return false;}}
    String url(){return store.prefs.getString("feed_url","");}
    String status(){return loading?"Updating headlines…":!error.isEmpty()?error:url().isEmpty()?"Choose an RSS or Atom feed.":store.prefs.getLong("feed_updated",0)>0?"Cached headlines • "+android.text.format.DateFormat.getTimeFormat(store.context).format(new Date(store.prefs.getLong("feed_updated",0))):"No headlines yet.";}
    static JSONArray items(DesktopStore store){try{return new JSONArray(store.prefs.getString("feed_items","[]"));}catch(JSONException ex){return new JSONArray();}}
    void active(boolean value){active=value;if(value)refresh(false);}
    void configure(String url){generation++;HttpURLConnection old=connection;if(old!=null)old.disconnect();loading=false;error="";store.prefs.edit().putString("feed_url",url).remove("feed_items").remove("feed_updated").remove("feed_expires").apply();changed.run();refresh(true);}
    void refresh(boolean requested){
        if(closed||!active||loading||!validUrl(url()))return;long now=System.currentTimeMillis();if(now<store.prefs.getLong("feed_expires",0)){if(requested)changed.run();return;}loading=true;error="";int request=generation;String address=url();changed.run();
        worker.execute(()->{if(closed||request!=generation)return;JSONArray result=null;String failure="";long expires=System.currentTimeMillis()+900000;HttpURLConnection current=null;try{
            String next=address;for(int i=0;i<4;i++){if(!validUrl(next))throw new IOException("Use an HTTPS feed address");current=(HttpURLConnection)new URL(next).openConnection();connection=current;current.setInstanceFollowRedirects(false);current.setRequestProperty("User-Agent",WeatherClient.AGENT);current.setConnectTimeout(6000);current.setReadTimeout(8000);int code=current.getResponseCode();if(code>=300&&code<400){String target=current.getHeaderField("Location");String redirected=new URL(new URL(next),target).toString();current.disconnect();current=null;next=redirected;continue;}if(code!=200)throw new IOException("Feed service returned "+code);expires=Math.max(expires,current.getHeaderFieldDate("Expires",expires));result=parse(CitySearch.read(current.getInputStream(),512000),next);break;}if(result==null)throw new IOException("Too many redirects");if(result.length()==0)throw new IOException("No readable article headlines in this feed");
        }catch(Exception ex){failure="Headlines unavailable. Saved headlines are kept; check the feed address or connection.";}finally{if(current!=null)current.disconnect();if(connection==current)connection=null;}
            JSONArray headlines=result;String message=failure;long until=expires;main.post(()->{if(closed||request!=generation)return;loading=false;error=message;android.content.SharedPreferences.Editor edit=store.prefs.edit().putLong("feed_expires",until);if(headlines!=null&&message.isEmpty())edit.putString("feed_items",headlines.toString()).putLong("feed_updated",System.currentTimeMillis());edit.apply();changed.run();});
        });
    }
    static JSONArray parse(String xml,String base)throws Exception {
        if(xml.toUpperCase(Locale.ROOT).contains("<!DOCTYPE"))throw new IOException("Unsupported XML declaration");XmlPullParser parser=XmlPullParserFactory.newInstance().newPullParser();parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES,true);parser.setInput(new StringReader(xml));JSONArray items=new JSONArray();Set<String> seen=new HashSet<>();int entryDepth=-1;String title="",link="";
        for(int event=parser.getEventType();event!=XmlPullParser.END_DOCUMENT&&items.length()<12;event=parser.next()){
            if(event==XmlPullParser.START_TAG){String tag=parser.getName();if(tag.equalsIgnoreCase("item")||tag.equalsIgnoreCase("entry")){entryDepth=parser.getDepth();title="";link="";}else if(entryDepth>0&&tag.equalsIgnoreCase("title")){title=text(parser);}else if(entryDepth>0&&tag.equalsIgnoreCase("link")){String href=parser.getAttributeValue(null,"href"),rel=parser.getAttributeValue(null,"rel");if(href!=null&&(rel==null||rel.equals("alternate")))link=href;else if(href==null)link=text(parser);}}
            else if(event==XmlPullParser.END_TAG&&parser.getDepth()==entryDepth){entryDepth=-1;if(title.isEmpty()||link.isEmpty())continue;String target;try{target=new URL(new URL(base),link.trim()).toString();}catch(MalformedURLException invalid){continue;}Uri uri=Uri.parse(target);if(!Arrays.asList("https","http").contains(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null||!seen.add(target))continue;String clean=Html.fromHtml(title,Html.FROM_HTML_MODE_LEGACY).toString().trim();if(clean.isEmpty())continue;items.put(new JSONObject().put("title",clean.substring(0,Math.min(180,clean.length()))).put("url",target));}
        }return items;
    }
    private static String text(XmlPullParser parser)throws Exception {int depth=parser.getDepth();StringBuilder text=new StringBuilder();for(int event=parser.next();!(event==XmlPullParser.END_TAG&&parser.getDepth()==depth)&&event!=XmlPullParser.END_DOCUMENT;event=parser.next())if(event==XmlPullParser.TEXT&&text.length()<1000)text.append(parser.getText());return text.toString().trim();}
    void close(){closed=true;active=false;generation++;HttpURLConnection old=connection;if(old!=null)old.disconnect();worker.shutdownNow();main.removeCallbacksAndMessages(null);}
}
