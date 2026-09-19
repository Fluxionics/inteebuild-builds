package com.inteebuild.nf0rhfey8gjn;

import android.content.Context;
import android.webkit.JavascriptInterface;

public class AudioBridge {
    private final Context ctx;
    public AudioBridge(Context ctx) { this.ctx = ctx.getApplicationContext(); }
    @JavascriptInterface public void play(String url) { RadioService.play(ctx, url); }
    @JavascriptInterface public void pause() { RadioService.pause(ctx); }
    @JavascriptInterface public boolean isPlaying() { return RadioService.isPlaying(); }
}
