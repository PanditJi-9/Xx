package dev.redlab.gamespace.atmosphere;

/* loaded from: classes.dex */
public class CustomBean {
    String type;
    String url;

    public CustomBean(String str, String str2) {
        this.url = str;
        this.type = str2;
    }

    public void setUrl(String str) {
        this.url = str;
    }

    public String getUrl() {
        return this.url;
    }

    public void setType(String str) {
        this.type = str;
    }

    public String getType() {
        return this.type;
    }

    public String toString() {
        return "CustomBean{type = " + this.type + ", url = " + this.url + '}';
    }
}
