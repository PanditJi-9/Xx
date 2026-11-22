package dev.redlab.gamespace.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import dev.redlab.gamespace.R;

/**
 * Owned stand-in for the original {@code cn.nubia.gamelauncher.view.GamePlayLayout}.
 *
 * <p>The original class needs {@code GameStartView}, {@code ZoomButtonX}, a sound pool and a
 * controller connection. This version keeps the original lobby's slot and uses the original
 * {@code game_space_start_game_bg} drawable and {@code lobby_start_game} string, so the screen
 * keeps the RedMagic shape while remaining launchable on a non-Nubia phone.
 */
public final class GamePlayLayout extends ConstraintLayout {

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
        TextView start = new TextView(getContext());
        start.setText(R.string.lobby_start_game);
        start.setGravity(Gravity.CENTER);
        start.setTextColor(0xffffffff);
        start.setTextSize(14f);
        start.setShadowLayer(4f, 0f, 2f, 0xcc000000);
        start.setBackgroundResource(R.mipmap.game_space_start_game_bg);
        start.setClickable(false);

        int size = Math.round(96f * getResources().getDisplayMetrics().density);
        addView(start, new LayoutParams(size, size));
        setClickable(true);
        setFocusable(true);
    }
}
