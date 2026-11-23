package cn.nubia.gamelauncher.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

/**
 * Owned stand-in for the original {@code cn.nubia.gamelauncher.view.OneMoreThing}.
 *
 * <p>The vendor class is a small decorative indicator tied to the vendor's "top game" tab. Its
 * animation data is not part of the ported resources, so the view keeps the shape the layout
 * needs (a wrap-content child above the tabs) without drawing a guessed graphic. The activity
 * hides it, because the tab it annotates is not reachable on this build.
 */
public class OneMoreThing extends View {

    public OneMoreThing(Context context) {
        super(context);
    }

    public OneMoreThing(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public OneMoreThing(Context context, AttributeSet attributeSet, int defStyleAttr) {
        super(context, attributeSet, defStyleAttr);
    }
}
