package dev.redlab.gamespace;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import dev.redlab.gamespace.recycler.BannerRecyclerView;
import dev.redlab.gamespace.view.MarqueeTextView;
import dev.redlab.gamespace.view.GamePlayLayout;
import dev.redlab.gamespace.view.SelectedScaleView;

/**
 * The original lobby shape, filled with the apps this phone can launch.
 *
 * <p>It inflates the original {@code game_lobby_layout} verbatim and uses the lobby's full banner
 * mode: a vertical {@link LinearLayoutManager} over the original {@code game_item_full} cards.
 * The banner geometry is the measured original one ({@code BannerManager.updateParamsWithNormal}
 * with full mode on): 1700 px wide, 144 px from the top, 16 px from the left, pinned from top to
 * bottom of the parent. The original stream feature is absent on a stock phone, so the measured
 * 15 px bottom margin becomes 0, exactly as that method computes it.
 *
 * <p>The play slot is the owned {@link GamePlayLayout}, and the card is the owned
 * {@link SelectedScaleView}; both keep the original resources and need no Nubia service. No layout
 * file is edited.
 */
public final class GameListActivity extends Activity {

    /** Measured in BannerManager.updateParamsWithNormal(true). */
    private static final int BANNER_WIDTH_PX = 1700;
    private static final int BANNER_TOP_MARGIN_PX = 144;
    private static final int BANNER_LEFT_MARGIN_PX = 16;

    private final List<GameLibrary.App> entries = new ArrayList<>();
    private GameAdapter adapter;
    /** Lobby content that gives way to the planet strip while the second tab is open. */
    private static final int[] LOBBY_CONTENT = {
            R.id.game_list,
            R.id.lobby_game_name,
            R.id.lobby_play_view,
            R.id.lobby_3a,
            R.id.lobby_gift_bag,
            R.id.red_magic_point,
            R.id.lobby_control,
            R.id.lobby_custom,
            R.id.lobby_notes,
            R.id.lobby_red_time
    };

    /** The planet strip cards, in the order the vendor's layout inflates them. */
    private static final int[] PLANET_CARDS = {
            R.id.game_agent_layout,
            R.id.x_gravitation_layout,
            R.id.plug_unit_parent_layout,
            R.id.international_plugin_unit_layout,
            R.id.moji_broadcast_layout,
            R.id.learned_behavior_layout,
            R.id.multi_screen_layout
    };

