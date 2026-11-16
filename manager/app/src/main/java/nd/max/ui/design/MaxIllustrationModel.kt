/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.design

/**
 * Navigation artwork, not telemetry: its topology never changes with a reading.
 *
 * The current family is a **filled pictogram**, not a wireframe: one wash silhouette gives the
 * domain a shape at 34dp, one accent detail names it, and the outline keeps it legible on both
 * light and dark surfaces. The older set was almost entirely 1.5dp line work; at row size the
 * domains read as the same thin diagram with different internal scratches.
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
            box(0.28f, 0.10f, 0.44f, 0.80f, wash, true, 0.10f)
            box(0.28f, 0.10f, 0.44f, 0.80f, radius = 0.10f)
            line(0.40f, 0.17f, 0.60f, 0.17f, ink = outline)
            line(0.45f, 0.82f, 0.55f, 0.82f, ink = outline)
            line(0.35f, 0.59f, 0.44f, 0.47f, 0.53f, 0.57f, 0.65f, 0.40f)
            ring(0.50f, 0.72f, 0.035f, filled = true)
            line(0.16f, 0.30f, 0.23f, 0.30f, ink = outline)
            line(0.77f, 0.66f, 0.84f, 0.66f, ink = outline)
        }
        MaxIllustrationKind.Cpu -> {
            box(0.24f, 0.24f, 0.52f, 0.52f, wash, true, 0.10f)
            box(0.24f, 0.24f, 0.52f, 0.52f, radius = 0.10f)
            box(0.36f, 0.36f, 0.28f, 0.28f, wash, true, 0.06f)
            box(0.36f, 0.36f, 0.28f, 0.28f, radius = 0.06f)
            line(0.50f, 0.37f, 0.50f, 0.63f, ink = outline)
            line(0.37f, 0.50f, 0.63f, 0.50f, ink = outline)
            for (i in 0..3) {
                val p = 0.32f + i * 0.12f
                line(p, 0.13f, p, 0.23f, ink = outline)
                line(p, 0.77f, p, 0.87f, ink = outline)
                line(0.13f, p, 0.23f, p, ink = outline)
                line(0.77f, p, 0.87f, p, ink = outline)
            }
        }
        MaxIllustrationKind.Gpu -> {
            line(0.50f, 0.12f, 0.86f, 0.32f, 0.86f, 0.68f, 0.50f, 0.88f, 0.14f, 0.68f, 0.14f, 0.32f, closed = true, filled = true, ink = wash)
            line(0.50f, 0.12f, 0.86f, 0.32f, 0.86f, 0.68f, 0.50f, 0.88f, 0.14f, 0.68f, 0.14f, 0.32f, closed = true)
            line(0.50f, 0.12f, 0.50f, 0.50f, 0.86f, 0.68f, ink = outline)
            line(0.50f, 0.50f, 0.14f, 0.68f, ink = outline)
            line(0.34f, 0.32f, 0.66f, 0.50f, 0.34f, 0.68f, closed = true, filled = true)
            ring(0.50f, 0.50f, 0.055f, filled = true)
        }
        MaxIllustrationKind.Memory -> {
            box(0.12f, 0.26f, 0.76f, 0.48f, wash, true, 0.08f)
            box(0.12f, 0.26f, 0.76f, 0.48f, radius = 0.08f)
            for (i in 0..2) {
                val x = 0.20f + i * 0.21f
                box(x, 0.36f, 0.15f, 0.28f, if (i == 1) wash else outline, filled = i == 1, radius = 0.03f)
                line(x + 0.075f, 0.36f, x + 0.075f, 0.28f, ink = outline)
            }
            for (i in 0..5) line(0.20f + i * 0.12f, 0.74f, 0.20f + i * 0.12f, 0.84f, ink = outline)
        }
        MaxIllustrationKind.Storage -> {
            for (i in 0..2) {
                val y = 0.16f + i * 0.24f
                box(0.18f, y, 0.64f, 0.20f, if (i == 0) wash else outline, filled = i == 0, radius = 0.05f)
                if (i == 0) box(0.18f, y, 0.64f, 0.20f, radius = 0.05f)
                ring(0.29f, y + 0.10f, 0.03f, filled = true)
                line(0.58f, y + 0.10f, 0.72f, y + 0.10f, ink = outline)
            }
        }
        MaxIllustrationKind.Display -> {
            box(0.18f, 0.10f, 0.64f, 0.72f, wash, true, 0.07f)
            box(0.18f, 0.10f, 0.64f, 0.72f, radius = 0.07f)
            line(0.32f, 0.66f, 0.68f, 0.28f)
            line(0.32f, 0.56f, 0.32f, 0.66f, 0.42f, 0.66f)
            line(0.52f, 0.28f, 0.68f, 0.28f, 0.68f, 0.38f)
            line(0.38f, 0.82f, 0.62f, 0.82f, ink = outline)
            line(0.50f, 0.72f, 0.50f, 0.82f, ink = outline)
        }
        MaxIllustrationKind.Power -> {
            box(0.12f, 0.30f, 0.70f, 0.40f, wash, true, 0.08f)
            box(0.12f, 0.30f, 0.70f, 0.40f, radius = 0.08f)
            box(0.82f, 0.42f, 0.06f, 0.16f, radius = 0.02f)
            line(0.46f, 0.33f, 0.30f, 0.53f, 0.46f, 0.53f, 0.38f, 0.67f, 0.56f, 0.47f, 0.40f, 0.47f, closed = true, filled = true)
            line(0.20f, 0.79f, 0.64f, 0.79f, ink = outline)
        }
        MaxIllustrationKind.Thermal -> {
            line(0.40f, 0.16f, 0.60f, 0.16f, 0.60f, 0.58f, 0.69f, 0.70f, 0.60f, 0.82f, 0.40f, 0.82f, 0.31f, 0.70f, 0.40f, 0.58f, closed = true, filled = true, ink = wash)
            line(0.40f, 0.16f, 0.60f, 0.16f, 0.60f, 0.58f, 0.69f, 0.70f, 0.60f, 0.82f, 0.40f, 0.82f, 0.31f, 0.70f, 0.40f, 0.58f, closed = true)
            ring(0.50f, 0.72f, 0.13f, wash, true)
            ring(0.50f, 0.72f, 0.13f)
            line(0.50f, 0.38f, 0.50f, 0.72f)
            for (i in 0..2) line(0.68f, 0.30f + i * 0.12f, 0.78f, 0.30f + i * 0.12f, ink = outline)
        }
        MaxIllustrationKind.Network -> {
            ring(0.50f, 0.50f, 0.40f, wash, true)
            line(0.50f, 0.10f, 0.50f, 0.90f, ink = outline)
            line(0.12f, 0.50f, 0.88f, 0.50f, ink = outline)
            line(0.50f, 0.10f, 0.78f, 0.24f, 0.88f, 0.50f, 0.78f, 0.76f, 0.50f, 0.90f, 0.22f, 0.76f, 0.12f, 0.50f, 0.22f, 0.24f, closed = true, filled = true, ink = wash)
            ring(0.50f, 0.50f, 0.10f)
            ring(0.50f, 0.18f, 0.055f, filled = true)
            ring(0.23f, 0.72f, 0.055f)
            ring(0.77f, 0.72f, 0.055f)
        }
        MaxIllustrationKind.Audio -> {
            line(0.16f, 0.38f, 0.32f, 0.38f, 0.48f, 0.22f, 0.48f, 0.78f, 0.32f, 0.62f, 0.16f, 0.62f, closed = true, filled = true, ink = wash)
            line(0.16f, 0.38f, 0.32f, 0.38f, 0.48f, 0.22f, 0.48f, 0.78f, 0.32f, 0.62f, 0.16f, 0.62f, closed = true)
            line(0.60f, 0.36f, 0.68f, 0.43f, 0.70f, 0.50f, 0.68f, 0.57f, 0.60f, 0.64f)
            line(0.71f, 0.24f, 0.81f, 0.36f, 0.85f, 0.50f, 0.81f, 0.64f, 0.71f, 0.76f, ink = outline)
        }
        MaxIllustrationKind.Games -> {
            line(0.18f, 0.30f, 0.82f, 0.30f, 0.90f, 0.68f, 0.74f, 0.78f, 0.62f, 0.60f, 0.38f, 0.60f, 0.26f, 0.78f, 0.10f, 0.68f, closed = true, filled = true, ink = wash)
            line(0.18f, 0.30f, 0.82f, 0.30f, 0.90f, 0.68f, 0.74f, 0.78f, 0.62f, 0.60f, 0.38f, 0.60f, 0.26f, 0.78f, 0.10f, 0.68f, closed = true)
            line(0.24f, 0.44f, 0.38f, 0.44f)
            line(0.31f, 0.37f, 0.31f, 0.51f)
            ring(0.69f, 0.42f, 0.04f, filled = true)
            ring(0.79f, 0.50f, 0.04f)
            line(0.43f, 0.53f, 0.57f, 0.53f, ink = outline)
        }
        MaxIllustrationKind.Apps -> {
            box(0.16f, 0.16f, 0.29f, 0.29f, wash, true, 0.07f)
            box(0.16f, 0.16f, 0.29f, 0.29f, radius = 0.07f)
            box(0.55f, 0.16f, 0.29f, 0.29f, radius = 0.07f)
            box(0.16f, 0.55f, 0.29f, 0.29f, radius = 0.07f)
            ring(0.695f, 0.695f, 0.145f, wash, true)
            ring(0.695f, 0.695f, 0.145f)
            line(0.61f, 0.695f, 0.78f, 0.695f)
            line(0.695f, 0.61f, 0.695f, 0.78f)
        }
        MaxIllustrationKind.System -> {
            line(0.50f, 0.10f, 0.88f, 0.31f, 0.50f, 0.52f, 0.12f, 0.31f, closed = true, filled = true, ink = wash)
            line(0.50f, 0.10f, 0.88f, 0.31f, 0.50f, 0.52f, 0.12f, 0.31f, closed = true)
            line(0.12f, 0.48f, 0.50f, 0.69f, 0.88f, 0.48f, ink = outline)
            line(0.12f, 0.66f, 0.50f, 0.87f, 0.88f, 0.66f, ink = outline)
            ring(0.50f, 0.31f, 0.05f, filled = true)
        }
        MaxIllustrationKind.Security -> {
            line(0.50f, 0.10f, 0.82f, 0.24f, 0.76f, 0.60f, 0.50f, 0.90f, 0.24f, 0.60f, 0.18f, 0.24f, closed = true, filled = true, ink = wash)
            line(0.50f, 0.10f, 0.82f, 0.24f, 0.76f, 0.60f, 0.50f, 0.90f, 0.24f, 0.60f, 0.18f, 0.24f, closed = true)
            ring(0.50f, 0.42f, 0.10f, wash, true)
            ring(0.50f, 0.42f, 0.10f)
            line(0.50f, 0.52f, 0.50f, 0.66f)
            ring(0.50f, 0.69f, 0.025f, filled = true)
        }
        MaxIllustrationKind.Controls -> {
            for (i in 0..2) {
                val y = 0.24f + i * 0.26f
                val x = if (i == 1) 0.65f else 0.35f
                line(0.14f, y, x - 0.09f, y, ink = outline)
                line(x + 0.09f, y, 0.86f, y, ink = outline)
                ring(x, y, 0.075f, wash, true)
                ring(x, y, 0.075f)
                ring(x, y, 0.025f, filled = true)
            }
        }
        MaxIllustrationKind.Tools -> {
            line(0.18f, 0.76f, 0.72f, 0.22f, 0.82f, 0.32f, 0.28f, 0.86f, closed = true, filled = true, ink = wash)
            line(0.18f, 0.76f, 0.72f, 0.22f, 0.82f, 0.32f, 0.28f, 0.86f, closed = true)
            line(0.18f, 0.42f, 0.18f, 0.20f, 0.40f, 0.20f, ink = outline)
            ring(0.68f, 0.70f, 0.09f, wash, true)
            ring(0.68f, 0.70f, 0.09f)
            line(0.62f, 0.70f, 0.74f, 0.70f)
        }
        MaxIllustrationKind.Camera -> {
            box(0.12f, 0.28f, 0.76f, 0.50f, wash, true, 0.08f)
            box(0.12f, 0.28f, 0.76f, 0.50f, radius = 0.08f)
            line(0.30f, 0.28f, 0.38f, 0.18f, 0.60f, 0.18f, 0.68f, 0.28f)
            ring(0.50f, 0.54f, 0.18f, wash, true)
            ring(0.50f, 0.54f, 0.18f)
            ring(0.50f, 0.54f, 0.11f, ink = outline)
            ring(0.76f, 0.38f, 0.03f, filled = true)
        }
        MaxIllustrationKind.Sensors -> {
            ring(0.50f, 0.50f, 0.34f, wash, true)
            ring(0.50f, 0.50f, 0.34f)
            ring(0.50f, 0.50f, 0.20f)
            ring(0.50f, 0.50f, 0.08f, filled = true)
            line(0.50f, 0.16f, 0.50f, 0.30f, ink = outline)
            line(0.50f, 0.70f, 0.50f, 0.84f, ink = outline)
            line(0.16f, 0.50f, 0.30f, 0.50f, ink = outline)
            line(0.70f, 0.50f, 0.84f, 0.50f, ink = outline)
        }
        MaxIllustrationKind.Intelligence -> {
            line(0.50f, 0.12f, 0.88f, 0.50f, 0.50f, 0.88f, 0.12f, 0.50f, closed = true, filled = true, ink = wash)
            line(0.50f, 0.12f, 0.88f, 0.50f, 0.50f, 0.88f, 0.12f, 0.50f, closed = true)
            ring(0.50f, 0.50f, 0.16f, wash, true)
            ring(0.50f, 0.50f, 0.16f)
            ring(0.50f, 0.16f, 0.06f, filled = true)
            ring(0.84f, 0.50f, 0.06f)
            ring(0.50f, 0.84f, 0.06f)
            ring(0.16f, 0.50f, 0.06f)
        }
        MaxIllustrationKind.Island -> {
            box(0.12f, 0.39f, 0.76f, 0.22f, wash, true, 0.11f)
            box(0.12f, 0.39f, 0.76f, 0.22f, radius = 0.11f)
            ring(0.27f, 0.50f, 0.035f, filled = true)
            line(0.36f, 0.50f, 0.52f, 0.50f, ink = outline)
            line(0.63f, 0.45f, 0.70f, 0.50f, 0.63f, 0.55f, closed = true, filled = true)
        }
        MaxIllustrationKind.Overlay -> {
            box(0.10f, 0.18f, 0.58f, 0.44f, wash, true, 0.05f)
            box(0.10f, 0.18f, 0.58f, 0.44f, radius = 0.05f)
            box(0.32f, 0.40f, 0.58f, 0.44f, outline, radius = 0.05f)
            line(0.40f, 0.62f, 0.50f, 0.52f, 0.58f, 0.57f, 0.68f, 0.46f)
            line(0.40f, 0.68f, 0.78f, 0.68f, ink = outline)
        }
        MaxIllustrationKind.Clips -> {
            box(0.14f, 0.20f, 0.72f, 0.60f, wash, true, 0.05f)
            box(0.14f, 0.20f, 0.72f, 0.60f, radius = 0.05f)
            for (i in 0..2) {
                val y = 0.25f + i * 0.17f
                box(0.18f, y, 0.08f, 0.09f, outline, radius = 0.015f)
                box(0.74f, y, 0.08f, 0.09f, outline, radius = 0.015f)
            }
            line(0.40f, 0.37f, 0.64f, 0.50f, 0.40f, 0.63f, closed = true, filled = true)
        }
        MaxIllustrationKind.Sessions -> {
            ring(0.50f, 0.52f, 0.30f, wash, true)
            ring(0.50f, 0.52f, 0.30f)
            line(0.50f, 0.52f, 0.50f, 0.34f)
            line(0.50f, 0.52f, 0.64f, 0.61f)
            ring(0.50f, 0.52f, 0.035f, filled = true)
            line(0.20f, 0.30f, 0.28f, 0.22f, 0.36f, 0.25f, ink = outline)
            line(0.20f, 0.30f, 0.20f, 0.20f, ink = outline)
        }
        MaxIllustrationKind.Manifest -> {
            box(0.24f, 0.12f, 0.50f, 0.76f, wash, true, 0.05f)
            box(0.24f, 0.12f, 0.50f, 0.76f, radius = 0.05f)
            line(0.62f, 0.12f, 0.74f, 0.24f, 0.62f, 0.24f, closed = true, ink = outline)
            line(0.32f, 0.34f, 0.58f, 0.34f, ink = outline)
            line(0.32f, 0.44f, 0.58f, 0.44f, ink = outline)
            line(0.32f, 0.54f, 0.50f, 0.54f, ink = outline)
            ring(0.62f, 0.74f, 0.10f, wash, true)
            ring(0.62f, 0.74f, 0.10f)
            line(0.57f, 0.74f, 0.61f, 0.79f, 0.68f, 0.69f)
        }
        MaxIllustrationKind.Scan -> {
            ring(0.50f, 0.50f, 0.34f, outline)
            ring(0.50f, 0.50f, 0.20f)
            ring(0.50f, 0.50f, 0.035f, filled = true)
            line(0.50f, 0.50f, 0.74f, 0.26f)
            line(0.50f, 0.22f, 0.62f, 0.28f, 0.70f, 0.40f, ink = outline)
            ring(0.33f, 0.58f, 0.03f, filled = true)
            ring(0.64f, 0.64f, 0.03f, filled = true)
        }
        MaxIllustrationKind.Usage -> {
            line(0.16f, 0.80f, 0.84f, 0.80f, ink = outline)
            box(0.24f, 0.56f, 0.13f, 0.24f, wash, true, 0.02f)
            box(0.43f, 0.40f, 0.13f, 0.40f, radius = 0.02f)
            box(0.62f, 0.24f, 0.13f, 0.56f, outline, radius = 0.02f)
            line(0.24f, 0.50f, 0.43f, 0.34f, 0.62f, 0.18f, 0.78f, 0.12f)
            ring(0.78f, 0.12f, 0.025f, filled = true)
        }
        MaxIllustrationKind.BatteryHistory -> {
            box(0.12f, 0.34f, 0.68f, 0.36f, wash, true, 0.06f)
            box(0.12f, 0.34f, 0.68f, 0.36f, radius = 0.06f)
            box(0.82f, 0.43f, 0.06f, 0.18f, radius = 0.02f)
            line(0.20f, 0.56f, 0.32f, 0.48f, 0.44f, 0.52f, 0.56f, 0.40f, 0.72f, 0.44f)
            ring(0.72f, 0.44f, 0.025f, filled = true)
            line(0.20f, 0.62f, 0.74f, 0.62f, ink = outline)
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
