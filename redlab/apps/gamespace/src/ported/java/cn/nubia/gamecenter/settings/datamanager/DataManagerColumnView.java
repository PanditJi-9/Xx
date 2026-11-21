package cn.nubia.gamecenter.settings.datamanager;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import cn.nubia.gamecenter.settings.R;
import cn.nubia.gamecenter.settings.utils.LogUtil;

/* loaded from: classes.dex */
public class DataManagerColumnView extends View {
    private static final String TAG = "[DataManager]-ColumnView";
    private String GAME_NOTE;
    private String REDMAGIC_TIME;
    int bottomTextNoteDuration;
    int bottomTextSize;
    int bottomTextTimeDuration;
    int bottomTextXShiftGN;
    int bottomTextXShiftRMT;
    int bottomTextY;
    private int collectionVideos;
    int columnDuration;
    private int deathVideos;
    int figureDefalutY;
    int figureY;
    private int fullVideos;
    private Canvas mCanvas;
    private Context mContext;
    private String mMode;
    private Paint mPaint;
    private int manualVideos;
    private int momentVideos;
    private int pictureNotes;
    private int[] recordArray;
    private int recordMax;
    int rectBottomShiftY;
    int rectTopShiftY;
    int rectTopY;
    private int textNotes;

    public DataManagerColumnView(Context context) {
        super(context);
        this.REDMAGIC_TIME = "REDMAGIC_TIME";
        this.GAME_NOTE = "GAME_NOTE";
        init(context);
    }

