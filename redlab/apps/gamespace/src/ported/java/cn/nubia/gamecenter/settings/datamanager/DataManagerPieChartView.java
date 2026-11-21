package cn.nubia.gamecenter.settings.datamanager;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import androidx.core.internal.view.SupportMenu;
import cn.nubia.gamecenter.settings.R;
import cn.nubia.gamecenter.settings.utils.LogUtil;

/* loaded from: classes.dex */
public class DataManagerPieChartView extends View {
    private static final float RAngle = 360.0f;
    private static final String TAG = "[DataManager]-PieChartView";
    private String GAME_NOTE;
    private String REDMAGIC_TIME;
    private float angle1;
    private float angle2;
    private float angle3;
    private float angle4;
    private Bitmap bitmapBlack;
    private Bitmap bitmapBlueChecked;
    private Bitmap bitmapBlueEnd;
    private Bitmap bitmapBlueStart;
    private Bitmap bitmapBlueUnChecked;
    private Bitmap bitmapBlueUnCheckedGap;
    private Bitmap bitmapGrayChecked;
    private Bitmap bitmapGrayEnd;
    private Bitmap bitmapGrayStart;
    private Bitmap bitmapGrayUnChecked;
    private Bitmap bitmapGrayUnCheckedGap;
    private Bitmap bitmapRedChecked;
    private Bitmap bitmapRedEnd;
    private Bitmap bitmapRedStart;
    private Bitmap bitmapRedUnChecked;
    private Bitmap bitmapRedUnCheckedGap;
    private Bitmap bitmapYellowChecked;
    private Bitmap bitmapYellowEnd;
    private Bitmap bitmapYellowStart;
    private Bitmap bitmapYellowUnChecked;
    private Bitmap bitmapYellowUnCheckedGap;
    private IPieChartCallBack iPieChartCallBack;
    private Canvas mCanvas;
    private Context mContext;
    private boolean mIsTablet;
    private int mLength;
    private String mMode;
    private Region mRegion1;
    private Region mRegion2;
    private Region mRegion3;
    private Region mRegion4;
    private int mode;
    private Path ovalPath;
    private String pkg1;
    private String pkg2;
    private String pkg3;
    private String pkg4;

    public interface IPieChartCallBack {
        void notifyPkgChange(String str, String str2, int i);
    }

    public DataManagerPieChartView(Context context) {
        super(context);
        this.mLength = 0;
        this.mode = -1;
        this.REDMAGIC_TIME = "REDMAGIC_TIME";
        this.GAME_NOTE = "GAME_NOTE";
        init(context);
    }

