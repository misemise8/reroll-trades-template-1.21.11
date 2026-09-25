# 3.2.0 validation — 2026-09-25

## Build and runtime coverage

All 17 configured distributions compile and package as version 3.2.0.
Server integration checks passed on these six configurations:

| Target | Assertions | Result |
| --- | ---: | --- |
| fabric-1.21 | 1913 | passed |
| fabric-1.21.11 | 1913 | passed |
| fabric-26.1 | 1913 | passed |
| fabric-26.2 | 1913 | passed |
| neoforge-1.21 | 1913 | passed |
| neoforge-26.2 | 1913 | passed |

Each run samples 400 librarian rerolls, inspecting both selling and buying offers, and also runs deterministic price boundaries and controller assertions. All six configurations passed 1,913 assertions after the icon GUI and price-bound update.

Covered behavior:

- Every vanilla profession's catalog at all five levels, including generated base prices.
- Mending's 10–38 base range, inclusive lower/upper bounds, exact enchantment levels, and exclusion of unavailable Sharpness V equipment.
- Paper buying targets with a base input quantity of 24, separate buying/selling matching, server rejection of inverted or nonpositive bounds, and migration of saved targets without the new fields.
- Overlapping rules receiving distinct eligible slots; existing locks remaining stable.
- Thirty partial rerolls and Undo operations preserving the locked slot and restoring the other slot.
- A waiting target locking when generated, and Undo preserving the newly acquired lock.
- Rejection of stale revisions and wrong container IDs; target removal releasing its slot.
- Saved entity attachments restoring rules and locks on both loaders; packet codec round trips.
- Blocking rerolls after a completed trade and when all slots are fixed.

The tests run inside Minecraft servers with a synthetic player and captured outbound packets. They do not simulate a real remote connection.

## Client rendering

The 26.2 Fabric development client renders the actual centered icon editor at 1280×720 with GUI scale 2. Japanese and English runs exercise:

- Item → enchantment/type → level → price, including direct item-to-price and single-level skips.
- Upper-bound input, blank bounds, inverted bounds, zero, and nonnumeric rejection.
- Saved targets and their detail/removal page.
- Pagination, empty search, localized enchantment search, and back navigation.
- A 320×240 GUI viewport (the 224×196 panel fits without clipping).

Screenshots of the eight stages are stored in `versions/fabric-26.2/build/client-smoke-run/screenshots`; `client-smoke-result-ja_jp.json` and `client-smoke-result-en_us.json` record successful completion. The Gradle task fails if the corresponding report is absent or failed.
The fixture uses fixed data outside a world, with minimal bound item components for rendering. It is not an end-to-end multiplayer test, and it does not submit mutations through a real network connection. The server tests cover those controllers separately.

The screen wrapper handles background rendering on Minecraft 1.21.6 and newer; older versions draw it explicitly. Rebuilding is deferred until screen initialization to prevent duplicate pre-initialization widgets.

## Remaining runtime scope

Manual multiplayer interaction, all 17 client combinations, and third-party trade factories have not been verified.
The supported automated commands and report paths are documented in [trade-targets.md](trade-targets.md).
