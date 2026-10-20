/*
 * Copyright (C) 2024-2025 Rem01Gaming
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

/**
 * @brief Upper bound for one log file, in bytes, from the user's setting.
 *
 * MAX_LOG_FILE_BYTES stays the compiled default and the fallback: a missing property, an
 * unparsable value or one outside the floor/ceil range all return the default rather than an
 * unchecked number. That bound is the point of this function -- a mistyped property
 * (`logmaxkb=999999999`) must not turn into "never rotate", because a log that never rotates
 * eventually fills the partition the module itself lives on.
 *
 * @return Bytes before rotate_log_if_needed() renames the file, always between
 *         LOG_MAX_KB_FLOOR*1024 and LOG_MAX_KB_CEIL*1024.
 */
static long max_log_file_bytes(void) {
    char val[PROP_VALUE_MAX] = {0};
    if (__system_property_get(LOG_MAX_KB_PROP, val) <= 0)
        return MAX_LOG_FILE_BYTES;

    long kb = strtol(val, NULL, 10);
    if (kb < LOG_MAX_KB_FLOOR || kb > LOG_MAX_KB_CEIL)
        return MAX_LOG_FILE_BYTES;

    return kb * 1024L;
}

/**
 * @brief Writes formatted content to the specified file with optional appending and flock
 * protection.
 * @note Avoid using flock on /sdcard due to Android FUSE limitations.
 * @param filename Path to the target file.
 * @param append Set to true for append mode, false for overwrite (truncate) mode.
 * @param use_flock Set to true to acquire an exclusive lock during the write operation.
 * @param data Format string for content, followed by variable arguments.
 * @return 0 if the content was written successfully, -1 on any error or truncation.
 */
int write2file(const char* filename, const bool append, const bool use_flock, const char* data, ...) {
    if (!data)
        return -1;

    char content[MAX_DATA_LENGTH];
    va_list args;
    va_start(args, data);
    int len = vsnprintf(content, sizeof(content), data, args);
    va_end(args);

    if (len <= 0)
        return -1;

    if (len >= (int)sizeof(content))
        return -1;

    int flags = O_WRONLY | O_CREAT;
    flags |= append ? O_APPEND : O_TRUNC;
    int fd = open(filename, flags, 0644);
    if (fd == -1)
        return -1;

    if (use_flock && flock(fd, LOCK_EX) == -1) {
        close(fd);
        return -1;
    }

    ssize_t written = write(fd, content, len);

    if (use_flock)
        flock(fd, LOCK_UN);
    close(fd);

    return (written == len) ? 0 : -1;
}

/**
 * @brief Checks whether the specified file is empty or not.
 * @param filename Path to the target file.
 * @return 1 if the file is empty, 0 if it contains data, or -1 if the file cannot be opened.
 */
int is_file_empty(const char* filename) {
    FILE* file = fopen(filename, "rb");
    if (!file) {
        perror("fopen failed");
        return -1;
    }

    int ch = fgetc(file);
    if (ch == EOF) {
        if (feof(file)) {
            fclose(file);
            return 1;
        } else {
            perror("fgetc failed");
            fclose(file);
            return -1;
        }
    }

    fclose(file);
    return 0;
}

/**
 * @brief Keeps a log file bounded in size by rotating it through LOG_KEEP_FILES older
 *        generations: "<filename>" -> "<filename>.1" -> ... -> "<filename>.<LOG_KEEP_FILES>",
 *        dropping the oldest. Runs once the file reaches the configured cap
 *        (max_log_file_bytes()). Meant to be called by the logging entry points right before
 *        they append a new line, so the cap holds even across days of uninterrupted uptime --
 *        not just at service.sh startup.
 * @param filename Path to the log file to check.
 */
void rotate_log_if_needed(const char* filename) {
    struct stat st;
    if (stat(filename, &st) != 0)
        return; // doesn't exist yet -- nothing to rotate

    if (st.st_size < max_log_file_bytes())
        return;

    // Move from the oldest generation down: <f>.(N-1) -> <f>.N, ..., <f> -> <f>.1.
    // rename() replaces its destination, so the first move drops the oldest generation.
    // Best-effort: a failed move costs one generation of history at most; it never blocks
    // or crashes the caller over a housekeeping rotation.
    char src[PATH_MAX];
    char dst[PATH_MAX];
    for (int gen = LOG_KEEP_FILES; gen >= 1; gen--) {
        if (gen == 1)
            snprintf(src, sizeof(src), "%s", filename);
        else
            snprintf(src, sizeof(src), "%s.%d", filename, gen - 1);

        if (snprintf(dst, sizeof(dst), "%s.%d", filename, gen) >= (int)sizeof(dst))
            return;

        rename(src, dst);
    }
}
