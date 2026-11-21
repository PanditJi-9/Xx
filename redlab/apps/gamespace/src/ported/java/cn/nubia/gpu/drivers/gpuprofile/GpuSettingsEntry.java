package cn.nubia.gpu.drivers.gpuprofile;

import android.os.Parcel;
import android.os.Parcelable;

/* loaded from: classes2.dex */
public class GpuSettingsEntry implements Parcelable {
    public static final Parcelable.Creator<GpuSettingsEntry> CREATOR = new Parcelable.Creator<GpuSettingsEntry>() { // from class: cn.nubia.gpu.drivers.gpuprofile.GpuSettingsEntry.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public GpuSettingsEntry createFromParcel(Parcel parcel) {
            return new GpuSettingsEntry(parcel);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public GpuSettingsEntry[] newArray(int i) {
            return new GpuSettingsEntry[i];
        }
    };
    private int autoVRS;
    private int fpsCap;
    private int gpuSetting;
    private int maxSamples;
    private int mipLodBias;
    private int textureFilteringQuality;
    private int textureMaxAniso;
    private int vsync;

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    public int getGpuSetting() {
        return this.gpuSetting;
    }

    public void setGpuSetting(int i) {
        this.gpuSetting = i;
    }

    public int getAutoVRS() {
        return this.autoVRS;
    }

    public void setAutoVRS(int i) {
        this.autoVRS = i;
    }

    public int getMaxSamples() {
        return this.maxSamples;
    }

    public void setMaxSamples(int i) {
        this.maxSamples = i;
    }

    public int getTextureMaxAniso() {
        return this.textureMaxAniso;
    }

    public void setTextureMaxAniso(int i) {
        this.textureMaxAniso = i;
    }

    public int getTextureFilteringQuality() {
        return this.textureFilteringQuality;
    }

    public void setTextureFilteringQuality(int i) {
        this.textureFilteringQuality = i;
    }

    public int getMipLodBias() {
        return this.mipLodBias;
    }

    public void setMipLodBias(int i) {
        this.mipLodBias = i;
    }

    public int getVsync() {
        return this.vsync;
    }

    public void setVsync(int i) {
        this.vsync = i;
    }

    public int getFpsCap() {
        return this.fpsCap;
    }

    public void setFpsCap(int i) {
        this.fpsCap = i;
    }

    public GpuSettingsEntry() {
        this.gpuSetting = 0;
        this.autoVRS = 0;
        this.maxSamples = 1;
        this.textureMaxAniso = 2;
        this.textureFilteringQuality = 1;
    }

    protected GpuSettingsEntry(Parcel parcel) {
        this.gpuSetting = 0;
        this.autoVRS = 0;
        this.maxSamples = 1;
        this.textureMaxAniso = 2;
        this.textureFilteringQuality = 1;
        this.gpuSetting = parcel.readInt();
        this.autoVRS = parcel.readInt();
        this.maxSamples = parcel.readInt();
        this.textureMaxAniso = parcel.readInt();
        this.textureFilteringQuality = parcel.readInt();
        this.mipLodBias = parcel.readInt();
        this.vsync = parcel.readInt();
        this.fpsCap = parcel.readInt();
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.gpuSetting);
        parcel.writeInt(this.autoVRS);
        parcel.writeInt(this.maxSamples);
        parcel.writeInt(this.textureMaxAniso);
        parcel.writeInt(this.textureFilteringQuality);
        parcel.writeInt(this.mipLodBias);
        parcel.writeInt(this.vsync);
        parcel.writeInt(this.fpsCap);
    }

    public String toString() {
        return "GpuSettingsEntry{gpuSetting=" + this.gpuSetting + ", autoVRS=" + this.autoVRS + ", maxSamples=" + this.maxSamples + ", textureMaxAniso=" + this.textureMaxAniso + ", textureFilteringQuality=" + this.textureFilteringQuality + ", mipLodBias=" + this.mipLodBias + ", vsync=" + this.vsync + ", fpsCap=" + this.fpsCap + '}';
    }
}
