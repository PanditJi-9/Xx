package dev.redlab.gamespace.bean;

import android.util.Log;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class AtmosphereBean {
    private String mAtmosphereUrl;
    private int mId;
    private String mName;
    private String mPackageName;

    public AtmosphereBean(JSONObject jSONObject) {
        try {
            this.mPackageName = jSONObject.getString("package_name");
            this.mId = jSONObject.getInt("id");
            this.mName = jSONObject.getString("res_name");
            this.mAtmosphereUrl = jSONObject.getString("game_cover_url");
        } catch (Exception e) {
            Log.e("Atmosphere", "AtmosphereBean init Error!! " + e.fillInStackTrace());
        }
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public void setPackageName(String str) {
        this.mPackageName = str;
    }

    public int getId() {
        return this.mId;
    }

    public void setId(int i) {
        this.mId = i;
    }

    public String getName() {
        return this.mName;
    }

    public void setName(String str) {
        this.mName = str;
    }

    public String getUrl() {
        return this.mAtmosphereUrl;
    }

    public void setUrl(String str) {
        this.mAtmosphereUrl = str;
    }

    public boolean isValid() {
        return (getPackageName() == null || getUrl() == null) ? false : true;
    }

    public String toString() {
        return "AtmosphereBean{PackageName='" + this.mPackageName + "', SoftId=" + this.mId + ", SoftName='" + this.mName + "', Url='" + this.mAtmosphereUrl + "'}";
    }
}
