package net.misemise.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RerollLockedPayload() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RerollLockedPayload> ID = new CustomPacketPayload.Type<>(
            Identifier.fromNamespaceAndPath("reroll-trades", "reroll_locked"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RerollLockedPayload> CODEC =
            StreamCodec.unit(new RerollLockedPayload());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
