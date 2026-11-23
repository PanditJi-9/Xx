package cn.nubia.common.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;

/**
 * Owned stand-in for the original {@code cn.nubia.common.view.NubiaFanView}.
 *
 * <p>The vendor class drives the phone fan through a Nubia hardware service that does not exist
 * on an ordinary device. The view itself is only a Lottie animation: {@code lower_view.xml}
 * supplies {@code app:lottie_fileName="lottie/fan_close.json"}, and the stock
 * {@link ZoomLottieAnimationView} already loads that file and owns the press feedback. What is
 * added here is the looping the vendor view performs for a running fan, so the lower bar keeps
 * its original shape instead of failing inflation on a missing class.
 */
public class NubiaFanView extends ZoomLottieAnimationView {

    public NubiaFanView(Context context) {
        this(context, null);
    }

    public NubiaFanView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setRepeatCount(ValueAnimator.INFINITE);
    }
}
