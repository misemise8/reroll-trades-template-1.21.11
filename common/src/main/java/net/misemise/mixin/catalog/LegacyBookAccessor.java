//#if MC < 260100
package net.misemise.mixin.catalog;

import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.tags.TagKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

//#if MC >= 12111
@Mixin(targets = "net.minecraft.world.entity.npc.villager.VillagerTrades$EnchantBookForEmeralds")
//#else
//$$ @Mixin(targets = "net.minecraft.world.entity.npc.VillagerTrades$EnchantBookForEmeralds")
//#endif
public interface LegacyBookAccessor {
    @Accessor("tradeableEnchantments") TagKey<Enchantment> rerollTrades$tradeableEnchantments();
    @Accessor("minLevel") int rerollTrades$minLevel();
    @Accessor("maxLevel") int rerollTrades$maxLevel();
}
//#endif
