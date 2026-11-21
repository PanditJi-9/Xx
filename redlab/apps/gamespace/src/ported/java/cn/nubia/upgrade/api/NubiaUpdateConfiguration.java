package cn.nubia.upgrade.api;

/* loaded from: classes2.dex */
public class NubiaUpdateConfiguration {
    private boolean mAllowMobileNetwork;
    private String mDownloadPath;
    private boolean mShowNotification;
    private boolean mSilentDownload;
    private boolean mSilentInstall;

    private NubiaUpdateConfiguration(Builder builder) {
        this.mSilentInstall = false;
        this.mSilentDownload = false;
        this.mShowNotification = false;
        this.mAllowMobileNetwork = false;
    }

    public static class Builder {
        public Builder setAllowMobileNetwork(boolean z) {
            return this;
        }

        public Builder setDownloadPath(String str) {
            return this;
        }

        public Builder setDownloadRunMode(RunMode runMode) {
            return this;
        }

        public Builder setInstallRunMode(RunMode runMode) {
            return this;
        }

        public Builder setSilentDownload(boolean z) {
            return this;
        }

        public Builder setSilentInstall(boolean z) {
            return this;
        }

        public NubiaUpdateConfiguration build() {
            return new NubiaUpdateConfiguration(this);
        }
    }

    public String getDownloadPath() {
        return "";
    }
}
