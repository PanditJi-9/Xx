package cn.nubia.gamecenter.settings.compatible;

import android.content.Context;
import android.util.AttributeSet;
import cn.nubia.gamecenter.settings.R;
import cn.nubia.gamecenter.settings.utils.FlickerUtils;

/* loaded from: classes.dex */
public class SwitchPreference extends androidx.preference.SwitchPreference {
    private OnPreferenceChangeListener mOnChangeListener;

    public interface OnPreferenceChangeListener {
        boolean onPreferenceChange(SwitchPreference switchPreference, Object obj);
    }

    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
    }

    public SwitchPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override // androidx.preference.Preference
    public boolean callChangeListener(Object obj) {
        OnPreferenceChangeListener onPreferenceChangeListener = this.mOnChangeListener;
        if (onPreferenceChangeListener != null) {
            return onPreferenceChangeListener.onPreferenceChange(this, obj);
        }
        return super.callChangeListener(obj);
    }

    public void setOnPreferenceChangeListener(OnPreferenceChangeListener onPreferenceChangeListener) {
        this.mOnChangeListener = onPreferenceChangeListener;
    }

    @Override // androidx.preference.SwitchPreference, androidx.preference.Preference
    public void onBindViewHolder(androidx.preference.PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        onBindViewHolder(new PreferenceViewHolder(preferenceViewHolder));
        FlickerUtils.setFlickerName(preferenceViewHolder.findViewById(R.id.flicker), getKey());
    }
}
