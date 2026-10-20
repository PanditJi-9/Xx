/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 *
 * In-memory property store behind the host shim. Order of insertion is
 * preserved so a test can assert exactly which properties a daemon pass sees.
 */
#include <string.h>

#include "sys/system_properties.h"

#define HOST_PROP_MAX 512

struct prop_info {
    const char* name;
    const char* value;
    uint32_t serial;
};

static struct prop_info store[HOST_PROP_MAX];
static char names[HOST_PROP_MAX][128];
static char values[HOST_PROP_MAX][PROP_VALUE_MAX];
static int store_count;
static uint32_t next_serial = 1;

void host_prop_reset(void) {
    store_count = 0;
    next_serial = 1;
    memset(names, 0, sizeof(names));
    memset(values, 0, sizeof(values));
}

void host_prop_set(const char* name, const char* value) {
    if (!name)
        return;
    for (int i = 0; i < store_count; i++) {
        if (strcmp(store[i].name, name) == 0) {
            strncpy(values[i], value ? value : "", PROP_VALUE_MAX - 1);
            store[i].value = values[i];
            store[i].serial = next_serial++;
            return;
        }
    }
    if (store_count >= HOST_PROP_MAX)
        return;
    int i = store_count++;
    strncpy(names[i], name, sizeof(names[i]) - 1);
    strncpy(values[i], value ? value : "", PROP_VALUE_MAX - 1);
    store[i].name = names[i];
    store[i].value = values[i];
    store[i].serial = next_serial++;
}

int host_prop_count(void) {
    return store_count;
}

int __system_property_get(const char* name, char* value) {
    if (!name || !value)
        return 0;
    value[0] = '\0';
    for (int i = 0; i < store_count; i++) {
        if (strcmp(store[i].name, name) == 0) {
            strncpy(value, store[i].value, PROP_VALUE_MAX - 1);
            value[PROP_VALUE_MAX - 1] = '\0';
            return (int)strlen(value);
        }
    }
    return 0;
}

int __system_property_set(const char* name, const char* value) {
    host_prop_set(name, value);
    return 0;
}

void __system_property_read_callback(const prop_info* pi, prop_read_callback callback, void* cookie) {
    if (!pi || !callback)
        return;
    /*
     * ليس كل `prop_info` يمرّ هنا من مخزننا: bionic نفسه ينادي هذا الرمز حين يحلّ
     * `localtime()` منطقته الزمنية (`__bionic_get_system_tz` يقرأ `persist.sys.timezone`)،
     * وكائنُه هناك ليس `struct prop_info` المضيف — فقراءة `pi->name` كانت تُسقط المجموعة
     * على أندرويد (Termux) بينما تمرّ على glibc في CI لأن `localtime` هناك لا يسأل الخاصية.
     * فنحصر الفكّ في عناصر مخزننا، ونعطي الغريب اسماً فارغاً: لا يطابق أي مفتاح، فيسقط
     * bionic إلى منطقه الافتراضية بدل قراءة ذاكرة غريبة.
     */
    uintptr_t addr = (uintptr_t)pi;
    uintptr_t base = (uintptr_t)store;
    bool ours = addr >= base && addr < base + sizeof(store) &&
                (addr - base) % sizeof(store[0]) == 0;
    if (!ours) {
        callback(cookie, "", "", 0);
        return;
    }
    callback(cookie, pi->name, pi->value, pi->serial);
}

int __system_property_foreach(void (*callback)(const prop_info* pi, void* cookie), void* cookie) {
    if (!callback)
        return -1;
    for (int i = 0; i < store_count; i++)
        callback(&store[i], cookie);
    return 0;
}
