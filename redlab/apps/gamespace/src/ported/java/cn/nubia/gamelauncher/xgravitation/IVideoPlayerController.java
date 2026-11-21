package dev.redlab.gamespace.xgravitation;

import android.graphics.SurfaceTexture;

/* loaded from: classes2.dex */
public interface IVideoPlayerController {
    void release();

    void restart(boolean z, SurfaceTexture surfaceTexture);

    void stop();
}
