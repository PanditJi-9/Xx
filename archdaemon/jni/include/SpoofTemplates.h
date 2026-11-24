#ifndef MAXMANAGER_SPOOF_TEMPLATES_H
#define MAXMANAGER_SPOOF_TEMPLATES_H

#include <stdbool.h>
#include <stddef.h>

typedef struct {
    char template_id[32];
    char package_name[128];
    char model[64];
    char manufacturer[64];
    char brand[64];
    char device[64];
    int target_fps;
} SpoofTemplate;

/* استرجاع قالب تزييف افتراضي معتمد للعبة (مثل 120FPS لـ PUBG/CODM/Genshin) */
bool st_get_builtin_template(const char *package_name, SpoofTemplate *out_tpl);

/* كتابة ملف إعدادات محصور بالتطبيق فقط دون تغيير هوية النظام الكلية */
bool st_write_scoped_override(const SpoofTemplate *tpl, const char *dir_path);

/* حذف التجاوز المحصور بالتطبيق */
bool st_clear_scoped_override(const char *package_name, const char *dir_path);

#endif /* MAXMANAGER_SPOOF_TEMPLATES_H */
