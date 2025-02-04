package com.android.server.pm.ext;

import com.android.internal.pm.pkg.component.ParsedPermission;
import com.android.internal.pm.pkg.component.ParsedProvider;
import com.android.internal.pm.pkg.component.ParsedUsesPermission;
import com.android.internal.pm.pkg.parsing.PackageParsingHooks;

class GsfParsingHooks extends PackageParsingHooks {

    @Override
    public boolean shouldSkipPermissionDefinition(ParsedPermission p) {
        String name = p.getName();
        if (GmsCoreHooks.ParsingHooks.shouldSkipPermissionDefinition(name)) {
            // these permissions are declared both in GSF and in GmsCore
            return true;
        }

        switch (name) {
            // DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION is used to emulate registering a
            // receiver with RECEIVER_NOT_EXPORTED flag on OS versions older than 13:
            // https://cs.android.com/androidx/platform/frameworks/support/+/0177ceca157c815f5e5e46fe5c90e12d9faf4db3
            // https://cs.android.com/androidx/platform/frameworks/support/+/cb9edef10187fe5e0fc55a49db6b84bbecf4ebf2
            // Normally, it is declared as <package name>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION,
            // (ie com.google.android.gsf.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION for GSF)
            // androidx.core.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION declaration seems to
            // be a build system bug.
            // There's also
            // {androidx.fragment,androidx.legacy.coreutils,does.not.matter}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION
            // Each of these prefixes is a packageName of a library that GSF seems to be compiled with.

            // All of these DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION permissions are declared
            // with android:protectionLevel="signature", which means that app installation
            // will fail if an app that has the same declaration is already installed
            // (there are some exceptions to this for system apps, but not for regular apps)

            // System package com.shannon.imsservice declares
            // androidx.core.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION (likely due to the same
            // bug), which blocks GSF from being installed.
            // Since this permission isn't actually used for anything, removing it is safe.
            case "androidx.core.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION":
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean shouldSkipUsesPermission(ParsedUsesPermission p) {
        // See shouldSkipProvider(). GSF is in the same sharedUid as GmsCore, ignore all
        // uses-permission declarations to remove misleading permission entries in App info UI for
        // GSF.
        return true;
    }

    @Override
    public boolean shouldSkipProvider(ParsedProvider p) {
        // On SDK 35, all GSF ContentProviders are moved to GmsCore and GSF becomes an
        // "android:hasCode=false" package, i.e. it never runs.
        //
        // Users who have installed GSF 34 while on SDK 34 (Android 14) will still have it after
        // updating to SDK 35 (Android 15), which leads to GmsCore breaking due to ContentProvider
        // conflicts between GSF and GmsCore.
        //
        // As a workaround, ignore all GSF ContentProvider.
        return true;
    }
}
