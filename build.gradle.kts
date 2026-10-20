// Max Control — مركز تحكّم المالك. مشروع Gradle مستقلّ عن `manager/` (تطبيق المستخدمين).
//
// **التوحيد على محرّك `manager/`** (`MAX-CONTROL-01` §٣): كان هذا المشروع على AGP 8.5.2 /
// Kotlin 2.0.20 / compileSdk 34، وهو محرّك **آخر** لا يوجد في أي بيئة بناء عندنا اليوم،
// فيستحيل التحقّق من ترجمته. و`manager/gradle/libs.versions.toml` هو المصدر المعتمد للإصدارات،
// وهذه هي نسخه نفسها ⇒ بناء واحد يفهم المشروعين، ولا يُصان إصداران في مكانين (ADR-18).
plugins {
    id("com.android.application") version "9.2.0" apply false
    // وبلا `org.jetbrains.kotlin.android`: AGP 9 يحمل دعم Kotlin داخله (كما في `manager/`).
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.10" apply false
}
