//#if MC >= 260100
package net.misemise.mixin.catalog;

import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.Optional;

@Mixin(VillagerTrade.class)
public interface ModernTradeAccessor {
    @Accessor("merchantPredicate") Optional<LootItemCondition> rerollTrades$predicate();
    @Accessor("gives") ItemStackTemplate rerollTrades$gives();
}
//#endif
