package cn.nubia.gamecenter.settings.widget.particle;

/* loaded from: classes.dex */
public abstract class absParticle {
    public float mAx;
    public float mAy;
    public float mAz;
    public boolean mIsAlive;
    public float mLifeTime = 0.0f;
    public float mRadius;
    public float mVx;
    public float mVy;
    public float mVz;
    public float mX;
    public float mY;
    public float mZ;

    public abstract void update(float f);
}
