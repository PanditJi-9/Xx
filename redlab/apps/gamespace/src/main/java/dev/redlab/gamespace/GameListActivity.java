package dev.redlab.gamespace;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dev.redlab.gamespace.recycler.BannerRecyclerView;
import dev.redlab.gamespace.view.GamePlayLayout;

/**
 * The original lobby shape, filled with the apps this phone can launch.
 *
 * <p>It inflates the original {@code game_lobby_layout} verbatim: background, title, side
 * controls, Lottie frame, {@link BannerRecyclerView} and the play slot all come from the RedMagic
 * resources. Only the play slot is replaced by {@link GamePlayLayout}, which needs no Nubia
 * service. No layout file is edited.
 */
public final class GameListActivity extends Activity {

    private final List<Entry> entries = new ArrayList<>();
    private GameAdapter adapter;
    private TextView selectedName;
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
        setTitle(R.string.game_space_app_name);

        selectedName = findViewById(R.id.lobby_game_name);

        BannerRecyclerView list = findViewById(R.id.game_list);
        list.setAlpha(1f);
        list.setLayoutManager(new GridLayoutManager(this, 4));
        adapter = new GameAdapter();
        list.setAdapter(adapter);

        GamePlayLayout play = findViewById(R.id.lobby_play_view);
        play.setOnClickListener(view -> openSelected());

        loadLauncherApps();
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
                adapter.notifyDataSetChanged();
                if (!entries.isEmpty()) {
                    select(entries.get(0));
                }
            });
        }, "game-list").start();
    }

    private void select(Entry entry) {
        selected = entry;
        selectedName.setText(entry.label);
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
            View card = getLayoutInflater().inflate(R.layout.game_item_medium, parent, false);
            return new GameHolder(card);
        }

        @Override
        public void onBindViewHolder(GameHolder holder, int position) {
            Entry entry = entries.get(position);
            holder.label.setText(entry.label);
            holder.label.setVisibility(View.VISIBLE);
            holder.icon.setImageDrawable(entry.icon);
            holder.icon.setVisibility(View.VISIBLE);
            holder.itemView.setOnClickListener(view -> select(entry));
            holder.itemView.setOnLongClickListener(view -> {
                select(entry);
                open(entry.packageName);
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return entries.size();
        }
    }

    private static final class GameHolder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView label;

        GameHolder(View card) {
            super(card);
            // The original card carries a banner slot and a glossy mask meant for downloaded
            // artwork; a launchable app has only an icon, so those two stay hidden and the icon
            // fills the slot. The card itself keeps the original size, colour and corner radius.
            card.findViewById(R.id.game_banner).setVisibility(View.GONE);
            card.findViewById(R.id.card_mask).setVisibility(View.GONE);
            icon = card.findViewById(R.id.icon);
            label = card.findViewById(R.id.game_name);
        }
    }
}
