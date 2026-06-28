package net.misemise.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.misemise.RerollTrades;
import net.misemise.command.RerollCommands;
import net.misemise.network.RerollActionPayload;
import net.misemise.network.RerollEffectPayload;
import net.misemise.network.RerollStatePayload;
import net.misemise.reroll.RerollController;

public final class RerollTradesFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        RerollTrades.init();

        PayloadTypeRegistry.serverboundPlay().register(RerollActionPayload.TYPE, RerollActionPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(RerollStatePayload.TYPE, RerollStatePayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(RerollEffectPayload.TYPE, RerollEffectPayload.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(RerollActionPayload.TYPE, (payload, context) ->
                context.server().execute(() -> RerollController.handleAction(
                        context.player(), payload.action(), payload.containerId()))
        );

        ServerTickEvents.END_SERVER_TICK.register(RerollController::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                RerollController.onPlayerLogout(handler.getPlayer()));
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                RerollCommands.register(dispatcher));
    }
}
