package dev.redlab.gamespace;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.IBinder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * Decides which of the three transports this phone offers, before any control screen is shown.
 *
 * <p>Order matters: root is tried first because it needs no companion app, Shizuku second
 * because it gives shell identity without root, and NONE last. The probe only reads — no
 * command that changes state is run here.
 */
public final class ModeProbe {

    private static final String SHIZUKU_PACKAGE = "moe.shizuku.privileged.api";
    private static final long ROOT_TIMEOUT_SECONDS = 3;

    private ModeProbe() {
    }

    public static ControlMode detect(Context context) {
        if (hasRootShell()) {
            return ControlMode.ROOT;
        }
        if (hasShizuku(context)) {
            return ControlMode.SHIZUKU;
        }
        return ControlMode.NONE;
    }

    /** Runs `su -c id` and accepts only a real uid 0 answer; a missing su binary is not an error. */
    private static boolean hasRootShell() {
        Process process = null;
        try {
            process = new ProcessBuilder("su", "-c", "id").redirectErrorStream(true).start();
            if (!process.waitFor(ROOT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                return false;
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                return line != null && line.contains("uid=0");
            }
        } catch (Exception unavailable) {
            return false;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    /**
     * A package alone does not prove Shizuku is running: the binder lookup does. When the hidden
     * ServiceManager call is blocked the answer is "not available", never a guess.
     */
    private static boolean hasShizuku(Context context) {
        try {
            context.getPackageManager().getPackageInfo(SHIZUKU_PACKAGE, 0);
        } catch (PackageManager.NameNotFoundException absent) {
            return false;
        }
        try {
            Class<?> serviceManager = Class.forName("android.os.ServiceManager");
            Method getService = serviceManager.getDeclaredMethod("getService", String.class);
            Object binder = getService.invoke(null, "shizuku");
            return binder instanceof IBinder;
        } catch (Throwable blocked) {
            return false;
        }
    }
}
