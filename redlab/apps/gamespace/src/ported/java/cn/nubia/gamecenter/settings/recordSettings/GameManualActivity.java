package cn.nubia.gamecenter.settings.recordSettings;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.Settings;
import cn.nubia.gamecenter.settings.R;
import cn.nubia.gamecenter.settings.compatible.GameKeysHelper;
import cn.nubia.gamecenter.settings.utils.LogUtil;

/* loaded from: classes.dex */
public class GameManualActivity extends Activity implements StartInfo {
    public static final int GSC_BARRAGE_MESSAGE_EXIT = 0;
    public static final int GSC_BARRAGE_MESSAGE_ON = 1;
    public static final String GSC_BARRAGE_MESSAGE_PREVIEW = "gsc_barrage_message_preview";
    private static final String KEY_ACTION = "cn.nubia.gamecenter.settings.action.GAME_CENTER_MANUAL_DETAIL";
    private static final String KEY_PREFIX = "prefix";
    private static final String KEY_START_TYPE = "gcs_start_type";
    private static final String KEY_TITLE = "title";
    private static final String TAG = "GameManualActivity";
    private Context mContext;
    private GameManualHelper m_helper;
    private HandlerThread mWorkHandlerThread = null;
    private Handler mWorkHandler = null;

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        requestWindowFeature(1);
        getWindow().setFlags(1024, 1024);
        setContentView(R.layout.gcs_gamecenter_game_settings_manual);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        getIntent().getStringExtra("title");
        this.m_helper = new GameManualHelper(this, getIntent().getStringExtra("title"));
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
        GameManualHelper gameManualHelper = this.m_helper;
        if (gameManualHelper != null) {
            gameManualHelper.onPause();
        }
        super.onPause();
    }

    @Override // android.app.Activity
    public void onResume() {
        GameManualHelper gameManualHelper = this.m_helper;
        if (gameManualHelper != null) {
            gameManualHelper.onResume();
        }
        super.onResume();
        this.mWorkHandler.post(new Runnable() { // from class: cn.nubia.gamecenter.settings.recordSettings.GameManualActivity.1
            @Override // java.lang.Runnable
            public void run() {
                String readNodeValue = GameKeysHelper.getDefault().readNodeValue(GameManualActivity.this.mContext);
                if (readNodeValue == null) {
                    readNodeValue = "0";
                }
                if ("0".equals(readNodeValue)) {
                    LogUtil.d(GameManualActivity.TAG, "game keys is closed,finish!");
                    GameManualActivity.this.finish();
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
        GameManualHelper gameManualHelper = this.m_helper;
        if (gameManualHelper != null) {
            return gameManualHelper.isTestMode();
        }
        return false;
    }

    @Override // android.app.Activity
    public void onStart() {
        Settings.Global.putInt(getContentResolver(), "gsc_barrage_message_preview", 1);
        super.onStart();
        LogUtil.d(TAG, "******onStart");
    }

    @Override // android.app.Activity
    public void onStop() {
        Settings.Global.putInt(getContentResolver(), "gsc_barrage_message_preview", 0);
        super.onResume();
    }
}
