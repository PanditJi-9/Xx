package cn.nubia.neostore.gameplace.popup;

/* loaded from: classes2.dex */
public class PopupConfig {
    private int gravity;
    private int width;

    private PopupConfig(Builder builder) {
        this.width = builder.width;
        this.gravity = builder.gravity;
    }

    public int getWidth() {
        return this.width;
    }

    public int getGravity() {
        return this.gravity;
    }

    public static class Builder {
        private int width = -2;
        private int gravity = 17;

        public Builder heightPercent(float f) {
            return this;
        }

        public Builder width(int i) {
            this.width = i;
            return this;
        }

        public Builder gravity(int i) {
            this.gravity = i;
            return this;
        }

        public PopupConfig build() {
            return new PopupConfig(this);
        }
    }
}
