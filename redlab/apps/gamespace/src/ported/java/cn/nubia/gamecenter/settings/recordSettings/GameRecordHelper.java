package cn.nubia.gamecenter.settings.recordSettings;

import android.view.View;
import android.widget.TextView;
import cn.nubia.gamecenter.settings.GameCenterTester;
import cn.nubia.gamecenter.settings.R;

/* loaded from: classes.dex */
public class GameRecordHelper implements GameCenterTester.TestModeFunctions {
    private static final String TAG = "GameRecordHelper";
    private final GameRecordActivity m_activity;
    private final String m_title;
    private View m_titleView;
    private View.OnClickListener m_titleClickListener = new View.OnClickListener() { // from class: cn.nubia.gamecenter.settings.recordSettings.GameRecordHelper.1
        @Override // android.view.View.OnClickListener
        public void onClick(View view) {
            GameRecordHelper.this.m_activity.finish();
        }
    };
    private boolean m_testMode = false;

    public void onPause() {
    }

    public void onResume() {
    }

    public GameRecordHelper(GameRecordActivity gameRecordActivity, String str) {
        this.m_activity = gameRecordActivity;
        this.m_title = str;
        init();
    }

    private void init() {
        init_testMode();
        getTitleView();
        setTitle();
    }

    private void setTitle() {
        TextView textView = (TextView) findViewById(R.id.titlebar_text);
        if (textView != null) {
            textView.setText(this.m_title);
        }
    }

    private View getTitleView() {
        if (this.m_titleView == null) {
            View findViewById = findViewById(R.id.left_name);
            this.m_titleView = findViewById;
            if (findViewById != null) {
                findViewById.setClickable(true);
                this.m_titleView.setOnClickListener(this.m_titleClickListener);
            }
        }
        return this.m_titleView;
    }

    public GameRecordActivity getActivity() {
        return this.m_activity;
    }

    public <T extends View> T findViewById(int i) {
        return (T) this.m_activity.findViewById(i);
    }

    private void init_testMode() {
        new GameCenterTester(this.m_activity, this);
    }

    public boolean isTestMode() {
        return this.m_testMode;
    }

    @Override // cn.nubia.gamecenter.settings.GameCenterTester.TestModeFunctions
    public void setTestMode() {
        this.m_testMode = true;
    }
}
