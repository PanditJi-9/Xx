# Features

MaxManager's surface is 61 screens across ten control domains, plus a small set of tools. This page
explains what each part is *for* — not just that it exists.

A rule that shapes everything below: **a control that your device does not expose is not shown as a
switch that does nothing.** If MaxManager cannot read or write an interface, the screen says so and
explains which of the seven capability states applies ([Max Atlas](max-atlas.md)).

---

## Control domains

### CPU — *"Cores, governor, vendor boost and kernel preferences"*

| Screen | What it gives you |
| --- | --- |
| **Cores grid** | Every cluster and core with its live frequency, and the ability to pin a floor or a ceiling per cluster. Cluster names are read from the platform, not guessed. |
| **Governors** | The available governors per policy with the ability to select one. Choices are validated against what the kernel actually advertises. |
| **Property tweaks** | Reviewer-managed system properties that affect scheduling and power, applied in a form you can read before and after. |

### GPU — *"GPU frequencies and graphics-specific vendor parameters"*

**GPU Reality Studio** discovers the driver's advertised frequency table, its unit (Hz vs MHz is a real
mismatch between vendors), the writable controls, and a restoration baseline — then presents only what
it could verify. On MediaTek devices the OPP table is read as a whole table rather than a first-match,
because the first match can be a runtime-narrowed view.

### Memory — *"Compression, VM behaviour and swappiness"*

**ZRAM manager** covers the compression device: size, algorithm, and the VM parameters around it. The
engine also reads **memory stall pressure** (`/proc/pressure/memory`) rather than fill percentage,
because a nearly-full-but-idle device and a stalling device need opposite responses.

### Display — *"Refresh rate, colour and canvas scale"*

**Display studio** and **Resolution** (including per-app downscaling) — always validated against what the
platform advertises, with the current mode readable.

Night light is the platform's own switch, not a private filter: the screen, the Quick Settings tile and
the **schedule** all write `secure/night_display_activated` and read it back, so they cannot disagree.
The schedule is a start/end hour window that may cross midnight, applied on a single batched alarm —
the system may fire it a few minutes late, and the description says so instead of implying stopwatch
precision.

### Responsiveness — *"Touch, frame pacing and scheduling latency"*

Touch boost, **FPS GO** (frame-rate governance), and **FAS** (frame-aware scheduling). The **FPS
overlay** — a live HUD over other apps — lives with the tools rather than here, because it is something
you look at, not something you tune.

### Thermal — *"Temperatures, throttling and thermal parameters"*

Live zone temperatures, the throttling picture, and the parameters that govern it. The `thermalcore`
daemon (see [thermal.md](thermal.md)) is the part that acts: it learns how this device responds, and
predicts. **Thermal trip points are on the never-touch list** — they are the hardware's own protection,
and no profile may override them.

A **session trend** draws the hottest readable zone once every three seconds for as long as the screen
stays open — five minutes of window, scaled to the range actually measured rather than to a fixed scale,
with peak and low shown beside it. It lives in memory only: leaving the screen discards it, and nothing
is written to storage on its own. **Export session** writes exactly the samples on screen — elapsed
time, zone, temperature — to a temporary CSV in the cache and hands it to the share sheet, so the file
matches the curve the user is looking at.

### Power — *"Charging, bypass, sleep and battery health"*

Charging and battery health in one screen, **bypass charging** (running from the cable without pushing
current through the pack) with a dedicated **status check**, and **Doze**. Before a backup is applied, the restore dialog lists the saved settings that actually differ from this device (old → new), so the confirm button is a decision rather than a guess; keys that exist only on the device are not counted, because a restore writes and never deletes. The app list is measured the same way — lines added and removed — because it is replaced as a whole file rather than merged. Preferences are merged rather than wiped, so their line reports how many keys actually change. The restore that runs after a reboot now reports the last time it ran and how many fast reboots
the boot guard has counted, with a warning at two — read-only, and deliberately without a reset
button, because a zeroed counter would hide the reboot it was counted for. Turning the dangerous automatic apply back on asks once, names what will be written, and records that you accepted it — the responsibility for the change is yours, and the app says so instead of hiding the switch. The nodes it changes are read first, so the previous values can be written back with one tap
— an undo of the last restore, not a history of every run.

**Battery history** keeps a local timeline of level, voltage and temperature — one point every fifteen
minutes, off until you switch it on. The level curve and a separate battery-temperature trend are
drawn only from points the platform actually recorded, so gaps stay gaps (the line breaks instead of
inventing a value), and the discharge rate is computed from discharging points alone rather than
modelled. Each new point also records whether the screen was lit, so the drain is reported three ways:
overall, screen-on only and screen-off only — a figure that mixes a busy afternoon with a sleeping
night describes neither. The visible window can be exported as one CSV file (ISO timestamp plus the
raw epoch, empty fields where the platform reported nothing) and handed to an app of your choice
through the system share sheet — nothing is uploaded. Clearing the history deletes the local file and
nothing else.

