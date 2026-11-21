package cn.nubia.gamecenter.settings.preference;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;
import cn.nubia.gamecenter.settings.R;
import cn.nubia.gamecenter.settings.compatible.PreferenceViewHolder;
import cn.nubia.gamecenter.settings.compatible.SwitchPreference;
import cn.nubia.gamecenter.settings.helper.AnimatorHelper;
import cn.nubia.gamecenter.settings.utils.LogUtil;

/* loaded from: classes.dex */
public class GameCenterSwitchPreference extends SwitchPreference {
    private static String TAG = "GameCenterSwitchPreference";
    private final Listener mListener;
    private View mSwitchLayout;
    private TextView mSwitchOff;
    private TextView mSwitchOn;
    private boolean performClick;
    private ImageView switchButtonThumbBlack;
    private ImageView switchButtonThumbRed;
    private ImageView switchButtonTrackBlack;
    private ImageView switchButtonTrackGray;

    private class Listener implements CompoundButton.OnCheckedChangeListener {
        private Listener() {
        }

        @Override // android.widget.CompoundButton.OnCheckedChangeListener
        public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
            if (!GameCenterSwitchPreference.this.callChangeListener(Boolean.valueOf(z))) {
                compoundButton.setChecked(!z);
            } else {
                GameCenterSwitchPreference.this.setChecked(z);
            }
        }
    }

    public GameCenterSwitchPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mListener = new Listener();
        this.performClick = false;
        setLayoutResource(R.layout.gcs_switch_preference);
        setWidgetLayoutResource(R.layout.gcs_preference_widget_switch);
    }

    public void setThumbAnim(View view, View view2, View view3, View view4, boolean z, boolean z2) {
        LogUtil.i(TAG, "setThumbAnim anim: " + z2);
        AnimatorSet animatorSet = new AnimatorSet();
        float applyDimension = TypedValue.applyDimension(1, 30.0f, getContext().getResources().getDisplayMetrics());
        ObjectAnimator ofFloat = ObjectAnimator.ofFloat(view, AnimatorHelper.Item.TRANSLATIONX, 0.0f, applyDimension);
        ObjectAnimator ofFloat2 = ObjectAnimator.ofFloat(view2, AnimatorHelper.Item.TRANSLATIONX, 0.0f, applyDimension);
        ObjectAnimator ofFloat3 = ObjectAnimator.ofFloat(view, AnimatorHelper.Item.TRANSLATIONX, applyDimension, 0.0f);
        ObjectAnimator ofFloat4 = ObjectAnimator.ofFloat(view2, AnimatorHelper.Item.TRANSLATIONX, applyDimension, 0.0f);
        ObjectAnimator ofFloat5 = ObjectAnimator.ofFloat(view, AnimatorHelper.Item.ALPHA, 0.0f, 1.0f);
        ObjectAnimator ofFloat6 = ObjectAnimator.ofFloat(view2, AnimatorHelper.Item.ALPHA, 0.0f, 1.0f);
        ObjectAnimator ofFloat7 = ObjectAnimator.ofFloat(view3, AnimatorHelper.Item.ALPHA, 0.0f, 1.0f);
        ObjectAnimator ofFloat8 = ObjectAnimator.ofFloat(view4, AnimatorHelper.Item.ALPHA, 0.0f, 1.0f);
        ObjectAnimator ofFloat9 = ObjectAnimator.ofFloat(view, AnimatorHelper.Item.ALPHA, 1.0f, 0.0f);
        ObjectAnimator ofFloat10 = ObjectAnimator.ofFloat(view2, AnimatorHelper.Item.ALPHA, 1.0f, 0.0f);
        ObjectAnimator ofFloat11 = ObjectAnimator.ofFloat(view3, AnimatorHelper.Item.ALPHA, 1.0f, 0.0f);
        ObjectAnimator ofFloat12 = ObjectAnimator.ofFloat(view4, AnimatorHelper.Item.ALPHA, 1.0f, 0.0f);
        animatorSet.setDuration(z2 ? 250L : 0L);
        if (z) {
            animatorSet.playTogether(ofFloat, ofFloat2, ofFloat9, ofFloat6, ofFloat7, ofFloat12);
        } else {
            animatorSet.playTogether(ofFloat3, ofFloat4, ofFloat5, ofFloat10, ofFloat11, ofFloat8);
        }
        animatorSet.start();
    }

    @Override // cn.nubia.gamecenter.settings.compatible.SwitchPreference
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        ToggleButton toggleButton = (ToggleButton) preferenceViewHolder.findViewById(R.id.gamespace_switch);
        this.mSwitchOn = (TextView) preferenceViewHolder.findViewById(R.id.gamespace_switch_on_id);
        this.mSwitchOff = (TextView) preferenceViewHolder.findViewById(R.id.gamespace_switch_off_id);
        this.mSwitchLayout = preferenceViewHolder.findViewById(R.id.switch_button_layout);
        this.switchButtonTrackBlack = (ImageView) preferenceViewHolder.findViewById(R.id.switch_button_track_black);
        this.switchButtonTrackGray = (ImageView) preferenceViewHolder.findViewById(R.id.switch_button_track_gray);
        this.switchButtonThumbRed = (ImageView) preferenceViewHolder.findViewById(R.id.switch_button_thumb_red);
        this.switchButtonThumbBlack = (ImageView) preferenceViewHolder.findViewById(R.id.switch_button_thumb_black);
        LogUtil.i(TAG, "---onBindViewHolder");
        syncSwitchView(toggleButton, this.performClick);
        this.performClick = false;
    }

    @Override // androidx.preference.SwitchPreference, androidx.preference.Preference
    protected void performClick(View view) {
        super.performClick(view);
        LogUtil.i(TAG, "---performClick");
        this.performClick = true;
        syncSwitchView(view, true);
    }

    /* JADX WARN: Multi-variable type inference failed */
    private void syncSwitchView(View view, boolean z) {
        if (view instanceof Checkable) {
            ((Checkable) view).setChecked(this.mChecked);
            if (isEnabled()) {
                this.mSwitchLayout.setAlpha(1.0f);
            } else {
                this.mSwitchLayout.setAlpha(0.3f);
            }
            setThumbAnim(this.switchButtonThumbBlack, this.switchButtonThumbRed, this.switchButtonTrackBlack, this.switchButtonTrackGray, this.mChecked, z);
        }
    }
}
