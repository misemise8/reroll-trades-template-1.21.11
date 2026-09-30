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
- Saved targets and their detail/removal page. Three saved rows expose names, price bounds and locked/waiting states.
- Pagination, empty search, localized enchantment search, and back navigation.
- Wheel paging, fractional trackpad input, first/last-page clamping and no paging outside the panel.
- Long-name truncation remaining valid ASCII; names wrapping onto two lines without corrupt suffixes.
- Centering and widget bounds at 640×360, 320×240 and 292×237 GUI viewports. The panel is at most 320×232 and adapts to smaller windows.
- Twelve entries per page normally and nine at the narrow viewport.

Screenshots of the eight stages are stored in `versions/fabric-26.2/build/client-smoke-run/screenshots`; `client-smoke-result-ja_jp.json` and `client-smoke-result-en_us.json` record successful completion. The Gradle task fails if the corresponding report is absent or failed.
The fixture uses fixed data outside a world, with minimal bound item components for rendering. It is not an end-to-end multiplayer test, and it does not submit mutations through a real network connection. The server tests cover those controllers separately.

The screen wrapper handles background rendering on Minecraft 1.21.6 and newer; older versions draw it explicitly. Rebuilding is deferred until screen initialization to prevent duplicate pre-initialization widgets.

## Remaining runtime scope

Manual multiplayer interaction, all 17 client combinations, and third-party trade factories have not been verified.
The supported automated commands and report paths are documented in [trade-targets.md](trade-targets.md).

## Texture refinement

On 2026-09-30, the generated grain background was replaced by Minecraft's chest frame, fitted to the panel with fixed four-pixel corners and edges. Item icons now use matching inset slots. The vanilla texture is referenced at runtime rather than bundled.

All 17 targets passed `buildAndGather` after this change. The Japanese 26.2 Fabric client fixture passed again, including navigation, saved targets, wheel paging and centering at three viewport sizes. Its rendered screenshots were inspected for the frame and slots. The English client and server-controller results above are from the preceding 3.2.0 run; this refinement changes only client drawing.

## Secondary emerald input regression (2026-09-30)

The catalog and lock matcher now accept sales with emeralds in either input slot. Flint, cooked cod and cooked salmon are described before generation with a base emerald price of 1; bounds apply to that emerald input rather than the material count. Visible custom exchanges with secondary emerald costs are also included.

All 17 release distributions were rebuilt after the fix. The expanded server suite passed on these configurations:

| Target | Assertions | Result |
| --- | ---: | --- |
| fabric-1.21 | 2138 | passed |
| fabric-26.1 | 2110 | passed |
| fabric-26.2 | 2124 | passed |
| neoforge-1.21 | 2136 | passed |
| neoforge-26.1 | 2122 | passed |
| neoforge-26.2 | 2128 | passed |

Each suite samples 40 rerolls for each of the three exchange types, checks catalog membership and exact-price lock acquisition, and exercises editor payload/save, five partial rerolls with Undo, and removal for each exchange. Deterministic cases reject emerald prices outside the bounds and non-emerald barter. Assertion totals vary with the sampled exchange count.

These tests use Minecraft server registries, generation, controllers and loader attachments with captured outbound packets. The client drawing and remote multiplayer path were not rerun for this server-side fix.
