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
                .set(net.minecraft.core.component.DataComponents.ITEM_NAME, net.minecraft.network.chat.Component.translatable((item == Items.BOOKSHELF ? "block.minecraft." : "item.minecraft.") + name))
                .set(net.minecraft.core.component.DataComponents.MAX_STACK_SIZE, 64)
                .set(net.minecraft.core.component.DataComponents.ITEM_MODEL, net.minecraft.resources.Identifier.withDefaultNamespace(name)).build();
        item.builtInRegistryHolder().bindComponents(components);
        ItemStack stack = new ItemStack(item);
        if (item == Items.ENCHANTED_BOOK) stack.set(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return stack;
    }
    private static Object field(TradeTargetScreen screen, String name) throws Exception {
        var field = TradeTargetScreen.class.getDeclaredField(name); field.setAccessible(true); return field.get(screen);
    }
    private static void choose(TradeTargetScreen screen, int index) throws Exception {
        ((net.minecraft.client.gui.components.Button)((List<?>)field(screen, "cells")).get(index)).onPress(null);
    }
    private static void chooseEnchantment(TradeTargetScreen screen, String id) throws Exception {
        set(screen, "search", net.minecraft.network.chat.Component.translatable("enchantment.minecraft." + id).getString());
        choose(screen, 0);
    }
    private static void chooseItem(TradeTargetScreen screen, net.minecraft.world.item.Item item) throws Exception {
        for (Object entry : (List<?>)field(screen, "cells")) {
            var field = entry.getClass().getDeclaredField("item"); field.setAccessible(true);
            if (((ItemStack)field.get(entry)).is(item)) { ((net.minecraft.client.gui.components.Button)entry).onPress(null); return; }
        }
        throw new AssertionError("Missing item " + item);
    }
    private static void back(TradeTargetScreen screen) throws Exception {
        var method = TradeTargetScreen.class.getDeclaredMethod("back"); method.setAccessible(true); method.invoke(screen);
    }
    private static void set(TradeTargetScreen screen, String name, String value) throws Exception {
        ((net.minecraft.client.gui.components.EditBox)field(screen, name)).setValue(value);
    }
    private static void expect(TradeTargetScreen screen, String stage) throws Exception {
        check(field(screen, "stage").toString().equals(stage), "expected " + stage + ", got " + field(screen, "stage"));
        centered(screen);
    }
    private static void centered(TradeTargetScreen screen) throws Exception {
        int x = (Integer)field(screen, "left"), y = (Integer)field(screen, "top");
        int w = (Integer)field(screen, "panelWidth"), h = (Integer)field(screen, "panelHeight");
        check(Math.abs(2 * x + w - screen.width) <= 1 && Math.abs(2 * y + h - screen.height) <= 1, "panel stays centered");
        for (var child : screen.children()) if (child instanceof net.minecraft.client.gui.components.AbstractWidget widget && widget.visible)
            check(widget.getX() >= x && widget.getY() >= y && widget.getX() + widget.getWidth() <= x + w && widget.getY() + widget.getHeight() <= y + h, "widget inside panel: " + widget.getMessage().getString());
    }
    private static void check(boolean value, String name) { if (!value) throw new AssertionError(name); }
    private static void capture(net.minecraft.client.Minecraft client, String name) {
        Screenshot.grab(Path.of(".").toFile(), name + "-" + client.options.languageCode + ".png", client.gameRenderer.mainRenderTarget(), 1, message -> {});
    }
    private int ticks;
    private boolean started;
    @Override public void onInitializeClient() {
        if (!Boolean.getBoolean("tradeTargetClientSmoke")) return;
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.gui.overlay() != null) return;
            if (!started) {
                started = true;
                client.options.languageCode = System.getProperty("tradeTargetLanguage", "ja_jp");
                client.getLanguageManager().setSelected(System.getProperty("tradeTargetLanguage", "ja_jp"));
                client.options.guiScale().set(2);
                client.resizeGui();
                client.reloadResourcePacks();
                return;
            }
            ticks++;
            org.lwjgl.glfw.GLFW.glfwSetCursorPos(client.getWindow().handle(), 2, 2);
            if (ticks == 20) {
                try { Class.forName("net.minecraft.client.gui.screens.inventory.MerchantScreen"); }
                catch (Exception e) { throw new RuntimeException(e); }
                fixture(Items.WRITABLE_BOOK);
                var candidates = List.of(
                    new TradeCandidate(fixture(Items.ENCHANTED_BOOK), "minecraft:mending", 1, new PriceRange(10, 38), false),
                    new TradeCandidate(fixture(Items.ENCHANTED_BOOK), "minecraft:unbreaking", 1, new PriceRange(5, 19), false),
                    new TradeCandidate(fixture(Items.ENCHANTED_BOOK), "minecraft:unbreaking", 2, new PriceRange(8, 32), false),
                    new TradeCandidate(fixture(Items.ENCHANTED_BOOK), "minecraft:unbreaking", 3, new PriceRange(11, 45), false),
                    new TradeCandidate(fixture(Items.ENCHANTED_BOOK), "minecraft:efficiency", 1, new PriceRange(5, 19), false),
                    new TradeCandidate(fixture(Items.ENCHANTED_BOOK), "minecraft:fortune", 1, new PriceRange(5, 19), false),
                    new TradeCandidate(fixture(Items.ENCHANTED_BOOK), "minecraft:sharpness", 1, new PriceRange(5, 19), false),
                    new TradeCandidate(fixture(Items.ENCHANTED_BOOK), "minecraft:protection", 1, new PriceRange(5, 19), false),
                    new TradeCandidate(fixture(Items.ENCHANTED_BOOK), "minecraft:looting", 1, new PriceRange(5, 19), false),
                    new TradeCandidate(fixture(Items.ENCHANTED_BOOK), "minecraft:binding_curse", 1, new PriceRange(5, 19), false),
                    new TradeCandidate(fixture(Items.ENCHANTED_BOOK), "minecraft:vanishing_curse", 1, new PriceRange(5, 19), false),
                    new TradeCandidate(fixture(Items.ENCHANTED_BOOK), "minecraft:depth_strider", 1, new PriceRange(5, 19), false),
                    new TradeCandidate(fixture(Items.ENCHANTED_BOOK), "minecraft:feather_falling", 1, new PriceRange(5, 19), false),
                    new TradeCandidate(fixture(Items.ENCHANTED_BOOK), "minecraft:fire_aspect", 1, new PriceRange(5, 19), false),
                    new TradeCandidate(fixture(Items.ENCHANTED_BOOK), "minecraft:aqua_affinity", 1, new PriceRange(5, 19), false),
                    new TradeCandidate(fixture(Items.ENCHANTED_BOOK), "minecraft:respiration", 1, new PriceRange(5, 19), false),
                    new TradeCandidate(fixture(Items.ENCHANTED_BOOK), "minecraft:sweeping_edge", 1, new PriceRange(5, 19), false),
                    new TradeCandidate(fixture(Items.ENCHANTED_BOOK), "minecraft:silk_touch", 1, new PriceRange(5, 19), false),
                    new TradeCandidate(fixture(Items.ENCHANTED_BOOK), "minecraft:projectile_protection", 1, new PriceRange(5, 19), false),
                    new TradeCandidate(fixture(Items.ENCHANTED_BOOK), "minecraft:blast_protection", 1, new PriceRange(5, 19), false),
                    new TradeCandidate(fixture(Items.BOOKSHELF), "", 0, PriceRange.fixed(9), false),
                    new TradeCandidate(fixture(Items.PAPER), "", 0, PriceRange.fixed(24), false, true));
                var screen = new TradeTargetScreen(new TitleScreen(), 7);
                screen.applyData(new TradeTargetDataPayload(7, 1, candidates,
                    List.of(candidates.getFirst().rule("test", 10), candidates.get(3).rule("unbreaking", 5, 20), candidates.getLast().rule("paper", 24)), List.of(0, -1, 1), true, ""));
                client.setScreenAndShow(screen);
            }
            if (!(client.gui.screen() instanceof TradeTargetScreen screen)) return;
            try {
                if (ticks == 60) { capture(client, "01-items"); chooseItem(screen, Items.ENCHANTED_BOOK); expect(screen, "TYPE"); }
                if (ticks == 90) { capture(client, "02-types");
                    var next = (net.minecraft.client.gui.components.Button)field(screen, "next");
                    check(next.active && next.visible, "pagination available"); next.onPress(null);
                    check((Integer)field(screen, "page") == 1, "next page");
                    check((Integer)field(screen, "pageSize") == 12, "twelve entries per page");
                    double x = (Integer)field(screen, "left") + 100, y = (Integer)field(screen, "top") + 100;
                    screen.mouseScrolled(x, y, 0, 1); check((Integer)field(screen, "page") == 0, "wheel goes back");
                    for (int i = 0; i < 4; i++) screen.mouseScrolled(x, y, 0, -0.25);
                    check((Integer)field(screen, "page") == 1, "fractional wheel accumulates");
                    screen.mouseScrolled(0, 0, 0, 1); check((Integer)field(screen, "page") == 1, "outside panel does not scroll");
                    screen.mouseScrolled(x, y, 0, -10); check((Integer)field(screen, "page") == 1, "last page clamp");
                    screen.mouseScrolled(x, y, 0, 10); check((Integer)field(screen, "page") == 0, "first page clamp");
                    var fit = TradeTargetScreen.class.getDeclaredMethod("fit", net.minecraft.network.chat.Component.class, int.class); fit.setAccessible(true);
                    String clipped = ((net.minecraft.network.chat.Component) fit.invoke(screen, net.minecraft.network.chat.Component.literal("Curse of Vanishing with a deliberately long suffix"), 60)).getString();
                    check(clipped.endsWith("...") && clipped.chars().allMatch(c -> c < 128), "truncation stays valid ASCII");
                    set(screen, "search", "no_such_enchantment"); check(((List<?>)field(screen, "groups")).isEmpty(), "empty search");
                    chooseEnchantment(screen, "unbreaking"); expect(screen, "LEVEL"); }
                if (ticks == 120) { capture(client, "03-levels"); choose(screen, 2); expect(screen, "PRICE"); set(screen, "maximum", "10"); }
                if (ticks == 150) {
                    capture(client, "04-price");
                    check(((net.minecraft.client.gui.components.Button) field(screen, "save")).active, "upper bound valid");
                    set(screen, "minimum", "11"); check(!((net.minecraft.client.gui.components.Button) field(screen, "save")).active, "inverted bounds rejected");
                    set(screen, "minimum", "0"); check(!((net.minecraft.client.gui.components.Button) field(screen, "save")).active, "zero rejected");
                    set(screen, "minimum", "abc"); check(!((net.minecraft.client.gui.components.Button) field(screen, "save")).active, "non-number rejected");
                    set(screen, "minimum", ""); set(screen, "maximum", "");
                    check(((net.minecraft.client.gui.components.Button) field(screen, "save")).active, "unspecified bounds allowed");
                    back(screen); expect(screen, "LEVEL"); back(screen); expect(screen, "TYPE"); chooseEnchantment(screen, "mending"); expect(screen, "PRICE");
                    back(screen); back(screen); expect(screen, "ITEM"); chooseItem(screen, Items.BOOKSHELF); expect(screen, "PRICE");
                }
                if (ticks == 180) { capture(client, "05-bookshelf"); back(screen); chooseItem(screen, Items.PAPER); expect(screen, "PRICE"); }
                if (ticks == 210) {
                    capture(client, "06-paper"); back(screen);
                    screen.children().stream().filter(e -> e instanceof net.minecraft.client.gui.components.Button b && (b.getMessage().getString().contains("(3)") || b.getMessage().getString().contains("（3）")))
                        .map(e -> (net.minecraft.client.gui.components.Button)e).findFirst().orElseThrow().onPress(null);
                    expect(screen, "RULES");
                }
                if (ticks == 240) { capture(client, "07-rules");
                    var cells = (List<?>)field(screen, "cells");
                    int visible = 0;
                    for (Object cell : cells) if (((net.minecraft.client.gui.components.Button)cell).visible) {
                        var caption = cell.getClass().getDeclaredField("caption"); caption.setAccessible(true);
                        var subtitle = cell.getClass().getDeclaredField("subtitle"); subtitle.setAccessible(true);
                        check(!((net.minecraft.network.chat.Component)caption.get(cell)).getString().isBlank(), "saved row shows target name");
                        check(((net.minecraft.network.chat.Component)subtitle.get(cell)).getString().contains("-" ) || ((net.minecraft.network.chat.Component)subtitle.get(cell)).getString().contains("〜"), "saved row shows bounds");
                        visible++;
                    }
                    check(visible == 3, "all three saved rules are listed");
                    choose(screen, 0); expect(screen, "DETAIL"); }
                if (ticks == 270) { capture(client, "08-detail");
                    screen.resize(320, 240); centered(screen);
                    back(screen); centered(screen);
                    var method = TradeTargetScreen.class.getDeclaredMethod("selectTab", boolean.class); method.setAccessible(true); method.invoke(screen, false);
                    chooseItem(screen, Items.ENCHANTED_BOOK);
                    screen.resize(292, 237); centered(screen);
                    check((Integer)field(screen, "pageSize") == 9, "nine entries in narrow viewport");
                    screen.resize(640, 360); centered(screen);
                    java.nio.file.Files.writeString(Path.of("client-smoke-result-" + client.options.languageCode + ".json"), "{\"status\":\"passed\",\"checks\":\"item/type/level/price navigation, skipped steps, bounds validation, saved rules, pagination, empty search, small viewport, centered widgets, 12 or 9 entries, wheel and trackpad, clean truncation, saved summaries\"}");
                    client.stop();
                }
            } catch (Exception error) { throw new RuntimeException(error); }
        });
    }
}
//#endif
