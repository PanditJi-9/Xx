package cn.nubia.gamecenter.settings.widget.particle;

import android.opengl.GLES20;

/* loaded from: classes.dex */
public class ShaderProgram {
    public int mColorHandle;
    public int mId;
    public int mMVPMatrixHandle;
    public int mTexCoorHandle;
    public int mVertexHandle;

    public ShaderProgram(int i) {
        this.mId = i;
        this.mMVPMatrixHandle = GLES20.glGetUniformLocation(i, "uMVPMatrix");
        this.mVertexHandle = GLES20.glGetAttribLocation(this.mId, "aVertex");
        this.mTexCoorHandle = GLES20.glGetAttribLocation(this.mId, "aTexCoor");
        this.mColorHandle = GLES20.glGetUniformLocation(i, "uColor");
    }
}
