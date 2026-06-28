# Changelog

## 3.1.0

- Changed traded-villager locking from per-player storage to a global lock detected from `uses > 0` and persisted across restocks and restarts.
- Added one-step, 30-second Undo for the latest reroll in the same villager screen.
- Added a server-side reroll cooldown and an optional persistent per-player/per-villager limit.
- Added server-authoritative screen state so unsupported merchants never show reroll controls.
- Split settings into server and client JSON files with safe legacy migration and malformed-JSON fallback.
- Added `/rerolltrades reload` and `/rerolltrades status` for permission level 2 operators.
- Added client settings screens, optional Fabric Mod Menu integration, sounds, richer tooltips, and Japanese/English text.
- Standardized Fabric and NeoForge artifact names as `reroll-trades-<loader>-3.1.0-<minecraft-version>.jar`.
