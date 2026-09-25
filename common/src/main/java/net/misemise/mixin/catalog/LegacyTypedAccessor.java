//#if MC < 260100
package net.misemise.mixin.catalog;

//#if MC >= 12111
import net.minecraft.world.entity.npc.villager.VillagerTrades;
//#else
//$$ import net.minecraft.world.entity.npc.VillagerTrades;
//#endif
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.Map;

//#if MC >= 12111
@Mixin(targets = "net.minecraft.world.entity.npc.villager.VillagerTrades$TypeSpecificTrade")
//#else
//$$ @Mixin(targets = "net.minecraft.world.entity.npc.VillagerTrades$TypeSpecificTrade")
//#endif
public interface LegacyTypedAccessor {
    @Accessor("trades") Map<?, VillagerTrades.ItemListing> rerollTrades$trades();
}
//#endif
