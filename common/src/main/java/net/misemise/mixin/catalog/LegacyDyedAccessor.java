//#if MC < 260100
package net.misemise.mixin.catalog;

import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

//#if MC >= 12111
@Mixin(targets = "net.minecraft.world.entity.npc.villager.VillagerTrades$DyedArmorForEmeralds")
//#else
//$$ @Mixin(targets = "net.minecraft.world.entity.npc.VillagerTrades$DyedArmorForEmeralds")
//#endif
public interface LegacyDyedAccessor {
    @Accessor("item") Item rerollTrades$item();
    @Accessor("value") int rerollTrades$value();
}
//#endif
