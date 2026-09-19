package com.inteebuild.nf0rhfey8gjn;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import java.util.Collections;

public class RadioService extends Service {
    private static final String CHANNEL_ID = "inteebuild_radio";
    private static final int NOTIF_ID = 1;
    public static final String ACTION_PLAY = "com.inteebuild.nf0rhfey8gjn.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.inteebuild.nf0rhfey8gjn.ACTION_PAUSE";
    private static final String STREAM_URL = "https://sp2.servidorrprivado.com:1221/=1";
    private static final boolean AUTOPLAY = true;
    private static RadioService instance;
    private MediaPlayer mp;
    private MediaSession session;
    private String currentUrl = STREAM_URL;
    private boolean wantPlay = false;

    public static void play(Context ctx, String url) {
        Intent i = new Intent(ctx, RadioService.class);
        i.setAction(ACTION_PLAY);
        if (url != null && !url.isEmpty()) i.putExtra("url", url);
        try { if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(i); else ctx.startService(i); } catch (Exception ignored) {}
    }

    public static void pause(Context ctx) {
        if (instance != null) instance.doPause();
        else { Intent i = new Intent(ctx, RadioService.class); i.setAction(ACTION_PAUSE); try { ctx.startService(i); } catch (Exception ignored) {} }
    }

    public static boolean isPlaying() {
        try { return instance != null && instance.mp != null && instance.mp.isPlaying(); }
        catch (Exception e) { return false; }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Reproduccion en segundo plano", NotificationManager.IMPORTANCE_LOW);
            nm.createNotificationChannel(ch);
        }
        try {
            session = new MediaSession(this, "InteeBuildSession");
            session.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
            session.setCallback(new MediaSession.Callback() {
                @Override public void onPlay() { doPlay(currentUrl); }
                @Override public void onPause() { doPause(); }
            });
            session.setActive(true);
        } catch (Exception ignored) {}
        startForeground(NOTIF_ID, buildNotif(false));
        if (AUTOPLAY && STREAM_URL.length() > 0) doPlay(STREAM_URL);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        if (ACTION_PAUSE.equals(action)) { doPause(); }
        else {
            String u = intent != null ? intent.getStringExtra("url") : null;
            if (u == null || u.isEmpty()) u = currentUrl;
            if (ACTION_PLAY.equals(action) || AUTOPLAY) doPlay(u); else doPlay(u);
        }
        return START_STICKY;
    }

    private synchronized void doPlay(String url) {
        if (url == null || url.isEmpty()) url = STREAM_URL;
        if (url.isEmpty()) return;
        currentUrl = url;
        wantPlay = true;
        try {
            if (mp != null) { try { mp.reset(); } catch (Exception ignored) {} }
            else {
                mp = new MediaPlayer();
                mp.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
                if (Build.VERSION.SDK_INT >= 21) mp.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
                mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() { public void onPrepared(MediaPlayer p) { p.start(); updateState(true); } });
                mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() { public void onCompletion(MediaPlayer p) { if (wantPlay) { try { p.reset(); connectAndPrepare(currentUrl); } catch (Exception ignored) {} } } });
                mp.setOnErrorListener(new MediaPlayer.OnErrorListener() { public boolean onError(MediaPlayer p, int what, int extra) { if (wantPlay) { try { p.reset(); connectAndPrepare(currentUrl); } catch (Exception ignored) {} return true; } return false; } });
            }
            connectAndPrepare(url);
        } catch (Exception ignored) {}
    }

    private void connectAndPrepare(String url) throws Exception {
        mp.setDataSource(getApplicationContext(), Uri.parse(url), Collections.singletonMap("Icy-MetaData", "0"));
        mp.prepareAsync();
        updateState(false);
    }

    private synchronized void doPause() {
        wantPlay = false;
        try { if (mp != null && mp.isPlaying()) mp.pause(); } catch (Exception ignored) {}
        updateState(false);
    }

    private void updateState(boolean playing) {
        try {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.notify(NOTIF_ID, buildNotif(playing));
            if (session != null) {
                PlaybackState st = new PlaybackState.Builder().setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_PLAY_PAUSE).setState(playing ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED, 0, 1.0f).build();
                session.setPlaybackState(st);
            }
        } catch (Exception ignored) {}
    }

    private Notification buildNotif(boolean playing) {
        Intent launch = getPackageManager().getLaunchIntentForPackage(getPackageName());
        PendingIntent content = launch != null ? PendingIntent.getActivity(this, 0, launch, pendingFlags()) : null;
        Intent togel = new Intent(this, RadioService.class);
        togel.setAction(playing ? ACTION_PAUSE : ACTION_PLAY);
        PendingIntent act = PendingIntent.getService(this, 1, togel, pendingFlags());
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
        b.setContentTitle("Nf0rhfey8gjnerbgyuer")
                .setContentText(playing ? "Transmitiendo en directo" : "Toca play en la app")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .addAction(playing ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play, playing ? "Pausar" : "Play", act);
        if (content != null) b.setContentIntent(content);
        try { if (session != null && Build.VERSION.SDK_INT >= 21) b.setStyle(new Notification.MediaStyle().setMediaSession(session.getSessionToken()).setShowActionsInCompactView(0)); } catch (Exception ignored) {}
        try { return b.build(); } catch (Exception e) { return new Notification(); }
    }

    private int pendingFlags() {
        return Build.VERSION.SDK_INT >= 23 ? (PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE) : PendingIntent.FLAG_UPDATE_CURRENT;
    }

    @Override
    public void onDestroy() {
        wantPlay = false;
        try { if (session != null) { session.setActive(false); session.release(); } } catch (Exception ignored) {}
        try { if (mp != null) { mp.release(); mp = null; } } catch (Exception ignored) {}
        instance = null;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