    public DataManagerPieChartView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mLength = 0;
        this.mode = -1;
        this.REDMAGIC_TIME = "REDMAGIC_TIME";
        this.GAME_NOTE = "GAME_NOTE";
        init(context);
    }

    public DataManagerPieChartView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mLength = 0;
        this.mode = -1;
        this.REDMAGIC_TIME = "REDMAGIC_TIME";
        this.GAME_NOTE = "GAME_NOTE";
        init(context);
    }

    public void setMode(String str) {
        this.mMode = str;
    }

    public void setCallBack(IPieChartCallBack iPieChartCallBack) {
        this.iPieChartCallBack = iPieChartCallBack;
    }

    public void setTouchMode(int i) {
        this.mode = i;
        this.iPieChartCallBack.notifyPkgChange(this.pkg4, this.mMode, 3);
    }

    private void init(Context context) {
        LogUtil.i(TAG, "init");
        this.mContext = context;
        this.mIsTablet = context.getResources().getBoolean(R.bool.datamanager_is_tablet);
        this.bitmapRedChecked = BitmapFactory.decodeResource(this.mContext.getResources(), R.mipmap.gcs_datamanager_chart_red_bg_pressed);
        this.bitmapBlueChecked = BitmapFactory.decodeResource(this.mContext.getResources(), R.mipmap.gcs_datamanager_chart_blue_bg_pressed);
        this.bitmapYellowChecked = BitmapFactory.decodeResource(this.mContext.getResources(), R.mipmap.gcs_datamanager_chart_yellow_bg_pressed);
        this.bitmapGrayChecked = BitmapFactory.decodeResource(this.mContext.getResources(), R.mipmap.gcs_datamanager_chart_gray_bg_pressed);
        this.bitmapRedUnChecked = BitmapFactory.decodeResource(this.mContext.getResources(), R.mipmap.chart_red_bg_disable);
        this.bitmapBlueUnChecked = BitmapFactory.decodeResource(this.mContext.getResources(), R.mipmap.chart_blue_bg_disable);
        this.bitmapYellowUnChecked = BitmapFactory.decodeResource(this.mContext.getResources(), R.mipmap.chart_yellow_bg_disable);
        this.bitmapGrayUnChecked = BitmapFactory.decodeResource(this.mContext.getResources(), R.mipmap.chart_gray_bg_disable);
        this.bitmapRedUnCheckedGap = BitmapFactory.decodeResource(this.mContext.getResources(), R.mipmap.chart_red_bg_disable_gap);
        this.bitmapBlueUnCheckedGap = BitmapFactory.decodeResource(this.mContext.getResources(), R.mipmap.chart_blue_bg_disable_gap);
        this.bitmapYellowUnCheckedGap = BitmapFactory.decodeResource(this.mContext.getResources(), R.mipmap.chart_yellow_bg_disable_gap);
        this.bitmapGrayUnCheckedGap = BitmapFactory.decodeResource(this.mContext.getResources(), R.mipmap.chart_gray_bg_disable_gap);
        this.bitmapGrayStart = BitmapFactory.decodeResource(this.mContext.getResources(), R.mipmap.chart_gray_line_02);
        this.bitmapGrayEnd = BitmapFactory.decodeResource(this.mContext.getResources(), R.mipmap.chart_gray_line_01);
        this.bitmapYellowStart = BitmapFactory.decodeResource(this.mContext.getResources(), R.mipmap.chart_yellow_line_02);
        this.bitmapYellowEnd = BitmapFactory.decodeResource(this.mContext.getResources(), R.mipmap.chart_yellow_line_01);
        this.bitmapBlueStart = BitmapFactory.decodeResource(this.mContext.getResources(), R.mipmap.chart_blue_line_02);
        this.bitmapBlueEnd = BitmapFactory.decodeResource(this.mContext.getResources(), R.mipmap.chart_blue_line_01);
        this.bitmapRedStart = BitmapFactory.decodeResource(this.mContext.getResources(), R.mipmap.chart_red_line_02);
        this.bitmapRedEnd = BitmapFactory.decodeResource(this.mContext.getResources(), R.mipmap.chart_red_line_01);
        this.bitmapBlack = BitmapFactory.decodeResource(this.mContext.getResources(), R.mipmap.chart_black_line);
        this.mRegion1 = new Region();
        this.mRegion2 = new Region();
        this.mRegion3 = new Region();
        this.mRegion4 = new Region();
    }

    public void setAngle(float f, float f2, float f3, float f4) {
        this.angle1 = f;
        this.angle2 = f2;
        this.angle3 = f3;
        this.angle4 = f4;
    }

    public void setPkgName(String str, String str2, String str3, String str4) {
        this.pkg1 = str;
        this.pkg2 = str2;
        this.pkg3 = str3;
        this.pkg4 = str4;
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        this.mCanvas = canvas;
        LogUtil.i(TAG, "draw mode = " + this.mode + " +; angle4 = " + this.angle4 + "; angle3 = " + this.angle3 + "; angle2 = " + this.angle2 + "; angle1 = " + this.angle1);
        drawAllArcView(this.mCanvas, this.mode);
        Region region = this.mRegion1;
        Canvas canvas2 = this.mCanvas;
        float f = this.angle1;
        drawArcRegion(region, canvas2, f, f / 2.0f);
        Region region2 = this.mRegion2;
        Canvas canvas3 = this.mCanvas;
        float f2 = this.angle2;
        drawArcRegion(region2, canvas3, f2, this.angle1 + (f2 / 2.0f));
        Region region3 = this.mRegion3;
        Canvas canvas4 = this.mCanvas;
        float f3 = this.angle3;
        drawArcRegion(region3, canvas4, f3, this.angle1 + this.angle2 + (f3 / 2.0f));
        Region region4 = this.mRegion4;
        Canvas canvas5 = this.mCanvas;
        float f4 = this.angle4;
        drawArcRegion(region4, canvas5, f4, this.angle1 + this.angle2 + this.angle3 + (f4 / 2.0f));
    }

    public void reDraw() {
        LogUtil.i(TAG, "reDraw mode = " + this.mode + " +; angle4 = " + this.angle4 + "; angle3 = " + this.angle3 + "; angle2 = " + this.angle2 + "; angle1 = " + this.angle1);
        invalidate();
    }

    private void drawArcView(Bitmap bitmap, Canvas canvas, float f, float f2) {
        Log.i(TAG, "Cwl angle = " + f + "; degrees = " + f2);
        canvas.save();
        Bitmap createScaledBitmap = Bitmap.createScaledBitmap(bitmap, getWidth(), getWidth(), true);
        Paint paint = new Paint(1);
        RectF rectF = new RectF(0.0f, 0.0f, getWidth(), getWidth());
        if (!this.mIsTablet) {
            canvas.translate(getWidth() / 2, getWidth() / 2);
            canvas.scale(1.2429f, 1.2429f);
            canvas.translate((-getWidth()) / 2, (-getWidth()) / 2);
        }
        int saveLayer = canvas.saveLayer(0.0f, 0.0f, getWidth(), getWidth(), null, 31);
        canvas.rotate((-90.0f) + f2, getWidth() / 2, getWidth() / 2);
        canvas.drawBitmap(createScaledBitmap, (Rect) null, rectF, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        canvas.drawArc(rectF, f, RAngle - f, true, paint);
        paint.setXfermode(null);
        canvas.restoreToCount(saveLayer);
        canvas.restore();
    }

    private void drawArcRegion(Region region, Canvas canvas, float f, float f2) {
        Path path = new Path();
        this.ovalPath = path;
        path.moveTo(getWidth() / 2, getWidth() / 2);
        this.ovalPath.addArc(new RectF(0.0f, 0.0f, getWidth(), getWidth()), (270.0f - (0.5f * f)) + f2, f);
        this.ovalPath.lineTo(getWidth() / 2, getWidth() / 2);
        this.ovalPath.close();
        RectF rectF = new RectF();
        this.ovalPath.computeBounds(rectF, true);
        region.setPath(this.ovalPath, new Region((int) rectF.left, (int) rectF.top, (int) rectF.right, (int) rectF.bottom));
        Paint paint = new Paint();
        paint.setColor(SupportMenu.CATEGORY_MASK);
        paint.setAlpha(0);
        paint.setAntiAlias(true);
        paint.setStrokeWidth(0.0f);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawPath(this.ovalPath, paint);
    }

    private void drawArcStartEnd(Bitmap bitmap, Bitmap bitmap2, Canvas canvas, float f, float f2, int i) {
        Paint paint = new Paint();
        canvas.save();
        if (!this.mIsTablet) {
            canvas.translate(getWidth() / 2, getWidth() / 2);
            canvas.scale(1.2429f, 1.2429f);
            canvas.translate((-getWidth()) / 2, (-getWidth()) / 2);
        }
        canvas.rotate(f, getWidth() / 2, getWidth() / 2);
        Bitmap createScaledBitmap = Bitmap.createScaledBitmap(bitmap, getWidth(), getWidth(), true);
        paint.setAlpha(i);
        canvas.drawBitmap(createScaledBitmap, 0.0f, 0.0f, paint);
        canvas.restore();
        Paint paint2 = new Paint();
        canvas.save();
        if (!this.mIsTablet) {
            canvas.translate(getWidth() / 2, getWidth() / 2);
            canvas.scale(1.2429f, 1.2429f);
            canvas.translate((-getWidth()) / 2, (-getWidth()) / 2);
        }
        canvas.rotate(f2, getWidth() / 2, getWidth() / 2);
        Bitmap createScaledBitmap2 = Bitmap.createScaledBitmap(bitmap2, getWidth(), getWidth(), true);
        paint2.setAlpha(i);
        canvas.drawBitmap(createScaledBitmap2, 0.0f, 0.0f, paint2);
        canvas.restore();
        Paint paint3 = new Paint();
        canvas.save();
        if (!this.mIsTablet) {
            canvas.translate(getWidth() / 2, getWidth() / 2);
            canvas.scale(1.2429f, 1.2429f);
            canvas.translate((-getWidth()) / 2, (-getWidth()) / 2);
        }
        canvas.rotate(f, getWidth() / 2, getWidth() / 2);
        Bitmap createScaledBitmap3 = Bitmap.createScaledBitmap(this.bitmapBlack, getWidth(), getWidth(), true);
        this.bitmapBlack = createScaledBitmap3;
        canvas.drawBitmap(createScaledBitmap3, 0.0f, 0.0f, paint3);
        canvas.drawCircle(getWidth() / 2, getWidth() / 2, 2.0f, paint3);
        canvas.restore();
    }

    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent motionEvent) {
        LogUtil.i(TAG, "onTouchEvent");
        if (motionEvent.getAction() == 0) {
            float x = motionEvent.getX();
            float y = motionEvent.getY();
            int i = (int) x;
            int i2 = (int) y;
            boolean contains = this.mRegion1.contains(i, i2);
            boolean contains2 = this.mRegion2.contains(i, i2);
            boolean contains3 = this.mRegion3.contains(i, i2);
            boolean contains4 = this.mRegion4.contains(i, i2);
            LogUtil.i(TAG, "onTouchEvent: b1: " + contains + " x: " + x + " y: " + y + "; b2: " + contains2 + " x: " + x + " y: " + y + "; b3: " + contains3 + " x: " + x + " y: " + y + "; b4: " + contains4 + " x: " + x + " y: " + y);
            if (contains) {
                this.mode = 0;
                this.iPieChartCallBack.notifyPkgChange(this.pkg1, this.mMode, 0);
                LogUtil.i(TAG, "pkg = " + this.pkg1);
            } else if (contains2) {
                this.mode = 1;
                this.iPieChartCallBack.notifyPkgChange(this.pkg2, this.mMode, 1);
                LogUtil.i(TAG, "pkg = " + this.pkg2);
            } else if (contains3) {
                this.mode = 2;
                this.iPieChartCallBack.notifyPkgChange(this.pkg3, this.mMode, 2);
                LogUtil.i(TAG, "pkg = " + this.pkg3);
            } else if (contains4) {
                this.mode = 3;
                this.iPieChartCallBack.notifyPkgChange(this.pkg4, this.mMode, 3);
                LogUtil.i(TAG, "pkg = " + this.pkg4);
            }
            invalidate();
        }
        return true;
    }

    private void drawAllArcView(Canvas canvas, int i) {
        if (i == 0) {
            drawArcView(this.bitmapGrayChecked, canvas, this.angle1, 0.0f);
            drawArcView(this.bitmapYellowUnChecked, canvas, this.angle2, this.angle1);
            drawArcView(this.bitmapBlueUnChecked, canvas, this.angle3, this.angle1 + this.angle2);
            drawArcView(this.bitmapRedUnChecked, canvas, this.angle4, this.angle1 + this.angle2 + this.angle3);
            Bitmap bitmap = this.bitmapYellowUnCheckedGap;
            float f = this.angle2;
            drawArcView(bitmap, canvas, f, this.angle1 + (f / 2.0f));
            Bitmap bitmap2 = this.bitmapBlueUnCheckedGap;
            float f2 = this.angle3;
            drawArcView(bitmap2, canvas, f2, this.angle1 + this.angle2 + (f2 / 2.0f));
            Bitmap bitmap3 = this.bitmapRedUnCheckedGap;
            float f3 = this.angle4;
            drawArcView(bitmap3, canvas, f3, this.angle1 + this.angle2 + this.angle3 + (f3 / 2.0f));
        } else if (i == 1) {
            drawArcView(this.bitmapGrayUnChecked, canvas, this.angle1, 0.0f);
            drawArcView(this.bitmapYellowChecked, canvas, this.angle2, this.angle1);
            drawArcView(this.bitmapBlueUnChecked, canvas, this.angle3, this.angle1 + this.angle2);
            drawArcView(this.bitmapRedUnChecked, canvas, this.angle4, this.angle1 + this.angle2 + this.angle3);
            drawArcView(this.bitmapGrayUnCheckedGap, canvas, this.angle1, 0.0f);
            drawArcView(this.bitmapBlueUnCheckedGap, canvas, this.angle3, this.angle1 + this.angle2);
            drawArcView(this.bitmapRedUnCheckedGap, canvas, this.angle4, this.angle1 + this.angle2 + this.angle3);
        } else if (i == 2) {
            drawArcView(this.bitmapGrayUnChecked, canvas, this.angle1, 0.0f);
            drawArcView(this.bitmapYellowUnChecked, canvas, this.angle2, this.angle1);
            drawArcView(this.bitmapBlueChecked, canvas, this.angle3, this.angle1 + this.angle2);
            drawArcView(this.bitmapRedUnChecked, canvas, this.angle4, this.angle1 + this.angle2 + this.angle3);
            drawArcView(this.bitmapGrayUnCheckedGap, canvas, this.angle1, 0.0f);
            drawArcView(this.bitmapYellowUnCheckedGap, canvas, this.angle2, this.angle1);
            drawArcView(this.bitmapRedUnCheckedGap, canvas, this.angle4, this.angle1 + this.angle2 + this.angle3);
        } else if (i == 3) {
            drawArcView(this.bitmapGrayUnChecked, canvas, this.angle1, 0.0f);
            drawArcView(this.bitmapYellowUnChecked, canvas, this.angle2, this.angle1);
            drawArcView(this.bitmapBlueUnChecked, canvas, this.angle3, this.angle1 + this.angle2);
            drawArcView(this.bitmapRedChecked, canvas, this.angle4, this.angle1 + this.angle2 + this.angle3);
            drawArcView(this.bitmapGrayUnCheckedGap, canvas, this.angle1, 0.0f);
            drawArcView(this.bitmapYellowUnCheckedGap, canvas, this.angle2, this.angle1);
            drawArcView(this.bitmapBlueUnCheckedGap, canvas, this.angle3, this.angle1 + this.angle2);
            drawArcView(this.bitmapRedUnCheckedGap, canvas, this.angle4, this.angle1 + this.angle2 + this.angle3);
        }
        drawArcFrame(canvas, i);
    }

    private void drawArcFrame(Canvas canvas, int i) {
        float f = this.angle4;
        if (f != RAngle) {
            if (f == 0.0f && this.angle3 == 0.0f && this.angle2 == 0.0f && this.angle1 == 0.0f) {
                return;
            }
            if (i == 0) {
                int i2 = this.mLength;
                if (i2 == 2) {
                    drawArcStartEnd(this.bitmapGrayStart, this.bitmapGrayEnd, canvas, 0.0f, this.angle1, 100);
                    Bitmap bitmap = this.bitmapRedStart;
                    Bitmap bitmap2 = this.bitmapRedEnd;
                    float f2 = this.angle1;
                    drawArcStartEnd(bitmap, bitmap2, canvas, f2, f2 + this.angle4, 42);
                    return;
                }
                if (i2 == 3) {
                    drawArcStartEnd(this.bitmapGrayStart, this.bitmapGrayEnd, canvas, 0.0f, this.angle1, 100);
                    Bitmap bitmap3 = this.bitmapBlueStart;
                    Bitmap bitmap4 = this.bitmapBlueEnd;
                    float f3 = this.angle1;
                    drawArcStartEnd(bitmap3, bitmap4, canvas, f3, f3 + this.angle3, 42);
                    Bitmap bitmap5 = this.bitmapRedStart;
                    Bitmap bitmap6 = this.bitmapRedEnd;
                    float f4 = this.angle1;
                    float f5 = this.angle3;
                    drawArcStartEnd(bitmap5, bitmap6, canvas, f4 + f5, f4 + f5 + this.angle4, 42);
                    return;
                }
                if (i2 == 4) {
                    drawArcStartEnd(this.bitmapGrayStart, this.bitmapGrayEnd, canvas, 0.0f, this.angle1, 100);
                    Bitmap bitmap7 = this.bitmapYellowStart;
                    Bitmap bitmap8 = this.bitmapYellowEnd;
                    float f6 = this.angle1;
                    drawArcStartEnd(bitmap7, bitmap8, canvas, f6, f6 + this.angle2, 42);
                    Bitmap bitmap9 = this.bitmapBlueStart;
                    Bitmap bitmap10 = this.bitmapBlueEnd;
                    float f7 = this.angle1;
                    float f8 = this.angle2;
                    drawArcStartEnd(bitmap9, bitmap10, canvas, f7 + f8, f7 + f8 + this.angle3, 42);
                    Bitmap bitmap11 = this.bitmapRedStart;
                    Bitmap bitmap12 = this.bitmapRedEnd;
                    float f9 = this.angle1;
                    float f10 = this.angle2;
                    float f11 = this.angle3;
                    drawArcStartEnd(bitmap11, bitmap12, canvas, f9 + f10 + f11, f9 + f10 + f11 + this.angle4, 42);
                    return;
                }
                return;
            }
            if (i == 1) {
                int i3 = this.mLength;
                if (i3 == 2) {
                    drawArcStartEnd(this.bitmapGrayStart, this.bitmapGrayEnd, canvas, 0.0f, this.angle1, 42);
                    Bitmap bitmap13 = this.bitmapRedStart;
                    Bitmap bitmap14 = this.bitmapRedEnd;
                    float f12 = this.angle1;
                    drawArcStartEnd(bitmap13, bitmap14, canvas, f12, f12 + this.angle4, 100);
                    return;
                }
                if (i3 == 3) {
                    drawArcStartEnd(this.bitmapGrayStart, this.bitmapGrayEnd, canvas, 0.0f, this.angle1, 42);
                    Bitmap bitmap15 = this.bitmapBlueStart;
                    Bitmap bitmap16 = this.bitmapBlueEnd;
                    float f13 = this.angle1;
                    drawArcStartEnd(bitmap15, bitmap16, canvas, f13, f13 + this.angle3, 100);
                    Bitmap bitmap17 = this.bitmapRedStart;
                    Bitmap bitmap18 = this.bitmapRedEnd;
                    float f14 = this.angle1;
                    float f15 = this.angle3;
                    drawArcStartEnd(bitmap17, bitmap18, canvas, f14 + f15, f14 + f15 + this.angle4, 42);
                    return;
                }
                if (i3 == 4) {
                    drawArcStartEnd(this.bitmapGrayStart, this.bitmapGrayEnd, canvas, 0.0f, this.angle1, 42);
                    Bitmap bitmap19 = this.bitmapYellowStart;
                    Bitmap bitmap20 = this.bitmapYellowEnd;
                    float f16 = this.angle1;
                    drawArcStartEnd(bitmap19, bitmap20, canvas, f16, f16 + this.angle2, 100);
                    Bitmap bitmap21 = this.bitmapBlueStart;
                    Bitmap bitmap22 = this.bitmapBlueEnd;
                    float f17 = this.angle1;
                    float f18 = this.angle2;
                    drawArcStartEnd(bitmap21, bitmap22, canvas, f17 + f18, f17 + f18 + this.angle3, 42);
                    Bitmap bitmap23 = this.bitmapRedStart;
                    Bitmap bitmap24 = this.bitmapRedEnd;
                    float f19 = this.angle1;
                    float f20 = this.angle2;
                    float f21 = this.angle3;
                    drawArcStartEnd(bitmap23, bitmap24, canvas, f19 + f20 + f21, f19 + f20 + f21 + this.angle4, 42);
                    return;
                }
                return;
            }
            if (i == 2) {
                int i4 = this.mLength;
                if (i4 == 2) {
                    drawArcStartEnd(this.bitmapGrayStart, this.bitmapGrayEnd, canvas, 0.0f, this.angle1, 42);
                    Bitmap bitmap25 = this.bitmapRedStart;
                    Bitmap bitmap26 = this.bitmapRedEnd;
                    float f22 = this.angle1;
                    drawArcStartEnd(bitmap25, bitmap26, canvas, f22, f22 + this.angle4, 42);
                    return;
                }
                if (i4 == 3) {
                    drawArcStartEnd(this.bitmapGrayStart, this.bitmapGrayEnd, canvas, 0.0f, this.angle1, 42);
                    Bitmap bitmap27 = this.bitmapBlueStart;
                    Bitmap bitmap28 = this.bitmapBlueEnd;
                    float f23 = this.angle1;
                    drawArcStartEnd(bitmap27, bitmap28, canvas, f23, f23 + this.angle3, 42);
                    Bitmap bitmap29 = this.bitmapRedStart;
                    Bitmap bitmap30 = this.bitmapRedEnd;
                    float f24 = this.angle1;
                    float f25 = this.angle3;
                    drawArcStartEnd(bitmap29, bitmap30, canvas, f24 + f25, f24 + f25 + this.angle4, 100);
                    return;
                }
                if (i4 == 4) {
                    drawArcStartEnd(this.bitmapGrayStart, this.bitmapGrayEnd, canvas, 0.0f, this.angle1, 42);
                    Bitmap bitmap31 = this.bitmapYellowStart;
                    Bitmap bitmap32 = this.bitmapYellowEnd;
                    float f26 = this.angle1;
                    drawArcStartEnd(bitmap31, bitmap32, canvas, f26, f26 + this.angle2, 42);
                    Bitmap bitmap33 = this.bitmapBlueStart;
                    Bitmap bitmap34 = this.bitmapBlueEnd;
                    float f27 = this.angle1;
                    float f28 = this.angle2;
                    drawArcStartEnd(bitmap33, bitmap34, canvas, f27 + f28, f27 + f28 + this.angle3, 100);
                    Bitmap bitmap35 = this.bitmapRedStart;
                    Bitmap bitmap36 = this.bitmapRedEnd;
                    float f29 = this.angle1;
                    float f30 = this.angle2;
                    float f31 = this.angle3;
                    drawArcStartEnd(bitmap35, bitmap36, canvas, f29 + f30 + f31, f29 + f30 + f31 + this.angle4, 42);
                    return;
                }
                return;
            }
            if (i == 3) {
                int i5 = this.mLength;
                if (i5 == 2) {
                    drawArcStartEnd(this.bitmapGrayStart, this.bitmapGrayEnd, canvas, 0.0f, this.angle1, 42);
                    Bitmap bitmap37 = this.bitmapRedStart;
                    Bitmap bitmap38 = this.bitmapRedEnd;
                    float f32 = this.angle1;
                    drawArcStartEnd(bitmap37, bitmap38, canvas, f32, f32 + this.angle4, 42);
                    return;
                }
                if (i5 == 3) {
                    drawArcStartEnd(this.bitmapGrayStart, this.bitmapGrayEnd, canvas, 0.0f, this.angle1, 42);
                    Bitmap bitmap39 = this.bitmapBlueStart;
                    Bitmap bitmap40 = this.bitmapBlueEnd;
                    float f33 = this.angle1;
                    drawArcStartEnd(bitmap39, bitmap40, canvas, f33, f33 + this.angle3, 42);
                    Bitmap bitmap41 = this.bitmapRedStart;
                    Bitmap bitmap42 = this.bitmapRedEnd;
                    float f34 = this.angle1;
                    float f35 = this.angle3;
                    drawArcStartEnd(bitmap41, bitmap42, canvas, f34 + f35, f34 + f35 + this.angle4, 100);
                    return;
                }
                if (i5 == 4) {
                    drawArcStartEnd(this.bitmapGrayStart, this.bitmapGrayEnd, canvas, 0.0f, this.angle1, 42);
                    Bitmap bitmap43 = this.bitmapYellowStart;
                    Bitmap bitmap44 = this.bitmapYellowEnd;
                    float f36 = this.angle1;
                    drawArcStartEnd(bitmap43, bitmap44, canvas, f36, f36 + this.angle2, 42);
                    Bitmap bitmap45 = this.bitmapBlueStart;
                    Bitmap bitmap46 = this.bitmapBlueEnd;
                    float f37 = this.angle1;
                    float f38 = this.angle2;
                    drawArcStartEnd(bitmap45, bitmap46, canvas, f37 + f38, f37 + f38 + this.angle3, 42);
                    Bitmap bitmap47 = this.bitmapRedStart;
                    Bitmap bitmap48 = this.bitmapRedEnd;
                    float f39 = this.angle1;
                    float f40 = this.angle2;
                    float f41 = this.angle3;
                    drawArcStartEnd(bitmap47, bitmap48, canvas, f39 + f40 + f41, f39 + f40 + f41 + this.angle4, 100);
                }
            }
        }
    }

    public void setArcSize(int i) {
        this.mLength = i;
    }
}
