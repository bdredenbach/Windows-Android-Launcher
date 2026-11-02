package com.nthexperience.desktop;

public final class BrowserUrlsCheck {
    public static void main(String[] args){
        check(BrowserUrls.address("example.com/path?q=a","Bing").equals("https://example.com/path?q=a"));
        check(BrowserUrls.address("localhost:8080/one","Bing").equals("https://localhost:8080/one"));
        check(BrowserUrls.address("cats and dogs","Google").equals("https://www.google.com/search?q=cats+and+dogs"));
        check(BrowserUrls.address("café","DuckDuckGo").equals("https://duckduckgo.com/?q=caf%C3%A9"));
        check(BrowserUrls.address("https://bücher.example/","Bing").equals("https://xn--bcher-kva.example/"));
        check(BrowserUrls.origin("https://EXAMPLE.com:443/hello").equals("https://example.com"));
        check(BrowserUrls.origin("http://example.com:8080/a").equals("http://example.com:8080"));
        for(String value:new String[]{"javascript:alert(1)","file:///etc/passwd","content://somewhere","data:text/html,hi","intent://x/#Intent;end","https://user:pass@example.com/","https:///no-host"}){boolean rejected=false;try{BrowserUrls.address(value,"Bing");}catch(IllegalArgumentException expected){rejected=true;}check(rejected);check(!BrowserUrls.web(value));}
        check(!BrowserUrls.web("https://example.com/\nHeader: value"));
        System.out.println("PASS: browser address/search normalization, international host names, origin matching, unsafe schemes and credential URLs");
    }
    private static void check(boolean condition){if(!condition)throw new AssertionError("Browser URL policy");}
}
