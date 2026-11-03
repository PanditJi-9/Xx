/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

#ifndef MAXMANAGER_SEPOLICY_GEN_H
#define MAXMANAGER_SEPOLICY_GEN_H

#include <stdbool.h>
#include <stdio.h>

/*
 * مولّد sepolicy داخل الخادم: `sys.maxmanager-service --gen-sepolicy` — يقرأ رفض SELinux من الهاتف نفسه
 * ويكتب قواعد `allow` جاهزة. فلا بايثون ولا حاسوب: من يبني الروم يشغّل أمرًا واحدًا على هاتف التجربة (جذر).
 * وهو نفس ما يفعله `make_sepolicy.py` على الحاسوب، بنفس صيغة الخرج، ويدمج مع ملفّ سابق للتكرار.
 */

#define SEPOL_MAX_RULES 1024
#define SEPOL_MAX_PERMS 32
#define SEPOL_NAME_MAX 64

typedef struct {
    char source[SEPOL_NAME_MAX];
    char target[SEPOL_NAME_MAX];
    char tclass[SEPOL_NAME_MAX];
    char perms[SEPOL_MAX_PERMS][SEPOL_NAME_MAX];
    int perm_count;
} SepolRule;

typedef struct {
    SepolRule* rules; /* calloc(SEPOL_MAX_RULES) */
    int count;
    int denials_seen;
    int enforcing_seen; /* سطور permissive=0: كان الخادم محجوبًا فعلًا، فالتكرار لازم */
} SepolRules;

bool sepol_init(SepolRules* set);
void sepol_free(SepolRules* set);
bool sepol_add(SepolRules* set, const char* source, const char* target, const char* tclass, const char* perm);
bool sepol_parse_avc_line(const char* line, const char* domain, SepolRules* set);
int sepol_collect(FILE* in, const char* domain, SepolRules* set);
int sepol_load_existing(FILE* in, SepolRules* set);
void sepol_render(const SepolRules* set, FILE* out);
/*
 * فحص الجهاز (بلا سجلّات ولا بناء ولا فلاش): يقرأ نوع SELinux الحقيقيّ لكل عقدة تسمح بها قناة الخادم (قوائم
 * IpcServer) وللملفّات التي يحتاجها الخادم بنفسه، فيكتب `allow maxmanager <النوع> …`. وهو يعمل على الروم
 * الحاليّ للمطوّر مهما كانت سياسته — فالأنواع تُقرأ ولا تُخمَّن، وتُصاغ القواعد بأنواع هذا الروم نفسه.
 * `sepol_con_hook` و`sepol_fs_root` للاختبارات فقط (نوع وهميّ وجذر مؤقّت).
 */
typedef int (*SepolConHook)(const char* logical_path, char* type, size_t size);
extern SepolConHook sepol_con_hook;
extern const char* sepol_fs_root;
int sepol_scan_device(SepolRules* set, const char* domain);
int handle_gen_sepolicy(int argc, char** argv);

#endif /* MAXMANAGER_SEPOLICY_GEN_H */
