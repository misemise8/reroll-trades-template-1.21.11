# Reroll Trades

Reroll Trades adds a button and configurable keybind for regenerating a villager's offers without charging emeralds, experience, or items.

## 3.1.0 features

- Free server-authoritative rerolls
- A villager becomes permanently locked for everyone after any completed trade, including after restocks and restarts
- One-step Undo for the latest reroll in the same screen
- Server-side cooldown and optional per-player/per-villager limit
- Separate server and client JSON settings
- Optional Mod Menu integration on Fabric and a config screen in the NeoForge mod list
- English and Japanese UI

The buttons only appear for supported villagers after the server confirms the current screen. Wandering traders and other merchant screens are left unchanged.

## Configuration

Server settings are stored in `config/reroll-trades-server.json`. Client display and effect settings are stored in `config/reroll-trades-client.json`.

Use `/rerolltrades reload` to reload server settings and `/rerolltrades status` to inspect the active values. Both commands require permission level 2.

An existing `config/reroll-trades.json` is read once when the corresponding new file does not yet exist. The legacy file is never deleted automatically.

See [`docs/config`](docs/config) for complete examples.

## Building all supported versions

Use JDK 25 to run Gradle, with JDK 21 also installed for NeoForge 1.21 toolchain tasks.
The 1.21 targets emit Java 21 bytecode; 26.x targets emit Java 25 bytecode.
All 17 Fabric and NeoForge targets are managed on this branch, with one shared implementation.

```powershell
# Build all 17 distributions, gathered into build/libs
.\gradlew.bat buildAndGather

# Build one version without switching Git branches
.\gradlew.bat "-Ptarget=fabric-26.2" buildAndGather
.\gradlew.bat "-Ptarget=neoforge-1.21.4" buildAndGather

# Run a selected development client
.\gradlew.bat "-Ptarget=fabric-26.2" :fabric-26.2:runClient

# List targets and validate the preprocessor
.\gradlew.bat listTargets
.\gradlew.bat -p buildSrc test
```

On macOS/Linux, replace `.\gradlew.bat` with `./gradlew`.
`-Ptarget` also accepts a comma-separated list. Quote the argument in PowerShell.
`buildAndGather` synchronizes `build/libs` to the selected targets; each target also keeps its jars in `versions/<target>/build/libs`.

Edit gameplay, UI, configuration, packets and mixins in `common/src/main`.
Loader entrypoints and platform hooks live in `fabric/src/main` and `neoforge/src/main`.
Minecraft API differences use `//#if MC >= ...` comments in those same files.
The version/dependency catalog is [`versions/targets.json`](versions/targets.json).

See [the multiversion guide](docs/multiversion.md) for the source format, target maintenance and validation limits.
