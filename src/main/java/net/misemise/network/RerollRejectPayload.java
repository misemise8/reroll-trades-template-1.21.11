package net.misemise.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RerollRejectPayload() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RerollRejectPayload> ID = new CustomPacketPayload.Type<>(
            Identifier.fromNamespaceAndPath("reroll-trades", "reroll_reject"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RerollRejectPayload> CODEC =
            StreamCodec.unit(new RerollRejectPayload());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
