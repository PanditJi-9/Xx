package cn.nubia.gamecenter.settings.recordSettings;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import cn.nubia.gamecenter.settings.R;
import cn.nubia.gamecenter.settings.compatible.GameKeysHelper;
import cn.nubia.gamecenter.settings.utils.LogUtil;

/* loaded from: classes.dex */
public class GameRecordActivity extends Activity implements StartInfo {
    private static final String KEY_ACTION = "cn.nubia.gamecenter.settings.action.GAME_CENTER_RECORD_DETAIL";
    private static final String KEY_PREFIX = "prefix";
    private static final String KEY_START_TYPE = "gcs_start_type";
    private static final String KEY_TITLE = "title";
    private static final String TAG = "GameRecordActivity";
    private Context mContext;
    private GameRecordHelper m_helper;
    private HandlerThread mWorkHandlerThread = null;
    private Handler mWorkHandler = null;

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        requestWindowFeature(1);
        getWindow().setFlags(1024, 1024);
        setContentView(R.layout.gcs_gamecenter_game_settings_main);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        getIntent().getStringExtra("title");
        this.m_helper = new GameRecordHelper(this, getIntent().getStringExtra("title"));
        HandlerThread handlerThread = new HandlerThread(TAG);
        this.mWorkHandlerThread = handlerThread;
        handlerThread.start();
        this.mWorkHandler = new Handler(this.mWorkHandlerThread.getLooper());
        this.mContext = getApplicationContext();
    }

    @Override // android.app.Activity
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
    }

    @Override // android.app.Activity
    public void onPause() {
        GameRecordHelper gameRecordHelper = this.m_helper;
        if (gameRecordHelper != null) {
            gameRecordHelper.onPause();
        }
        super.onPause();
    }

    @Override // android.app.Activity
    public void onResume() {
        GameRecordHelper gameRecordHelper = this.m_helper;
        if (gameRecordHelper != null) {
            gameRecordHelper.onResume();
        }
        super.onResume();
        this.mWorkHandler.post(new Runnable() { // from class: cn.nubia.gamecenter.settings.recordSettings.GameRecordActivity.1
            @Override // java.lang.Runnable
            public void run() {
                String readNodeValue = GameKeysHelper.getDefault().readNodeValue(GameRecordActivity.this.mContext);
                if (readNodeValue == null) {
                    readNodeValue = "0";
                }
                if ("0".equals(readNodeValue)) {
                    LogUtil.d(GameRecordActivity.TAG, "game keys is closed,finish!");
                    GameRecordActivity.this.finish();
                }
            }
        });
    }

    @Override // android.app.Activity
    public void onDestroy() {
        HandlerThread handlerThread = this.mWorkHandlerThread;
        if (handlerThread != null) {
            handlerThread.quit();
        }
        super.onDestroy();
    }

    public static final void startActivity(Context context, String str, String str2) {
        Intent intent = new Intent(KEY_ACTION);
        intent.putExtra("title", str);
        intent.putExtra(KEY_PREFIX, str2);
        context.startActivity(intent);
    }

    @Override // cn.nubia.gamecenter.settings.recordSettings.StartInfo
    public String getStartType() {
        return getIntent().getStringExtra(KEY_PREFIX);
    }

    @Override // cn.nubia.gamecenter.settings.recordSettings.StartInfo
    public boolean isTestMode() {
        GameRecordHelper gameRecordHelper = this.m_helper;
        if (gameRecordHelper != null) {
            return gameRecordHelper.isTestMode();
        }
        return false;
    }
}