### Storage & compiler — *"Compilation mode and storage health"*

**Dex2oat** compilation mode and storage health. Storage scanning is done in the native layer in
batches, not by shelling out per directory.

### Network — *"Traffic scheduling and link state"*

Network detail (what the link is doing right now) and the scheduler controls. **Usage stats** reads
Android's own usage database for foreground time per app, and the same screen can switch to **network
usage per app** — Wi-Fi and mobile bytes split, with totals. A transport the device does not report is
shown as unavailable, not as zero; when the platform returns only aggregate counters, the screen says so
and hides the per-app list instead of inventing one.

The time view also charts **daily observed usage**, regardless of the app-list period, with a choice of
a 7-day or 30-day window — one read covers 30 days, and the week is taken from the end so the comparison
below does not move when the window changes. It unions
overlapping foreground activity sessions and splits them at local midnight, including daylight-saving
changes. Bar heights are relative to that week's busiest day; screen-reader descriptions include each
date and duration. Android may retain incomplete events, so this is an estimate that can differ from the
app totals. A read failure is shown as unavailable, and no counted events is not proof of no usage.
In the 30-day window only every fifth day carries its date so the bars do not shrink to fit labels.
Below the bars one line states the change from the 7 days before them — a duration and a percentage, or
plainly that nothing was counted earlier, since a percentage against zero would be invented rather than
measured. Reading runs off the UI thread and needs no new permission, root access, or network request.

**Wi-Fi channels** in Device Lab read the connected channel, how many networks the last scan saw, and
which channel they crowd. Android returns scan results only to an app holding the location permission, so
the section shows why it is empty and asks for the permission on a tap — it never asks for a position,
never stores one, and never recommends a channel it has not measured.


**Link quality** is measured only on request: one name lookup and four TCP connects to a named host,
reported as fastest, average and jitter, with failed attempts counted and named instead of drawn as
zero. The screen states that these are TCP handshakes rather than ICMP ping, that a repeated lookup is
cached, and that the bandwidth figures are the platform's own link estimate — nothing is downloaded to
produce a number.

### Audio — *"Output devices, streams and declared effects"*

Read-only today, and deliberately so: the hub reports what the platform declares for the primary
output (sample rate, frames per buffer), every endpoint the device announces — outputs first, then
inputs — and the effects the audio engine reports. An empty list is a reading (*"no effects"* — the
device announced none), while an unreadable one is reported as unreadable rather than as zero.

---

## Apps

| Screen | What it gives you |
| --- | --- |
| **Apps** | Everything installed, searchable, with per-app policy entry points. |
| **App settings** (per app) | Per-app performance, refresh rate and resolution, thermal ceiling, and its own quick actions — the same knobs as the global screens, scoped to one package. |
| **Debloat & freeze** | Disable or freeze apps you do not want running, reversibly. The first freeze asks once, names what can break (launcher, keyboard, services, this app), and records that you accepted it; unfreezing never asks. |

Per-app work is tracked per knob, with a recovery store, so a session that ends badly does not leave a
package in an unexplained state.

---

## Tools

