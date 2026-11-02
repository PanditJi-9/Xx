/*
 * Copyright (C) 2024-2025 Rem01Gaming x Zexshia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <MaxManager.h>

#include <limits.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

/**
 * هوية الوحدة كما هي مشحونة فعلًا في `mainfiles/module.prop`.
 *
 * **العطب الذي وُلد من هذين السطرين، مقيسًا على جهاز:** كان الفحص ينفّذ
 * `grep -q '^name=Max Manager$'` — والاسم في `module.prop` صار `MaxManager` بلا فراغ
 * (تغيير مقصود في تاريخ المستودع)، وبقي الفحص يطلب الاسم القديم. فصار الحارس يفشل على
 * **كل تنصيب نظيف** لا على عبث طرف ثالث:
 *
 * ```
 * --- START OF MAXMANAGER SERVICE ---
 * 2026-09-28 20:59:16.060 F MaxManager: EVENT=MODULE_INTEGRITY_FAILED reason=modified_by_third_party
 * ```
 *
 * ثم `exit(EXIT_FAILURE)`، فتُصفَّر `persist.sys.maxmanager.service` ويُكتب
 * `persist.sys.maxmanager.state=stopped`، ولا يعمل بعد ذلك شيء من الخادم: لا ملف عام،
 * ولا حاكم لكل تطبيق، ولا `--checkbypasschg` — وهذه هي رسالة المستخدم نفسها:
 * `ERROR: MaxManager daemon is not running.`
 *
 * **ولماذا ثابتان لا نصّان في نداء:** لأن عطبًا كهذا لا يُمنع بتبديل نصّين، بل بمصدر واحد
 * يُقاس من الطرفين. بوابة `module_identity` في `tools/bundle_contract.py` تقرأ هذين
 * الثابتين وتقرأ `mainfiles/module.prop` وتُخرج بخطأ عند أي انزياح — فتغيير الاسم في
 * `module.prop` يُسقط CI، ولا يُقتل خادم المستخدم صامتًا مرّة أخرى.
 */
#define MODULE_IDENTITY_NAME   "MaxManager"
#define MODULE_IDENTITY_AUTHOR "MaxManager Project"

/** سقف نصّ `module.prop` في الذاكرة — الملفّ الحقيقي يقارب ٢٥٠ بايتًا، والسقف يُستوعبه عشرة أضعاف. */
#define MODULE_PROP_MAX_LEN 2048

/**
 * @brief صفّ `key=value` مطابق تمامًا: لا بادئة، ولا مسافة حول العلامة، و`\r` من CRLF مقبول.
 * @note النصّ الزائد يُرفض: `name=MaxManagerX` ليس `name=MaxManager`، و`name =MaxManager` كذلك.
 */
static bool prop_line_equals(const char* line, const char* key, const char* expected) {
    const size_t key_len = strlen(key);

    if (strncmp(line, key, key_len) != 0 || line[key_len] != '=') {
        return false;
    }

    const char* value = line + key_len + 1;
    const size_t value_len = strcspn(value, "\r\n");

    return value_len == strlen(expected) && strncmp(value, expected, value_len) == 0;
}

/**
 * @brief هل نصّ `module.prop` يحمل هوية الوحدة؟
 * @note دالّة **نقيّة** عمدًا: تُقاس على المضيف بمدخلات مكتوبة، بلا ملفّ وبلا صدفة.
 * @note الاسم **والمؤلف** كلاهما مطلوب: مفتاح واحد صحيح لا يجعل الوحدة وحدة.
 */
bool module_identity_ok(const char* prop_text) {
    if (prop_text == NULL) {
        return false;
    }

    bool name_seen = false;
    bool author_seen = false;
    char line[MODULE_PROP_MAX_LEN];

    const char* cursor = prop_text;
    while (*cursor != '\0') {
        const char* end = strpbrk(cursor, "\r\n");
        const size_t length = end != NULL ? (size_t)(end - cursor) : strlen(cursor);

        /* سطر أطول من كل module.prop حقيقي ⇒ النصّ ليس هو: يُرفض ولا يُقتطع. */
        if (length >= sizeof(line)) {
            return false;
        }

        memcpy(line, cursor, length);
        line[length] = '\0';

        if (prop_line_equals(line, "name", MODULE_IDENTITY_NAME)) {
            name_seen = true;
        }
        if (prop_line_equals(line, "author", MODULE_IDENTITY_AUTHOR)) {
            author_seen = true;
        }

        if (end == NULL) {
            break;
        }
        cursor = end + 1;
        while (*cursor == '\r' || *cursor == '\n') {
            cursor++;
        }
    }

    return name_seen && author_seen;
}

