/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.navigation

import nd.max.ui.design.MaxIllustrationKind
import nd.max.ui.design.maxIllustrationKind

/** Presentation only. Inheriting a domain does not change access, ownership or risk. */
val MaxDestination.illustrationKind: MaxIllustrationKind
    get() = when (this) {
        MaxDestination.Now, MaxDestination.DeviceInfo -> MaxIllustrationKind.Device
        MaxDestination.Apps, MaxDestination.UsageStats -> MaxIllustrationKind.Apps
        MaxDestination.Control -> MaxIllustrationKind.Controls
        MaxDestination.Settings, MaxDestination.About -> MaxIllustrationKind.System
        MaxDestination.MaxAi, MaxDestination.MaxLive, MaxDestination.MaxAiIntro -> MaxIllustrationKind.Intelligence
        MaxDestination.CpuHub -> MaxIllustrationKind.Cpu
        MaxDestination.GpuHub -> MaxIllustrationKind.Gpu
        MaxDestination.MemoryHub -> MaxIllustrationKind.Memory
        MaxDestination.DisplayHub -> MaxIllustrationKind.Display
        MaxDestination.ResponsivenessHub -> MaxIllustrationKind.Controls
        MaxDestination.ThermalHub -> MaxIllustrationKind.Thermal
        MaxDestination.PowerHub, MaxDestination.BatteryHistory -> MaxIllustrationKind.Power
        MaxDestination.StorageHub, MaxDestination.MaxBackup, MaxDestination.ConfigBackup -> MaxIllustrationKind.Storage
        MaxDestination.NetworkHub -> MaxIllustrationKind.Network
        MaxDestination.AudioHub -> MaxIllustrationKind.Audio
        MaxDestination.GameLobby, MaxDestination.EmulatorHub -> MaxIllustrationKind.Games
        MaxDestination.Permissions, MaxDestination.Privilege, MaxDestination.License -> MaxIllustrationKind.Security
        MaxDestination.DeviceLab -> MaxIllustrationKind.Sensors
        else -> parent?.illustrationKind ?: maxIllustrationKind(icon.name) ?: MaxIllustrationKind.Tools
    }
