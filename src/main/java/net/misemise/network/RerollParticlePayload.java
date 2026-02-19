package net.misemise.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * S2C packet sent after a successful reroll to trigger particle effects on the
 * client.
 * Carries the villager's block position so particles appear at the right
 * location.
 */
public record RerollParticlePayload(BlockPos pos) implements CustomPayload {

    public static final CustomPayload.Id<RerollParticlePayload> ID = new CustomPayload.Id<>(
            Identifier.of("reroll-trades", "particle"));

    public static final PacketCodec<RegistryByteBuf, RerollParticlePayload> CODEC = PacketCodec.tuple(
            BlockPos.PACKET_CODEC, RerollParticlePayload::pos,
            RerollParticlePayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
