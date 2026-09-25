//#if MC < 260100
package net.misemise.mixin.catalog;

import net.minecraft.world.item.ItemStack;
import java.util.Optional;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

//#if MC >= 12111
@Mixin(targets = "net.minecraft.world.entity.npc.villager.VillagerTrades$ItemsAndEmeraldsToItems")
//#else
//$$ @Mixin(targets = "net.minecraft.world.entity.npc.VillagerTrades$ItemsAndEmeraldsToItems")
//#endif
public interface LegacyExchangeAccessor {
    @Accessor("toItem") ItemStack rerollTrades$toItem();
    @Accessor("emeraldCost") int rerollTrades$emeraldCost();
    @Accessor("enchantmentProvider") Optional<?> rerollTrades$enchantmentProvider();
}
//#endif
