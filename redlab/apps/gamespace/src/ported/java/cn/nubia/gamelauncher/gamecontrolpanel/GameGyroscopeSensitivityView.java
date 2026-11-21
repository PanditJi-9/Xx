package dev.redlab.gamespace.gamecontrolpanel;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import dev.redlab.gamespace.R;
import dev.redlab.gamespace.gamecontrolpanel.utils.LogUtil;

/* loaded from: classes.dex */
public class GameGyroscopeSensitivityView extends FrameLayout implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {
    private static final int GYROSCOPE_SENSITIVITY_DEFAULT_VALUE = 100;
    private static final int GYROSCOPE_SENSITIVITY_MAX_VALUE = 200;
    private static final int INVALID = -1;
    private static final int OFFSET = 1;
    private static final int TAB = 3;
    private static final String TAG = "GameGyroscopeSensitivityView";
    private ImageView mDownGyroSenX;
    private ImageView mDownGyroSenY;
    protected IGameStrengthSelectedListener mGameStrengthSelectedListener;
    private RelativeLayout mGyroSenPanel;
    private GyroSenSeekBar mSeekBarGyroX;
    private GyroSenSeekBar mSeekBarGyroY;
    private TextView mTvValueGyroSenX;
    private TextView mTvValueGyroSenY;
    private TextView mUnspportedMsg;
    private ImageView mUpGyroSenX;
    private ImageView mUpGyroSenY;
    private int mValueGyroSenX;
    private int mValueGyroSenY;

    public GameGyroscopeSensitivityView(Context context) {
        this(context, null);
    }

