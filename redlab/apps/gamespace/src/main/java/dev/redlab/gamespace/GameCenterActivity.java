package dev.redlab.gamespace;

import android.app.Activity;
import android.app.AppOpsManager;
import android.app.NotificationManager;
import android.app.usage.UsageStatsManager;
import android.app.WallpaperManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.XmlResourceParser;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.xmlpull.v1.XmlPullParser;

import androidx.constraintlayout.widget.Group;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import cn.nubia.gamecenter.settings.GcsAnimationUtil;

/**
 * The original Game Space Center shell, with the sections this phone can really serve.
 *
 * <p>It inflates the original {@code gcs_gamecenter_main}, {@code gcs_gamecenter_titlebar},
 * {@code gcs_category_item} and {@code gcs_divider} resources unchanged, keeps the original
 * category icons, strings and colours, and renders each section from the original
 * {@code res/xml/gcs_*} preference screen, so the rows, order and wording are the stock ones.
 * A row does real work through a standard Android API where one exists; a hardware-only row
 * keeps its stock appearance and says once, when touched, that it needs the RedMagic hardware.
 */
public final class GameCenterActivity extends Activity {

    /** Opened by the lobby wallpaper control; selects that category instead of the first one. */
    public static final String EXTRA_SECTION = "dev.redlab.gamespace.extra.SECTION";
    public static final String SECTION_WALLPAPER = "wallpaper";
    /** Opened by the lobby's 3A/PC track, which the original wires to the streaming feature. */
    public static final String SECTION_STREAMING = "streaming";
    /** Opened by the upper bar's score-record button. */
    public static final String SECTION_RECORDS = "records";
    /** Opened by the upper bar's personal-center button. */
    public static final String SECTION_PROFILE = "profile";
    /** Opened by the lobby's fan button: the real SoC and battery temperatures. */
    public static final String SECTION_FAN = "fan";
    /** Opened by the lobby's mode button: the power modes this device reports. */
    public static final String SECTION_POWER = "power";
    /** Opened by the planet strip's mirror and multi-screen cards. */
    public static final String SECTION_MIRROR = "mirror";
    /** Opened by the planet strip's plugin and behaviour cards. */
    public static final String SECTION_OTHER = "other";

    private static final String PREFS = "game_space_center";
    private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";
    /**
     * Preference keys whose only implementation is genuinely RedMagic hardware or a Nubia
     * service. Everything outside this list is either a real Android setting or a Game Space
     * option this port keeps, so it never answers with the hardware sentence.
     */
    private static final String[] HARDWARE_KEYS = {
            "auto_dissipating",
            "device",
            "fiber_catcher",
            "learned_behavior",
            "shoulder_key_launch_gamespace",
            "key_mirror_host_mode",
            "xr_mirror",
            "gamekey_lamp_switch",
            "gamemode_video_quality",
            "gcs_record_game_switch",
            "gcs_record_full_video",
            "gcs_game_video_death",
            "gcs_game_real_time_video_death",
            "gcs_game_scene_interaction_switch"
    };

    private static final int[] WALLPAPERS = {
            R.mipmap.wallpaper_0, R.mipmap.wallpaper_1, R.mipmap.wallpaper_2, R.mipmap.wallpaper_3};

    private final List<Row> rows = new ArrayList<>();

    private FrameLayout content;
    private SettingsAdapter adapter;
    private int selectedPosition = -1;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        PortLayoutInflater.install(getLayoutInflater());
        setContentView(R.layout.gcs_gamecenter_main);
        setTitle(R.string.gcs_gamecenter_title);

        getWindow().setStatusBarColor(Color.BLACK);
        fitSystemBars();

        content = findViewById(R.id.content);

        View back = findViewById(R.id.left_name);
        back.setOnClickListener(view -> finish());
        View title = findViewById(R.id.titlebar_text);
        if (title instanceof TextView) {
            ((TextView) title).setText(R.string.gcs_gamecenter_title);
        }

        buildRows();

        RecyclerView category = findViewById(R.id.category);
        category.setLayoutManager(new LinearLayoutManager(this));
        category.setHasFixedSize(true);
        adapter = new SettingsAdapter();
        category.setAdapter(adapter);

