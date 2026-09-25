//#if MC < 260100
package net.misemise.mixin.catalog;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

//#if MC >= 12111
@Mixin(targets = "net.minecraft.world.entity.npc.villager.VillagerTrades$EnchantedItemForEmeralds")
//#else
//$$ @Mixin(targets = "net.minecraft.world.entity.npc.VillagerTrades$EnchantedItemForEmeralds")
//#endif
public interface LegacyEquipmentAccessor {
    @Accessor("itemStack") ItemStack rerollTrades$itemStack();
    @Accessor("baseEmeraldCost") int rerollTrades$baseEmeraldCost();
}
//#endif
