package cn.nubia.gamecenter.settings.summary.presenter;

import cn.nubia.gamecenter.settings.summary.entities.GameAppInfo;
import cn.nubia.gamecenter.settings.summary.entities.GameTimeInfo;
import cn.nubia.gamecenter.settings.summary.entities.OneGameTimeAndLaunchTimesInfo;
import java.util.List;

/* loaded from: classes.dex */
public interface IGameParmsCallback {
    void gameParmsCallback(GameTimeInfo[] gameTimeInfoArr, List<List<GameAppInfo>> list);

    void oneGameParmsCallback(OneGameTimeAndLaunchTimesInfo oneGameTimeAndLaunchTimesInfo);
}
