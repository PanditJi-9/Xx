package dev.redlab.gamespace.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import androidx.constraintlayout.widget.ConstraintLayout;

import dev.redlab.gamespace.R;

/**
 * Owned stand-in for the original {@code cn.nubia.gamelauncher.view.GamePlayLayout}.
 *
 * <p>The original class also owns the three controller buttons and a Lottie switch that need
 * Nubia's play-mode feature flags and controller service. Those do not exist on a stock phone, so
 * this port keeps the original start control alone: the real {@code game_start_button} layout,
 * inflated by {@link GameStartView}, with its enter reveal and press scale. The activity keeps the
 * click that launches the selected app.
 */
public final class GamePlayLayout extends ConstraintLayout {

    private GameStartView start;
    private boolean entered;

    public GamePlayLayout(Context context) {
        this(context, null);
    }

    public GamePlayLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GamePlayLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        start = new GameStartView(getContext());
        int height = intrinsicPillHeight();
        addView(start, new LayoutParams(LayoutParams.WRAP_CONTENT, height));
        // GameStartView turns itself and its pill clickable for the press animation, exactly like
        // the original. The original lobby attaches its start action to the pill, so forward that
        // click to this layout, which the activity already listens to.
        start.setOnClickListener(view -> performClick());
        View pill = start.findViewById(R.id.game_start);
        if (pill != null) {
            pill.setOnClickListener(view -> performClick());
        }
        setClickable(true);
        setFocusable(true);
    }

    /** The original nine-patch is 132 px high; use its own measurement, not a guessed dip value. */
    private int intrinsicPillHeight() {
        Drawable pill = getResources().getDrawable(R.drawable.game_lobby_other_play, null);
        int height = pill == null ? 0 : pill.getIntrinsicHeight();
        return height > 0 ? height : Math.round(40f * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!entered) {
            entered = true;
            post(start::doEnterAnim);
        }
    }
}
