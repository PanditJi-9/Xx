package cn.nubia.gamecenter.settings.barrageMessage;

import android.text.TextUtils;

/* loaded from: classes.dex */
public class AppBean {
    private boolean checked;
    private String label;
    private String lowerCaseLabel;
    private String packageName;

    public AppBean(String str, String str2, boolean z) {
        this.packageName = str;
        this.label = str2;
        this.checked = z;
        if (str2 != null && !TextUtils.isEmpty(str2)) {
            str2 = str2.toLowerCase();
        }
        this.lowerCaseLabel = str2;
    }

    public String getPackageName() {
        return this.packageName;
    }

    public void setPackageName(String str) {
        this.packageName = str;
    }

    public String getLabel() {
        return this.label;
    }

    public String getLowerCaseLabel() {
        return this.lowerCaseLabel;
    }

    public void setLabel(String str) {
        this.label = str;
    }

    public boolean isChecked() {
        return this.checked;
    }

    public void setChecked(boolean z) {
        this.checked = z;
    }

    public String toString() {
        return "AppBean{packageName='" + this.packageName + "', label='" + this.label + "'}";
    }
}