| Tool | What it gives you |
| --- | --- |
| **Process manager** | What is running, what it costs, and what can be stopped. |
| **Device lab** | Interactive hardware checks on the device itself: screen colours, multi-touch, hardware keys, vibration, speaker tone and earpiece routing, live audio outputs, microphone level, flashlight, live sensors, charging state, and what the panel reports about itself — current mode, supported resolutions and refresh rates, HDR types, peak luminance and wide colour gamut. The microphone test asks for the permission on the spot, shows a live dBFS level and peak, and keeps nothing: samples are discarded after each reading. Display capabilities are a plain platform read; nothing the system does not report is shown as zero. The video-decoder read-out lists what the device reports it can decode and the largest frame per format, taken from the platform codec list — nothing is played or downloaded. |
| **Usage stats** | Foreground time per app from Android's usage database, and network bytes per app per period — read-only, one access switch. |
| **FPS overlay** | A live frame-rate and load HUD drawn over other apps, with optional battery level and network rate fields. Up to three independent monitors can float at once, each with its own fields, shape and position, all fed by one reading loop. |
| **Quick settings tiles** | Bypass charging, profile, clean status bar, battery-history recording, high-brightness mode, night light and the FPS overlay, each reading its state back before it claims to have changed anything. The **freeze manager** tile is an entrance, not a switch: it opens the picker so you choose the package yourself, and it never freezes anything on its own or guesses a target — the once-only confirmation still lives on the screen. Long-pressing the tile opens the same screen, and a failed launch says so in the tile subtitle instead of pretending it worked. |
| **Log viewer** | The device log, parsed into events with a **glossary written into the log file itself** — the file explains its own fields. |
| **Max Backup** | Back up apps and check their integrity; restore when you need to. |
| **Permissions** | Declared permissions vs what the platform actually allows (`AppOps`), in one place, because "declared" and "granted" differ. A manifest summary above the list reads what the package itself declares: version and version code, minimum and target SDK, install location, component counts (activities, services, receivers, providers) with the exported ones called out, split APKs, and whether the build is debuggable. Anything the package manager does not report is shown as unknown, never as zero. |
| **SetEdit** | Direct access to the settings database for values with no UI. |
| **Activity Launcher** | Launch exported and internal activities. |
| **File manager** | A file browser for the paths this kind of work touches, with bookmarks and history. |
| **Backup & restore** | Save your MaxManager configuration to a file, and restore it. Tweaks require the same chipset; per-app settings restore anywhere. |
| **Config backup** | The file-level half of the same idea, written where you choose. |

---

## The intelligence layer

| System | What it does for you | Read more |
| --- | --- | --- |
| **Max Atlas** | Makes every feature work on *your* device — discovering which interfaces exist, which routes reach them, and remembering what actually succeeded | [max-atlas.md](max-atlas.md) |
| **Max AI** | Decides what should change now: an objective, the smallest sufficient intervention, safety first, and a real measurement before any reward | [max-ai.md](max-ai.md) |

Both are **off by default**. Nothing self-enables, and no control moves without either your action or an
engine you switched on.

---

## Diagnostics and support

| Feature | What it gives you |
| --- | --- |
| **Diagnostic centre** | Live memory of what is wrong right now, instead of making you read log files. |
| **Route health** | For any control: *why* it can or cannot be activated on this device, from facts. |
| **Excluded ledger** | Every capability this app deliberately does **not** ship, in the diagnostic centre, with the reason and what would happen if it were forced — in three separate groups, because the reasons differ: refused by our own policy (voltages, SoC tunables under a custom kernel, sysconfig/Doze edits, kernel flashing, competitive benchmark ordering, disabling telemetry without measurement), unsupported by this platform (wakelocks, LMK, stune), and shipped but dangerous, which sits behind a one-time recorded acknowledgement rather than a hidden switch. It is a list, not a control: no switch, no toggle, nothing to tap — an entry that looked operable but did nothing would be worse than saying why it is absent. |
| **Kernel node catalog** | The diagnostic centre counts the device nodes this app actually knows — read here, not assumed present: how many are readable now, how many do not exist on this device, and how many exist but the system refuses to read. It is read-only and it is the honest precondition for any later recipe writing: a node that is absent cannot be configured, and one we cannot read is not "supported". The card has no switch, because existence is a necessary condition, never a permission. |
| **Recipe decisions** | A recipe is text, and it is decided before anything is touched: every line is resolved to a key, a path and a privilege from the control catalog, or the whole recipe is refused with the reason (unknown key, protected entry, no write privilege, no candidate path, or a value that does not fit its declared kind). A partial application is never offered, because half a recipe is a state nobody chose. The decider opens no file and writes nothing. |
| **Recipe execution** | A recipe is applied one step at a time, and every step is read back before it counts: the old value is captured first, the write is verified against what the node reports, and a mismatch restores the old value. If any step fails, the steps already applied are undone in reverse order, and a recipe containing one unusable step is not started at all — so a half-applied recipe is never left on the device. The runner opens no file and knows no privilege; the write path and the arbiter are supplied to it. |
| **Device blueprint** | A read-only snapshot included in every diagnostic export, so a report can be reproduced. |
| **Support report** | A minimized artifact you choose to send at the end of the safe stages — not a screenshot. |
| **Atlas doctor** | Replays a recorded device and checks the report reproduces (the maintainer's loop). |

---

## Interface and language

<p align="center"><img src="assets/locales.svg" width="100%" alt="84 languages, and right-to-left is first class"></p>


- **Light and dark**, with a themable key colour and a custom palette screen.
- **84 locales** plus English; the picker follows the system language without a restart.
- **RTL is enforced** by a gate, not by hope — Arabic, Farsi, Hebrew and Urdu ship.
- A **design system** (`ui/design/`) rather than per-screen improvisation, which is why the screens
  share one rhythm and one set of tokens.
