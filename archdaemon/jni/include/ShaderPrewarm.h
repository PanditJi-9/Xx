#ifndef MAXMANAGER_SHADER_PREWARM_H
#define MAXMANAGER_SHADER_PREWARM_H

#include <stdbool.h>
#include <stddef.h>

typedef struct {
    size_t files_warmed;
    size_t bytes_advised;
    bool skipped_low_ram;
} PrewarmReport;

/* تدفئة ملفات .pak و .obb وذاكرة Vulkan/GL Shader Cache بحدود آمنة للذاكرة */
bool sp_prewarm_package(const char *package_name,
                        size_t min_avail_ram_mb,
                        size_t max_budget_mb,
                        const char *block_readahead_node,
                        PrewarmReport *out_report);

#endif /* MAXMANAGER_SHADER_PREWARM_H */
