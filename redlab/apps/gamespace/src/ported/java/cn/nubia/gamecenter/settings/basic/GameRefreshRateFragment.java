package cn.nubia.gamecenter.settings.basic;

import android.os.Bundle;
import android.provider.Settings;
import cn.nubia.gamecenter.settings.R;
import cn.nubia.gamecenter.settings.compatible.PreferenceFragment;
import cn.nubia.gamecenter.settings.compatible.SwitchPreference;
import cn.nubia.gamecenter.settings.preference.GameCenterSwitchPreference;

/* JADX INFO: loaded from: classes.dex */
public class GameRefreshRateFragment extends PreferenceFragment implements SwitchPreference.OnPreferenceChangeListener {
    private static final String DB_GAMES_REFRESH_RATE = "game_refresh_rate_sc_switch";
    private static final int SWITCH_CLOSED_STATUS = 0;
    private static final int SWITCH_OPENED_STATUS = 1;
    private static final String TAG = "GameRefreshRateFragment";
    private GameCenterSwitchPreference mGameRefreshRate;

    @Override // androidx.preference.PreferenceFragmentCompat, androidx.fragment.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.gcs_game_refresh_rate);
        initGameRefreshRateView();
    }

    private void initGameRefreshRateView() {
        GameCenterSwitchPreference gameCenterSwitchPreference = (GameCenterSwitchPreference) findPreference(DB_GAMES_REFRESH_RATE);
        this.mGameRefreshRate = gameCenterSwitchPreference;
        gameCenterSwitchPreference.setOnPreferenceChangeListener(this);
        this.mGameRefreshRate.setChecked(getGameRefreshRateSwitch());
    }

    @Override // cn.nubia.gamecenter.settings.compatible.SwitchPreference.OnPreferenceChangeListener
    public boolean onPreferenceChange(SwitchPreference switchPreference, Object obj) {
        if (switchPreference != this.mGameRefreshRate) {
            return false;
        }
        boolean zBooleanValue = ((Boolean) obj).booleanValue();
        putGameRefreshRateSwitch(zBooleanValue);
        this.mGameRefreshRate.setChecked(zBooleanValue);
        return false;
    }

    private boolean getGameRefreshRateSwitch() {
        return Settings.Global.getInt(getContentResolver(), DB_GAMES_REFRESH_RATE, 0) != 0;
    }

    private void putGameRefreshRateSwitch(boolean z) {
        Settings.Global.putInt(getContentResolver(), DB_GAMES_REFRESH_RATE, z ? 1 : 0);
    }
}
