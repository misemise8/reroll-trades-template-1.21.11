# 3.2.0 Trade targets

## Using targets

1. Open a villager's trading screen and press **L**.
2. Search the current profession/level's sale catalog. Enchanted books and equipment have separate entries for their enchantment levels.
3. Select an entry and enter the maximum emerald cost, such as **10**.
4. Save the target. An existing matching offer locks immediately; otherwise it waits for a matching manual reroll.
5. Continue rerolling the other slots. **Saved targets** shows each target's waiting state or its locked slot number.
6. Remove a target to release its slot. Saving the same target again updates its price condition and reevaluates its lock.

The displayed price range is the normal generated base price, before player discounts and demand.
For example, ordinary Mending books range from 10 to 38 emeralds, and a limit of 10 includes exactly 10.
Matching uses the current emerald cost, so a discounted offer can meet a limit below the base range.
After locking, ordinary discounts and demand remain active even if the price exceeds the saved limit.
For enchanted equipment, the displayed price range covers the item's possible rolls, not a conditional range for one enchantment combination.
Additional trade ingredients and the result quantity are unchanged.

Each target holds one slot. Overlapping new targets are assigned to different matching slots where possible; existing locked slots stay in place.
There are at most 16 targets per villager. They are shared by players and saved with the villager, so any player who can edit that villager's targets can remove them.
Changing profession resets the targets. A changed offer releases its obsolete lock.
Undo restores only unlocked slots, including when the most recent reroll found a new target.
When every offer is fixed, remove a target to enable rerolling again.
Once any trade is completed, the existing permanent restriction disables rerolls and target editing.

## Catalog scope

- All 13 vanilla trading professions are supported. The list follows the current profession, level and enabled trade data, including biome restrictions.
- Buying items from the player (emeralds as the result) is outside the sale catalog.
- Books expose enchantment and exact level. Standard equipment trades exclude levels outside their enchantment power range.
- Maps are described without locating structures or creating maps just to display the list.
- Random colors and potion variants use the item target where the factory does not expose a fixed variant. Extra effects on equipment may coexist with the selected enchantment.
- Unsupported custom factories can be selected once their offers are visible. Their price range is shown as unknown.
- Some custom loot modifiers cannot be analyzed exactly; their base price range is also unknown.

## Development verification

The opt-in test mod is not part of release jars. Run with JDK 25 for Gradle and JDK 21 installed for 1.21 runtimes:

```powershell
.\gradlew.bat "-Ptarget=fabric-26.2" -PtradeSmokeTest runSmoke
.\gradlew.bat "-Ptarget=neoforge-1.21,neoforge-26.2" -PtradeSmokeTest runSmoke
.\gradlew.bat "-Ptarget=fabric-26.2" -PtradeSmokeTest :fabric-26.2:runClientSmoke
.\gradlew.bat buildAndGather
```

Server tests use real Minecraft registries, trade generation, mixins, loader attachments, the target controller and the reroll controller.
A synthetic player captures outbound packets; this does not exercise a remote client's connection.
Tests cover all professions and levels, generated librarian price bounds, exact enchantment/price matching, overlapping targets, lock acquisition, partial rerolls, Undo, stale requests, menu validation, attachment save/load and completed-trade restrictions.
The test run binds only to loopback on a dynamically assigned port and writes `versions/<target>/build/smoke-run/trade-smoke-result.json`.
The task fails if the report is missing or reports a failed assertion.

The client fixture renders the actual editor with fixed data in Japanese and saves screenshots in `versions/fabric-26.2/build/client-smoke-run/screenshots`.
It checks screen loading and layout separately from gameplay. Manual multiplayer interaction and modpack-specific trade factories still require in-game testing.
