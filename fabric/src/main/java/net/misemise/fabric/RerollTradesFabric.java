package net.misemise.fabric;

import net.misemise.RerollTrades;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.misemise.command.RerollCommands;
import net.misemise.network.RerollActionPayload;
import net.misemise.network.RerollEffectPayload;
import net.misemise.network.RerollStatePayload;
import net.misemise.network.TradeTargetActionPayload;
import net.misemise.network.TradeTargetDataPayload;
import net.misemise.target.TradeTargetController;
import net.misemise.platform.PlatformServices;
import net.misemise.reroll.RerollController;

public final class RerollTradesFabric implements ModInitializer {


    @Override
    public void onInitialize() {
        PlatformServices.init();

//#if MC >= 260100
        PayloadTypeRegistry.serverboundPlay().register(RerollActionPayload.TYPE, RerollActionPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(TradeTargetActionPayload.TYPE, TradeTargetActionPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(TradeTargetDataPayload.TYPE, TradeTargetDataPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(RerollStatePayload.TYPE, RerollStatePayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(RerollEffectPayload.TYPE, RerollEffectPayload.STREAM_CODEC);
//#else
//$$         PayloadTypeRegistry.playC2S().register(RerollActionPayload.TYPE, RerollActionPayload.STREAM_CODEC);
//$$         PayloadTypeRegistry.playC2S().register(TradeTargetActionPayload.TYPE, TradeTargetActionPayload.STREAM_CODEC);
//$$         PayloadTypeRegistry.playS2C().register(TradeTargetDataPayload.TYPE, TradeTargetDataPayload.STREAM_CODEC);
//$$         PayloadTypeRegistry.playS2C().register(RerollStatePayload.TYPE, RerollStatePayload.STREAM_CODEC);
//$$         PayloadTypeRegistry.playS2C().register(RerollEffectPayload.TYPE, RerollEffectPayload.STREAM_CODEC);
//#endif

        ServerPlayNetworking.registerGlobalReceiver(RerollActionPayload.TYPE, (payload, context) ->
                context.server().execute(() -> RerollController.handleAction(
                        context.player(), payload.action(), payload.containerId()))
        );

        ServerTickEvents.END_SERVER_TICK.register(RerollController::tick);
        ServerPlayNetworking.registerGlobalReceiver(TradeTargetActionPayload.TYPE, (payload, context) ->
                context.server().execute(() -> TradeTargetController.handle(context.player(), payload)));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                RerollController.onPlayerLogout(handler.getPlayer()));
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                RerollCommands.register(dispatcher));

        RerollTrades.init();
    }
}
