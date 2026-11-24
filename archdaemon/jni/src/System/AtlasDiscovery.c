#include "AtlasDiscovery.h"
#include "SysfsDirect.h"

#include <stdio.h>
#include <string.h>

static void pick_first_existing(const char **candidates, char *dst, size_t dst_size) {
    dst[0] = '\0';
    for (int i = 0; candidates[i] != NULL; ++i) {
        if (sysfs_path_exists(candidates[i])) {
            snprintf(dst, dst_size, "%s", candidates[i]);
            return;
        }
    }
}

static bool extract_json_val(const char *json, const char *key, char *out, size_t out_size) {
    char pat[64];
    snprintf(pat, sizeof(pat), "\"%s\"", key);
    const char *p = strstr(json, pat);
    if (!p) return false;
    p = strchr(p + strlen(pat), ':');
    if (!p) return false;
    p = strchr(p, '"');
    if (!p) return false;
    p++;
    const char *e = strchr(p, '"');
    if (!e) return false;
    size_t len = (size_t)(e - p);
    if (len >= out_size) len = out_size - 1;
    memcpy(out, p, len);
    out[len] = '\0';
    return true;
}

bool atlas_discover_and_save(AtlasHardwareMap *m) {
    if (!m) return false;
    memset(m, 0, sizeof(*m));

    static const char *kCap[] = {
        "/sys/class/power_supply/battery/capacity",
        "/sys/class/power_supply/bms/capacity",
        NULL
    };
    static const char *kTemp[] = {
        "/sys/class/power_supply/battery/temp",
        "/sys/class/power_supply/bms/temp",
        NULL
    };
    static const char *kVolt[] = {
        "/sys/class/power_supply/battery/voltage_now",
        "/sys/class/power_supply/bms/voltage_now",
        NULL
    };
    static const char *kCurr[] = {
        "/sys/class/power_supply/battery/current_now",
        "/sys/class/power_supply/bms/current_now",
        NULL
    };
    static const char *kBypass[] = {
        "/sys/class/power_supply/battery/input_suspend",
        "/sys/class/power_supply/battery/charging_enabled",
        "/sys/class/power_supply/battery/mmi_charging_enable",
        "/sys/class/qcom-battery/input_suspend",
        "/sys/devices/platform/charger/bypass_charger",
        "/proc/mtk_battery_cmd/current_cmd",
        NULL
    };
    static const char *kTouch[] = {
        "/sys/class/touch/touch_dev/game_mode",
        "/sys/devices/virtual/touch/touch_dev/bump_sample_rate",
        "/proc/touchpanel/game_switch_enable",
        "/sys/module/xiaomi_touch/parameters/touch_boost",
        NULL
    };
    static const char *kReadAhead[] = {
        "/sys/block/sda/queue/read_ahead_kb",
        "/sys/block/sde/queue/read_ahead_kb",
        "/sys/block/mmcblk0/queue/read_ahead_kb",
        NULL
    };
    static const char *kSched[] = {
        "/sys/block/sda/queue/scheduler",
        "/sys/block/sde/queue/scheduler",
        "/sys/block/mmcblk0/queue/scheduler",
        NULL
    };
    static const char *kCgroup[] = {
        "/sys/fs/cgroup/cgroup.controllers",
        "/acct/cgroup.controllers",
        NULL
    };

    pick_first_existing(kCap, m->battery_capacity, sizeof(m->battery_capacity));
    pick_first_existing(kTemp, m->battery_temp, sizeof(m->battery_temp));
    pick_first_existing(kVolt, m->battery_voltage, sizeof(m->battery_voltage));
    pick_first_existing(kCurr, m->battery_current, sizeof(m->battery_current));
    pick_first_existing(kBypass, m->bypass_charge_node, sizeof(m->bypass_charge_node));
    pick_first_existing(kTouch, m->touch_gamemode_node, sizeof(m->touch_gamemode_node));
    pick_first_existing(kReadAhead, m->block_readahead_node, sizeof(m->block_readahead_node));
    pick_first_existing(kSched, m->block_scheduler_node, sizeof(m->block_scheduler_node));
    pick_first_existing(kCgroup, m->cgroup_root, sizeof(m->cgroup_root));

    FILE *fp = fopen(ATLAS_MAP_PATH, "w");
    if (fp) {
        fprintf(fp,
                "{\n"
                "  \"battery_capacity\": \"%s\",\n"
                "  \"battery_temp\": \"%s\",\n"
                "  \"battery_voltage\": \"%s\",\n"
                "  \"battery_current\": \"%s\",\n"
                "  \"bypass_charge_node\": \"%s\",\n"
                "  \"touch_gamemode_node\": \"%s\",\n"
                "  \"block_readahead_node\": \"%s\",\n"
                "  \"block_scheduler_node\": \"%s\",\n"
                "  \"cgroup_root\": \"%s\"\n"
                "}\n",
                m->battery_capacity, m->battery_temp, m->battery_voltage,
                m->battery_current, m->bypass_charge_node, m->touch_gamemode_node,
                m->block_readahead_node, m->block_scheduler_node, m->cgroup_root);
        fclose(fp);
    }
    return true;
}

bool atlas_load_or_repair(AtlasHardwareMap *m) {
    if (!m) return false;

    FILE *fp = fopen(ATLAS_MAP_PATH, "r");
    if (!fp) {
        return atlas_discover_and_save(m);
    }

    char buf[2048];
    size_t n = fread(buf, 1, sizeof(buf) - 1, fp);
    fclose(fp);
    buf[n] = '\0';

    memset(m, 0, sizeof(*m));
    extract_json_val(buf, "battery_capacity", m->battery_capacity, sizeof(m->battery_capacity));
    extract_json_val(buf, "battery_temp", m->battery_temp, sizeof(m->battery_temp));
    extract_json_val(buf, "battery_voltage", m->battery_voltage, sizeof(m->battery_voltage));
    extract_json_val(buf, "battery_current", m->battery_current, sizeof(m->battery_current));
    extract_json_val(buf, "bypass_charge_node", m->bypass_charge_node, sizeof(m->bypass_charge_node));
    extract_json_val(buf, "touch_gamemode_node", m->touch_gamemode_node, sizeof(m->touch_gamemode_node));
    extract_json_val(buf, "block_readahead_node", m->block_readahead_node, sizeof(m->block_readahead_node));
    extract_json_val(buf, "block_scheduler_node", m->block_scheduler_node, sizeof(m->block_scheduler_node));
    extract_json_val(buf, "cgroup_root", m->cgroup_root, sizeof(m->cgroup_root));

    /* التحقق من صلاحية المسارات وإعادة الاكتشاف التلقائي إذا تغيرت بعد تحديث الروم */
    bool broken = false;
    if (m->battery_capacity[0] && !sysfs_path_exists(m->battery_capacity)) broken = true;
    if (m->bypass_charge_node[0] && !sysfs_path_exists(m->bypass_charge_node)) broken = true;
    if (m->touch_gamemode_node[0] && !sysfs_path_exists(m->touch_gamemode_node)) broken = true;
    if (m->block_readahead_node[0] && !sysfs_path_exists(m->block_readahead_node)) broken = true;

    if (broken || m->battery_capacity[0] == '\0') {
        return atlas_discover_and_save(m);
    }
    return true;
}
