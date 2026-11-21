package cn.nubia.gamepad.utils;

import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

/* loaded from: classes2.dex */
public class GamepadContentHelper {
    public static final String DB_GAME_DEVICES_CONNECT_INFOS = "nubia_operation_device_connect_infos";
    private static final String DB_GAME_DEVICES_ENABLE = "nubia_operation_devices_enable";
    private static final String DB_GAME_DEVICES_INFOS = "nubia_operation_device_infos";
    public static final String DB_GAME_DEVICES_STATE = "nubia_operation_devices_state";
    private static final String DB_GAME_FIRST_USE_OPERATION_DEVICE = "nubia_first_use_operation_device";
    private static final String DB_GAME_SELECT_DEVICE_INFO = "nubia_operation_select_device_infos";
    public static final int GAMEPAD_STATE = 1;
    public static final int KEYBOARD_MOUSE_STATE = 2;
    public static final int NONE_STATE = 0;
    private static final String TAG = "Gamepad_ContentHelper";
    public static final String VIBRATE_INPUT_DEVICES = "vibrate_input_devices";

    public static void setVibrateDevices(Context context, int i) {
        Settings.System.putInt(context.getContentResolver(), VIBRATE_INPUT_DEVICES, i);
    }

    public static int getGameDeviceState(Context context) {
        return Settings.Global.getInt(context.getContentResolver(), DB_GAME_DEVICES_STATE, -1);
    }

    public static void setGameDeviceState(Context context, int i) {
        Log.d(TAG, "setGameDeviceState state=" + i);
        Settings.Global.putInt(context.getContentResolver(), DB_GAME_DEVICES_STATE, i);
    }

    public static String getDbGameDevicesConnectInfos(Context context) {
        return Settings.Global.getString(context.getContentResolver(), DB_GAME_DEVICES_CONNECT_INFOS);
    }

    public static void setDbGameDevicesConnectInfos(Context context, String str) {
        Log.d(TAG, "setDbGameDevicesConnectInfos gameDeviceConnectInfos=" + str);
        Settings.Global.putString(context.getContentResolver(), DB_GAME_DEVICES_CONNECT_INFOS, str);
    }

    public static boolean isBluetoothConnected(Context context, int i) {
        String dbGameDevicesConnectInfos = getDbGameDevicesConnectInfos(context);
        Log.d(TAG, "isBluetoothConnected devicesInfosStr=" + dbGameDevicesConnectInfos);
        if (!TextUtils.isEmpty(dbGameDevicesConnectInfos)) {
            for (String str : dbGameDevicesConnectInfos.split(";")) {
                if (!TextUtils.isEmpty(str)) {
                    String[] split = str.split(",");
                    if (split.length == 2 && !TextUtils.isEmpty(split[0]) && !TextUtils.isEmpty(split[1]) && Integer.parseInt(split[0]) == i) {
                        return Boolean.parseBoolean(split[1]);
                    }
                }
            }
        }
        return false;
    }

    public static String getDeviceUniqueid(Context context, int i) {
        String string = Settings.Global.getString(context.getContentResolver(), DB_GAME_DEVICES_INFOS);
        Log.d(TAG, "getDeviceid devicesInfosStr=" + string);
        if (!TextUtils.isEmpty(string)) {
            for (String str : string.split(";")) {
                if (!TextUtils.isEmpty(str)) {
                    String[] split = str.split(",");
                    if (split.length == 4 && !TextUtils.isEmpty(split[2]) && Integer.parseInt(split[2]) == i) {
                        Log.d(TAG, "deviceInfo[1] =" + split[1]);
                        return split[1];
                    }
                }
            }
            return "";
        }
        return "";
    }
}