        select(sectionIndex(getIntent().getStringExtra(EXTRA_SECTION)), true);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        // launchMode is the stock singleTask, so a second stock-action launch reuses this
        // instance; the requested section must still be applied.
        if (adapter != null) {
            select(sectionIndex(intent.getStringExtra(EXTRA_SECTION)), true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // The zen and network panels read live system state; refresh them after returning from a
        // system screen so the panel never shows a stale value.
        if (adapter != null) {
            renderCurrent();
        }
    }

    /**
     * Android 15+ draws activities edge to edge; the original title bar is measured for a
     * non-edge-to-edge window, so reserve the system bar insets on the content root.
     */
    private void fitSystemBars() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return;
        }
        View root = findViewById(android.R.id.content);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            android.graphics.Insets bars = insets.getInsets(
                    WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        WindowInsetsController controller = getWindow().getInsetsController();
        if (controller != null) {
            controller.setSystemBarsAppearance(
                    0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
        }
    }

    private int sectionIndex(String section) {
        Kind wanted = SECTION_WALLPAPER.equals(section) ? Kind.WALLPAPER
                : SECTION_STREAMING.equals(section) ? Kind.STREAMING
                : SECTION_RECORDS.equals(section) ? Kind.RECORDS
                : SECTION_PROFILE.equals(section) ? Kind.PROFILE
                : SECTION_FAN.equals(section) ? Kind.FAN
                : SECTION_POWER.equals(section) ? Kind.POWER
                : SECTION_MIRROR.equals(section) ? Kind.MIRROR
                : SECTION_OTHER.equals(section) ? Kind.OTHER
                : null;
        if (wanted != null) {
            for (int i = 0; i < rows.size(); i++) {
                if (rows.get(i).kind == wanted) {
                    return i;
                }
            }
        }
        return firstCategoryIndex();
    }

    private int firstCategoryIndex() {
        for (int i = 0; i < rows.size(); i++) {
            if (!rows.get(i).divider) {
                return i;
            }
        }
        return 0;
    }

    /**
     * The original {@code GameCenterActivity.getShowCategory()} order, with none of its removals:
     * the vendor build dropped a category when a feature flag or a Nubia-only chassis was absent,
     * and this build deliberately keeps every one of them visible. A category that needs vendor
     * hardware opens its stock screen and says so, instead of disappearing.
     */
    private void buildRows() {
        addCategory(R.drawable.user_login, R.string.gcs_gamecenter_menu_arkbase, Kind.PROFILE);
        addCategory(R.drawable.data_manager, R.string.gcs_gamecenter_menu_datamanager, Kind.DATA);
        addCategory(R.drawable.user_login, R.string.gcs_gamecenter_menu_game_status, Kind.SUMMARY);
        addDivider();
        addCategory(R.drawable.network_settings, R.string.gcs_gamecenter_menu_network, Kind.NETWORK);
        addCategory(R.drawable.not_disturb_settings, R.string.gcs_gamecenter_menu_not_disturb, Kind.ZEN);
        addCategory(R.drawable.false_touch_settings, R.string.gcs_gamecenter_menu_false_touch,
                Kind.FALSE_TOUCH);
        addCategory(R.drawable.false_touch_settings, R.string.gcs_gamecenter_menu_false_touch_zte,
                Kind.FALSE_TOUCH_ZTE);
        addCategory(R.drawable.competitive_off, R.string.gcs_gamecenter_menu_competitive_off,
                Kind.RACE_KEY);
        addCategory(R.drawable.game_space_wallpaper, R.string.gcs_gamecenter_menu_game_wallpaper,
                Kind.WALLPAPER);
        addCategory(R.drawable.gcs_watermark_edit_cursor, R.string.gcs_gamecenter_menu_screenshot_mark,
                Kind.WATERMARK);
        addCategory(R.drawable.stream_game, stringId("gcs_streaming"), Kind.STREAMING);
        addCategory(R.drawable.other_options, R.string.gcs_gamecenter_menu_other_options, Kind.OTHER);
        addCategory(R.drawable.competitive_lamp, R.string.gamekey_lamp_switch, Kind.LAMP);
        addCategory(R.drawable.cooling_fan_on, R.string.gcs_game_fan_title, Kind.FAN);
        addCategory(R.drawable.icon_strengthen_performance,
                R.string.gamecontrol_menu_strengthperformance, Kind.POWER);
        addCategory(R.drawable.mirror_host, stringId("gcs_game_mirror_host_mode_title"), Kind.MIRROR);
        addCategory(R.drawable.screen_settings, R.string.gcs_gamecenter_menu_screen, Kind.SCREEN);
        addCategory(R.drawable.custom_refreshrate_60hz, R.string.gcs_game_refresh_rate_title,
                Kind.REFRESH_RATE);
        addCategory(R.drawable.game_dock_settings, R.string.gcs_game_chicken_mode_title, Kind.CHICKEN);
        addCategory(R.drawable.gcs_record_settings, R.string.gcs_gamecenter_menu_records, Kind.RECORDS);
        addDivider();
        addCategory(R.drawable.about_gamespace, R.string.gcs_gamecenter_menu_about, Kind.ABOUT);
    }

    private void addCategory(int icon, int title, Kind kind) {
        Row row = new Row();
        row.icon = icon;
        row.title = title;
        row.kind = kind;
        rows.add(row);
    }

    private void addDivider() {
        Row row = new Row();
        row.divider = true;
        rows.add(row);
    }

    private void select(int position, boolean force) {
        if (position < 0 || position >= rows.size() || rows.get(position).divider) {
            position = firstCategoryIndex();
        }
        if (!force && position == selectedPosition) {
            renderCurrent();
            return;
        }
        int previous = selectedPosition;
        selectedPosition = position;
        if (previous >= 0) {
            adapter.notifyItemChanged(previous);
        }
        adapter.notifyItemChanged(position);
        renderCurrent();
    }

    private void renderCurrent() {
        if (selectedPosition < 0 || selectedPosition >= rows.size()) {
            return;
        }
        Row row = rows.get(selectedPosition);
        if (row.divider) {
            return;
        }
        content.removeAllViews();
        content.addView(buildPanel(row));
    }

    private View buildPanel(Row row) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundResource(R.drawable.gcs_gamecenter_fragment_background);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(28);
        box.setPadding(pad, dp(22), pad, dp(22));
        scroll.addView(box, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        switch (row.kind) {
            case PROFILE:
                buildProfile(box);
                break;
            case DATA:
                buildData(box);
                break;
            case SUMMARY:
                buildSummary(box);
                break;
            case SCREEN:
                buildScreen(box);
                break;
            case NETWORK:
                buildNetwork(box);
                break;
            case ZEN:
                buildZen(box);
                break;
            case FALSE_TOUCH:
                renderVendorScreen(box, R.xml.gcs_basic_false_touch);
                break;
            case FALSE_TOUCH_ZTE:
                renderVendorScreen(box, R.xml.gcs_basic_false_touch_zte);
                break;
            case RACE_KEY:
                renderVendorScreen(box, R.xml.gcs_race_key_off);
                break;
            case WALLPAPER:
                buildWallpaper(box);
                break;
            case WATERMARK:
                renderVendorScreen(box, R.xml.gcs_watermark_settings);
                break;
            case STREAMING:
                buildStreaming(box);
                break;
            case LAMP:
                renderVendorScreen(box, R.xml.gcs_gamekey_lamp_settings);
                break;
            case MIRROR:
                buildMirror(box);
                break;
            case REFRESH_RATE:
                buildRefreshRate(box);
                break;
            case CHICKEN:
                renderVendorScreen(box, R.xml.gcs_game_chicken_mode);
                break;
            case RECORDS:
                renderVendorScreen(box, R.xml.gcs_records_settings);
                break;
            case OTHER:
                buildOther(box);
                break;
            case FAN:
                buildFan(box);
                break;
            case POWER:
                buildPower(box);
                break;
            case ABOUT:
                buildAbout(box);
                break;
        }
        return scroll;
    }

    /**
     * The vendor fan page, with the real thermal readings this phone exposes.
     *
     * <p>Every control the stock page lists is kept: intelligent/manual cooling and the three
     * speeds. Setting them needs the RedMagic fan service, which is absent, so each row says so
     * once when touched. The temperatures above them are read from sysfs and the battery API, so
     * the page is not empty information.
     */
    private void buildFan(LinearLayout box) {
        addHeading(box, getString(R.string.gcs_game_fan_title));
        addBody(box, detail(getString(stringId("rl_hub_thermal_soc")), socTemperature()));
        addBody(box, detail(getString(stringId("rl_hub_thermal_battery")), batteryTemperature()));
        addBody(box, getString(R.string.gcs_game_fan_auto_summary));
        addBody(box, getString(R.string.gcs_game_fan_manual_summary));
        addBody(box, getString(R.string.gcs_game_fan_speed_low) + " \u00b7 "
                + getString(R.string.gcs_game_fan_speed_middle) + " \u00b7 "
                + getString(R.string.gcs_game_fan_speed_high));
        addBody(box, getString(stringId("rl_hub_fan_note")));
    }

    /**
     * The stock performance modes, with the current thermal state as the measurable part.
     *
     * <p>The mode names are the vendor's own (Eco / Balance / Rise / Diablo). Switching between
     * them is a system-server call the RedMagic performance service owns; on this phone the list
     * and the current readings are real, and the switch is explained instead of faked.
     */
    private void buildPower(LinearLayout box) {
        addHeading(box, getString(R.string.gamecontrol_menu_strengthperformance));
        addBody(box, detail(getString(stringId("rl_hub_thermal_soc")), socTemperature()));
        addBody(box, detail(getString(stringId("rl_hub_thermal_battery")), batteryTemperature()));
        addSubheading(box, getString(stringId("rl_hub_power_modes")));
        addBody(box, getString(R.string.nubia_game_performance_mode_1) + " \u00b7 "
                + getString(R.string.nubia_game_performance_mode_2) + " \u00b7 "
                + getString(R.string.nubia_game_performance_mode_3) + " \u00b7 "
                + getString(R.string.nubia_game_performance_mode_4));
        // The measurable power state of this phone: the battery, its charging state and the
        // system battery saver, which is the one power mode every Android build really has.
        Intent battery = registerReceiver(null,
                new android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (battery != null) {
            int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            int status = battery.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING
                    || status == BatteryManager.BATTERY_STATUS_FULL;
            if (level >= 0 && scale > 0) {
                String value = Math.round(level * 100f / scale) + "%"
                        + (charging ? " \u00b7 " + getString(stringId("rl_hub_charging")) : "");
                addBody(box, detail(getString(stringId("rl_hub_battery_level")), value));
            }
        }
        android.os.PowerManager power = getSystemService(android.os.PowerManager.class);
        addAction(box, getString(stringId("rl_hub_battery_saver")),
                yesNo(power != null && power.isPowerSaveMode()),
                view -> open(Settings.ACTION_BATTERY_SAVER_SETTINGS));
        addBody(box, getString(stringId("rl_hub_power_note")));
    }

    /** The streaming category, with the system's own cast page as its real entry. */
    private void buildStreaming(LinearLayout box) {
        addAction(box, getString(stringId("rl_hub_cast_open")), null,
                view -> open(Settings.ACTION_CAST_SETTINGS));
        renderVendorScreen(box, R.xml.gcs_streaming);
    }

    /** The mirror-host category: the system cast page is the standard, working equivalent. */
    private void buildMirror(LinearLayout box) {
        addAction(box, getString(stringId("rl_hub_cast_open")), null,
                view -> open(Settings.ACTION_CAST_SETTINGS));
        renderVendorScreen(box, R.xml.gcs_mirror_host_settings);
    }

    /** Hottest readable thermal zone, in Celsius. */
    private String socTemperature() {
        File zones = new File("/sys/class/thermal");
        File[] files = zones.listFiles((dir, name) -> name.startsWith("thermal_zone"));
        if (files == null) {
            return getString(stringId("rl_hub_thermal_none"));
        }
        double hottest = Double.NaN;
        for (File zone : files) {
            double value = readTemperature(new File(zone, "temp"));
            if (!Double.isNaN(value) && (Double.isNaN(hottest) || value > hottest)) {
                hottest = value;
            }
        }
        if (Double.isNaN(hottest)) {
            return getString(stringId("rl_hub_thermal_none"));
        }
        return String.format(Locale.US, "%.1f \u00b0C", hottest);
    }

    private double readTemperature(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            if (line == null) {
                return Double.NaN;
            }
            double raw = Double.parseDouble(line.trim());
            // The kernel reports millidegrees on nearly every Android device.
            return raw > 1000 ? raw / 1000.0 : raw;
        } catch (IOException | NumberFormatException unreadable) {
            return Double.NaN;
        }
    }

    private String batteryTemperature() {
        Intent battery = registerReceiver(null,
                new android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int tenths = battery == null ? -1
                : battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
        if (tenths < 0) {
            return getString(stringId("rl_hub_thermal_none"));
        }
        return String.format(Locale.US, "%.1f \u00b0C", tenths / 10.0);
    }

    private void buildSummary(LinearLayout box) {
        addHeading(box, getString(R.string.gcs_gamecenter_menu_game_status));
        TextView count = addBody(box, getString(stringId("rl_hub_games_count"), countLaunchable()));
        count.setTextColor(Color.WHITE);
        // The measurable part of the vendor's play-time card: today's foreground time in the
        // games this lobby really lists. It needs the usage-access grant Android requires;
        // without it the row offers the grant instead of inventing a number.
        if (usageAccessGranted()) {
            addBody(box, getString(stringId("rl_hub_playtime_today"),
                    duration(playtimeToday())));
        } else {
            addAction(box, getString(stringId("rl_hub_usage_grant")), null,
                    view -> open(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        }
        addBody(box, getString(stringId("rl_hub_summary_note")));
    }

    /** Foreground time today, summed over the packages the lobby currently lists. */
    private long playtimeToday() {
        UsageStatsManager manager = getSystemService(UsageStatsManager.class);
        if (manager == null) {
            return 0;
        }
        Set<String> games = new LinkedHashSet<>();
        for (GameLibrary.App app : GameLibrary.library(this)) {
            games.add(app.packageName);
        }
        if (games.isEmpty()) {
            return 0;
        }
        java.util.Calendar start = java.util.Calendar.getInstance();
        start.set(java.util.Calendar.HOUR_OF_DAY, 0);
        start.set(java.util.Calendar.MINUTE, 0);
        start.set(java.util.Calendar.SECOND, 0);
        start.set(java.util.Calendar.MILLISECOND, 0);
        long total = 0;
        for (android.app.usage.UsageStats stats : manager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY, start.getTimeInMillis(),
                System.currentTimeMillis())) {
            if (games.contains(stats.getPackageName())) {
                total += stats.getTotalTimeInForeground();
            }
        }
        return total;
    }

    private void buildScreen(LinearLayout box) {
        renderOriginalScreen(box, R.xml.gcs_screen_settings);
    }

    private void buildNetwork(LinearLayout box) {
        renderOriginalScreen(box, R.xml.gcs_net_settings);
        buildNetworkDetails(box);
    }

    /** Real values from the active network, not the vendor acceleration service. */
    private void buildNetworkDetails(LinearLayout box) {
        addSubheading(box, getString(stringId("rl_hub_net_details_title")));
        ConnectivityManager manager = getSystemService(ConnectivityManager.class);
        Network network = manager == null ? null : manager.getActiveNetwork();
        NetworkCapabilities caps = network == null || manager == null
                ? null : manager.getNetworkCapabilities(network);
        if (caps == null) {
            addBody(box, detail(getString(stringId("rl_hub_net_type")),
                    getString(stringId("rl_hub_net_none"))));
            return;
        }
        addBody(box, detail(getString(stringId("rl_hub_net_type")), activeNetworkName()));
        addBody(box, detail(getString(stringId("rl_hub_net_metered")),
                yesNo(!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED))));
        addBody(box, detail(getString(stringId("rl_hub_net_verified")),
                yesNo(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))));
        LinkProperties link = manager.getLinkProperties(network);
        if (link != null) {
            addBody(box, detail(getString(stringId("rl_hub_net_dns")),
                    join(link.getDnsServers())));
            addBody(box, detail(getString(stringId("rl_hub_net_ipv4")),
                    join(link.getLinkAddresses())));
            addBody(box, detail(getString(stringId("rl_hub_net_down")),
                    caps.getLinkDownstreamBandwidthKbps() + " Kbps"));
            addBody(box, detail(getString(stringId("rl_hub_net_up")),
                    caps.getLinkUpstreamBandwidthKbps() + " Kbps"));
        }
        addBody(box, getString(stringId("rl_hub_vendor_screen_note")));
    }

    private String detail(String label, String value) {
        return getString(stringId("rl_hub_net_detail"), label, value);
    }

    private String yesNo(boolean value) {
        return getString(stringId(value ? "rl_hub_yes" : "rl_hub_no"));
    }

    private static String join(List<?> values) {
        if (values == null || values.isEmpty()) {
            return "-";
        }
        StringBuilder text = new StringBuilder();
        for (Object value : values) {
            if (text.length() > 0) {
                text.append(", ");
            }
            text.append(value);
        }
        return text.toString();
    }

    private void buildZen(LinearLayout box) {
        renderOriginalScreen(box, R.xml.gcs_not_disturb_settings);
    }

    private void buildOther(LinearLayout box) {
        renderOriginalScreen(box, R.xml.gcs_other_options_settings);
    }

    private void buildAbout(LinearLayout box) {
        String version = "?";
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            if (info.versionName != null) {
                version = info.versionName;
            }
        } catch (PackageManager.NameNotFoundException ignored) {
            // The package is this process's package; the fallback only covers a broken install.
        }
        addBody(box, getString(stringId("rl_hub_about_body"), getPackageName(), version));
        addGap(box, 8);
        renderOriginalScreen(box, R.xml.gcs_about);
    }

