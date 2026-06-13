package net.misemise.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RerollParticlePayload(BlockPos pos) implements CustomPacketPayload {

    public static final Type<RerollParticlePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("reroll-trades", "particle"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RerollParticlePayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    RerollParticlePayload::pos,
                    RerollParticlePayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
