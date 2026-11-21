package cn.nubia.gamecenter.settings.compatible;

import android.content.Context;
import android.util.AttributeSet;
import androidx.preference.Preference;

/* loaded from: classes.dex */
public class ListPreference extends androidx.preference.ListPreference {
    private Preference.OnPreferenceClickListener mBaseOnClickListener;
    private OnPreferenceChangeListener mOnChangeListener;
    private OnPreferenceClickListener mOnClickListener;

    public interface OnPreferenceChangeListener {
        boolean onPreferenceChange(ListPreference listPreference, Object obj);
    }

    public interface OnPreferenceClickListener {
        boolean onPreferenceClick(ListPreference listPreference);
    }

    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
    }

    public ListPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mBaseOnClickListener = new Preference.OnPreferenceClickListener() { // from class: cn.nubia.gamecenter.settings.compatible.ListPreference.1
            @Override // androidx.preference.Preference.OnPreferenceClickListener
            public boolean onPreferenceClick(androidx.preference.Preference preference) {
                if (ListPreference.this.mOnClickListener != null) {
                    return ListPreference.this.mOnClickListener.onPreferenceClick((ListPreference) preference);
                }
                return false;
            }
        };
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

    public void setOnPreferenceClickListener(OnPreferenceClickListener onPreferenceClickListener) {
        this.mOnClickListener = onPreferenceClickListener;
        setOnPreferenceClickListener(this.mBaseOnClickListener);
    }

    @Override // androidx.preference.Preference
    public void onBindViewHolder(androidx.preference.PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        onBindViewHolder(new PreferenceViewHolder(preferenceViewHolder));
    }
}
