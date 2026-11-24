#include "SysfsDirect.h"

#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

bool sysfs_path_exists(const char *path) {
    return (path != NULL && access(path, F_OK) == 0);
}

bool sysfs_read_str(const char *path, char *buf, size_t buf_size) {
    if (!path || !buf || buf_size < 2) return false;
    int fd;
    do { fd = open(path, O_RDONLY | O_CLOEXEC); } while (fd < 0 && errno == EINTR);
    if (fd < 0) return false;
    ssize_t n;
    do { n = read(fd, buf, buf_size - 1); } while (n < 0 && errno == EINTR);
    close(fd);
    if (n < 0) return false;
    buf[n] = '\0';
    while (n > 0 && (buf[n - 1] == '\n' || buf[n - 1] == '\r' || buf[n - 1] == ' ')) {
        buf[--n] = '\0';
    }
    return true;
}

bool sysfs_read_int(const char *path, int *out_val) {
    if (!out_val) return false;
    char buf[64];
    if (!sysfs_read_str(path, buf, sizeof(buf))) return false;
    char *endptr = NULL;
    errno = 0;
    long v = strtol(buf, &endptr, 10);
    if (errno != 0 || endptr == buf) return false;
    *out_val = (int)v;
    return true;
}

bool sysfs_write_str(const char *path, const char *value) {
    if (!path || !value) return false;
    int fd;
    do { fd = open(path, O_WRONLY | O_CLOEXEC); } while (fd < 0 && errno == EINTR);
    if (fd < 0) return false;
    size_t len = strlen(value);
    size_t written = 0;
    while (written < len) {
        ssize_t n = write(fd, value + written, len - written);
        if (n < 0) {
            if (errno == EINTR) continue;
            close(fd);
            return false;
        }
        written += (size_t)n;
    }
    close(fd);
    return true;
}

bool sysfs_write_int(const char *path, int value) {
    char buf[32];
    int len = snprintf(buf, sizeof(buf), "%d", value);
    if (len <= 0 || (size_t)len >= sizeof(buf)) return false;
    return sysfs_write_str(path, buf);
}
