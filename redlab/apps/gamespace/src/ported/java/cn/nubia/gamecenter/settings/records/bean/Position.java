package cn.nubia.gamecenter.settings.records.bean;

/* loaded from: classes.dex */
public class Position {
    int offset;
    int pos;
    int type;

    public Position(int i, int i2, int i3) {
        this.pos = i;
        this.offset = i2;
        this.type = i3;
    }

    public int getPos() {
        return this.pos;
    }

    public void setPos(int i) {
        this.pos = i;
    }

    public int getOffset() {
        return this.offset;
    }

    public void setOffset(int i) {
        this.offset = i;
    }

    public int getType() {
        return this.type;
    }

    public void setType(int i) {
        this.type = i;
    }
}
