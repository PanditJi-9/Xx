package cn.nubia.common.wallpaper;

/* loaded from: classes.dex */
public class WallpaperItemBean {
    private int mId;
    private boolean mIsSelected;
    private String mPreviewUrl;
    private int mWallpaperType;
    private String mWallpaperUrl;

    public WallpaperItemBean(int i, int i2, String str, String str2, boolean z) {
        this.mWallpaperType = i;
        this.mPreviewUrl = str;
        this.mWallpaperUrl = str2;
        this.mIsSelected = z;
        this.mId = i2;
    }

    public String getWallpaperUrl() {
        return this.mWallpaperUrl;
    }

    public void setWallpaperUrl(String str) {
        this.mWallpaperUrl = str;
    }

    public boolean isLiveWallpaper() {
        return this.mWallpaperType == 1;
    }

    public boolean isImageWallpaper() {
        return this.mWallpaperType == 2;
    }

    public String getPreviewUrl() {
        String str = this.mPreviewUrl;
        return str == null ? getWallpaperUrl() : str;
    }

    public void setSelected(boolean z) {
        this.mIsSelected = z;
    }

    public boolean isSelected() {
        return this.mIsSelected;
    }

    public int getType() {
        return this.mWallpaperType;
    }

    public void setType(int i) {
        this.mWallpaperType = i;
    }

    public String toString() {
        return "{mId = " + this.mId + ", type = " + this.mWallpaperType + ", wallpaperUrl = " + this.mWallpaperUrl + ", previewUrl = " + this.mPreviewUrl + ", isSelected = " + this.mIsSelected + " }";
    }
}
