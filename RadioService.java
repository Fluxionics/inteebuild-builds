package ember.ai.muragok.teams;

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
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import java.util.Collections;

public class RadioService extends Service {
    private static final String TAG = "InteeRadio";
    private static final String CHANNEL_ID = "inteebuild_radio";
    private static final int NOTIF_ID = 1;
    public static final String ACTION_PLAY = "ember.ai.muragok.teams.ACTION_PLAY";
    public static final String ACTION_PAUSE = "ember.ai.muragok.teams.ACTION_PAUSE";
    public static final String ACTION_KEEP = "ember.ai.muragok.teams.ACTION_KEEP";
    private static final String STREAM_URL = "";
    private static final boolean AUTOPLAY = false;
    private static volatile RadioService instance;
    private MediaPlayer mp;
    private MediaSession session;
    private PowerManager.WakeLock wakeLock;
    private WifiManager.WifiLock wifiLock;
    private String currentUrl = STREAM_URL;
    private volatile boolean wantPlay = false;
    private volatile boolean webAwake = false;
    private long pendingSeek = -1;

    public static void play(Context ctx, String url) {
        play(ctx, url, -1);
    }

    public static void play(Context ctx, String url, long startMs) {
        Intent i = new Intent(ctx, RadioService.class);
        i.setAction(ACTION_PLAY);
        if (url != null && !url.isEmpty()) i.putExtra("url", url);
        if (startMs > 0) i.putExtra("seek", startMs);
        try { if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(i); else ctx.startService(i); } catch (Exception ignored) { Log.e(TAG, "Error starting service", ignored); }
    }

    public static void pause(Context ctx) {
        if (instance != null) instance.doPause();
        else { Intent i = new Intent(ctx, RadioService.class); i.setAction(ACTION_PAUSE); try { ctx.startService(i); } catch (Exception ignored) { Log.e(TAG, "Error pausing service", ignored); } }
    }

    public static boolean isPlaying() {
        try { return instance != null && instance.mp != null && instance.mp.isPlaying(); }
        catch (Exception e) { return false; }
    }

    public static boolean isActive() {
        try { RadioService s = instance; return s != null && (s.wantPlay || s.webAwake); }
        catch (Exception e) { return false; }
    }

    public static void keepAwake(Context ctx, boolean on) {
        RadioService s = instance;
        if (s == null) {
            if (!on) return;
            Intent i = new Intent(ctx, RadioService.class);
            i.setAction(ACTION_KEEP);
            try { if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(i); else ctx.startService(i); } catch (Exception ignored) { Log.e(TAG, "Error starting service", ignored); }
            return;
        }
        s.webAwake = on;
        s.refreshWakeLock();
        s.updateState(s.mpPlaying() || s.webAwake);
    }

