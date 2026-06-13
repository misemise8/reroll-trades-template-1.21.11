package net.misemise.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record RerollLockedPayload() implements CustomPayload {

    public static final Id<RerollLockedPayload> ID = new Id<>(Identifier.of("reroll-trades", "reroll_locked"));

    public static final PacketCodec<RegistryByteBuf, RerollLockedPayload> CODEC = PacketCodec
            .unit(new RerollLockedPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
