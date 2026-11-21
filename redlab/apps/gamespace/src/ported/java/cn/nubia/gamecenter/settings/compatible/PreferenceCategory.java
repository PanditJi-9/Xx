package cn.nubia.gamecenter.settings.compatible;

import android.content.Context;
import android.util.AttributeSet;

/* loaded from: classes.dex */
public class PreferenceCategory extends androidx.preference.PreferenceCategory {
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
    }

    public PreferenceCategory(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
    }

    public PreferenceCategory(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    public PreferenceCategory(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public PreferenceCategory(Context context) {
        super(context);
    }

    @Override // androidx.preference.PreferenceCategory, androidx.preference.Preference
    public void onBindViewHolder(androidx.preference.PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        onBindViewHolder(new PreferenceViewHolder(preferenceViewHolder));
    }
}
