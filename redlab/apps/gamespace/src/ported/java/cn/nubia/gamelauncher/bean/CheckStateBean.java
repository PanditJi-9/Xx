package dev.redlab.gamespace.bean;

import dev.redlab.gamespace.util.LogUtil;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class CheckStateBean {
    private static final String JSON_PACKAGENAME = "PackageName";
    private static final String JSON_STATE = "State";
    private static final String JSON_TOPIC_ID = "TopicId";
    private String packageName;
    private int state;
    private int topicId;

    public CheckStateBean(JSONObject jSONObject) {
        try {
            this.packageName = jSONObject.getString(JSON_PACKAGENAME);
            this.state = jSONObject.getInt(JSON_STATE);
            this.topicId = jSONObject.getInt(JSON_TOPIC_ID);
        } catch (Exception unused) {
            LogUtil.e("ResponseBean", "ResponseBean init Error!!");
        }
    }

    public String getPackageName() {
        return this.packageName;
    }

    public void setPackageName(String str) {
        this.packageName = str;
    }

    public int getState() {
        return this.state;
    }

    public void setState(int i) {
        this.state = i;
    }

    public int getTopicId() {
        return this.topicId;
    }

    public void setTopicId(int i) {
        this.topicId = i;
    }

    public String toString() {
        return "CheckStateBean{packageName='" + this.packageName + "', state=" + this.state + ", topicId=" + this.topicId + '}';
    }
}
