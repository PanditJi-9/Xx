# Copyright (C) 2026 Nader Magdy. All rights reserved.
# Proprietary and confidential — not licensed for use, copying, or distribution
# without prior written permission from the copyright holder.
# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# ─────────────────────────────────────────────────────────────────────────
# حماية الملكية الفكريّة — تصليب R8 (ADR-69 · 2026-10-11)
# ─────────────────────────────────────────────────────────────────────────
#
# **الحدّ المُعلَن أولًا، لأنه يمنع كل توهّم لاحق:** R8 يُعيد التسمية ولا يُعمّي المنطق.
# بنية الشروط والحلقات تبقى مقروءة لمن يفكّ الـDEX، وأسماء السلاسل تبقى صريحة. فما يلي
# **يرفع كلفة الفهم** (يُخفي المعمارية وأسماء الوحدات ويسمح بمزيد من الدمج والحذف)،
# **ولا يجعل الكود غير قابل للقراءة**. ومن قال غير ذلك فقد وعد بما لا يعطيه R8.
#
# ومرجع القرار: تقرير `cybersecurity.asee.io` (2026-02-25) «Why R8 Obfuscation Is No
# Longer Enough for Android App Protection» — وخلاصته أنّ تسمية الرموز وحدها لا تكفي،
# ولهذا بُنيت هذه الطبقة كلّها (`docs/ai/DECISIONS.md` ADR-69) لا هذا الملفّ وحده.

# يسمح بتضييق صلاحيات الأصناف والدوال بعد الدمج ⇒ دمج وحذف أوسع، ومخرَج أصغر وأغمض.
#
# **وأقوى ما يعطيه R8 في هذا الباب يأتي بعده:** تُنقل كل الأصناف المُعاد تسميتها إلى حزمة واحدة،
# فتُمحى المعمارية التي يقرؤها أي محلّل من الأسماء وحدها: `core/spoof` · `core/maxai` ·
# `core/atlas` … كانت خريطة طريق جاهزة لمن يريد استهداف أسرع شاشة أو أهمّ محرّك.
#
# **وحدّها المُعلَن:** تُعيد تسمية الأصناف **غير المحفوظة** فقط، فلا تمسّ ما يُسمّى في
# البيان (الأنشطة والخدمات والمستقبِلات ومزوّد Shizuku) ولا ما تُثبّته قواعد `-keep` أدناه.
# وقد قُوبلت مواضع الانعكاس بالاسم في الشجرة قبل تشغيلها: كلّها على أصناف **النظام**
# (`android.os.ServiceManager` · `android.app.IActivityManager` · `android.media.audiofx.*`
# · ووصفات Stub في حزم شركات أخرى)، ولا موضع واحد يسأل عن صنف من أصنافنا بالاسم.
-repackageclasses 'o'
-overloadaggressively

# خمس مرّات تحسين بدل الافتراضي: مزيد من الدمج والإزالة ⇒ مخرَج أصغر وأغمض.
-optimizationpasses 5

# يُخفي اسم ملفّ المصدر الأصلي من جداول الاستثناءات، فيبقى `LineNumberTable` وحده —
# وبه يُزال التعتيم عند الحاجة بـ`mapping.txt`، بينما لا يعرف من يفكّ الحزمة أسماء ملفّاتنا
# ومعها أسماء الشاشات والمحرّكات. **ولماذا أُبقي `LineNumberTable`:** بلاغات الأعطال
# الحقيقية تُقرأ بها، وإزالتها تعمية عن المالك لا عن المهاجم.
-renamesourcefileattribute SourceFile

# ── قواعد الإبقاء (Keep) ──
#
# كل قاعدة هنا **مُبرَّرة بموضع يستدعي الاسم نصًّا**، لا بالاحتياط: قاعدة إبقاء بلا سبب
# تُبقي إدخالًا في الحزمة وتحرس صنفًا لا يحتاج حراسة.

# `app_process` يشغّل هذا الصنف باسمه الكامل (`nd.max.AppMonitor`) — فالتسمية عقد خارجي
# لا يملكه R8.
-keep class nd.max.AppMonitor {
    public static void main(java.lang.String[]);
}

# `hiddenapibypass` يُستدعى بالانعكاس على أسماء أعضائه الداخلية (وهو بديل `Unsafe` المحجوز).
-keep class org.lsposed.hiddenapibypass.** { *; }

# عقد الربط مع العملية الجذرية: الواجهة المُولَّدة من AIDL تحمل ثابت الوصف
# (`nd.max.core.ipc.IRootNodeService`) داخل معاملة binder، واسم الواجهة يُقرأ منه في
# `IRootNodeService.Stub.asInterface`. وإعادة تسمية الواجهة **دون** الثابت تكسر الربط،
# والثابت نصّ لا يُعاد كتابته (لا `-adaptclassstrings` في هذا الملفّ عن قصد: هي تُعيد
# كتابة كل سلسلة تشبه اسم صنف، وفي التطبيق عشرات الوصفات لنظام وشركات أخرى).
-keep interface nd.max.core.ipc.IRootNodeService { *; }
-keep class nd.max.core.ipc.IRootNodeService$Stub { *; }

# حارس سلامة الحزمة (`core/security/**`): تُحفظ دواله الأصلية في JNI، ويُعاد تسمية الباقي.
# ── قواعد إبقاء دوال ومكتبات JNI الأصلية ──
# الحفاظ على أسماء الدوال الأصلية والأصناف الحاضنة لها حتى لا تنكسر روابط JNI مع C++/Rust
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class nd.max.core.jni.** { *; }
-keep class nd.max.core.security.SecurityBridge { *; }
-keep class com.android.apksig.** { *; }


# خدمة المستخدم التي يُنشئها خادم Shizuku **بالاسم** في عملية يملكها (`AR-20`): المُنشئ بلا
# وسائط لا يستدعيه كودنا، فيراه R8 غير مستعمل فيحذفه أو يدمج الصنف — والخدمة عندئذ تفشل عند
# الربط بلا أن يسقط شيء في الترجمة. تُبقى أيضًا واجهة العقد و`Stub` لثبات الوصف بين الطرفين
# (نفس سبب `IRootNodeService` أعلاه: الثابت نصٌّ لا يُعاد كتابته دون `-adaptclassstrings`).
-keep class nd.max.core.privilege.ShizukuShellService { *; }
-keep interface nd.max.core.privilege.IShizukuShell { *; }
-keep class nd.max.core.privilege.IShizukuShell$Stub { *; }
