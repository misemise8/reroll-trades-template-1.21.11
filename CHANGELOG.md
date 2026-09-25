# Changelog

## 3.2.0

- Added a compact centered icon picker for all villager professions: item → type/enchantment → level → price, skipping unnecessary steps. Labels follow Minecraft's language setting.
- Show generated price ranges with optional inclusive minimum/maximum conditions. Include villager buying trades with clearly labeled required item quantities.
- Keep saved targets, lock status and removal accessible through the book-and-quill icon.
- Automatically lock one matching offer per target and preserve those slots across manual rerolls and Undo. Normal discounts and demand changes remain active.
- Persist targets and locked slots on the villager, shared across players; allow removal from the editor and clear obsolete targets on profession changes.
- Validate target selections, prices, menu ownership and revisions on the server. Stop rerolls when every slot is locked and retain the completed-trade restriction.
- Describe legacy trade factories and modern trade data without generating map offers just to populate the catalog.
- Refresh merchant output slots and player discounts after rerolls and restore offers if generation fails or the locked layout changes.
- Update the networking protocol for the new screen state; use matching 3.2.0 clients and servers.
- Add development-only server integration tests for both loaders and a 26.2 Fabric UI rendering fixture.

## 3.1.0

- Changed traded-villager locking from per-player storage to a global lock detected from `uses > 0` and persisted across restocks and restarts.
- Added one-step, 30-second Undo for the latest reroll in the same villager screen.
- Added a server-side reroll cooldown and an optional persistent per-player/per-villager limit.
- Added server-authoritative screen state so unsupported merchants never show reroll controls.
- Split settings into server and client JSON files with safe legacy migration and malformed-JSON fallback.
- Added `/rerolltrades reload` and `/rerolltrades status` for permission level 2 operators.
- Added client settings screens, optional Fabric Mod Menu integration, sounds, richer tooltips, and Japanese/English text.
- Standardized Fabric and NeoForge artifact names as `reroll-trades-<loader>-3.1.0-<minecraft-version>.jar`.
