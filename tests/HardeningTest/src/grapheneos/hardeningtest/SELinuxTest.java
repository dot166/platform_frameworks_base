package grapheneos.hardeningtest;

import android.content.pm.GosPackageStateFlag;

import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.testtype.DeviceJUnit4ClassRunner;
import com.android.tradefed.testtype.junit4.BaseHostJUnit4Test;
import com.android.tradefed.testtype.junit4.DeviceTestRunOptions;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;

@RunWith(DeviceJUnit4ClassRunner.class)
public class SELinuxTest extends BaseHostJUnit4Test {

    private static final String TEST_PACKAGE_BASE_NAME = "app.grapheneos.hardeningtest";

    private static final String TEST_PACKAGE_SDK_27 = TEST_PACKAGE_BASE_NAME + ".sdk_27";
    private static final String TEST_PACKAGE_SDK_LATEST = TEST_PACKAGE_BASE_NAME + ".sdk_latest";
    private static final String TEST_PACKAGE_SDK_LATEST_PREINSTALLED = TEST_PACKAGE_BASE_NAME + ".preinstalled";

    private void runDeviceTest(String pkgName, String name) {
        for (String suffix : new String[] {"", "Isolated"}) {
            var opts = new DeviceTestRunOptions(pkgName);
            opts.setTestClassName("app.grapheneos.hardeningtest.HardeningTest");
            opts.setTestMethodName(name + suffix);
            try {
                runDeviceTests(opts);
            } catch (DeviceNotAvailableException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private void editGosPackageState(String pkgName, int[] addFlags, int[] clearFlags) {
        try {
            var device = getDevice();
            var cmd = new StringBuilder("pm edit-gos-package-state " + pkgName + " " + device.getCurrentUser());
            for (int flag : addFlags) {
                cmd.append(" add-flag ").append(flag);
            }
            for (int flag : clearFlags) {
                cmd.append(" clear-flag ").append(flag);
            }
            var edRes = device.executeShellV2Command(cmd.toString());
            assertEquals(edRes.toString(), 0L, (long) edRes.getExitCode());
        } catch (DeviceNotAvailableException e) {
            throw new IllegalStateException(e);
        }
    }

    private void setComplexFlagState(String pkgName, int flag, int nonDefaultFlag, boolean isSet) {
        int[] addFlags = isSet ? new int[] { nonDefaultFlag, flag } : new int[] { nonDefaultFlag };
        int[] clearFlags = isSet ? new int[0] : new int[] { flag };
        editGosPackageState(pkgName, addFlags, clearFlags);
    }

    private void forEachPackage(Consumer<String> action) {
        for (String pkg : new String[] {TEST_PACKAGE_SDK_27, TEST_PACKAGE_SDK_LATEST, TEST_PACKAGE_SDK_LATEST_PREINSTALLED}) {
            if (pkg == TEST_PACKAGE_SDK_LATEST_PREINSTALLED) {
                try {
                    if (!getDevice().getBooleanProperty("ro.debuggable", false)) {
                        // preinstalled app is present only on debuggable builds
                        continue;
                    }
                } catch (DeviceNotAvailableException e) {
                    throw new IllegalStateException(e);
                }
            }
            action.accept(pkg);
        }
    }

    enum DclTestType {
        Memory(GosPackageStateFlag.RESTRICT_MEMORY_DYN_CODE_LOADING, GosPackageStateFlag.RESTRICT_MEMORY_DYN_CODE_LOADING_NON_DEFAULT),
        Storage(GosPackageStateFlag.RESTRICT_STORAGE_DYN_CODE_LOADING, GosPackageStateFlag.RESTRICT_STORAGE_DYN_CODE_LOADING_NON_DEFAULT),
        ;

        final int gosPsFlag;
        final int gosPsNonDefaultFlag;

        DclTestType(int gosPsFlag, int gosPsNonDefaultFlag) {
            this.gosPsFlag = gosPsFlag;
            this.gosPsNonDefaultFlag = gosPsNonDefaultFlag;
        }

        String testName(String suffix) {
            return "test" + name() + suffix;
        }
    }

    @Test
    public void testDynamicCodeLoadingRestricted() {
        forEachPackage(pkg -> {
            for (var t : DclTestType.values()) {
                setComplexFlagState(pkg, t.gosPsFlag, t.gosPsNonDefaultFlag, true);
                runDeviceTest(pkg, t.testName("DclRestricted"));

                if (pkg == TEST_PACKAGE_SDK_LATEST_PREINSTALLED) {
                    // check that DCL is blocked regardless of GosPackageState flags
                    setComplexFlagState(pkg, t.gosPsFlag, t.gosPsNonDefaultFlag, false);
                    runDeviceTest(pkg, t.testName("DclRestricted"));
                }
            }
        });
    }

    @Test
    public void testDynamicCodeLoadingAllowed() {
        forEachPackage(pkg -> {
            if (pkg == TEST_PACKAGE_SDK_LATEST_PREINSTALLED) {
                // preinstalled apps are DCL-restricted (except allowlisted ones)
                return;
            }

            for (var t : DclTestType.values()) {
                setComplexFlagState(pkg, t.gosPsFlag, t.gosPsNonDefaultFlag, false);
                runDeviceTest(pkg, t.testName("DclAllowed"));
            }
        });
    }

    @Test
    public void testPtraceAllowed() {
        forEachPackage(pkg -> {
            if (pkg == TEST_PACKAGE_SDK_LATEST_PREINSTALLED) {
                // preinstalled apps are always denied ptrace access
                return;
            }

            setComplexFlagState(pkg,
                GosPackageStateFlag.BLOCK_NATIVE_DEBUGGING,
                GosPackageStateFlag.BLOCK_NATIVE_DEBUGGING_NON_DEFAULT,
                false);
            runDeviceTest(pkg, "testPtraceAllowed");
        });
    }

    @Test
    public void testPtraceDenied() {
        forEachPackage(pkg -> {
            setComplexFlagState(pkg,
                GosPackageStateFlag.BLOCK_NATIVE_DEBUGGING,
                GosPackageStateFlag.BLOCK_NATIVE_DEBUGGING_NON_DEFAULT,
                true);
            runDeviceTest(pkg, "testPtraceDenied");
        });
    }
}
