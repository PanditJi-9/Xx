package cn.nubia.gamelauncher.redmagicplanet.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.HorizontalScrollView;

/**
 * Owned stand-in for the original {@code CustomHorizontalScrollView}.
 *
 * <p>The vendor class only overrides the touch interception used by its remote-control steering.
 * The Planet strip is scrolled by the user here, so the stock HorizontalScrollView behaviour is
 * the whole view.
 */
public class CustomHorizontalScrollView extends HorizontalScrollView {

    public CustomHorizontalScrollView(Context context) {
        super(context);
    }

    public CustomHorizontalScrollView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public CustomHorizontalScrollView(Context context, AttributeSet attributeSet, int defStyleAttr) {
        super(context, attributeSet, defStyleAttr);
    }
}
