package cn.nubia.gamecenter.settings.preference;

import android.R;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;
import cn.nubia.gamecenter.settings.compatible.PreferenceCategory;
import cn.nubia.gamecenter.settings.compatible.PreferenceViewHolder;

/* loaded from: classes.dex */
public class GameRecordPreferenceCategory extends PreferenceCategory {
    private static final String TAG = "GameRecordPreferenceCategory";
    private Context mContext;

    public GameRecordPreferenceCategory(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mContext = context;
    }

    @Override // cn.nubia.gamecenter.settings.compatible.PreferenceCategory
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        TextView textView = (TextView) preferenceViewHolder.findViewById(R.id.title);
        if (textView != null) {
            textView.setTextColor(this.mContext.getResources().getColor(cn.nubia.gamecenter.settings.R.color.game_space_settings_state_color));
        }
    }
}