    /** ArkBase is a Nubia account profile; the device facts it would show are real here. */
    private void buildProfile(LinearLayout box) {
        addHeading(box, getString(R.string.gcs_gamecenter_menu_arkbase));
        addBody(box, getString(stringId("rl_hub_profile_body"),
                Build.MANUFACTURER + " " + Build.MODEL,
                Build.VERSION.RELEASE, Build.VERSION.SDK_INT));
        addBody(box, getString(stringId("rl_hub_vendor_screen_note")));
    }

    /**
     * Data is the vendor's DataManager screen. Its numbers come from the same source Android
     * exposes to any app the user authorises: {@link UsageStatsManager}. Nothing is invented and
     * nothing is fabricated when the permission is missing.
     */
    private void buildData(LinearLayout box) {
        addHeading(box, getString(R.string.gcs_gamecenter_menu_datamanager));
        if (!usageAccessGranted()) {
            addBody(box, getString(stringId("rl_hub_usage_empty")));
            addAction(box, getString(stringId("rl_hub_usage_grant")), null,
                    view -> open(Settings.ACTION_USAGE_ACCESS_SETTINGS));
            addBody(box, getString(stringId("rl_hub_usage_note")));
            return;
        }
        List<UsageEntry> usage = readUsage();
        if (usage.isEmpty()) {
            addBody(box, getString(stringId("rl_hub_usage_empty")));
        } else {
            long total = 0;
            for (UsageEntry entry : usage) {
                total += entry.millis;
            }
            addBody(box, getString(stringId("rl_hub_usage_total"), duration(total)));
            int shown = 0;
            for (UsageEntry entry : usage) {
                if (shown == 10) {
                    break;
                }
                addAction(box, entry.label, duration(entry.millis),
                        view -> launchPackage(entry.packageName));
                shown++;
            }
        }
        addBody(box, getString(stringId("rl_hub_usage_note")));
    }

