package cn.nubia.gamecenter.settings.compatible;

import android.content.Context;
import android.util.AttributeSet;
import cn.nubia.gamecenter.settings.R;
import cn.nubia.gamecenter.settings.utils.FlickerUtils;

/* loaded from: classes.dex */
public class CheckBoxPreference extends androidx.preference.CheckBoxPreference {
    private OnCheckedChangeListener mOnChangeListener;

    public interface OnCheckedChangeListener {
        boolean onCheckedChanged(CheckBoxPreference checkBoxPreference, Object obj);
    }

    public CheckBoxPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override // androidx.preference.Preference
    public boolean callChangeListener(Object obj) {
        OnCheckedChangeListener onCheckedChangeListener = this.mOnChangeListener;
        if (onCheckedChangeListener != null) {
            return onCheckedChangeListener.onCheckedChanged(this, obj);
        }
        return super.callChangeListener(obj);
    }

    @Override // androidx.preference.CheckBoxPreference, androidx.preference.Preference
    public void onBindViewHolder(androidx.preference.PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        FlickerUtils.setFlickerName(preferenceViewHolder.findViewById(R.id.flicker), getKey());
    }

    public void setOnCheckedChangeWidgetListener(OnCheckedChangeListener onCheckedChangeListener) {
        this.mOnChangeListener = onCheckedChangeListener;
    }
}
