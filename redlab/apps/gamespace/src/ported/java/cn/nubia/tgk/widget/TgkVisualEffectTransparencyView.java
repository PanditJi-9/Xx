package cn.nubia.tgk.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import dev.redlab.gamespace.R;

/* loaded from: classes2.dex */
public class TgkVisualEffectTransparencyView extends LinearLayout {
    private static final String TAG = "TgkVisualEffectTransparencyView";
    private Context mContext;
    private OnChangeListener mListener;
    private int mProgress;
    private View mRootView;
    private SeekBar mSeekBar;
    private MarqueeTextView mTitle;

    public interface OnChangeListener {
        void onChanged(int i);
    }

    public TgkVisualEffectTransparencyView(Context context) {
        this(context, null);
        this.mContext = context;
    }

    public TgkVisualEffectTransparencyView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mProgress = -1;
        this.mContext = context;
        initView();
    }

    private void initView() {
        LayoutInflater.from(this.mContext).inflate(R.layout.tgk_visual_effect_transparency_layout, this);
        this.mRootView = findViewById(R.id.root_view);
        MarqueeTextView marqueeTextView = (MarqueeTextView) findViewById(R.id.title);
        this.mTitle = marqueeTextView;
        marqueeTextView.setText(this.mContext.getString(R.string.gamemode_barrage_message_touch_transparency));
        this.mSeekBar = (SeekBar) findViewById(R.id.tgk_center_visual_effect_seekbar);
    }

    @Override // android.view.View
    public void setEnabled(boolean z) {
        super.setEnabled(z);
        this.mRootView.setEnabled(z);
        this.mTitle.setEnabled(z);
        this.mSeekBar.setEnabled(z);
    }

    public void setProgress(int i) {
        int i2 = (i - 20) / 8;
        if (this.mProgress != i2) {
            this.mProgress = i2;
            this.mSeekBar.setProgress(i2);
        }
    }

    public void setChangedListener(OnChangeListener onChangeListener) {
        this.mListener = onChangeListener;
        this.mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() { // from class: cn.nubia.tgk.widget.TgkVisualEffectTransparencyView.1
            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                if (TgkVisualEffectTransparencyView.this.mProgress != progress) {
                    TgkVisualEffectTransparencyView.this.mProgress = progress;
                    if (TgkVisualEffectTransparencyView.this.mListener != null) {
                        TgkVisualEffectTransparencyView.this.mListener.onChanged((TgkVisualEffectTransparencyView.this.mProgress * 8) + 20);
                    }
                }
            }
        });
    }
}
