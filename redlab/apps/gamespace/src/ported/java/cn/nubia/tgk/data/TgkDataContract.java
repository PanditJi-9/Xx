package cn.nubia.tgk.data;

import android.provider.BaseColumns;

/* loaded from: classes2.dex */
public class TgkDataContract {
    public static final int GAME_MORE_INFO_TABLE_ID = 10;
    public static final int IMPORT_TABLE_ID = 1;
    public static final int LAMP_TABLE_ID = 2;
    public static final String POINT_DEFAULT_VALUE_L = "430|519|514|603|0|0|0|0";
    public static final String POINT_DEFAULT_VALUE_M = "864|587|948|671|0|0|0|0";
    public static final String POINT_DEFAULT_VALUE_R = "1139|680|1223|764|0|0|0|0";
    public static final int PRESET_TABLE_ID = 0;
    public static final int SENSITIVITY_HIGHT = 3;
    public static final int SENSITIVITY_LOW = 1;
    public static final int SENSITIVITY_NORMAL = 2;

    public static final class TgkEntry implements BaseColumns {
        public static final String GAME_MORE_INFO_TABLE_NAME = "game_more_info_table";
        public static final String IMPORT_CASE_TABLE_NAME = "import_case_table";
        public static final String LAMP_CASE_STATE = "state";
        public static final String LAMP_CASE_TABLE_NAME = "lamp_case_table";
        public static final String LAMP_PACKAGE_NAME = "package_name";
        public static final String PRESET_CASE_TABLE_NAME = "preset_case_table";
        public static final String TGK_CASE_CHANGE = "change";
        public static final String TGK_CASE_KEY = "uniqueId";
        public static final String TGK_CASE_L_LINK_OPTION = "left_link_option";
        public static final String TGK_CASE_L_OPTION = "left_option";
        public static final String TGK_CASE_L_POINTS = "left_points";
        public static final String TGK_CASE_L_SENSITIVITY = "left_sensitivity";
        public static final String TGK_CASE_L_SW = "left_sw";
        public static final String TGK_CASE_MAIN_SW = "main_sw";
        public static final String TGK_CASE_M_OPTION = "middle_option";
        public static final String TGK_CASE_M_POINTS = "middle_points";
        public static final String TGK_CASE_M_SW = "middle_sw";
        public static final String TGK_CASE_ORG_NAME = "original_name";
        public static final String TGK_CASE_PICTURE = "picture";
        public static final String TGK_CASE_R_LINK_OPTION = "right_link_option";
        public static final String TGK_CASE_R_OPTION = "right_option";
        public static final String TGK_CASE_R_POINTS = "right_points";
        public static final String TGK_CASE_R_SENSITIVITY = "right_sensitivity";
        public static final String TGK_CASE_R_SW = "right_sw";
        public static final String TGK_CASE_SHOT_PICTURE = "shot_picture";
        public static final String TGK_CASE_SHOW_NAME = "show_name";
        public static final String TGK_CASE_STATE = "state";
        public static final String TGK_CASE_UPDATE_TIME = "update_time";
        public static final String TGK_CASE_VIBRATE_SW = "vibrate_sw";
        public static final String TGK_CENTER_VISUAL_EFFECT_SW = "center_visual_effect_sw";
        public static final String TGK_CENTER_VISUAL_EFFECT_TRANSPARENCY = "center_visual_effect_transparency";
        public static final String TGK_IS_LANDSCAPE = "isLandscape";
        public static final String TGK_PACKAGE_NAME = "package_name";
        public static final String TGK_TOP_VISUAL_EFFECT_SW = "top_visual_effect_sw";
        public static final String _ID = "_id";
    }

    private TgkDataContract() {
    }
}