    private boolean usageAccessGranted() {
        AppOpsManager ops = getSystemService(AppOpsManager.class);
        if (ops == null) {
            return false;
        }
        int mode = ops.unsafeCheckOpNoThrow("android:get_usage_stats",
                android.os.Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    /** Foreground time over the last seven days, the window the vendor DataManager reports. */
    private List<UsageEntry> readUsage() {
        List<UsageEntry> result = new ArrayList<>();
        UsageStatsManager manager = getSystemService(UsageStatsManager.class);
        if (manager == null) {
            return result;
        }
        long now = System.currentTimeMillis();
        java.util.Map<String, Long> totals = new java.util.LinkedHashMap<>();
        for (android.app.usage.UsageStats stats : manager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY, now - 7L * 24 * 60 * 60 * 1000, now)) {
            if (stats.getTotalTimeInForeground() <= 0) {
                continue;
            }
            Long previous = totals.get(stats.getPackageName());
            totals.put(stats.getPackageName(),
                    (previous == null ? 0 : previous) + stats.getTotalTimeInForeground());
        }
        for (java.util.Map.Entry<String, Long> entry : totals.entrySet()) {
            String packageName = entry.getKey();
            if (getPackageName().equals(packageName)) {
                continue;
            }
            String label;
            try {
                label = getPackageManager()
                        .getApplicationLabel(getPackageManager().getApplicationInfo(packageName, 0))
                        .toString();
            } catch (PackageManager.NameNotFoundException gone) {
                continue;
            }
            result.add(new UsageEntry(packageName, label, entry.getValue()));
        }
        result.sort((a, b) -> Long.compare(b.millis, a.millis));
        return result;
    }

    private String duration(long millis) {
        long minutes = millis / 60000L;
        long hours = minutes / 60;
        minutes %= 60;
        return hours > 0
                ? getString(stringId("rl_hub_duration_hours"), (int) hours, (int) minutes)
                : getString(stringId("rl_hub_duration_minutes"), (int) minutes);
    }

    private void launchPackage(String packageName) {
        Intent launch = getPackageManager().getLaunchIntentForPackage(packageName);
        if (launch != null) {
            openIntent(launch);
        }
    }

    /** The stock refresh-rate screen plus the modes this display really reports. */
    private void buildRefreshRate(LinearLayout box) {
        addHeading(box, getString(R.string.gcs_game_refresh_rate_title));
        addBody(box, refreshSummary());
        addAction(box, getString(stringId("rl_hub_refresh_open")), null,
                view -> openIntent(new Intent(this,
                        cn.nubia.gamecenter.settings.basic.GameRefreshRateActivity.class)));
        addBody(box, getString(stringId("rl_hub_refresh_note")));
        renderOriginalScreen(box, R.xml.gcs_game_refresh_rate);
    }

    /** A stock screen the vendor gated behind hardware or a service: shown whole, then explained. */
    private void renderVendorScreen(LinearLayout box, int xml) {
        renderOriginalScreen(box, xml);
        addBody(box, getString(stringId("rl_hub_vendor_screen_note")));
    }

    /**
     * The stock screens are PreferenceScreens in res/xml, so they are built from the exact files
     * the vendor build used: same rows, order, titles, summaries and icons. Only the interaction
     * is ours, so a row can do real work on a phone that is not a RedMagic.
     */
    private void renderOriginalScreen(LinearLayout box, int xml) {
        try (XmlResourceParser parser = getResources().getXml(xml)) {
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                String tag = parser.getName();
                if ("PreferenceScreen".equals(tag)) {
                    continue;
                }
                String title = text(parser, "title");
                if (title == null || title.isEmpty()) {
                    // Rows without a title are decorative pieces of the stock screen.
                    continue;
                }
                String summary = text(parser, "summary");
                String key = parser.getAttributeValue(ANDROID_NS, "key");
                if (tag.endsWith("SwitchPreference")) {
                    addSwitchRow(box, key, title, summary);
                } else {
                    addPreferenceRow(box, key, title, summary);
                }
            }
        } catch (Exception unreadable) {
            android.util.Log.w("GameCenter",
                    "cannot read " + getResources().getResourceName(xml), unreadable);
        }
    }

