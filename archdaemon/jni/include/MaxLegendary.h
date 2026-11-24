#ifndef MAXMANAGER_LEGENDARY_FACADE_H
#define MAXMANAGER_LEGENDARY_FACADE_H

#include "AtlasDiscovery.h"
#include "CgroupFreezer.h"
#include "InputBoost.h"
#include "KernelProcEvents.h"
#include "NetworkQoS.h"
#include "ShaderPrewarm.h"
#include "SpoofTemplates.h"
#include "StealthIpc.h"
#include "ThreadAffinity.h"

#include <stdbool.h>
#include <stddef.h>
#include <sys/types.h>

typedef struct {
    bool enable_thread_pinning;
    bool enable_cgroup_freezer;
    bool enable_network_qos;
    bool mute_background_network;
    bool enable_input_boost;
    bool enable_shader_prewarm;
    size_t prewarm_min_ram_mb;
    size_t prewarm_budget_mb;
} LegendarySessionOptions;

typedef struct {
    AtlasHardwareMap hw_map;
    CpuTopology cpu_topo;
    CgroupFreezerState freezer;
    NetworkQoSState net_qos;
    InputBoostState input_boost;
    KernelProcMonitor proc_mon;
    pid_t active_game_pid;
    uid_t active_game_uid;
    int pinned_threads;
    int frozen_apps;
    bool initialized;
} MaxLegendaryContext;

void ml_options_default(LegendarySessionOptions *opts);
void ml_init(MaxLegendaryContext *ctx);
void ml_on_game_start(MaxLegendaryContext *ctx,
                      const char *package_name,
                      pid_t game_pid,
                      uid_t game_uid,
                      const uid_t *whitelist_uids,
                      size_t whitelist_count,
                      const LegendarySessionOptions *opts);
void ml_on_game_stop(MaxLegendaryContext *ctx);
void ml_shutdown(MaxLegendaryContext *ctx);

/* توليد JSON حي يتضمن استهلاك الطاقة بالواط وحالة التجميد والتثبيت */
bool ml_format_hud_json(const MaxLegendaryContext *ctx, char *out_buf, size_t buf_size);

#endif /* MAXMANAGER_LEGENDARY_FACADE_H */
