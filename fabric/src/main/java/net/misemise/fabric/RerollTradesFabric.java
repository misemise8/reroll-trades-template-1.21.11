package net.misemise.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.misemise.RerollTrades;
import net.misemise.network.RerollParticlePayload;
import net.misemise.network.RerollTradesPayload;

public final class RerollTradesFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        RerollTrades.init();

        PayloadTypeRegistry.serverboundPlay().register(RerollTradesPayload.TYPE, RerollTradesPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(RerollParticlePayload.TYPE, RerollParticlePayload.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(RerollTradesPayload.TYPE, (payload, context) ->
                context.server().execute(() -> RerollTrades.handleReroll(context.player()))
        );
    }
}
