#ifndef _GNU_SOURCE
#define _GNU_SOURCE
#endif

#include "ThreadAffinity.h"
#include "SysfsDirect.h"

#include <dirent.h>
#include <sched.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#define TA_MAX_CPUS 32

static const char *kCriticalThreadPatterns[] = {
    "UnityMain",
    "UnityGfxDevice",
    "GameThread",
    "RenderThread",
    "RHIThread",
    "Vulkan",
    "GLThread",
    "Job.Worker",
    "MainThread",
    "Render",
    "WorkerThread",
    NULL
};

static bool is_critical_thread_name(const char *comm) {
    if (!comm || !comm[0]) return false;
    for (int i = 0; kCriticalThreadPatterns[i] != NULL; ++i) {
        if (strstr(comm, kCriticalThreadPatterns[i]) != NULL) {
            return true;
        }
    }
    return false;
}

static bool apply_tid_mask(pid_t tid, uint64_t mask, int total_cpus) {
    if (tid <= 0 || mask == 0) return false;
    cpu_set_t set;
    CPU_ZERO(&set);
    for (int cpu = 0; cpu < total_cpus && cpu < CPU_SETSIZE; ++cpu) {
        if (mask & (1ULL << cpu)) {
            CPU_SET(cpu, &set);
        }
    }
    return (sched_setaffinity(tid, sizeof(set), &set) == 0);
}

bool ta_detect_topology(CpuTopology *out_topo) {
    if (!out_topo) return false;
    memset(out_topo, 0, sizeof(*out_topo));

    long ncpu = sysconf(_SC_NPROCESSORS_CONF);
    if (ncpu <= 0) ncpu = 8;
    if (ncpu > TA_MAX_CPUS) ncpu = TA_MAX_CPUS;
    out_topo->total_cpus = (int)ncpu;

    int freqs[TA_MAX_CPUS];
    int min_freq = 0x7fffffff;
    int max_freq = 0;

    for (int i = 0; i < out_topo->total_cpus; ++i) {
        char path[128];
        snprintf(path, sizeof(path),
                 "/sys/devices/system/cpu/cpu%d/cpufreq/cpuinfo_max_freq", i);
        int f = 0;
        if (!sysfs_read_int(path, &f) || f <= 0) {
            f = 1800000;
        }
        freqs[i] = f;
        if (f < min_freq) min_freq = f;
        if (f > max_freq) max_freq = f;
        out_topo->all_mask |= (1ULL << i);
    }

    /* في المعالجات ذات الأنوية الموحدة (All-Big مثل Dimensity 8400) نصنف النصف الأعلى كـ Big/Prime */
    if (min_freq == max_freq) {
        int split = out_topo->total_cpus / 2;
        for (int i = 0; i < out_topo->total_cpus; ++i) {
            if (i < split) out_topo->little_mask |= (1ULL << i);
            else out_topo->big_medium_mask |= (1ULL << i);
        }
    } else {
        int threshold = min_freq + ((max_freq - min_freq) / 3);
        for (int i = 0; i < out_topo->total_cpus; ++i) {
            if (freqs[i] <= threshold) {
                out_topo->little_mask |= (1ULL << i);
            } else {
                out_topo->big_medium_mask |= (1ULL << i);
            }
        }
    }

    if (out_topo->big_medium_mask == 0) out_topo->big_medium_mask = out_topo->all_mask;
    if (out_topo->little_mask == 0) out_topo->little_mask = out_topo->all_mask;
    return true;
}

int ta_pin_game_critical_threads(pid_t game_pid, const CpuTopology *topo) {
    if (game_pid <= 1 || !topo) return 0;

    char task_dir[64];
    snprintf(task_dir, sizeof(task_dir), "/proc/%d/task", (int)game_pid);

    DIR *dir = opendir(task_dir);
    if (!dir) return 0;

    int pinned = 0;
    struct dirent *ent;
    while ((ent = readdir(dir)) != NULL) {
        if (ent->d_name[0] < '0' || ent->d_name[0] > '9') continue;
        pid_t tid = (pid_t)atoi(ent->d_name);
        if (tid <= 0) continue;

        char comm_path[128];
        char comm[64];
        snprintf(comm_path, sizeof(comm_path), "/proc/%d/task/%d/comm", (int)game_pid, (int)tid);
        if (sysfs_read_str(comm_path, comm, sizeof(comm))) {
            if (is_critical_thread_name(comm)) {
                if (apply_tid_mask(tid, topo->big_medium_mask, topo->total_cpus)) {
                    pinned++;
                }
            }
        }
    }
    closedir(dir);
    return pinned;
}

bool ta_pin_pid_to_little_cores(pid_t bg_pid, const CpuTopology *topo) {
    if (bg_pid <= 1 || !topo) return false;
    return apply_tid_mask(bg_pid, topo->little_mask, topo->total_cpus);
}

int ta_restore_pid_affinity(pid_t pid, const CpuTopology *topo) {
    if (pid <= 1 || !topo) return 0;

    char task_dir[64];
    snprintf(task_dir, sizeof(task_dir), "/proc/%d/task", (int)pid);
    DIR *dir = opendir(task_dir);
    if (!dir) return 0;

    int restored = 0;
    struct dirent *ent;
    while ((ent = readdir(dir)) != NULL) {
        if (ent->d_name[0] < '0' || ent->d_name[0] > '9') continue;
        pid_t tid = (pid_t)atoi(ent->d_name);
        if (tid > 0 && apply_tid_mask(tid, topo->all_mask, topo->total_cpus)) {
            restored++;
        }
    }
    closedir(dir);
    return restored;
}
