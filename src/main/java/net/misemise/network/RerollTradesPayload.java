package net.misemise.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RerollTradesPayload() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RerollTradesPayload> ID = new CustomPacketPayload.Type<>(
            Identifier.fromNamespaceAndPath("reroll-trades", "reroll"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RerollTradesPayload> CODEC =
            StreamCodec.unit(new RerollTradesPayload());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}