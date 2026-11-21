package cn.nubia.gamecenter.settings.widget.particle;

import android.opengl.GLES20;

/* loaded from: classes.dex */
public class ParticleShaderProgram extends ShaderProgram {
    public int mAlphaHandle;

    public ParticleShaderProgram(int i) {
        super(i);
        this.mAlphaHandle = GLES20.glGetAttribLocation(i, "aAlpha");
    }
}