    private String text(XmlResourceParser parser, String name) {
        int id = parser.getAttributeResourceValue(ANDROID_NS, name, 0);
        return id != 0 ? getString(id) : parser.getAttributeValue(ANDROID_NS, name);
    }

    private void addSwitchRow(LinearLayout box, String key, String title, String summary) {
        View row = getLayoutInflater().inflate(R.layout.gcs_switch_preference, box, false);
        bindRowText(row, title, summary);
        ViewGroup frame = row.findViewById(android.R.id.widget_frame);
        View widget = getLayoutInflater().inflate(R.layout.gcs_preference_widget_switch, frame, false);
        frame.addView(widget);
        ToggleButton toggle = widget.findViewById(R.id.gamespace_switch);
        toggle.setAlpha(1.0f);
        toggle.setClickable(false);
        toggle.setFocusable(false);
        hideSwitchDecor(widget);
        toggle.setChecked(switchState(key));
        row.setOnClickListener(view -> {
            boolean next = !toggle.isChecked();
            if (isHardwareKey(key)) {
                hardwareOnly();
                return;
            }
            boolean applied = applyKey(key, next);
            // Every switch that is not tied to RedMagic hardware keeps its state on this phone,
            // whether or not it also reaches a system setting, so the row never flips back.
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(key, next).apply();
            toggle.setChecked(next);
            if (!applied) {
                Toast.makeText(this, stringId("rl_saved"), Toast.LENGTH_SHORT).show();
            }
        });
        box.addView(row);
    }

