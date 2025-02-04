package com.android.server.logcat;

import android.annotation.Nullable;
import android.app.ActivityManagerInternal;
import android.app.ActivityManagerInternal.ProcessRecordSnapshot;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.GosPackageStateFlag;
import android.ext.SettingsIntents;
import android.util.Slog;

import com.android.internal.R;
import com.android.internal.os.SELinuxFlags;
import com.android.server.LocalServices;
import com.android.server.ext.AppSwitchNotification;
import com.android.server.ext.DynCodeLoadingUtils;
import com.android.server.os.nano.AppCompatProtos;

import java.lang.reflect.Field;

import libcore.util.HexEncoding;

import static java.nio.charset.StandardCharsets.UTF_8;

public class LogdNotableMessage {
    static final String TAG = LogdNotableMessage.class.getSimpleName();

    private static final int TYPE_SELINUX_TSEC_FLAG_DENIAL = 0;

    static void onNotableMessage(Context ctx, int type, int uid, int pid, byte[] msgBytes) {
        Slog.d(TAG, "uid " + uid + ", pid " + pid + ", msg " + new String(msgBytes, UTF_8));

        switch (type) {
            case TYPE_SELINUX_TSEC_FLAG_DENIAL -> handleSELinuxTsecFlagDenial(ctx, msgBytes);
        }
    }

    static void handleSELinuxTsecFlagDenial(Context ctx, byte[] msgBytes) {
        String TAG = "SELinuxTsecFlagDenial";

        String msg = new String(msgBytes, UTF_8);

        String[] msgParts = msg.split(",");

        int uid = -1;
        int pid = -1;
        int topPidWithSameUid = -1;

        for (String part : msgParts) {
            String uidPrefix = " uid ";
            String pidPrefix = " pid ";
            String topPidPrefix = " top_pid_with_same_uid ";
            if (part.startsWith(uidPrefix)) {
                if (uid != -1) {
                    Slog.w(TAG, "duplicate uid prefix; " + msg);
                    return;
                }
                uid = Integer.parseInt(part.substring(uidPrefix.length()));
                continue;
            }
            if (part.startsWith(pidPrefix)) {
                if (pid != -1) {
                    Slog.w(TAG, "duplicate pid prefix; " + msg);
                    return;
                }
                pid = Integer.parseInt(part.substring(pidPrefix.length()));
                continue;
            }
            if (part.startsWith(topPidPrefix)) {
                if (topPidWithSameUid != -1) {
                    Slog.w(TAG, "duplicate topPid prefix; " + msg);
                    return;
                }
                topPidWithSameUid = Integer.parseInt(part.substring(topPidPrefix.length()));
                continue;
            }
        }

        if (uid == -1 || pid == -1 || topPidWithSameUid == -1)  {
            Slog.w(TAG, "missing message part(s); " + msg);
            return;
        }

        var ami = LocalServices.getService(ActivityManagerInternal.class);

        ProcessRecordSnapshot prs = ami.getProcessRecordByPid(pid);

        if (prs == null) {
            Slog.w(TAG,"missing ProcessRecordSnapshot for pid; " + msg);

            prs = ami.getProcessRecordByPid(topPidWithSameUid);
            if (prs == null) {
                Slog.w(TAG,"missing ProcessRecordSnapshot for topPidWithSameUid; " + msg);
                return;
            }
        }

        String flagStart = " TSEC_FLAG_";
        int flagIdx = msg.indexOf(flagStart);
        if (flagIdx <= 1) {
            Slog.w(TAG, "missing flag; " + msg);
            return;
        }

        int endIdx = msg.indexOf(':', flagIdx);
        if (endIdx < 0) {
            Slog.w(TAG, "missing flag end; " + msg);
            return;
        }

        String flagName = msg.substring(flagIdx + flagStart.length(), endIdx);
        long flagValue;

        try {
            Field flagField = SELinuxFlags.class.getField(flagName);
            flagValue = (long) flagField.get(null);
        } catch (ReflectiveOperationException e) {
            Slog.w(TAG, msg, e);
            return;
        }

        ApplicationInfo appInfo = prs.appInfo;

        if (flagValue == SELinuxFlags.DENY_PROCESS_PTRACE) {
            if (appInfo.ext().hasCompatChange(AppCompatProtos.SUPPRESS_NATIVE_DEBUGGING_NOTIFICATION)) {
                Slog.d(TAG, "ptrace notification is disabled by compat change for " + appInfo.packageName);
                return;
            }

            var n = AppSwitchNotification.create(ctx, appInfo, SettingsIntents.APP_NATIVE_DEBUGGING);
            n.titleRes = R.string.notif_native_debug_title;
            n.gosPsFlagSuppressNotif = GosPackageStateFlag.BLOCK_NATIVE_DEBUGGING_SUPPRESS_NOTIF;
            n.maybeShow();
        }
        else if ((SELinuxFlags.RESTRICT_MEMORY_DYN_CODE_EXEC_FLAGS & flagValue) != 0) {
            AppSwitchNotification n = DynCodeLoadingUtils.createMemoryDclNotif(ctx, appInfo);
            var report = DynCodeLoadingUtils.DclReport.createForMemoryDcl(flagName);
            n.moreInfoIntent = DynCodeLoadingUtils.getMoreInfoIntent(n, report);
            n.maybeShow();
        }
        else if ((SELinuxFlags.RESTRICT_STORAGE_DYN_CODE_EXEC_FLAGS & flagValue) != 0) {
            var n = DynCodeLoadingUtils.createStorageDclNotif(ctx, appInfo);
            var report = DynCodeLoadingUtils.DclReport.createForStorageDcl(flagName);
            DynCodeLoadingUtils.handleStorageDclAuditMessage(msg, n, report);
            n.moreInfoIntent = DynCodeLoadingUtils.getMoreInfoIntent(n, report);
            n.maybeShow();
        }
        else {
            Slog.w(TAG, "unknown flag " + msg);
        }
    }

    // see common/security/lsm_audit.c in Linux kernel, function dump_common_audit_data()
    @Nullable
    public static String extractAuditUntrustedString(String msg, String dataPrefix) {
        // leading space is important, it's never included in data itself
        String dataStart = ' ' + dataPrefix + '=';

        int idx = msg.indexOf(dataStart);
        String res = null;
        if (idx > 0) {
            res = extractAuditUntrustedStringInner(msg, idx + dataStart.length());
        }
        return res;
    }

    // see common/kernel/audit.c in Linux kernel, function audit_log_n_untrustedstring()
    @Nullable
    public static String extractAuditUntrustedStringInner(String s, int start) {
        if (s.charAt(start) == '"') {
            int end = s.indexOf('"', start + 1);
            if (end > start) {
                return s.substring(start + 1, end);
            }
        } else {
            int end = s.indexOf(' ', start);
            if (end == -1) {
                end = s.length();
            }
            try {
                byte[] b = HexEncoding.decode(s.substring(start, end));
                return new String(b, UTF_8);
            } catch (Exception e) {
                Slog.d(TAG, "", e);
            }
        }
        Slog.d(TAG, "extractUntrustedAuditString: malformed string: start: " + start + ", str: " + s);
        return null;
    }
}
