//#if MC < 260100
package net.misemise.mixin.catalog;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

//#if MC >= 12111
@Mixin(targets = "net.minecraft.world.entity.npc.villager.VillagerTrades$TippedArrowForItemsAndEmeralds")
//#else
//$$ @Mixin(targets = "net.minecraft.world.entity.npc.VillagerTrades$TippedArrowForItemsAndEmeralds")
//#endif
public interface LegacyArrowAccessor {
    @Accessor("toItem") ItemStack rerollTrades$toItem();
    @Accessor("emeraldCost") int rerollTrades$emeraldCost();
}
//#endif