    private TextView selectedName;
    private int selectedPosition = -1;
    private GameLibrary.App selected;
    /** Index of the first child the two original bars add to the lobby root. */
    private int barsStart = -1;
    private View planetPanel;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setBackgroundDrawableResource(R.mipmap.wallpaper_0);
        getWindow().setFlags(
                android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,
                android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
        PortLayoutInflater.install(getLayoutInflater());
        setContentView(R.layout.game_lobby_layout);
        hideSystemBars();
        setTitle(R.string.game_space_app_name);

        selectedName = findViewById(R.id.lobby_game_name);

        BannerRecyclerView list = findViewById(R.id.game_list);
        list.setAlpha(1f);
        list.setHasFixedSize(true);
        list.setLayoutManager(new LinearLayoutManager(this));
        // The XML card stops above lobby_3a for the card-track mode; full mode pins the list to the
        // parent instead, so replace the inflated params with the measured ones.
        ConstraintLayout.LayoutParams params =
                new ConstraintLayout.LayoutParams(BANNER_WIDTH_PX, 0);
        params.leftToLeft = 0;
        params.topToTop = 0;
        params.bottomToBottom = 0;
        params.leftMargin = BANNER_LEFT_MARGIN_PX;
        params.topMargin = BANNER_TOP_MARGIN_PX;
        params.bottomMargin = 0;
        list.setLayoutParams(params);

        adapter = new GameAdapter();
        list.setAdapter(adapter);

        GamePlayLayout play = findViewById(R.id.lobby_play_view);
        play.setOnClickListener(view -> openSelected());

        // Owner rule: no element is hidden for lacking a vendor service. The 3A/PC track and the
        // gift bag stay visible, and answer with the measured reason instead of disappearing.
        View track3a = findViewById(R.id.lobby_3a);
        track3a.setVisibility(View.VISIBLE);
        track3a.setOnClickListener(view -> {
            vendorToast("rl_vendor_3a");
            openCenter(GameCenterActivity.SECTION_STREAMING);
        });
        View gift = findViewById(R.id.lobby_gift_bag);
        gift.setAlpha(1f);
        gift.setOnClickListener(view -> vendorToast("rl_vendor_gift"));
        findViewById(R.id.red_magic_point).setAlpha(1f);
        // The vendor control panel and atmosphere editor are absent. Their universal substitutes
        // are the Game Space Center and its wallpaper section, reached from the same original
        // controls so the lobby keeps its shape.
        findViewById(R.id.lobby_control).setOnClickListener(view -> openCenter(null));
        // The gear next to it is the vendor's settings entry, not the wallpaper shortcut: the
        // wallpaper has its own card in the center. Opening the center's default category keeps
        // the gear a settings button instead of a second wallpaper button.
        findViewById(R.id.lobby_custom).setOnClickListener(view -> openCenter(null));
        // These three icons address Nubia services that are not installed. They stay in place and
        // answer with the measured reason instead of doing nothing.
        findViewById(R.id.lobby_notes).setOnClickListener(view ->
                vendorToast("rl_vendor_notes"));
        findViewById(R.id.lobby_red_time).setOnClickListener(view ->
                vendorToast("rl_vendor_red_time"));

        inflateUpperBar();
        inflateLowerBar();
        tuneBottomTabs();
        selectTab(false);
        addLibraryButton();
        loadLauncherApps();
    }

    /**
     * The two bottom tabs, sized from their own labels.
     *
     * <p>The stock XML pads them with raw pixels (195px and 160px) to clear the icon that is baked
     * into their background art. On this phone those numbers left the second tab overlapping the
     * first and clipped the Arabic label, so the padding is set from density and the max width is
     * released. The labels are short; they fit.
     */
    private void tuneBottomTabs() {
        float density = getResources().getDisplayMetrics().density;
        // The stock planet label is the feature's full name ("power base" in Arabic), which is
        // wider than the tab the vendor shows. The reference tab carries the short product name,
        // so use that here and keep the full name where the strip itself is described.
        View strip = findViewById(R.id.tab_red_magic_planet);
        int shortName = getResources().getIdentifier("rl_tab_planet", "string", getPackageName());
        if (strip instanceof TextView && shortName != 0) {
            ((TextView) strip).setText(shortName);
        }
        tuneTab(R.id.tab_game_lobby, Math.round(150f * density / 3.25f));
        tuneTab(R.id.tab_red_magic_planet, Math.round(120f * density / 3.25f));
        View lobby = findViewById(R.id.tab_game_lobby);
        if (lobby != null && strip != null) {
            lobby.post(() -> separateTabs(lobby, strip, Math.round(8f * density)));
        }
    }

    /**
     * The two tab labels sit in one packed chain. If a translation is still wide enough to push
     * them into each other, give the second label less icon clearance instead of letting the
     * labels overprint. The clearance is only ever reduced, never the text itself.
     */
    private void separateTabs(View lobby, View strip, int gap) {
        int overlap = lobby.getRight() + gap - strip.getLeft();
        if (overlap <= 0) {
            return;
        }
        int clearance = Math.max(0, strip.getPaddingLeft() - overlap);
        strip.setPadding(clearance, strip.getPaddingTop(),
                strip.getPaddingRight(), strip.getPaddingBottom());
    }

    private void tuneTab(int id, int iconClearance) {
        View tab = findViewById(id);
        if (!(tab instanceof TextView)) {
            return;
        }
        TextView label = (TextView) tab;
        label.setMaxWidth(Integer.MAX_VALUE);
        label.setSingleLine(true);
        label.setEllipsize(TextUtils.TruncateAt.END);
        if (label instanceof MarqueeTextView) {
            ((MarqueeTextView) label).setSelected(false);
        }
        int end = Math.round(12f * getResources().getDisplayMetrics().density);
        label.setPadding(iconClearance, 0, end, 0);
    }

