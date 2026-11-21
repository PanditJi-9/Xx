package cn.nubia.gamecenter.settings;

/* loaded from: classes.dex */
public class CategoryInfo {
    public static final int TYPE_DIVIDER = 0;
    public static final String TYPE_DIVIDER_NAME = "divider";
    public static final int TYPE_NORMAL = 1;
    private Class<?> cls;
    private int icon;
    private String simpleName;
    private int title;
    private int type = 0;

    public CategoryInfo() {
    }

    public CategoryInfo(Class<?> cls, int i, int i2) {
        this.cls = cls;
        this.simpleName = cls.getSimpleName();
        this.icon = i;
        this.title = i2;
    }

    public String getSimpleName() {
        return this.simpleName;
    }

    public void setSimpleName(String str) {
        this.simpleName = str;
    }

    public int getIcon() {
        return this.icon;
    }

    public void setIcon(int i) {
        this.icon = i;
    }

    public int getTitle() {
        return this.title;
    }

    public void setTitle(int i) {
        this.title = i;
    }

    public Class<?> getCls() {
        return this.cls;
    }

    public void setCls(Class<?> cls) {
        this.cls = cls;
    }

    public int getType() {
        return this.type;
    }

    public void setType(int i) {
        this.type = i;
    }

    public String toString() {
        return "MenuInfo{simpleName='" + this.simpleName + "', icon=" + this.icon + ", title=" + this.title + '}';
    }
}
