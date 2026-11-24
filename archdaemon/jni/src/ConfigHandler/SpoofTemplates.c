#include "SpoofTemplates.h"

#include <stdio.h>
#include <string.h>
#include <unistd.h>

static const SpoofTemplate kBuiltins[] = {
    {"fps120_rog", "com.tencent.ig",             "ASUS_AI2401", "asus",    "asus",    "AI2401", 120},
    {"fps120_rog", "com.pubg.imobile",           "ASUS_AI2401", "asus",    "asus",    "AI2401", 120},
    {"fps120_rog", "com.pubg.krmobile",          "ASUS_AI2401", "asus",    "asus",    "AI2401", 120},
    {"fps120_s24", "com.activision.callofduty.shooter", "SM-S928B", "samsung", "samsung", "e3q", 120},
    {"fps120_op",  "com.miHoYo.GenshinImpact",   "CPH2581",     "OnePlus", "OnePlus", "OP594DL1", 120}
};

bool st_get_builtin_template(const char *package_name, SpoofTemplate *out_tpl) {
    if (!package_name || !out_tpl) return false;
    size_t count = sizeof(kBuiltins) / sizeof(kBuiltins[0]);
    for (size_t i = 0; i < count; ++i) {
        if (strcmp(kBuiltins[i].package_name, package_name) == 0) {
            *out_tpl = kBuiltins[i];
            return true;
        }
    }
    return false;
}

bool st_write_scoped_override(const SpoofTemplate *tpl, const char *dir_path) {
    if (!tpl || !tpl->package_name[0]) return false;
    const char *base = (dir_path && dir_path[0]) ? dir_path : "/data/adb/maxmanager/spoof";

    char path[256];
    snprintf(path, sizeof(path), "%s/%s.json", base, tpl->package_name);

    FILE *fp = fopen(path, "w");
    if (!fp) return false;

    fprintf(fp,
            "{\"template\":\"%s\",\"package\":\"%s\",\"model\":\"%s\","
            "\"manufacturer\":\"%s\",\"brand\":\"%s\",\"device\":\"%s\","
            "\"target_fps\":%d,\"scope\":\"per_app_only\"}\n",
            tpl->template_id, tpl->package_name, tpl->model,
            tpl->manufacturer, tpl->brand, tpl->device, tpl->target_fps);
    fclose(fp);
    return true;
}

bool st_clear_scoped_override(const char *package_name, const char *dir_path) {
    if (!package_name || !package_name[0]) return false;
    const char *base = (dir_path && dir_path[0]) ? dir_path : "/data/adb/maxmanager/spoof";
    char path[256];
    snprintf(path, sizeof(path), "%s/%s.json", base, package_name);
    return (unlink(path) == 0);
}
