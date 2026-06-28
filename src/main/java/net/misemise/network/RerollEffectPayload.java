package net.misemise.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.misemise.RerollTrades;

public record RerollEffectPayload(BlockPos pos, boolean undo) implements CustomPacketPayload {

    public static final Type<RerollEffectPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(RerollTrades.MOD_ID, "effect"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RerollEffectPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                BlockPos.STREAM_CODEC.encode(buffer, payload.pos);
                buffer.writeBoolean(payload.undo);
            },
            buffer -> new RerollEffectPayload(
                    BlockPos.STREAM_CODEC.decode(buffer),
                    buffer.readBoolean()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
