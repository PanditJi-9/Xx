package cn.nubia.gamecenter.settings.preference;

import android.content.Context;
import android.provider.Settings;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.TextView;
import cn.nubia.gamecenter.settings.R;
import cn.nubia.gamecenter.settings.compatible.ListPreference;
import cn.nubia.gamecenter.settings.compatible.PreferenceViewHolder;
import cn.nubia.settings.trackclient.NubiaTrackManager;

/* loaded from: classes.dex */
public class GameSpaceListPreference extends ListPreference implements ListPreference.OnPreferenceChangeListener, ListPreference.OnPreferenceClickListener {
    private static final String GAME_MODE_MANUAL_DURATION_DB_NAME = "nubia_game_manual_record_time";
    private static final String GAME_MODE_MANUAL_QUALITY_DB_NAME = "db_game_video_quality_manual";
    private static final String GAME_MODE_VIDEO_QUALITY_DB_NAME = "db_game_video_quality";
    private static final String KEY_GAME_MODE_MANUAL_DURATION = "gamemode_manual_video_duration";
    private static final String KEY_GAME_MODE_MANUAL_QUALITY = "gamemode_manual_video_quality";
    private static final String KEY_GAME_MODE_VIDEO_QUALITY = "gamemode_video_quality";
    private static final String TAG = "GameSpaceListPreference";
    private static final int VIDEO_QUALITY_HIGH = 1;
    private static final int VIDEO_QUALITY_LOW = 0;
    private Callback mCallback;
    private Context mContext;
    private TextView mDesc;
    private ImageView mDriver;
    private TextView mTitle;
    private String m_startType;

    public interface Callback {
        void updateDefaultPosition(boolean z);
    }

