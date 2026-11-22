package cn.nubia.gamecenter.settings;

import android.animation.ObjectAnimator;
import android.view.View;
import cn.nubia.gamecenter.settings.helper.AnimatorHelper;

/* loaded from: classes.dex */
public class GcsAnimationUtil {
    public static void setGcsRedItemAlpha(View view) {
        ObjectAnimator ofFloat = ObjectAnimator.ofFloat(view, AnimatorHelper.Item.ALPHA, 0.0f, 1.0f);
        ofFloat.setDuration(300L);
        ofFloat.setRepeatCount(0);
        ofFloat.start();
    }

    public static void setGcsItemBgTranslationX(View view) {
        ObjectAnimator ofFloat = ObjectAnimator.ofFloat(view, AnimatorHelper.Item.TRANSLATIONX, -900.0f, 0.0f);
        ofFloat.setDuration(300L);
        ofFloat.setRepeatCount(0);
        ofFloat.start();
    }

    public static void setGcsMenuTranslationX(View view) {
        ObjectAnimator ofFloat = ObjectAnimator.ofFloat(view, AnimatorHelper.Item.TRANSLATIONX, -300.0f, 0.0f);
        ofFloat.setDuration(300L);
        ofFloat.setRepeatCount(0);
        ofFloat.start();
    }

    public static void setGcsMenuTranslationXRTL(View view) {
        ObjectAnimator ofFloat = ObjectAnimator.ofFloat(view, AnimatorHelper.Item.TRANSLATIONX, 0.0f, -300.0f);
        ofFloat.setDuration(300L);
        ofFloat.setRepeatCount(0);
        ofFloat.start();
    }

    public static void setGcsItemTranslationX(View view, int i) {
        ObjectAnimator ofFloat = ObjectAnimator.ofFloat(view, AnimatorHelper.Item.TRANSLATIONX, (i + 1) * 100, 0.0f);
        ofFloat.setDuration(300L);
        ofFloat.setRepeatCount(0);
        ofFloat.start();
    }

    public static void setGcsItemTranslationY(View view) {
        ObjectAnimator ofFloat = ObjectAnimator.ofFloat(view, AnimatorHelper.Item.TRANSLATIONY, 100.0f, 0.0f);
        ofFloat.setDuration(300L);
        ofFloat.setRepeatCount(0);
        ofFloat.start();
    }

    public static void setGcsItemAlpha(View view) {
        ObjectAnimator ofFloat = ObjectAnimator.ofFloat(view, AnimatorHelper.Item.ALPHA, 0.5f, 1.0f);
        ofFloat.setDuration(300L);
        ofFloat.setRepeatCount(0);
        ofFloat.start();
    }

    public static void setDataManagerFragmentAlpha(View view) {
        ObjectAnimator ofFloat = ObjectAnimator.ofFloat(view, AnimatorHelper.Item.ALPHA, 1.0f, 0.0f);
        ofFloat.setDuration(300L);
        ofFloat.setRepeatCount(0);
        ofFloat.start();
        ObjectAnimator ofFloat2 = ObjectAnimator.ofFloat(view, AnimatorHelper.Item.ALPHA, 0.0f, 1.0f);
        ofFloat2.setDuration(300L);
        ofFloat2.setRepeatCount(0);
        ofFloat2.start();
    }
}
