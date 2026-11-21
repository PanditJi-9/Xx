package cn.nubia.tgk.util;

import android.os.IBinder;
import android.os.IInterface;
import android.util.Log;
import java.lang.reflect.InvocationTargetException;

/* loaded from: classes2.dex */
public class ImeHeightUtils {
    private static final String TAG = "ImeHeightUtils";

    public static int getImeHeight() {
        try {
            IBinder iBinder = (IBinder) Class.forName("android.os.ServiceManager").getMethod("getService", String.class).invoke(null, "window");
            if (iBinder == null) {
                Log.e(TAG, "Failed to get WindowManagerService");
                return 0;
            }
            IInterface iInterface = (IInterface) Class.forName("android.view.IWindowManager$Stub").getMethod("asInterface", IBinder.class).invoke(null, iBinder);
            if (iInterface == null) {
                Log.e(TAG, "Failed to get IWindowManager interface");
                return 0;
            }
            int intValue = ((Integer) iInterface.getClass().getMethod("getImeWindowHeight", new Class[0]).invoke(iInterface, new Object[0])).intValue();
            Log.d(TAG, "IME Height: " + intValue);
            return intValue;
        } catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            Log.e(TAG, "Reflection error: ", e);
            return 0;
        }
    }
}
