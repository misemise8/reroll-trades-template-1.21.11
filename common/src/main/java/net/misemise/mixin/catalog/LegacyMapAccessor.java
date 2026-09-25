//#if MC < 260100
package net.misemise.mixin.catalog;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

//#if MC >= 12111
@Mixin(targets = "net.minecraft.world.entity.npc.villager.VillagerTrades$TreasureMapForEmeralds")
//#else
//$$ @Mixin(targets = "net.minecraft.world.entity.npc.VillagerTrades$TreasureMapForEmeralds")
//#endif
public interface LegacyMapAccessor {
    @Accessor("emeraldCost") int rerollTrades$emeraldCost();
    @Accessor("displayName") String rerollTrades$displayName();
}
//#endif
