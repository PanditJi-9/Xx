package dev.redlab.gamespace;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.WallpaperManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.XmlResourceParser;
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
import android.widget.ToggleButton;

import org.xmlpull.v1.XmlPullParser;

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
 * category icons, strings and colours, and renders each section from the original
 * {@code res/xml/gcs_*} preference screen, so the rows, order and wording are the stock ones.
 * A row does real work through a standard Android API where one exists; a hardware-only row
 * keeps its stock appearance and says once, when touched, that it needs the RedMagic hardware.
 */
public final class GameCenterActivity extends Activity {

    /** Opened by the lobby wallpaper control; selects that category instead of the first one. */
    public static final String EXTRA_SECTION = "dev.redlab.gamespace.extra.SECTION";
    public static final String SECTION_WALLPAPER = "wallpaper";

    private static final String PREFS = "game_space_center";
    private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";
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
        addCategory(R.drawable.competitive_lamp, R.string.gamekey_lamp_switch, Kind.LAMP);
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
            case LAMP:
                renderOriginalScreen(box, R.xml.gcs_gamekey_lamp_settings);
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
        TextView count = addBody(box, getString(stringId("rl_hub_games_count"), countLaunchable()));
        count.setTextColor(Color.WHITE);
        addBody(box, getString(stringId("rl_hub_summary_note")));
    }

    private void buildScreen(LinearLayout box) {
        renderOriginalScreen(box, R.xml.gcs_screen_settings);
    }

    private void buildNetwork(LinearLayout box) {
        renderOriginalScreen(box, R.xml.gcs_net_settings);
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
            if (applyKey(key, next)) {
                toggle.setChecked(next);
                getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(key, next).apply();
            } else {
                hardwareOnly();
            }
        });
        box.addView(row);
    }

    private void addPreferenceRow(LinearLayout box, String key, String title, String summary) {
        View row = getLayoutInflater().inflate(R.layout.gcs_preference, box, false);
        bindRowText(row, title, summary);
        row.setOnClickListener(view -> {
            if (!applyAction(key)) {
                hardwareOnly();
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
        return false;
    }

    private boolean applyAction(String key) {
        if ("gcs_privacy_policy".equals(key)) {
            openIntent(new Intent(Intent.ACTION_VIEW,
                    Uri.parse(getString(R.string.zte_privacy_policy_url))));
            return true;
        }
        if ("gcs_system_update".equals(key)) {
            Toast.makeText(this, stringId("rl_hub_up_to_date"), Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
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

    private enum Kind {
        SUMMARY,
        SCREEN,
        NETWORK,
        ZEN,
        WALLPAPER,
        LAMP,
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
