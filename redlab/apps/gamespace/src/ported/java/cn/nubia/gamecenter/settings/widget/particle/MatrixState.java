package cn.nubia.gamecenter.settings.widget.particle;

import android.opengl.Matrix;
import java.util.Stack;

/* loaded from: classes.dex */
public class MatrixState {
    private static final float[] mPMatrix = new float[16];
    private static final float[] mVMatrix = new float[16];
    private static float[] mMMatrix = new float[16];
    private static final Stack<float[]> mMatrixStack = new Stack<>();

    public static void setInitStack() {
        float[] fArr = new float[16];
        mMMatrix = fArr;
        Matrix.setRotateM(fArr, 0, 0.0f, 1.0f, 0.0f, 0.0f);
    }

    public static void pushMatrix() {
        mMatrixStack.push((float[]) mMMatrix.clone());
    }

    public static void popMatrix() {
        mMMatrix = mMatrixStack.pop();
    }

    public static void translate(float f, float f2, float f3) {
        Matrix.translateM(mMMatrix, 0, f, f2, f3);
    }

    public static void rotate(float f, float f2, float f3, float f4) {
        Matrix.rotateM(mMMatrix, 0, f, f2, f3, f4);
    }

    public static void scale(float f, float f2, float f3) {
        Matrix.scaleM(mMMatrix, 0, f, f2, f3);
    }

    public static void setProjectFrustum(float f, float f2, float f3, float f4, float f5, float f6) {
        Matrix.frustumM(mPMatrix, 0, f, f2, f3, f4, f5, f6);
    }

    public static void setProjectOrtho(float f, float f2, float f3, float f4, float f5, float f6) {
        Matrix.orthoM(mPMatrix, 0, f, f2, f3, f4, f5, f6);
    }

    public static void setCamera(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9) {
        Matrix.setLookAtM(mVMatrix, 0, f, f2, f3, f4, f5, f6, f7, f8, f9);
    }

    public static void rotateCamera(float f, float f2, float f3, float f4) {
        Matrix.rotateM(mVMatrix, 0, f, f2, f3, f4);
    }

    public static float[] getMVPMatrix() {
        float[] fArr = new float[16];
        Matrix.multiplyMM(fArr, 0, mVMatrix, 0, mMMatrix, 0);
        Matrix.multiplyMM(fArr, 0, mPMatrix, 0, fArr, 0);
        return fArr;
    }

    public static float[] getVPMatrix() {
        float[] fArr = new float[16];
        Matrix.multiplyMM(fArr, 0, mPMatrix, 0, mVMatrix, 0);
        return fArr;
    }

    public static float[] getMMatrix() {
        return mMMatrix;
    }

    public static float[] getVMatrix() {
        return mVMatrix;
    }

    public static float[] getPMatrix() {
        return mPMatrix;
    }
}