    public DataManagerColumnView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.REDMAGIC_TIME = "REDMAGIC_TIME";
        this.GAME_NOTE = "GAME_NOTE";
        init(context);
    }

    public DataManagerColumnView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.REDMAGIC_TIME = "REDMAGIC_TIME";
        this.GAME_NOTE = "GAME_NOTE";
        init(context);
    }

    private void init(Context context) {
        LogUtil.i(TAG, "init");
        this.mContext = context;
        Paint paint = new Paint(1);
        this.mPaint = paint;
        paint.setStrokeWidth(2.0f);
        this.mPaint.setColor(Color.parseColor("#D9FFFFFF"));
        setParmeter(context);
    }

    public void reDraw() {
        invalidate();
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        this.mCanvas = canvas;
        drawColumn(canvas);
        drawBottomText(canvas, this.mMode);
    }

    private void drawColumn(Canvas canvas) {
        String str;
        String str2;
        String str3;
        int i = 24;
        float f = 24;
        LinearGradient linearGradient = new LinearGradient(0.0f, 430, f, 0.0f, new int[]{-13748921, -6312231}, (float[]) null, Shader.TileMode.MIRROR);
        boolean equals = this.mMode.equals(this.REDMAGIC_TIME);
        String str4 = "";
        String str5 = "; recordMax = ";
        String str6 = "#FF000000";
        String str7 = TAG;
        String str8 = "-";
        if (equals) {
            if (this.recordArray == null) {
                LogUtil.i(TAG, "recordArray == null");
                this.mPaint.setTextSize(36.0f);
                this.mPaint.setColor(Color.parseColor("#80FFFFFF"));
                for (int i2 = 0; i2 < 5; i2++) {
                    canvas.drawText("-", this.bottomTextXShiftRMT + 24 + ((this.columnDuration + 24) * i2), this.figureDefalutY, this.mPaint);
                }
                return;
            }
            int i3 = 0;
            for (int i4 = 5; i3 < i4; i4 = 5) {
                this.mPaint.setColor(Color.parseColor(str6));
                int i5 = this.bottomTextXShiftRMT;
                int i6 = this.columnDuration;
                String str9 = str6;
                String str10 = str7;
                String str11 = str5;
                RectF rectF = new RectF(i + i5 + ((i + i6) * i3), this.rectTopY - ((430 - this.rectTopShiftY) * (this.recordArray[i3] / this.recordMax)), i5 + 48 + ((i6 + 24) * i3), 430 - this.rectBottomShiftY);
                this.mPaint.setStyle(Paint.Style.FILL);
                this.mPaint.setShader(linearGradient);
                this.mPaint.setShadowLayer(2.01f, 2.0f, 2.0f, 452984831);
                canvas.drawRect(rectF, this.mPaint);
                this.mPaint.setShader(null);
                this.mPaint.setTextSize(36.0f);
                this.mPaint.setColor(Color.parseColor("#80FFFFFF"));
                if (this.recordMax == 0) {
                    canvas.drawText("-", 24 + this.bottomTextXShiftRMT + ((this.columnDuration + 24) * i3), this.figureDefalutY, this.mPaint);
                } else {
                    canvas.drawText(this.recordArray[i3] == 0 ? "-" : this.recordArray[i3] + "", 24 + this.bottomTextXShiftRMT + ((this.columnDuration + 24) * i3), this.figureY - ((this.recordArray[i3] / this.recordMax) * 320.0f), this.mPaint);
                }
                LogUtil.i(str10, "recordArray[i] / recordMax = " + (this.recordArray[i3] / this.recordMax) + "; recordArray[i] = " + this.recordArray[i3] + str11 + this.recordMax);
                i3++;
                str5 = str11;
                str7 = str10;
                str6 = str9;
                i = 24;
            }
            return;
        }
        if (this.mMode.equals(this.GAME_NOTE)) {
            float f2 = 5.5f;
            int i7 = 2;
            if (this.recordArray == null) {
                this.mPaint.setTextSize(36.0f);
                this.mPaint.setColor(Color.parseColor("#80FFFFFF"));
                for (int i8 = 0; i8 < 2; i8++) {
                    canvas.drawText("-", this.bottomTextXShiftRMT + (f * 5.5f) + (i8 * 2 * (24 + this.columnDuration)), this.figureDefalutY, this.mPaint);
                }
                return;
            }
            int i9 = 0;
            while (i9 < i7) {
                this.mPaint.setColor(Color.parseColor("#FF000000"));
                int i10 = this.bottomTextXShiftRMT;
                float f3 = f * f2;
                int i11 = i9 * 2;
                int i12 = this.columnDuration;
                String str12 = str4;
                String str13 = str8;
                RectF rectF2 = new RectF(i10 + f3 + (i11 * (24 + i12)), this.rectTopY - ((430 - this.rectTopShiftY) * (this.recordArray[i9] / this.recordMax)), i10 + (6.5f * f) + ((i12 + 24) * i11), 430 - this.rectBottomShiftY);
                LogUtil.i(TAG, "recordArray[i] / recordMax = " + (this.recordArray[i9] / this.recordMax) + "; recordArray[i] = " + this.recordArray[i9] + "; recordMax = " + this.recordMax);
                this.mPaint.setStyle(Paint.Style.FILL);
                this.mPaint.setShader(linearGradient);
                this.mPaint.setShadowLayer(2.01f, 2.0f, 2.0f, 452984831);
                canvas.drawRect(rectF2, this.mPaint);
                this.mPaint.setShader(null);
                this.mPaint.setTextSize(36.0f);
                this.mPaint.setColor(Color.parseColor("#80FFFFFF"));
                int i13 = this.recordMax;
                if (i13 == 0) {
                    str = str13;
                    canvas.drawText(str, this.bottomTextXShiftRMT + f3 + ((24 + this.columnDuration) * i11), this.figureDefalutY, this.mPaint);
                    str2 = str12;
                } else {
                    str = str13;
                    int i14 = this.recordArray[i9];
                    float f4 = (this.rectTopY - ((430 - this.rectTopShiftY) * (i14 / i13))) - 10.0f;
                    if (i14 == 0) {
                        str3 = str;
                        str2 = str12;
                    } else {
                        str2 = str12;
                        str3 = this.recordArray[i9] + str2;
                    }
                    canvas.drawText(str3, this.bottomTextXShiftRMT + f3 + ((24 + this.columnDuration) * i11), f4, this.mPaint);
                }
                i9++;
                str4 = str2;
                i7 = 2;
                str8 = str;
                f2 = 5.5f;
            }
        }
    }

    private void drawBottomText(Canvas canvas, String str) {
        this.mPaint.setColor(Color.parseColor("#D9FFFFFF"));
        this.mPaint.setTextSize(this.bottomTextSize);
        if (str.equals(this.REDMAGIC_TIME)) {
            canvas.drawText(this.mContext.getString(R.string.datamanager_column_redmagic_time_die), this.bottomTextXShiftRMT, this.bottomTextY, this.mPaint);
            canvas.drawText(this.mContext.getString(R.string.datamanager_column_redmagic_time_whole), this.bottomTextXShiftRMT + this.bottomTextTimeDuration + 72, this.bottomTextY, this.mPaint);
            canvas.drawText(this.mContext.getString(R.string.datamanager_column_redmagic_time_collection), this.bottomTextXShiftRMT + ((this.bottomTextTimeDuration + 72) * 2), this.bottomTextY, this.mPaint);
            canvas.drawText(this.mContext.getString(R.string.datamanager_column_redmagic_time_moment), this.bottomTextXShiftRMT + ((this.bottomTextTimeDuration + 72) * 3), this.bottomTextY, this.mPaint);
            canvas.drawText(this.mContext.getString(R.string.datamanager_column_redmagic_time_record), this.bottomTextXShiftRMT + ((this.bottomTextTimeDuration + 72) * 4), this.bottomTextY, this.mPaint);
            return;
        }
        if (str.equals(this.GAME_NOTE)) {
            canvas.drawText(this.mContext.getString(R.string.datamanager_column_gamenote_picture), this.bottomTextXShiftGN, this.bottomTextY, this.mPaint);
            canvas.drawText(this.mContext.getString(R.string.datamanager_column_gamenote_word), this.bottomTextXShiftGN + this.bottomTextNoteDuration + 72, this.bottomTextY, this.mPaint);
        }
    }

    private void setParmeter(Context context) {
        Resources resources = context.getResources();
        this.bottomTextY = resources.getInteger(R.integer.dm_barchart_bottom_text_y);
        this.bottomTextXShiftRMT = resources.getInteger(R.integer.dm_barchart_bottom_text_x_shift_rmt);
        this.bottomTextXShiftGN = resources.getInteger(R.integer.dm_barchart_bottom_text_x_shift_gn);
        this.bottomTextSize = resources.getInteger(R.integer.dm_barchart_bottom_text_size);
        this.bottomTextTimeDuration = resources.getInteger(R.integer.dm_barchart_time_bottom_text_duration);
        this.bottomTextNoteDuration = resources.getInteger(R.integer.dm_barchart_note_bottom_text_duration);
        this.figureY = resources.getInteger(R.integer.dm_barchart_figure_y);
        this.figureDefalutY = resources.getInteger(R.integer.dm_barchart_figure_default_y);
        this.rectTopShiftY = resources.getInteger(R.integer.dm_barchart_rect_top_shift_y);
        this.rectBottomShiftY = resources.getInteger(R.integer.dm_barchart_rect_bottom_shift_y);
        this.rectTopY = resources.getInteger(R.integer.dm_barchart_rect_top_y);
        this.columnDuration = resources.getInteger(R.integer.dm_barchart_column_duration);
    }

    public void setMode(String str) {
        this.mMode = str;
    }

    public void setData(boolean z, String str, String str2, String str3, String str4, String str5, String str6, String str7, String str8) {
        this.mMode = str;
        LogUtil.i(TAG, "isDraw = " + z);
        if (!z) {
            this.recordArray = null;
            return;
        }
        String str9 = this.mMode;
        int i = 0;
        if (str9 == this.REDMAGIC_TIME) {
            this.deathVideos = Integer.parseInt(str2);
            this.fullVideos = Integer.parseInt(str3);
            this.collectionVideos = Integer.parseInt(str4);
            this.momentVideos = Integer.parseInt(str5);
            int parseInt = Integer.parseInt(str6);
            this.manualVideos = parseInt;
            int[] iArr = {this.deathVideos, this.fullVideos, this.collectionVideos, this.momentVideos, parseInt};
            this.recordArray = iArr;
            this.recordMax = iArr[0];
            while (true) {
                int[] iArr2 = this.recordArray;
                if (i >= iArr2.length) {
                    break;
                }
                int i2 = this.recordMax;
                int i3 = iArr2[i];
                if (i2 < i3) {
                    this.recordMax = i3;
                }
                i++;
            }
        } else if (str9 == this.GAME_NOTE) {
            this.pictureNotes = Integer.parseInt(str7);
            int parseInt2 = Integer.parseInt(str8);
            this.textNotes = parseInt2;
            int[] iArr3 = {this.pictureNotes, parseInt2};
            this.recordArray = iArr3;
            this.recordMax = iArr3[0];
            while (true) {
                int[] iArr4 = this.recordArray;
                if (i >= iArr4.length) {
                    break;
                }
                int i4 = this.recordMax;
                int i5 = iArr4[i];
                if (i4 < i5) {
                    this.recordMax = i5;
                }
                i++;
            }
        }
        reDraw();
    }
}
