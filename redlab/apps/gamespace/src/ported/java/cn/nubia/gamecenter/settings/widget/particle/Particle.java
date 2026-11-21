package cn.nubia.gamecenter.settings.widget.particle;

/* loaded from: classes.dex */
public class Particle extends absParticle {
    private static final int ALPHA_TIME = 300;
    float mAlpha;
    float mMaxAlpha;
    int mMaxLifeTime;

    @Override // cn.nubia.gamecenter.settings.widget.particle.absParticle
    public void update(float f) {
        this.mLifeTime += f;
        if (this.mLifeTime > this.mMaxLifeTime) {
            this.mIsAlive = false;
            return;
        }
        this.mAlpha = this.mMaxAlpha;
        if (this.mLifeTime > this.mMaxLifeTime - 300) {
            this.mAlpha = this.mMaxAlpha * (1.0f - (((this.mLifeTime - this.mMaxLifeTime) + 300.0f) / 300.0f));
        }
        this.mX += (this.mVx * f) + (((this.mAx * f) * f) / 2.0f);
        this.mY += (this.mVy * f) + (((this.mAy * f) * f) / 2.0f);
        this.mZ += (this.mVz * f) + (((this.mAz * f) * f) / 2.0f);
        this.mVx += this.mAx * f;
        this.mVy += this.mAy * f;
        this.mVz += this.mAz * f;
    }
}
