package cn.nubia.tgk.proxy;

import android.content.Context;
import android.graphics.Rect;
import android.hardware.input.InputManager;
import android.provider.Settings;
import android.util.Log;

/* loaded from: classes2.dex */
public class InputManagerProxy {
    private static boolean DEBUG = true;
    private static final String TAG = "InputManagerProxy";
    protected static final int TGK_COUNT = 3;
    protected static final int TGK_LEFT_KEY_CODE = 137;
    protected static final int TGK_MIDDLE_KEY_CODE = 136;
    public static final int TGK_MOVE_VISION_OPT = 3;
    protected static final int TGK_RIGHT_KEY_CODE = 138;
    private Context mContext;
    private InputManager mInputManager;

    public InputManagerProxy(Context context) {
        this.mContext = context;
        this.mInputManager = (InputManager) context.getSystemService("input");
    }

    public void setGameKeyEnable(boolean z) {
        try {
            if (DEBUG) {
                Log.i(TAG, "set tgk enable=" + z);
            }
            InputManager.class.getDeclaredMethod("setGameKeyEnable", Boolean.TYPE, Context.class).invoke(this.mInputManager, Boolean.valueOf(z), this.mContext);
        } catch (Exception unused) {
            Log.e(TAG, "setGameKeyEnable failed");
        }
    }

    public void releaseTgk() {
        try {
            InputManager.class.getDeclaredMethod("releaseTgk", new Class[0]).invoke(this.mInputManager, new Object[0]);
        } catch (Exception unused) {
            Log.e(TAG, "releaseTgk failed");
        }
    }

    public boolean isGameKeyEnable() {
        boolean z = false;
        try {
            z = ((Boolean) InputManager.class.getDeclaredMethod("isGameKeyEnable", new Class[0]).invoke(this.mInputManager, new Object[0])).booleanValue();
            if (DEBUG) {
                Log.i(TAG, "is tgk enable=" + z);
            }
        } catch (Exception unused) {
            Log.e(TAG, "isGameKeyEnable failed");
        }
        return z;
    }

