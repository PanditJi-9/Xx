/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.design

/**
 * Navigation artwork, not telemetry: its topology never changes with a reading.
 *
 * العائلة الحالية «زجاج ثنائي اللون» (إعادة تصميم ٢٠٢٦-١١-١٧ بأمر المالك — الرموز السابقة
 * كانت سيّئة جدًّا، وأولها الهاتف): جسم زجاجي بحدّ، قلب صلب بلون التمييز، تفاصيل مثقوبة
 * فوق الصلب، وتفاصيل ثانوية خافتة. حدود بصرية ثابتة (0.06..0.94 في المربع الواحد)،
 * وخط ~6% من ضلع الرمز. أشكالها: هاتف حديث، معالج بأرجل، بطاقة رسوميات بمروحتين، رام،
 * قرص، شاشة، بطارية برق، ميزان حرارة، واي فاي، سماعة، يد تحكّم، شبكة تطبيقات، ترس،
 * درع بعلامة صح، منزلقات، مفتاح ربط، كاميرا، حسّاسات، شبكة الذكاء العصبية (لا نجمة — بوّابة
 * `VisualIdentityTest`)، جزيرة، نافذة فوق نافذة، شريط فيلم، ساعة، مستند بطية، رادار، أعمدة،
 * بطارية نبض، لمس بحلقات موجة، هلال نوم، سحابة نسخ احتياطي، مفتاح.
 */
enum class MaxIllustrationKind {
    Device, Cpu, Gpu, Memory, Storage, Display, Power, Thermal, Network, Audio,
    Games, Apps, System, Security, Controls, Tools, Camera, Sensors, Intelligence,
    Island, Overlay, Clips, Sessions, Manifest, Scan, Usage, BatteryHistory,
    Touch, Sleep, Backup, Key,
}

/**
 * Accent  — تدرّج اللون الأساسي (جسم الرمز الصلب).
 * Outline — حدّ الجسم (مائل قليلًا إلى لون الأساس).
 * Wash    — تعبئة زجاجية شفّافة بتدرّج.
 * Soft    — لون أساسي بنصف شفافية للتفاصيل الثانوية (أرجل، علامات).
 * Cutout  — «ثقب» بلون سطح البطاقة يُرسم فوق الأجسام الصلبة (نوافذ، مراوح، علامات).
 */
enum class MaxIllustrationInk { Accent, Outline, Wash, Soft, Cutout }

data class MaxIllustrationPoint(val x: Float, val y: Float)

/** Unit-square geometry lets the same original drawing serve a small mark and a large header. */
sealed interface MaxIllustrationPart {
    val ink: MaxIllustrationInk

    data class Line(
        val points: List<MaxIllustrationPoint>,
        override val ink: MaxIllustrationInk = MaxIllustrationInk.Accent,
        val closed: Boolean = false,
        /** Filled silhouettes carry the domain; [closed] must be true for them to read cleanly. */
        val filled: Boolean = false,
        /** Multiplier on the family stroke width — thick round-capped strokes draw teeth, blades, handles. */
        val weight: Float = 1f,
    ) : MaxIllustrationPart

    data class Box(
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val radius: Float = 0.04f,
        override val ink: MaxIllustrationInk = MaxIllustrationInk.Accent,
        val filled: Boolean = false,
    ) : MaxIllustrationPart

    data class Ring(
        val x: Float,
        val y: Float,
        val radius: Float,
        override val ink: MaxIllustrationInk = MaxIllustrationInk.Accent,
        val filled: Boolean = false,
    ) : MaxIllustrationPart
}

