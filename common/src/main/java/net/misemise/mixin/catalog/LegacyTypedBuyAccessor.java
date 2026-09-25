//#if MC < 260100
package net.misemise.mixin.catalog;
import java.util.Map;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
//#if MC >= 12111
@Mixin(targets = "net.minecraft.world.entity.npc.villager.VillagerTrades$EmeraldsForVillagerTypeItem")
//#else
//$$ @Mixin(targets = "net.minecraft.world.entity.npc.VillagerTrades$EmeraldsForVillagerTypeItem")
//#endif
public interface LegacyTypedBuyAccessor {
    @Accessor("trades") Map<?, Item> rerollTrades$trades();
    @Accessor("cost") int rerollTrades$cost();
}
//#endif
