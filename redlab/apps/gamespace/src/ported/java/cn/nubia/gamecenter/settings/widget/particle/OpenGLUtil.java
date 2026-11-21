package cn.nubia.gamecenter.settings.widget.particle;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import java.nio.IntBuffer;
import java.util.List;

/* loaded from: classes.dex */
public class OpenGLUtil {
    public static int addTexture(Bitmap bitmap) {
        return addTexture(bitmap, 9729);
    }

    public static int addTexture(Bitmap bitmap, int i) {
        int[] iArr = new int[1];
        GLES20.glGenTextures(1, iArr, 0);
        int i2 = iArr[0];
        GLES20.glBindTexture(3553, i2);
        float f = i;
        GLES20.glTexParameterf(3553, 10241, f);
        GLES20.glTexParameterf(3553, 10240, f);
        GLES20.glTexParameterf(3553, 10242, 10497.0f);
        GLES20.glTexParameterf(3553, 10243, 10497.0f);
        GLUtils.texImage2D(3553, 0, bitmap, 0);
        return i2;
    }

    public static void delTextures(int[] iArr) {
        int length = iArr.length;
        if (length <= 0) {
            return;
        }
        IntBuffer allocate = IntBuffer.allocate(length);
        for (int i : iArr) {
            allocate.put(i);
        }
        allocate.position(0);
        GLES20.glDeleteTextures(length, allocate);
        allocate.clear();
    }

    public static void delTextures(List<Integer> list) {
        int size = list.size();
        if (size <= 0) {
            return;
        }
        IntBuffer allocate = IntBuffer.allocate(size);
        for (int i = 0; i < size; i++) {
            allocate.put(list.get(i).intValue());
        }
        allocate.position(0);
        GLES20.glDeleteTextures(size, allocate);
        allocate.clear();
    }
}
