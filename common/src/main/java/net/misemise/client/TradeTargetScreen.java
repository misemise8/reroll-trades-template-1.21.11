package net.misemise.client;

import net.minecraft.client.Minecraft;
//#if MC >= 260100
import net.minecraft.client.gui.GuiGraphicsExtractor;
//#else
//$$ import net.minecraft.client.gui.GuiGraphics;
//#endif
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
//#if MC >= 12111
import net.minecraft.resources.Identifier;
//#else
//$$ import net.minecraft.resources.ResourceLocation;
//#endif
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.misemise.network.TradeTargetActionPayload;
import net.misemise.network.TradeTargetDataPayload;
import net.misemise.network.RerollStatePayload;
import net.misemise.platform.ClientPlatformServices;
import net.misemise.target.PriceRange;
import net.misemise.target.TradeCandidate;
import net.misemise.target.TradeRule;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Keeps the existing merchant menu open while editing its server-owned targets. */
public final class TradeTargetScreen extends Screen {
    private final Screen parent;
    private final int containerId;
    private TradeTargetDataPayload data;
    private final List<Button> rows = new ArrayList<>();
    private final List<Button> removeButtons = new ArrayList<>();
    private List<Integer> filtered = List.of();
    private EditBox search, maximum;
    private StringWidget pageLabel, status, maximumLabel;
    private Button previous, next, add, catalogTab, rulesTab;
    private boolean rulesMode, waiting;
    private int page, selected = -1, listWidth, waitingTicks;
    private String query = "", maxValue = "64";

    public TradeTargetScreen(Screen parent, int containerId) {
        super(Component.translatable("target.reroll-trades.title"));
        this.parent = parent;
        this.containerId = containerId;
    }

