package cn.nubia.tgk.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import dev.redlab.gamespace.R;

/* loaded from: classes2.dex */
public class TgkMultSeekBarView extends LinearLayout {
    private static final String TAG = "TgkMultSeekBarView";
    private Context mContext;
    private OnChangeListener mListener;
    private int mProgress;
    private SeekBar mSeekBar;
    private TextView[] mSensitivityTvs;

    public interface OnChangeListener {
        void onChanged(TgkMultSeekBarView tgkMultSeekBarView, int i);
    }

    public TgkMultSeekBarView(Context context) {
        this(context, null);
        this.mContext = context;
    }

    public TgkMultSeekBarView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mProgress = -1;
        this.mContext = context;
        initView(attributeSet);
    }

    public void initView(AttributeSet attributeSet) {
        LayoutInflater.from(this.mContext).inflate(R.layout.tgk_layout_mulitclicks_seekbar_view, this);
        this.mSensitivityTvs = new TextView[]{(TextView) findViewById(R.id.sensitivity_tv_l), (TextView) findViewById(R.id.sensitivity_tv_n), (TextView) findViewById(R.id.sensitivity_tv_h)};
        this.mSeekBar = (SeekBar) findViewById(R.id.sensitivity_seek_bar);
    }

    public void setProgress(int i) {
        if (this.mProgress != i) {
            this.mProgress = i;
            this.mSeekBar.setProgress(i);
        }
    }

    public void setChangedListener(OnChangeListener onChangeListener) {
        this.mListener = onChangeListener;
        this.mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() { // from class: cn.nubia.tgk.widget.TgkMultSeekBarView.1
            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                if (TgkMultSeekBarView.this.mProgress != progress) {
                    int i = 0;
                    while (i < TgkMultSeekBarView.this.mSensitivityTvs.length) {
                        TgkMultSeekBarView.this.mSensitivityTvs[i].setEnabled(i == progress);
                        i++;
                    }
                    TgkMultSeekBarView.this.mListener.onChanged(TgkMultSeekBarView.this, progress);
                    TgkMultSeekBarView.this.mProgress = progress;
                }
            }
        });
    }

    @Override // android.view.View
    public void setEnabled(boolean z) {
        this.mSeekBar.setEnabled(z);
        if (z) {
            int i = 0;
            while (true) {
                TextView[] textViewArr = this.mSensitivityTvs;
                if (i >= textViewArr.length) {
                    return;
                }
                textViewArr[i].setEnabled(i == this.mProgress);
                i++;
            }
        } else {
            int i2 = 0;
            while (true) {
                TextView[] textViewArr2 = this.mSensitivityTvs;
                if (i2 >= textViewArr2.length) {
                    return;
                }
                textViewArr2[i2].setEnabled(false);
                i2++;
            }
        }
    }
}
