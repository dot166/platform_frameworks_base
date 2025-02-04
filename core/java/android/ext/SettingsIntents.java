package android.ext;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/** @hide */
public class SettingsIntents {

    public static final String APP_NATIVE_DEBUGGING = "android.settings.OPEN_APP_NATIVE_DEBUGGING_SETTINGS";
    public static final String APP_MEMTAG = "android.settings.OPEN_APP_MEMTAG_SETTINGS";
    public static final String APP_HARDENED_MALLOC = "android.settings.OPEN_APP_HARDENED_MALLOC_SETTINGS";
    public static final String APP_MEMORY_DYN_CODE_LOADING = "android.settings.OPEN_APP_MEMORY_DYN_CODE_LOADING_SETTINGS";
    public static final String APP_STORAGE_DYN_CODE_LOADING = "android.settings.OPEN_APP_STORAGE_DYN_CODE_LOADING_SETTINGS";

    public static Intent getAppIntent(Context ctx, String action, String pkgName) {
        var i = new Intent(action);
        i.setData(Uri.fromParts("package", pkgName, null));
        i.setPackage(KnownSystemPackages.get(ctx).settings);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return i;
    }
}