    @Override protected void init() {
        rows.clear(); removeButtons.clear();
        int width = Math.max(240, Math.min(620, this.width - 24));
        int x = (this.width - width) / 2;
        int y = 16;
        listWidth = width;
        this.addRenderableOnly(new StringWidget(x, y, width, 20, title, font));
        catalogTab = addRenderableWidget(Button.builder(Component.translatable("target.reroll-trades.catalog"), button -> selectTab(false))
                .bounds(x, y + 24, width / 2 - 2, 20).build());
        rulesTab = addRenderableWidget(Button.builder(Component.translatable("target.reroll-trades.rules"), button -> selectTab(true))
                .bounds(x + width / 2 + 2, y + 24, width / 2 - 2, 20).build());
        search = addRenderableWidget(new EditBox(font, x, y + 50, width, 20, Component.translatable("target.reroll-trades.search")));
        search.setMaxLength(128);
        search.setHint(Component.translatable("target.reroll-trades.search"));
        search.setValue(query);
        search.setResponder(value -> { query = value; page = 0; filter(); });
        int count = Math.max(2, Math.min(12, (this.height - 190) / 24));
        int firstRow = y + 76;
        for (int row = 0; row < count; row++) {
            final int index = row;
            Button button = addRenderableWidget(Button.builder(Component.empty(), ignored -> selectRow(index))
                    .bounds(x, firstRow + row * 24, width, 20).build());
            rows.add(button);
            removeButtons.add(addRenderableWidget(Button.builder(Component.translatable("target.reroll-trades.remove"), ignored -> removeRow(index))
                    .bounds(x + width - 54, firstRow + row * 24, 54, 20).build()));
        }
        int controlsY = firstRow + count * 24 + 2;
        previous = addRenderableWidget(Button.builder(Component.literal("<"), b -> { page--; refreshRows(); }).bounds(x, controlsY, 28, 20).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), b -> { page++; refreshRows(); }).bounds(x + width - 28, controlsY, 28, 20).build());
        pageLabel = addRenderableOnly(new StringWidget(x + 32, controlsY, width - 64, 20, Component.empty(), font));
        maximumLabel = addRenderableOnly(new StringWidget(x, controlsY + 27, 100, 20, Component.translatable("target.reroll-trades.maximum"), font));
        maximum = addRenderableWidget(new EditBox(font, x + 104, controlsY + 27, 64, 20, Component.translatable("target.reroll-trades.maximum")));
        maximum.setMaxLength(3);
        maximum.setValue(maxValue);
        maximum.setResponder(value -> { maxValue = value; refreshControls(); });
        add = addRenderableWidget(Button.builder(Component.translatable("target.reroll-trades.add"), b -> addSelected())
                .bounds(x + 176, controlsY + 27, width - 176, 20).build());
        status = addRenderableOnly(new StringWidget(x, controlsY + 51, width, 14, Component.empty(), font));
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> onClose())
                .bounds(x + (width - 120) / 2, controlsY + 72, 120, 20).build());
        filter();
        if (data == null) request();
    }

    public void applyData(TradeTargetDataPayload payload) {
        if (payload.containerId() != containerId) return;
        TradeRule selection = data != null && selected >= 0 && selected < data.catalog().size()
                ? data.catalog().get(selected).rule("", 999) : null;
        data = payload;
        waiting = false;
        waitingTicks = 0;
        if (selection != null) {
            selected = -1;
            for (int i = 0; i < data.catalog().size(); i++) if (selection.sameTarget(data.catalog().get(i).rule("", 999))) { selected = i; break; }
        }
        filter();
    }

    public void applyState(RerollStatePayload state) {
        if (parent instanceof RerollScreenAccess access) access.rerollTrades$applyState(state);
    }

    private void request() {
        waiting = true;
        waitingTicks = 0;
        ClientPlatformServices.sendTargetAction(new TradeTargetActionPayload(containerId, TradeTargetActionPayload.REQUEST, 0, 0, 0));
    }

    private void selectTab(boolean rules) { rulesMode = rules; page = 0; refreshRows(); }

    private void filter() {
        if (data == null) { filtered = List.of(); refreshRows(); return; }
        String needle = query.toLowerCase(Locale.ROOT);
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < data.catalog().size(); i++) {
            TradeCandidate candidate = data.catalog().get(i);
            String text = label(candidate.template(), candidate.enchantment(), candidate.level()).getString().toLowerCase(Locale.ROOT);
            if (text.contains(needle) || candidate.enchantment().contains(needle)) indices.add(i);
        }
        indices.sort(Comparator.comparing(i -> label(data.catalog().get(i).template(), data.catalog().get(i).enchantment(), data.catalog().get(i).level()).getString()));
        filtered = List.copyOf(indices);
        refreshRows();
    }

    private void refreshRows() {
        if (search == null || rows.isEmpty()) return;
        int total = data == null ? 0 : rulesMode ? data.rules().size() : filtered.size();
        int pages = Math.max(1, (total + rows.size() - 1) / rows.size());
        page = Math.max(0, Math.min(page, pages - 1));
        for (int row = 0; row < rows.size(); row++) {
            int index = page * rows.size() + row;
            Button button = rows.get(row), remove = removeButtons.get(row);
            button.visible = index < total;
            remove.visible = rulesMode && button.visible;
            remove.active = !waiting && data != null && data.editable();
            button.setWidth(rulesMode ? listWidth - 60 : listWidth);
            button.active = !rulesMode && !waiting;
            if (!button.visible) continue;
            Component full;
            if (rulesMode) {
                TradeRule rule = data.rules().get(index);
                int slot = data.lockedSlots().get(index);
                full = label(rule.template(), rule.enchantment(), rule.level()).copy()
                        .append(Component.translatable("target.reroll-trades.rule_price", rule.maxEmeralds()))
                        .append("  ").append(Component.translatable(slot < 0 ? "target.reroll-trades.waiting" : "target.reroll-trades.locked", slot + 1));
            } else {
                int catalogIndex = filtered.get(index);
                TradeCandidate candidate = data.catalog().get(catalogIndex);
                full = Component.literal(catalogIndex == selected ? "> " : "")
                        .append(label(candidate.template(), candidate.enchantment(), candidate.level()))
                        .append("  ").append(range(candidate.price()));
            }
            button.setMessage(Component.literal(font.plainSubstrByWidth(full.getString(), button.getWidth() - 12)));
            button.setTooltip(Tooltip.create(full.copy().append("\n").append(Component.translatable(
                    rulesMode ? "target.reroll-trades.remove_hint" : "target.reroll-trades.range_hint"))));
        }
        search.visible = !rulesMode;
        maximum.visible = !rulesMode;
        maximumLabel.visible = !rulesMode;
        add.visible = !rulesMode;
        catalogTab.active = rulesMode;
        rulesTab.active = !rulesMode;
        previous.active = page > 0;
        next.active = page + 1 < pages;
        pageLabel.setMessage(Component.translatable("target.reroll-trades.page", page + 1, pages, total));
        refreshControls();
    }

    private void refreshControls() {
        if (add == null || status == null) return;
        int price = maxPrice();
        add.active = !waiting && data != null && data.editable() && selected >= 0 && price > 0;
        Component message = Component.translatable("target.reroll-trades.price_hint");
        if (waiting) message = Component.translatable("target.reroll-trades.loading");
        else if (data != null && !data.editable()) message = Component.translatable("reason.reroll-trades.already_traded");
        else if (data != null && !data.message().isEmpty()) message = Component.translatable(data.message());
        else if (price < 1 && !rulesMode) message = Component.translatable("target.reroll-trades.invalid_price");
        else if (!rulesMode && data != null && selected >= 0 && data.catalog().get(selected).price().known()
                && price < data.catalog().get(selected).price().min()) message = Component.translatable("target.reroll-trades.discount_needed");
        status.setMessage(Component.literal(font.plainSubstrByWidth(message.getString(), listWidth)));
        status.setTooltip(Tooltip.create(message));
    }

    private int maxPrice() {
        try { int value = Integer.parseInt(maxValue); return value > 0 && value <= 999 ? value : -1; }
        catch (NumberFormatException ignored) { return -1; }
    }

    private void selectRow(int row) {
        if (rulesMode || data == null) return;
        int index = page * rows.size() + row;
        if (index < filtered.size()) { selected = filtered.get(index); refreshRows(); }
    }

    private void addSelected() {
        if (!add.active || data == null) return;
        waiting = true;
        ClientPlatformServices.sendTargetAction(new TradeTargetActionPayload(containerId, TradeTargetActionPayload.ADD, data.revision(), selected, maxPrice()));
        refreshRows();
    }

    private void removeRow(int row) {
        if (data == null || waiting || !data.editable()) return;
        int index = page * rows.size() + row;
        if (index >= data.rules().size()) return;
        waiting = true;
        ClientPlatformServices.sendTargetAction(new TradeTargetActionPayload(containerId, TradeTargetActionPayload.REMOVE, data.revision(), index, 0));
        refreshRows();
    }

    private Component label(ItemStack item, String enchantment, int level) {
        if (!enchantment.isEmpty() && minecraft != null && minecraft.level != null) {
            try {
//#if MC >= 12111
                var id = Identifier.parse(enchantment);
//#else
//$$                 var id = ResourceLocation.parse(enchantment);
//#endif
                var holder = minecraft.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(ResourceKey.create(Registries.ENCHANTMENT, id));
                if (holder.isPresent()) return item.getHoverName().copy().append(" / ").append(Enchantment.getFullname(holder.get(), level));
            } catch (RuntimeException ignored) { }
        }
        var name = item.getHoverName().copy();
        var stew = item.get(DataComponents.SUSPICIOUS_STEW_EFFECTS);
        if (stew != null) for (var effect : stew.effects()) name.append(" / ").append(effect.effect().value().getDisplayName());
        return name;
    }

    private static Component range(PriceRange range) {
        return range.known() ? Component.translatable("target.reroll-trades.range", range.min(), range.max())
                : Component.translatable("target.reroll-trades.range_unknown");
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override public void tick() {
        if (minecraft == null || minecraft.player == null || minecraft.player.containerMenu.containerId != containerId) {
            if (parent instanceof RerollScreenAccess access) access.rerollTrades$setTargetScreenOpen(false);
            show(null);
        } else if (waiting && ++waitingTicks >= 20) request();
    }

    @Override public void onClose() {
        if (parent instanceof RerollScreenAccess access) access.rerollTrades$setTargetScreenOpen(false);
        if (minecraft != null && minecraft.player != null && minecraft.player.containerMenu.containerId == containerId) show(parent);
        else show(null);
    }

    private void show(Screen screen) {
        if (minecraft == null) return;
//#if MC >= 12109
        minecraft.setScreenAndShow(screen);
//#else
//$$         minecraft.setScreen(screen);
//#endif
    }

//#if MC < 12106
//$$     @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
//$$         renderBackground(graphics, mouseX, mouseY, partialTick);
//$$         super.render(graphics, mouseX, mouseY, partialTick);
//$$     }
//#endif
}
