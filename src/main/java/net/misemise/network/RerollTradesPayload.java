package net.misemise.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * C2S packet sent when the player clicks the Reroll button.
 * No data needed — the server identifies the villager from the player's open
 * screen.
 */
public record RerollTradesPayload() implements CustomPayload {

    public static final CustomPayload.Id<RerollTradesPayload> ID = new CustomPayload.Id<>(
            Identifier.of("reroll-trades", "reroll"));

    public static final PacketCodec<RegistryByteBuf, RerollTradesPayload> CODEC = PacketCodec
            .unit(new RerollTradesPayload());

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
