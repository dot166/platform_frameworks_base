package com.android.internal.os;

import android.annotation.Nullable;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.GosPackageState;

// Extra args for:
// - children of main zygote{,64}, including AppZygotes, but excluding WebViewZygote
// - children of WebViewZygote
//
// AppZygote is treated differently from WebViewZygote because the former runs untrusted app code
// (see android.app.ZygotePreload).
public class ZygoteExtraArgs {
    public final long selinuxFlags;

    public static final String PREFIX = "--flat-extra-args=";

    public static final ZygoteExtraArgs DEFAULT = new ZygoteExtraArgs(0L);

    public ZygoteExtraArgs(long selinuxFlags) {
        this.selinuxFlags = selinuxFlags;
    }

    private static final int IDX_SELINUX_FLAGS = 0;
    private static final int ARR_LEN = 1;
    private static final String SEPARATOR = "\t";

    public static String createFlat(Context ctx, int userId, ApplicationInfo appInfo,
                                GosPackageState ps,
                                boolean isIsolatedProcess) {
        String[] arr = new String[ARR_LEN];
        arr[IDX_SELINUX_FLAGS] = Long.toHexString(
            SELinuxFlags.get(ctx, userId, appInfo, ps, isIsolatedProcess)
        );
        return PREFIX + String.join(SEPARATOR, arr);
    }

    public static String createFlatForWebviewProcess(Context ctx, int userId,
             ApplicationInfo callerAppInfo, GosPackageState callerPs) {
        String[] arr = new String[ARR_LEN];
        arr[IDX_SELINUX_FLAGS] = Long.toHexString(
            SELinuxFlags.getForWebViewProcess(ctx, userId, callerAppInfo, callerPs)
        );
        return PREFIX + String.join(SEPARATOR, arr);
    }

    static ZygoteExtraArgs parse(String flat) {
        String[] arr = flat.split(SEPARATOR);
        long selinuxFlags = Long.parseLong(arr[IDX_SELINUX_FLAGS], 16);
        return new ZygoteExtraArgs(selinuxFlags);
    }

    // keep in sync with ExtraArgs struct in core/jni/com_android_internal_os_Zygote.cpp
    public long[] makeJniLongArray() {
        long[] res = new long[ARR_LEN];
        res[IDX_SELINUX_FLAGS] = selinuxFlags;
        return res;
    }
}
