package cn.nubia.gamecenter.settings.summary;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.format.DateUtils;
import cn.nubia.gamecenter.settings.summary.entities.GameAppInfo;
import cn.nubia.gamecenter.settings.summary.entities.GameTimeInfo;
import cn.nubia.gamecenter.settings.utils.LogUtil;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/* loaded from: classes.dex */
public class SummaryDataHelper {
    private static final int INDEX_DAY = 0;
    private static final int INDEX_WEEK = 1;
    private static final int MAX_DAY = 1440;
    private static final int MAX_WEEK = 10080;
    public static final int MODE_DAY = 1;
    public static final int MODE_UNDEFINE = 0;
    public static final int MODE_WEEK = 2;
    private static final long ONE_MINITE = 60000;
    private static final String TAG = "SummaryDataHelper";
    private List<ModeChangeListener> mOnModeChangeListeners;
    private GameTimeInfo[] m_infos;
    private List<List<GameAppInfo>> m_lists;
    private int m_mode = 0;

    public enum LEVEL {
        NORMAL,
        TIRED,
        EXCESS
    }

    public interface ModeChangeListener {
        void onChange(int i);
    }

    public void setMode(int i) {
        if (this.m_mode == i) {
            return;
        }
        this.m_mode = i;
        dispatchOnChangeSelected(i);
    }

    public int getMode() {
        return this.m_mode;
    }

    public int getMaxTimeRange() {
        int i = this.m_mode;
        if (1 == i) {
            return MAX_DAY;
        }
        if (2 == i) {
            return MAX_WEEK;
        }
        return 0;
    }

    private List<GameAppInfo> getCurrentList() {
        List<List<GameAppInfo>> list = this.m_lists;
        if (list == null) {
            return null;
        }
        if (this.m_mode == 2) {
            if (list.size() <= 1) {
                return null;
            }
            return this.m_lists.get(1);
        }
        if (list.size() <= 0) {
            return null;
        }
        return this.m_lists.get(0);
    }

    private GameTimeInfo getCurrentInfo() {
        GameTimeInfo[] gameTimeInfoArr = this.m_infos;
        if (gameTimeInfoArr == null) {
            return null;
        }
        if (this.m_mode == 2) {
            if (gameTimeInfoArr.length <= 1) {
                return null;
            }
            LogUtil.d(TAG, "getCurrentInfo, INDEX_WEEK");
            return this.m_infos[1];
        }
        if (gameTimeInfoArr.length <= 0) {
            return null;
        }
        LogUtil.d(TAG, "getCurrentInfo, INDEX_DAY");
        return this.m_infos[0];
    }

    public String getRangeText(Context context) {
        String str;
        if (getCurrentInfo() == null || (str = getCurrentInfo().mGameTimeSpan) == null || str.isEmpty()) {
            return "";
        }
        int[][] parseToMonthDayFormat = parseToMonthDayFormat(str);
        if (parseToMonthDayFormat != null) {
            if (parseToMonthDayFormat.length == 1) {
                int[] iArr = parseToMonthDayFormat[0];
                if (iArr.length == 2) {
                    str = formatDate(context, iArr[0], iArr[1]);
                }
            }
            if (parseToMonthDayFormat.length == 2) {
                int[] iArr2 = parseToMonthDayFormat[0];
                if (iArr2.length == 2 && parseToMonthDayFormat[1].length == 2) {
                    StringBuilder append = new StringBuilder().append(formatDate(context, iArr2[0], iArr2[1])).append("-");
                    int[] iArr3 = parseToMonthDayFormat[1];
                    str = append.append(formatDate(context, iArr3[0], iArr3[1])).toString();
                }
            }
            str = "found it but data error!";
        }
        return " (" + str + ")";
    }

    private String formatDate(Context context, int i, int i2) {
        Calendar calendar = Calendar.getInstance();
        LogUtil.d(TAG, "SummaryDataHelper formatDate：年:" + calendar.get(1) + ",月:" + calendar.get(2) + ",日:" + calendar.get(5) + ",毫秒:" + calendar.get(14) + ",完整毫秒:" + calendar.getTimeInMillis());
        calendar.set(2, i - 1);
        calendar.set(5, i2);
        return DateUtils.formatDateTime(context, calendar.getTimeInMillis(), 526360);
    }

