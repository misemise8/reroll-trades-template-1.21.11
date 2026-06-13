package net.misemise.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record RerollRejectPayload() implements CustomPayload {

    public static final Id<RerollRejectPayload> ID = new Id<>(Identifier.of("reroll-trades", "reroll_reject"));

    public static final PacketCodec<RegistryByteBuf, RerollRejectPayload> CODEC = PacketCodec
            .unit(new RerollRejectPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
