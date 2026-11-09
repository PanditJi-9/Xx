/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.design

/** Navigation artwork, not telemetry: its topology never changes with a reading. */
enum class MaxIllustrationKind {
    Device, Cpu, Gpu, Memory, Storage, Display, Power, Thermal, Network, Audio,
    Games, Apps, System, Security, Controls, Tools, Camera, Sensors, Intelligence,
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
    "Tune", "TouchApp", "SmartButton", "PictureInPicture" -> MaxIllustrationKind.Controls
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
    fun line(vararg xy: Float, ink: MaxIllustrationInk = MaxIllustrationInk.Accent, closed: Boolean = false) {
        add(MaxIllustrationPart.Line(xy.toList().chunked(2).map { MaxIllustrationPoint(it[0], it[1]) }, ink, closed))
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
            box(0.29f, 0.12f, 0.42f, 0.76f, wash, true, 0.08f)
            box(0.29f, 0.12f, 0.42f, 0.76f, radius = 0.08f)
            line(0.42f, 0.20f, 0.58f, 0.20f)
            line(0.43f, 0.79f, 0.57f, 0.79f)
            line(0.38f, 0.48f, 0.46f, 0.40f, 0.56f, 0.53f, 0.63f, 0.45f, ink = outline)
            line(0.15f, 0.33f, 0.22f, 0.33f, ink = outline)
            line(0.78f, 0.67f, 0.86f, 0.67f, ink = outline)
        }
        MaxIllustrationKind.Cpu -> {
            box(0.26f, 0.26f, 0.48f, 0.48f, wash, true)
            box(0.26f, 0.26f, 0.48f, 0.48f)
            for (i in 0..3) {
                val p = 0.34f + i * 0.105f
                line(p, 0.15f, p, 0.24f, ink = outline)
                line(p, 0.76f, p, 0.85f, ink = outline)
                line(0.15f, p, 0.24f, p, ink = outline)
                line(0.76f, p, 0.85f, p, ink = outline)
            }
            box(0.37f, 0.37f, 0.26f, 0.26f, radius = 0.025f)
            line(0.50f, 0.38f, 0.50f, 0.62f, ink = outline)
            line(0.38f, 0.50f, 0.62f, 0.50f, ink = outline)
        }
        MaxIllustrationKind.Gpu -> {
            line(0.16f, 0.63f, 0.50f, 0.83f, 0.84f, 0.63f, ink = outline)
            line(0.16f, 0.49f, 0.50f, 0.69f, 0.84f, 0.49f, ink = outline)
            line(0.16f, 0.34f, 0.50f, 0.14f, 0.84f, 0.34f, 0.50f, 0.54f, closed = true)
            line(0.50f, 0.15f, 0.50f, 0.53f, ink = outline)
            line(0.33f, 0.24f, 0.67f, 0.44f, ink = outline)
            line(0.67f, 0.24f, 0.33f, 0.44f, ink = outline)
            ring(0.50f, 0.34f, 0.045f, filled = true)
        }
        MaxIllustrationKind.Memory -> {
            box(0.14f, 0.29f, 0.72f, 0.42f, wash, true)
            box(0.14f, 0.29f, 0.72f, 0.42f)
            for (i in 0..2) box(0.23f + i * 0.20f, 0.38f, 0.13f, 0.20f, radius = 0.02f)
            for (i in 0..5) line(0.25f + i * 0.10f, 0.73f, 0.25f + i * 0.10f, 0.81f, ink = outline)
        }
        MaxIllustrationKind.Storage -> {
            for (i in 0..2) {
                val y = 0.17f + i * 0.23f
                box(0.19f, y, 0.62f, 0.18f, if (i == 0) wash else outline, filled = i == 0)
                if (i == 0) box(0.19f, y, 0.62f, 0.18f)
                ring(0.29f, y + 0.09f, 0.025f, filled = true)
                line(0.58f, y + 0.09f, 0.71f, y + 0.09f, ink = outline)
            }
        }
        MaxIllustrationKind.Display -> {
            box(0.23f, 0.11f, 0.54f, 0.78f, wash, true, 0.055f)
            box(0.23f, 0.11f, 0.54f, 0.78f, radius = 0.055f)
            line(0.35f, 0.68f, 0.65f, 0.32f)
            line(0.35f, 0.58f, 0.35f, 0.68f, 0.45f, 0.68f)
            line(0.55f, 0.32f, 0.65f, 0.32f, 0.65f, 0.42f)
            line(0.40f, 0.80f, 0.60f, 0.80f, ink = outline)
        }
        MaxIllustrationKind.Power -> {
            box(0.16f, 0.30f, 0.65f, 0.40f, wash, true, 0.07f)
            box(0.16f, 0.30f, 0.65f, 0.40f, radius = 0.07f)
            line(0.85f, 0.43f, 0.85f, 0.57f)
            line(0.51f, 0.35f, 0.39f, 0.51f, 0.55f, 0.51f, 0.43f, 0.65f)
            line(0.20f, 0.79f, 0.63f, 0.79f, ink = outline)
        }
        MaxIllustrationKind.Thermal -> {
            box(0.39f, 0.16f, 0.20f, 0.47f, radius = 0.10f)
            ring(0.49f, 0.73f, 0.14f, wash, true)
            ring(0.49f, 0.73f, 0.14f)
            line(0.49f, 0.37f, 0.49f, 0.73f)
            for (i in 0..2) line(0.66f, 0.28f + i * 0.13f, 0.76f, 0.28f + i * 0.13f, ink = outline)
        }
        MaxIllustrationKind.Network -> {
            line(0.28f, 0.70f, 0.50f, 0.50f, 0.72f, 0.70f, ink = outline)
            line(0.50f, 0.50f, 0.50f, 0.26f, ink = outline)
            ring(0.50f, 0.50f, 0.11f, wash, true)
            ring(0.50f, 0.50f, 0.11f)
            ring(0.50f, 0.19f, 0.065f)
            ring(0.23f, 0.75f, 0.065f)
            ring(0.77f, 0.75f, 0.065f)
            line(0.18f, 0.41f, 0.23f, 0.31f, 0.32f, 0.24f, ink = outline)
            line(0.68f, 0.24f, 0.77f, 0.31f, 0.82f, 0.41f, ink = outline)
        }
        MaxIllustrationKind.Audio -> {
            line(0.17f, 0.40f, 0.32f, 0.40f, 0.48f, 0.25f, 0.48f, 0.75f, 0.32f, 0.60f, 0.17f, 0.60f, closed = true)
            line(0.60f, 0.37f, 0.67f, 0.43f, 0.69f, 0.50f, 0.67f, 0.57f, 0.60f, 0.63f)
            line(0.70f, 0.25f, 0.80f, 0.36f, 0.84f, 0.50f, 0.80f, 0.64f, 0.70f, 0.75f, ink = outline)
        }
        MaxIllustrationKind.Games -> {
            line(0.22f, 0.31f, 0.78f, 0.31f, 0.88f, 0.69f, 0.75f, 0.75f, 0.63f, 0.61f, 0.37f, 0.61f, 0.25f, 0.75f, 0.12f, 0.69f, closed = true)
            line(0.25f, 0.45f, 0.39f, 0.45f)
            line(0.32f, 0.38f, 0.32f, 0.52f)
            ring(0.69f, 0.42f, 0.035f, filled = true)
            ring(0.77f, 0.51f, 0.035f, filled = true)
            line(0.44f, 0.52f, 0.54f, 0.52f, ink = outline)
        }
        MaxIllustrationKind.Apps -> {
            box(0.17f, 0.17f, 0.27f, 0.27f, wash, true, 0.06f)
            box(0.17f, 0.17f, 0.27f, 0.27f, radius = 0.06f)
            box(0.56f, 0.17f, 0.27f, 0.27f, outline, radius = 0.06f)
            box(0.17f, 0.56f, 0.27f, 0.27f, outline, radius = 0.06f)
            ring(0.695f, 0.695f, 0.13f)
            line(0.63f, 0.695f, 0.76f, 0.695f)
            line(0.695f, 0.63f, 0.695f, 0.76f)
        }
        MaxIllustrationKind.System -> {
            line(0.18f, 0.29f, 0.50f, 0.13f, 0.82f, 0.29f, 0.50f, 0.45f, closed = true)
            line(0.18f, 0.45f, 0.50f, 0.61f, 0.82f, 0.45f, ink = outline)
            line(0.18f, 0.61f, 0.50f, 0.77f, 0.82f, 0.61f, ink = outline)
            line(0.18f, 0.77f, 0.50f, 0.93f, 0.82f, 0.77f, ink = outline)
        }
        MaxIllustrationKind.Security -> {
            line(0.50f, 0.12f, 0.80f, 0.25f, 0.75f, 0.60f, 0.65f, 0.75f, 0.50f, 0.87f, 0.35f, 0.75f, 0.25f, 0.60f, 0.20f, 0.25f, closed = true)
            ring(0.50f, 0.43f, 0.085f, wash, true)
            ring(0.50f, 0.43f, 0.085f)
            line(0.50f, 0.52f, 0.50f, 0.65f)
        }
        MaxIllustrationKind.Controls -> {
            for (i in 0..2) {
                val y = 0.24f + i * 0.26f
                val x = if (i == 1) 0.65f else 0.35f
                line(0.16f, y, x - 0.08f, y, ink = outline)
                line(x + 0.08f, y, 0.84f, y, ink = outline)
                ring(x, y, 0.07f)
            }
        }
        MaxIllustrationKind.Tools -> {
            line(0.19f, 0.74f, 0.74f, 0.19f, 0.82f, 0.27f, 0.27f, 0.82f, closed = true)
            for (i in 0..2) {
                val p = 0.39f + i * 0.13f
                line(p, 0.81f - p, p + 0.06f, 0.87f - p, ink = outline)
            }
            line(0.23f, 0.20f, 0.23f, 0.40f, 0.40f, 0.40f, ink = outline)
            ring(0.68f, 0.70f, 0.075f)
        }
        MaxIllustrationKind.Camera -> {
            box(0.14f, 0.29f, 0.72f, 0.48f, wash, true, 0.07f)
            box(0.14f, 0.29f, 0.72f, 0.48f, radius = 0.07f)
            line(0.30f, 0.28f, 0.36f, 0.19f, 0.59f, 0.19f, 0.65f, 0.28f)
            ring(0.50f, 0.53f, 0.17f)
            ring(0.50f, 0.53f, 0.10f, outline)
            ring(0.75f, 0.39f, 0.025f, filled = true)
        }
        MaxIllustrationKind.Sensors -> {
            ring(0.50f, 0.50f, 0.12f, wash, true)
            ring(0.50f, 0.50f, 0.12f)
            line(0.50f, 0.17f, 0.50f, 0.32f, ink = outline)
            line(0.50f, 0.68f, 0.50f, 0.83f, ink = outline)
            line(0.17f, 0.50f, 0.32f, 0.50f, ink = outline)
            line(0.68f, 0.50f, 0.83f, 0.50f, ink = outline)
            ring(0.24f, 0.24f, 0.055f)
            ring(0.76f, 0.76f, 0.055f)
            ring(0.76f, 0.24f, 0.055f, outline)
            ring(0.24f, 0.76f, 0.055f, outline)
        }
        MaxIllustrationKind.Intelligence -> {
            line(0.50f, 0.20f, 0.80f, 0.50f, 0.50f, 0.80f, 0.20f, 0.50f, closed = true, ink = outline)
            ring(0.50f, 0.50f, 0.14f, wash, true)
            ring(0.50f, 0.50f, 0.14f)
            ring(0.50f, 0.20f, 0.06f)
            ring(0.80f, 0.50f, 0.06f)
            ring(0.50f, 0.80f, 0.06f)
            ring(0.20f, 0.50f, 0.06f)
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
