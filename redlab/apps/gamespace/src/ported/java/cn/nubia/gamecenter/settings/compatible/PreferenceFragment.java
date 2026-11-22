package cn.nubia.gamecenter.settings.compatible;

import android.content.ContentResolver;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;

/* JADX INFO: loaded from: classes.dex */
public class PreferenceFragment extends PreferenceFragmentCompat {
    private ContentResolver mContentResolver;

    public int getMetricsCategory() {
        return 5;
    }

    protected int getPreferenceScreenResId() {
        return -1;
    }

    public boolean onPreferenceTreeClick(Preference preference) {
        return false;
    }

    public boolean removePreference(String str) {
        return removePreference(getPreferenceScreen(), str);
    }

    boolean removePreference(PreferenceGroup preferenceGroup, String str) {
        int preferenceCount = preferenceGroup.getPreferenceCount();
        for (int i = 0; i < preferenceCount; i++) {
            androidx.preference.Preference preference = preferenceGroup.getPreference(i);
            if (TextUtils.equals(preference.getKey(), str)) {
                return preferenceGroup.removePreference(preference);
            }
            if ((preference instanceof PreferenceGroup) && removePreference((PreferenceGroup) preference, str)) {
                return true;
            }
        }
        return false;
    }

    protected ContentResolver getContentResolver() {
        FragmentActivity activity = getActivity();
        if (activity != null) {
            this.mContentResolver = activity.getContentResolver();
        }
        return this.mContentResolver;
    }

    @Override // androidx.preference.PreferenceFragmentCompat
    public void onCreatePreferences(Bundle bundle, String str) {
        int preferenceScreenResId = getPreferenceScreenResId();
        if (preferenceScreenResId > 0) {
            addPreferencesFromResource(preferenceScreenResId);
        }
    }

    @Override // androidx.preference.PreferenceFragmentCompat, androidx.preference.PreferenceManager.OnPreferenceTreeClickListener
    public boolean onPreferenceTreeClick(androidx.preference.Preference preference) {
        if (preference instanceof Preference) {
            return onPreferenceTreeClick((Preference) preference);
        }
        return super.onPreferenceTreeClick(preference);
    }
}
