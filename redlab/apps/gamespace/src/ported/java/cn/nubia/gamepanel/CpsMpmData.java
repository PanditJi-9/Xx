package cn.nubia.gamepanel;

/* loaded from: classes2.dex */
public class CpsMpmData {
    private static CpsMpmData instance;
    private int cps = 0;
    private int mpm = 0;
    private long startTime = 0;

    public static CpsMpmData getInstance() {
        if (instance == null) {
            instance = new CpsMpmData();
        }
        return instance;
    }

    public long getStartTime() {
        return this.startTime;
    }

    public void setStartTime(long j) {
        this.startTime = j;
    }

    public int getCps() {
        return this.cps;
    }

    public void setCps(int i) {
        this.cps = i;
    }

    public int getMpm() {
        return this.mpm;
    }

    public void setMpm(int i) {
        this.mpm = i;
    }

    public void clearData() {
        this.cps = 0;
        this.mpm = 0;
        this.startTime = 0L;
    }
}
