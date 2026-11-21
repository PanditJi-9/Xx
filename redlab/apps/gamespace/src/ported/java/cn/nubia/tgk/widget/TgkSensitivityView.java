package cn.nubia.tgk.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import dev.redlab.gamespace.R;

/* loaded from: classes2.dex */
public class TgkSensitivityView extends LinearLayout {
    private static final String TAG = "TgkSensitivityView";
    private Context mContext;
    private View mDisableView;
    private OnChangeListener mListener;
    private int mProgress;
    private SeekBar mSeekBar;
    private TextView[] mSensitivityTvs;
    private MarqueeTextView mTitle;

    public interface OnChangeListener {
        void onChanged(TgkSensitivityView tgkSensitivityView, int i);
    }

    public TgkSensitivityView(Context context) {
        this(context, null);
        this.mContext = context;
    }

    public TgkSensitivityView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mProgress = -1;
        this.mContext = context;
        initView(attributeSet);
    }

    public void initView(AttributeSet attributeSet) {
        LayoutInflater.from(this.mContext).inflate(R.layout.tgk_sensitivity_view_layout, this);
        this.mTitle = (MarqueeTextView) findViewById(R.id.tgk_sensitivity_title);
        this.mSensitivityTvs = new TextView[]{(TextView) findViewById(R.id.sensitivity_tv_l), (TextView) findViewById(R.id.sensitivity_tv_n), (TextView) findViewById(R.id.sensitivity_tv_h)};
        this.mSeekBar = (SeekBar) findViewById(R.id.sensitivity_seek_bar);
        TypedArray obtainStyledAttributes = this.mContext.obtainStyledAttributes(attributeSet, R.styleable.TgkSensitivityView);
        String string = obtainStyledAttributes.getString(0);
        obtainStyledAttributes.recycle();
        this.mTitle.setText(this.mContext.getResources().getString(R.string.tgk_sensitivity_msg) + string);
    }

    public void setProgress(int i) {
        if (this.mProgress != i) {
            this.mProgress = i;
            this.mSeekBar.setProgress(i);
        }
    }

    public void setChangedListener(OnChangeListener onChangeListener) {
        this.mListener = onChangeListener;
        this.mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() { // from class: cn.nubia.tgk.widget.TgkSensitivityView.1
            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                if (TgkSensitivityView.this.mProgress != progress) {
                    int i = 0;
                    while (i < TgkSensitivityView.this.mSensitivityTvs.length) {
                        TgkSensitivityView.this.mSensitivityTvs[i].setEnabled(i == progress);
                        i++;
                    }
                    TgkSensitivityView.this.mListener.onChanged(TgkSensitivityView.this, progress);
                    TgkSensitivityView.this.mProgress = progress;
                }
            }
        });
    }

    @Override // android.view.View
    public void setEnabled(boolean z) {
        this.mSeekBar.setEnabled(z);
        this.mTitle.setEnabled(z);
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
