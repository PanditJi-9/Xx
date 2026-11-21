package cn.nubia.gamecenter.settings.summary;

import android.content.Context;
import android.graphics.drawable.Drawable;
import cn.nubia.gamecenter.settings.R;
import cn.nubia.gamecenter.settings.summary.entities.GameAppInfo;
import cn.nubia.gamecenter.settings.summary.entities.GameTimeInfo;
import cn.nubia.gamecenter.settings.summary.presenter.ICallback;
import cn.nubia.gamecenter.settings.summary.presenter.IGameParmsPresenter;
import cn.nubia.gamecenter.settings.utils.LogUtil;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class SummaryDataTester implements IGameParmsPresenter {
    private static final long ONE_DAY = 86400000;
    private static final long ONE_MINITE = 60000;
    private static final long ONE_SECOND = 1000;
    private static final String TAG = "SummaryDataTester";
    private final ICallback m_Callback;
    private final Context m_context;
    List<List<GameAppInfo>> m_lists;
    GameTimeInfo[] m_timeInfo;

    @Override // cn.nubia.gamecenter.settings.summary.presenter.IGameParmsPresenter
    public void stopLoadGameParms() {
    }

    public SummaryDataTester(Context context, ICallback iCallback) {
        this.m_context = context;
        this.m_Callback = iCallback;
        init();
    }

    private void init() {
        ArrayList arrayList = new ArrayList();
        this.m_lists = arrayList;
        arrayList.add(init_createGameAppInfo(1));
        this.m_lists.add(init_createGameAppInfo(2));
        this.m_timeInfo = new GameTimeInfo[]{init_createGameTimeInfo(0), init_createGameTimeInfo(1)};
    }

    public void updateData(GameTimeInfo[] gameTimeInfoArr, List<List<GameAppInfo>> list) {
        if (gameTimeInfoArr == null || 2 != gameTimeInfoArr.length || list == null || list.size() != 2) {
            LogUtil.w(TAG, "updateData, invalid infos !!!");
        } else {
            this.m_Callback.gameParms(gameTimeInfoArr, list);
        }
    }

    private GameTimeInfo init_createGameTimeInfo(int i) {
        GameTimeInfo gameTimeInfo = new GameTimeInfo();
        if (i == 0) {
            gameTimeInfo.mGameTimeSpan = "1.31";
            gameTimeInfo.mMaxTimePkgName = "com.android.settings";
            gameTimeInfo.label = getFirstAppLable(0);
            gameTimeInfo.mMaxTimeAppIcon = getFirstAppIcon(0);
            gameTimeInfo.mGameTotalTime = getTotalTime(0);
            gameTimeInfo.mGameMaxTime = getFirstAppTime(0);
            gameTimeInfo.mSuggestType = 1;
        } else {
            gameTimeInfo.mGameTimeSpan = "1.18-1.24";
            gameTimeInfo.mMaxTimePkgName = "cn.nubia.vigi";
            gameTimeInfo.label = getFirstAppLable(1);
            gameTimeInfo.mMaxTimeAppIcon = getFirstAppIcon(1);
            gameTimeInfo.mGameTotalTime = getTotalTime(1);
            gameTimeInfo.mGameMaxTime = getFirstAppTime(1);
            gameTimeInfo.mSuggestType = 2;
        }
        return gameTimeInfo;
    }

    private List<GameAppInfo> init_createGameAppInfo(int i) {
        ArrayList arrayList = new ArrayList();
        if (i == 1) {
            arrayList.add(init_createGameAppInfo_day(1));
        } else {
            arrayList.add(init_createGameAppInfo_week(1));
            arrayList.add(init_createGameAppInfo_week(2));
            arrayList.add(init_createGameAppInfo_week(3));
            arrayList.add(init_createGameAppInfo_week(4));
            arrayList.add(init_createGameAppInfo_week(5));
            arrayList.add(init_createGameAppInfo_week(6));
            arrayList.add(init_createGameAppInfo_week(7));
        }
        return arrayList;
    }

    private GameAppInfo init_createGameAppInfo_day(int i) {
        GameAppInfo gameAppInfo = new GameAppInfo();
        if (i == 1) {
            gameAppInfo.icon = this.m_context.getDrawable(R.drawable.gcs_help_1);
            gameAppInfo.label = "绝地求生 刺激战场";
            gameAppInfo.totalTimeInForeground = 5128000L;
        } else if (i == 2) {
            gameAppInfo.icon = this.m_context.getDrawable(R.drawable.gcs_help_8);
            gameAppInfo.label = "王者荣耀";
            gameAppInfo.totalTimeInForeground = 9000000L;
        } else if (i == 3) {
            gameAppInfo.icon = this.m_context.getDrawable(R.drawable.gcs_help_3);
            gameAppInfo.label = "绝地求生 全军出击";
            gameAppInfo.totalTimeInForeground = 3000000L;
        } else {
            gameAppInfo.icon = null;
            gameAppInfo.label = "未定义";
            gameAppInfo.totalTimeInForeground = 20L;
        }
        return gameAppInfo;
    }

    private GameAppInfo init_createGameAppInfo_week(int i) {
        GameAppInfo gameAppInfo = new GameAppInfo();
        if (i == 1) {
            gameAppInfo.icon = this.m_context.getDrawable(R.drawable.gcs_help_1);
            gameAppInfo.label = "绝地求生 刺激战场";
            gameAppInfo.totalTimeInForeground = 172800000L;
        } else if (i == 2) {
            gameAppInfo.icon = this.m_context.getDrawable(R.drawable.gcs_help_8);
            gameAppInfo.label = "王者荣耀";
            gameAppInfo.totalTimeInForeground = 24000000L;
        } else if (i == 3) {
            gameAppInfo.icon = this.m_context.getDrawable(R.drawable.gcs_help_3);
            gameAppInfo.label = "绝地求生 全军出击";
            gameAppInfo.totalTimeInForeground = 18000000L;
        } else if (i == 4) {
            gameAppInfo.icon = this.m_context.getDrawable(R.drawable.gcs_help_4);
            gameAppInfo.label = "王者荣耀";
            gameAppInfo.totalTimeInForeground = 12000000L;
        } else if (i == 5) {
            gameAppInfo.icon = this.m_context.getDrawable(R.drawable.gcs_help_5);
            gameAppInfo.label = "第五人格";
            gameAppInfo.totalTimeInForeground = 6000000L;
        } else if (i == 6) {
            gameAppInfo.icon = this.m_context.getDrawable(R.drawable.gcs_help_6);
            gameAppInfo.label = "荒野行动";
            gameAppInfo.totalTimeInForeground = 3000000L;
        } else {
            gameAppInfo.icon = null;
            gameAppInfo.label = "未定义";
            gameAppInfo.totalTimeInForeground = 0L;
        }
        return gameAppInfo;
    }

    private String getFirstAppLable(int i) {
        List<GameAppInfo> appList = getAppList(i);
        if (appList == null || appList.size() == 0 || appList.get(0) == null) {
            return "";
        }
        return appList.get(0).label;
    }

    private Drawable getFirstAppIcon(int i) {
        List<GameAppInfo> appList = getAppList(i);
        if (appList == null || appList.size() == 0 || appList.get(0) == null) {
            return null;
        }
        return appList.get(0).icon;
    }

    private long getFirstAppTime(int i) {
        List<GameAppInfo> appList = getAppList(i);
        if (appList == null || appList.size() == 0 || appList.get(0) == null) {
            return 0L;
        }
        return appList.get(0).totalTimeInForeground;
    }

    private long getTotalTime(int i) {
        List<GameAppInfo> appList = getAppList(i);
        long j = 0;
        if (appList != null && appList.size() != 0 && appList.get(0) != null) {
            for (int i2 = 0; i2 < appList.size(); i2++) {
                if (appList.get(0) != null) {
                    j += appList.get(i2).totalTimeInForeground;
                }
            }
        }
        return j;
    }

    private List<GameAppInfo> getAppList(int i) {
        List<List<GameAppInfo>> list = this.m_lists;
        if (list == null || list.size() <= i) {
            return null;
        }
        return this.m_lists.get(i);
    }

    @Override // cn.nubia.gamecenter.settings.summary.presenter.IGameParmsPresenter
    public void loadGameParms() {
        updateData(this.m_timeInfo, this.m_lists);
    }
}
