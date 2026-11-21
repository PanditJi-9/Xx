package dev.redlab.gamespace.gamecontrolpanel.utils;

import android.os.Build;

/* loaded from: classes.dex */
public class PerformanceConstant {
    public static final String DEFAULT_PATH_CUR_CPU_MAIN = "/sys/devices/system/cpu/cpu7/cpufreq/scaling_cur_freq";
    public static final String DEFAULT_PATH_CUR_CPU_MIDDLE = "/sys/devices/system/cpu/cpu4/cpufreq/scaling_cur_freq";
    public static final String DEFAULT_PATH_CUR_CPU_MINOR = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq";
    public static final String DEFAULT_PATH_CUR_GPU = "/sys/class/kgsl/kgsl-3d0/gpuclk";
    public static final String DEFAULT_PATH_MAX_CPU_MAIN = "/sys/devices/system/cpu/cpu7/cpufreq/cpuinfo_max_freq";
    public static final String DEFAULT_PATH_MAX_GPU = "/sys/class/kgsl/kgsl-3d0/max_gpuclk";
    public static final String MTK_GPU_STATUS = "/proc/gpufreqv2/gpufreq_status";
    public static final String MTK_GPU_TABLE = "/proc/gpufreqv2/gpu_signed_opp_table";
    public static final String SPRD_DEFAULT_PATH_CUR_GPU = "/sys/devices/platform/soc/soc:mm/23140000.gpu/devfreq/23140000.gpu/cur_freq";
    public static final String SPRD_DEFAULT_PATH_CUR_GPU_ANDROIDV = "/sys/devices/platform/soc/soc:mm/23100000.gpu/devfreq/23100000.gpu/cur_freq";
    public static final String SPRD_DEFAULT_PATH_MAX_GPU = "/sys/devices/platform/soc/soc:mm/23140000.gpu/devfreq/23140000.gpu/max_freq";
    public static final String SPRD_DEFAULT_PATH_MAX_GPU_ANDROIDV = "/sys/devices/platform/soc/soc:mm/23100000.gpu/devfreq/23100000.gpu/max_freq";
    public static final boolean MTK_CHIP = "Mediatek".equals(Build.SOC_MANUFACTURER);
    public static final boolean SPREAD_CHIP = "Spreadtrum".equals(Build.SOC_MANUFACTURER);
}