    public GameSpaceListPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.m_startType = "";
        this.mContext = context;
        setLayoutResource(R.layout.gcs_list_preference);
        setWidgetLayoutResource(R.layout.arrow_layout);
        setOnPreferenceChangeListener(this);
        setOnPreferenceClickListener(this);
    }

    @Override // cn.nubia.gamecenter.settings.compatible.ListPreference
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        this.mTitle = (TextView) preferenceViewHolder.findViewById(R.id.system_keys_title);
        this.mDesc = (TextView) preferenceViewHolder.findViewById(R.id.system_keys_desc);
        this.mDriver = (ImageView) preferenceViewHolder.findViewById(R.id.system_keys_driver);
        this.mTitle.setText(getTitle());
        refreshDesc(false);
    }

    public void hideDriver() {
        ImageView imageView = this.mDriver;
        if (imageView != null) {
            imageView.setVisibility(8);
        }
    }

    public void setTitle(String str) {
        TextView textView = this.mTitle;
        if (textView != null) {
            textView.setText(str);
        }
    }

    public void setDesc(String str) {
        TextView textView = this.mDesc;
        if (textView != null) {
            textView.setText(str);
        }
    }

    public void refreshDesc(boolean z) {
        if (this.mDesc == null) {
            return;
        }
        String key = getKey();
        if (key.equals(KEY_GAME_MODE_VIDEO_QUALITY)) {
            refreshLongPressFunDesc(getGameVideoQualityValue(), key, z);
        } else if (key.equals(KEY_GAME_MODE_MANUAL_DURATION)) {
            refreshLongPressFunDesc(getGameManualDurationValue(), key, z);
        } else if (key.equals(KEY_GAME_MODE_MANUAL_QUALITY)) {
            refreshLongPressFunDesc(getGameManualQualityValue(), key, z);
        }
    }

    private void refreshLongPressFunDesc(int i, String str, boolean z) {
        if (str.equals(KEY_GAME_MODE_VIDEO_QUALITY) || str.equals(KEY_GAME_MODE_MANUAL_QUALITY)) {
            if (i == 0) {
                setDesc(this.mContext.getString(R.string.game_space_video_quality_low));
            } else if (i == 1) {
                setDesc(this.mContext.getString(R.string.game_space_video_quality_high));
            }
        } else if (str.equals(KEY_GAME_MODE_MANUAL_DURATION)) {
            if (i == 0) {
                setDesc(this.mContext.getString(R.string.gcs_game_manual_record_duration_0));
            } else if (i == 1) {
                setDesc(this.mContext.getString(R.string.gcs_game_manual_record_duration_1));
            } else if (i == 2) {
                setDesc(this.mContext.getString(R.string.gcs_game_manual_record_duration_2));
            } else if (i == 3) {
                setDesc(this.mContext.getString(R.string.gcs_game_manual_record_duration_3));
            }
        }
        if (z) {
            setIndex(i, str);
        }
    }

    private void setIndex(int i, String str) {
        if (str.equals(KEY_GAME_MODE_VIDEO_QUALITY) || str.equals(KEY_GAME_MODE_MANUAL_QUALITY)) {
            if (i == 0) {
                setValueIndex(0);
                return;
            } else {
                if (i == 1) {
                    setValueIndex(1);
                    return;
                }
                return;
            }
        }
        if (str.equals(KEY_GAME_MODE_MANUAL_DURATION)) {
            if (i == 0) {
                setValueIndex(0);
                return;
            }
            if (i == 1) {
                setValueIndex(1);
            } else if (i == 2) {
                setValueIndex(2);
            } else if (i == 3) {
                setValueIndex(3);
            }
        }
    }

    @Override // cn.nubia.gamecenter.settings.compatible.ListPreference.OnPreferenceClickListener
    public boolean onPreferenceClick(ListPreference listPreference) {
        refreshDesc(true);
        return true;
    }

    @Override // cn.nubia.gamecenter.settings.compatible.ListPreference.OnPreferenceChangeListener
    public boolean onPreferenceChange(ListPreference listPreference, Object obj) {
        String key = getKey();
        int parseInt = Integer.parseInt((String) obj);
        if (key.equals(KEY_GAME_MODE_VIDEO_QUALITY)) {
            if (parseInt == 0) {
                NubiaTrackManager.getInstance().sendEvent("com.android.settings", "gamespace_video_quality_status", "视频质量", "标清");
                setGameVideoQualityValue(0);
            } else if (parseInt == 1) {
                NubiaTrackManager.getInstance().sendEvent("com.android.settings", "gamespace_video_quality_status", "视频质量", "高清");
                setGameVideoQualityValue(1);
            }
        } else if (key.equals(KEY_GAME_MODE_MANUAL_DURATION)) {
            setGameManualDurationValue(parseInt);
        } else if (key.equals(KEY_GAME_MODE_MANUAL_QUALITY)) {
            setGameManualQualityValue(parseInt);
        }
        refreshDesc(true);
        return true;
    }

    private int getGameVideoQualityValue() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), GAME_MODE_VIDEO_QUALITY_DB_NAME + this.m_startType, 0);
    }

    private int getGameManualDurationValue() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), GAME_MODE_MANUAL_DURATION_DB_NAME, 1);
    }

    private int getGameManualQualityValue() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), GAME_MODE_MANUAL_QUALITY_DB_NAME, 0);
    }

    private void setGameVideoQualityValue(int i) {
        Settings.Global.putInt(this.mContext.getContentResolver(), GAME_MODE_VIDEO_QUALITY_DB_NAME + this.m_startType, i);
    }

    private void setGameManualDurationValue(int i) {
        Settings.Global.putInt(this.mContext.getContentResolver(), GAME_MODE_MANUAL_DURATION_DB_NAME, i);
    }

    private void setGameManualQualityValue(int i) {
        Settings.Global.putInt(this.mContext.getContentResolver(), GAME_MODE_MANUAL_QUALITY_DB_NAME, i);
    }

    public void setSystemKeysFragment(Callback callback) {
        this.mCallback = callback;
    }

    public void setStartType(String str) {
        this.m_startType = str;
        refreshDesc(false);
    }
}
