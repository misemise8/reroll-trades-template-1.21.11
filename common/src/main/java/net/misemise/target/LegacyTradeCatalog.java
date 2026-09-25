//#if MC < 260100
package net.misemise.target;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.EnchantmentTags;
//#if MC >= 12111
import net.minecraft.world.entity.npc.villager.VillagerTrades;
//#else
//$$ import net.minecraft.world.entity.npc.VillagerTrades;
//#endif
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.providers.EnchantmentProvider;
import net.minecraft.world.item.enchantment.providers.SingleEnchantment;
import net.minecraft.world.item.enchantment.providers.EnchantmentsByCost;
import java.util.Optional;
import net.misemise.mixin.catalog.*;

final class LegacyTradeCatalog {
    private LegacyTradeCatalog() {}

    static void populate(TradeCatalog.Builder builder) {
        var villager = builder.villager;
//#if MC >= 12105
        Object profession = villager.getVillagerData().profession().unwrapKey().orElse(null);
//#else
//$$         Object profession = villager.getVillagerData().getProfession();
//#endif
        var trades = villager.level().enabledFeatures().contains(FeatureFlags.TRADE_REBALANCE)
                ? VillagerTrades.EXPERIMENTAL_TRADES.getOrDefault(profession, VillagerTrades.TRADES.get(profession))
                : VillagerTrades.TRADES.get(profession);
        if (trades == null) return;
        var listings = trades.get(TradeCatalog.level(villager));
        if (listings != null) for (var listing : listings) describe(builder, listing);
    }

    private static void describe(TradeCatalog.Builder b, VillagerTrades.ItemListing listing) {
        if (listing instanceof LegacyTypedAccessor typed) {
//#if MC >= 12105
            Object type = b.villager.getVillagerData().type().unwrapKey().orElse(null);
//#else
//$$             Object type = b.villager.getVillagerData().getType();
//#endif
            var specific = typed.rerollTrades$trades().get(type);
            if (specific != null) describe(b, specific);
        } else if (listing instanceof LegacyItemsAccessor sale) {
            provided(b, sale.rerollTrades$itemStack(), PriceRange.fixed(sale.rerollTrades$emeraldCost()), sale.rerollTrades$enchantmentProvider());
        } else if (listing instanceof LegacyExchangeAccessor sale) {
            provided(b, sale.rerollTrades$toItem(), PriceRange.fixed(sale.rerollTrades$emeraldCost()), sale.rerollTrades$enchantmentProvider());
        } else if (listing instanceof LegacyEquipmentAccessor sale) {
            b.equipment(sale.rerollTrades$itemStack(), new PriceRange(sale.rerollTrades$baseEmeraldCost() + 5, sale.rerollTrades$baseEmeraldCost() + 19).clamp(64), new PriceRange(5, 19), b.equipmentEnchantments());
        } else if (listing instanceof LegacyDyedAccessor sale) {
            b.item(new ItemStack(sale.rerollTrades$item()), PriceRange.fixed(sale.rerollTrades$value()), false);
        } else if (listing instanceof LegacyBookAccessor sale) {
            for (var holder : b.villager.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements().toList()) {
                if (holder.is(sale.rerollTrades$tradeableEnchantments())) {
                    b.book(holder, sale.rerollTrades$minLevel(), sale.rerollTrades$maxLevel(), PriceRange.fixed(0), holder.is(EnchantmentTags.DOUBLE_TRADE_PRICE));
                }
            }
        } else if (listing instanceof LegacyMapAccessor sale) {
            ItemStack map = new ItemStack(Items.FILLED_MAP);
            map.set(DataComponents.ITEM_NAME, Component.translatable(sale.rerollTrades$displayName()));
            b.item(map, PriceRange.fixed(sale.rerollTrades$emeraldCost()), false);
        } else if (listing instanceof LegacyStewAccessor sale) {
            ItemStack stew = new ItemStack(Items.SUSPICIOUS_STEW);
            stew.set(DataComponents.SUSPICIOUS_STEW_EFFECTS, sale.rerollTrades$effects());
            b.item(stew, PriceRange.fixed(1), false);
        } else if (listing instanceof LegacyArrowAccessor sale) {
            b.item(sale.rerollTrades$toItem(), PriceRange.fixed(sale.rerollTrades$emeraldCost()), false);
        }
    }

    @SuppressWarnings("unchecked")
    private static void provided(TradeCatalog.Builder b, ItemStack item, PriceRange price, Optional<?> providerKey) {
        b.item(item, price, false);
        if (providerKey.isEmpty()) return;
        var provider = b.villager.registryAccess().lookupOrThrow(Registries.ENCHANTMENT_PROVIDER)
                .get((ResourceKey<EnchantmentProvider>) providerKey.get()).orElseThrow().value();
        if (provider instanceof SingleEnchantment single) {
            int low = Math.max(single.enchantment().value().getMinLevel(), single.level().getMinValue());
            int high = Math.min(single.enchantment().value().getMaxLevel(), single.level().getMaxValue());
            for (int level = low; level <= Math.min(255, high); level++)
                b.add(item, single.enchantment().unwrapKey().map(TradeCatalog::key).orElse(""), level, price, true);
        } else if (provider instanceof EnchantmentsByCost byCost) {
            b.equipment(item, price, new PriceRange(byCost.cost().getMinValue(), byCost.cost().getMaxValue()), byCost.enchantments().stream().toList());
        }
    }
}
//#endif
