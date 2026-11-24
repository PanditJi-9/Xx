#include "MaxLegendary.h"
#include "MemoryGuard.h"
#include "SysfsDirect.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

void ml_options_default(LegendarySessionOptions *opts) {
    if (!opts) return;
    opts->enable_thread_pinning = true;
    opts->enable_cgroup_freezer = true;
    opts->enable_network_qos = true;
    opts->mute_background_network = false;
    opts->enable_input_boost = true;
    opts->enable_shader_prewarm = true;
    opts->prewarm_min_ram_mb = 1536;
    opts->prewarm_budget_mb = 192;
}

void ml_init(MaxLegendaryContext *ctx) {
    if (!ctx) return;
    memset(ctx, 0, sizeof(*ctx));

    atlas_load_or_repair(&ctx->hw_map);
    ta_detect_topology(&ctx->cpu_topo);
    cf_init(&ctx->freezer);
    nqos_init(&ctx->net_qos);
    ib_init(&ctx->input_boost, ctx->hw_map.touch_gamemode_node);
    kpe_open(&ctx->proc_mon);

    ctx->initialized = true;
}

void ml_on_game_start(MaxLegendaryContext *ctx,
                      const char *package_name,
                      pid_t game_pid,
                      uid_t game_uid,
                      const uid_t *whitelist_uids,
                      size_t whitelist_count,
                      const LegendarySessionOptions *opts) {
    if (!ctx || !ctx->initialized || !opts) return;

    ctx->active_game_pid = game_pid;
    ctx->active_game_uid = game_uid;

    if (opts->enable_shader_prewarm && package_name) {
        PrewarmReport rep;
        sp_prewarm_package(package_name,
                           opts->prewarm_min_ram_mb,
                           opts->prewarm_budget_mb,
                           ctx->hw_map.block_readahead_node,
                           &rep);
    }

    if (opts->enable_thread_pinning && game_pid > 1) {
        ctx->pinned_threads = ta_pin_game_critical_threads(game_pid, &ctx->cpu_topo);
    }

    if (opts->enable_cgroup_freezer && game_uid >= 10000) {
        ctx->frozen_apps = cf_freeze_background_uids(&ctx->freezer,
                                                     game_uid,
                                                     whitelist_uids,
                                                     whitelist_count);
    }

    if (opts->enable_network_qos && game_uid >= 10000) {
        nqos_enable_for_game(&ctx->net_qos, game_uid, opts->mute_background_network);
    }

    if (opts->enable_input_boost) {
        ib_enable(&ctx->input_boost);
    }
}

void ml_on_game_stop(MaxLegendaryContext *ctx) {
    if (!ctx || !ctx->initialized) return;

    ib_disable(&ctx->input_boost);
    nqos_disable(&ctx->net_qos);
    cf_unfreeze_all(&ctx->freezer);

    if (ctx->active_game_pid > 1) {
        ta_restore_pid_affinity(ctx->active_game_pid, &ctx->cpu_topo);
    }

    ctx->active_game_pid = 0;
    ctx->active_game_uid = 0;
    ctx->pinned_threads = 0;
    ctx->frozen_apps = 0;
}

void ml_shutdown(MaxLegendaryContext *ctx) {
    if (!ctx) return;
    ml_on_game_stop(ctx);
    kpe_close(&ctx->proc_mon);
    ctx->initialized = false;
}

bool ml_format_hud_json(const MaxLegendaryContext *ctx, char *out_buf, size_t buf_size) {
    if (!ctx || !out_buf || buf_size < 128) return false;

    int volt_uv = 0;
    int curr_ua = 0;
    int soc = 0;
    int temp_dc = 0;
    size_t ram_mb = 0;

    if (ctx->hw_map.battery_voltage[0]) sysfs_read_int(ctx->hw_map.battery_voltage, &volt_uv);
    if (ctx->hw_map.battery_current[0]) sysfs_read_int(ctx->hw_map.battery_current, &curr_ua);
    if (ctx->hw_map.battery_capacity[0]) sysfs_read_int(ctx->hw_map.battery_capacity, &soc);
    if (ctx->hw_map.battery_temp[0]) {
        sysfs_read_int(ctx->hw_map.battery_temp, &temp_dc);
        if (temp_dc > 1000) temp_dc /= 100;
    }
    mg_get_available_ram_mb(&ram_mb);

    /* حساب القدرة بالملي واط لتفادي أخطاء الفاصلة العائمة في C */
    long long v = (volt_uv > 100000) ? (volt_uv / 1000) : volt_uv;   /* mV */
    long long c = llabs((long long)curr_ua);
    if (c > 10000) c /= 1000;                                        /* mA */
    long long power_mw = (v * c) / 1000;

    int n = snprintf(
        out_buf, buf_size,
        "{\"power_mw\":%lld,\"battery_soc\":%d,\"battery_temp_dc\":%d,"
        "\"ram_avail_mb\":%zu,\"pinned_threads\":%d,\"frozen_apps\":%d,"
        "\"qos_active\":%s,\"input_boost\":%s,\"netlink_zero_idle\":%s}",
        power_mw, soc, temp_dc, ram_mb,
        ctx->pinned_threads, ctx->frozen_apps,
        ctx->net_qos.active ? "true" : "false",
        ctx->input_boost.active ? "true" : "false",
        ctx->proc_mon.supported ? "true" : "false");

    return (n > 0 && (size_t)n < buf_size);
}
