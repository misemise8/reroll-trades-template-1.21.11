package net.misemise.target;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;

public record TradeRule(String id, ItemStack template, String enchantment, int level, int maxEmeralds) {
    public static final Codec<TradeRule> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("id").forGetter(TradeRule::id),
            ItemStack.CODEC.fieldOf("item").forGetter(TradeRule::template),
            Codec.STRING.optionalFieldOf("enchantment", "").forGetter(TradeRule::enchantment),
            Codec.intRange(0, 255).optionalFieldOf("level", 0).forGetter(TradeRule::level),
            Codec.intRange(1, 999).fieldOf("max_emeralds").forGetter(TradeRule::maxEmeralds)
    ).apply(i, TradeRule::new));

    public boolean matches(MerchantOffer offer) {
        return offer.getCostA().is(Items.EMERALD) && offer.getCostA().getCount() <= maxEmeralds
                && matchesResult(offer.getResult());
    }

    public boolean matchesResult(ItemStack result) {
        if (result.isEmpty() || !result.is(template.getItem())) return false;
        for (var entry : template.getComponentsPatch().entrySet()) {
            if (entry.getValue().isPresent() && !entry.getValue().get().equals(result.get(entry.getKey()))) return false;
            if (entry.getValue().isEmpty() && result.has(entry.getKey())) return false;
        }
        if (enchantment.isEmpty()) return true;
        ItemEnchantments enchantments = result.getOrDefault(result.is(Items.ENCHANTED_BOOK)
                ? DataComponents.STORED_ENCHANTMENTS : DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        return enchantments.entrySet().stream().anyMatch(e -> e.getKey().unwrapKey()
                .map(key -> TradeCatalog.key(key).equals(enchantment)).orElse(false) && e.getIntValue() == level);
    }

    public boolean sameTarget(TradeRule other) {
        return ItemStack.isSameItemSameComponents(template, other.template)
                && enchantment.equals(other.enchantment) && level == other.level;
    }
}
