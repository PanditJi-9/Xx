package dev.redlab.gamespace;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The Game Space library: which launchable apps the lobby shows.
 *
 * <p>The vendor app adds everything it recognises as a game automatically and lets the user add
 * or remove the rest from the "Add Games" screen. That is two sets, kept in the app's own
 * preferences: packages the user added by hand, and games the user explicitly removed. A package
 * that is in neither set follows its own launcher category, so a newly installed game appears
 * without the app contacting any service.
 */
final class GameLibrary {

    private static final String PREFS = "game_library";
    private static final String ADDED = "added";
    private static final String REMOVED = "removed";

    private GameLibrary() {
    }

    static final class App {
        final String label;
        final String packageName;
        final Drawable icon;
        final boolean game;

        App(String label, String packageName, Drawable icon, boolean game) {
            this.label = label;
            this.packageName = packageName;
            this.icon = icon;
            this.game = game;
        }
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static boolean isIncluded(Context context, String packageName, boolean game) {
        SharedPreferences prefs = prefs(context);
        if (prefs.getStringSet(REMOVED, Collections.emptySet()).contains(packageName)) {
            return false;
        }
        return game || prefs.getStringSet(ADDED, Collections.emptySet()).contains(packageName);
    }

    /** Adds or removes one package; a game that is removed stays out across reinstall-free runs. */
    static void setIncluded(Context context, String packageName, boolean game, boolean included) {
        SharedPreferences prefs = prefs(context);
        Set<String> added = new HashSet<>(prefs.getStringSet(ADDED, Collections.emptySet()));
        Set<String> removed = new HashSet<>(prefs.getStringSet(REMOVED, Collections.emptySet()));
        if (included) {
            removed.remove(packageName);
            if (!game) {
                added.add(packageName);
            } else {
                added.remove(packageName);
            }
        } else {
            added.remove(packageName);
            if (game) {
                removed.add(packageName);
            } else {
                removed.remove(packageName);
            }
        }
        prefs.edit().putStringSet(ADDED, added).putStringSet(REMOVED, removed).apply();
    }

    /** Every launchable app except this one, in the launcher's own label order. */
    static List<App> candidates(Context context) {
        PackageManager manager = context.getPackageManager();
        Intent launcher = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolved = manager.queryIntentActivities(launcher, 0);
        List<App> found = new ArrayList<>();
        String self = context.getPackageName();
        for (ResolveInfo info : resolved) {
            if (info.activityInfo == null || info.activityInfo.applicationInfo == null) {
                continue;
            }
            String packageName = info.activityInfo.packageName;
            if (self.equals(packageName)) {
                continue;
            }
            CharSequence label = info.loadLabel(manager);
            Drawable icon = info.loadIcon(manager);
            boolean game = info.activityInfo.applicationInfo.category
                    == ApplicationInfo.CATEGORY_GAME;
            found.add(new App(label == null ? packageName : label.toString(),
                    packageName, icon, game));
        }
        Collections.sort(found, (a, b) -> a.label.compareToIgnoreCase(b.label));
        return found;
    }

    /** The apps the lobby shows: the included ones, in label order. */
    static List<App> library(Context context) {
        List<App> included = new ArrayList<>();
        for (App app : candidates(context)) {
            if (isIncluded(context, app.packageName, app.game)) {
                included.add(app);
            }
        }
        return included;
    }
}
