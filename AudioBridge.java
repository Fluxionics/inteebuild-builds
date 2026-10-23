package com.inteebuild.cableplay;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

public class AudioBridge {
    private final Context ctx;
    private final Handler ui = new Handler(Looper.getMainLooper());
    public AudioBridge(Context ctx) { this.ctx = ctx.getApplicationContext(); }
    private void toast(final String msg) {
        try { ui.post(new Runnable() { public void run() { try { Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show(); } catch (Exception ignored) {} } }); } catch (Exception ignored) {}
    }
    @JavascriptInterface public void play(String url) { toast("Audio nativo: reproduciendo"); RadioService.play(ctx, url); }
    @JavascriptInterface public void playAt(String url, long startMs) { toast("Audio nativo: reproduciendo"); RadioService.play(ctx, url, startMs); }
    @JavascriptInterface public void pause() { toast("Audio nativo: en pausa"); RadioService.pause(ctx); }
    @JavascriptInterface public boolean isPlaying() { return RadioService.isPlaying(); }
    @JavascriptInterface public boolean isActive() { return RadioService.isActive(); }
    @JavascriptInterface public void keepAwake(boolean on) { RadioService.keepAwake(ctx, on); }
}
