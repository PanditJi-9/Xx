/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.design

/**
 * Navigation artwork, not telemetry: its topology never changes with a reading.
 *
 * العائلة الحالية (إعادة تصميم مقيسة ٢٠٢٦-١١-١٧ بعد ورقة معاينة `tools/design/icon_preview.cjs`
 * وأمر المالك «اعد تمصيم الايقونات…»): **صورة مملوءة لا سلكيّة** — جسم رمز واحد بحدود
 * بصرية ثابتة (0.08..0.92 في المربع الواحد)، وتفصيل Accent واحد صلب يسمّي المجال،
 * وخط ~5.5% من ضلع الرمز (كان 1.5dp ثابتًا ⇒ 3.75% عند 40dp أي أنحف ~2.2× من
 * شبكة Material 2/24). كل الأشكال متساوية الوزن البصري: هاتف بشاشة مضاءة،
 * شريحة بأرجل، بطاقة رسوميات بمروحة، ذاكرة بشرائح، قرص بطبق، شاشة بقاعدة،
 * بطارية برق، ميزان حرارة، واي فاي، سماعة، يد تحكّم، شبكة تطبيقات، ترس، درع
 * بمفتاح، منزلقات، مفكّ، كاميرا، رادار، ماسة الذكاء (لا نجمة — بوّابة
 * `VisualIdentityTest`)، جزيرة، تلوين فوق، مشهد فيلم، ساعة، مستند بطية،
 * راديو-مسح، أعمدة، بطارية نبض.
 */
enum class MaxIllustrationKind {
    Device, Cpu, Gpu, Memory, Storage, Display, Power, Thermal, Network, Audio,
    Games, Apps, System, Security, Controls, Tools, Camera, Sensors, Intelligence,
    Island, Overlay, Clips, Sessions, Manifest, Scan, Usage, BatteryHistory,
}

enum class MaxIllustrationInk { Accent, Outline, Wash }

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
    "PhoneAndroid", "PhoneIphone", "Smartphone", "Info", "Home" -> MaxIllustrationKind.Device
    "Memory", "DeveloperBoard" -> MaxIllustrationKind.Cpu
    "Speed", "VideogameAsset" -> MaxIllustrationKind.Gpu
    "Storage", "Save", "Backup", "DataUsage", "Folder", "FolderOpen" -> MaxIllustrationKind.Storage
    "DisplaySettings", "AspectRatio", "Monitor", "ScreenRotation" -> MaxIllustrationKind.Display
    "BatteryChargingFull", "BatteryFull", "BatteryStd", "Power", "Cable", "Bedtime" -> MaxIllustrationKind.Power
    "Thermostat", "DeviceThermostat", "Whatshot" -> MaxIllustrationKind.Thermal
    "NetworkCheck", "Wifi", "Language", "Router" -> MaxIllustrationKind.Network
    "GraphicEq", "VolumeUp", "Headphones", "Audiotrack", "MusicNote" -> MaxIllustrationKind.Audio
    "Gamepad", "SportsEsports", "Games" -> MaxIllustrationKind.Games
    "Apps", "AppSettingsAlt", "Android" -> MaxIllustrationKind.Apps
    "Settings", "SettingsApplications", "Code", "Terminal" -> MaxIllustrationKind.System
    "Shield", "Security", "Key", "Lock", "VerifiedUser" -> MaxIllustrationKind.Security
    "Tune", "TouchApp" -> MaxIllustrationKind.Controls
    "SmartButton" -> MaxIllustrationKind.Island
    "PictureInPicture" -> MaxIllustrationKind.Overlay
    "VideoLibrary" -> MaxIllustrationKind.Clips
    "History" -> MaxIllustrationKind.Sessions
    "Policy", "FactCheck" -> MaxIllustrationKind.Manifest
    "Radar", "TravelExplore", "NetworkPing" -> MaxIllustrationKind.Scan
    "QueryStats", "BarChart", "InsertChart", "HourglassEmpty" -> MaxIllustrationKind.Usage
    "BatterySaver", "BatteryAlert", "BatteryCharging90" -> MaxIllustrationKind.BatteryHistory
    "Build", "Science", "CleaningServices", "BugReport", "Edit" -> MaxIllustrationKind.Tools
    "Camera", "CameraAlt", "PhotoCamera" -> MaxIllustrationKind.Camera
    "Sensors", "SensorsOff", "Explore" -> MaxIllustrationKind.Sensors
    // ولا تُسجَّل هنا أسماء رموز الذكاء: بوّابة `VisualIdentityTest` تمنع «النجمة» خارج أسطح
    // Max AI، فمجالُ الذكاء يأتي من خريطة الوجهات وحدها (`MaxDestinationIllustrations`).
    "Insights", "Timeline" -> MaxIllustrationKind.Intelligence
    else -> null
}

