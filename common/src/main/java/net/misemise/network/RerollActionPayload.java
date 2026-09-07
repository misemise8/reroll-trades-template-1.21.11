package net.misemise.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//#if MC >= 12111
import net.minecraft.resources.Identifier;
//#else
//$$ import net.minecraft.resources.ResourceLocation;
//#endif
import net.misemise.RerollTrades;
import net.misemise.reroll.RerollAction;

public record RerollActionPayload(RerollAction action, int containerId) implements CustomPacketPayload {

    public static final Type<RerollActionPayload> TYPE =
//#if MC >= 12111
            new Type<>(Identifier.fromNamespaceAndPath(RerollTrades.MOD_ID, "action"));
//#else
//$$             new Type<>(ResourceLocation.fromNamespaceAndPath(RerollTrades.MOD_ID, "action"));
//#endif

    public static final StreamCodec<RegistryFriendlyByteBuf, RerollActionPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.action.ordinal());
                buffer.writeVarInt(payload.containerId);
            },
            buffer -> new RerollActionPayload(
                    RerollAction.byId(buffer.readVarInt()),
                    buffer.readVarInt()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
