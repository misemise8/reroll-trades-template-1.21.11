package net.misemise.smoke;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
@Mod("trade_target_smoke")
public final class NeoForgeSmoke {
    public NeoForgeSmoke() { NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) -> TradeTargetSmoke.run(event.getServer())); }
}
