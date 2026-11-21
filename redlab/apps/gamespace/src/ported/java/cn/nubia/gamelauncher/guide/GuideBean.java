package dev.redlab.gamespace.guide;

/* loaded from: classes.dex */
public class GuideBean {
    private String mExplain;
    Integer mLogoId;
    private String mTitle;

    public GuideBean(String str, String str2, Integer num) {
        this.mTitle = str;
        this.mExplain = str2;
        this.mLogoId = num;
    }

    public int getLogoId() {
        return this.mLogoId.intValue();
    }

    public void setLogoId(int i) {
        this.mLogoId = Integer.valueOf(i);
    }

    public String getTitle() {
        return this.mTitle;
    }

    public void setAppType(String str) {
        this.mTitle = str;
    }

    public String getExplain() {
        return this.mExplain;
    }

    public void setExplain(String str) {
        this.mExplain = str;
    }
}
