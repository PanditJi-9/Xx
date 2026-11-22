package cn.nubia.gamecenter.settings.helper;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.util.IntProperty;
import android.util.Property;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.view.animation.PathInterpolator;
import cn.nubia.gamecenter.settings.widget.CircleClipImageView;
import cn.nubia.gamecenter.settings.widget.ClipImageView;
import cn.nubia.gamecenter.settings.widget.GradientTextView;
import cn.nubia.gamecenter.settings.widget.NumberTextView;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class AnimatorHelper {
    private static final String TAG = "AnimatorHelper";
    private final Item[] m_items;
    private final View m_root;
    private AnimatorSet m_set;

    public AnimatorHelper(View view, Item[] itemArr) {
        this.m_root = view;
        this.m_items = itemArr;
    }

    public void start() {
        addItems(this.m_items);
        AnimatorSet animatorSet = this.m_set;
        if (animatorSet == null) {
            return;
        }
        if (animatorSet.isStarted()) {
            this.m_set.cancel();
        }
        this.m_set.start();
    }

    public void cancel() {
        AnimatorSet animatorSet = this.m_set;
        if (animatorSet != null && animatorSet.isStarted()) {
            this.m_set.cancel();
        }
    }

    public boolean isStarted() {
        AnimatorSet animatorSet = this.m_set;
        if (animatorSet == null) {
            return false;
        }
        return animatorSet.isStarted();
    }

    private void addItems(Item[] itemArr) {
        if (this.m_set != null) {
            return;
        }
        AnimatorSet animatorSet = new AnimatorSet();
        this.m_set = animatorSet;
        animatorSet.playTogether(getAnimators(itemArr));
    }

    private List getAnimators(Item[] itemArr) {
        ArrayList arrayList = new ArrayList();
        for (Item item : itemArr) {
            Animator animator = item.getAnimator(this);
            if (animator != null) {
                arrayList.add(animator);
            }
        }
        return arrayList;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public View getView(int i) {
        View view = this.m_root;
        if (view == null) {
            return null;
        }
        return view.findViewById(i);
    }

    public static class Item {
        public static final String ALPHA = "alpha";
        public static final String CUST_MOVE_Y = "cust_moveY";
        public static final String CUST_NUMBER = "cust_number";
        public static final String CUST_NUMBER_GROW = "cust_number_grow";
        public static final String CUST_PERCENT_END = "cust_percent_end";
        public static final String ROTATE = "rotation";
        public static final String SCALEX = "scaleX";
        public static final String SCALEY = "scaleY";
        public static final String TRANSLATIONX = "translationX";
        public static final String TRANSLATIONY = "translationY";
        private static final LinearInterpolator sDefaultInterpolator = new LinearInterpolator();
        private ObjectAnimator m_anim;
        private final String m_attr;
        private final float[] m_curve;
        private int m_delay;
        private int m_duration;
        private TimeInterpolator m_interpolator;
        private float[] m_paras;
        private int m_repeatCount;
        private int m_viewId;

        public Item(int i, String str, float[] fArr, float[] fArr2, int i2, int i3) {
            this.m_viewId = i;
            this.m_attr = str;
            this.m_paras = fArr;
            this.m_curve = fArr2;
            this.m_duration = i2;
            this.m_delay = i3;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public Animator getAnimator(AnimatorHelper animatorHelper) {
            float[] fArr;
            float[] fArr2;
            float[] fArr3;
            float[] fArr4;
            View view = animatorHelper.getView(this.m_viewId);
            ObjectAnimator objectAnimator = this.m_anim;
            if (objectAnimator != null) {
                return objectAnimator;
            }
            ObjectAnimator objectAnimator2 = null;
            if (view == null) {
                return null;
            }
            if (ALPHA.equals(this.m_attr) || ROTATE.equals(this.m_attr) || SCALEX.equals(this.m_attr) || SCALEY.equals(this.m_attr) || TRANSLATIONX.equals(this.m_attr) || TRANSLATIONY.equals(this.m_attr)) {
                float[] fArr5 = this.m_paras;
                if (fArr5 != null && fArr5.length == 2) {
                    objectAnimator2 = ObjectAnimator.ofFloat(view, this.m_attr, fArr5[0], fArr5[1]);
                } else if (fArr5 != null && fArr5.length == 3) {
                    objectAnimator2 = ObjectAnimator.ofFloat(view, this.m_attr, fArr5[0], fArr5[1], fArr5[2]);
                } else if (fArr5 != null && fArr5.length == 4) {
                    objectAnimator2 = ObjectAnimator.ofFloat(view, this.m_attr, fArr5[0], fArr5[1], fArr5[2], fArr5[3]);
                }
            } else if (CUST_PERCENT_END.equals(this.m_attr) && (view instanceof ClipImageView) && (fArr4 = this.m_paras) != null && fArr4.length == 2) {
                IntProperty<ClipImageView> intProperty = ClipImageView.CUST_PERCENT_END;
                float[] fArr6 = this.m_paras;
                objectAnimator2 = ObjectAnimator.ofInt((ClipImageView) view, intProperty, (int) fArr6[0], (int) fArr6[1]);
            } else if (CUST_MOVE_Y.equals(this.m_attr) && (view instanceof CircleClipImageView) && (fArr3 = this.m_paras) != null && fArr3.length == 2) {
                Property<CircleClipImageView, Float> property = CircleClipImageView.CUST_MOVE_Y;
                float[] fArr7 = this.m_paras;
                objectAnimator2 = ObjectAnimator.ofFloat((CircleClipImageView) view, property, fArr7[0], fArr7[1]);
            } else if (CUST_NUMBER.equals(this.m_attr) && (view instanceof NumberTextView) && (fArr2 = this.m_paras) != null && fArr2.length == 2) {
                Property<NumberTextView, Float> property2 = NumberTextView.CUST_NUMBER;
                float[] fArr8 = this.m_paras;
                objectAnimator2 = ObjectAnimator.ofFloat((NumberTextView) view, property2, fArr8[0], fArr8[1]);
            } else if (CUST_NUMBER_GROW.equals(this.m_attr) && (view instanceof GradientTextView) && (fArr = this.m_paras) != null && fArr.length == 2) {
                Property<GradientTextView, Float> property3 = GradientTextView.CUST_NUMBER_GROW;
                float[] fArr9 = this.m_paras;
                objectAnimator2 = ObjectAnimator.ofFloat((GradientTextView) view, property3, fArr9[0], fArr9[1]);
            }
            if (objectAnimator2 != null) {
                TimeInterpolator timeInterpolator = this.m_interpolator;
                if (timeInterpolator == null) {
                    addInterpolator(objectAnimator2, this.m_curve);
                } else {
                    setInterpolator(objectAnimator2, timeInterpolator);
                }
                objectAnimator2.setDuration(this.m_duration);
                objectAnimator2.setRepeatCount(this.m_repeatCount);
                objectAnimator2.setStartDelay(this.m_delay);
            }
            this.m_anim = objectAnimator2;
            return objectAnimator2;
        }

        private static final void addInterpolator(ObjectAnimator objectAnimator, float[] fArr) {
            if (fArr == null) {
                setInterpolator(objectAnimator, sDefaultInterpolator);
                return;
            }
            PathInterpolator pathInterpolator = fArr.length == 4 ? new PathInterpolator(fArr[0], fArr[1], fArr[2], fArr[3]) : null;
            if (pathInterpolator != null) {
                setInterpolator(objectAnimator, pathInterpolator);
            }
        }

        public static void setInterpolator(ObjectAnimator objectAnimator, TimeInterpolator timeInterpolator) {
            if (objectAnimator != null) {
                objectAnimator.setInterpolator(timeInterpolator);
            }
        }

        public void setInterpolator(TimeInterpolator timeInterpolator) {
            this.m_interpolator = timeInterpolator;
            ObjectAnimator objectAnimator = this.m_anim;
            if (objectAnimator == null) {
                return;
            }
            setInterpolator(objectAnimator, timeInterpolator);
        }

        public void setParams(float[] fArr) {
            this.m_paras = fArr;
            ObjectAnimator objectAnimator = this.m_anim;
            if (objectAnimator == null || fArr == null || fArr.length != 2) {
                return;
            }
            objectAnimator.setFloatValues(fArr[0], fArr[1]);
        }

        public void setParams(int i, int i2) {
            this.m_paras = new float[]{i, i2};
            ObjectAnimator objectAnimator = this.m_anim;
            if (objectAnimator == null) {
                return;
            }
            objectAnimator.setIntValues(i, i2);
        }

        public void setDuration(int i) {
            this.m_duration = i;
            ObjectAnimator objectAnimator = this.m_anim;
            if (objectAnimator == null) {
                return;
            }
            objectAnimator.setDuration(i);
        }

        public void setDelay(int i) {
            this.m_delay = i;
            ObjectAnimator objectAnimator = this.m_anim;
            if (objectAnimator == null) {
                return;
            }
            objectAnimator.setStartDelay(i);
        }

        public void setRepeatCount(int i) {
            this.m_repeatCount = i;
            ObjectAnimator objectAnimator = this.m_anim;
            if (objectAnimator == null) {
                return;
            }
            objectAnimator.setRepeatCount(i);
        }

        public void setTarget(View view) {
            this.m_viewId = view.getId();
            ObjectAnimator objectAnimator = this.m_anim;
            if (objectAnimator == null) {
                return;
            }
            objectAnimator.setTarget(view);
        }

        public int getViewId() {
            return this.m_viewId;
        }

        public void cancel() {
            ObjectAnimator objectAnimator = this.m_anim;
            if (objectAnimator == null) {
                return;
            }
            objectAnimator.cancel();
        }
    }
}