    private static boolean looksLive(String url) {
        if (url == null || url.isEmpty()) return true;
        String s = url.toLowerCase();
        String[] hints = { "/stream", "/live", "icecast", "shoutcast", ".m3u8", ".aac", ".pls", "radio", "playlist" };
        for (String h : hints) if (s.contains(h)) return true;
        int q = s.indexOf('?');
        String path = q >= 0 ? s.substring(0, q) : s;
        int dot = path.lastIndexOf('.');
        if (dot < 0) return true;
        String ext = path.substring(dot);
        return !(ext.equals(".mp3") || ext.equals(".ogg") || ext.equals(".m4a") || ext.equals(".wav") || ext.equals(".opus") || ext.equals(".flac") || ext.equals(".webm") || ext.equals(".mp4"));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            try {
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG + ":Audio");
                wakeLock.setReferenceCounted(false);
            } catch (Exception e) { wakeLock = null; Log.e(TAG, "WakeLock unavailable", e); }
        }
        try {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm != null) {
                wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, TAG + ":Wifi");
                wifiLock.setReferenceCounted(false);
            }
        } catch (Exception e) { wifiLock = null; Log.e(TAG, "WifiLock unavailable", e); }
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Reproduccion en segundo plano", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Control de reproduccion de audio");
            nm.createNotificationChannel(ch);
        }
        try {
            session = new MediaSession(this, "InteeBuildSession");
            session.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
            session.setCallback(new MediaSession.Callback() {
                @Override public void onPlay() { doPlay(currentUrl); }
                @Override public void onPause() { doPause(); }
                @Override public void onStop() { doPause(); }
            });
            session.setActive(true);
        } catch (Exception e) { Log.e(TAG, "MediaSession error", e); }
        startForeground(NOTIF_ID, buildNotif(false));
        if (AUTOPLAY && STREAM_URL.length() > 0) doPlay(STREAM_URL);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        if (ACTION_PAUSE.equals(action)) { doPause(); }
        else if (ACTION_KEEP.equals(action)) { webAwake = true; refreshWakeLock(); updateState(mpPlaying() || webAwake); }
        else {
            String u = intent != null ? intent.getStringExtra("url") : null;
            pendingSeek = intent != null ? intent.getLongExtra("seek", -1) : -1;
            if (u == null || u.isEmpty()) u = currentUrl;
            doPlay(u);
        }
        return START_STICKY;
    }

    private synchronized void doPlay(String url) {
        if (url == null || url.isEmpty()) url = STREAM_URL;
        if (url.isEmpty()) return;
        currentUrl = url;
        wantPlay = true;
        refreshWakeLock();
        try {
            if (mp != null) { try { mp.reset(); } catch (Exception ignored) {} }
            else {
                mp = new MediaPlayer();
                try { mp.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK); } catch (Exception e) { Log.e(TAG, "setWakeMode error", e); }
                if (Build.VERSION.SDK_INT >= 21) mp.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
                mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() { public void onPrepared(MediaPlayer p) { p.start(); long s = pendingSeek; pendingSeek = -1; if (s > 0) { try { p.seekTo((int) Math.min(s, Integer.MAX_VALUE)); } catch (Exception ignored) {} } updateState(true); } });
                mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() { public void onCompletion(MediaPlayer p) { if (wantPlay && looksLive(currentUrl)) { try { p.reset(); connectAndPrepare(currentUrl); } catch (Exception ignored) {} } else { wantPlay = false; updateState(false); refreshWakeLock(); } } });
                mp.setOnErrorListener(new MediaPlayer.OnErrorListener() { public boolean onError(MediaPlayer p, int what, int extra) { if (wantPlay && looksLive(currentUrl)) { try { p.reset(); connectAndPrepare(currentUrl); } catch (Exception ignored) {} return true; } wantPlay = false; updateState(false); refreshWakeLock(); return false; } });
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
        refreshWakeLock();
    }

    private boolean mpPlaying() {
        try { return mp != null && mp.isPlaying(); } catch (Exception e) { return false; }
    }

    private void refreshWakeLock() {
        boolean need = webAwake || wantPlay;
        try {
            if (wakeLock != null) {
                if (need && !wakeLock.isHeld()) wakeLock.acquire();
                else if (!need && wakeLock.isHeld()) wakeLock.release();
            }
        } catch (Exception e) { Log.e(TAG, "WakeLock error", e); }
        try {
            if (wifiLock != null) {
                if (need && !wifiLock.isHeld()) wifiLock.acquire();
                else if (!need && wifiLock.isHeld()) wifiLock.release();
            }
        } catch (Exception e) { Log.e(TAG, "WifiLock error", e); }
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
        b.setContentTitle("Ember AI")
                .setContentText(playing ? "Reproduciendo en segundo plano" : "Toca play en la app")
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
        webAwake = false;
        try { if (wakeLock != null && wakeLock.isHeld()) wakeLock.release(); } catch (Exception ignored) {}
        try { if (wifiLock != null && wifiLock.isHeld()) wifiLock.release(); } catch (Exception ignored) {}
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