    public void setDefaultGameKeyLinkFunctionEnable(int i) {
        try {
            setLeftTgkLinkFunction(i);
            setRightTgkLinkFunction(i);
            setMiddleTgkLinkFunction(i);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setGameKeyLinkFunctionEnable(int i) {
        try {
            if (i == 136) {
                setMiddleTgkLinkFunction(i);
            } else if (i == 137) {
                setLeftTgkLinkFunction(i);
            } else if (i != 138) {
            } else {
                setRightTgkLinkFunction(i);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setSubTgkEnable(int i, boolean z) {
        if (i == 0) {
            setLeftTgkEnable(z);
        } else if (1 == i) {
            setRightTgkEnable(z);
        } else if (2 == i) {
            setMiddleTgkEnable(z);
        }
    }

    public void setMiddleTgkEnable(boolean z) {
        try {
            if (DEBUG) {
                Log.i(TAG, "setMiddleTgkEnable=" + z);
            }
            InputManager.class.getDeclaredMethod("setMiddleTgkEnable", Boolean.TYPE).invoke(this.mInputManager, Boolean.valueOf(z));
        } catch (Exception unused) {
            Log.e(TAG, "setMiddleTgkEnable failed");
        }
    }

    public void setRightTgkEnable(boolean z) {
        try {
            if (DEBUG) {
                Log.i(TAG, "setRightTgkEnable=" + z);
            }
            InputManager.class.getDeclaredMethod("setRightTgkEnable", Boolean.TYPE).invoke(this.mInputManager, Boolean.valueOf(z));
        } catch (Exception unused) {
            Log.e(TAG, "setRightTgkEnable failed");
        }
    }

    public void setLeftTgkEnable(boolean z) {
        try {
            if (DEBUG) {
                Log.i(TAG, "setLeftTgkEnable=" + z);
            }
            InputManager.class.getDeclaredMethod("setLeftTgkEnable", Boolean.TYPE).invoke(this.mInputManager, Boolean.valueOf(z));
        } catch (Exception unused) {
            Log.e(TAG, "setLeftTgkEnable failed");
        }
    }

    public void setTgkMode(int i, int i2) {
        try {
            Log.d(TAG, "in setTgkMode mode=" + i + ", keycode=" + i2);
            InputManager.class.getDeclaredMethod("setTgkMode", Integer.TYPE, Integer.TYPE).invoke(this.mInputManager, Integer.valueOf(i), Integer.valueOf(i2));
            if (136 != i2) {
                if (i2 == 137) {
                    Settings.Global.putInt(this.mContext.getContentResolver(), "nubia_left_tgk_vision_mode_change", i == 3 ? 1 : 0);
                } else if (i2 == 138) {
                    Settings.Global.putInt(this.mContext.getContentResolver(), "nubia_right_tgk_vision_mode_change", i == 3 ? 3 : 2);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "setTgkMode failed");
        }
    }

    public void showTgkView(boolean z) {
        try {
            if (DEBUG) {
                Log.i(TAG, "showTgkView=" + z);
            }
            InputManager.class.getDeclaredMethod("showTgkView", Boolean.TYPE).invoke(this.mInputManager, Boolean.valueOf(z));
        } catch (Exception unused) {
            Log.e(TAG, "showTgkView failed");
        }
    }

    public void setTgkRapidFireCount(int i, int i2) {
        try {
            InputManager.class.getDeclaredMethod("setTgkRapidFireCount", Integer.TYPE, Integer.TYPE).invoke(this.mInputManager, Integer.valueOf(i), Integer.valueOf(i2));
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "setTgkRapidFireCount failed");
        }
    }

    public void sendTgkRectsToNative(Rect[][] rectArr) {
        int[][] iArr = {new int[]{-1, -1}, new int[]{-1, -1}, new int[]{-1, -1}, new int[]{-1, -1}, new int[]{-1, -1}, new int[]{-1, -1}};
        int[] iArr2 = {137, 138, 136};
        for (int i = 0; i < 3; i++) {
            for (int i2 = 0; i2 < 2; i2++) {
                Rect rect = rectArr[i][i2];
                if (rect != null) {
                    int i3 = (i * 2) + i2;
                    iArr[i3][0] = (rect.left + rect.right) / 2;
                    iArr[i3][1] = (rect.top + rect.bottom) / 2;
                }
            }
            int i4 = i * 2;
            setTgkPoint(iArr[i4], iArr[i4 + 1], iArr2[i]);
        }
    }

    public void setTgkPoint(int[] iArr, int[] iArr2, int i) {
        try {
            if (DEBUG) {
                Log.d(TAG, "in setTgkPoint keycode=" + i + " point1=(" + iArr[0] + ", " + iArr[1] + ") point2=(" + iArr2[0] + ", " + iArr2[1] + ")");
            }
            InputManager.class.getDeclaredMethod("setTgkPoint", int[].class, int[].class, Integer.TYPE).invoke(this.mInputManager, iArr, iArr2, Integer.valueOf(i));
        } catch (Exception e) {
            if (DEBUG) {
                e.printStackTrace();
            }
            Log.e(TAG, "setTgkPoint failed");
        }
    }

    public void setTgkSensitivity(int i, int i2) {
        try {
            if (DEBUG) {
                Log.d(TAG, "in setTgkSensitivity keycode=" + i2 + " value=" + (i + 1));
            }
            InputManager.class.getDeclaredMethod("setTgkSensitivity", Integer.TYPE, Integer.TYPE).invoke(this.mInputManager, Integer.valueOf(i + 1), Integer.valueOf(i2));
        } catch (Exception e) {
            if (DEBUG) {
                e.printStackTrace();
            }
            Log.e(TAG, "setTgkSensitivity failed");
        }
    }

    public void setTouchHapticFeedbackEnable(boolean z) {
        try {
            if (DEBUG) {
                Log.d(TAG, "setTouchHapticFeedbackEnable enable:" + z);
            }
            InputManager.class.getDeclaredMethod("setTouchHapticFeedbackEnable", Boolean.TYPE, Context.class).invoke(this.mInputManager, Boolean.valueOf(z), null);
        } catch (Exception e) {
            if (DEBUG) {
                e.printStackTrace();
            }
            Log.e(TAG, "setTouchHapticFeedbackEnable failed");
        }
    }

    public void setLeftTgkLinkFunction(int i) {
        try {
            if (DEBUG) {
                Log.i(TAG, "setTgkLeftLinkFunction=" + i);
            }
            InputManager.class.getDeclaredMethod("setGameLeftKeyLinkFunction", Integer.TYPE).invoke(this.mInputManager, Integer.valueOf(i));
        } catch (Exception unused) {
            Log.e(TAG, "setTgkLeftLinkFunction failed");
        }
    }

    public void setRightTgkLinkFunction(int i) {
        try {
            if (DEBUG) {
                Log.i(TAG, "setTgkRightLinkFunction=" + i);
            }
            InputManager.class.getDeclaredMethod("setGameRightKeyLinkFunction", Integer.TYPE).invoke(this.mInputManager, Integer.valueOf(i));
        } catch (Exception unused) {
            Log.e(TAG, "setTgkRightLinkFunction failed");
        }
    }

    public void setMiddleTgkLinkFunction(int i) {
        try {
            if (DEBUG) {
                Log.i(TAG, "setTgkMiddleLinkFunction=" + i);
            }
            InputManager.class.getDeclaredMethod("setGameMiddleKeyLinkFunction", Integer.TYPE).invoke(this.mInputManager, Integer.valueOf(i));
        } catch (Exception unused) {
            Log.e(TAG, "setTgkMiddleLinkFunction failed");
        }
    }

    public void setTgkTopEffectEnable(boolean z) {
        try {
            if (DEBUG) {
                Log.i(TAG, "set tgk top effect enable=" + z);
            }
            InputManager.class.getDeclaredMethod("setTgkTopEffectEnable", Boolean.TYPE).invoke(this.mInputManager, Boolean.valueOf(z));
        } catch (Exception unused) {
            Log.e(TAG, "setTgkTopEffectEnable failed");
        }
    }

    public void setTgkCenterEffectEnable(boolean z) {
        try {
            if (DEBUG) {
                Log.i(TAG, "set tgk center effect enable=" + z);
            }
            InputManager.class.getDeclaredMethod("setTgkCenterEffectEnable", Boolean.TYPE).invoke(this.mInputManager, Boolean.valueOf(z));
        } catch (Exception unused) {
            Log.e(TAG, "setTgkCenterEffectEnable failed");
        }
    }

    public void setTgkTransparency(int i) {
        try {
            if (DEBUG) {
                Log.i(TAG, "set tgk transparency=" + i);
            }
            InputManager.class.getDeclaredMethod("setTgkTransparency", Integer.TYPE).invoke(this.mInputManager, Integer.valueOf(i));
        } catch (Exception unused) {
            Log.e(TAG, "setTgkTransparency failed");
        }
    }
}
