package cn.nubia.gamecenter.settings.summary.presenter;

import cn.nubia.gamecenter.settings.summary.entities.GameAppInfo;
import cn.nubia.gamecenter.settings.summary.entities.GameTimeInfo;
import cn.nubia.gamecenter.settings.summary.entities.OneGameTimeAndLaunchTimesInfo;
import java.util.List;

/* loaded from: classes.dex */
public interface ICallback {
    void gameParms(GameTimeInfo[] gameTimeInfoArr, List<List<GameAppInfo>> list);

    void gameParmsOneGame(OneGameTimeAndLaunchTimesInfo oneGameTimeAndLaunchTimesInfo);
}
