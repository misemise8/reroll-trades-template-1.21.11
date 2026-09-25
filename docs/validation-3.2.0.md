# 3.2.0 validation — 2026-09-25

## Build and runtime coverage

All 17 configured distributions compile and package as version 3.2.0.
Server integration checks passed on these six configurations:

| Target | Assertions | Result |
| --- | ---: | --- |
| fabric-1.21 | 1360 | passed |
| fabric-1.21.11 | 1358 | passed |
| fabric-26.1 | 1368 | passed |
| fabric-26.2 | 1370 | passed |
| neoforge-1.21 | 1374 | passed |
| neoforge-26.2 | 1348 | passed |

Counts vary because the tests inspect actual randomly generated librarian offers. Each run samples 400 rerolls; it also runs deterministic price boundaries and controller assertions.

Covered behavior:

- Every vanilla profession's catalog at all five levels, including generated base prices.
- Mending's 10–38 base range, an inclusive maximum of 10, exact enchantment levels, and exclusion of unavailable Sharpness V equipment.
- Overlapping rules receiving distinct eligible slots; existing locks remaining stable.
- Thirty partial rerolls and Undo operations preserving the locked slot and restoring the other slot.
- A waiting target locking when generated, and Undo preserving the newly acquired lock.
- Rejection of stale revisions and wrong container IDs; target removal releasing its slot.
- Saved entity attachments restoring rules and locks on both loaders; packet codec round trips.
- Blocking rerolls after a completed trade and when all slots are fixed.

The tests run inside Minecraft servers with a synthetic player and captured outbound packets. They do not simulate a real remote connection.

## Client rendering

The 26.2 Fabric development client rendered both editor tabs in Japanese at 1280×720 with GUI scale 2. Screenshots were inspected for readable labels, price ranges, price input, lock status and buttons.
The fixture uses fixed data outside a world; it is not an end-to-end multiplayer test.
The client check exposed a duplicate background blur crash on newer Minecraft versions. The editor now draws its own background only before 1.21.6, where the screen wrapper does not draw it automatically.

## Remaining runtime scope

Manual multiplayer interaction, all 17 client combinations, and third-party trade factories have not been verified.
The supported automated commands and report paths are documented in [trade-targets.md](trade-targets.md).
