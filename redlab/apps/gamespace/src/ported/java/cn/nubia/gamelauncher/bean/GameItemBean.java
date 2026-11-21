package dev.redlab.gamespace.bean;

import dev.redlab.gamespace.util.LogUtil;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class GameItemBean {
    public static final int UNKNOWN_PROGRESS = -1;
    private int mAppType;
    private String mIconUrl;
    private String mMiddleUrl;
    private String mPackageName;
    private int mProgress;
    private int mSoftId;
    private String mSoftName;
    private String mStatus;
    private String mUrl;

    public GameItemBean(JSONObject jSONObject) {
        this.mProgress = -1;
        try {
            this.mPackageName = jSONObject.getString("PackageName");
            this.mSoftId = jSONObject.getInt("SoftId");
            this.mAppType = jSONObject.getInt("AppType");
            this.mSoftName = jSONObject.getString("SoftName");
            this.mUrl = jSONObject.getString("Url");
        } catch (Exception e) {
            LogUtil.e("GameItemBean", "GameItemBean init Error!! " + e.fillInStackTrace());
        }
        parseIconUrl(jSONObject);
    }

    private void parseIconUrl(JSONObject jSONObject) {
        try {
            this.mIconUrl = jSONObject.getJSONObject("SoftItem").getJSONObject("Icon").getString("Px78");
        } catch (JSONException e) {
            LogUtil.e("GameItemBean", "parseIconUrl " + e.fillInStackTrace());
        }
    }

    public GameItemBean(String str, int i, String str2) {
        this.mProgress = -1;
        this.mPackageName = str;
        this.mAppType = i;
        this.mUrl = str2;
    }

    public GameItemBean(String str, int i, String str2, String str3) {
        this.mProgress = -1;
        this.mPackageName = str;
        this.mAppType = i;
        this.mUrl = str2;
        this.mMiddleUrl = str3;
    }

    public GameItemBean(String str, String str2, int i) {
        this.mPackageName = str;
        this.mStatus = str2;
        this.mProgress = i;
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public void setPackageName(String str) {
        this.mPackageName = str;
    }

    public int getSoftId() {
        return this.mSoftId;
    }

    public void setSoftId(int i) {
        this.mSoftId = i;
    }

    public int getAppType() {
        return this.mAppType;
    }

    public void setAppType(int i) {
        this.mAppType = i;
    }

    public String getSoftName() {
        return this.mSoftName;
    }

    public void setSoftName(String str) {
        this.mSoftName = str;
    }

    public String getUrl() {
        return this.mUrl;
    }

    public void setUrl(String str) {
        this.mUrl = str;
    }

    public String getMiddleUrl() {
        return this.mMiddleUrl;
    }

    public void setMiddleUrl(String str) {
        this.mMiddleUrl = str;
    }

    public String getIconUrl() {
        return this.mIconUrl;
    }

    public void setIconUrl(String str) {
        this.mIconUrl = str;
    }

    public int getProgress() {
        return this.mProgress;
    }

    public void setProgress(int i) {
        this.mProgress = i;
    }

    public String getStatus() {
        return this.mStatus;
    }

    public void setStatus(String str) {
        this.mStatus = str;
    }

    public String toString() {
        return "GameItemBean{PackageName='" + this.mPackageName + "', SoftId=" + this.mSoftId + ", AppType=" + this.mAppType + ", SoftName='" + this.mSoftName + "', Url='" + this.mUrl + "', mMiddleUrl='" + this.mMiddleUrl + "', mIconUrl='" + this.mIconUrl + "', mProgress=" + this.mProgress + ", mStatus='" + this.mStatus + "'}";
    }
}
