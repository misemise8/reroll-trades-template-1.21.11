package net.misemise.neoforge;

import net.misemise.RerollTrades;
import net.misemise.command.RerollCommands;
import net.misemise.client.RerollConfigScreen;
import net.misemise.client.RerollTradesClient;
import net.misemise.network.RerollActionPayload;
import net.misemise.network.RerollEffectPayload;
import net.misemise.network.RerollStatePayload;
import net.misemise.reroll.RerollController;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
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
        PayloadRegistrar registrar = event.registrar("2");
        registrar.playToServer(
                RerollActionPayload.TYPE,
                RerollActionPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> RerollController.handleAction(
                        (ServerPlayer) context.player(), payload.action(), payload.containerId()))
        );
        registrar.playToClient(
                RerollStatePayload.TYPE,
                RerollStatePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> RerollTradesClient.handleState(payload))
        );
        registrar.playToClient(
                RerollEffectPayload.TYPE,
                RerollEffectPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> RerollTradesClient.handleEffect(payload))
        );
    }

    @EventBusSubscriber(modid = NEOFORGE_MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class ClientModEvents {

        private ClientModEvents() {
        }

        @SubscribeEvent
        public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
            RerollTradesNeoForgeClient.registerKeyMappings(event);
        }

        @SubscribeEvent
        public static void clientSetup(FMLClientSetupEvent event) {
            ModLoadingContext.get().registerExtensionPoint(
                    IConfigScreenFactory.class,
                    () -> (container, parent) -> new RerollConfigScreen(parent)
            );
        }
    }

    @EventBusSubscriber(modid = NEOFORGE_MOD_ID)
    public static final class CommonEvents {

        private CommonEvents() {
        }

        @SubscribeEvent
        public static void registerCommands(RegisterCommandsEvent event) {
            RerollCommands.register(event.getDispatcher());
        }

        @SubscribeEvent
        public static void serverTick(ServerTickEvent.Post event) {
            RerollController.tick(event.getServer());
        }

        @SubscribeEvent
        public static void playerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                RerollController.onPlayerLogout(player);
            }
        }
    }
}
