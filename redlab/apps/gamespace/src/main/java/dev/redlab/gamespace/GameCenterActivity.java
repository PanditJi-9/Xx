package dev.redlab.gamespace;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
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

import androidx.constraintlayout.widget.Group;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
 * category icons, strings and colours, and hosts owned panels instead of the vendor fragments.
 * Every action here either calls a standard Android surface (system settings, notification
 * policy, wallpaper manager) or states in place why the Nubia-only feature is not available.
 */
public final class GameCenterActivity extends Activity {

    /** Opened by the lobby wallpaper control; selects that category instead of the first one. */
    public static final String EXTRA_SECTION = "dev.redlab.gamespace.extra.SECTION";
    public static final String SECTION_WALLPAPER = "wallpaper";

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
        if (SECTION_WALLPAPER.equals(section)) {
            for (int i = 0; i < rows.size(); i++) {
                if (rows.get(i).kind == Kind.WALLPAPER) {
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

    private void buildRows() {
        addCategory(R.drawable.user_login, R.string.gcs_gamecenter_menu_game_status, Kind.SUMMARY);
        addDivider();
        addCategory(R.drawable.screen_settings, R.string.gcs_gamecenter_menu_screen, Kind.SCREEN);
        addCategory(R.drawable.network_settings, R.string.gcs_gamecenter_menu_network, Kind.NETWORK);
        addCategory(R.drawable.not_disturb_settings, R.string.gcs_gamecenter_menu_not_disturb, Kind.ZEN);
        addCategory(R.drawable.game_space_wallpaper, R.string.gcs_gamecenter_menu_game_wallpaper, Kind.WALLPAPER);
        addDivider();
        addCategory(R.drawable.other_options, R.string.gcs_gamecenter_menu_other_options, Kind.OTHER);
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
            case WALLPAPER:
                buildWallpaper(box);
                break;
            case OTHER:
                buildOther(box);
                break;
            case ABOUT:
                buildAbout(box);
                break;
        }
        return scroll;
    }

    private void buildSummary(LinearLayout box) {
        addHeading(box, getString(R.string.gcs_gamecenter_menu_game_status));
        TextView count = addBody(box, getString(R.string.rl_hub_games_count, countLaunchable()));
        count.setTextColor(Color.WHITE);
        addBody(box, getString(R.string.rl_hub_summary_note));
    }

    private void buildScreen(LinearLayout box) {
        addHeading(box, getString(R.string.gcs_gamecenter_menu_screen));
        addAction(box, getString(R.string.rl_hub_system_display), null,
                view -> open(Settings.ACTION_DISPLAY_SETTINGS));
        addAction(box, getString(R.string.rl_hub_brightness), null,
                view -> open(Settings.ACTION_DISPLAY_SETTINGS));
        addAction(box, getString(R.string.rl_hub_timeout), null,
                view -> open(Settings.ACTION_DISPLAY_SETTINGS));
        addSubheading(box, getString(R.string.rl_hub_refresh));
        addBody(box, refreshSummary());
        addBody(box, getString(R.string.rl_hub_refresh_note));
        addAction(box, getString(R.string.rl_hub_rotation), null,
                view -> open(Settings.ACTION_DISPLAY_SETTINGS));
    }

    private void buildNetwork(LinearLayout box) {
        addHeading(box, getString(R.string.gcs_gamecenter_menu_network));
        addBody(box, getString(R.string.rl_hub_network_value, activeNetworkName()));
        addAction(box, getString(R.string.rl_hub_wifi), null,
                view -> open(Settings.ACTION_WIFI_SETTINGS));
        addAction(box, getString(R.string.rl_hub_wireless), null,
                view -> open(Settings.ACTION_WIRELESS_SETTINGS));
        addAction(box, getString(R.string.rl_hub_data), null,
                view -> open(Settings.ACTION_DATA_USAGE_SETTINGS));
    }

    private void buildZen(LinearLayout box) {
        addHeading(box, getString(R.string.gcs_gamecenter_menu_not_disturb));
        NotificationManager manager = getSystemService(NotificationManager.class);
        boolean granted = manager != null && manager.isNotificationPolicyAccessGranted();
        String state = getString(R.string.rl_hub_net_none);
        if (manager != null) {
            state = manager.getCurrentInterruptionFilter() == NotificationManager.INTERRUPTION_FILTER_ALL
                    ? getString(R.string.rl_hub_zen_off)
                    : getString(R.string.rl_hub_zen_on);
        }
        addBody(box, getString(R.string.rl_hub_zen_state, state));

        if (!granted) {
            addAction(box, getString(R.string.rl_hub_zen_grant), null,
                    view -> open(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
        } else {
            addAction(box, getString(R.string.rl_hub_zen_toggle), null, view -> toggleZen());
        }
        addBody(box, getString(R.string.rl_hub_zen_note));
    }

    private void toggleZen() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager == null || !manager.isNotificationPolicyAccessGranted()) {
            return;
        }
        int next = manager.getCurrentInterruptionFilter() == NotificationManager.INTERRUPTION_FILTER_ALL
                ? NotificationManager.INTERRUPTION_FILTER_PRIORITY
                : NotificationManager.INTERRUPTION_FILTER_ALL;
        try {
            manager.setInterruptionFilter(next);
        } catch (SecurityException denied) {
            open(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
        }
        renderCurrent();
    }

    private void buildWallpaper(LinearLayout box) {
        addHeading(box, getString(R.string.gcs_gamecenter_menu_game_wallpaper));

        ImageView preview = new ImageView(this);
        preview.setImageResource(R.mipmap.wallpaper_0);
        preview.setAdjustViewBounds(true);
        preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        box.addView(preview, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(210)));
        addGap(box, 14);

        addAction(box, getString(R.string.rl_hub_wallpaper_set), null, view -> applyWallpaper());
        addAction(box, getString(R.string.rl_hub_wallpaper_choose), null, view -> {
            Intent intent = new Intent(Intent.ACTION_SET_WALLPAPER);
            openIntent(intent);
        });
        addBody(box, getString(R.string.rl_hub_wallpaper_note));
    }

    private void applyWallpaper() {
        Toast.makeText(this, R.string.rl_hub_wallpaper_failed, Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            boolean ok;
            try {
                android.app.WallpaperManager manager = android.app.WallpaperManager.getInstance(this);
                manager.setBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.wallpaper_0));
                ok = true;
            } catch (IOException | SecurityException | OutOfMemoryError failed) {
                ok = false;
            }
            boolean result = ok;
            runOnUiThread(() -> Toast.makeText(
                    this,
                    result ? R.string.rl_hub_wallpaper_done : R.string.rl_hub_wallpaper_failed,
                    Toast.LENGTH_SHORT).show());
        }, "set-wallpaper").start();
    }

    private void buildOther(LinearLayout box) {
        addHeading(box, getString(R.string.gcs_gamecenter_menu_other_options));
        addAction(box, getString(R.string.rl_hub_notifications), null, view ->
                openIntent(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName())));
        addAction(box, getString(R.string.rl_hub_app_info), null, view ->
                openIntent(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.parse("package:" + getPackageName()))));
        addAction(box, getString(R.string.rl_hub_accessibility), null,
                view -> open(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        addAction(box, getString(R.string.rl_hub_dev_options), null,
                view -> open(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
        addGap(box, 10);
        addSubheading(box, getString(R.string.rl_hub_disabled_title));
        addBody(box, getString(R.string.rl_hub_disabled_body));
    }

    private void buildAbout(LinearLayout box) {
        addHeading(box, getString(R.string.gcs_gamecenter_menu_about));
        String version = "?";
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            if (info.versionName != null) {
                version = info.versionName;
            }
        } catch (PackageManager.NameNotFoundException ignored) {
            // The package is this process's package; the fallback only covers a broken install.
        }
        addBody(box, getString(R.string.rl_hub_about_body, getPackageName(), version));
        addAction(box, getString(R.string.rl_hub_app_info), null, view ->
                openIntent(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.parse("package:" + getPackageName()))));
        addGap(box, 10);
        addSubheading(box, getString(R.string.rl_hub_disabled_title));
        addBody(box, getString(R.string.rl_hub_disabled_body));
    }

    private String refreshSummary() {
        Display display = getDisplay();
        if (display == null) {
            return getString(R.string.rl_hub_refresh_value, "?");
        }
        Display.Mode current = display.getMode();
        String now = formatMode(current);
        Set<String> modes = new LinkedHashSet<>();
        for (Display.Mode mode : display.getSupportedModes()) {
            modes.add(formatMode(mode));
        }
        String list = String.join(" · ", modes);
        return getString(R.string.rl_hub_refresh_value, now)
                + "\n" + getString(R.string.rl_hub_refresh_modes, list);
    }

    private static String formatMode(Display.Mode mode) {
        return String.format(Locale.US, "%dx%d@%dHz",
                mode.getPhysicalWidth(), mode.getPhysicalHeight(), Math.round(mode.getRefreshRate()));
    }

    private String activeNetworkName() {
        ConnectivityManager manager = getSystemService(ConnectivityManager.class);
        if (manager == null) {
            return getString(R.string.rl_hub_net_none);
        }
        Network network = manager.getActiveNetwork();
        NetworkCapabilities caps = network == null ? null : manager.getNetworkCapabilities(network);
        if (caps == null) {
            return getString(R.string.rl_hub_net_none);
        }
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            return getString(R.string.rl_hub_net_wifi);
        }
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            return getString(R.string.rl_hub_net_mobile);
        }
        return getString(R.string.rl_hub_net_other);
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
            Toast.makeText(this, R.string.rl_hub_disabled_title, Toast.LENGTH_SHORT).show();
        }
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

    private enum Kind {
        SUMMARY,
        SCREEN,
        NETWORK,
        ZEN,
        WALLPAPER,
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
