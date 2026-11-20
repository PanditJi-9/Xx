# MAXMANAGER_PORT_MAP — the unlock order for the ported engine

Generated from the real source tree: for every staged file, the MaxManager symbols it still
needs that are neither compiled here nor in the module. Rule: a file moves from `staging/`
to `src/` only when its row is empty and `:hardware:test` is green.

| staged file | still needs |
| --- | --- |
| `CatalogControlIndex` | `AssetVault`, `HardwareControlKey` |
| `CpuHardwareBackend` | `PlatformCeilingAuthority`, `RootFileAccess` |
| `CpuStateDistribution` | `CpuHardwareBackend` |
| `GpuCeilingPolicy` | `HardwareVerification`, `ThermalCeilingRouter` |
| `GpuFrequencyWindow` | `GpuHardwareBackend` |
| `GpuHardwareBackend` | `CpuHardwareBackend`, `CpuStateDistribution`, `GpuCeilingPolicy`, `GpuDiscoverySnapshot`, `HardwareControlArbiter` … (+6) |
| `HardwareCapability` | `CapabilityMatrixCard`, `MemoryUfsHardwareBackend` |
| `HardwareControlArbiter` | `AppMonitor`, `HardwareVerification`, `HealthLedger`, `SharedHardwareOwnershipStore` |
| `HardwareControlKey` | `AudioStreamBackend`, `CatalogControlIndex`, `MemoryUfsHardwareBackend` |
| `HardwareDeviceOverlay` | `HardwareOverlayStore`, `MaxManagerApplication`, `MemoryUfsHardwareBackend` |
| `HardwareOverlayStore` | `HardwareDeviceOverlay` |
| `HardwareRepairExecutor` | `HardwareControlArbiter` |
| `HardwareVerification` | `GpuCeilingPolicy`, `HardwareControlArbiter` |
| `HealthModel` | `HardwareControlArbiter`, `HardwareControlKey`, `HealthLedger`, `StatsRules`, `WriteVerification` |
| `MemoryUfsHardwareBackend` | `GpuHardwareBackend`, `HardwareControlArbiter`, `HardwareDeviceOverlay`, `HardwareVerification`, `MemoryUfsModel` … (+1) |
| `MtkGpuFixedIndex` | `GpuCeilingPolicy`, `GpuHardwareBackend`, `MtkGpuOppTable` |
| `MtkGpuOppTable` | `GpuHardwareBackend` |
| `PerAppControlRegistry` | `AtlasAdaptiveExecutor`, `HardwareControlArbiter`, `HardwareRepairExecutor`, `HardwareVerification`, `RouteEvidenceFacts` … (+1) |
| `PerAppRecoveryStore` | `AppMonitor`, `RootFileAccess` |
| `PlatformCeilingAuthority` | `CpuHardwareBackend`, `GpuHardwareBackend`, `RootFileAccess`, `WriteVerification` |
| `RouteEvidenceFacts` | `AtlasRoutePlanner`, `GpuHardwareBackend`, `PerAppControlRegistry` |
| `WriteReceipt` | `DaemonIpc`, `RootFileAccess`, `WriteVerification` |
| `WriteVerification` | `RootFileAccess`, `TouchBoostViewModel` |

## Biggest unlockers

Ported in this order, each one unblocks the listed number of files:

| file | unblocks |
| --- | --- |
| `RootFileAccess` | 7 |
| `GpuHardwareBackend` | 6 |
| `HardwareControlArbiter` | 6 |
| `HardwareVerification` | 5 |
| `MemoryUfsHardwareBackend` | 4 |
| `CpuHardwareBackend` | 3 |
| `GpuCeilingPolicy` | 3 |
| `WriteVerification` | 3 |
| `HardwareControlKey` | 2 |
| `PlatformCeilingAuthority` | 2 |
| `ThermalCeilingRouter` | 2 |
| `MtkGpuOppTable` | 2 |

## Compiled today

- `ControlOwnership`
- `DriftGuard`
- `ManualControlLocks`
- `VerifiedControl`
