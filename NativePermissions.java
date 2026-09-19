package com.inteebuild.nf0rhfey8gjn;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;

public class NativePermissions {
    public static String[] getRequiredPermissions(android.content.Context ctx) {
        List<String> perms = new ArrayList<>();
        try {
            String[] declared = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), android.content.pm.PackageManager.GET_PERMISSIONS).requestedPermissions;
            if (declared == null) return new String[0];
            for (String p : declared) {
                if (p.equals(Manifest.permission.INTERNET) || p.equals(Manifest.permission.ACCESS_NETWORK_STATE) || p.equals(Manifest.permission.ACCESS_WIFI_STATE) || p.equals(Manifest.permission.VIBRATE) || p.equals(Manifest.permission.WAKE_LOCK) || p.equals(Manifest.permission.FOREGROUND_SERVICE) || p.equals(Manifest.permission.FOREGROUND_SERVICE_DATA_SYNC) || p.equals(Manifest.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK) || p.equals(Manifest.permission.USE_BIOMETRIC) || p.equals(Manifest.permission.NFC) || p.equals(Manifest.permission.REQUEST_INSTALL_PACKAGES) || p.equals(Manifest.permission.SYSTEM_ALERT_WINDOW)) {
                    // not runtime or handled separately, but still need check for some
                    if (p.equals(Manifest.permission.NFC) || p.equals(Manifest.permission.USE_BIOMETRIC)) perms.add(p);
                    continue;
                }
                if (p.startsWith("android.permission.")) {
                    if (ContextCompat.checkSelfPermission(ctx, p) != PackageManager.PERMISSION_GRANTED) perms.add(p);
                    else if (p.equals(Manifest.permission.POST_NOTIFICATIONS) || p.equals(Manifest.permission.ACCESS_FINE_LOCATION) || p.equals(Manifest.permission.CAMERA) || p.equals(Manifest.permission.RECORD_AUDIO)) perms.add(p);
                }
            }
        } catch (Exception ignored) {}
        // dedup
        java.util.LinkedHashSet<String> set=new java.util.LinkedHashSet<>(perms);
        return set.toArray(new String[0]);
    }
    public static void requestAll(android.app.Activity act, int code) {
        String[] req = getRequiredPermissions(act);
        List<String> need = new ArrayList<>();
        for (String p : req) if (ContextCompat.checkSelfPermission(act, p) != PackageManager.PERMISSION_GRANTED) need.add(p);
        if (!need.isEmpty()) ActivityCompat.requestPermissions(act, need.toArray(new String[0]), code);
    }
}
