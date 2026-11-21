package dev.redlab.gamespace.anim;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.util.Property;
import android.view.View;
import android.view.animation.PathInterpolator;

/* loaded from: classes.dex */
public class AnimHelper {
    public static final PathInterpolator INTERPOLATOR = new PathInterpolator(0.42f, 0.0f, 0.58f, 1.0f);
    public static final PathInterpolator EASY_EASE = new PathInterpolator(0.33f, 0.0f, 0.0f, 1.0f);
    public static final PathInterpolator EASY_EASE2 = new PathInterpolator(0.33f, 0.0f, 0.67f, 1.0f);

    public static ObjectAnimator createPropertyAnim(View view, int i, PathInterpolator pathInterpolator, AnimBean... animBeanArr) {
        int length = animBeanArr.length;
        PropertyValuesHolder[] propertyValuesHolderArr = new PropertyValuesHolder[length];
        for (int i2 = 0; i2 < length; i2++) {
            AnimBean animBean = animBeanArr[i2];
            propertyValuesHolderArr[i2] = PropertyValuesHolder.ofFloat(animBean.mProperty, animBean.mStart, animBean.mEnd);
        }
        ObjectAnimator ofPropertyValuesHolder = ObjectAnimator.ofPropertyValuesHolder(view, propertyValuesHolderArr);
        ofPropertyValuesHolder.setDuration(i);
        if (pathInterpolator != null) {
            ofPropertyValuesHolder.setInterpolator(pathInterpolator);
        }
        return ofPropertyValuesHolder;
    }

    public static ObjectAnimator createPropertyAnim(View view, int i, AnimBean... animBeanArr) {
        return createPropertyAnim(view, i, INTERPOLATOR, animBeanArr);
    }

    public static ObjectAnimator createPropertyAnimNoInterpolator(View view, int i, AnimBean... animBeanArr) {
        return createPropertyAnim(view, i, null, animBeanArr);
    }

    public static ObjectAnimator createPropertyValuesAnimator(View view, Property<?, Float> property, int i, int i2, int i3, PathInterpolator pathInterpolator) {
        ObjectAnimator ofPropertyValuesHolder = ObjectAnimator.ofPropertyValuesHolder(view, PropertyValuesHolder.ofFloat(property, i, i2));
        ofPropertyValuesHolder.setInterpolator(pathInterpolator);
        ofPropertyValuesHolder.setDuration(i3);
        return ofPropertyValuesHolder;
    }
}
