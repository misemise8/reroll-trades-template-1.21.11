package net.misemise.target;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public record TradeCandidate(ItemStack template, String enchantment, int level, PriceRange price, boolean enchantedEquipment, boolean buying) {
    public TradeCandidate(ItemStack template, String enchantment, int level, PriceRange price, boolean enchantedEquipment) {
        this(template, enchantment, level, price, enchantedEquipment, false);
    }
    public TradeRule rule(String id, int maxPrice) { return rule(id, 1, maxPrice); }

    public TradeRule rule(String id, int minPrice, int maxPrice) {
        return new TradeRule(id, template.copy(), enchantment, level, maxPrice, minPrice, buying);
    }

    public void write(RegistryFriendlyByteBuf buffer) {
        ItemStack.STREAM_CODEC.encode(buffer, template);
        buffer.writeUtf(enchantment, 256);
        buffer.writeVarInt(level);
        buffer.writeVarInt(price.min());
        buffer.writeVarInt(price.max());
        buffer.writeBoolean(enchantedEquipment);
        buffer.writeBoolean(buying);
    }

    public static TradeCandidate read(RegistryFriendlyByteBuf buffer) {
        return new TradeCandidate(ItemStack.STREAM_CODEC.decode(buffer), buffer.readUtf(256), buffer.readVarInt(),
                new PriceRange(buffer.readVarInt(), buffer.readVarInt()), buffer.readBoolean(), buffer.readBoolean());
    }
}
