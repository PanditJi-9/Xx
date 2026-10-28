/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
// وحدة **للتصريف فقط**: تُعلن شكل واجهة Xposed التي يوفّرها LSPosed وقت التشغيل، كي يُصرَّف خطّاف IMEI
// بلا تبعية شبكية. تُضاف للتطبيق بـ`compileOnly` فلا تُحزَّم في الـAPK (لو حُزّمت لتعارضت مع أصناف الإطار).
// التواقيع مطابقة لواجهة Xposed API 82 (الأوصاف الثنائية تهمّ هنا لا الأجسام).
plugins {
    `java-library`
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
