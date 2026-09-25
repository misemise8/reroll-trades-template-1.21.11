package net.misemise.target;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
//#if MC >= 12111
import net.minecraft.world.entity.npc.villager.Villager;
//#else
//$$ import net.minecraft.resources.ResourceLocation;
//$$ import net.minecraft.world.entity.npc.Villager;
//#endif
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.trading.MerchantOffer;
import net.misemise.RerollTrades;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;

public final class TradeCatalog {
    public static final int MAX_ENTRIES = 2048;
    private TradeCatalog() {}

    public static List<TradeCandidate> create(Villager villager) {
        Builder builder = new Builder(villager);
        try {
//#if MC >= 260100
            ModernTradeCatalog.populate(builder);
//#else
//$$             LegacyTradeCatalog.populate(builder);
//#endif
        } catch (RuntimeException exception) {
            RerollTrades.LOGGER.warn("Could not describe the complete trade catalog for {}", profession(villager), exception);
        }
        // Custom factories can still be selected once they are visible. Their generated range is unknown.
        for (MerchantOffer offer : villager.getOffers()) {
            boolean buying = !offer.getCostA().is(Items.EMERALD) && offer.getResult().is(Items.EMERALD);
            if (!buying && !offer.getCostA().is(Items.EMERALD)) continue;
            ItemStack item = buying ? offer.getCostA() : offer.getResult();
            boolean known = builder.entries.stream().anyMatch(candidate -> candidate.buying() == buying && candidate.rule("", 999).matchesResult(item));
            if (!known) builder.add(item.copy(), "", 0, PriceRange.UNKNOWN, false, buying);
        }
        return List.copyOf(builder.entries);
    }

    public static String key(ResourceKey<?> key) {
//#if MC >= 12111
        return key.identifier().toString();
//#else
//$$         return key.location().toString();
//#endif
    }

    public static String profession(Villager villager) {
//#if MC >= 12105
        return villager.getVillagerData().profession().unwrapKey().map(TradeCatalog::key).orElse("");
//#else
//$$         return BuiltInRegistries.VILLAGER_PROFESSION.getKey(villager.getVillagerData().getProfession()).toString();
//#endif
    }

    public static int level(Villager villager) {
//#if MC >= 12105
        return villager.getVillagerData().level();
//#else
//$$         return villager.getVillagerData().getLevel();
//#endif
    }

    public static final class Builder {
        public final Villager villager;
        private final List<TradeCandidate> entries = new ArrayList<>();
        public Builder(Villager villager) { this.villager = villager; }

        public void add(ItemStack template, String enchantment, int level, PriceRange price, boolean equipment) {
            add(template, enchantment, level, price, equipment, false);
        }

        public void add(ItemStack template, String enchantment, int level, PriceRange price, boolean equipment, boolean buying) {
            if (template.isEmpty() || entries.size() >= MAX_ENTRIES) return;
            template = template.copyWithCount(1);
            TradeCandidate candidate = new TradeCandidate(template, enchantment, level, price, equipment, buying);
            for (int i = 0; i < entries.size(); i++) {
                TradeCandidate current = entries.get(i);
                if (current.rule("", 999).sameTarget(candidate.rule("", 999))) {
                    entries.set(i, new TradeCandidate(current.template(), enchantment, level, current.price().union(price), equipment, buying));
                    return;
                }
            }
            entries.add(candidate);
        }

        public void item(ItemStack item, PriceRange price, boolean equipment) {
            add(item, "", 0, price, equipment);
            if (equipment) {
                for (Holder.Reference<Enchantment> holder : villager.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements().toList()) {
                    if (!holder.is(EnchantmentTags.ON_TRADED_EQUIPMENT) || !holder.value().canEnchant(item)) continue;
                    for (int level = holder.value().getMinLevel(); level <= Math.min(255, holder.value().getMaxLevel()); level++) {
                        add(item, holder.unwrapKey().map(TradeCatalog::key).orElse(""), level, price, true);
                    }
                }
            }
        }

        public List<Holder.Reference<Enchantment>> equipmentEnchantments() {
            return villager.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements()
                    .filter(holder -> holder.is(EnchantmentTags.ON_TRADED_EQUIPMENT)).toList();
        }

        public void equipment(ItemStack item, PriceRange price, PriceRange power, List<? extends Holder<Enchantment>> enchantments) {
            add(item, "", 0, price, true);
//#if MC >= 12102
            var component = item.get(DataComponents.ENCHANTABLE);
            int enchantability = component == null ? 0 : component.value();
//#else
//$$             int enchantability = item.getItem().getEnchantmentValue();
//#endif
            if (enchantability <= 0) return;
            int low = power.known() ? Math.max(1, Math.round((power.min() + 1) * 0.85f)) : 1;
            int high = power.known() ? Math.round((power.max() + 1 + 2 * (enchantability / 4)) * 1.15f) : 1024;
            for (Holder<Enchantment> holder : enchantments) {
                if (!holder.value().isPrimaryItem(item)) continue;
                var levels = new HashSet<Integer>();
                for (int cost = low; cost <= Math.min(4096, high); cost++) {
                    for (int level = Math.min(255, holder.value().getMaxLevel()); level >= holder.value().getMinLevel(); level--) {
                        if (cost >= holder.value().getMinCost(level) && cost <= holder.value().getMaxCost(level)) {
                            levels.add(level);
                            break;
                        }
                    }
                }
                levels.stream().sorted().forEach(level -> add(item, holder.unwrapKey().map(TradeCatalog::key).orElse(""), level, price, true));
            }
        }

        public void book(Holder<Enchantment> holder, int low, int high, PriceRange base, boolean doubled) {
            for (int level = Math.max(low, holder.value().getMinLevel()); level <= Math.min(Math.min(high, holder.value().getMaxLevel()), 255); level++) {
                // Double the enchantment surcharge before the final item-stack clamp.
                int factor = doubled ? 2 : 1;
                PriceRange surcharge = new PriceRange((2 + 3 * level) * factor, (6 + 13 * level) * factor);
                add(new ItemStack(Items.ENCHANTED_BOOK), holder.unwrapKey().map(TradeCatalog::key).orElse(""), level,
                        base.plus(surcharge).clamp(64), false);
            }
        }
    }
}