    private void addPreferenceRow(LinearLayout box, String key, String title, String summary) {
        View row = getLayoutInflater().inflate(R.layout.gcs_preference, box, false);
        bindRowText(row, title, summary);
        row.setOnClickListener(view -> {
            if (applyAction(key, title)) {
                return;
            }
            if (isHardwareKey(key)) {
                hardwareOnly();
            } else {
                Toast.makeText(this, stringId("rl_no_page"), Toast.LENGTH_SHORT).show();
            }
        });
        box.addView(row);
    }

    private void bindRowText(View row, String title, String summary) {
        ((TextView) row.findViewById(android.R.id.title)).setText(title);
        TextView summaryView = row.findViewById(android.R.id.summary);
        if (summary == null || summary.isEmpty()) {
            summaryView.setVisibility(View.GONE);
        } else {
            summaryView.setText(summary);
        }
    }

    private void hideSwitchDecor(View widget) {
        int[] ids = {R.id.switch_button_bg, R.id.switch_button_track_black,
                R.id.switch_button_track_gray, R.id.switch_button_thumb_red,
                R.id.switch_button_thumb_black};
        for (int id : ids) {
            View layer = widget.findViewById(id);
            if (layer != null) {
                layer.setVisibility(View.GONE);
            }
        }
    }

