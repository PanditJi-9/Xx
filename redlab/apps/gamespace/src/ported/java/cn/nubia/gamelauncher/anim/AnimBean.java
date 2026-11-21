package dev.redlab.gamespace.anim;

import android.util.Property;

/* loaded from: classes.dex */
public class AnimBean {
    float mEnd;
    Property<?, Float> mProperty;
    float mStart;

    public AnimBean(Property<?, Float> property, float f, float f2) {
        this.mProperty = property;
        this.mStart = f;
        this.mEnd = f2;
    }
}
