package net.misemise.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * S2C packet sent when the server rejects a reroll request (e.g. must sneak,
 * no profession, etc.). The client uses this to re-enable the reroll button
 * that was optimistically disabled when the request was sent.
 *
 * Without this, the button would remain grayed out permanently after a
 * rejected reroll attempt.
 */
public record RerollRejectPayload() implements CustomPayload {

    public static final Id<RerollRejectPayload> ID = new Id<>(
            Identifier.of("reroll-trades", "reroll_reject"));

    public static final PacketCodec<RegistryByteBuf, RerollRejectPayload> CODEC = PacketCodec
            .unit(new RerollRejectPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
