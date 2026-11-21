package cn.nubia.gamecenter.settings.records;

import java.util.ArrayList;

/* loaded from: classes.dex */
public class RecordsTester {
    private static final String IMAGE_PATH = "/storage/emulated/0/Pictures/Screenshots";
    private static final String TAG = "RecordsTester";
    private static final String VIDEO_PATH = "/storage/emulated/0/Pictures/Screen Recorder";

    public String getVideoDataPath(String str) {
        return VIDEO_PATH;
    }

    public String getImageDataPath() {
        return IMAGE_PATH;
    }

    public void addBaseInfo(ArrayList<String> arrayList, ArrayList<String> arrayList2) {
        arrayList.add("测试名称");
        arrayList2.add("cn.nubia.test.video_image");
    }
}
