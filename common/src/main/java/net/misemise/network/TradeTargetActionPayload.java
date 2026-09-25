package net.misemise.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//#if MC >= 12111
import net.minecraft.resources.Identifier;
//#else
//$$ import net.minecraft.resources.ResourceLocation;
//#endif
import net.misemise.RerollTrades;

public record TradeTargetActionPayload(int containerId, int action, int revision, int selection, int maxEmeralds) implements CustomPacketPayload {
    public static final int REQUEST = 0, ADD = 1, REMOVE = 2;
    public static final Type<TradeTargetActionPayload> TYPE =
//#if MC >= 12111
            new Type<>(Identifier.fromNamespaceAndPath(RerollTrades.MOD_ID, "target_action"));
//#else
//$$             new Type<>(ResourceLocation.fromNamespaceAndPath(RerollTrades.MOD_ID, "target_action"));
//#endif
    public static final StreamCodec<RegistryFriendlyByteBuf, TradeTargetActionPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.containerId); buffer.writeVarInt(payload.action);
                buffer.writeVarInt(payload.revision); buffer.writeVarInt(payload.selection); buffer.writeVarInt(payload.maxEmeralds);
            },
            buffer -> new TradeTargetActionPayload(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
