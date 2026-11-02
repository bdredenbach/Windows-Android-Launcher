package com.nthexperience.desktop;

import java.net.IDN;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Address-bar parsing is deliberately separate from WebView navigation. */
final class BrowserUrls {
    static boolean web(String value) {
        try {
            URI uri=new URI(value);
            return value.length()<=8192 && ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                && uri.getHost()!=null && uri.getRawUserInfo()==null;
        } catch(Exception ex) { return false; }
    }
    static String address(String input,String provider) {
        String value=input.trim();
        if(value.isEmpty())return "";
        if(value.length()>8192)throw new IllegalArgumentException("This address is too long.");
        if(value.matches("(?i)^https?://.*")) {
            try {
                URI raw=new URI(value.replace(" ","%20"));
                // Convert international host names without allowing credentials in the address bar.
                if(raw.getHost()==null && raw.getRawAuthority()!=null && !raw.getRawAuthority().contains("@")) {
                    String authority=IDN.toASCII(raw.getRawAuthority());
                    value=raw.getScheme()+"://"+authority+(raw.getRawPath()==null?"":raw.getRawPath())+(raw.getRawQuery()==null?"":"?"+raw.getRawQuery())+(raw.getRawFragment()==null?"":"#"+raw.getRawFragment());
                } else value=raw.toASCIIString();
            } catch(Exception ex) { throw new IllegalArgumentException("Check the website address."); }
            if(!web(value))throw new IllegalArgumentException("Enter a website address without a username or password.");
            return value;
        }
        // A host and optional numeric port are addresses; other explicit schemes are rejected.
        if(!value.contains(" ") && value.matches("(?i)(localhost|[a-z0-9\\p{L}][a-z0-9\\p{L}.\\-]*\\.[a-z0-9\\p{L}\\-]+)(:[0-9]{1,5})?([/?#].*)?"))return address("https://"+value,provider);
        if(value.matches("(?s)^[A-Za-z][A-Za-z0-9+.-]*:.*"))throw new IllegalArgumentException("Only http and https website addresses can open here.");
        String query;try{query=URLEncoder.encode(value,"UTF-8");}catch(java.io.UnsupportedEncodingException impossible){throw new AssertionError(impossible);}
        return ("DuckDuckGo".equals(provider)?"https://duckduckgo.com/?q=":"Google".equals(provider)?"https://www.google.com/search?q=":"https://www.bing.com/search?q=")+query;
    }
    static String origin(String value) {
        if(!web(value))return "";
        URI uri=URI.create(value);int port=uri.getPort();
        return uri.getScheme().toLowerCase(Locale.ROOT)+"://"+uri.getHost().toLowerCase(Locale.ROOT)+(port<0 || port==443&&uri.getScheme().equalsIgnoreCase("https") || port==80&&uri.getScheme().equalsIgnoreCase("http")?"":":"+port);
    }
}
