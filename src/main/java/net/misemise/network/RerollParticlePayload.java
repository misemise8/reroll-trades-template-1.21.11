package net.misemise.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RerollParticlePayload(BlockPos pos) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RerollParticlePayload> ID = new CustomPacketPayload.Type<>(
            Identifier.fromNamespaceAndPath("reroll-trades", "particle"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RerollParticlePayload> CODEC =
            StreamCodec.composite(  // tuple → composite
                    BlockPos.STREAM_CODEC, RerollParticlePayload::pos,
                    RerollParticlePayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}