//#if MC < 260100
package net.misemise.mixin.catalog;
import net.minecraft.world.item.trading.ItemCost;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
//#if MC >= 12111
@Mixin(targets = "net.minecraft.world.entity.npc.villager.VillagerTrades$EmeraldForItems")
//#else
//$$ @Mixin(targets = "net.minecraft.world.entity.npc.VillagerTrades$EmeraldForItems")
//#endif
public interface LegacyBuyAccessor {
    @Accessor("itemStack") ItemCost rerollTrades$itemStack();
}
//#endif
