# RedLab Ecosystem — foundation build

A **separate** Gradle build from MaxManager's root build on purpose: the ecosystem is its own
project (`docs/SYSTEM_ARCHITECTURE.md`), and nothing here may change MaxManager's build.

```
redlab/
├── settings.gradle.kts     rootProject "RedLabEcosystem", includes :core
├── build.gradle.kts        Kotlin 2.3.10, matching versions already used in this repo
└── core/                   dev.redlab.core — plain JVM library, no Android SDK needed
```

## What `:core` is

The contract every module will depend on (`docs/CAPABILITY_REGISTRY.md`,
`docs/HARDWARE_CONTROL_SAFETY.md`):

| Type | Rule it enforces |
| --- | --- |
| `CapabilityState` | four states; `UNKNOWN` is the default, never a guessed `AVAILABLE` |
| `Capability` | `AVAILABLE` without evidence and `UNSUPPORTED` without a reason are unrepresentable |
| `CapabilityRegistry` | one owner per capability; recording an undeclared capability is refused |
| `HardwareControlArbiter` | one holder per control, `SAFETY` absolute, equal priority denied, stale tokens cannot release |
| `GuardedNode` | read before write, only values the kernel lists, restore the **first** value seen |

## Build and test

CI: `.github/workflows/redlab-foundation.yml` runs `./gradlew :core:test`.

Locally (needs JDK 17 only — no Android SDK):

```sh
cd redlab
./gradlew :core:test
```

## What a green run means — and does not

It means the rules compile and their unit tests pass. It does **not** mean any capability is
`AVAILABLE` on any device: no probe has run on hardware yet, and the phase-5 acceptance item
"capability report on two devices" stays open until it is measured on real devices.
