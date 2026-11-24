#ifndef MAXMANAGER_THREAD_AFFINITY_H
#define MAXMANAGER_THREAD_AFFINITY_H

#include <stdbool.h>
#include <stdint.h>
#include <sys/types.h>

typedef struct {
    int total_cpus;
    uint64_t little_mask;
    uint64_t big_medium_mask;
    uint64_t all_mask;
} CpuTopology;

/* اكتشاف الأنوية الكبرى والمتوسطة والصغيرة تلقائياً عبر cpufreq */
bool ta_detect_topology(CpuTopology *out_topo);

/* تثبيت الخيوط الحرجة (UnityMain/GameThread/RenderThread/RHIThread...) على الأنوية الكبرى */
int ta_pin_game_critical_threads(pid_t game_pid, const CpuTopology *topo);

/* حصر عملية خلفية على الأنوية الصغيرة فقط */
bool ta_pin_pid_to_little_cores(pid_t bg_pid, const CpuTopology *topo);

/* إعادة جميع خيوط العملية لكامل الأنوية عند الخروج */
int ta_restore_pid_affinity(pid_t pid, const CpuTopology *topo);

#endif /* MAXMANAGER_THREAD_AFFINITY_H */