    private boolean switchState(String key) {
        if ("mute".equals(key)) {
            AudioManager audio = getSystemService(AudioManager.class);
            return audio != null && audio.getStreamVolume(AudioManager.STREAM_MUSIC) == 0;
        }
        if ("key_call_widonw".equals(key) || "key_new_message_remind".equals(key)) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null && manager.isNotificationPolicyAccessGranted()) {
                int flag = "key_call_widonw".equals(key)
                        ? NotificationManager.Policy.PRIORITY_CATEGORY_CALLS
                        : NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES;
                return (manager.getNotificationPolicy().priorityCategories & flag) != 0;
            }
        }
        return getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(key, false);
    }

    private boolean applyKey(String key, boolean checked) {
        if ("key_call_widonw".equals(key) || "key_new_message_remind".equals(key)) {
            return setPriorityCategory("key_call_widonw".equals(key)
                    ? NotificationManager.Policy.PRIORITY_CATEGORY_CALLS
                    : NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES, checked);
        }
        if ("gamemode_fix_light".equals(key)) {
            return setFixedBrightness(checked);
        }
        if ("mute".equals(key)) {
            return setMediaMuted(checked);
        }
        if ("game_refresh_rate_sc_switch".equals(key)) {
            openIntent(new Intent(this,
                    cn.nubia.gamecenter.settings.basic.GameRefreshRateActivity.class));
            return true;
        }
        String page = settingsPageFor(key);
        if (page != null) {
            open(page);
            return true;
        }
        // The rest are software options of the Game Space this port carries; the switch keeps
        // its state and the caller says so, instead of blaming missing hardware.
        return false;
    }

    private boolean isHardwareKey(String key) {
        if (key == null) {
            return false;
        }
        for (String hardware : HARDWARE_KEYS) {
            if (hardware.equals(key)) {
                return true;
            }
        }
        return key.startsWith("game_mode_fang_wu_chu")
                || key.startsWith("gamemode_networkacceleration")
                || key.startsWith("gcs_game_chicken_mode")
                || key.startsWith("gcs_record_")
                || key.startsWith("select_gamekey_lamp");
    }

    /** The stock preference key to the system page that really owns its behaviour. */
    private String settingsPageFor(String key) {
        if (key == null) {
            return null;
        }
        switch (key) {
            case "gamemode_save_energy":
                return Settings.ACTION_BATTERY_SAVER_SETTINGS;
            case "gamemode_color_enhanced":
            case "gamemode_brilliant_time":
            case "gamemode_tempered_screen_protector":
            case "black_screen":
            case "lock_screen":
            case "key_off_intell_screen":
                return Settings.ACTION_DISPLAY_SETTINGS;
            case "gamemode_network":
            case "gamemode_change_network":
                return Settings.ACTION_WIRELESS_SETTINGS;
            case "gamemode_assistant_sim":
            case "key_off_screen_assistant":
            case "key_off_three_finger_shot":
            case "screen_shot_settings":
            case "mis_operate_ban_toast":
                return Settings.ACTION_ACCESSIBILITY_SETTINGS;
            default:
                return null;
        }
    }

    /** Media mute is a real device state, so the switch mirrors and changes it. */
    private boolean setMediaMuted(boolean muted) {
        AudioManager audio = getSystemService(AudioManager.class);
        if (audio == null) {
            return false;
        }
        int max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int target = muted ? 0
                : getSharedPreferences(PREFS, MODE_PRIVATE).getInt("media_volume", max / 2);
        try {
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0);
        } catch (SecurityException denied) {
            return false;
        }
        if (!muted) {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putInt("media_volume", target).apply();
        }
        return true;
    }

    private boolean applyAction(String key, String title) {
        if ("gcs_privacy_policy".equals(key)) {
            openIntent(new Intent(Intent.ACTION_VIEW,
                    Uri.parse(getString(R.string.zte_privacy_policy_url))));
            return true;
        }
        if ("gcs_system_update".equals(key) || "gcs_about_update".equals(key)) {
            Toast.makeText(this, stringId("rl_hub_up_to_date"), Toast.LENGTH_SHORT).show();
            return true;
        }
        if ("gcs_watermark_message".equals(key)) {
            editWatermark(title);
            return true;
        }
        String page = settingsPageFor(key);
        if (page != null) {
            open(page);
            return true;
        }
        if ("gcs_icp".equals(key)) {
            // The stock row is a licence number, not a link; show the number the build carries.
            Toast.makeText(this, getString(stringId("rl_icp_value")), Toast.LENGTH_LONG).show();
            return true;
        }
        return false;
    }

    /** The stock watermark text row was an EditTextPreference; keep that shape with a dialog. */
    private void editWatermark(String title) {
        android.widget.EditText input = new android.widget.EditText(this);
        input.setText(getSharedPreferences(PREFS, MODE_PRIVATE)
                .getString("gcs_watermark_message", ""));
        int pad = dp(20);
        FrameLayout frame = new FrameLayout(this);
        frame.setPadding(pad, pad, pad, pad);
        frame.addView(input, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        new android.app.AlertDialog.Builder(this)
                .setTitle(title)
                .setView(frame)
                .setPositiveButton(android.R.string.ok, (dialog, which) ->
                        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                                .putString("gcs_watermark_message",
                                        input.getText().toString().trim()).apply())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private boolean setPriorityCategory(int flag, boolean allow) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager == null) {
            return false;
        }
        if (!manager.isNotificationPolicyAccessGranted()) {
            open(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            return false;
        }
        NotificationManager.Policy policy = manager.getNotificationPolicy();
        int categories = allow
                ? policy.priorityCategories | flag
                : policy.priorityCategories & ~flag;
        try {
            manager.setNotificationPolicy(new NotificationManager.Policy(categories,
                    policy.priorityCallSenders, policy.priorityMessageSenders,
                    policy.suppressedVisualEffects));
        } catch (SecurityException denied) {
            return false;
        }
        return true;
    }

    private boolean setFixedBrightness(boolean fixed) {
        if (!Settings.System.canWrite(this)) {
            try {
                startActivity(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                        Uri.parse("package:" + getPackageName())));
            } catch (ActivityNotFoundException missing) {
                open(Settings.ACTION_SETTINGS);
            }
            return false;
        }
        return Settings.System.putInt(getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                fixed ? Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                        : Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
    }

    private void hardwareOnly() {
        Toast.makeText(this, stringId("rl_hw_only"), Toast.LENGTH_SHORT).show();
    }

    private void buildWallpaper(LinearLayout box) {
        int selected = getSharedPreferences(PREFS, MODE_PRIVATE).getInt("wallpaper_choice", 0);
        for (int index = 0; index < WALLPAPERS.length; index++) {
            View item = getLayoutInflater().inflate(R.layout.gcs_wallpaper_item, box, false);
            ImageView preview = item.findViewById(R.id.wallpaper_preview);
            preview.setImageResource(WALLPAPERS[index]);
            preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
            item.findViewById(R.id.preview_selected_bg)
                    .setVisibility(index == selected ? View.VISIBLE : View.GONE);
            item.findViewById(R.id.preview_selected_mark)
                    .setVisibility(index == selected ? View.VISIBLE : View.GONE);
            item.findViewById(R.id.live_label).setVisibility(View.GONE);
            item.findViewById(R.id.live_label_text).setVisibility(View.GONE);
            item.findViewById(R.id.title).setVisibility(View.GONE);
            int choice = index;
            item.setOnClickListener(view -> applyWallpaper(choice));
            box.addView(item);
            addGap(box, 10);
        }
        addBody(box, getString(stringId("rl_hub_wallpaper_note")));
    }

    private void applyWallpaper(int index) {
        new Thread(() -> {
            boolean ok;
            try {
                WallpaperManager manager = WallpaperManager.getInstance(this);
                manager.setBitmap(BitmapFactory.decodeResource(getResources(), WALLPAPERS[index]));
                ok = true;
            } catch (IOException | SecurityException | OutOfMemoryError failed) {
                ok = false;
            }
            boolean result = ok;
            runOnUiThread(() -> {
                if (result) {
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                            .putInt("wallpaper_choice", index).apply();
                    renderCurrent();
                }
                Toast.makeText(this, stringId(result
                        ? "rl_hub_wallpaper_done" : "rl_hub_wallpaper_failed"),
                        Toast.LENGTH_SHORT).show();
            });
        }, "set-wallpaper").start();
    }

    private String refreshSummary() {
        Display display = getWindowManager().getDefaultDisplay();
        if (display == null) {
            return getString(stringId("rl_hub_refresh_value"), "?");
        }
        Display.Mode current = display.getMode();
        String now = formatMode(current);
        Set<String> modes = new LinkedHashSet<>();
        for (Display.Mode mode : display.getSupportedModes()) {
            modes.add(formatMode(mode));
        }
        String list = String.join(" · ", modes);
        return getString(stringId("rl_hub_refresh_value"), now)
                + "\n" + getString(stringId("rl_hub_refresh_modes"), list);
    }

    private static String formatMode(Display.Mode mode) {
        return String.format(Locale.US, "%dx%d@%dHz",
                mode.getPhysicalWidth(), mode.getPhysicalHeight(), Math.round(mode.getRefreshRate()));
    }

    private String activeNetworkName() {
        ConnectivityManager manager = getSystemService(ConnectivityManager.class);
        if (manager == null) {
            return getString(stringId("rl_hub_net_none"));
        }
        Network network = manager.getActiveNetwork();
        NetworkCapabilities caps = network == null ? null : manager.getNetworkCapabilities(network);
        if (caps == null) {
            return getString(stringId("rl_hub_net_none"));
        }
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            return getString(stringId("rl_hub_net_wifi"));
        }
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            return getString(stringId("rl_hub_net_mobile"));
        }
        return getString(stringId("rl_hub_net_other"));
    }

    private int countLaunchable() {
        Intent launcher = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        int count = 0;
        for (android.content.pm.ResolveInfo info :
                getPackageManager().queryIntentActivities(launcher, 0)) {
            if (info.activityInfo != null && !getPackageName().equals(info.activityInfo.packageName)) {
                count++;
            }
        }
        return count;
    }

    private void open(String action) {
        openIntent(new Intent(action));
    }

    private void openIntent(Intent intent) {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException missing) {
            Toast.makeText(this, stringId("rl_open_failed"), Toast.LENGTH_SHORT).show();
        }
    }

    /** The sealed original R has no fields for owned strings, as in MainActivity.text(). */
    private int stringId(String name) {
        return getResources().getIdentifier(name, "string", getPackageName());
    }

    private TextView addHeading(LinearLayout box, String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(21f);
        view.setTextColor(Color.WHITE);
        view.setTypeface(view.getTypeface(), android.graphics.Typeface.BOLD);
        view.setPadding(0, 0, 0, dp(14));
        box.addView(view);
        return view;
    }

    private TextView addSubheading(LinearLayout box, String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(16f);
        view.setTextColor(getColor(R.color.gcs_gamecenter_menu_text_checked));
        view.setTypeface(view.getTypeface(), android.graphics.Typeface.BOLD);
        view.setPadding(0, dp(6), 0, dp(6));
        box.addView(view);
        return view;
    }

    private TextView addBody(LinearLayout box, String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(14f);
        view.setTextColor(getColor(R.color.gcs_summary_normal_summary));
        view.setLineSpacing(dp(3), 1f);
        view.setPadding(0, dp(2), 0, dp(10));
        box.addView(view);
        return view;
    }

    private void addAction(LinearLayout box, String title, String subtitle, View.OnClickListener click) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(13), dp(16), dp(13));
        row.setClickable(true);
        row.setFocusable(true);
        android.util.TypedValue ripple = new android.util.TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.selectableItemBackground, ripple, true)) {
            row.setBackgroundResource(ripple.resourceId);
        }

        TextView label = new TextView(this);
        label.setText(title);
        label.setTextSize(15f);
        label.setTextColor(Color.WHITE);
        row.addView(label);

        if (subtitle != null) {
            TextView sub = new TextView(this);
            sub.setText(subtitle);
            sub.setTextSize(12f);
            sub.setTextColor(getColor(R.color.gcs_summary_normal_summary));
            row.addView(sub);
        }

        row.setOnClickListener(click);
        box.addView(row);
        addLine(box);
    }

    private void addLine(LinearLayout box) {
        View line = new View(this);
        line.setBackgroundColor(0x22ffffff);
        box.addView(line, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(1))));
    }

    private void addGap(LinearLayout box, int dp) {
        View gap = new View(this);
        box.addView(gap, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(dp)));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static final class UsageEntry {
        final String packageName;
        final String label;
        final long millis;

        UsageEntry(String packageName, String label, long millis) {
            this.packageName = packageName;
            this.label = label;
            this.millis = millis;
        }
    }

    private enum Kind {
        PROFILE,
        DATA,
        SUMMARY,
        SCREEN,
        FALSE_TOUCH,
        FALSE_TOUCH_ZTE,
        RACE_KEY,
        WATERMARK,
        STREAMING,
        MIRROR,
        REFRESH_RATE,
        CHICKEN,
        RECORDS,
        NETWORK,
        ZEN,
        WALLPAPER,
        LAMP,
        FAN,
        POWER,
        OTHER,
        ABOUT
    }

    private static final class Row {
        boolean divider;
        int icon;
        int title;
        Kind kind;
    }

    private final class SettingsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @Override
        public int getItemViewType(int position) {
            return rows.get(position).divider ? 0 : 1;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == 0) {
                return new DividerHolder(getLayoutInflater()
                        .inflate(R.layout.gcs_divider, parent, false));
            }
            return new CategoryHolder(getLayoutInflater()
                    .inflate(R.layout.gcs_category_item, parent, false));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (!(holder instanceof CategoryHolder)) {
                return;
            }
            CategoryHolder category = (CategoryHolder) holder;
            Row row = rows.get(position);
            category.title.setText(row.title);
            category.title.setCompoundDrawablesRelativeWithIntrinsicBounds(row.icon, 0, 0, 0);

            boolean checked = position == selectedPosition;
            category.title.setTextColor(getColorStateList(checked
                    ? R.color.gcs_gamecenter_menu_text_checked
                    : R.color.gcs_summary_normal_summary));
            category.selectGroup.setVisibility(checked ? View.VISIBLE : View.GONE);
            if (checked) {
                GcsAnimationUtil.setGcsItemBgTranslationX(category.background);
                GcsAnimationUtil.setGcsRedItemAlpha(category.redEnd);
                GcsAnimationUtil.setGcsRedItemAlpha(category.redLineEnd);
            }
            category.root.setOnClickListener(view -> {
                int current = holder.getBindingAdapterPosition();
                if (current != RecyclerView.NO_POSITION) {
                    select(current, false);
                }
            });
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }
    }

    private static final class DividerHolder extends RecyclerView.ViewHolder {
        DividerHolder(View view) {
            super(view);
        }
    }

    private static final class CategoryHolder extends RecyclerView.ViewHolder {
        final View root;
        final TextView title;
        final Group selectGroup;
        final ImageView background;
        final ImageView redEnd;
        final ImageView redLineEnd;

        CategoryHolder(View view) {
            super(view);
            root = view.findViewById(R.id.category_item_root);
            title = view.findViewById(R.id.title);
            selectGroup = view.findViewById(R.id.select_group);
            background = view.findViewById(R.id.background);
            redEnd = view.findViewById(R.id.red_end);
            redLineEnd = view.findViewById(R.id.red_line_end);
        }
    }
}
