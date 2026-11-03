/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

#include <MaxManager.h>
#include <SepolicyGen.h>

#include <ctype.h>
#include <errno.h>
#include <stdlib.h>
#include <string.h>

#define SEPOL_DEFAULT_OUTPUT "/sdcard/Download/maxmanager_generated.te"

bool sepol_init(SepolRules* set) {
    memset(set, 0, sizeof(*set));
    set->rules = calloc(SEPOL_MAX_RULES, sizeof(SepolRule));
    return set->rules != NULL;
}

void sepol_free(SepolRules* set) {
    free(set->rules);
    set->rules = NULL;
    set->count = 0;
}

static bool name_ok(const char* text) {
    if (!text || text[0] == '\0' || strlen(text) >= SEPOL_NAME_MAX) {
        return false;
    }
    for (const char* p = text; *p; p++) {
        if (!(isalnum((unsigned char)*p) || *p == '_' || *p == '-' || *p == '.')) {
            return false;
        }
    }
    return true;
}

/** @brief يضيف إذنًا إلى قاعدة (ويُنشئها عند الحاجة). ترفض الأسماء المشوَّهة فلا يخرج نصّ غريب إلى ملفّ السياسة. */
bool sepol_add(SepolRules* set, const char* source, const char* target, const char* tclass, const char* perm) {
    if (!name_ok(source) || !name_ok(target) || !name_ok(tclass) || !name_ok(perm)) {
        return false;
    }
    SepolRule* rule = NULL;
    for (int index = 0; index < set->count; index++) {
        SepolRule* candidate = &set->rules[index];
        if (strcmp(candidate->source, source) == 0 && strcmp(candidate->target, target) == 0 &&
            strcmp(candidate->tclass, tclass) == 0) {
            rule = candidate;
            break;
        }
    }
    if (!rule) {
        if (set->count >= SEPOL_MAX_RULES) {
            return false;
        }
        rule = &set->rules[set->count++];
        snprintf(rule->source, sizeof(rule->source), "%s", source);
        snprintf(rule->target, sizeof(rule->target), "%s", target);
        snprintf(rule->tclass, sizeof(rule->tclass), "%s", tclass);
    }
    for (int index = 0; index < rule->perm_count; index++) {
        if (strcmp(rule->perms[index], perm) == 0) {
            return true;
        }
    }
    if (rule->perm_count >= SEPOL_MAX_PERMS) {
        return false;
    }
    snprintf(rule->perms[rule->perm_count++], SEPOL_NAME_MAX, "%s", perm);
    return true;
}

static bool token_after(const char* line, const char* key, char* out, size_t size) {
    const char* start = strstr(line, key);
    if (!start) {
        return false;
    }
    start += strlen(key);
    size_t length = 0;
    while (start[length] && start[length] != ':' && !isspace((unsigned char)start[length]) && length + 1 < size) {
        length++;
    }
    memcpy(out, start, length);
    out[length] = '\0';
    return length > 0;
}

/**
 * @brief سطر `avc: denied { perms } ... scontext=u:r:S:… tcontext=u:(object_r|r):T:… tclass=C` → قاعدة.
 * يُقبل السطر فقط إن كان نطاق الخادم مصدرًا أو هدفًا (الهدف: رفض اتصال التطبيق بقناته).
 */
bool sepol_parse_avc_line(const char* line, const char* domain, SepolRules* set) {
    const char* avc = strstr(line, "avc:");
    if (!avc || !strstr(avc, "denied")) {
        return false;
    }
    const char* open = strchr(avc, '{');
    const char* close = open ? strchr(open, '}') : NULL;
    if (!open || !close) {
        return false;
    }
    char source[SEPOL_NAME_MAX];
    char target[SEPOL_NAME_MAX];
    char tclass[SEPOL_NAME_MAX];
    if (!token_after(line, "scontext=u:r:", source, sizeof(source)) ||
        !(token_after(line, "tcontext=u:object_r:", target, sizeof(target)) ||
          token_after(line, "tcontext=u:r:", target, sizeof(target))) ||
        !token_after(line, "tclass=", tclass, sizeof(tclass))) {
        return false;
    }
    if (strcmp(source, domain) != 0 && strcmp(target, domain) != 0) {
        return false;
    }
    set->denials_seen++;
    if (strstr(line, "permissive=0")) {
        set->enforcing_seen++;
    }
    char perms[256];
    const size_t length = (size_t)(close - open - 1);
    if (length >= sizeof(perms)) {
        return false;
    }
    memcpy(perms, open + 1, length);
    perms[length] = '\0';
    char* save = NULL;
    for (char* token = strtok_r(perms, " \t", &save); token; token = strtok_r(NULL, " \t", &save)) {
        sepol_add(set, source, target, tclass, token);
    }
    return true;
}

int sepol_collect(FILE* in, const char* domain, SepolRules* set) {
    char line[2048];
    int accepted = 0;
    while (fgets(line, sizeof(line), in)) {
        accepted += sepol_parse_avc_line(line, domain, set) ? 1 : 0;
    }
    return accepted;
}

/** @brief يقرأ قواعد `allow S T:C { p… };` من ملفّ ناتج سابق ليدمجها (تكرار الجولات). */
int sepol_load_existing(FILE* in, SepolRules* set) {
    char line[1024];
    int loaded = 0;
    while (fgets(line, sizeof(line), in)) {
        char source[SEPOL_NAME_MAX];
        char target[SEPOL_NAME_MAX];
        char tclass[SEPOL_NAME_MAX];
        char perms[512];
        if (sscanf(line, "allow %63s %63[^: ]:%63s { %511[^}]", source, target, tclass, perms) != 4) {
            continue;
        }
        char* save = NULL;
        for (char* token = strtok_r(perms, " \t", &save); token; token = strtok_r(NULL, " \t", &save)) {
            loaded += sepol_add(set, source, target, tclass, token) ? 1 : 0;
        }
    }
    return loaded;
}

