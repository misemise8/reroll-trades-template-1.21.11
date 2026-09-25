//#if MC >= 260200
package net.misemise.smoke;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.misemise.client.TradeTargetScreen;
import net.misemise.network.TradeTargetDataPayload;
import net.misemise.target.*;
import java.nio.file.Path;
import java.util.List;

public final class TargetClientSmoke implements ClientModInitializer {
    private static ItemStack fixture(net.minecraft.world.item.Item item) {
        String name = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getPath();
        var components = net.minecraft.core.component.DataComponentMap.builder()
                .set(net.minecraft.core.component.DataComponents.ITEM_NAME, net.minecraft.network.chat.Component.translatable("item.minecraft." + name))
                .set(net.minecraft.core.component.DataComponents.MAX_STACK_SIZE, 64).build();
        return new ItemStack(net.minecraft.core.Holder.direct(item, components));
    }
    private int ticks;
    private boolean started;
    @Override public void onInitializeClient() {
        if (!Boolean.getBoolean("tradeTargetClientSmoke")) return;
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.gui.overlay() != null) return;
            if (!started) {
                started = true;
                client.options.languageCode = "ja_jp";
                client.getLanguageManager().setSelected("ja_jp");
                client.options.guiScale().set(2);
                client.resizeGui();
                client.reloadResourcePacks();
                return;
            }
            ticks++;
            if (ticks == 20) {
                try { Class.forName("net.minecraft.client.gui.screens.inventory.MerchantScreen"); }
                catch (Exception e) { throw new RuntimeException(e); }
                var candidates = List.of(
                    new TradeCandidate(fixture(Items.ENCHANTED_BOOK), "", 0, new PriceRange(10, 38), false),
                    new TradeCandidate(fixture(Items.BREAD), "", 0, PriceRange.fixed(1), false),
                    new TradeCandidate(fixture(Items.APPLE), "", 0, PriceRange.fixed(1), false),
                    new TradeCandidate(fixture(Items.DIAMOND_SWORD), "", 0, new PriceRange(13, 27), true));
                var screen = new TradeTargetScreen(new TitleScreen(), 7);
                screen.applyData(new TradeTargetDataPayload(7, 1, candidates,
                    List.of(candidates.getFirst().rule("test", 10)), List.of(0), true, ""));
                client.setScreenAndShow(screen);
            }
            if (ticks == 60) Screenshot.grab(Path.of(".").toFile(), "trade-targets-ja.png", client.gameRenderer.mainRenderTarget(), 1, message -> {});
            if (ticks == 80 && client.gui.screen() instanceof TradeTargetScreen screen) {
                try {
                    var method = TradeTargetScreen.class.getDeclaredMethod("selectTab", boolean.class);
                    method.setAccessible(true);
                    method.invoke(screen, true);
                } catch (Exception e) { throw new RuntimeException(e); }
            }
            if (ticks == 110) Screenshot.grab(Path.of(".").toFile(), "trade-rules-ja.png", client.gameRenderer.mainRenderTarget(), 1, message -> {});
            if (ticks == 130) client.stop();
        });
    }
}
//#endif
