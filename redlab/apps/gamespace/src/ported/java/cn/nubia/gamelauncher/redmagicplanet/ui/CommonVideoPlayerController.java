package dev.redlab.gamespace.redmagicplanet.ui;

import android.content.Context;
import android.widget.FrameLayout;
import dev.redlab.gamespace.redmagicplanet.IRedMagicVideoPlayer;

/* loaded from: classes.dex */
public abstract class CommonVideoPlayerController extends FrameLayout {
    private Context mContext;
    protected IRedMagicVideoPlayer mRedMagicVideoPlayer;

    public abstract void addTextureView(int i);

    public abstract void fragmentOnPause();

    public abstract void fragmentOnResume();

    public abstract int getPagerSize();

    protected abstract boolean getPlayBackSeekBarIsTracking();

    protected abstract void onPlayModeChanged(int i);

    protected abstract void onPlayStateChanged(int i);

    public abstract void removeTextureView();

    protected abstract void reset();

    public abstract void setImage(int i);

    public abstract void setLength(long j);

    public abstract void setPkgName(String str);

    protected abstract void setPlayBackSeekBarToMax(int i);

    public abstract void setTitle(String str);

    public abstract void updateDefaultImage();

    protected abstract void updateProgress();

    public CommonVideoPlayerController(Context context) {
        super(context);
        this.mContext = context;
    }

    public void setRedMagicVideoPlayer(IRedMagicVideoPlayer iRedMagicVideoPlayer) {
        this.mRedMagicVideoPlayer = iRedMagicVideoPlayer;
    }
}
