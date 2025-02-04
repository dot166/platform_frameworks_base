package com.android.server.ext;

import android.annotation.CurrentTimeMillisLong;
import android.annotation.Nullable;
import android.app.ActivityManagerInternal;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.GosPackageState;
import android.content.pm.PackageManagerInternal;
import android.ext.LogViewerApp;
import android.ext.SettingsIntents;
import android.ext.settings.ExtSettings;
import android.os.DropBoxManager;
import android.os.Process;
import android.os.TombstoneWithHeadersProto;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Pair;
import android.util.Slog;
import android.util.proto.ProtoInputStream;

import com.android.internal.R;
import com.android.internal.os.Zygote;
import com.android.server.LocalServices;
import com.android.server.os.nano.TombstoneProtos;
import com.android.server.pm.pkg.AndroidPackage;
import com.android.server.pm.pkg.PackageState;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;

public class TombstoneHandler {
    private static final String TAG = TombstoneHandler.class.getSimpleName();

    private static final int SIGSEGV = 11;
    private static final int SEGV_MTEAERR = 8;
    private static final int SEGV_MTESERR = 9;

    public static final boolean isMemoryTaggingSupported = Zygote.nativeSupportsMemoryTagging();

    public static void handleNewFile(Context ctx, File protoFile) {
        long ts = System.currentTimeMillis();

        try {
            byte[] protoBytes = Files.readAllBytes(protoFile.toPath());

            handleTombstoneBytes(ctx, protoBytes, protoFile, ts, false);
        } catch (Throwable e) {
            // catch everything to reduce the chance of getting into a crash loop
            Slog.e(TAG, "", e);
        }
    }

    static void handleDropBoxEntry(DropBoxMonitor dm, DropBoxManager.Entry entry) {
        try (InputStream eis = entry.getInputStream()) {
            if (eis == null) {
                Slog.d(TAG, "tombstone entry getInputStream() is null");
                return;
            }

            var pis = new ProtoInputStream(eis);

            byte[] tombstoneBytes = null;
            while (pis.nextField() != ProtoInputStream.NO_MORE_FIELDS) {
                if (pis.getFieldNumber() == (int) TombstoneWithHeadersProto.TOMBSTONE) {
                    tombstoneBytes = pis.readBytes(TombstoneWithHeadersProto.TOMBSTONE);
                    break;
                }
            }

            if (tombstoneBytes == null) {
                Slog.d(TAG, "tombstoneBytes is null");
                return;
            }

            handleTombstoneBytes(dm.context, tombstoneBytes, null, entry.getTimeMillis(), true);
        } catch (Throwable e) {
            // catch everything to reduce the chance of getting into a crash loop
            Slog.e(TAG, "", e);
        }
    }

