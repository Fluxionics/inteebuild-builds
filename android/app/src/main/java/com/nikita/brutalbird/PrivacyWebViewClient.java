package com.nikita.brutalbird;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

public class PrivacyWebViewClient extends WebViewClient {
    private static final String TAG = "PrivacyWebViewClient";
    private final Context context;
    private final Set<String> blockedDomains = new HashSet<>();
    private final boolean blockCookies;
    private final boolean blockGeolocation;
    private final boolean blockRedirects;
    private boolean privacyInjected = false;

    public PrivacyWebViewClient(Context context, boolean blockCookies, boolean blockGeolocation, boolean blockRedirects) {
        this.context = context;
        this.blockCookies = blockCookies;
        this.blockGeolocation = blockGeolocation;
        this.blockRedirects = blockRedirects;
        loadBlocklist();
    }

    private void loadBlocklist() {
        try {
            InputStream is = context.getAssets().open("privacy_rules.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();

            JSONObject json = new JSONObject(sb.toString());
            addDomainsFromJson(json, "ads");
            addDomainsFromJson(json, "tracking");
            addDomainsFromJson(json, "custom");
            Log.d(TAG, "Loaded " + blockedDomains.size() + " blocked domains");
        } catch (IOException | JSONException e) {
            Log.w(TAG, "Failed to load privacy blocklist", e);
        }
    }

    private void addDomainsFromJson(JSONObject json, String key) {
        try {
            if (json.has(key)) {
                JSONArray arr = json.getJSONArray(key);
                for (int i = 0; i < arr.length(); i++) {
                    blockedDomains.add(arr.getString(i).toLowerCase());
                }
            }
        } catch (JSONException ignored) {}
    }

    private boolean isBlocked(String url) {
        if (url == null) return false;
        try {
            Uri uri = Uri.parse(url);
            String host = uri.getHost();
            if (host == null) return false;
            host = host.toLowerCase();
            for (String blocked : blockedDomains) {
                if (host.equals(blocked) || host.endsWith("." + blocked)) {
                    return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    @Override
    public boolean shouldOverrideUrlLoading(@NonNull WebView view, @NonNull WebResourceRequest request) {
        String url = request.getUrl().toString();
        if (blockRedirects && (url.startsWith("intent://") || url.startsWith("market://") || url.startsWith("whatsapp://"))) {
            Log.d(TAG, "Blocking redirect: " + url);
            return true;
        }
        if (isBlocked(url)) {
            Log.d(TAG, "Blocking request to: " + url);
            return true;
        }
        return super.shouldOverrideUrlLoading(view, request);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        if (!privacyInjected) {
            injectPrivacyScript(view);
            privacyInjected = true;
        }
    }

    private void injectPrivacyScript(WebView view) {
        StringBuilder js = new StringBuilder();
        js.append("(function(){");
        js.append("try{");
        
        if (blockCookies) {
            js.append("document.cookie='';");
            js.append("Object.defineProperty(document,'cookie',{get:function(){return '';},set:function(){}});");
        }

        if (blockGeolocation) {
            js.append("if(navigator.geolocation){");
            js.append("navigator.geolocation.getCurrentPosition=function(){};");
            js.append("navigator.geolocation.watchPosition=function(){};");
            js.append("}");
        }

        js.append("if(navigator.mediaDevices){");
        js.append("navigator.mediaDevices.getUserMedia=function(){return Promise.reject(new DOMException('Blocked by privacy mode','NotAllowedError'));};");
        js.append("}");

        js.append("if(window.Notification){");
        js.append("window.Notification.requestPermission=function(){return Promise.resolve('denied');};");
        js.append("Object.defineProperty(window.Notification,'permission',{value:'denied',writable:false});");
        js.append("}");

        js.append("Object.defineProperty(navigator,'hardwareConcurrency',{value:4});");
        js.append("Object.defineProperty(navigator,'deviceMemory',{value:4});");
        js.append("if(window.screen){");
        js.append("Object.defineProperty(screen,'colorDepth',{value:24});");
        js.append("Object.defineProperty(screen,'pixelDepth',{value:24});");
        js.append("}");

        if (blockedDomains.size() > 0) {
            js.append("var blocked=" + new JSONArray(blockedDomains).toString() + ";");
            js.append("var originalFetch=window.fetch;");
            js.append("window.fetch=function(url,opts){");
            js.append("try{var u=new URL(url);if(blocked.some(b=>u.hostname===b||u.hostname.endsWith('.'+b))){return Promise.reject(new Error('Blocked'));}}catch(e){}");
            js.append("return originalFetch.apply(this,arguments);};");
            js.append("var originalXHR=XMLHttpRequest.prototype.open;");
            js.append("XMLHttpRequest.prototype.open=function(method,url){");
            js.append("try{var u=new URL(url,location.href);if(blocked.some(b=>u.hostname===b||u.hostname.endsWith('.'+b))){throw new Error('Blocked');}}catch(e){}");
            js.append("return originalXHR.apply(this,arguments);};");
        }

        js.append("}catch(e){}");
        js.append("})();");

        view.evaluateJavascript(js.toString(), null);
    }
}