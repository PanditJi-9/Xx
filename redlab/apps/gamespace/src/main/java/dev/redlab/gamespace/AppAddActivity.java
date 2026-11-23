package dev.redlab.gamespace;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * The original "Add Games" screen.
 *
 * <p>It inflates the vendor's own {@code app_add_layout}, {@code app_add_titlebar_layout} and
 * {@code app_add_item_layout} unchanged, so the list, its switches and its title bar are the stock
 * ones. What it adds is the rule the vendor kept in its account-backed library: a game is in the
 * Game Space automatically, anything else is included or excluded by hand, and the choice is
 * stored on this phone.
 */
public final class AppAddActivity extends Activity {

    private static final float ON = 1.0f;
    private static final float OFF = 0.35f;

    private List<GameLibrary.App> apps;
    private Adapter adapter;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.app_add_layout);
        setTitle(R.string.add_game_title);

        View back = findViewById(R.id.left_icon);
        if (back != null) {
            back.setOnClickListener(view -> finish());
        }

        RecyclerView list = findViewById(R.id.app_add_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setHasFixedSize(true);
        apps = GameLibrary.candidates(this);
        adapter = new Adapter();
        list.setAdapter(adapter);
    }

    private final class Adapter extends RecyclerView.Adapter<Holder> {

        @Override
        public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new Holder(getLayoutInflater()
                    .inflate(R.layout.app_add_item_layout, parent, false));
        }

        @Override
        public void onBindViewHolder(Holder holder, int position) {
            GameLibrary.App app = apps.get(position);
            holder.icon.setImageDrawable(app.icon);
            holder.name.setText(app.label);
            boolean included = GameLibrary.isIncluded(AppAddActivity.this,
                    app.packageName, app.game);
            holder.toggle.setAlpha(included ? ON : OFF);
            holder.toggle.setContentDescription(getString(
                    included ? R.string.remove_switch : R.string.join_switch));
            holder.toggle.setOnClickListener(view -> {
                boolean now = !GameLibrary.isIncluded(AppAddActivity.this,
                        app.packageName, app.game);
                GameLibrary.setIncluded(AppAddActivity.this,
                        app.packageName, app.game, now);
                notifyItemChanged(position);
            });
            holder.itemView.setOnClickListener(view ->
                    holder.toggle.performClick());
        }

        @Override
        public int getItemCount() {
            return apps.size();
        }
    }

    private static final class Holder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView name;
        final ImageView toggle;
        final View radio;

        Holder(View item) {
            super(item);
            icon = item.findViewById(R.id.app_icon);
            name = item.findViewById(R.id.game_space_app_name);
            toggle = item.findViewById(R.id.app_switch);
            radio = item.findViewById(R.id.app_radio_btn);
            // The stock item carries two controls for two different vendor screens; this screen
            // uses the switch and keeps the radio slot out of the way instead of showing two
            // toggles for one choice.
            radio.setVisibility(View.GONE);
        }
    }
}
