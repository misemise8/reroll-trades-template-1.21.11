package net.misemise.neoforge;

import net.misemise.RerollTrades;
import net.misemise.network.RerollLockedPayload;
import net.misemise.network.RerollParticlePayload;
import net.misemise.network.RerollRejectPayload;
import net.misemise.network.RerollTradesPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(RerollTradesNeoForge.NEOFORGE_MOD_ID)
public final class RerollTradesNeoForge {

    public static final String NEOFORGE_MOD_ID = "reroll_trades";

    public RerollTradesNeoForge(IEventBus modEventBus) {
        RerollTradesNeoForgeAttachments.register(modEventBus);
        RerollTrades.init();
        modEventBus.addListener(this::registerPayloads);
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                RerollTradesPayload.TYPE,
                RerollTradesPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> RerollTrades.handleReroll((ServerPlayer) context.player()))
        );
        registrar.playToClient(RerollParticlePayload.TYPE, RerollParticlePayload.STREAM_CODEC);
        registrar.playToClient(RerollLockedPayload.TYPE, RerollLockedPayload.STREAM_CODEC);
        registrar.playToClient(RerollRejectPayload.TYPE, RerollRejectPayload.STREAM_CODEC);
    }

    @EventBusSubscriber(modid = NEOFORGE_MOD_ID, value = Dist.CLIENT)
    public static final class ClientModEvents {

        private ClientModEvents() {
        }

        @SubscribeEvent
        public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
            RerollTradesNeoForgeClient.registerKeyMappings(event);
        }

        @SubscribeEvent
        public static void registerClientPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
            RerollTradesNeoForgeClient.registerClientPayloadHandlers(event);
        }
    }
}