static int compare_names(const void* a, const void* b) {
    return strcmp((const char*)a, (const char*)b);
}

static int compare_rules(const void* a, const void* b) {
    const SepolRule* x = a;
    const SepolRule* y = b;
    int order = strcmp(x->source, y->source);
    if (order == 0) {
        order = strcmp(x->target, y->target);
    }
    return order != 0 ? order : strcmp(x->tclass, y->tclass);
}

static bool dangerous(const char* perm) {
    static const char* const LIST[] = {"sys_module", "sys_rawio", "setenforce", "load_policy", "ptrace", "mac_admin", "mac_override"};
    for (size_t index = 0; index < sizeof(LIST) / sizeof(LIST[0]); index++) {
        if (strcmp(perm, LIST[index]) == 0) {
            return true;
        }
    }
    return false;
}

void sepol_render(const SepolRules* set, FILE* out) {
    fputs("# Generated by sys.maxmanager-service --gen-sepolicy from real denials - review before shipping.\n"
          "# If the build rejects a line with a neverallow error, delete that line:\n"
          "# that feature stays unavailable in this ROM; everything else still works.\n\n",
          out);
    qsort(set->rules, (size_t)set->count, sizeof(SepolRule), compare_rules);
    for (int index = 0; index < set->count; index++) {
        SepolRule* rule = &set->rules[index];
        qsort(rule->perms, (size_t)rule->perm_count, SEPOL_NAME_MAX, compare_names);
        bool risky = false;
        fprintf(out, "allow %s %s:%s {", rule->source, rule->target, rule->tclass);
        for (int perm = 0; perm < rule->perm_count; perm++) {
            fprintf(out, " %s", rule->perms[perm]);
            risky = risky || dangerous(rule->perms[perm]);
        }
        fputs(" };", out);
        if (risky) {
            fputs("  # REVIEW: dangerous permission, grant only if you understand it", out);
        }
        fputc('\n', out);
    }
}

static int collect_from_command(const char* command, const char* domain, SepolRules* set) {
    FILE* pipe = popen(command, "r");
    if (!pipe) {
        return 0;
    }
    const int accepted = sepol_collect(pipe, domain, set);
    pclose(pipe);
    return accepted;
}

/**
 * @brief `--gen-sepolicy [LOG…] [-o FILE] [-d DOMAIN]`
 *
 * بلا ملفّات: يقرأ `dmesg` و`logcat -d -b all` من الهاتف. الخرج الافتراضيّ `/sdcard/Download/maxmanager_generated.te`
 * (`-o -` للشاشة)، ويُدمج مع الملفّ إن وُجد فتتراكم الجولات. وأخيرًا يطبع كم قاعدة جديدة.
 */
int handle_gen_sepolicy(int argc, char** argv) {
    const char* domain = "maxmanager";
    const char* output = SEPOL_DEFAULT_OUTPUT;
    const char* files[16];
    int file_count = 0;
    for (int index = 2; index < argc; index++) {
        if (strcmp(argv[index], "-o") == 0 && index + 1 < argc) {
            output = argv[++index];
        } else if (strcmp(argv[index], "-d") == 0 && index + 1 < argc) {
            domain = argv[++index];
        } else if (file_count < 16) {
            files[file_count++] = argv[index];
        }
    }

    SepolRules set;
    if (!sepol_init(&set)) {
        fprintf(stderr, "out of memory\n");
        return 1;
    }
    const bool to_screen = strcmp(output, "-") == 0;
    int before = 0;
    if (!to_screen) {
        FILE* previous = fopen(output, "r");
        if (previous) {
            sepol_load_existing(previous, &set);
            fclose(previous);
            before = set.count;
        }
    }
    int accepted = 0;
    if (file_count == 0) {
        accepted += collect_from_command("dmesg 2>/dev/null", domain, &set);
        accepted += collect_from_command("logcat -d -b all 2>/dev/null", domain, &set);
    }
    for (int index = 0; index < file_count; index++) {
        FILE* file = fopen(files[index], "r");
        if (!file) {
            fprintf(stderr, "cannot read %s: %s\n", files[index], strerror(errno));
            continue;
        }
        accepted += sepol_collect(file, domain, &set);
        fclose(file);
    }
    if (set.count == 0) {
        fprintf(stderr, "No denials for domain '%s' found. Is ro.maxmanager.sepolicy=1 set and the permissive file built in?\n", domain);
        sepol_free(&set);
        return 1;
    }
    FILE* out = to_screen ? stdout : fopen(output, "w");
    if (!out) {
        fprintf(stderr, "cannot write %s: %s\n", output, strerror(errno));
        sepol_free(&set);
        return 1;
    }
    sepol_render(&set, out);
    if (!to_screen) {
        fclose(out);
    }
    fprintf(stderr, "%d denials read -> %d rules (%d before) -> %s\n", accepted, set.count, before, to_screen ? "screen" : output);
    if (set.enforcing_seen > 0) {
        fprintf(stderr, "Note: %d denials were logged while enforcing, so the daemon was blocked part-way: add these rules, rebuild and repeat.\n", set.enforcing_seen);
    }
    if (!to_screen && set.count == before) {
        fprintf(stderr, "No new rules - you are done.\n");
    }
    sepol_free(&set);
    return 0;
}
