package dev.redlab.gamespace.aimhelper;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/* loaded from: classes.dex */
public class TopActivityMonitor extends Handler {
    private static final String CLASS_NAME = "android.app.NubiaSysState";
    private static final String METHOD_REGISTER = "registerReceiverHandler";
    private static final String METHOD_UNREGISTER = "unregisterReceiver";
    private static final int SYS_STATE_ACT_PAUSE = 2002;
    private static final int SYS_STATE_ACT_RESUME = 2001;
    private static final int SYS_STATE_ACT_RESUMED = 2005;
    private static final int SYS_STATE_ACT_STOP = 2003;
    private static final int SYS_STATE_ACT_TOP = 2000;
    private static final int SYS_STATE_APP_START = 2100;
    private static final int SYS_STATE_APP_STOP = 2101;
    private static final int SYS_STATE_KEYGUARD = 2102;
    private static final int SYS_STATE_RESUME_APP_DIED = 2004;
    private static final String TAG = "TopActivityMonitor";
    private Set<String> activityStack = null;
    private TopActivityMonitorCallback mCallback;
    private Object mNubiaSysState;

    public interface TopActivityMonitorCallback {
        void onActivityChange(Set<String> set);

        void onAppStop(String str);
    }

    public TopActivityMonitor(TopActivityMonitorCallback topActivityMonitorCallback) {
        this.mCallback = topActivityMonitorCallback;
    }

    public void start() {
        registerCallback();
    }

    public void stop() {
        removeCallbacksAndMessages(null);
        unregisterCallback();
    }

