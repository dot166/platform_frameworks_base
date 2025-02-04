package android.ext;

import android.content.Intent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

/** @hide */
public class LogViewerApp {
    private static final String PACKAGE_NAME = "app.grapheneos.logviewer";

    public static final String ACTION_ERROR_REPORT = PACKAGE_NAME + ".ERROR_REPORT";
    public static final String ACTION_LOGCAT = PACKAGE_NAME + ".LOGCAT";
    public static final String ACTION_PKG_LOGCAT = PACKAGE_NAME + ".PKG_LOGCAT";

    public static final String EXTRA_ERROR_TYPE = "type";
    public static final String EXTRA_GZIPPED_MESSAGE = "gzipped_msg";
    public static final String EXTRA_SOURCE_APP_INFO = "source_app_info";
    public static final String EXTRA_SHOW_REPORT_BUTTON = "show_report_button";
    public static final String EXTRA_TEXT_TOMBSTONE_FILE_PATH = "text_tombstone_file_path";
    // Tombstone file path can be reused by a subsequent tombstone file, last modified timestamp is
    // used to detect that case.
    public static final String EXTRA_TEXT_TOMBSTONE_LAST_MODIFIED_TIME = "text_tombstone_last_modified";
    public static final String EXTRA_PREFER_TEXT_TOMBSTONE = "prefer_text_tombstone";

    public static String getPackageName() {
        return PACKAGE_NAME;
    }

    public static Intent createBaseErrorReportIntent(String msg) {
        var i = new Intent(ACTION_ERROR_REPORT);
        i.putExtra(EXTRA_GZIPPED_MESSAGE, gzipString(msg));
        // this is needed for correct usage via PendingIntent
        i.setIdentifier(UUID.randomUUID().toString());
        i.setPackage(PACKAGE_NAME);
        return i;
    }

    public static Intent getPackageLogcatIntent(String pkgName) {
        var i = new Intent(ACTION_PKG_LOGCAT);
        i.putExtra(Intent.EXTRA_PACKAGE_NAME, pkgName);
        i.setPackage(PACKAGE_NAME);
        return i;
    }

    public static Intent getLogcatIntent() {
        var i = new Intent(ACTION_LOGCAT);
        i.setPackage(PACKAGE_NAME);
        return i;
    }

    private static byte[] gzipString(String msg) {
        var bos = new ByteArrayOutputStream(msg.length() / 4);

        try (var s = new GZIPOutputStream(bos)) {
            s.write(msg.getBytes(UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        return bos.toByteArray();
    }
}