    private static void handleTombstoneBytes(Context ctx, byte[] tombstoneBytes,
            @Nullable File protoFile,
            @CurrentTimeMillisLong long timestamp, boolean isHistorical)
            throws IOException
    {
        var tombstone = TombstoneProtos.Tombstone.parseFrom(tombstoneBytes);

        var sb = new StringBuilder();
        sb.append("osVersion: ");
        sb.append(tombstone.buildFingerprint);
        sb.append("\nuid: ");
        sb.append(tombstone.uid);
        {
            String sl = tombstone.selinuxLabel;
            if (!TextUtils.isEmpty(sl)) {
                int len = sl.length();
                // selinuxLabel usually includes terminating NUL byte. Amending protobuf generation
                // could break its other users, add a workaround here instead
                if (sl.charAt(len - 1) == '\u0000') {
                    sl = sl.substring(0, len - 1);
                }
            }
            sb.append(" (");
            sb.append(sl);
        }
        sb.append(")\ncmdline:");
        for (String s : tombstone.commandLine) {
            sb.append(' ');
            sb.append(s);
        }
        sb.append("\nprocessUptime: ");
        sb.append(tombstone.processUptime);
        sb.append('s');

        if (!tombstone.abortMessage.isEmpty()) {
            sb.append("\n\nabortMessage: ");
            sb.append(tombstone.abortMessage);
        }

        TombstoneProtos.Signal signal = tombstone.signalInfo;
        if (signal != null) {
            sb.append("\n\nsignal: ");
            sb.append(signal.number);
            sb.append(" (");
            sb.append(signal.name);
            sb.append("), code ");
            sb.append(signal.code);
            sb.append(" (");
            sb.append(signal.codeName);
            sb.append(")");
            if (signal.hasSender) {
                sb.append(", senderUid ");
                sb.append(signal.senderUid);
            }
            if (signal.hasFaultAddress) {
                sb.append(", faultAddr ");
                sb.append(Long.toHexString(signal.faultAddress));
            }
        }

        for (TombstoneProtos.Cause cause : tombstone.causes) {
            sb.append("\ncause: ");
            sb.append(cause.humanReadable);
        }

        var threads = tombstone.threads;
        TombstoneProtos.Thread thread = null;
        if (threads != null) {
            thread = threads.get(tombstone.tid);
        }

        TombstoneProtos.BacktraceFrame[] backtrace = null;
        if (thread == null) {
            sb.append("\n\nno thread info");
        } else {
            sb.append("\nthreadName: ");
            sb.append(thread.name);

            if (isMemoryTaggingSupported) {
                sb.append("\nMTE: ");

                final long PR_MTE_TCF_SYNC = 1 << 1;
                final long PR_MTE_TCF_ASYNC = 1 << 2;

                long tac = thread.taggedAddrCtrl;
                if ((tac & (PR_MTE_TCF_SYNC | PR_MTE_TCF_ASYNC)) != 0) {
                    if ((tac & PR_MTE_TCF_ASYNC) != 0) {
                        sb.append("enabled");
                    } else {
                        sb.append("enabled; sync");
                    }
                } else {
                    sb.append("not enabled");
                }
            }

            sb.append("\n\nbacktrace:");
            backtrace = thread.currentBacktrace;
            for (TombstoneProtos.BacktraceFrame frame : backtrace) {
                sb.append("\n    ");
                sb.append(frame.fileName);
                sb.append(" (");
                if (!frame.functionName.isEmpty()) {
                    sb.append(frame.functionName);
                    sb.append('+');
                    sb.append(frame.functionOffset);
                    sb.append(", ");
                }
                sb.append("pc ");
                sb.append(Long.toHexString(frame.relPc));
                sb.append(')');
            }
        }

        String msg = sb.toString();

        String progName = "//no progName//";
        if (tombstone.commandLine.length > 0) {
            String path = tombstone.commandLine[0];
            progName = path.substring(path.lastIndexOf('/') + 1);
        }

        var pm = LocalServices.getService(PackageManagerInternal.class);

        int uid = tombstone.uid;

        boolean isAppUid = Process.isApplicationUid(uid);

        boolean shouldSkip = false;

        if (isAppUid || Process.isIsolatedUid(uid)) {
            int pid = tombstone.pid;

            String firstPackageName = null;
            int packageUid = 0;
            boolean isSystem = false;

            var ami = LocalServices.getService(ActivityManagerInternal.class);
            ActivityManagerInternal.ProcessRecordSnapshot prs = ami.getProcessRecordByPid(pid);
            if (prs != null) {
                ApplicationInfo appInfo = prs.appInfo;
                packageUid = appInfo.uid;
                firstPackageName = appInfo.packageName;
                isSystem = appInfo.isSystemApp();
            } else if (isAppUid) {
                int appId = UserHandle.getAppId(uid);
                List<AndroidPackage> appIdPkgs = pm.getPackagesForAppId(appId);
                if (appIdPkgs.size() == 1) {
                    var pkg = appIdPkgs.get(0);
                    String pkgName = pkg.getPackageName();
                    firstPackageName = pkgName;
                    packageUid = uid;

                    PackageState pkgState = pm.getPackageStateInternal(pkg.getPackageName());
                    if (pkgState != null) {
                        isSystem = pkgState.isSystem() || pkgState.isUpdatedSystemApp();
                    }
                }
            }

            if (firstPackageName == null) {
                Slog.d(TAG, "firstPackageName is null for uid " + packageUid);
                return;
            }

            if (!isSystem) {
                if (!isHistorical) {
                    int aswNotifType = -1;
                    if (isMemtagError(tombstone)) {
                        aswNotifType = ASW_NOTIF_TYPE_MEMTAG;
                    } else if (isHardenedMallocFatalError(tombstone)) {
                        aswNotifType = ASW_NOTIF_TYPE_HARDENED_MALLOC;
                    }
                    if (aswNotifType != -1) {
                        maybeShowAswNotification(aswNotifType, ctx, tombstone, msg,
                                getTextTombstoneFileSpec(protoFile),
                                packageUid, firstPackageName);
                    }
                }
                // rely on the standard crash dialog for other crashes
                return;
            }

            progName = firstPackageName;
        } else {
            switch (progName) {
                // bootanimation intentionally crashes in some cases
                case "bootanimation" -> {
                    shouldSkip = true;
                }
            }
        }

        if (shouldIgnore(tombstone, backtrace)) {
            Slog.d(TAG, "ignored tombstone for " + progName);
            return;
        }

        final boolean showReportButton;

        if ("system_server".equals(progName)) {
            showReportButton = true;
        } else {
            boolean ignoreSetting = !isHistorical && isMemtagError(tombstone);
            showReportButton = ignoreSetting && !shouldSkip;

            if (shouldSkip) {
                Slog.d(TAG, "skipped crash notification for " + progName + "; msg: " + msg);
                return;
            }
        }

        SystemJournalNotif.showCrash(ctx, progName, msg, getTextTombstoneFileSpec(protoFile),
                timestamp, showReportButton);
    }

