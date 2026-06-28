package net.misemise;

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
import net.misemise.platform.PlatformServices;
import net.misemise.reroll.RerollController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RerollTrades implements ModInitializer {

    public static final String MOD_ID = "reroll-trades";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        PlatformServices.init();

        PayloadTypeRegistry.playC2S().register(RerollActionPayload.TYPE, RerollActionPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(RerollStatePayload.TYPE, RerollStatePayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(RerollEffectPayload.TYPE, RerollEffectPayload.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(RerollActionPayload.TYPE, (payload, context) ->
                context.server().execute(() -> RerollController.handleAction(
                        context.player(), payload.action(), payload.containerId()))
        );

        ServerTickEvents.END_SERVER_TICK.register(RerollController::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                RerollController.onPlayerLogout(handler.getPlayer()));
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                RerollCommands.register(dispatcher));

        LOGGER.info("Reroll Trades initialized");
    }
}
