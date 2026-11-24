#ifndef MAXMANAGER_ATLAS_DISCOVERY_H
#define MAXMANAGER_ATLAS_DISCOVERY_H

#include <stdbool.h>
#include <stddef.h>

#define ATLAS_MAP_PATH "/data/adb/maxmanager/hardware_map.json"

typedef struct {
    char battery_capacity[160];
    char battery_temp[160];
    char battery_voltage[160];
    char battery_current[160];
    char bypass_charge_node[160];
    char touch_gamemode_node[160];
    char block_readahead_node[160];
    char block_scheduler_node[160];
    char cgroup_root[128];
} AtlasHardwareMap;

/* يفحص جميع عقد العتاد ويحفظ خريطة موثّقة بصيغة JSON */
bool atlas_discover_and_save(AtlasHardwareMap *out_map);

/* يحمّل الخريطة المحفوظة، وإذا اختفى أي مسار بعد تحديث الروم يعيد اكتشافه تلقائياً */
bool atlas_load_or_repair(AtlasHardwareMap *out_map);

#endif /* MAXMANAGER_ATLAS_DISCOVERY_H */
