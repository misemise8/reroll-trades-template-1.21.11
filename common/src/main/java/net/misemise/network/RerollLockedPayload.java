package net.misemise.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RerollLockedPayload() implements CustomPacketPayload {

    public static final Type<RerollLockedPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("reroll-trades", "reroll_locked"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RerollLockedPayload> STREAM_CODEC =
            StreamCodec.unit(new RerollLockedPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