/** No platform calls, randomness, clocks or unbounded detail in the artwork. */
fun maxIllustrationParts(kind: MaxIllustrationKind): List<MaxIllustrationPart> = buildList {
    fun line(
        vararg xy: Float,
        ink: MaxIllustrationInk = MaxIllustrationInk.Accent,
        closed: Boolean = false,
        filled: Boolean = false,
    ) {
        add(
            MaxIllustrationPart.Line(
                points = xy.toList().chunked(2).map { MaxIllustrationPoint(it[0], it[1]) },
                ink = ink,
                closed = closed,
                filled = filled,
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

    when (kind) {
        MaxIllustrationKind.Device -> {
            box(0.22f, 0.08f, 0.56f, 0.84f, wash, filled = true, radius = 0.12f)
            box(0.22f, 0.08f, 0.56f, 0.84f, outline, radius = 0.12f)
            box(0.31f, 0.19f, 0.38f, 0.52f, filled = true, radius = 0.06f)
            line(0.43f, 0.8f, 0.57f, 0.8f, ink = outline)
        }
        MaxIllustrationKind.Cpu -> {
            box(0.2f, 0.2f, 0.6f, 0.6f, wash, filled = true, radius = 0.12f)
            box(0.2f, 0.2f, 0.6f, 0.6f, outline, radius = 0.12f)
            box(0.33f, 0.33f, 0.34f, 0.34f, filled = true, radius = 0.08f)
            line(0.3f, 0.09f, 0.3f, 0.19f, ink = outline)
            line(0.3f, 0.81f, 0.3f, 0.91f, ink = outline)
            line(0.09f, 0.3f, 0.19f, 0.3f, ink = outline)
            line(0.81f, 0.3f, 0.91f, 0.3f, ink = outline)
            line(0.5f, 0.09f, 0.5f, 0.19f, ink = outline)
            line(0.5f, 0.81f, 0.5f, 0.91f, ink = outline)
            line(0.09f, 0.5f, 0.19f, 0.5f, ink = outline)
            line(0.81f, 0.5f, 0.91f, 0.5f, ink = outline)
            line(0.7f, 0.09f, 0.7f, 0.19f, ink = outline)
            line(0.7f, 0.81f, 0.7f, 0.91f, ink = outline)
            line(0.09f, 0.7f, 0.19f, 0.7f, ink = outline)
            line(0.81f, 0.7f, 0.91f, 0.7f, ink = outline)
        }
        MaxIllustrationKind.Gpu -> {
            box(0.08f, 0.26f, 0.84f, 0.48f, wash, filled = true, radius = 0.1f)
            box(0.08f, 0.26f, 0.84f, 0.48f, outline, radius = 0.1f)
            ring(0.38f, 0.5f, 0.19f)
            ring(0.38f, 0.5f, 0.06f, filled = true)
            line(0.38f, 0.575f, 0.38f, 0.665f)
            line(0.315f, 0.463f, 0.237f, 0.417f)
            line(0.445f, 0.462f, 0.523f, 0.417f)
            line(0.64f, 0.76f, 0.64f, 0.9f, ink = outline)
            line(0.74f, 0.76f, 0.74f, 0.9f, ink = outline)
            line(0.84f, 0.76f, 0.84f, 0.9f, ink = outline)
        }
        MaxIllustrationKind.Memory -> {
            box(0.1f, 0.28f, 0.8f, 0.42f, wash, filled = true, radius = 0.08f)
            box(0.1f, 0.28f, 0.8f, 0.42f, outline, radius = 0.08f)
            box(0.17f, 0.35f, 0.18f, 0.2f, filled = true, radius = 0.03f)
            box(0.41f, 0.35f, 0.18f, 0.2f, filled = true, radius = 0.03f)
            box(0.65f, 0.35f, 0.18f, 0.2f, filled = true, radius = 0.03f)
            line(0.15f, 0.72f, 0.15f, 0.86f, ink = outline)
            line(0.275f, 0.72f, 0.275f, 0.86f, ink = outline)
            line(0.4f, 0.72f, 0.4f, 0.86f, ink = outline)
            line(0.525f, 0.72f, 0.525f, 0.86f, ink = outline)
            line(0.65f, 0.72f, 0.65f, 0.86f, ink = outline)
            line(0.775f, 0.72f, 0.775f, 0.86f, ink = outline)
        }
        MaxIllustrationKind.Storage -> {
            box(0.1f, 0.2f, 0.8f, 0.6f, wash, filled = true, radius = 0.1f)
            box(0.1f, 0.2f, 0.8f, 0.6f, outline, radius = 0.1f)
            ring(0.42f, 0.5f, 0.2f)
            ring(0.42f, 0.5f, 0.06f, filled = true)
        }
        MaxIllustrationKind.Display -> {
            box(0.1f, 0.14f, 0.8f, 0.58f, wash, filled = true, radius = 0.08f)
            box(0.1f, 0.14f, 0.8f, 0.58f, outline, radius = 0.08f)
            box(0.2f, 0.24f, 0.6f, 0.38f, filled = true, radius = 0.05f)
            line(0.5f, 0.72f, 0.5f, 0.82f, ink = outline)
            box(0.34f, 0.84f, 0.32f, 0.06f, outline, filled = true, radius = 0.03f)
        }
        MaxIllustrationKind.Power -> {
            box(0.08f, 0.36f, 0.74f, 0.36f, wash, filled = true, radius = 0.08f)
            box(0.08f, 0.36f, 0.74f, 0.36f, outline, radius = 0.08f)
            box(0.84f, 0.46f, 0.06f, 0.16f, outline, radius = 0.02f)
            line(0.56f, 0.1f, 0.3f, 0.52f, 0.46f, 0.52f, 0.34f, 0.9f, 0.62f, 0.44f, 0.48f, 0.44f, closed = true, filled = true)
        }
        MaxIllustrationKind.Thermal -> {
            box(0.42f, 0.1f, 0.16f, 0.54f, outline, radius = 0.08f)
            box(0.46f, 0.26f, 0.08f, 0.4f, filled = true, radius = 0.04f)
            ring(0.5f, 0.72f, 0.15f, wash, filled = true)
            ring(0.5f, 0.72f, 0.15f)
            ring(0.5f, 0.72f, 0.055f, filled = true)
            line(0.68f, 0.18f, 0.8f, 0.18f, ink = outline)
            line(0.68f, 0.32f, 0.8f, 0.32f, ink = outline)
            line(0.68f, 0.46f, 0.8f, 0.46f, ink = outline)
        }
        MaxIllustrationKind.Network -> {
            line(0.373f, 0.613f, 0.404f, 0.588f, 0.441f, 0.57f, 0.48f, 0.561f, 0.52f, 0.561f, 0.559f, 0.57f, 0.596f, 0.588f, 0.627f, 0.613f)
            line(0.274f, 0.514f, 0.33f, 0.469f, 0.394f, 0.438f, 0.464f, 0.422f, 0.536f, 0.422f, 0.606f, 0.438f, 0.67f, 0.469f, 0.726f, 0.514f, ink = outline)
            line(0.175f, 0.415f, 0.255f, 0.351f, 0.348f, 0.306f, 0.448f, 0.283f, 0.552f, 0.283f, 0.652f, 0.306f, 0.745f, 0.351f, 0.825f, 0.415f, ink = outline)
            ring(0.5f, 0.74f, 0.05f, filled = true)
        }
        MaxIllustrationKind.Audio -> {
            line(0.14f, 0.4f, 0.3f, 0.4f, 0.5f, 0.2f, 0.5f, 0.8f, 0.3f, 0.6f, 0.14f, 0.6f, closed = true, filled = true)
            line(0.647f, 0.346f, 0.682f, 0.382f, 0.706f, 0.426f, 0.718f, 0.475f, 0.718f, 0.525f, 0.706f, 0.574f, 0.682f, 0.618f, 0.647f, 0.654f, ink = outline)
            line(0.737f, 0.238f, 0.795f, 0.3f, 0.836f, 0.375f, 0.857f, 0.457f, 0.857f, 0.543f, 0.836f, 0.625f, 0.795f, 0.7f, 0.737f, 0.762f, ink = outline)
        }
        MaxIllustrationKind.Games -> {
            box(0.08f, 0.3f, 0.84f, 0.44f, wash, filled = true, radius = 0.22f)
            box(0.08f, 0.3f, 0.84f, 0.44f, outline, radius = 0.22f)
            box(0.21f, 0.4f, 0.07f, 0.24f, filled = true, radius = 0.03f)
            box(0.125f, 0.485f, 0.24f, 0.07f, filled = true, radius = 0.03f)
            ring(0.66f, 0.45f, 0.05f, filled = true)
            ring(0.79f, 0.58f, 0.05f)
        }
        MaxIllustrationKind.Apps -> {
            box(0.12f, 0.12f, 0.34f, 0.34f, filled = true, radius = 0.1f)
            box(0.54f, 0.12f, 0.34f, 0.34f, outline, radius = 0.1f)
            box(0.12f, 0.54f, 0.34f, 0.34f, outline, radius = 0.1f)
            box(0.54f, 0.54f, 0.34f, 0.34f, outline, radius = 0.1f)
        }
        MaxIllustrationKind.System -> {
            ring(0.5f, 0.5f, 0.31f, outline)
            line(0.81f, 0.5f, 0.93f, 0.5f, ink = outline)
            line(0.719f, 0.719f, 0.804f, 0.804f, ink = outline)
            line(0.5f, 0.81f, 0.5f, 0.93f, ink = outline)
            line(0.281f, 0.719f, 0.196f, 0.804f, ink = outline)
            line(0.19f, 0.5f, 0.07f, 0.5f, ink = outline)
            line(0.281f, 0.281f, 0.196f, 0.196f, ink = outline)
            line(0.5f, 0.19f, 0.5f, 0.07f, ink = outline)
            line(0.719f, 0.281f, 0.804f, 0.196f, ink = outline)
            ring(0.5f, 0.5f, 0.12f, filled = true)
        }
        MaxIllustrationKind.Security -> {
            line(0.5f, 0.08f, 0.84f, 0.2f, 0.8f, 0.56f, 0.5f, 0.9f, 0.2f, 0.56f, 0.16f, 0.2f, ink = wash, closed = true, filled = true)
            line(0.5f, 0.08f, 0.84f, 0.2f, 0.8f, 0.56f, 0.5f, 0.9f, 0.2f, 0.56f, 0.16f, 0.2f, ink = outline, closed = true)
            ring(0.5f, 0.4f, 0.09f)
            line(0.5f, 0.49f, 0.5f, 0.64f)
        }
        MaxIllustrationKind.Controls -> {
            line(0.12f, 0.22f, 0.88f, 0.22f, ink = outline)
            ring(0.32f, 0.22f, 0.085f, filled = true)
            line(0.12f, 0.5f, 0.88f, 0.5f, ink = outline)
            ring(0.68f, 0.5f, 0.085f, filled = true)
            line(0.12f, 0.78f, 0.88f, 0.78f, ink = outline)
            ring(0.42f, 0.78f, 0.085f, filled = true)
        }
        MaxIllustrationKind.Tools -> {
            box(0.38f, 0.08f, 0.24f, 0.32f, filled = true, radius = 0.08f)
            box(0.38f, 0.08f, 0.24f, 0.32f, outline, radius = 0.08f)
            line(0.5f, 0.42f, 0.5f, 0.76f)
            line(0.42f, 0.76f, 0.58f, 0.76f)
        }
        MaxIllustrationKind.Camera -> {
            line(0.3f, 0.3f, 0.36f, 0.2f, 0.64f, 0.2f, 0.7f, 0.3f, ink = outline)
            box(0.08f, 0.3f, 0.84f, 0.52f, wash, filled = true, radius = 0.1f)
            box(0.08f, 0.3f, 0.84f, 0.52f, outline, radius = 0.1f)
            ring(0.5f, 0.56f, 0.16f)
            ring(0.5f, 0.56f, 0.07f, filled = true)
        }
        MaxIllustrationKind.Sensors -> {
            ring(0.5f, 0.5f, 0.09f, filled = true)
            line(0.344f, 0.656f, 0.314f, 0.617f, 0.292f, 0.573f, 0.281f, 0.525f, 0.281f, 0.475f, 0.292f, 0.427f, 0.314f, 0.383f, 0.344f, 0.344f, ink = outline)
            line(0.245f, 0.755f, 0.195f, 0.692f, 0.16f, 0.619f, 0.142f, 0.54f, 0.142f, 0.46f, 0.16f, 0.381f, 0.195f, 0.308f, 0.245f, 0.245f, ink = outline)
            line(0.656f, 0.344f, 0.686f, 0.383f, 0.708f, 0.427f, 0.719f, 0.475f, 0.719f, 0.525f, 0.708f, 0.573f, 0.686f, 0.617f, 0.656f, 0.656f, ink = outline)
            line(0.755f, 0.245f, 0.805f, 0.308f, 0.84f, 0.381f, 0.858f, 0.46f, 0.858f, 0.54f, 0.84f, 0.619f, 0.805f, 0.692f, 0.755f, 0.755f, ink = outline)
        }
        MaxIllustrationKind.Intelligence -> {
            line(0.5f, 0.1f, 0.9f, 0.5f, 0.5f, 0.9f, 0.1f, 0.5f, ink = wash, closed = true, filled = true)
            line(0.5f, 0.1f, 0.9f, 0.5f, 0.5f, 0.9f, 0.1f, 0.5f, ink = outline, closed = true)
            ring(0.5f, 0.5f, 0.14f)
            ring(0.5f, 0.5f, 0.05f, filled = true)
        }
        MaxIllustrationKind.Island -> {
            box(0.1f, 0.38f, 0.8f, 0.24f, wash, filled = true, radius = 0.12f)
            box(0.1f, 0.38f, 0.8f, 0.24f, outline, radius = 0.12f)
            ring(0.24f, 0.5f, 0.06f, filled = true)
            line(0.42f, 0.5f, 0.66f, 0.5f, ink = outline)
        }
        MaxIllustrationKind.Overlay -> {
            box(0.1f, 0.12f, 0.8f, 0.62f, wash, filled = true, radius = 0.08f)
            box(0.1f, 0.12f, 0.8f, 0.62f, outline, radius = 0.08f)
            box(0.46f, 0.48f, 0.38f, 0.3f, filled = true, radius = 0.06f)
            box(0.46f, 0.48f, 0.38f, 0.3f, outline, radius = 0.06f)
        }
        MaxIllustrationKind.Clips -> {
            box(0.1f, 0.18f, 0.8f, 0.64f, wash, filled = true, radius = 0.08f)
            box(0.1f, 0.18f, 0.8f, 0.64f, outline, radius = 0.08f)
            box(0.15f, 0.26f, 0.07f, 0.09f, outline, radius = 0.02f)
            box(0.78f, 0.26f, 0.07f, 0.09f, outline, radius = 0.02f)
            box(0.15f, 0.45f, 0.07f, 0.09f, outline, radius = 0.02f)
            box(0.78f, 0.45f, 0.07f, 0.09f, outline, radius = 0.02f)
            box(0.15f, 0.64f, 0.07f, 0.09f, outline, radius = 0.02f)
            box(0.78f, 0.64f, 0.07f, 0.09f, outline, radius = 0.02f)
            line(0.42f, 0.36f, 0.42f, 0.64f, 0.68f, 0.5f, closed = true, filled = true)
        }
        MaxIllustrationKind.Sessions -> {
            ring(0.5f, 0.52f, 0.3f, wash, filled = true)
            ring(0.5f, 0.52f, 0.3f, outline)
            line(0.5f, 0.52f, 0.5f, 0.34f)
            line(0.5f, 0.52f, 0.66f, 0.6f)
            line(0.124f, 0.385f, 0.166f, 0.3f, 0.226f, 0.228f, 0.302f, 0.173f, 0.388f, 0.136f, 0.481f, 0.12f, 0.575f, 0.127f, ink = outline)
            line(0.13f, 0.39f, 0.13f, 0.53f, ink = outline)
            line(0.13f, 0.39f, 0.26f, 0.35f, ink = outline)
        }
        MaxIllustrationKind.Manifest -> {
            line(0.26f, 0.1f, 0.62f, 0.1f, 0.76f, 0.24f, 0.76f, 0.9f, 0.26f, 0.9f, ink = wash, closed = true, filled = true)
            line(0.26f, 0.1f, 0.62f, 0.1f, 0.76f, 0.24f, 0.76f, 0.9f, 0.26f, 0.9f, ink = outline, closed = true)
            line(0.62f, 0.1f, 0.62f, 0.24f, 0.76f, 0.24f, ink = outline)
            line(0.34f, 0.42f, 0.68f, 0.42f)
            line(0.34f, 0.54f, 0.68f, 0.54f)
            line(0.34f, 0.66f, 0.56f, 0.66f)
        }
        MaxIllustrationKind.Scan -> {
            ring(0.5f, 0.5f, 0.36f, outline)
            line(0.5f, 0.14f, 0.563f, 0.145f, 0.623f, 0.162f, 0.68f, 0.188f, 0.731f, 0.224f, 0.776f, 0.269f, 0.812f, 0.32f)
            line(0.5f, 0.28f, 0.546f, 0.285f, 0.589f, 0.299f, 0.629f, 0.322f, 0.663f, 0.353f, 0.691f, 0.39f)
            ring(0.5f, 0.5f, 0.05f, filled = true)
            ring(0.64f, 0.32f, 0.035f, filled = true)
            ring(0.34f, 0.66f, 0.035f, outline)
        }
        MaxIllustrationKind.Usage -> {
            line(0.12f, 0.84f, 0.88f, 0.84f, ink = outline)
            box(0.2f, 0.56f, 0.14f, 0.28f, filled = true, radius = 0.02f)
            box(0.43f, 0.4f, 0.14f, 0.44f, filled = true, radius = 0.02f)
            box(0.66f, 0.22f, 0.14f, 0.62f, filled = true, radius = 0.02f)
        }
        MaxIllustrationKind.BatteryHistory -> {
            box(0.08f, 0.36f, 0.74f, 0.36f, wash, filled = true, radius = 0.08f)
            box(0.08f, 0.36f, 0.74f, 0.36f, outline, radius = 0.08f)
            box(0.84f, 0.46f, 0.06f, 0.16f, outline, radius = 0.02f)
            line(0.16f, 0.54f, 0.32f, 0.54f, 0.4f, 0.42f, 0.5f, 0.64f, 0.58f, 0.54f, 0.74f, 0.54f)
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