/** Unknown icons keep their existing ImageVector; names are never derived from translated titles. */
fun maxIllustrationKind(iconName: String?): MaxIllustrationKind? = when (iconName?.substringAfterLast('.')) {
    "PhoneAndroid", "PhoneIphone", "Smartphone", "Home" -> MaxIllustrationKind.Device
    "Memory", "DeveloperBoard" -> MaxIllustrationKind.Cpu
    "Speed", "VideogameAsset" -> MaxIllustrationKind.Gpu
    "Storage", "Save", "DataUsage", "Folder", "FolderOpen" -> MaxIllustrationKind.Storage
    "Backup", "SettingsBackupRestore" -> MaxIllustrationKind.Backup
    "SdStorage" -> MaxIllustrationKind.Memory
    "DisplaySettings", "AspectRatio", "Monitor", "ScreenRotation" -> MaxIllustrationKind.Display
    "BatteryChargingFull", "BatteryFull", "BatteryStd", "Power", "Cable" -> MaxIllustrationKind.Power
    "Bedtime" -> MaxIllustrationKind.Sleep
    "Thermostat", "DeviceThermostat", "Whatshot" -> MaxIllustrationKind.Thermal
    "NetworkCheck", "Wifi", "Router" -> MaxIllustrationKind.Network
    "GraphicEq", "VolumeUp", "Headphones", "Audiotrack", "MusicNote" -> MaxIllustrationKind.Audio
    "Gamepad", "SportsEsports", "Games" -> MaxIllustrationKind.Games
    "Apps", "AppSettingsAlt", "Android" -> MaxIllustrationKind.Apps
    "Settings", "SettingsApplications", "Code", "Terminal" -> MaxIllustrationKind.System
    "Shield", "Security", "Lock", "VerifiedUser" -> MaxIllustrationKind.Security
    "Key" -> MaxIllustrationKind.Key
    "Tune" -> MaxIllustrationKind.Controls
    "TouchApp" -> MaxIllustrationKind.Touch
    "SmartButton" -> MaxIllustrationKind.Island
    "PictureInPicture" -> MaxIllustrationKind.Overlay
    "VideoLibrary" -> MaxIllustrationKind.Clips
    "History" -> MaxIllustrationKind.Sessions
    "Policy", "FactCheck" -> MaxIllustrationKind.Manifest
    "Radar", "TravelExplore", "NetworkPing" -> MaxIllustrationKind.Scan
    "QueryStats", "BarChart", "InsertChart" -> MaxIllustrationKind.Usage
    "BatterySaver", "BatteryAlert", "BatteryCharging90" -> MaxIllustrationKind.BatteryHistory
    "Build", "Science", "CleaningServices", "BugReport" -> MaxIllustrationKind.Tools
    "Camera", "CameraAlt", "PhotoCamera" -> MaxIllustrationKind.Camera
    "Sensors", "SensorsOff", "Explore" -> MaxIllustrationKind.Sensors
    // ولا تُسجَّل هنا أسماء رموز الذكاء: بوّابة `VisualIdentityTest` تمنع «النجمة» خارج أسطح
    // Max AI، فمجالُ الذكاء يأتي من خريطة الوجهات وحدها (`MaxDestinationIllustrations`).
    "Insights", "Timeline" -> MaxIllustrationKind.Intelligence
    else -> null
}

/**
 * No platform calls, randomness, clocks or unbounded detail in the artwork.
 *
 * عائلة «زجاج ثنائي اللون» (إعادة تصميم ٢٠٢٦-١١-١٧ بأمر المالك: الرموز السابقة سيّئة جدًّا):
 * جسم زجاجي شفّاف بحدّ واضح + قلب صلب بلون التمييز + تفاصيل «مثقوبة» (Cutout) فوق الصلب
 * + تفاصيل ثانوية خافتة (Soft). الخط أثقل، والنهايات دائرية، والأشكال أكبر وأكثر وضوحًا.
 * لا تَراكُب بين تعبئتين شفّافتين (يظهر كدرز)، ولا نجمة (بوّابة `VisualIdentityTest`).
 */