    /**
     * The original upper bar: the RedMagic mark, a live clock, a live battery view and the stock
     * buttons. It is the vendor's own {@code upper_view} layout, inflated through the same tag
     * redirect as the lobby, so nothing in it is redrawn or re-styled here.
     *
     * <p>Inflation is guarded: this bar is an addition to a lobby that already works, and a bar
     * that cannot be created must not take the game list down with it.
     */
    private void inflateUpperBar() {
        ViewGroup content = findViewById(android.R.id.content);
        if (content == null || content.getChildCount() == 0
                || !(content.getChildAt(0) instanceof ViewGroup)) {
            return;
        }
        ViewGroup root = (ViewGroup) content.getChildAt(0);
        if (barsStart < 0) {
            barsStart = root.getChildCount();
        }
        try {
            getLayoutInflater().inflate(R.layout.upper_view, root, true);
        } catch (RuntimeException | LinkageError unsupported) {
            android.util.Log.w("GameList", "upper bar unavailable", unsupported);
            return;
        }
        bindUpperBar(root);
    }

    private void bindUpperBar(ViewGroup root) {
        View news = root.findViewById(R.id.top_news);
        if (news != null) {
            news.setOnClickListener(view -> vendorToast("rl_vendor_news"));
        }
        View detector = root.findViewById(R.id.top_detector);
        if (detector != null) {
            detector.setOnClickListener(view -> vendorToast("rl_vendor_detector"));
        }
        View score = root.findViewById(R.id.top_score);
        if (score != null) {
            score.setOnClickListener(view -> openCenter(GameCenterActivity.SECTION_RECORDS));
        }
        View mora = root.findViewById(R.id.top_mora);
        if (mora != null) {
            mora.setOnClickListener(view -> vendorToast("rl_vendor_mora"));
        }
        View recommend = root.findViewById(R.id.game_recommend);
        if (recommend != null) {
            recommend.setOnClickListener(view -> vendorToast("rl_vendor_recommend"));
        }
        View extern = root.findViewById(R.id.extern_device);
        if (extern != null) {
            // Pairing an external controller or headset is a standard Android task.
            extern.setOnClickListener(view -> openSystem(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS));
        }
        View profile = root.findViewById(R.id.personal_center);
        if (profile != null) {
            profile.setOnClickListener(view -> openCenter(GameCenterActivity.SECTION_PROFILE));
        }
        View shortcut = root.findViewById(R.id.add_shortcut);
        if (shortcut != null) {
            shortcut.setOnClickListener(view -> pinSelectedShortcut());
        }
        // play_center is hidden by the vendor build itself (GameLobbyFragment line 204), so it
        // keeps that measured state instead of becoming a dead button here.
        View playCenter = root.findViewById(R.id.play_center);
        if (playCenter != null) {
            playCenter.setVisibility(View.GONE);
        }
    }