    private void createSysStateObj() {
        try {
            if (this.mNubiaSysState == null) {
                Class<?> cls = Class.forName(CLASS_NAME);
                if (this.mNubiaSysState == null) {
                    this.mNubiaSysState = cls.getDeclaredConstructor(new Class[0]).newInstance(new Object[0]);
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e2) {
            e2.printStackTrace();
        } catch (InstantiationException e3) {
            e3.printStackTrace();
        } catch (NoSuchMethodException e4) {
            e4.printStackTrace();
        } catch (InvocationTargetException e5) {
            e5.printStackTrace();
        }
    }

    private void registerCallback() {
        createSysStateObj();
        try {
            Method declaredMethod = Class.forName(CLASS_NAME).getDeclaredMethod(METHOD_REGISTER, Handler.class);
            declaredMethod.setAccessible(true);
            declaredMethod.invoke(this.mNubiaSysState, this);
            LogUtil.i(TAG, "registerReceiverHandler success");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e2) {
            e2.printStackTrace();
        } catch (NoSuchMethodException e3) {
            e3.printStackTrace();
        } catch (InvocationTargetException e4) {
            e4.printStackTrace();
        }
    }

    private void unregisterCallback() {
        try {
            if (this.mNubiaSysState != null) {
                Method declaredMethod = Class.forName(CLASS_NAME).getDeclaredMethod(METHOD_UNREGISTER, new Class[0]);
                declaredMethod.setAccessible(true);
                declaredMethod.invoke(this.mNubiaSysState, new Object[0]);
            }
            LogUtil.i(TAG, "unregisterReceiver success");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e2) {
            e2.printStackTrace();
        } catch (NoSuchMethodException e3) {
            e3.printStackTrace();
        } catch (InvocationTargetException e4) {
            e4.printStackTrace();
        }
    }

    @Override // android.os.Handler
    public void handleMessage(Message message) {
        super.handleMessage(message);
        Bundle data = message.getData();
        String string = data.getString("packageName");
        String string2 = data.getString("activityName");
        int i = message.what;
        switch (i) {
            case 2000:
                handleTopChange(data);
                break;
            case 2001:
                onActivityResume(string, string2);
                break;
            case 2002:
                onActivityPause(string, string2);
                break;
            case 2003:
                onActivityStop(string, string2);
                break;
            case 2004:
                break;
            case 2005:
                onActivityResumed(string, string2);
                break;
            default:
                switch (i) {
                    case SYS_STATE_APP_START /* 2100 */:
                    case SYS_STATE_APP_STOP /* 2101 */:
                    case SYS_STATE_KEYGUARD /* 2102 */:
                        break;
                    default:
                        String str = "unhandle msg " + message.what;
                        break;
                }
        }
    }

    private void onActivityResumed(String str, String str2) {
        String str3 = TAG;
        LogUtil.d(str3, "--------------------------------------------------------------------------");
        LogUtil.d(str3, "resumed " + str + "/" + str2);
        Set<String> set = this.activityStack;
        if (set != null) {
            set.add(str + "/");
            TopActivityMonitorCallback topActivityMonitorCallback = this.mCallback;
            if (topActivityMonitorCallback != null) {
                topActivityMonitorCallback.onActivityChange(this.activityStack);
            }
        }
    }

    private void onActivityResume(String str, String str2) {
        String str3 = TAG;
        LogUtil.v(str3, "--------------------------------------------------------------------------");
        LogUtil.v(str3, "resume " + str + "/" + str2);
        if (this.activityStack != null) {
            if ("dev.redlab.gamespace".equals(str) && "dev.redlab.gamespace.activity.GameSpaceActivity".equals(str2)) {
                this.activityStack.clear();
            }
            this.activityStack.add(str + "/" + str2);
            TopActivityMonitorCallback topActivityMonitorCallback = this.mCallback;
            if (topActivityMonitorCallback != null) {
                topActivityMonitorCallback.onActivityChange(this.activityStack);
            }
        }
    }

    private void onActivityPause(String str, String str2) {
        String str3 = TAG;
        LogUtil.v(str3, "--------------------------------------------------------------------------");
        LogUtil.v(str3, "pause " + str + "/" + str2);
        Set<String> set = this.activityStack;
        if (set != null) {
            set.remove(str + "/" + str2);
            TopActivityMonitorCallback topActivityMonitorCallback = this.mCallback;
            if (topActivityMonitorCallback != null) {
                topActivityMonitorCallback.onActivityChange(this.activityStack);
            }
        }
    }

    private void onActivityStop(String str, String str2) {
        String str3 = TAG;
        LogUtil.v(str3, "--------------------------------------------------------------------------");
        LogUtil.v(str3, "stop " + str + "/" + str2);
        Set<String> set = this.activityStack;
        if (set != null) {
            set.remove(str + "/" + str2);
            TopActivityMonitorCallback topActivityMonitorCallback = this.mCallback;
            if (topActivityMonitorCallback != null) {
                topActivityMonitorCallback.onActivityChange(this.activityStack);
            }
        }
    }

    private void handleTopChange(Bundle bundle) {
        if (this.activityStack == null) {
            this.activityStack = new HashSet();
            String string = bundle.getString("packageName");
            String string2 = bundle.getString("activityName");
            if (!TextUtils.isEmpty(string) && !TextUtils.isEmpty(string2)) {
                this.activityStack.add(string + "/" + string2);
            }
            int i = 0;
            while (true) {
                if (i >= 100 || !bundle.containsKey("stackId" + i) || !TextUtils.isEmpty(null)) {
                    break;
                }
                String string3 = bundle.getString("packageName" + i);
                if ("com.tencent.mm".equals(string3) || "com.tencent.mobileqq".equals(string3) || "cn.nubia.browser".equals(string3)) {
                    i++;
                } else {
                    String string4 = bundle.getString("activityName" + i);
                    if (!TextUtils.isEmpty(string3) && !TextUtils.isEmpty(string4)) {
                        this.activityStack.add(string3 + "/" + string4);
                    }
                }
            }
            TopActivityMonitorCallback topActivityMonitorCallback = this.mCallback;
            if (topActivityMonitorCallback != null) {
                topActivityMonitorCallback.onActivityChange(this.activityStack);
            }
        }
    }

    private void onAppStop(String str) {
        LogUtil.i(TAG, "onAppStop " + str);
        if (this.activityStack != null && !TextUtils.isEmpty(str)) {
            Iterator<String> it = this.activityStack.iterator();
            while (it.hasNext()) {
                String next = it.next();
                if (next != null && next.startsWith(str + "/")) {
                    it.remove();
                }
            }
        }
        TopActivityMonitorCallback topActivityMonitorCallback = this.mCallback;
        if (topActivityMonitorCallback != null) {
            topActivityMonitorCallback.onActivityChange(this.activityStack);
        }
    }

    public void notifyTopChange() {
        Set<String> set;
        TopActivityMonitorCallback topActivityMonitorCallback = this.mCallback;
        if (topActivityMonitorCallback == null || (set = this.activityStack) == null) {
            return;
        }
        topActivityMonitorCallback.onActivityChange(set);
    }
}
