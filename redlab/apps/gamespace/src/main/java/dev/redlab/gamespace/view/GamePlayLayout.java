package dev.redlab.gamespace.view;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.constraintlayout.widget.ConstraintLayout;

import dev.redlab.gamespace.PortLayoutInflater;
import dev.redlab.gamespace.R;

/**
 * The original {@code game_play_layout}: start pill, chevron and the three launch modes.
 *
 * <p>The layout is the vendor's own, inflated through the tag redirect, so the pill, the chevron
 * and the "mouse / handle / mirror" buttons keep their stock resources and constraints. The
 * vendor class asked its controller service which modes the connected hardware supports; on an
 * ordinary phone none of that hardware exists, so every mode is shown and each one launches the
 * selected app normally instead of starting a controller session. The chevron reveals and hides
 * them with the same slide-and-fade the stock screen uses.
 */
public final class GamePlayLayout extends ConstraintLayout {

    private static final int REVEAL_DURATION = 200;

    private boolean expanded;

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
        LayoutInflater inflater = LayoutInflater.from(getContext()).cloneInContext(getContext());
        PortLayoutInflater.install(inflater);
        inflater.inflate(R.layout.game_play_layout, this, true);

        int pillHeight = getResources().getDrawable(R.drawable.game_lobby_other_play, null)
                .getIntrinsicHeight();
        if (pillHeight <= 0) {
            pillHeight = Math.round(40f * getResources().getDisplayMetrics().density);
        }

        View start = findViewById(R.id.start_button);
        if (start != null) {
            // game_play_layout gives the pill and the chevron 0dp/percent heights, which resolve
            // against this wrapper. The wrapper is wrap_content, so left alone the percent is a
            // percentage of zero and both collapse. Pin them to the measured pill height.
            ViewGroup.LayoutParams startParams = start.getLayoutParams();
            startParams.height = pillHeight;
            start.setLayoutParams(startParams);
            start.setOnClickListener(view -> performClick());
            if (start instanceof GameStartView) {
                // game_start_button starts with the label and both side marks transparent; the
                // original lobby reveals them when the view is attached. Without this call the
                // pill keeps its background and looks empty.
                GameStartView starter = (GameStartView) start;
                post(starter::doEnterAnim);
            }
        }
        View pill = findViewById(R.id.game_start);
        if (pill != null) {
            pill.setOnClickListener(view -> performClick());
        }

        View chevron = findViewById(R.id.switch_view);
        if (chevron != null) {
            ViewGroup.LayoutParams chevronParams = chevron.getLayoutParams();
            chevronParams.width = pillHeight;
            chevronParams.height = pillHeight;
            chevron.setLayoutParams(chevronParams);
            chevron.setOnClickListener(view -> toggleModes());
            if (chevron instanceof com.airbnb.lottie.LottieAnimationView) {
                com.airbnb.lottie.LottieAnimationView lottie =
                        (com.airbnb.lottie.LottieAnimationView) chevron;
                try {
                    lottie.setAnimation("lottie/switch_expand.json");
                    lottie.setProgress(0f);
                } catch (RuntimeException missing) {
                    android.util.Log.w("GamePlayLayout", "chevron animation unavailable", missing);
                }
                // The stock asset points left at progress 0; the collapsed lobby shows it
                // pointing right, towards the options it reveals.
                chevron.setRotation(180f);
            }
        }

        View[] modes = {findViewById(R.id.play_by_mouse),
                findViewById(R.id.play_by_handle), findViewById(R.id.play_by_mirror)};
        for (View mode : modes) {
            if (mode == null) {
                continue;
            }
            mode.setAlpha(0f);
            // GONE, not just transparent: the three pills sit to the left of the chevron and
            // would otherwise cover the start label before the first expand.
            mode.setVisibility(View.GONE);
            mode.setClickable(true);
            // None of these sessions exist on a stock phone; each still starts the game the same
            // way the main pill does, so the control is not dead.
            mode.setOnClickListener(view -> performClick());
        }

        setClickable(true);
        setFocusable(true);
    }

    private void toggleModes() {
        expanded = !expanded;
        int[] ids = {R.id.play_by_mouse, R.id.play_by_handle, R.id.play_by_mirror};
        for (int i = 0; i < ids.length; i++) {
            View mode = findViewById(ids[i]);
            if (mode == null) {
                continue;
            }
            if (expanded) {
                mode.setAlpha(0f);
                mode.setVisibility(View.VISIBLE);
                ObjectAnimator.ofFloat(mode, View.ALPHA, 0f, 1f)
                        .setDuration(REVEAL_DURATION)
                        .start();
            } else {
                ObjectAnimator fade = ObjectAnimator.ofFloat(mode, View.ALPHA, 1f, 0f);
                fade.setDuration(REVEAL_DURATION);
                fade.addListener(new android.animation.AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(android.animation.Animator animation) {
                        mode.setVisibility(View.GONE);
                    }
                });
                fade.start();
            }
        }
        View chevron = findViewById(R.id.switch_view);
        if (chevron != null) {
            chevron.animate().rotation(expanded ? 0f : 180f)
                    .setDuration(REVEAL_DURATION).start();
        }
    }
}