    public GameGyroscopeSensitivityView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public GameGyroscopeSensitivityView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mValueGyroSenX = 100;
        this.mValueGyroSenY = 100;
        initView();
    }

    private void initView() {
        LayoutInflater.from(getContext()).inflate(R.layout.nubia_game_gyro_sensitivity_seekbar_layout, this);
        this.mGyroSenPanel = (RelativeLayout) findViewById(R.id.gyro_sen_panel);
        this.mTvValueGyroSenX = (TextView) findViewById(R.id.gyro_sen_x_value);
        this.mDownGyroSenX = (ImageView) findViewById(R.id.gyro_sen_x_down);
        this.mUpGyroSenX = (ImageView) findViewById(R.id.gyro_sen_x_up);
        this.mDownGyroSenX.setOnClickListener(this);
        this.mUpGyroSenX.setOnClickListener(this);
        this.mTvValueGyroSenY = (TextView) findViewById(R.id.gyro_sen_y_value);
        this.mDownGyroSenY = (ImageView) findViewById(R.id.gyro_sen_y_down);
        this.mUpGyroSenY = (ImageView) findViewById(R.id.gyro_sen_y_up);
        this.mDownGyroSenY.setOnClickListener(this);
        this.mUpGyroSenY.setOnClickListener(this);
        GyroSenSeekBar gyroSenSeekBar = (GyroSenSeekBar) findViewById(R.id.seekBar_gyro_x);
        this.mSeekBarGyroX = gyroSenSeekBar;
        gyroSenSeekBar.setOnSeekBarChangeListener(this);
        this.mSeekBarGyroX.setBeginCenter(true);
        this.mSeekBarGyroX.setMax(200);
        GyroSenSeekBar gyroSenSeekBar2 = (GyroSenSeekBar) findViewById(R.id.seekBar_gyro_y);
        this.mSeekBarGyroY = gyroSenSeekBar2;
        gyroSenSeekBar2.setOnSeekBarChangeListener(this);
        this.mSeekBarGyroY.setBeginCenter(true);
        this.mSeekBarGyroY.setMax(200);
        onSeekBarChanged(100, 100);
        this.mUnspportedMsg = (TextView) findViewById(R.id.unsupported_msg);
    }

    private void onSeekBarChanged(int i, int i2) {
        if (-1 != i) {
            this.mSeekBarGyroX.setProgress(i);
        }
        if (-1 != i2) {
            this.mSeekBarGyroY.setProgress(i2);
        }
    }

    private void updateGyroSenTv(int i, int i2) {
        if (-1 != i) {
            this.mTvValueGyroSenX.setText(i + "%");
            this.mValueGyroSenX = i;
        }
        if (-1 != i2) {
            this.mTvValueGyroSenY.setText(i2 + "%");
            this.mValueGyroSenY = i2;
        }
    }

    public int getGyroSenX() {
        return this.mValueGyroSenX;
    }

    public int getGyroSenY() {
        return this.mValueGyroSenY;
    }

    @Override // android.widget.SeekBar.OnSeekBarChangeListener
    public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
        int id = seekBar.getId();
        if (R.id.seekBar_gyro_x == id) {
            int progress = this.mSeekBarGyroX.getProgress();
            updateGyroSenTv(progress >= 1 ? progress : 1, -1);
        } else if (R.id.seekBar_gyro_y == id) {
            int progress2 = this.mSeekBarGyroY.getProgress();
            updateGyroSenTv(-1, progress2 >= 1 ? progress2 : 1);
        }
    }

    @Override // android.widget.SeekBar.OnSeekBarChangeListener
    public void onStartTrackingTouch(SeekBar seekBar) {
        LogUtil.d(TAG, "onStartTrackingTouch");
    }

    @Override // android.widget.SeekBar.OnSeekBarChangeListener
    public void onStopTrackingTouch(SeekBar seekBar) {
        LogUtil.d(TAG, "onStopTrackingTouch");
        IGameStrengthSelectedListener iGameStrengthSelectedListener = this.mGameStrengthSelectedListener;
        if (iGameStrengthSelectedListener != null) {
            iGameStrengthSelectedListener.onGameStrengthSelected(3, -1, new int[]{this.mValueGyroSenX, this.mValueGyroSenY});
        }
    }

    public void setGameStrengthSelectedListener(IGameStrengthSelectedListener iGameStrengthSelectedListener) {
        LogUtil.d(TAG, "setGameStrengthSelectedListener");
        this.mGameStrengthSelectedListener = iGameStrengthSelectedListener;
    }

    public void setGameStrengthenGyroSenValue(String str) {
        LogUtil.d(TAG, "setGameStrengthenGyroSenValue, value = " + str);
        if (!TextUtils.isEmpty(str)) {
            this.mGyroSenPanel.setVisibility(0);
            this.mUnspportedMsg.setVisibility(8);
            try {
                int indexOf = str.indexOf("&");
                String substring = str.substring(0, indexOf);
                String substring2 = str.substring(indexOf + 1, str.length());
                int parseInt = Integer.parseInt(substring);
                int parseInt2 = Integer.parseInt(substring2);
                LogUtil.d(TAG, "setGameStrengthenGyroSenValue, sen_x = " + parseInt + ", sen_y = " + parseInt2);
                IGameStrengthSelectedListener iGameStrengthSelectedListener = this.mGameStrengthSelectedListener;
                if (iGameStrengthSelectedListener != null) {
                    iGameStrengthSelectedListener.onGameStrengthSelected(3, -1, new int[]{parseInt, parseInt2});
                }
                onSeekBarChanged(parseInt, parseInt2);
                updateGyroSenTv(parseInt, parseInt2);
                return;
            } catch (Exception e) {
                LogUtil.e(TAG, "setGameStrengthenGyroSenValue: " + e.toString());
                return;
            }
        }
        this.mGyroSenPanel.setVisibility(8);
        this.mUnspportedMsg.setVisibility(0);
    }

    /* JADX WARN: Code restructure failed: missing block: B:21:0x0037, code lost:
    
        if (r5 < 1) goto L22;
     */
    /* JADX WARN: Removed duplicated region for block: B:12:? A[RETURN, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:9:0x0052  */
    @Override // android.view.View.OnClickListener
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public void onClick(android.view.View r5) {
        /*
            r4 = this;
            int r5 = r5.getId()
            r0 = 2131362476(0x7f0a02ac, float:1.8344734E38)
            r1 = 1
            r2 = -1
            if (r5 != r0) goto L18
            dev.redlab.gamespace.gamecontrolpanel.GyroSenSeekBar r5 = r4.mSeekBarGyroX
            int r5 = r5.getProgress()
            int r5 = r5 - r1
            if (r5 >= r1) goto L15
            goto L16
        L15:
            r1 = r5
        L16:
            r5 = r2
            goto L48
        L18:
            r0 = 2131362479(0x7f0a02af, float:1.834474E38)
            r3 = 200(0xc8, float:2.8E-43)
            if (r5 != r0) goto L2b
            dev.redlab.gamespace.gamecontrolpanel.GyroSenSeekBar r5 = r4.mSeekBarGyroX
            int r5 = r5.getProgress()
            int r1 = r1 + r5
            if (r1 <= r3) goto L16
            r5 = r2
            r1 = r3
            goto L48
        L2b:
            r0 = 2131362481(0x7f0a02b1, float:1.8344744E38)
            if (r5 != r0) goto L3a
            dev.redlab.gamespace.gamecontrolpanel.GyroSenSeekBar r5 = r4.mSeekBarGyroY
            int r5 = r5.getProgress()
            int r5 = r5 - r1
            if (r5 >= r1) goto L47
            goto L46
        L3a:
            dev.redlab.gamespace.gamecontrolpanel.GyroSenSeekBar r5 = r4.mSeekBarGyroY
            int r5 = r5.getProgress()
            int r1 = r1 + r5
            if (r1 <= r3) goto L46
            r1 = r2
            r5 = r3
            goto L48
        L46:
            r5 = r1
        L47:
            r1 = r2
        L48:
            r4.onSeekBarChanged(r1, r5)
            r4.updateGyroSenTv(r1, r5)
            dev.redlab.gamespace.gamecontrolpanel.IGameStrengthSelectedListener r5 = r4.mGameStrengthSelectedListener
            if (r5 == 0) goto L5e
            int r0 = r4.mValueGyroSenX
            int r4 = r4.mValueGyroSenY
            int[] r4 = new int[]{r0, r4}
            r0 = 3
            r5.onGameStrengthSelected(r0, r2, r4)
        L5e:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: dev.redlab.gamespace.gamecontrolpanel.GameGyroscopeSensitivityView.onClick(android.view.View):void");
    }
}
