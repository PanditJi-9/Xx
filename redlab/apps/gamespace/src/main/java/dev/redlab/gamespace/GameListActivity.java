package dev.redlab.gamespace;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dev.redlab.gamespace.recycler.BannerRecyclerView;
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

    private final List<Entry> entries = new ArrayList<>();
    private GameAdapter adapter;
    private TextView selectedName;
    private int selectedPosition = -1;
    private Entry selected;

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

        loadLauncherApps();
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
            PackageManager manager = getPackageManager();
            Intent launcher = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> resolved = manager.queryIntentActivities(launcher, 0);
            List<Entry> found = new ArrayList<>();
            for (ResolveInfo info : resolved) {
                if (info.activityInfo == null) {
                    continue;
                }
                String packageName = info.activityInfo.packageName;
                if (getPackageName().equals(packageName)) {
                    continue;
                }
                CharSequence label = info.loadLabel(manager);
                Drawable icon = info.loadIcon(manager);
                found.add(new Entry(label == null ? packageName : label.toString(), packageName, icon));
            }
            Collections.sort(found, (a, b) -> a.label.compareToIgnoreCase(b.label));
            runOnUiThread(() -> {
                entries.clear();
                entries.addAll(found);
                if (entries.isEmpty()) {
                    selectedPosition = -1;
                    selected = null;
                } else {
                    selectedPosition = 0;
                    selected = entries.get(0);
                    selectedName.setText(selected.label);
                }
                adapter.notifyDataSetChanged();
            });
        }, "game-list").start();
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

    private static final class Entry {
        final String label;
        final String packageName;
        final Drawable icon;

        Entry(String label, String packageName, Drawable icon) {
            this.label = label;
            this.packageName = packageName;
            this.icon = icon;
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
            Entry entry = entries.get(position);
            holder.icon.setImageDrawable(entry.icon);
            holder.name.setText(entry.label);
            holder.card.setSupportProperty(false);
            holder.card.setType(position == selectedPosition ? 0 : 1);

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