    private static boolean isMemtagError(TombstoneProtos.Tombstone t) {
        TombstoneProtos.Signal s = t.signalInfo;

        return isMemoryTaggingSupported && s != null && s.number == SIGSEGV
                && (s.code == SEGV_MTEAERR || s.code == SEGV_MTESERR);
    }

    private static boolean isHardenedMallocFatalError(TombstoneProtos.Tombstone t) {
        String abortMsg = t.abortMessage;
        return abortMsg != null && abortMsg.startsWith("hardened_malloc: fatal allocator error: ");
    }

    private static final int ASW_NOTIF_TYPE_MEMTAG = 0;
    private static final int ASW_NOTIF_TYPE_HARDENED_MALLOC = 1;

    private static void maybeShowAswNotification(int type, Context ctx, TombstoneProtos.Tombstone tombstone,
                                                 String errorReport,
                                                 @Nullable Pair<String, Long> textTombstoneFileSpec,
                                                 int packageUid, String firstPackageName) {
        AppSwitchNotification n;
        switch (type) {
            case ASW_NOTIF_TYPE_MEMTAG -> {
                n = AppSwitchNotification.maybeCreate(ctx, firstPackageName, packageUid,
                        SettingsIntents.APP_MEMTAG);
                if (n == null) {
                    return;
                }
                n.titleRes = R.string.notif_memtag_crash_title;
                n.gosPsFlagSuppressNotif = GosPackageState.FLAG_FORCE_MEMTAG_SUPPRESS_NOTIF;
            }
            case ASW_NOTIF_TYPE_HARDENED_MALLOC -> {
                n = AppSwitchNotification.maybeCreate(ctx, firstPackageName, packageUid,
                        SettingsIntents.APP_HARDENED_MALLOC);
                if (n == null) {
                    return;
                }
                n.titleRes = R.string.notif_hmalloc_crash_title;
            }
            default -> throw new IllegalArgumentException(Integer.toString(type));
        }
        Intent i = LogViewerApp.createBaseErrorReportIntent(errorReport);
        i.putExtra(LogViewerApp.EXTRA_SOURCE_APP_INFO, n.appInfo);
        if (textTombstoneFileSpec != null) {
            i.putExtra(LogViewerApp.EXTRA_TEXT_TOMBSTONE_FILE_PATH, textTombstoneFileSpec.first);
            i.putExtra(LogViewerApp.EXTRA_TEXT_TOMBSTONE_LAST_MODIFIED_TIME, textTombstoneFileSpec.second.longValue());
        }
        n.moreInfoIntent = i;

        n.maybeShow();
    }

