package dev.redlab.gamespace.util;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/* loaded from: classes2.dex */
public class BluetoothUtils {
    public static List<BluetoothDevice> getSystemConnectedDevices() {
        BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
        ArrayList arrayList = new ArrayList();
        if (defaultAdapter == null) {
            return arrayList;
        }
        try {
            Method declaredMethod = BluetoothAdapter.class.getDeclaredMethod("getConnectionState", null);
            declaredMethod.setAccessible(true);
            if (((Integer) declaredMethod.invoke(defaultAdapter, null)).intValue() == 2) {
                for (BluetoothDevice bluetoothDevice : defaultAdapter.getBondedDevices()) {
                    Method declaredMethod2 = BluetoothDevice.class.getDeclaredMethod("isConnected", null);
                    declaredMethod2.setAccessible(true);
                    if (((Boolean) declaredMethod2.invoke(bluetoothDevice, null)).booleanValue()) {
                        arrayList.add(bluetoothDevice);
                    }
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e2) {
            e2.printStackTrace();
        } catch (InvocationTargetException e3) {
            e3.printStackTrace();
        }
        return arrayList;
    }

    public static boolean isBond(String str) {
        Set<BluetoothDevice> bondedDevices;
        BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
        if (defaultAdapter != null && (bondedDevices = defaultAdapter.getBondedDevices()) != null) {
            Iterator<BluetoothDevice> it = bondedDevices.iterator();
            while (it.hasNext()) {
                if (it.next().getAddress().equals(str)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean removeBond(BluetoothDevice bluetoothDevice) {
        try {
            Method declaredMethod = BluetoothDevice.class.getDeclaredMethod("removeBond", null);
            declaredMethod.setAccessible(true);
            return ((Boolean) declaredMethod.invoke(bluetoothDevice, null)).booleanValue();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return false;
        } catch (NoSuchMethodException e2) {
            e2.printStackTrace();
            return false;
        } catch (InvocationTargetException e3) {
            e3.printStackTrace();
            return false;
        }
    }
}