    /**
     * The stock "+" from the lobby: opens the original Add Games screen.
     *
     * <p>The vendor draws it with {@code mipmap/game_lobby_add} above the 3A track. It is added
     * programmatically rather than by editing {@code game_lobby_layout.xml}, because the materials
     * are the original resources and are never rewritten; the real icon and the same corner are
     * used.
     */
    private void addLibraryButton() {
        ViewGroup content = findViewById(android.R.id.content);
        if (content == null || content.getChildCount() == 0
                || !(content.getChildAt(0) instanceof ConstraintLayout)) {
            return;
        }
        ConstraintLayout root = (ConstraintLayout) content.getChildAt(0);
        ImageView add = new ImageView(this);
        add.setImageResource(R.mipmap.game_lobby_add);
        add.setContentDescription(getString(R.string.add_game));
        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT);
        params.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID;
        params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
        params.leftMargin = Math.round(26f * getResources().getDisplayMetrics().density);
        params.bottomMargin = Math.round(72f * getResources().getDisplayMetrics().density);
        add.setLayoutParams(params);
        add.setOnClickListener(view -> startActivity(new Intent(this, AppAddActivity.class)));
        root.addView(add);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // The library may have changed in the Add Games screen; the lobby reloads on return.
        if (adapter != null) {
            loadLauncherApps();
        }
    }

    /**
     * The original lower bar: the fan / BGM / display / performance buttons and the lobby tabs.
     *
     * <p>This is the vendor's own {@code lower_view}, inflated through the same tag redirect as
     * the rest of the lobby, so the bottom of the screen keeps its original shape. The controls
     * that address Nubia hardware services answer with the measured reason instead of becoming a
     * dead icon.
     */
    private void inflateLowerBar() {
        ViewGroup content = findViewById(android.R.id.content);
        if (content == null || content.getChildCount() == 0
                || !(content.getChildAt(0) instanceof ViewGroup)) {
            return;
        }
        ViewGroup root = (ViewGroup) content.getChildAt(0);
        // Scoped search: lower_view reuses ids the lobby already has (switch_view is also the
        // chevron inside game_play_layout). Only the children this inflation adds are ours.
        int first = root.getChildCount();
        try {
            getLayoutInflater().inflate(R.layout.lower_view, root, true);
        } catch (RuntimeException | LinkageError unsupported) {
            android.util.Log.w("GameList", "lower bar unavailable", unsupported);
            return;
        }
        // The support/top-game tab strip and the handheld tips belong to the vendor's handheld
        // mode. The stock lobby shows the two pill tabs, so keep those and hide the alternatives.
        hide(root, first, R.id.tab_view_support_top_game);
        hide(root, first, R.id.handheld_tips);
        hide(root, first, R.id.one_more_thing_in);
        hide(root, first, R.id.one_more_thing_in_support_top_game);

        // The three lower buttons are sized by layout_constraintWidth_percent in the original
        // layout, which only applies when the default is explicitly "percent". The vendor build
        // got that from its own ConstraintLayout version; measured here, the views collapsed to
        // zero width and the bar looked empty. Give them the dimension the layout already names
        // for their focus backgrounds, and keep every original constraint in place.
        int button = getResources().getDimensionPixelSize(R.dimen.lower_button_size);
        size(root, first, R.id.fan_anim_view, button);
        size(root, first, R.id.bgm_switch, button);
        size(root, first, R.id.switch_view, button);
        size(root, first, R.id.mode_view, button);

        // The vendor build gets these compositions from the same XML attribute. On this build the
        // attribute was measured not to reach Lottie (the views laid out at full size and drew
        // nothing), so load each composition explicitly from the original asset and show its
        // first frame. The files themselves are the original ones in materials/assets/lottie.
        play(root, first, R.id.fan_anim_view, "lottie/fan_close.json");
        play(root, first, R.id.bgm_switch, "lottie/bgm_on.json");
        play(root, first, R.id.switch_view, "lottie/switch_full.json");
        play(root, first, R.id.mode_view, "lottie/mode_normal.json");

        // The fan and performance buttons open the center's real readings (SoC and battery
        // temperature, the power modes this device reports) instead of only naming the missing
        // vendor service. The sliders button is the vendor's route into the settings list, which
        // this port serves with the Game Space Center.
        // Background music is the device's media volume, so the stock music button opens the
        // sound page instead of naming a service that does not exist here.
        View bgm = find(root, first, R.id.bgm_switch);
        if (bgm != null) {
            bgm.setOnClickListener(view ->
                    openSystem(android.provider.Settings.ACTION_SOUND_SETTINGS));
        }
        bindCenter(root, first, R.id.fan_anim_view, GameCenterActivity.SECTION_FAN);
        bindCenter(root, first, R.id.mode_view, GameCenterActivity.SECTION_POWER);
        bindCenter(root, first, R.id.switch_view, null);
    }

    private View find(ViewGroup root, int first, int id) {
        for (int i = first; i < root.getChildCount(); i++) {
            View found = root.getChildAt(i).findViewById(id);
            if (found != null) {
                return found;
            }
            if (root.getChildAt(i).getId() == id) {
                return root.getChildAt(i);
            }
        }
        return null;
    }

    private void play(ViewGroup root, int first, int id, String asset) {
        View view = find(root, first, id);
        if (!(view instanceof com.airbnb.lottie.LottieAnimationView)) {
            return;
        }
        try {
            com.airbnb.lottie.LottieAnimationView lottie =
                    (com.airbnb.lottie.LottieAnimationView) view;
            lottie.setAnimation(asset);
            // These are state icons, not loops. Playing them repeated made the fan/BGM/display
            // buttons blink on and off forever; show the finished frame and hold it.
            lottie.setRepeatCount(0);
            lottie.cancelAnimation();
            lottie.setProgress(1f);
        } catch (RuntimeException missing) {
            android.util.Log.w("GameList", "cannot load " + asset, missing);
        }
    }

    private void size(ViewGroup root, int first, int id, int pixels) {
        View view = find(root, first, id);
        if (view == null) {
            return;
        }
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.width = pixels;
        params.height = pixels;
        view.setLayoutParams(params);
    }

    private void hide(ViewGroup root, int first, int id) {
        View view = find(root, first, id);
        if (view != null) {
            view.setVisibility(View.GONE);
        }
    }

    private void bindClick(ViewGroup root, int first, int id, String message) {
        View view = find(root, first, id);
        if (view != null) {
            view.setOnClickListener(v -> vendorToast(message));
        }
    }

    private void bindCenter(ViewGroup root, int first, int id, String section) {
        View view = find(root, first, id);
        if (view != null) {
            view.setOnClickListener(v -> openCenter(section));
        }
    }

    // Android resolves these from the android:onClick attributes in the original lower_view.xml.
    public void switchToGameLobby(View view) {
        hidePlanet();
    }

    public void switchToRegMagicPlanet(View view) {
        showPlanet();
    }

    /**
     * The second tab is the vendor's own planet strip, shown in place instead of in a second
     * activity: in the vendor build both tabs are one screen over one wallpaper, with both bars
     * kept on top. The strip is the original {@code redmagic_planet_layout}, inflated through the
     * same tag redirect as the lobby, so its cards keep their art and order. Nothing is redrawn.
     */
    private void showPlanet() {
        if (planetPanel != null) {
            return;
        }
        ViewGroup content = findViewById(android.R.id.content);
        if (content == null || content.getChildCount() == 0
                || !(content.getChildAt(0) instanceof ViewGroup)) {
            return;
        }
        ViewGroup root = (ViewGroup) content.getChildAt(0);
        android.widget.FrameLayout panel = new android.widget.FrameLayout(this);
        try {
            getLayoutInflater().inflate(R.layout.redmagic_planet_layout, panel, true);
        } catch (RuntimeException | LinkageError unsupported) {
            android.util.Log.w("GameList", "planet strip unavailable", unsupported);
            return;
        }
        // Every card is a real entry: the planet strip is a launcher for the same features the
        // center carries, so each one opens its section instead of naming a missing service.
        String[] targets = {null, GameCenterActivity.SECTION_OTHER,
                GameCenterActivity.SECTION_OTHER, GameCenterActivity.SECTION_OTHER,
                GameCenterActivity.SECTION_MIRROR, GameCenterActivity.SECTION_OTHER,
                GameCenterActivity.SECTION_MIRROR};
        for (int i = 0; i < PLANET_CARDS.length; i++) {
            View card = panel.findViewById(PLANET_CARDS[i]);
            if (card != null) {
                String target = i < targets.length ? targets[i] : null;
                card.setOnClickListener(view -> openCenter(target));
            }
        }
        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        // Insert below the two bars so they keep their taps, and above the lobby content.
        int index = barsStart < 0 ? root.getChildCount() : barsStart;
        root.addView(panel, index, params);
        planetPanel = panel;
        for (int id : LOBBY_CONTENT) {
            View view = root.findViewById(id);
            if (view != null) {
                view.setVisibility(View.INVISIBLE);
            }
        }
        selectTab(true);
    }

    private void hidePlanet() {
        if (planetPanel == null) {
            return;
        }
        ViewGroup root = (ViewGroup) planetPanel.getParent();
        if (root != null) {
            root.removeView(planetPanel);
            for (int id : LOBBY_CONTENT) {
                View view = root.findViewById(id);
                if (view != null) {
                    view.setVisibility(View.VISIBLE);
                }
            }
        }
        planetPanel = null;
        selectTab(false);
    }

    /**
     * The stock tab art is a two-state selector (pressed/normal); the vendor build painted the
     * open tab with its pressed art. Do the same here so the open tab is visible at a glance,
     * including the red planet tab the reference shows.
     */
    private void selectTab(boolean planet) {
        View lobby = findViewById(R.id.tab_game_lobby);
        View strip = findViewById(R.id.tab_red_magic_planet);
        // The stock tab art is a two-state selector (pressed/normal), so the open tab is given a
        // selector built from the same two vendor drawables with a selected state added. The
        // generated port R only carries ids the original public.xml declares, so the drawables
        // are resolved by name instead of a new R field.
        if (lobby != null) {
            lobby.setBackground(tabArt("tab_game_lobby"));
            lobby.setSelected(!planet);
        }
        if (strip != null) {
            strip.setBackground(tabArt("tab_planet"));
            strip.setSelected(planet);
        }
    }

    public void switchToTopGame(View view) {
        vendorToast("rl_vendor_top_game");
    }

    public void backClick(View view) {
        if (planetPanel != null) {
            hidePlanet();
            return;
        }
        finish();
    }

    @Override
    public void onBackPressed() {
        if (planetPanel != null) {
            hidePlanet();
            return;
        }
        super.onBackPressed();
    }

    /** The stock add-shortcut button: a real pinned launcher shortcut for the selected game. */
    private void pinSelectedShortcut() {
        if (selected == null) {
            return;
        }
        android.content.pm.ShortcutManager manager = getSystemService(android.content.pm.ShortcutManager.class);
        if (manager == null || !manager.isRequestPinShortcutSupported()) {
            vendorToast("rl_shortcut_unavailable");
            return;
        }
        Intent launch = getPackageManager().getLaunchIntentForPackage(selected.packageName);
        if (launch == null) {
            return;
        }
        try {
            Context other = createPackageContext(selected.packageName, 0);
            int iconId = getPackageManager().getApplicationInfo(selected.packageName, 0).icon;
            android.content.pm.ShortcutInfo info = new android.content.pm.ShortcutInfo.Builder(
                    this, selected.packageName)
                    .setShortLabel(selected.label)
                    .setLongLabel(selected.label)
                    .setIcon(android.graphics.drawable.Icon.createWithResource(other, iconId))
                    .setIntent(launch)
                    .build();
            manager.requestPinShortcut(info, null);
        } catch (PackageManager.NameNotFoundException | IllegalArgumentException failed) {
            vendorToast("rl_shortcut_unavailable");
        }
    }

    /** The stock tab drawables, plus the selected state the vendor build paints. */
    private android.graphics.drawable.Drawable tabArt(String name) {
        android.graphics.drawable.Drawable press = getDrawableByName(name + "_press");
        android.graphics.drawable.Drawable normal = getDrawableByName(name + "_normal");
        if (press == null || normal == null) {
            return null;
        }
        android.graphics.drawable.StateListDrawable art =
                new android.graphics.drawable.StateListDrawable();
        art.addState(new int[]{android.R.attr.state_selected}, press);
        art.addState(new int[]{android.R.attr.state_pressed}, press);
        art.addState(new int[]{}, normal);
        return art;
    }

    private android.graphics.drawable.Drawable getDrawableByName(String name) {
        int id = getResources().getIdentifier(name, "drawable", getPackageName());
        return id == 0 ? null : getResources().getDrawable(id, getTheme());
    }

    private void openSystem(String action) {
        try {
            startActivity(new Intent(action));
        } catch (ActivityNotFoundException missing) {
            vendorToast("rl_open_failed");
        }
    }

    private void openCenter(String section) {
        Intent intent = new Intent(this, GameCenterActivity.class);
        if (section != null) {
            intent.putExtra(GameCenterActivity.EXTRA_SECTION, section);
        }
        startActivity(intent);
    }

    private void vendorToast(String name) {
        // Owned strings are not members of the sealed original R; resolve the packaged name.
        int message = getResources().getIdentifier(name, "string", getPackageName());
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemBars();
        }
    }

    /** Keeps the original immersive lobby, including after returning from a launched game. */
    private void hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    /** Reads only what the launcher already exposes; no package permission, no vendor call. */
    private void loadLauncherApps() {
        new Thread(() -> {
            List<GameLibrary.App> found = GameLibrary.library(this);
            runOnUiThread(() -> {
                entries.clear();
                entries.addAll(found);
                boolean keep = selected != null && contains(selected.packageName);
                if (entries.isEmpty()) {
                    selectedPosition = -1;
                    selected = null;
                    selectedName.setText("");
                } else if (!keep) {
                    selectedPosition = 0;
                    selected = entries.get(0);
                    selectedName.setText(selected.label);
                } else {
                    selectedPosition = indexOf(selected.packageName);
                }
                adapter.notifyDataSetChanged();
            });
        }, "game-list").start();
    }

    private boolean contains(String packageName) {
        for (GameLibrary.App app : entries) {
            if (app.packageName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    private int indexOf(String packageName) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).packageName.equals(packageName)) {
                return i;
            }
        }
        return 0;
    }

    /** The original adapter selects through the card position; the selected card does not launch. */
    private void select(int position) {
        if (position < 0 || position >= entries.size() || position == selectedPosition) {
            return;
        }
        int previous = selectedPosition;
        selectedPosition = position;
        selected = entries.get(position);
        selectedName.setText(selected.label);
        if (previous >= 0) {
            adapter.notifyItemChanged(previous);
        }
        adapter.notifyItemChanged(position);
    }

    private void openSelected() {
        if (selected != null) {
            open(selected.packageName);
        }
    }

    private void open(String packageName) {
        Intent launch = getPackageManager().getLaunchIntentForPackage(packageName);
        if (launch == null) {
            return;
        }
        try {
            startActivity(launch);
        } catch (ActivityNotFoundException gone) {
            // The package was uninstalled between the scan and the tap.
        }
    }

    private final class GameAdapter extends RecyclerView.Adapter<GameHolder> {

        @Override
        public GameHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View card = getLayoutInflater().inflate(R.layout.game_item_full, parent, false);
            return new GameHolder(card);
        }

        @Override
        public void onBindViewHolder(GameHolder holder, int position) {
            GameLibrary.App entry = entries.get(position);
            holder.icon.setImageDrawable(entry.icon);
            holder.name.setText(entry.label);
            holder.card.setSupportProperty(false);
            // setType() leaves the view's own selection flag untouched, so re-selecting a card
            // after another was chosen would not restore its frame. Drive the real selected state.
            holder.card.setSelected(position == selectedPosition);

            holder.itemView.setOnClickListener(view -> {
                int current = holder.getBindingAdapterPosition();
                if (current != RecyclerView.NO_POSITION) {
                    select(current);
                }
            });
            holder.itemView.setOnLongClickListener(view -> {
                int current = holder.getBindingAdapterPosition();
                if (current != RecyclerView.NO_POSITION) {
                    select(current);
                    open(entries.get(current).packageName);
                }
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return entries.size();
        }
    }

    private static final class GameHolder extends RecyclerView.ViewHolder {
        final SelectedScaleView card;
        final ImageView icon;
        final TextView name;

        GameHolder(View card) {
            super(card);
            this.card = (SelectedScaleView) card;
            this.icon = card.findViewById(R.id.icon);
            this.name = card.findViewById(R.id.full_item_game_name);
            // A launchable app has no download progress; the original adapter hides this view for
            // every item that is not a download.
            card.findViewById(R.id.icon_progress).setVisibility(View.GONE);
        }
    }
}