fun maxIllustrationParts(kind: MaxIllustrationKind): List<MaxIllustrationPart> = buildList {
    fun line(
        vararg xy: Float,
        ink: MaxIllustrationInk = MaxIllustrationInk.Accent,
        closed: Boolean = false,
        filled: Boolean = false,
        weight: Float = 1f,
    ) {
        add(
            MaxIllustrationPart.Line(
                points = xy.toList().chunked(2).map { MaxIllustrationPoint(it[0], it[1]) },
                ink = ink,
                closed = closed,
                filled = filled,
                weight = weight,
            ),
        )
    }
    fun box(
        x: Float, y: Float, w: Float, h: Float,
        ink: MaxIllustrationInk = MaxIllustrationInk.Accent,
        filled: Boolean = false, radius: Float = 0.04f,
    ) {
        add(MaxIllustrationPart.Box(x, y, w, h, radius, ink, filled))
    }
    fun ring(
        x: Float, y: Float, radius: Float,
        ink: MaxIllustrationInk = MaxIllustrationInk.Accent, filled: Boolean = false,
    ) {
        add(MaxIllustrationPart.Ring(x, y, radius, ink, filled))
    }
    val outline = MaxIllustrationInk.Outline
    val wash = MaxIllustrationInk.Wash
    val soft = MaxIllustrationInk.Soft
    val cut = MaxIllustrationInk.Cutout

    when (kind) {
        // هاتف حديث: جسم زجاجي، شاشة مضاءة، كاميرا ثقب، ودجتات، شريط إيماءات، أزرار جانبية.
        MaxIllustrationKind.Device -> {
            box(0.25f, 0.06f, 0.5f, 0.88f, wash, filled = true, radius = 0.13f)
            box(0.25f, 0.06f, 0.5f, 0.88f, outline, radius = 0.13f)
            box(0.31f, 0.12f, 0.38f, 0.76f, filled = true, radius = 0.08f)
            ring(0.5f, 0.19f, 0.02f, cut, filled = true)
            box(0.36f, 0.27f, 0.28f, 0.17f, cut, filled = true, radius = 0.04f)
            box(0.36f, 0.48f, 0.13f, 0.13f, cut, filled = true, radius = 0.035f)
            box(0.51f, 0.48f, 0.13f, 0.13f, cut, filled = true, radius = 0.035f)
            line(0.42f, 0.79f, 0.58f, 0.79f, ink = cut, weight = 0.9f)
            line(0.795f, 0.26f, 0.795f, 0.38f, ink = outline, weight = 1.5f)
            line(0.795f, 0.44f, 0.795f, 0.52f, ink = outline, weight = 1.5f)
        }
        // معالج: جسم بـ١٢ رجلًا، نواة صلبة، وبصمة مثقوبة في المنتصف.
        MaxIllustrationKind.Cpu -> {
            for (p in floatArrayOf(0.32f, 0.5f, 0.68f)) {
                line(p, 0.09f, p, 0.2f, ink = soft, weight = 1.3f)
                line(p, 0.8f, p, 0.91f, ink = soft, weight = 1.3f)
                line(0.09f, p, 0.2f, p, ink = soft, weight = 1.3f)
                line(0.8f, p, 0.91f, p, ink = soft, weight = 1.3f)
            }
            box(0.2f, 0.2f, 0.6f, 0.6f, wash, filled = true, radius = 0.12f)
            box(0.2f, 0.2f, 0.6f, 0.6f, outline, radius = 0.12f)
            box(0.32f, 0.32f, 0.36f, 0.36f, filled = true, radius = 0.08f)
            box(0.43f, 0.43f, 0.14f, 0.14f, cut, filled = true, radius = 0.04f)
        }
        // بطاقة رسوميات: مروحتان بريشات مثقوبة وموصل سفلي.
        MaxIllustrationKind.Gpu -> {
            box(0.06f, 0.22f, 0.88f, 0.52f, wash, filled = true, radius = 0.1f)
            box(0.06f, 0.22f, 0.88f, 0.52f, outline, radius = 0.1f)
            for (cx in floatArrayOf(0.32f, 0.68f)) {
                ring(cx, 0.48f, 0.15f, filled = true)
                line(cx, 0.48f, cx, 0.375f, ink = cut, weight = 0.9f)
                line(cx, 0.48f, cx + 0.0909f, 0.5325f, ink = cut, weight = 0.9f)
                line(cx, 0.48f, cx - 0.0909f, 0.5325f, ink = cut, weight = 0.9f)
                ring(cx, 0.48f, 0.04f, cut, filled = true)
            }
            box(0.14f, 0.78f, 0.56f, 0.06f, soft, filled = true, radius = 0.02f)
        }
        // شريحة رام: أربع رقائق صلبة وثمانية أرجل.
        MaxIllustrationKind.Memory -> {
            box(0.06f, 0.26f, 0.88f, 0.44f, wash, filled = true, radius = 0.07f)
            box(0.06f, 0.26f, 0.88f, 0.44f, outline, radius = 0.07f)
            for (i in 0..3) box(0.13f + i * 0.2f, 0.34f, 0.16f, 0.24f, filled = true, radius = 0.03f)
            for (i in 0..7) line(0.16f + i * 0.1f, 0.74f, 0.16f + i * 0.1f, 0.88f, ink = soft, weight = 1.2f)
        }
        // قرص: طبق بحلقة مثقوبة، ذراع قراءة، ومؤشر.
        MaxIllustrationKind.Storage -> {
            box(0.08f, 0.16f, 0.84f, 0.68f, wash, filled = true, radius = 0.12f)
            box(0.08f, 0.16f, 0.84f, 0.68f, outline, radius = 0.12f)
            ring(0.4f, 0.5f, 0.22f, filled = true)
            ring(0.4f, 0.5f, 0.12f, cut)
            ring(0.4f, 0.5f, 0.04f, cut, filled = true)
            line(0.8f, 0.28f, 0.6f, 0.48f, ink = outline, weight = 1.5f)
            ring(0.8f, 0.28f, 0.04f, filled = true)
            ring(0.82f, 0.72f, 0.025f, soft, filled = true)
        }
        // شاشة: لوح مضاء بشمس وجبال مثقوبة وحامل.
        MaxIllustrationKind.Display -> {
            box(0.06f, 0.14f, 0.88f, 0.58f, wash, filled = true, radius = 0.08f)
            box(0.06f, 0.14f, 0.88f, 0.58f, outline, radius = 0.08f)
            box(0.12f, 0.2f, 0.76f, 0.46f, filled = true, radius = 0.04f)
            ring(0.3f, 0.36f, 0.05f, cut, filled = true)
            line(0.16f, 0.6f, 0.36f, 0.44f, 0.52f, 0.54f, 0.7f, 0.38f, 0.84f, 0.52f, ink = cut, weight = 0.9f)
            line(0.5f, 0.72f, 0.5f, 0.84f, ink = outline, weight = 1.4f)
            box(0.32f, 0.84f, 0.36f, 0.07f, outline, filled = true, radius = 0.03f)
        }
        // بطارية: مستوى شحن صلب وبرق مثقوب.
        MaxIllustrationKind.Power -> {
            box(0.06f, 0.26f, 0.78f, 0.48f, wash, filled = true, radius = 0.11f)
            box(0.06f, 0.26f, 0.78f, 0.48f, outline, radius = 0.11f)
            box(0.87f, 0.42f, 0.07f, 0.16f, outline, filled = true, radius = 0.03f)
            box(0.12f, 0.32f, 0.5f, 0.36f, filled = true, radius = 0.06f)
            line(0.43f, 0.34f, 0.28f, 0.53f, 0.37f, 0.53f, 0.32f, 0.66f, 0.48f, 0.46f, 0.39f, 0.46f, ink = cut, closed = true, filled = true)
        }
        // ميزان حرارة: قشرة صلبة، تجويف مثقوب، زئبق صلب، وعلامات.
        MaxIllustrationKind.Thermal -> {
            box(0.36f, 0.08f, 0.28f, 0.6f, filled = true, radius = 0.14f)
            ring(0.5f, 0.71f, 0.21f, filled = true)
            box(0.44f, 0.16f, 0.12f, 0.5f, cut, filled = true, radius = 0.06f)
            ring(0.5f, 0.71f, 0.13f, cut, filled = true)
            box(0.47f, 0.36f, 0.06f, 0.34f, filled = true, radius = 0.03f)
            ring(0.5f, 0.71f, 0.075f, filled = true)
            line(0.74f, 0.2f, 0.86f, 0.2f, ink = soft, weight = 1.2f)
            line(0.74f, 0.32f, 0.86f, 0.32f, ink = soft, weight = 1.2f)
            line(0.74f, 0.44f, 0.86f, 0.44f, ink = soft, weight = 1.2f)
        }
        // واي فاي: ثلاث أقواس متدرّجة ونقطة.
        MaxIllustrationKind.Network -> {
            line(0.373f, 0.613f, 0.404f, 0.588f, 0.441f, 0.57f, 0.48f, 0.561f, 0.52f, 0.561f, 0.559f, 0.57f, 0.596f, 0.588f, 0.627f, 0.613f, weight = 1.4f)
            line(0.274f, 0.514f, 0.33f, 0.469f, 0.394f, 0.438f, 0.464f, 0.422f, 0.536f, 0.422f, 0.606f, 0.438f, 0.67f, 0.469f, 0.726f, 0.514f, weight = 1.4f)
            line(0.175f, 0.415f, 0.255f, 0.351f, 0.348f, 0.306f, 0.448f, 0.283f, 0.552f, 0.283f, 0.652f, 0.306f, 0.745f, 0.351f, 0.825f, 0.415f, ink = soft, weight = 1.4f)
            ring(0.5f, 0.74f, 0.065f, filled = true)
        }
        // سماعة: مغناطيس ومخروط صلبان وموجتان.
        MaxIllustrationKind.Audio -> {
            box(0.1f, 0.38f, 0.16f, 0.24f, filled = true, radius = 0.04f)
            line(0.24f, 0.4f, 0.48f, 0.18f, 0.48f, 0.82f, 0.24f, 0.6f, closed = true, filled = true)
            line(0.647f, 0.346f, 0.682f, 0.382f, 0.706f, 0.426f, 0.718f, 0.475f, 0.718f, 0.525f, 0.706f, 0.574f, 0.682f, 0.618f, 0.647f, 0.654f, weight = 1.4f)
            line(0.737f, 0.238f, 0.795f, 0.3f, 0.836f, 0.375f, 0.857f, 0.457f, 0.857f, 0.543f, 0.836f, 0.625f, 0.795f, 0.7f, 0.737f, 0.762f, ink = soft, weight = 1.4f)
        }
        // يد تحكّم: عصا اتجاه، أزرار، وكتفان.
        MaxIllustrationKind.Games -> {
            line(0.2f, 0.2f, 0.36f, 0.2f, ink = outline, weight = 1.5f)
            line(0.64f, 0.2f, 0.8f, 0.2f, ink = outline, weight = 1.5f)
            box(0.06f, 0.3f, 0.88f, 0.46f, wash, filled = true, radius = 0.23f)
            box(0.06f, 0.3f, 0.88f, 0.46f, outline, radius = 0.23f)
            box(0.245f, 0.42f, 0.07f, 0.22f, filled = true, radius = 0.02f)
            box(0.17f, 0.495f, 0.22f, 0.07f, filled = true, radius = 0.02f)
            ring(0.72f, 0.44f, 0.045f, filled = true)
            ring(0.72f, 0.62f, 0.045f, soft, filled = true)
            ring(0.63f, 0.53f, 0.045f, soft, filled = true)
            ring(0.81f, 0.53f, 0.045f, soft, filled = true)
        }
        // شبكة تطبيقات: رقعة شطرنج صلب/زجاج.
        MaxIllustrationKind.Apps -> {
            box(0.12f, 0.12f, 0.34f, 0.34f, filled = true, radius = 0.1f)
            box(0.54f, 0.12f, 0.34f, 0.34f, wash, filled = true, radius = 0.1f)
            box(0.54f, 0.12f, 0.34f, 0.34f, outline, radius = 0.1f)
            box(0.12f, 0.54f, 0.34f, 0.34f, wash, filled = true, radius = 0.1f)
            box(0.12f, 0.54f, 0.34f, 0.34f, outline, radius = 0.1f)
            box(0.54f, 0.54f, 0.34f, 0.34f, filled = true, radius = 0.1f)
        }
        // ترس: ثماني أسنان شبه منحرفة مصمتة حول جسم دائري بثقب مركزي — ترس حقيقي لا «شمس».
        MaxIllustrationKind.System -> {
            line(0.740f, 0.600f, 0.920f, 0.570f, 0.920f, 0.430f, 0.740f, 0.400f, closed = true, filled = true)
            line(0.599f, 0.740f, 0.747f, 0.846f, 0.846f, 0.747f, 0.740f, 0.599f, closed = true, filled = true)
            line(0.400f, 0.740f, 0.430f, 0.920f, 0.570f, 0.920f, 0.600f, 0.740f, closed = true, filled = true)
            line(0.260f, 0.599f, 0.154f, 0.747f, 0.253f, 0.846f, 0.401f, 0.740f, closed = true, filled = true)
            line(0.260f, 0.400f, 0.080f, 0.430f, 0.080f, 0.570f, 0.260f, 0.600f, closed = true, filled = true)
            line(0.401f, 0.260f, 0.253f, 0.154f, 0.154f, 0.253f, 0.260f, 0.401f, closed = true, filled = true)
            line(0.600f, 0.260f, 0.570f, 0.080f, 0.430f, 0.080f, 0.400f, 0.260f, closed = true, filled = true)
            line(0.740f, 0.401f, 0.846f, 0.253f, 0.747f, 0.154f, 0.599f, 0.260f, closed = true, filled = true)
            ring(0.5f, 0.5f, 0.3f, filled = true)
            ring(0.5f, 0.5f, 0.135f, cut, filled = true)
            ring(0.5f, 0.5f, 0.06f, filled = true)
        }
        // درع: جسم زجاجي، درع داخلي صلب، وعلامة صح مثقوبة.
        MaxIllustrationKind.Security -> {
            line(0.5f, 0.07f, 0.84f, 0.19f, 0.84f, 0.5f, 0.74f, 0.72f, 0.5f, 0.93f, 0.26f, 0.72f, 0.16f, 0.5f, 0.16f, 0.19f, ink = wash, closed = true, filled = true)
            line(0.5f, 0.07f, 0.84f, 0.19f, 0.84f, 0.5f, 0.74f, 0.72f, 0.5f, 0.93f, 0.26f, 0.72f, 0.16f, 0.5f, 0.16f, 0.19f, ink = outline, closed = true)
            line(0.5f, 0.208f, 0.731f, 0.289f, 0.731f, 0.5f, 0.663f, 0.65f, 0.5f, 0.792f, 0.337f, 0.65f, 0.269f, 0.5f, 0.269f, 0.289f, closed = true, filled = true)
            line(0.4f, 0.5f, 0.47f, 0.58f, 0.61f, 0.4f, ink = cut, weight = 1.5f)
        }
        // منزلقات: مسارات خافتة، جزء نشط، ومقابض بنقطة مثقوبة.
        MaxIllustrationKind.Controls -> {
            val rows = floatArrayOf(0.22f, 0.5f, 0.78f)
            val knobs = floatArrayOf(0.32f, 0.68f, 0.45f)
            for (i in 0..2) {
                line(0.12f, rows[i], 0.88f, rows[i], ink = soft)
                line(0.12f, rows[i], knobs[i], rows[i], weight = 1.4f)
                ring(knobs[i], rows[i], 0.1f, filled = true)
                ring(knobs[i], rows[i], 0.035f, cut, filled = true)
            }
        }
        // مفتاح ربط: رأس بفكّ مثقوب ومقبض سميك.
        MaxIllustrationKind.Tools -> {
            line(0.54f, 0.46f, 0.22f, 0.78f, weight = 2.6f)
            ring(0.2f, 0.8f, 0.09f, filled = true)
            ring(0.66f, 0.34f, 0.22f, filled = true)
            line(0.66f, 0.34f, 0.84f, 0.16f, ink = cut, weight = 2f)
        }
        // كاميرا: نتوء علوي، عدسة بحلقة مثقوبة، وفلاش.
        MaxIllustrationKind.Camera -> {
            box(0.34f, 0.17f, 0.32f, 0.11f, soft, filled = true, radius = 0.04f)
            box(0.06f, 0.28f, 0.88f, 0.56f, wash, filled = true, radius = 0.12f)
            box(0.06f, 0.28f, 0.88f, 0.56f, outline, radius = 0.12f)
            ring(0.5f, 0.56f, 0.2f, filled = true)
            ring(0.5f, 0.56f, 0.12f, cut)
            ring(0.5f, 0.56f, 0.045f, cut, filled = true)
            ring(0.82f, 0.4f, 0.03f, filled = true)
        }
        // حسّاسات: نقطة مصدر وموجات من الجانبين.
        MaxIllustrationKind.Sensors -> {
            ring(0.5f, 0.5f, 0.1f, filled = true)
            line(0.344f, 0.656f, 0.314f, 0.617f, 0.292f, 0.573f, 0.281f, 0.525f, 0.281f, 0.475f, 0.292f, 0.427f, 0.314f, 0.383f, 0.344f, 0.344f, weight = 1.4f)
            line(0.245f, 0.755f, 0.195f, 0.692f, 0.16f, 0.619f, 0.142f, 0.54f, 0.142f, 0.46f, 0.16f, 0.381f, 0.195f, 0.308f, 0.245f, 0.245f, ink = soft, weight = 1.4f)
            line(0.656f, 0.344f, 0.686f, 0.383f, 0.708f, 0.427f, 0.719f, 0.475f, 0.719f, 0.525f, 0.708f, 0.573f, 0.686f, 0.617f, 0.656f, 0.656f, weight = 1.4f)
            line(0.755f, 0.245f, 0.805f, 0.308f, 0.84f, 0.381f, 0.858f, 0.46f, 0.858f, 0.54f, 0.84f, 0.619f, 0.805f, 0.692f, 0.755f, 0.755f, ink = soft, weight = 1.4f)
        }
        // ذكاء: شبكة عصبية — جسم زجاجي، عقدة مركزية صلبة بنواة مثقوبة، خمس عقد طرفية وخيوط وصل (لا نجمة).
        MaxIllustrationKind.Intelligence -> {
            ring(0.5f, 0.5f, 0.43f, wash, filled = true)
            ring(0.5f, 0.5f, 0.43f, outline)
            line(0.5f, 0.5f, 0.5f, 0.215f, ink = soft, weight = 1.2f)
            line(0.5f, 0.5f, 0.771f, 0.412f, ink = soft, weight = 1.2f)
            line(0.5f, 0.5f, 0.668f, 0.731f, ink = soft, weight = 1.2f)
            line(0.5f, 0.5f, 0.332f, 0.731f, ink = soft, weight = 1.2f)
            line(0.5f, 0.5f, 0.229f, 0.412f, ink = soft, weight = 1.2f)
            line(0.5f, 0.215f, 0.771f, 0.412f, 0.668f, 0.731f, 0.332f, 0.731f, 0.229f, 0.412f, ink = soft, closed = true, weight = 0.9f)
            ring(0.5f, 0.215f, 0.065f, filled = true)
            ring(0.771f, 0.412f, 0.065f, filled = true)
            ring(0.668f, 0.731f, 0.065f, filled = true)
            ring(0.332f, 0.731f, 0.065f, filled = true)
            ring(0.229f, 0.412f, 0.065f, filled = true)
            ring(0.5f, 0.5f, 0.13f, filled = true)
            ring(0.5f, 0.5f, 0.05f, cut, filled = true)
        }
        // جزيرة ديناميكية: حبّة زجاجية بكاميرا ومؤشر نشاط.
        MaxIllustrationKind.Island -> {
            box(0.06f, 0.34f, 0.88f, 0.32f, wash, filled = true, radius = 0.16f)
            box(0.06f, 0.34f, 0.88f, 0.32f, outline, radius = 0.16f)
            ring(0.25f, 0.5f, 0.075f, filled = true)
            box(0.5f, 0.45f, 0.3f, 0.1f, filled = true, radius = 0.05f)
        }
        // نافذة فوق نافذة: خلفية بسطور ونافذة أمامية صلبة.
        MaxIllustrationKind.Overlay -> {
            box(0.06f, 0.14f, 0.88f, 0.64f, wash, filled = true, radius = 0.08f)
            box(0.06f, 0.14f, 0.88f, 0.64f, outline, radius = 0.08f)
            line(0.14f, 0.28f, 0.5f, 0.28f, ink = soft, weight = 1.2f)
            line(0.14f, 0.4f, 0.36f, 0.4f, ink = soft, weight = 1.2f)
            box(0.46f, 0.46f, 0.4f, 0.3f, filled = true, radius = 0.06f)
            line(0.54f, 0.56f, 0.78f, 0.56f, ink = cut, weight = 0.9f)
            line(0.54f, 0.66f, 0.7f, 0.66f, ink = cut, weight = 0.9f)
        }
        // شريط فيلم: ثقوب جانبية ومثلث تشغيل.
        MaxIllustrationKind.Clips -> {
            box(0.06f, 0.18f, 0.88f, 0.64f, wash, filled = true, radius = 0.08f)
            box(0.06f, 0.18f, 0.88f, 0.64f, outline, radius = 0.08f)
            for (y in floatArrayOf(0.26f, 0.45f, 0.64f)) {
                box(0.12f, y, 0.08f, 0.1f, soft, filled = true, radius = 0.02f)
                box(0.8f, y, 0.08f, 0.1f, soft, filled = true, radius = 0.02f)
            }
            line(0.42f, 0.34f, 0.42f, 0.66f, 0.68f, 0.5f, closed = true, filled = true)
        }
        // ساعة: وجه زجاجي، علامات، وعقربان.
        MaxIllustrationKind.Sessions -> {
            ring(0.5f, 0.5f, 0.38f, wash, filled = true)
            ring(0.5f, 0.5f, 0.38f, outline)
            line(0.5f, 0.2f, 0.5f, 0.25f, ink = soft, weight = 1.1f)
            line(0.8f, 0.5f, 0.75f, 0.5f, ink = soft, weight = 1.1f)
            line(0.5f, 0.8f, 0.5f, 0.75f, ink = soft, weight = 1.1f)
            line(0.2f, 0.5f, 0.25f, 0.5f, ink = soft, weight = 1.1f)
            line(0.5f, 0.5f, 0.5f, 0.3f, weight = 1.4f)
            line(0.5f, 0.5f, 0.66f, 0.58f, weight = 1.4f)
            ring(0.5f, 0.5f, 0.05f, filled = true)
        }
        // مستند بطية: سطور بأوّل سطر صلب.
        MaxIllustrationKind.Manifest -> {
            line(0.22f, 0.08f, 0.6f, 0.08f, 0.78f, 0.26f, 0.78f, 0.92f, 0.22f, 0.92f, ink = wash, closed = true, filled = true)
            line(0.22f, 0.08f, 0.6f, 0.08f, 0.78f, 0.26f, 0.78f, 0.92f, 0.22f, 0.92f, ink = outline, closed = true)
            line(0.6f, 0.08f, 0.6f, 0.26f, 0.78f, 0.26f, ink = outline)
            line(0.32f, 0.46f, 0.68f, 0.46f, weight = 1.3f)
            line(0.32f, 0.58f, 0.68f, 0.58f, ink = soft, weight = 1.3f)
            line(0.32f, 0.7f, 0.56f, 0.7f, ink = soft, weight = 1.3f)
        }
        // رادار: قرص زجاجي، قطاع مسح، حافة قائدة، ونقاط.
        MaxIllustrationKind.Scan -> {
            ring(0.5f, 0.5f, 0.38f, wash, filled = true)
            ring(0.5f, 0.5f, 0.38f, outline)
            ring(0.5f, 0.5f, 0.2f, soft)
            line(0.5f, 0.5f, 0.5932f, 0.1523f, 0.7065f, 0.2051f, 0.7949f, 0.2935f, 0.8477f, 0.4068f, ink = soft, closed = true, filled = true)
            line(0.5f, 0.5f, 0.8477f, 0.4068f, weight = 1.3f)
            ring(0.5f, 0.5f, 0.045f, filled = true)
            ring(0.7f, 0.3f, 0.035f, filled = true)
            ring(0.34f, 0.64f, 0.035f, soft, filled = true)
        }
        // أعمدة: ثلاثة أعمدة تصاعدية والأعلى صلب.
        MaxIllustrationKind.Usage -> {
            line(0.1f, 0.86f, 0.9f, 0.86f, ink = outline)
            box(0.16f, 0.52f, 0.17f, 0.34f, soft, filled = true, radius = 0.04f)
            box(0.415f, 0.34f, 0.17f, 0.52f, soft, filled = true, radius = 0.04f)
            box(0.67f, 0.14f, 0.17f, 0.72f, filled = true, radius = 0.04f)
        }
        // بطارية بنبض: جسم زجاجي وخط نبض سميك.
        MaxIllustrationKind.BatteryHistory -> {
            box(0.06f, 0.26f, 0.78f, 0.48f, wash, filled = true, radius = 0.11f)
            box(0.06f, 0.26f, 0.78f, 0.48f, outline, radius = 0.11f)
            box(0.87f, 0.42f, 0.07f, 0.16f, outline, filled = true, radius = 0.03f)
            line(0.14f, 0.52f, 0.3f, 0.52f, 0.4f, 0.38f, 0.5f, 0.64f, 0.6f, 0.52f, 0.76f, 0.52f, weight = 1.4f)
        }
        // لمس: شاشة زجاجية عليها نقطة لمس صلبة بحلقتَي موجة — «أين لمستَ» لا «كم انزلق».
        MaxIllustrationKind.Touch -> {
            box(0.2f, 0.1f, 0.6f, 0.8f, wash, filled = true, radius = 0.12f)
            box(0.2f, 0.1f, 0.6f, 0.8f, outline, radius = 0.12f)
            ring(0.5f, 0.46f, 0.25f, soft)
            ring(0.5f, 0.46f, 0.15f, soft)
            ring(0.5f, 0.46f, 0.075f, filled = true)
            line(0.4f, 0.8f, 0.6f, 0.8f, ink = soft, weight = 1.0f)
        }
        // نوم: هلال صلب (قصّ بدائرة مثقوبة) مع حرفَي z — وضع السكون العميق.
        MaxIllustrationKind.Sleep -> {
            ring(0.46f, 0.54f, 0.34f, filled = true)
            ring(0.62f, 0.4f, 0.29f, cut, filled = true)
            line(0.62f, 0.14f, 0.78f, 0.14f, 0.62f, 0.3f, 0.78f, 0.3f, weight = 1.2f)
            line(0.78f, 0.46f, 0.88f, 0.46f, 0.78f, 0.56f, 0.88f, 0.56f, ink = soft, weight = 1.0f)
        }
        // نسخ احتياطي: سحابة مصمتة بسهم صاعد مثقوب.
        MaxIllustrationKind.Backup -> {
            ring(0.33f, 0.59f, 0.17f, filled = true)
            ring(0.52f, 0.45f, 0.23f, filled = true)
            ring(0.71f, 0.6f, 0.16f, filled = true)
            box(0.33f, 0.52f, 0.38f, 0.24f, filled = true, radius = 0.0f)
            line(0.52f, 0.7f, 0.52f, 0.42f, ink = cut, weight = 1.3f)
            line(0.41f, 0.52f, 0.52f, 0.41f, 0.63f, 0.52f, ink = cut, weight = 1.3f)
        }
        // مفتاح: رأس دائري بثقب وساق بسنّين — الترخيص والمفاتيح.
        MaxIllustrationKind.Key -> {
            ring(0.29f, 0.5f, 0.19f, filled = true)
            ring(0.29f, 0.5f, 0.075f, cut, filled = true)
            line(0.45f, 0.5f, 0.88f, 0.5f, weight = 1.7f)
            line(0.72f, 0.5f, 0.72f, 0.68f, weight = 1.5f)
            line(0.84f, 0.5f, 0.84f, 0.63f, weight = 1.5f)
        }
    }
}

/** Invalid readings remain absent; callers must not turn an unreadable value into a zero fill. */
fun maxMeasuredFraction(value: Float?, maximum: Float?): Float? {
    if (value == null || maximum == null || !value.isFinite() || !maximum.isFinite()) return null
    if (value < 0f || maximum <= 0f) return null
    return (value / maximum).coerceIn(0f, 1f)
}

data class MaxDisplayProportion(val width: Float, val height: Float)

/** Fit the declared panel inside a unit square without inventing a notch, bezel or orientation. */
fun maxDisplayProportion(width: Float?, height: Float?): MaxDisplayProportion? {
    if (width == null || height == null || !width.isFinite() || !height.isFinite()) return null
    if (width <= 0f || height <= 0f) return null
    val longest = maxOf(width, height)
    return MaxDisplayProportion(width / longest, height / longest)
}
