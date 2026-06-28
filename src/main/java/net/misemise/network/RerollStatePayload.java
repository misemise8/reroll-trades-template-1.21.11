package net.misemise.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.misemise.RerollTrades;
import net.misemise.reroll.RerollBlockReason;

public record RerollStatePayload(
        int containerId,
        boolean supported,
        boolean canReroll,
        RerollBlockReason reason,
        boolean canUndo,
        int remainingCooldownTicks,
        int remainingRerolls,
        boolean requireSneaking
) implements CustomPacketPayload {

    public static final Type<RerollStatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RerollTrades.MOD_ID, "screen_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RerollStatePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.containerId);
                buffer.writeBoolean(payload.supported);
                buffer.writeBoolean(payload.canReroll);
                buffer.writeVarInt(payload.reason.ordinal());
                buffer.writeBoolean(payload.canUndo);
                buffer.writeVarInt(payload.remainingCooldownTicks);
                buffer.writeVarInt(payload.remainingRerolls + 1);
                buffer.writeBoolean(payload.requireSneaking);
            },
            buffer -> new RerollStatePayload(
                    buffer.readVarInt(),
                    buffer.readBoolean(),
                    buffer.readBoolean(),
                    RerollBlockReason.byId(buffer.readVarInt()),
                    buffer.readBoolean(),
                    buffer.readVarInt(),
                    buffer.readVarInt() - 1,
                    buffer.readBoolean()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
