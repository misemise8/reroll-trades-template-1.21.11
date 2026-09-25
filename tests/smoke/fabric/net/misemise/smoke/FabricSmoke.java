package net.misemise.smoke;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
public final class FabricSmoke implements ModInitializer {
    @Override public void onInitialize() { ServerLifecycleEvents.SERVER_STARTED.register(TradeTargetSmoke::run); }
}
