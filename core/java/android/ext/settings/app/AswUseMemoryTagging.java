package android.ext.settings.app;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.GosPackageState;
import android.content.pm.GosPackageStateFlag;
import android.ext.PackageId;
import android.ext.settings.ExtSettings;

import com.android.server.os.nano.AppCompatProtos;

import dalvik.system.VMRuntime;

/** @hide */
public class AswUseMemoryTagging extends AppSwitch {
    public static final AswUseMemoryTagging I = new AswUseMemoryTagging();

    private AswUseMemoryTagging() {
        gosPsFlag = GosPackageStateFlag.FORCE_MEMTAG;
        gosPsFlagNonDefault = GosPackageStateFlag.FORCE_MEMTAG_NON_DEFAULT;
        gosPsFlagSuppressNotif = GosPackageStateFlag.FORCE_MEMTAG_SUPPRESS_NOTIF;
        compatChangeToDisableHardening = AppCompatProtos.DISABLE_MEMORY_TAGGING;
    }

    @Override
    public Boolean getImmutableValue(Context ctx, int userId, ApplicationInfo appInfo,
                                     GosPackageState ps, StateInfo si) {
        final String primaryAbi = appInfo.primaryCpuAbi;
        if (primaryAbi == null) {
            si.immutabilityReason = IR_NO_NATIVE_CODE;
            return true;
        }

        if (!VMRuntime.is64BitAbi(primaryAbi)) {
            si.immutabilityReason = IR_NON_64_BIT_NATIVE_CODE;
            return false;
        }

        if (appInfo.isSystemApp()) {
            switch (appInfo.packageName) {
                case PackageId.PIXEL_CAMERA_SERVICES_NAME:
                    return false;
            }
            si.immutabilityReason = IR_IS_SYSTEM_APP;
            return true;
        }

        int mm = appInfo.getMemtagMode();
        if (mm == ApplicationInfo.MEMTAG_ASYNC || mm == ApplicationInfo.MEMTAG_SYNC) {
            si.immutabilityReason = IR_OPTED_IN_VIA_MANIFEST;
            return true;
        }

        if (ps.hasFlag(GosPackageStateFlag.ENABLE_EXPLOIT_PROTECTION_COMPAT_MODE)) {
            si.immutabilityReason = IR_EXPLOIT_PROTECTION_COMPAT_MODE;
            return false;
        }

        return null;
    }

    @Override
    protected boolean getDefaultValueInner(Context ctx, int userId, ApplicationInfo appInfo,
                                           GosPackageState ps, StateInfo si) {
        si.defaultValueReason = DVR_DEFAULT_SETTING;
        return ExtSettings.FORCE_APP_MEMTAG_BY_DEFAULT.get(ctx, userId);
    }
}
