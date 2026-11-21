package dev.redlab.gamespace.redmagicplanet;

import android.widget.FrameLayout;
import java.util.Map;

/* loaded from: classes.dex */
public interface IRedMagicVideoPlayer {
    void continueFromLastPosition(boolean z);

    void enterFullScreen();

    boolean exitFullScreen();

    int getCurrentPosition();

    int getDuration();

    FrameLayout getmContainer();

    boolean isCompleted();

    boolean isError();

    boolean isFullScreen();

    boolean isIdle();

    boolean isNormal();

    boolean isPaused();

    boolean isPlaying();

    boolean isPrepared();

    boolean isPreparing();

    boolean isStopExit();

    void pause();

    void release();

    void releasePlayer();

    void restart();

    void seekTo(int i);

    void setUp(String str, Map<String, String> map);

    void setViewPagerPosition(int i);

    void start();

    void start(int i);

    void stop();
}
