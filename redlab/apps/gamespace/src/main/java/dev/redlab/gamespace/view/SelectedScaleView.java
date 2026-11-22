package dev.redlab.gamespace.view;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import dev.redlab.gamespace.R;
import dev.redlab.gamespace.anim.AnimBean;
import dev.redlab.gamespace.anim.AnimHelper;

/**
 * The original full-banner card, ported without the vendor controller.
 *
 * <p>The original {@code cn.nubia.gamelauncher.view.SelectedScaleView} asks
 * {@code Controller.isPureMode()} for the pure-game variant and
 * {@code CommonUtil.isSlenderPhone()} for a 0.95 scale. Both answers are fixed here: this build
 * runs the normal lobby on ordinary phones, so the pure branch is not ported and the slender
 * check keeps the measured device-name test. Everything else — the frame swap, the property
 * visibility, the name offset and the 300 ms entrance — follows the original method bodies.
 *
 * <p>{@code game_item_full.xml} still names the vendor class; {@code PortLayoutInflater} resolves
 * that tag to this owned class, so the original layout is never edited.
 */
public class SelectedScaleView extends ConstraintLayout {

    /** The original {@code TgkHelper.P720F10_DEVICE}; only that device scales its children. */
    private static final String SLENDER_DEVICE = "P720F10";

    private AnimatorSet mAnimatorSet;
    private ImageView mBg;
    private ImageView mFrame;
    private ImageView mIcon;
    private ImageView mVip;
    private TextView mDays;
    private TextView mName;
    private TextView mSize;
    private TextView mTime;
    private boolean mInitialized;
    private boolean mIsSelected;
    private boolean mIsSupportProperty;

    public SelectedScaleView(Context context) {
        this(context, null);
    }

    public SelectedScaleView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mIsSelected = false;
        this.mIsSupportProperty = false;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initChild();
        doScale();
    }

    private void initChild() {
        this.mIcon = findViewById(R.id.icon);
        this.mBg = findViewById(R.id.item_selected_frame);
        this.mFrame = findViewById(R.id.game_selected_frame);
        this.mVip = findViewById(R.id.pure_vip);
        this.mName = findViewById(R.id.full_item_game_name);
        this.mTime = findViewById(R.id.property_time_value);
        this.mSize = findViewById(R.id.property_size_value);
        this.mDays = findViewById(R.id.property_days_value);
    }

    private void doScale() {
        if (Build.DEVICE != null && Build.DEVICE.contains(SLENDER_DEVICE)) {
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                child.setScaleX(0.95f);
                child.setScaleY(0.95f);
            }
        }
    }

    @Override
    public void setSelected(boolean selected) {
        // Every bind must reach onSelected at least once: the XML starts with the property
        // placeholders visible, and only onSelected(false) hides them on an unselected card.
        if (this.mInitialized && this.mIsSelected == selected) {
            return;
        }
        this.mInitialized = true;
        this.mIsSelected = selected;
        onSelected(selected);
    }

    public void setSupportProperty(boolean support) {
        this.mIsSupportProperty = support;
    }

    /**
     * The original adapter binds selection through this method, not through
     * {@link #setSelected(boolean)}: type 0 is the selected card and every other type is normal.
     */
    public void setType(int type) {
        onSelected(type == 0);
    }

    private void onSelected(boolean selected) {
        boolean showProperty = selected && this.mIsSupportProperty;
        if (selected) {
            doAnimator();
        }
        this.mDays.setVisibility(showProperty ? View.VISIBLE : View.GONE);
        this.mTime.setVisibility(showProperty ? View.VISIBLE : View.GONE);
        this.mSize.setVisibility(showProperty ? View.VISIBLE : View.GONE);
        // Normal mode never hides the name or the pill; only the pure lobby does.
        this.mName.setVisibility(View.VISIBLE);
        this.mBg.setVisibility(View.VISIBLE);
        this.mVip.setAlpha(0.0f);
        this.mName.setTranslationY(showProperty
                ? -getResources().getDimensionPixelOffset(R.dimen.game_name_trans_y)
                : 0.0f);
        this.mFrame.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
        this.mBg.setBackgroundResource(selected
                ? R.mipmap.game_lobby_item_selected
                : R.mipmap.game_lobby_item_unselected);
    }

    /** The original entrance: the pill slides in while the labels slide and fade in. */
    public void doAnimator() {
        AnimatorSet running = this.mAnimatorSet;
        if (running != null) {
            running.cancel();
        }
        ObjectAnimator pill = AnimHelper.createPropertyAnim(this.mBg, 300,
                new AnimBean(View.TRANSLATION_X, -200.0f, 0.0f),
                new AnimBean(View.SCALE_X, 0.2f, 1.0f));
        AnimBean slide = new AnimBean(View.TRANSLATION_X, 200.0f, 0.0f);
        AnimBean fade = new AnimBean(View.ALPHA, 0.0f, 1.0f);
        ObjectAnimator name = AnimHelper.createPropertyAnim(this.mName, 300, slide, fade);
        ObjectAnimator time = AnimHelper.createPropertyAnim(this.mTime, 300, slide, fade);
        ObjectAnimator size = AnimHelper.createPropertyAnim(this.mSize, 300, slide, fade);
        ObjectAnimator days = AnimHelper.createPropertyAnim(this.mDays, 300, slide, fade);
        AnimatorSet set = new AnimatorSet();
        this.mAnimatorSet = set;
        set.play(name).with(pill).with(time).with(size).with(days);
        set.start();
    }
}
