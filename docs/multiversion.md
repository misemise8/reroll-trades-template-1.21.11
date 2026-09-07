# Multiversion development

## Editable sources

The shared implementation is in `common/src/main/java`. There is one `RerollController`, one pair of configuration classes and one set of screen mixins.
Fabric and NeoForge each have five adapter/entrypoint classes in their corresponding `src/main/java` directory.
Translations, icons and mixin declarations are shared resources; loader metadata and service registrations remain loader-specific.

Each target is a Gradle subproject named `<loader>-<minecraft>`.
`versions/targets.json` is the only dependency catalog, and `gradle/target.gradle` is the shared build definition.
The old branches remain available as historical snapshots. Normal development no longer requires switching them.

## Minecraft API differences

The Java comment syntax follows the approach used by [sakura-ryoko/fabric-mod-template](https://github.com/sakura-ryoko/fabric-mod-template) and the [ReplayMod/Fallen-Breath preprocessor](https://github.com/Fallen-Breath/preprocessor).
This repository uses a small local Gradle task for MC-only conditional expansion, shared by Loom and ModDevGradle.
It does not use the template's remapping graph, pattern annotations or `setCoreVersion` source rewriting.
The newest code is ordinary Java; older alternatives use `//$$` comments:

```java
//#if MC >= 12111
import net.minecraft.world.entity.npc.villager.Villager;
//#else
//$$ import net.minecraft.world.entity.npc.Villager;
//#endif
```

The numeric encoding is `major * 10000 + minor * 100 + patch`: 1.21 is `12100`, 1.21.11 is `12111`, 26.1 is `260100`, and 26.2 is `260200`.
Supported directives are `//#if`, `//#elseif`, `//#else`, `//#endif`; conditions compare `MC` to integers with `>=`, `>`, `<=`, `<`, `==`, `!=`, `&&` or `||`.
Nested conditions are supported. Parentheses and other variables are not supported.

Gradle generates compilable Java into `versions/<target>/build/generated/sources/main/java` before compilation.
Do not edit generated files. Edit the corresponding common or loader source instead.
Blank lines preserve the original source line numbers for compiler diagnostics and stack traces.
Removed input files are removed from generated output on the next build.

## Build and IDE use

Run Gradle with JDK 25 and also install JDK 21 for NeoForge 1.21 toolchain tasks. CI installs both.
Set the IntelliJ Gradle JVM to JDK 25 and import the root `settings.gradle`.
The default selection contains all 17 subprojects. Each has its own mapped/deobfuscated Minecraft dependencies and development run tasks.
For a lighter import, add `target=fabric-26.2` (or another target ID) to your local Gradle user properties, then reload Gradle.
An explicit `-Ptarget=all` overrides that local selection when building releases.
The default IDE target is `fabric-26.2`: it owns the editable common/Fabric sources and provides their Minecraft classpath.
Use the local Gradle property `ide_target=neoforge-26.2` when editing NeoForge adapters with the latest NeoForge classpath instead.
The ordinary, uncommented Java in shared sources corresponds to 26.2.
Other targets expose their generated source to the IDE for API inspection; durable edits still belong in the shared source directories.
Compilers always read generated sources, even for the IDE target.
The IDE target's source jar retains conditional comments; other source jars contain the expanded target code.

```powershell
.\gradlew.bat "-Ptarget=all" buildAndGather
.\gradlew.bat "-Ptarget=fabric-1.21,neoforge-1.21" buildAndGather
.\gradlew.bat "-Ptarget=neoforge-26.2" :neoforge-26.2:runClient
.\gradlew.bat -p buildSrc test
```

`buildAndGather` includes only distribution jars, excluding source/development jars.
The root `build/libs` directory is synchronized to the current selection, so it cannot silently retain jars from an older selection.
Source jars remain in `versions/<target>/build/libs`.
Jars are still specific to a Minecraft version and loader. Shared source does not imply a universal binary.

## Adding a target

1. Add a target to `versions/targets.json` with its Minecraft version, integer encoding, Java level, supported range and loader dependencies.
2. Add `versions/<target>/.gitkeep` so the Gradle project directory exists in a clean checkout.
3. Build the target with `-Ptarget=<target>` and resolve API differences in the shared source using the smallest useful conditional block.
4. Build all targets to catch changes affecting other versions.
5. Verify client and dedicated-server behavior in the actual game before publishing.

Keep `mod_version` in the root `gradle.properties`; it applies to every target.
Preserve the loader-specific mod IDs and attachment identifiers when adding or changing adapters, because existing worlds contain those identifiers.

## Migration scope and runtime checks

The migration preserves the 3.1.0 gameplay model and dependency versions from the existing 17 branches.
Loom is pinned to 1.15.5 and ModDevGradle to 2.0.141 for the unified build.
The NeoForge 1.21.4/1.21.5 `updateTrades` invoker now uses the correct no-argument signature from the matching Minecraft API.
Fabric platform services initialize eagerly so data attachments are registered during mod initialization.
Other findings in the earlier 3.1.0 audit are separate follow-up work.

Compilation and jar inspection do not verify in-game mixin application, UI input, networking or saved-world behavior.
Before release, check each supported game range on the client and a dedicated server: open villager trades, reroll, Undo, complete a trade, restock, reconnect and restart the world; also check the config screen, keybind and command reload.
