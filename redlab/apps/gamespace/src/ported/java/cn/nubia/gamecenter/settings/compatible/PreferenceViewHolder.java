package cn.nubia.gamecenter.settings.compatible;

import android.view.View;

/* loaded from: classes.dex */
public class PreferenceViewHolder {
    private static final String TAG = "PreferenceViewHolder";
    private final androidx.preference.PreferenceViewHolder m_holder;

    PreferenceViewHolder(androidx.preference.PreferenceViewHolder preferenceViewHolder) {
        this.m_holder = preferenceViewHolder;
    }

    public View findViewById(int i) {
        androidx.preference.PreferenceViewHolder preferenceViewHolder = this.m_holder;
        if (preferenceViewHolder == null) {
            return null;
        }
        return preferenceViewHolder.findViewById(i);
    }
}
