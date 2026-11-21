package dev.redlab.gamespace;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * The first screen of the port, and deliberately a small one.
 *
 * <p>It renders what the port measured — the original package, how much material was staged, how
 * many components the original declares, and how the code triage split — because that is the only
 * claim this build can support. The original lobby replaces it once its classes compile; there is
 * no point writing a look-alike lobby that would have to be thrown away.
 *
 * <p>Java, not Kotlin: the original is Java (docs/MASTER_PLAN.md §1 — the language follows the
 * original, not a preference of ours).
 */
public final class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(24);
        column.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText(text("rl_app_name") + " — " + text("rl_port_title"));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        column.addView(title);

        JSONObject inventory = readInventory();
        if (inventory == null) {
            column.addView(line(text("rl_materials_missing"), null));
        } else {
            renderInventory(column, inventory);
        }

        TextView note = line(text("rl_build_note"), null);
        note.setPadding(0, dp(24), 0, 0);
        column.addView(note);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(column, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scroll);
    }

    private void renderInventory(LinearLayout column, JSONObject inventory) {
        JSONObject staged = inventory.optJSONObject("staged");
        JSONObject components = inventory.optJSONObject("components");
        JSONObject triage = inventory.optJSONObject("triage");

        column.addView(line(text("rl_label_package"), inventory.optString("package", "—")));

        if (staged != null) {
            String value = "res " + staged.optInt("res") + " · assets " + staged.optInt("assets");
            org.json.JSONArray abis = staged.optJSONArray("abis");
            for (int i = 0; abis != null && i < abis.length(); i++) {
                JSONObject abi = abis.optJSONObject(i);
                if (abi != null) {
                    value += " · " + abi.optString("abi") + " " + abi.optInt("libs");
                }
            }
            column.addView(line(text("rl_label_materials"), value));
        }

        if (components != null) {
            StringBuilder value = new StringBuilder();
            String[] keys = {"activities", "aliases", "services", "receivers", "providers"};
            for (String key : keys) {
                if (value.length() > 0) {
                    value.append(" · ");
                }
                value.append(key).append(' ').append(components.optJSONArray(key) == null
                        ? 0 : components.optJSONArray(key).length());
            }
            column.addView(line(text("rl_label_components"), value.toString()));
        }

        if (triage != null) {
            String value = "as-is " + triage.optInt("PORT_AS_IS")
                    + " · adapt " + triage.optInt("ADAPT")
                    + " · reimplement " + triage.optInt("REIMPLEMENT")
                    + " (of " + triage.optInt("total") + ")";
            column.addView(line(text("rl_label_triage"), value));
        }
    }

    private TextView line(String label, String value) {
        TextView view = new TextView(this);
        view.setText(value == null ? label : label + "\n" + value);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        view.setPadding(0, dp(14), 0, 0);
        view.setGravity(Gravity.START);
        return view;
    }

    /** Reads the staged inventory; a build without materials is a valid build, not an error. */
    /**
     * Why by name and not `R.string`: the ported sources carry their own decompiled `R` (the only
     * place the original's `styleable` constants survive), so `R.string` here would resolve to that
     * table instead of the one aapt generates from our resources. The generated table still exists;
     * we just ask for it explicitly.
     */
    private String text(String name) {
        int id = getResources().getIdentifier(name, "string", getPackageName());
        return id == 0 ? name : getString(id);
    }

    private JSONObject readInventory() {
        try (InputStream in = getAssets().open("port/inventory.json")) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return new JSONObject(out.toString(StandardCharsets.UTF_8.name()));
        } catch (Exception missing) {
            return null;
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
