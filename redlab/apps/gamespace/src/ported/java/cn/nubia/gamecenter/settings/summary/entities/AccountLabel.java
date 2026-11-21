package cn.nubia.gamecenter.settings.summary.entities;

/* loaded from: classes.dex */
public class AccountLabel {
    public int advancedRuleValue;
    public int realRuleValue;
    public int ruleValue;
    public int status;
    public String tagDesc;
    public int tagId;
    public String tagName;

    public String toString() {
        return "AccountLabel{tagId: " + this.tagId + ", tagName: " + this.tagName + ", status: " + this.status + ", tagDesc: " + this.tagDesc + "}";
    }
}
