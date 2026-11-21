package cn.nubia.gamecenter.settings;

import android.app.Activity;
import android.view.View;
import android.widget.Toast;

/* loaded from: classes.dex */
public class GameCenterTester {
    private static final int FIVE_SECOND = 5000;
    private static final int ONE_SECOND = 1000;
    private static final String TAG = "GameCenterTester";
    private static final int TESTMODE_CLICK1_COUNT = 3;
    private static final int TESTMODE_CLICK2_COUNT = 3;
    private static final int TWO_SECOND = 2000;
    private final Activity m_activity;
    private final TestModeFunctions m_helper;
    private View m_titleView;
    private boolean m_bTestMode = false;
    private long m_lastClickTime = 0;
    private boolean m_testMode_longclicked = false;
    private int m_testMode_clickCount1 = 0;
    private int m_testMode_clickCount2 = 0;
    private final View.OnClickListener m_titleClickListener = new View.OnClickListener() { // from class: cn.nubia.gamecenter.settings.GameCenterTester.1
        @Override // android.view.View.OnClickListener
        public void onClick(View view) {
            if (GameCenterTester.this.m_bTestMode) {
                return;
            }
            if (GameCenterTester.this.m_lastClickTime == 0 || System.currentTimeMillis() - GameCenterTester.this.m_lastClickTime >= 2000) {
                GameCenterTester.this.m_testMode_clickCount1 = 1;
                GameCenterTester.this.m_testMode_clickCount2 = 0;
                GameCenterTester.this.m_testMode_longclicked = false;
            } else if (!GameCenterTester.this.m_testMode_longclicked) {
                GameCenterTester.access$308(GameCenterTester.this);
            } else {
                GameCenterTester.access$408(GameCenterTester.this);
            }
            GameCenterTester.this.m_lastClickTime = System.currentTimeMillis();
            if (GameCenterTester.this.m_testMode_clickCount1 == 3 && GameCenterTester.this.m_testMode_clickCount2 == 3) {
                GameCenterTester.this.enableTestMode();
            }
        }
    };
    private final View.OnLongClickListener m_titleLongClickListener = new View.OnLongClickListener() { // from class: cn.nubia.gamecenter.settings.GameCenterTester.2
        @Override // android.view.View.OnLongClickListener
        public boolean onLongClick(View view) {
            if (GameCenterTester.this.m_bTestMode) {
                return true;
            }
            if (GameCenterTester.this.m_testMode_clickCount1 != 3 || GameCenterTester.this.m_lastClickTime == 0 || System.currentTimeMillis() - GameCenterTester.this.m_lastClickTime >= 5000) {
                GameCenterTester.this.m_testMode_clickCount1 = 0;
                GameCenterTester.this.m_testMode_clickCount2 = 0;
                GameCenterTester.this.m_testMode_longclicked = false;
            } else {
                GameCenterTester.this.m_testMode_longclicked = true;
            }
            GameCenterTester.this.m_lastClickTime = System.currentTimeMillis();
            return true;
        }
    };

    public interface TestModeFunctions {
        void setTestMode();
    }

    static /* synthetic */ int access$308(GameCenterTester gameCenterTester) {
        int i = gameCenterTester.m_testMode_clickCount1;
        gameCenterTester.m_testMode_clickCount1 = i + 1;
        return i;
    }

    static /* synthetic */ int access$408(GameCenterTester gameCenterTester) {
        int i = gameCenterTester.m_testMode_clickCount2;
        gameCenterTester.m_testMode_clickCount2 = i + 1;
        return i;
    }

    public GameCenterTester(Activity activity, TestModeFunctions testModeFunctions) {
        this.m_activity = activity;
        this.m_helper = testModeFunctions;
        init();
    }

    private void init() {
        this.m_bTestMode = false;
        if (getTitleView() != null) {
            getTitleView().setOnClickListener(this.m_titleClickListener);
            getTitleView().setOnLongClickListener(this.m_titleLongClickListener);
        }
    }

    private View getTitleView() {
        if (this.m_titleView == null) {
            this.m_titleView = this.m_activity.findViewById(R.id.titlebar_text);
        }
        return this.m_titleView;
    }

    public void enableTestMode() {
        this.m_bTestMode = true;
        this.m_helper.setTestMode();
        Toast.makeText(this.m_activity, "enable test mode", 0).show();
    }
}
