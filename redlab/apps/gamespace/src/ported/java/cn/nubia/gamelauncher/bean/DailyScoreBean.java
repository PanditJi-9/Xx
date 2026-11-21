package dev.redlab.gamespace.bean;

/* loaded from: classes.dex */
public class DailyScoreBean implements Comparable {
    private int mCpsCount;
    private int mCpsY;
    private Long mDate;
    private String mDateStr;
    private int mFontX;
    private int mFontY;
    private int mLineBottomY;
    private int mLineX;
    private int mMpmCount;
    private int mMpmY;
    private int mWinCount;
    private int mWinY;

    public Long getDate() {
        return this.mDate;
    }

    public void setDate(Long l) {
        this.mDate = l;
    }

    public String getDateStr() {
        return this.mDateStr;
    }

    public void setDateStr(String str) {
        this.mDateStr = str;
    }

    public int getLineX() {
        return this.mLineX;
    }

    public void setLineX(int i) {
        this.mLineX = i;
    }

    public void updateLineX(int i) {
        this.mLineX += i;
        this.mFontX += i;
    }

    public int getFontX() {
        return this.mFontX;
    }

    public void setFontX(int i) {
        this.mFontX = i;
    }

    public int getFontY() {
        return this.mFontY;
    }

    public void setFontY(int i) {
        this.mFontY = i;
    }

    public int getLineBottomY() {
        return this.mLineBottomY;
    }

    public void setLineBottomY(int i) {
        this.mLineBottomY = i;
        updateCpsY();
    }

    private void updateCpsY() {
        int i = this.mLineBottomY;
        float f = i / 2.0f;
        float f2 = f / 10.0f;
        float f3 = f / 90.0f;
        int i2 = this.mWinCount;
        if (i2 <= 10) {
            this.mWinY = (int) (i - (i2 * f2));
        } else {
            this.mWinY = (int) (f - ((i2 - 10) * f3));
        }
        int i3 = this.mCpsCount;
        if (i3 <= 10) {
            this.mCpsY = (int) (i - (i3 * f2));
        } else {
            this.mCpsY = (int) (f - ((i3 - 10) * f3));
        }
        int i4 = this.mMpmCount;
        if (i4 <= 10) {
            this.mMpmY = (int) (i - (i4 * f2));
        } else {
            this.mMpmY = (int) (f - ((i4 - 10) * f3));
        }
    }

    public int getWinY() {
        return this.mWinY;
    }

    public void setWinY(int i) {
        this.mWinY = i;
    }

    public int getCpsY() {
        return this.mCpsY;
    }

    public void setCpsY(int i) {
        this.mCpsY = i;
    }

    public int getMpmY() {
        return this.mMpmY;
    }

    public void setMpmY(int i) {
        this.mMpmY = i;
    }

    public void setCpsCount(int i) {
        this.mCpsCount = i;
    }

    public int getCpsCount() {
        return this.mCpsCount;
    }

    public int getMpmCount() {
        return this.mMpmCount;
    }

    public void setMpmCount(int i) {
        this.mMpmCount = i;
    }

    public int getWinCount() {
        return this.mWinCount;
    }

    public void setWinCount(int i) {
        this.mWinCount = i;
    }

    @Override // java.lang.Comparable
    public int compareTo(Object obj) {
        return this.mDate.longValue() >= ((DailyScoreBean) obj).mDate.longValue() ? -1 : 1;
    }
}
