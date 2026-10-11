package ember.ai.muragok.teams;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NativePermissions {
    private static final String TAG = "NativePermissions";
    public static final int REQ_BATCH = 9001;
    public static final int REQ_BACKGROUND = 9002;
    private static final String BG = "android.permission.ACCESS_BACKGROUND_LOCATION";

    // Generado desde la config: 1:1 con lo seleccionado en el Studio.
    private static final String[] BATCH = {
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.NEARBY_WIFI_DEVICES
    };
    private static final boolean WANTS_BG = false;

    public static boolean hasPermission(Context context, String permission) {
        if (context == null || permission == null) return false;
        try {
            return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
        } catch (Exception e) {
            Log.e(TAG, "Error checking permission: " + permission, e);
            return false;
        }
    }

    private static boolean declared(Context ctx, String perm) {
        try {
            String[] req = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), PackageManager.GET_PERMISSIONS).requestedPermissions;
            return req != null && Arrays.asList(req).contains(perm);
        } catch (Exception e) { return false; }
    }

    // Lote principal: todo lo runtime EXCEPTO background (Android 11+ lo exige separado).
    public static void requestAll(Activity activity) {
        requestAll(activity, REQ_BATCH);
    }

    public static void requestAll(Activity activity, int code) {
        if (activity == null || Build.VERSION.SDK_INT < 23) return;
        List<String> missing = new ArrayList<>();
        for (String p : BATCH) {
            if (declared(activity, p) && !hasPermission(activity, p)) missing.add(p);
        }
        if (missing.isEmpty()) { Log.d(TAG, "Batch already granted"); return; }
        try { activity.requestPermissions(missing.toArray(new String[0]), code); }
        catch (Exception e) { Log.e(TAG, "Error requesting batch", e); }
    }

    // Two-step: background SOLO después de conceder foreground (Android 11+ lo ignora en lote).
    public static void requestBackground(Activity activity) {
        requestBackground(activity, REQ_BACKGROUND);
    }

    public static void requestBackground(Activity activity, int code) {
        if (activity == null || !WANTS_BG || Build.VERSION.SDK_INT < 29) return;
        if (!declared(activity, BG) || hasPermission(activity, BG)) return;
        boolean fg = hasPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                || hasPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION);
        if (!fg) { Log.d(TAG, "Background deferred until foreground granted"); return; }
        try { activity.requestPermissions(new String[]{BG}, code); }
        catch (Exception e) { Log.e(TAG, "Error requesting background", e); }
    }

    public static boolean wantsBackground(Context ctx) {
        return WANTS_BG && declared(ctx, BG) && !hasPermission(ctx, BG);
    }

    public static String[] getManifestPermissions(Context context) {
        try {
            android.content.pm.PackageInfo packageInfo = context.getPackageManager()
                .getPackageInfo(context.getPackageName(), android.content.pm.PackageManager.GET_PERMISSIONS);
            return packageInfo.requestedPermissions;
        } catch (Exception e) {
            Log.e(TAG, "Error getting manifest permissions", e);
            return new String[0];
        }
    }

    public static Set<String> getRuntimePermissions(Context context) {
        Set<String> runtimePermissions = new HashSet<>();
        String[] manifestPermissions = getManifestPermissions(context);
        if (manifestPermissions == null) return runtimePermissions;
        for (String permission : manifestPermissions) {
            int r = context.checkPermission(permission, android.os.Process.myPid(), android.os.Process.myUid());
            if (r == PackageManager.PERMISSION_GRANTED) runtimePermissions.add(permission);
        }
        return runtimePermissions;
    }

    public static boolean isPermissionInManifest(Context context, String permission) {
        String[] manifestPermissions = getManifestPermissions(context);
        return manifestPermissions != null && Arrays.asList(manifestPermissions).contains(permission);
    }
}
