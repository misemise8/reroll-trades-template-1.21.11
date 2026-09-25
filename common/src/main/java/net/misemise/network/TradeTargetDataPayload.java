package net.misemise.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//#if MC >= 12111
import net.minecraft.resources.Identifier;
//#else
//$$ import net.minecraft.resources.ResourceLocation;
//#endif
import net.minecraft.world.item.ItemStack;
import net.misemise.RerollTrades;
import net.misemise.target.TradeCandidate;
import net.misemise.target.TradeCatalog;
import net.misemise.target.TradeLockData;
import net.misemise.target.TradeRule;
import java.util.ArrayList;
import java.util.List;

public record TradeTargetDataPayload(int containerId, int revision, List<TradeCandidate> catalog,
        List<TradeRule> rules, List<Integer> lockedSlots, boolean editable, String message) implements CustomPacketPayload {
    public static final Type<TradeTargetDataPayload> TYPE =
//#if MC >= 12111
            new Type<>(Identifier.fromNamespaceAndPath(RerollTrades.MOD_ID, "target_data_v2"));
//#else
//$$             new Type<>(ResourceLocation.fromNamespaceAndPath(RerollTrades.MOD_ID, "target_data_v2"));
//#endif
    public static final StreamCodec<RegistryFriendlyByteBuf, TradeTargetDataPayload> STREAM_CODEC =
            StreamCodec.of(TradeTargetDataPayload::write, TradeTargetDataPayload::read);

    private static void write(RegistryFriendlyByteBuf buffer, TradeTargetDataPayload data) {
        buffer.writeVarInt(data.containerId); buffer.writeVarInt(data.revision);
        buffer.writeBoolean(data.editable); buffer.writeUtf(data.message, 128);
        buffer.writeVarInt(data.catalog.size());
        for (TradeCandidate candidate : data.catalog) candidate.write(buffer);
        buffer.writeVarInt(data.rules.size());
        for (int i = 0; i < data.rules.size(); i++) {
            TradeRule rule = data.rules.get(i);
            buffer.writeUtf(rule.id(), 64); ItemStack.STREAM_CODEC.encode(buffer, rule.template());
            buffer.writeUtf(rule.enchantment(), 256); buffer.writeVarInt(rule.level()); buffer.writeVarInt(rule.maxEmeralds());
            buffer.writeVarInt(rule.minPrice()); buffer.writeBoolean(rule.buying());
            buffer.writeVarInt(data.lockedSlots.get(i));
        }
    }

    private static TradeTargetDataPayload read(RegistryFriendlyByteBuf buffer) {
        int containerId = buffer.readVarInt(), revision = buffer.readVarInt();
        boolean editable = buffer.readBoolean();
        String message = buffer.readUtf(128);
        int count = checkedCount(buffer, TradeCatalog.MAX_ENTRIES);
        List<TradeCandidate> catalog = new ArrayList<>();
        for (int i = 0; i < count; i++) catalog.add(TradeCandidate.read(buffer));
        int ruleCount = checkedCount(buffer, TradeLockData.MAX_RULES);
        List<TradeRule> rules = new ArrayList<>();
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < ruleCount; i++) {
            rules.add(new TradeRule(buffer.readUtf(64), ItemStack.STREAM_CODEC.decode(buffer), buffer.readUtf(256), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean()));
            slots.add(buffer.readVarInt());
        }
        return new TradeTargetDataPayload(containerId, revision, List.copyOf(catalog), List.copyOf(rules), List.copyOf(slots), editable, message);
    }

    private static int checkedCount(RegistryFriendlyByteBuf buffer, int maximum) {
        int count = buffer.readVarInt();
        if (count < 0 || count > maximum) throw new IllegalArgumentException("Invalid trade target count");
        return count;
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
