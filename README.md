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