    private int[][] parseToMonthDayFormat(String str) {
        int[] iArr = null;
        if (str != null && !str.isEmpty()) {
            String[] split = str.split("-");
            LogUtil.d(TAG, " part = " + split);
            if (split != null && split.length <= 2) {
                if (split.length == 1) {
                    String str2 = split[0];
                    if (str2 != null && !str2.isEmpty()) {
                        String[] split2 = str.split("\\.");
                        LogUtil.d(TAG, "SummaryDataHelper partDate = " + split2);
                        if (split2 != null && split2.length == 2) {
                            return new int[][]{new int[]{Integer.parseInt(split2[0]), Integer.parseInt(split2[1])}};
                        }
                    }
                    return null;
                }
                int[][] parseToMonthDayFormat = parseToMonthDayFormat(split[0]);
                int[][] parseToMonthDayFormat2 = parseToMonthDayFormat(split[1]);
                int[] iArr2 = (parseToMonthDayFormat == null || parseToMonthDayFormat.length == 0) ? null : parseToMonthDayFormat[0];
                if (parseToMonthDayFormat2 != null && parseToMonthDayFormat2.length != 0) {
                    iArr = parseToMonthDayFormat2[0];
                }
                return new int[][]{iArr2, iArr};
            }
        }
        return null;
    }

    public void addOnChangeListener(ModeChangeListener modeChangeListener) {
        if (this.mOnModeChangeListeners == null) {
            this.mOnModeChangeListeners = new ArrayList();
        }
        this.mOnModeChangeListeners.add(modeChangeListener);
    }

    private void dispatchOnChangeSelected(int i) {
        List<ModeChangeListener> list = this.mOnModeChangeListeners;
        if (list != null) {
            int size = list.size();
            for (int i2 = 0; i2 < size; i2++) {
                ModeChangeListener modeChangeListener = this.mOnModeChangeListeners.get(i2);
                if (modeChangeListener != null) {
                    modeChangeListener.onChange(i);
                }
            }
        }
    }

    public LEVEL getKeywordLevel() {
        if (getCurrentInfo() == null) {
            return LEVEL.NORMAL;
        }
        int i = getCurrentInfo().mSuggestType;
        if (i == 1) {
            return LEVEL.TIRED;
        }
        if (i == 2) {
            return LEVEL.EXCESS;
        }
        return LEVEL.NORMAL;
    }

    public static int msToMinite(long j) {
        int i = (int) ((59999 + j) / 60000);
        if (j <= 0 || j >= 60000) {
            return i;
        }
        return 1;
    }

    private int getTotalTimeReal() {
        if (getCurrentInfo() == null) {
            return 0;
        }
        LogUtil.d(TAG, "getTotalTimeReal, time = " + getCurrentInfo().mGameTotalTime);
        return msToMinite(getCurrentInfo().mGameTotalTime);
    }

    public int getTotalTime() {
        return adjustTimeToRange(getTotalTimeReal());
    }

    private int adjustTimeToRange(int i) {
        int maxTimeRange = getMaxTimeRange();
        return i > maxTimeRange ? maxTimeRange : i;
    }

    public Drawable getMostIcon() {
        if (getCurrentInfo() == null) {
            return null;
        }
        return getCurrentInfo().mMaxTimeAppIcon;
    }

    public String getMostTitle() {
        if (getCurrentInfo() == null) {
            return null;
        }
        return getCurrentInfo().label;
    }

    public int getMostTime() {
        if (getCurrentInfo() == null) {
            return 0;
        }
        long j = getCurrentInfo().mGameMaxTime;
        LogUtil.d(TAG, "getMostTime, time = " + j);
        return (int) (((j / 1000) + 59) / 60);
    }

    public int getRankPercent(int i) {
        return getAppPercent(getTotalTimeReal(), i - 1);
    }

    public String getRankName(int i) {
        return getAppName(i - 1);
    }

    public void setInfo(GameTimeInfo[] gameTimeInfoArr) {
        this.m_infos = gameTimeInfoArr;
    }

    public void setList(List<List<GameAppInfo>> list) {
        this.m_lists = list;
        dispatchOnChangeSelected(this.m_mode);
    }

    private String getAppName(int i) {
        List<GameAppInfo> currentList = getCurrentList();
        if (currentList == null || currentList.size() <= i) {
            return "";
        }
        return currentList.get(i).label;
    }

    private int getAppPercent(int i, int i2) {
        List<GameAppInfo> currentList = getCurrentList();
        if (currentList != null && currentList.size() > i2) {
            long j = currentList.get(i2).totalTimeInForeground;
            int i3 = (int) ((j / 1000) / 60);
            if (j != 0 && i != 0) {
                return (i3 * 100) / i;
            }
        }
        return 0;
    }

    public int getAppCount() {
        List<GameAppInfo> currentList = getCurrentList();
        if (currentList == null) {
            return 0;
        }
        return currentList.size();
    }

    public GameAppInfo getAppItem(int i) {
        List<GameAppInfo> currentList = getCurrentList();
        if (currentList == null || currentList.size() <= i) {
            return null;
        }
        return currentList.get(i);
    }
}
