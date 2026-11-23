package cn.nubia.gamelauncher.redmagicplanet.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * Owned stand-in for the original {@code RedMagicVideoPlayer}.
 *
 * <p>The vendor class wraps a Media3 player and plays the Planet preview clip from the Nubia
 * content service. That service is not on this phone, so the card keeps its original shape and
 * the poster frame the layout gives it; this view is the empty surface the player would have
 * filled. Nothing is drawn over it by guesswork.
 */
public class RedMagicVideoPlayer extends FrameLayout {

    public RedMagicVideoPlayer(Context context) {
        super(context);
    }

    public RedMagicVideoPlayer(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public RedMagicVideoPlayer(Context context, AttributeSet attributeSet, int defStyleAttr) {
        super(context, attributeSet, defStyleAttr);
    }
}
