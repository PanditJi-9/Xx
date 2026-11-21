package dev.redlab.gamespace.gamecontrolpanel.virtual.db;

import android.net.Uri;
import android.provider.BaseColumns;

/* loaded from: classes.dex */
public class DBConstant implements BaseColumns {
    public static final String AUTHORITY = "cn.nubia.virtualgamehandle";
    public static final String LEFT_GAME_HANDLE_STYLE = "0";
    public static final String PACKAGE_NAME = "package_name";
    public static final String RIGHT_GAME_HANDLE_STYLE_1 = "1";
    public static final String RIGHT_GAME_HANDLE_STYLE_2 = "2";
    public static final String RIGHT_GAME_HANDLE_STYLE_3 = "3";
    public static final String RIGHT_GAME_HANDLE_STYLE_4 = "4";
    public static final String RIGHT_GAME_HANDLE_STYLE_5 = "5";
    public static final String RIGHT_GAME_HANDLE_STYLE_6 = "6";
    public static final String TABLE_APP_GAME_HANDLE = "app_game_handle";
    public static final String TYPE = "type";
    public static final String TYPE_CUSTOMED = "1";
    public static final String TYPE_OFFICIAL = "0";
    public static final Uri URI_APP_GAME_HANDLE = Uri.parse("content://cn.nubia.virtualgamehandle/app_game_handle");
    public static final Uri URI_SWITCH_GAME_HANDLE = Uri.parse("content://cn.nubia.virtualgamehandle/switch");
    public static final Uri URI_SWITCH_GAME_IMAGE = Uri.parse("content://cn.nubia.virtualgamehandle/image");
    public static final String TITLE = "title";
    public static final String CUT_SIZE = "cut_size";
    public static final String RIGHT_GAME_HANDLE_STYLE = "right_game_handle_style";
    public static final String DEFAULT_CONFIG = "default_config";
    public static final String CURRENT_CONFIG = "current_config";
    public static final String IMAGE_URL = "image_url";
    public static final String LEFT_JOYSTICK = "left_joystick";
    public static final String RIGHT_JOYSTICK = "right_joystick";
    public static final String LEFT_ENTITY_KEY = "left_entity_key";
    public static final String RIGHT_ENTITY_KEY = "right_entity_key";
    public static final String LEFT_ARROW_KEY = "left_arrow_key";
    public static final String RIGHT_ARROW_KEY = "right_arrow_key";
    public static final String UP_ARROW_KEY = "up_arrow_key";
    public static final String DOWN_ARROW_KEY = "down_arrow_key";
    public static final String LETTER_A_KEY = "letter_A_key";
    public static final String LETTER_A1_KEY = "letter_A1_key";
    public static final String LETTER_A2_KEY = "letter_A2_key";
    public static final String LETTER_B_KEY = "letter_B_key";
    public static final String LETTER_X_KEY = "letter_X_key";
    public static final String LETTER_Y_KEY = "letter_Y_key";
    public static final String LETTER_Z_KEY = "letter_Z_key";
    public static String[] PROJECTION_APP_GAME_HANDLE_DATA = {"_id", TITLE, "package_name", CUT_SIZE, RIGHT_GAME_HANDLE_STYLE, DEFAULT_CONFIG, CURRENT_CONFIG, "type", IMAGE_URL, LEFT_JOYSTICK, RIGHT_JOYSTICK, LEFT_ENTITY_KEY, RIGHT_ENTITY_KEY, LEFT_ARROW_KEY, RIGHT_ARROW_KEY, UP_ARROW_KEY, DOWN_ARROW_KEY, LETTER_A_KEY, LETTER_A1_KEY, LETTER_A2_KEY, LETTER_B_KEY, LETTER_X_KEY, LETTER_Y_KEY, LETTER_Z_KEY};
}
