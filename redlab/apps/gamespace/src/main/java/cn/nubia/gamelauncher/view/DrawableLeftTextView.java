package cn.nubia.gamelauncher.view;

import android.content.Context;
import android.util.AttributeSet;

/**
 * Name bridge for the original layout.
 *
 * <p>The port keeps the original layouts verbatim, but the decompiled sources were repackaged to
 * {@code dev.redlab.gamespace.*}. {@code res/layout/game_item_medium.xml} still names
 * {@code cn.nubia.gamelauncher.view.DrawableLeftTextView}, so the inflater looks for that exact
 * class. This thin subclass keeps the original tag working without editing the layout, and the
 * implementation stays in one place ({@link dev.redlab.gamespace.view.DrawableLeftTextView}).
 */
public class DrawableLeftTextView extends dev.redlab.gamespace.view.DrawableLeftTextView {

    public DrawableLeftTextView(Context context) {
        super(context);
    }

    public DrawableLeftTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DrawableLeftTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
}
