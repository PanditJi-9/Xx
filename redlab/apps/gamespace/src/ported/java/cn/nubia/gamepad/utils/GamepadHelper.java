package cn.nubia.gamepad.utils;

import android.bluetooth.BluetoothDevice;
import android.text.TextUtils;
import dev.redlab.gamespace.util.BluetoothUtils;
import java.util.Iterator;

/* loaded from: classes2.dex */
public class GamepadHelper {
    public static final String TAG = "Gamepad_Helper";

    public static boolean isBluetoothConnected(String str) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        String upperCase = str.toUpperCase();
        Iterator<BluetoothDevice> it = BluetoothUtils.getSystemConnectedDevices().iterator();
        while (it.hasNext()) {
            if (upperCase.equals(it.next().getAddress())) {
                return true;
            }
        }
        return false;
    }
}
