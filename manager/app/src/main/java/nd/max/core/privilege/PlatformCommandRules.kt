/*
 * Copyright (C) 2026-2027 MaxManager contributors
 *
 * Licensed under the Apache License, Version 2.0.
 */
package nd.max.core.privilege

/**
 * حدود أوامر المنصّة — **نقيّة فتُقاس على JVM بلا جهاز**.
 *
 * **لماذا حدٌّ أصلًا:** [PlatformShell] هو المسار الذي يجعل ميزات مثل `wm`/`pm`/`appops`/`dumpsys`
 * تعمل عبر Shizuku (مستوى ADB). وShizuku **ليس جذرًا**: لا يكتب عقدة نواة ولا يتجاوز SELinux.
 * فلو مرّ من هنا أمرٌ يمسّ `/sys` أو `/proc` أو يستدعي `su` لَعاد فشلًا غامضًا، أو — الأسوأ —
 * نجح جزئيًّا في وضع الجذر وأوهم أن الوضعين متكافئان. القاعدة تمنع ذلك قبل التنفيذ وتُعلن الرفض.
 *
 * **والقاعدة ليست جدار أمان:** الجدار الحقيقي هو صلاحية العملية نفسها. هذه طبقة صدق: تمنع
 * الاستدعاء الخاطئ أن يُترجم إلى «نجاح» كاذب في وضع Shizuku.
 */
object PlatformCommandRules {

    /** الأوامر التي تُنفَّذ بمستوى ADB على أندرويد — لا تحتاج مالك العتاد. */
    private val ALLOWED_PREFIXES = listOf(
        "pm", "wm", "am", "cmd", "dumpsys", "settings", "service call", "pidof",
        "getprop", "id", "echo", "date", "ls",
    )

    /**
     * مسارات لا يجوز أن تمرّ من طبقة ADB إطلاقًا: كتابة عقد نواة أو إدارة وحدات.
     * والفحص على النصّ كلّه لا على أوّله، لأن `echo 1 > /sys/...` يبدأ بأمر مسموح.
     */
    private val FORBIDDEN_FRAGMENTS = listOf(
        "/sys/", "/proc/", "/data/adb", "/dev/block", " insmod", " rmmod", " modprobe",
        " mount ", " su ", "sudo ", "chmod ", "chown ",
    )

    /** إعادة توجيه مقبولة: كتم الخطأ فقط. وما عداها كتابة ملف — لا تمرّ. */
    private val SAFE_REDIRECTIONS = listOf("2>/dev/null", "2>&1", ">/dev/null")

    /** مرشّح واحد مسموح بعد أنبوب واحد: `grep` قراءة فقط، لا تنفيذ. */
    private val PIPE_FILTER = "grep"

    /**
     * محارف تحكّم الصدفة: تفتح أمرًا ثانيًا بعد بادئة مسموحة (`pm list; reboot`)، أو تنفّذه
     * في الخلفية (`pm list & reboot`)، أو تستبدل نصًّا (`$(id)`).
     *
     * والأنبوب `|` ليس منها: له قاعدة أضيق أدناه — أمر منصّة واحد يُرشَّح بـ`grep` وحده.
     */
    private val SHELL_CONTROL = listOf(";", "&&", "||", "`", "\$(", "\${", "\n", "\r", "<")

    /**
     * هل هذا الأمر من أوامر المنصّة المسموح بها في طبقة ADB؟
     *
     * الشكل المسموح: `<أمر منصّة>` أو `<أمر منصّة> | grep <نمط>`. والأنبوب إلى غير `grep`
     * مرفوض، وكذلك أي سلسلة (`;`/`&&`/`||`/خلفيّة) أو استبدال نصّ أو إعادة توجيه لملف.
     * و`grep` هنا **قراءة فقط**: لا تنفّذ ما يُمرَّر إليها.
     *
     * و[SAFE_REDIRECTIONS] تُحيَّد قبل فحص `>` و`&` حتى لا يُرفض `2>/dev/null` المشروع.
     */
    fun isPlatformCommand(command: String): Boolean {
        val normalized = command.trim()
        if (normalized.isEmpty()) return false
        if (FORBIDDEN_FRAGMENTS.any { normalized.contains(it) }) return false
        if (SHELL_CONTROL.any { normalized.contains(it) }) return false

        var scrubbed = normalized
        SAFE_REDIRECTIONS.forEach { scrubbed = scrubbed.replace(it, "") }
        if (scrubbed.contains(">") || scrubbed.contains("&")) return false

        val segments = scrubbed.split("|")
        if (segments.size > 2) return false
        if (!isPlatformVerb(segments.first().trim())) return false
        if (segments.size == 2 && !isGrepFilter(segments[1].trim())) return false
        return true
    }

    private fun isPlatformVerb(command: String): Boolean {
        if (command.isEmpty()) return false
        return ALLOWED_PREFIXES.any { prefix ->
            command == prefix || command.startsWith("$prefix ")
        }
    }

    private fun isGrepFilter(command: String): Boolean =
        command == PIPE_FILTER || command.startsWith("$PIPE_FILTER ")
}