/**
 * @brief هل هذا الخادم **مدمج في الروم** لا مُشغَّل من موديول Magisk/KernelSU؟
 *
 * **العطب المقيس (جهاز POCO X6 Pro، ٢٠٢٦-١١-٠١):** في الروم لا يوجد `/data/adb/modules/MaxManager`،
 * فكان `module_identity_violated()` يرى `module.prop` غير مقروء ⇒ «موت مقصود» ⇒ `exit(EXIT_FAILURE)`
 * في أول `is_kanged()` — فلا يعمل الخادم حتى لو شُغّل بنجاح.
 *
 * والقياس **مسار الثنائيّ نفسه** (`/proc/self/exe`) لا وجود مجلّد: مجلّد الموديول قد يغيب لحظةً في
 * تحديث، أمّا ثنائيّ يعمل من `/system/bin` أو `/system_ext/bin` أو `/product/bin` أو `/vendor/bin`
 * فهو روم حتمًا. وثنائيّ الموديول يقع تحت `/data/adb/modules/` فلا يُلتبس به، فيبقى حارس العبث
 * (`modified_by_third_party`) على الموديول كما كان بلا تخفيف.
 */
bool is_rom_install(void) {
    static const char* const ROM_BIN_DIRS[] = {"/system/bin/", "/system_ext/bin/", "/product/bin/", "/vendor/bin/"};

    char exe[PATH_MAX] = {0};
    const ssize_t length = readlink("/proc/self/exe", exe, sizeof(exe) - 1);
    if (length <= 0) {
        return false;
    }
    exe[length] = '\0';

    for (size_t index = 0; index < sizeof(ROM_BIN_DIRS) / sizeof(ROM_BIN_DIRS[0]); index++) {
        if (strncmp(exe, ROM_BIN_DIRS[index], strlen(ROM_BIN_DIRS[index])) == 0) {
            return true;
        }
    }
    return false;
}

/**
 * @brief قرار الحارس: هل يجب أن يموت الخادم الآن؟
 * @return `true` حين يكون الملفّ غير مقروء **أو** الهوية مخالفة؛ و`false` على وحدة سليمة.
 * @note الملفّ غير المقروء يبقى موتًا مقصودًا (لا هوية تُتحقَّق) — نفس سلوك النسخة القديمة.
 * @note يُقاس على المضيف عبر `fopen` الموجَّه (`host_route_open`)، فلا يحتاج جهازًا ولا صدفة.
 */
bool module_identity_violated(void) {
    /* روم مدمج: لا موديول ولا `module.prop` ليُحكم على هويّته — لا مخالفة تُقاس. */
    if (is_rom_install()) {
        return false;
    }

    char prop[MODULE_PROP_MAX_LEN] = {0};

    FILE* handle = fopen(MODULE_PROP, "r");
    if (handle == NULL) {
        return true;
    }

    const size_t read_bytes = fread(prop, 1, sizeof(prop) - 1, handle);
    fclose(handle);
    prop[read_bytes] = '\0';

    return !module_identity_ok(prop);
}

/**
 * @brief Checks if the module properties have been renamed or modified by a 3rd party.
 */
void is_kanged(void) {
    if (!module_identity_violated()) {
        return;
    }

    log_zenith(LOG_FATAL, "EVENT=MODULE_INTEGRITY_FAILED reason=modified_by_third_party");
    notify("Daemon Error", "Trying to rename me?", true, 0);
    __system_property_set("persist.sys.maxmanager.service", "");
    __system_property_set("persist.sys.maxmanager.state", "stopped");
    exit(EXIT_FAILURE);
}

/**
 * @brief Compares the version inside module.prop with the daemon version.
 */
void check_module_version(void) {
    /* روم مدمج: لا `module.prop` فيه سطر `version=` ليُقارن (الثنائيّ والتطبيق يُشحنان معًا). */
    if (is_rom_install()) {
        return;
    }

    char DAEMON_VERSION[MAX_LINE] = {0};

    snprintf(DAEMON_VERSION, sizeof(DAEMON_VERSION), "%s", MODULE_VERSION);

    int ret = systemv("grep -q '^version=%s$' %s", DAEMON_VERSION, MODULE_PROP);

    if (ret != 0) [[clang::unlikely]] {
        log_zenith(LOG_FATAL, "EVENT=MODULE_INTEGRITY_FAILED reason=version_mismatch expected=%s", DAEMON_VERSION);
        notify("Daemon Error", "MaxManager version mismatch, please reinstall!", true, 0);
        __system_property_set("persist.sys.maxmanager.service", "");
        __system_property_set("persist.sys.maxmanager.state", "stopped");
        exit(EXIT_FAILURE);
    }
}
