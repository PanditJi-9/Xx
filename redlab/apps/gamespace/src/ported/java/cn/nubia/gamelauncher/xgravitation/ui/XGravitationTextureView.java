package dev.redlab.gamespace.xgravitation.ui;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.TextureView;
import dev.redlab.gamespace.R;
import dev.redlab.gamespace.xgravitation.IController;
import dev.redlab.gamespace.xgravitation.IVideoPlayerController;
import dev.redlab.gamespace.xgravitation.util.LogUtils;
import java.io.IOException;

/* loaded from: classes2.dex */
public class XGravitationTextureView extends TextureView implements TextureView.SurfaceTextureListener, MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnBufferingUpdateListener, IVideoPlayerController {
    private static final String TAG = "XGravitationTextureView";
    private Context mContext;
    private IController mController;
    private boolean mStartEnterAnimation;
    private Surface mSurface;
    private SurfaceTexture mSurfaceTexture;
    private MediaPlayer mVideoPreviewPlayer;

    @Override // android.media.MediaPlayer.OnBufferingUpdateListener
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {
    }

    @Override // android.view.TextureView.SurfaceTextureListener
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i2) {
    }

    public XGravitationTextureView(Context context) {
        super(context);
        this.mContext = context;
        init();
    }

    public XGravitationTextureView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mContext = context;
        init();
    }

    public XGravitationTextureView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mContext = context;
        init();
    }

    public XGravitationTextureView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mContext = context;
        init();
    }

    private void init() {
        setOutlineProvider(new TextureVideoViewOutlineProvider(getResources().getDimensionPixelSize(R.dimen.x_gravitation_video_preview_round_radius_size)));
        setClipToOutline(true);
        setSurfaceTextureListener(this);
    }

    public void initMediaPlayer() {
        LogUtils.d(TAG, "initMediaPlayer: ");
        MediaPlayer mediaPlayer = this.mVideoPreviewPlayer;
        if (mediaPlayer == null) {
            MediaPlayer create = MediaPlayer.create(this.mContext, R.raw.magic_broadcast);
            this.mVideoPreviewPlayer = create;
            create.setAudioStreamType(3);
        } else {
            mediaPlayer.reset();
            try {
                setDataSourceFromResource(this.mContext.getResources(), this.mVideoPreviewPlayer, R.raw.magic_broadcast);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void openMediaPlayer() {
        LogUtils.d(TAG, "openMediaPlayer: ");
        if (this.mSurfaceTexture == null) {
            LogUtils.d(TAG, "openMediaPlayer:mSurfaceTexture is not init complete ");
            return;
        }
        this.mVideoPreviewPlayer.setOnPreparedListener(this);
        this.mVideoPreviewPlayer.setOnCompletionListener(this);
        this.mVideoPreviewPlayer.setOnErrorListener(this);
        this.mVideoPreviewPlayer.setOnBufferingUpdateListener(this);
        try {
            if (this.mSurface == null) {
                this.mSurface = new Surface(this.mSurfaceTexture);
            }
            this.mVideoPreviewPlayer.setSurface(this.mSurface);
            this.mVideoPreviewPlayer.prepare();
        } catch (IOException | IllegalStateException e) {
            e.printStackTrace();
        }
    }

    @Override // android.view.TextureView.SurfaceTextureListener
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i2) {
        LogUtils.d(TAG, "onSurfaceTextureAvailable: ");
        initMediaPlayer();
        SurfaceTexture surfaceTexture2 = this.mSurfaceTexture;
        if (surfaceTexture2 == null) {
            this.mSurfaceTexture = surfaceTexture;
            if (this.mVideoPreviewPlayer != null) {
                openMediaPlayer();
                return;
            }
            return;
        }
        setSurfaceTexture(surfaceTexture2);
    }

    @Override // android.view.TextureView.SurfaceTextureListener
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        return this.mSurfaceTexture == null;
    }

    @Override // android.view.TextureView.SurfaceTextureListener
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        IController iController = this.mController;
        if (iController != null) {
            iController.updateProgress(this.mVideoPreviewPlayer.getCurrentPosition());
        }
    }

    private void setDataSourceFromResource(Resources resources, MediaPlayer mediaPlayer, int i) throws IOException {
        AssetFileDescriptor openRawResourceFd = resources.openRawResourceFd(i);
        if (openRawResourceFd != null) {
            mediaPlayer.setDataSource(openRawResourceFd.getFileDescriptor(), openRawResourceFd.getStartOffset(), openRawResourceFd.getLength());
            openRawResourceFd.close();
        }
    }

    @Override // android.media.MediaPlayer.OnCompletionListener
    public void onCompletion(MediaPlayer mediaPlayer) {
        LogUtils.d(TAG, "onCompletion: ");
        IController iController = this.mController;
        if (iController == null || !iController.uiIsVisibility()) {
            return;
        }
        mediaPlayer.start();
        this.mController.playSound();
    }

    @Override // android.media.MediaPlayer.OnErrorListener
    public boolean onError(MediaPlayer mediaPlayer, int i, int i2) {
        LogUtils.d(TAG, "onError: what = " + i + " ;; extra = " + i2);
        return false;
    }

    @Override // android.media.MediaPlayer.OnPreparedListener
    public void onPrepared(MediaPlayer mediaPlayer) {
        LogUtils.d(TAG, "onPrepared: mStartEnterAnimation = " + this.mStartEnterAnimation);
        IController iController = this.mController;
        if (iController == null || !iController.uiIsVisibility()) {
            return;
        }
        mediaPlayer.start();
        if (this.mStartEnterAnimation) {
            this.mController.startEnterAnimation();
        }
        this.mController.setProgressMax(this.mVideoPreviewPlayer.getDuration());
        this.mController.playSound();
    }

    public void setIController(IController iController) {
        this.mController = iController;
    }

    public MediaPlayer getVideoPreviewPlayer() {
        return this.mVideoPreviewPlayer;
    }

    @Override // dev.redlab.gamespace.xgravitation.IVideoPlayerController
    public void stop() {
        MediaPlayer mediaPlayer = this.mVideoPreviewPlayer;
        if (mediaPlayer == null || !mediaPlayer.isPlaying()) {
            return;
        }
        this.mVideoPreviewPlayer.stop();
    }

    @Override // dev.redlab.gamespace.xgravitation.IVideoPlayerController
    public void release() {
        MediaPlayer mediaPlayer = this.mVideoPreviewPlayer;
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            this.mVideoPreviewPlayer.release();
        }
    }

    @Override // dev.redlab.gamespace.xgravitation.IVideoPlayerController
    public void restart(boolean z, SurfaceTexture surfaceTexture) {
        LogUtils.d(TAG, " restart  startEnterAnimation = " + z + ", surfaceTexture = " + surfaceTexture + ", mSurfaceTexture = " + this.mSurfaceTexture);
        if (this.mSurfaceTexture == null) {
            this.mSurfaceTexture = surfaceTexture;
        }
        this.mStartEnterAnimation = z;
        initMediaPlayer();
        openMediaPlayer();
    }
}