    @Nullable
    private static Pair<String, Long> getTextTombstoneFileSpec(@Nullable File protoTombstone) {
        if (protoTombstone == null) {
            return null;
        }

        String protoPath = protoTombstone.getAbsolutePath();
        String suffix = ".pb";
        if (!protoPath.endsWith(suffix)) {
            Slog.e(TAG, "unexpected proto tombstone path: " + protoPath);
            return null;
        }

        String textPath = protoPath.substring(0, protoPath.length() - suffix.length());
        File textFile = new File(textPath);
        if (!textFile.isFile()) {
            // text tombstone file is written before proto tombstone
            Slog.e(TAG, "missing text tombstone, path: " +  textPath);
            return null;
        }

        // Tombstone file can be replaced by a subsequent tombstone file. Add last modified
        // timestamp to detect that case
        long lastModified = textFile.lastModified();
        if (lastModified <= 0) {
            Slog.e(TAG, "lastModified time of " + textPath + " is " + lastModified);
            return null;
        }
        return Pair.create(textPath, Long.valueOf(lastModified));
    }

    private static boolean shouldIgnore(TombstoneProtos.Tombstone t, TombstoneProtos.BacktraceFrame[] backtrace) {
        String[] cmdline = t.commandLine;
        if (cmdline.length < 1) {
            return false;
        }
        if ("/vendor/bin/hw/android.hardware.bluetooth-service.bcmbtlinux".equals(cmdline[0])) {
            return checkBacktraceFunctionNames(backtrace, 0
                    , "abort"
                    , "fatal_error"
                    , "deallocate_small"
                    , "android::hardware::bluetooth::hci::HciFlowControl::~HciFlowControl()"
                    , "android::hardware::bluetooth::hci::shim::Deinitialize()"
                    , "android::hardware::bluetooth::aidl::bcmbtlinux::BluetoothHci::signal_handler(int)"
            ) || checkBacktraceFunctionNames(backtrace, 0
                    , "abort"
                    , "fatal_error"
                    , "deallocate_small"
                    , "android::hardware::bluetooth::hci::shim::Deinitialize()"
                    , "android::hardware::bluetooth::aidl::bcmbtlinux::BluetoothHci::signal_handler(int)"
            ) || checkBacktraceFunctionNames(backtrace, 0
                    , "abort"
                    , "fatal_error"
                    , "deallocate_small"
                    , "android::hardware::bluetooth::hci::H4Protocol::~H4Protocol()"
                    , "android::hardware::bluetooth::hci::shim::DeinitializeCallback()"
                    , "android::hardware::bluetooth::hci::HciFlowControl::~HciFlowControl()"
                    , "android::hardware::bluetooth::hci::shim::Deinitialize()"
                    , "android::hardware::bluetooth::aidl::bcmbtlinux::BluetoothHci::signal_handler(int)"
            ) || checkBacktraceFunctionNames(backtrace, 0
                    , "abort"
                    , "fatal_error"
                    , "deallocate_small"
                    , "android::hardware::bluetooth::hci::shim::DeinitializeCallback()"
                    , "android::hardware::bluetooth::hci::HciFlowControl::~HciFlowControl()"
                    , "android::hardware::bluetooth::hci::shim::Deinitialize()"
                    , "android::hardware::bluetooth::aidl::bcmbtlinux::BluetoothHci::signal_handler(int)"
            );
        }

        if ("/apex/com.google.android.hardware.biometrics.fingerprint/bin/hw/android.hardware.biometrics.fingerprint-service.goodix".equals(cmdline[0])) {
            // rare harmless crash, fingerprint service restarts and continues to work
            return checkBacktraceFunctionNames(backtrace, 0
                    , "android::VectorImpl::editArrayImpl()"
                    , "goodix::EventCenter::hasUpEvt()"
                    , "goodix::DelmarSensor::checkFingerUp(unsigned int)"
                    , "goodix::DelmarSensor::readImage(unsigned int, unsigned long)"
                    , "goodix::CustomizedSensor::readImage(unsigned int, unsigned long)"
                    , "goodix::DelmarFingerprintCore::onAfterAuthCapture(goodix::FingerprintCore::AuthenticateContext*)"
                    , "goodix::CustomizedFingerprintCore::onAfterAuthCapture(goodix::FingerprintCore::AuthenticateContext*)"
                    , "goodix::FingerprintCore::onAuthDownEvt()"
            );
        }

        return false;
    }

    private static boolean checkBacktraceFunctionNames(TombstoneProtos.BacktraceFrame[] backtrace, int offset, String... names) {
        if (offset + names.length > backtrace.length) {
            return false;
        }

        for (int i = 0; i < names.length; ++i) {
            if (!names[i].equals(backtrace[offset + i].functionName)) {
                return false;
            }
        }
        return true;
    }
}
