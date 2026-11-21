package dev.redlab.gamespace.xgravitation.util;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import androidx.media3.common.MimeTypes;
import java.io.IOException;

/* loaded from: classes2.dex */
public class PlaySoundController {
    private static int MAX_MUSIC_TYPE_STREAM_VALUE = 0;
    private static final String TAG = "PlaySoundController";
    private static volatile PlaySoundController mInstance;
    private AudioManager mAudioManager;
    private int mRawResId;
    private MediaPlayer mVideoPreviewPlayer;
    private boolean mVoiceEnabled = true;
    private MediaPlayer mVoicePlayer;

    private PlaySoundController() {
    }

    public void setVoiceId(int i) {
        this.mRawResId = i;
    }

    public void setVideoPreviewPlayer(MediaPlayer mediaPlayer) {
        this.mVideoPreviewPlayer = mediaPlayer;
    }

    public static PlaySoundController getInstance() {
        if (mInstance == null) {
            synchronized (PlaySoundController.class) {
                if (mInstance == null) {
                    mInstance = new PlaySoundController();
                }
            }
        }
        return mInstance;
    }

    private void initMediaPlayer() {
        MediaPlayer mediaPlayer = new MediaPlayer();
        this.mVoicePlayer = mediaPlayer;
        mediaPlayer.setAudioAttributes(mInstance.getAudioAttributes());
        applyVoiceVolumeToPlayer();
        this.mVoicePlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() { // from class: dev.redlab.gamespace.xgravitation.util.PlaySoundController.1
            @Override // android.media.MediaPlayer.OnErrorListener
            public boolean onError(MediaPlayer mediaPlayer2, int i, int i2) {
                LogUtils.d(PlaySoundController.TAG, "onError: ");
                return false;
            }
        });
        this.mVoicePlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() { // from class: dev.redlab.gamespace.xgravitation.util.PlaySoundController.2
            @Override // android.media.MediaPlayer.OnCompletionListener
            public void onCompletion(MediaPlayer mediaPlayer2) {
                LogUtils.d(PlaySoundController.TAG, "onCompletion: ");
            }
        });
        this.mVoicePlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() { // from class: dev.redlab.gamespace.xgravitation.util.PlaySoundController.3
            @Override // android.media.MediaPlayer.OnPreparedListener
            public void onPrepared(MediaPlayer mediaPlayer2) {
                LogUtils.d(PlaySoundController.TAG, "onPrepared: ");
                mediaPlayer2.start();
                PlaySoundController.mInstance.startPlayVideoPreview();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startPlayVideoPreview() {
        LogUtils.d(TAG, "startPlayVideoPreview: ");
        MediaPlayer mediaPlayer = this.mVideoPreviewPlayer;
        if (mediaPlayer != null) {
            mediaPlayer.start();
        }
    }

    public void playSound(Context context) {
        LogUtils.d(TAG, "playSound: ");
        if (context == null) {
            LogUtils.e(TAG, " playSound context is null ");
            return;
        }
        if (this.mAudioManager == null) {
            AudioManager audioManager = (AudioManager) context.getSystemService(MimeTypes.BASE_TYPE_AUDIO);
            this.mAudioManager = audioManager;
            MAX_MUSIC_TYPE_STREAM_VALUE = audioManager.getStreamMaxVolume(3);
        }
        if (this.mVoicePlayer == null) {
            initMediaPlayer();
        }
        MediaPlayer mediaPlayer = this.mVoicePlayer;
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                this.mVoicePlayer.stop();
            }
            try {
                this.mVoicePlayer.reset();
                setDataSourceFromResource(context.getResources(), this.mVoicePlayer, this.mRawResId);
                this.mVoicePlayer.prepare();
                applyVoiceVolumeToPlayer();
                this.mVoicePlayer.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void stop() {
        MediaPlayer mediaPlayer = this.mVoicePlayer;
        if (mediaPlayer == null || !mediaPlayer.isPlaying()) {
            return;
        }
        this.mVoicePlayer.stop();
    }

    public void release() {
        if (this.mVoicePlayer != null) {
            stop();
            this.mVoicePlayer.release();
            this.mVoicePlayer = null;
        }
    }

    private void setDataSourceFromResource(Resources resources, MediaPlayer mediaPlayer, int i) throws IOException {
        AssetFileDescriptor openRawResourceFd = resources.openRawResourceFd(i);
        if (openRawResourceFd != null) {
            mediaPlayer.setDataSource(openRawResourceFd.getFileDescriptor(), openRawResourceFd.getStartOffset(), openRawResourceFd.getLength());
            openRawResourceFd.close();
        }
    }

    public void setVoiceVolume(boolean z) {
        this.mVoiceEnabled = z;
        applyVoiceVolumeToPlayer();
    }

    private void applyVoiceVolumeToPlayer() {
        AudioManager audioManager;
        if (this.mVoicePlayer == null || (audioManager = this.mAudioManager) == null || MAX_MUSIC_TYPE_STREAM_VALUE <= 0) {
            return;
        }
        int streamVolume = this.mVoiceEnabled ? audioManager.getStreamVolume(3) : 0;
        float f = streamVolume / MAX_MUSIC_TYPE_STREAM_VALUE;
        LogUtils.d(TAG, "applyVoiceVolumeToPlayer: mVoiceEnabled = " + this.mVoiceEnabled + " ;; volume = " + streamVolume + " ;; MAX_MUSIC_TYPE_STREAM_VALUE = " + MAX_MUSIC_TYPE_STREAM_VALUE + " ;; currentVolume = " + f);
        this.mVoicePlayer.setVolume(f, f);
    }

    private AudioAttributes getAudioAttributes() {
        return new AudioAttributes.Builder().setUsage(1).setContentType(2).build();
    }
}
