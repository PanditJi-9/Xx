#ifndef MAXMANAGER_SYSFS_DIRECT_H
#define MAXMANAGER_SYSFS_DIRECT_H

#include <stdbool.h>
#include <stddef.h>

bool sysfs_read_str(const char *path, char *buf, size_t buf_size);
bool sysfs_read_int(const char *path, int *out_val);
bool sysfs_write_str(const char *path, const char *value);
bool sysfs_write_int(const char *path, int value);
bool sysfs_path_exists(const char *path);

#endif /* MAXMANAGER_SYSFS_DIRECT_H */
