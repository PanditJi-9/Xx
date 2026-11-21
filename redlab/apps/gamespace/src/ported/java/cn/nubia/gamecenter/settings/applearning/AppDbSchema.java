package cn.nubia.gamecenter.settings.applearning;

import android.net.Uri;

/* loaded from: classes.dex */
public class AppDbSchema {

    public static final class AppTable {
        public static final String ADD_ONE_DAY_INITIALLY_SQL = "alter table onedayreport add is_initially text ";
        public static final String CREATE_ONE_DAY_SQL = "create table onedayreport (_id integer primary key autoincrement,package_name text,event_name text unique,action_type text ,action_value text ,report_interval integer ,is_initially text );";
        public static final String CREATE_SQL = "create table appusage (_id integer primary key autoincrement,packageName text,beginTimeStamp integer ,endTimeStamp integer ,lastTimeUsed integer ,totalTimeInForeground integer ,launchDate integer ,appLaunchCount integer ,lastEvent integer ,performanceMode integer);";
        public static final String DROP_ONE_DAY_SQL = "drop table if exists onedayreport";
        public static final String DROP_SQL = "drop table if exists appusage";
        public static final String NAME = "appusage";
        public static final String NAME_ONE_DAY = "onedayreport";
        public static final Uri CONTENT_URI = Uri.parse("content://cn.nubia.owlsystem/appusage");
        public static final Uri CONTENT_ONE_DAY_REPORT_URI = Uri.parse("content://cn.nubia.owlsystem/oneday");

        public static final class Cols {
            public static final String APP_LAUNCH_COUNT = "appLaunchCount";
            public static final int APP_LAUNCH_COUNT_INDEX = 7;
            public static final String BEGIN_TIME_STAMP = "beginTimeStamp";
            public static final int BEGIN_TIME_STAMP_INDEX = 2;
            public static final String END_TIME_STAMP = "endTimeStamp";
            public static final int END_TIME_STAMP_INDEX = 3;
            public static final String ID = "_id";
            public static final int ID_INDEX = 0;
            public static final String LAST_EVENT = "lastEvent";
            public static final int LAST_EVENT_INDEX = 8;
            public static final String LAST_TIME_USED = "lastTimeUsed";
            public static final int LAST_TIME_USED_INDEX = 4;
            public static final String LAUNCH_DATE = "launchDate";
            public static final int LAUNCH_DATE_INDEX = 6;
            public static final String PACKAGE_NAME = "packageName";
            public static final int PACKAGE_NAME_INDEX = 1;
            public static final String PERFORMANCE_MODE = "performanceMode";
            public static final int PERFORMANCE_MODE_INDEX = 9;
            public static final String TOTAL_TIME_IN_FOREGROUND = "totalTimeInForeground";
            public static final int TOTAL_TIME_IN_FOREGROUND_INDEX = 5;
        }

        public static final class OneDayCols {
            public static final String ACTION_TYPE = "action_type";
            public static final int ACTION_TYPE_INDEX = 3;
            public static final String ACTION_VALUE = "action_value";
            public static final int ACTION_VALUE_INDEX = 4;
            public static final String EVENT_NAME = "event_name";
            public static final int EVENT_NAME_INDEX = 2;
            public static final String ID = "_id";
            public static final int ID_INDEX = 0;
            public static final String IS_INITIALLY = "is_initially";
            public static final int IS_INITIALLY_INDEX = 6;
            public static final String PACKAGE_NAME = "package_name";
            public static final int PACKAGE_NAME_INDEX = 1;
            public static final String REPORT_INTERVAL = "report_interval";
            public static final int REPORT_INTERVAL_INDEX = 5;
        }
    }

    public static final class OneKeyLockedAppsTable {
        public static final Uri CONTENT_URI = Uri.parse("content://cn.nubia.apptimelock/app_time_one_key");
        public static final String NAME = "app_time_one_key";

        public static final class Cols {
            public static final String DELAY_TIME = "delay";
            public static final int DELAY_TIME_INDEX = 2;
            public static final String LILIT_ENABLE = "enable";
            public static final int LILIT_ENABLE_INDEX = 3;
            public static final String LIMIT_TIME_STAMP = "time";
            public static final int LIMIT_TIME_STAMP_INDEX = 1;
            public static final String PACKAGE_NAME = "packagename";
            public static final int PACKAGE_NAME_INDEX = 0;
        }
    }
}
