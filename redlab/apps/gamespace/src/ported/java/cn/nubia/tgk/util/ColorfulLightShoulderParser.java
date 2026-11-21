package cn.nubia.tgk.util;

import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import java.util.Iterator;
import java.util.LinkedHashMap;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: classes2.dex */
public class ColorfulLightShoulderParser {
    public static final String TAG = "TgkLampHelper_ShoulderParser";

    public static class ShoulderSettings {
        public Action l_down;
        public Action l_up;
        public Action r_down;
        public Action r_up;
        public String selection;
        public LinkedHashMap<String, String> shoulder_configs = new LinkedHashMap<>();

        public String toString() {
            StringBuilder sb = new StringBuilder("l_down :" + this.l_down + "; l_up  :" + this.l_up + "; r_down  :" + this.r_down + "; r_up  :" + this.r_up + ";selection :" + this.selection);
            sb.append(";shoulder_configs size =" + this.shoulder_configs.size() + ";");
            LinkedHashMap<String, String> linkedHashMap = this.shoulder_configs;
            if (linkedHashMap != null && !linkedHashMap.isEmpty()) {
                for (String str : this.shoulder_configs.keySet()) {
                    sb.append("name:" + str + "|value:" + this.shoulder_configs.get(str) + ",");
                }
            }
            return sb.toString();
        }
    }

    public static class Action {
        public boolean enable;
        public int id;

        public String toString() {
            return "(id =" + this.id + ",enable =" + this.enable + ")";
        }
    }

    public static ShoulderSettings getShoulderSettings(Context context) {
        try {
            String string = Settings.Global.getString(context.getContentResolver(), "lighting_shoulder_config_json");
            Log.d(TAG, "getShoulderSettings json= " + string);
            if (TextUtils.isEmpty(string)) {
                return null;
            }
            return fromJson(string);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static ShoulderSettings fromJson(String str) throws Exception {
        if (str == null || str.trim().isEmpty()) {
            throw new JSONException("Input JSON string is empty.");
        }
        JSONObject jSONObject = new JSONObject(str);
        try {
            ShoulderSettings shoulderSettings = new ShoulderSettings();
            if (jSONObject.has("left_down")) {
                JSONObject jSONObject2 = jSONObject.getJSONObject("left_down");
                shoulderSettings.l_down = new Action();
                shoulderSettings.l_down.enable = jSONObject2.getBoolean("enable");
                shoulderSettings.l_down.id = Integer.parseInt(jSONObject2.getString("id"));
            }
            if (jSONObject.has("left_up")) {
                JSONObject jSONObject3 = jSONObject.getJSONObject("left_up");
                shoulderSettings.l_up = new Action();
                shoulderSettings.l_up.enable = jSONObject3.getBoolean("enable");
                shoulderSettings.l_up.id = Integer.parseInt(jSONObject3.getString("id"));
            }
            if (jSONObject.has("right_down")) {
                JSONObject jSONObject4 = jSONObject.getJSONObject("right_down");
                shoulderSettings.r_down = new Action();
                shoulderSettings.r_down.enable = jSONObject4.getBoolean("enable");
                shoulderSettings.r_down.id = Integer.parseInt(jSONObject4.getString("id"));
            }
            if (jSONObject.has("right_up")) {
                JSONObject jSONObject5 = jSONObject.getJSONObject("right_up");
                shoulderSettings.r_up = new Action();
                shoulderSettings.r_up.enable = jSONObject5.getBoolean("enable");
                shoulderSettings.r_up.id = Integer.parseInt(jSONObject5.getString("id"));
            }
            if (jSONObject.has("shoulder_effects")) {
                JSONObject jSONObject6 = jSONObject.getJSONObject("shoulder_effects");
                Iterator<String> keys = jSONObject6.keys();
                while (keys.hasNext()) {
                    String next = keys.next();
                    shoulderSettings.shoulder_configs.put(next, jSONObject6.getString(next));
                }
            }
            if (jSONObject.has("selection")) {
                shoulderSettings.selection = jSONObject.getString("selection");
            }
            return shoulderSettings;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
