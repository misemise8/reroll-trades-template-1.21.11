package net.misemise.client;

//#if MC >= 260100
import net.minecraft.client.gui.GuiGraphicsExtractor;
//#else
//$$ import net.minecraft.client.gui.GuiGraphics;
//#endif
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.util.FormattedCharSequence;
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
import net.minecraft.world.item.Items;
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

/** A compact, native item picker that keeps the existing merchant menu open. */
public final class TradeTargetScreen extends Screen {
    private static final int MAX_PANEL_WIDTH = 320, MAX_PANEL_HEIGHT = 232;
//#if MC >= 12111
    private static final Identifier PANEL_TEXTURE = Identifier.fromNamespaceAndPath("reroll-trades", "textures/gui/trade_panel.png");
//#else
//$$     private static final ResourceLocation PANEL_TEXTURE = ResourceLocation.fromNamespaceAndPath("reroll-trades", "textures/gui/trade_panel.png");
//#endif
    private enum Stage { ITEM, TYPE, LEVEL, PRICE, RULES, DETAIL }
    private record Route(Stage stage, List<Integer> scope, int page, String query) {}
    private final Screen parent;
    private final int containerId;
    private final List<Route> history = new ArrayList<>();
    private TradeTargetDataPayload data;
    private Stage stage = Stage.ITEM;
    private List<Integer> scope = List.of();
    private List<List<Integer>> groups = List.of();
    private final List<ItemButton> cells = new ArrayList<>();
    private EditBox search, minimum, maximum;
    private Button save, previous, next;
    private StringWidget status, pageLabel;
    private boolean waiting, pendingChange, initialized;
    private int page, selected = -1, detail = -1, waitingTicks, left, top;
    private int panelWidth, panelHeight, pageSize, columns, cellWidth, cellHeight;
    private double scrollRemainder;
    private String query = "", minValue = "", maxValue = "";

    public TradeTargetScreen(Screen parent, int containerId) {
        super(Component.translatable("target.reroll-trades.title"));
        this.parent = parent;
        this.containerId = containerId;
    }

    @Override protected void init() {
        initialized = true;
        panelWidth = Math.min(MAX_PANEL_WIDTH, width - 16);
        panelHeight = Math.min(MAX_PANEL_HEIGHT, height - 8);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        scrollRemainder = 0;
        cells.clear(); search = null; minimum = null; maximum = null; save = null;
        addRenderableOnly((graphics, mouseX, mouseY, tick) -> {
            graphics.fill(left - 1, top - 1, left + panelWidth + 1, top + panelHeight + 1, 0xFF373737);
//#if MC >= 12106
            graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, PANEL_TEXTURE, left, top, 0, 0, panelWidth, panelHeight, 1254, 1254, 1254, 1254);
//#elseif MC >= 12102
//$$             graphics.blit(net.minecraft.client.renderer.RenderType::guiTextured, PANEL_TEXTURE, left, top, 0, 0, panelWidth, panelHeight, 1254, 1254, 1254, 1254);
//#else
//$$             graphics.blit(PANEL_TEXTURE, left, top, panelWidth, panelHeight, 0, 0, 1254, 1254, 1254, 1254);
//#endif
            graphics.fill(left, top, left + panelWidth, top + panelHeight, 0x50C6C6C6);
            graphics.fill(left + 1, top + 1, left + panelWidth - 1, top + 2, 0xFFF3F3F3);
        });
        Component heading = Component.translatable("target.reroll-trades.step." + stage.name().toLowerCase(Locale.ROOT));
        text(38, 6, panelWidth - 76, 18, heading);
        Button back = addRenderableWidget(Button.builder(Component.literal("<"), b -> back()).bounds(left + 8, top + 6, 20, 18).build());
        back.setTooltip(Tooltip.create(Component.translatable("gui.back")));
        Button catalogTab = addRenderableWidget(Button.builder(Component.translatable("target.reroll-trades.catalog"), b -> selectTab(false))
                .bounds(left + 10, top + 28, (panelWidth - 24) / 2, 18).build());
        catalogTab.active = !waiting && stage != Stage.ITEM;
        Button savedTab = addRenderableWidget(Button.builder(Component.translatable("target.reroll-trades.saved_count", data == null ? 0 : data.rules().size()), b -> selectTab(true))
                .bounds(left + panelWidth / 2 + 2, top + 28, (panelWidth - 24) / 2, 18).build());
        savedTab.active = !waiting && data != null && stage != Stage.RULES;
        status = text(10, panelHeight - 14, panelWidth - 20, 12, Component.empty());
        if (stage == Stage.PRICE || stage == Stage.DETAIL) pricePage();
        else gridPage();
        refreshStatus();
        if (data == null && !waiting) request();
    }

    private StringWidget text(int x, int y, int w, int h, Component message) {
        StringWidget widget = new StringWidget(left + x, top + y, w, h, message, font) {
//#if MC >= 260100
            @Override public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tick) {
                graphics.text(font, getMessage(), getX() + (w - font.width(getMessage())) / 2, getY() + (h - 9) / 2, 0xFF404040, false);
            }
//#else
//$$             @Override public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float tick) {
//$$                 graphics.drawString(font, getMessage(), getX() + (w - font.width(getMessage())) / 2, getY() + (h - 9) / 2, 0xFF404040, false);
//$$             }
//#endif
        };
        return addRenderableOnly(widget);
    }

    private void rebuild() { if (initialized) rebuildWidgets(); }

    private void selectTab(boolean saved) {
        if (waiting) return;
        if (saved) navigate(Stage.RULES, List.of());
        else { stage = Stage.ITEM; scope = List.of(); page = 0; query = ""; history.clear(); rebuild(); }
    }

    private void gridPage() {
        search = addRenderableWidget(new EditBox(font, left + 12, top + 50, panelWidth - 24, 16,
                Component.translatable("target.reroll-trades.search")));
        search.setMaxLength(128);
        search.setHint(Component.translatable("target.reroll-trades.search"));
        search.setValue(query);
        search.setResponder(value -> { query = value; page = 0; scrollRemainder = 0; refreshGrid(); });
        columns = stage == Stage.RULES ? 1 : panelWidth >= 300 ? 4 : 3;
        int availableHeight = panelHeight - 106;
        int rowCount = Math.max(1, availableHeight / (stage == Stage.RULES ? 30 : 40));
        pageSize = columns * rowCount;
        cellWidth = (panelWidth - 24) / columns;
        cellHeight = availableHeight / rowCount;
        for (int i = 0; i < pageSize; i++) {
            int cell = i;
            cells.add(addRenderableWidget(new ItemButton(0, 0, cellWidth - 4, cellHeight - 2,
                    ItemStack.EMPTY, Component.empty(), Component.empty(), () -> choose(cell))));
        }
        previous = addRenderableWidget(Button.builder(Component.literal("<"), b -> changePage(-1))
                .bounds(left + 12, top + panelHeight - 34, 20, 18).build());
        previous.setTooltip(Tooltip.create(Component.translatable("target.reroll-trades.previous")));
        next = addRenderableWidget(Button.builder(Component.literal(">"), b -> changePage(1))
                .bounds(left + panelWidth - 32, top + panelHeight - 34, 20, 18).build());
        next.setTooltip(Tooltip.create(Component.translatable("target.reroll-trades.next")));
        pageLabel = text(36, panelHeight - 34, panelWidth - 72, 18, Component.empty());
        refreshGrid();
    }

    private void changePage(int direction) {
        if (waiting || search == null) return;
        int nextPage = Math.max(0, Math.min(page + direction, Math.max(0, (groups.size() - 1) / pageSize)));
        if (nextPage != page) { page = nextPage; refreshGrid(); }
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (search == null || waiting || mouseX < left || mouseX >= left + panelWidth || mouseY < top || mouseY >= top + panelHeight)
            return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
        if (vertical == 0) return false;
        if (Math.signum(scrollRemainder) != Math.signum(vertical)) scrollRemainder = 0;
        scrollRemainder += vertical;
        if (Math.abs(scrollRemainder) >= 1) {
            int pages = (int) scrollRemainder;
            scrollRemainder -= pages;
            changePage(-pages);
        }
        return true;
    }

    private void refreshGrid() {
        if (search == null) return;
        groups = grouped();
        int pages = Math.max(1, (groups.size() + pageSize - 1) / pageSize);
        page = Math.max(0, Math.min(page, pages - 1));
        int visibleCount = Math.min(pageSize, groups.size() - page * pageSize);
        int visibleRows = Math.max(1, (visibleCount + columns - 1) / columns);
        int gridY = top + 70 + (panelHeight - 106 - visibleRows * cellHeight) / 2;
        for (int cell = 0; cell < cells.size(); cell++) {
            ItemButton button = cells.get(cell);
            int index = page * pageSize + cell;
            button.visible = index < groups.size();
            button.active = !waiting;
            if (!button.visible) continue;
            int rowItems = Math.min(columns, visibleCount - cell / columns * columns);
            button.setX(left + (panelWidth - rowItems * cellWidth) / 2 + cell % columns * cellWidth + 2);
            button.setY(gridY + cell / columns * cellHeight);
            int entry = groups.get(index).getFirst();
            TradeRule rule = stage == Stage.RULES ? data.rules().get(entry) : data.catalog().get(entry).rule("", 999);
            Component name = stage == Stage.ITEM ? rule.template().getHoverName() : label(rule.template(), rule.enchantment(), rule.level(), stage != Stage.TYPE);
            Component caption = switch (stage) {
                case ITEM -> Component.empty();
                case LEVEL -> levelLabel(rule.level());
                case RULES -> name;
                default -> rule.enchantment().isEmpty() && data.catalog().get(entry).enchantedEquipment()
                        ? Component.translatable("target.reroll-trades.any_enchantment") : name;
            };
            Component tooltip = name.copy().append("\n").append(Component.translatable(rule.buying() ? "target.reroll-trades.buying" : "target.reroll-trades.selling"));
            if (stage == Stage.RULES) tooltip = tooltip.copy().append("\n").append(Component.translatable("target.reroll-trades.condition", rule.minPrice(), rule.maxEmeralds()));
            button.setContent(rule.template(), tooltip, caption);
            button.savedRow = stage == Stage.RULES;
            if (button.savedRow) {
                Component state = Component.translatable(data.lockedSlots().get(entry) < 0 ? "target.reroll-trades.waiting" : "target.reroll-trades.locked", data.lockedSlots().get(entry) + 1);
                button.subtitle = Component.translatable(rule.buying() ? "target.reroll-trades.saved_quantity" : "target.reroll-trades.saved_price", rule.minPrice(), rule.maxEmeralds()).append("  |  ").append(state);
            }
        }
        previous.visible = next.visible = pages > 1;
        previous.active = !waiting && page > 0; next.active = !waiting && page + 1 < pages;
        pageLabel.setMessage(Component.translatable(groups.isEmpty() ? "target.reroll-trades.empty" : "target.reroll-trades.pages", page + 1, pages));
        // Keep the field while filtering, even if the result fits on one page.
        search.visible = stage == Stage.TYPE || stage == Stage.RULES || (data != null && stage == Stage.ITEM && itemCount() > pageSize);
    }

    private int itemCount() {
        return (int) data.catalog().stream().map(c -> c.template().getItem().toString() + c.buying()).distinct().count();
    }

    private List<List<Integer>> grouped() {
        if (data == null) return List.of();
        List<List<Integer>> result = new ArrayList<>();
        List<Integer> indices = stage == Stage.ITEM || stage == Stage.RULES
                ? java.util.stream.IntStream.range(0, stage == Stage.RULES ? data.rules().size() : data.catalog().size()).boxed().toList() : scope;
        String needle = query.toLowerCase(Locale.ROOT);
        for (int index : indices) {
            TradeRule rule = stage == Stage.RULES ? data.rules().get(index) : data.catalog().get(index).rule("", 999);
            if (!(rule.template().getHoverName().getString() + " " + label(rule.template(), rule.enchantment(), rule.level(), true).getString()).toLowerCase(Locale.ROOT).contains(needle)) continue;
            List<Integer> group = null;
            if (stage == Stage.ITEM || stage == Stage.TYPE) for (List<Integer> candidate : result) {
                TradeCandidate first = data.catalog().get(candidate.getFirst());
                boolean same = stage == Stage.ITEM ? first.template().is(rule.template().getItem()) && first.buying() == rule.buying()
                        : ItemStack.isSameItemSameComponents(first.template(), rule.template()) && first.enchantment().equals(rule.enchantment());
                if (same) { group = candidate; break; }
            }
            if (group == null) { group = new ArrayList<>(); result.add(group); }
            group.add(index);
        }
        if (stage == Stage.LEVEL) result.sort(Comparator.comparingInt(g -> data.catalog().get(g.getFirst()).level()));
        else if (stage != Stage.RULES) result.sort(Comparator.comparing(g -> {
            TradeCandidate c = data.catalog().get(g.getFirst());
            return (stage == Stage.ITEM ? c.template().getHoverName() : label(c.template(), c.enchantment(), c.level(), false)).getString();
        }));
        return result;
    }

    private void choose(int cell) {
        int index = page * pageSize + cell;
        if (waiting || index >= groups.size()) return;
        List<Integer> entries = groups.get(index);
        if (stage == Stage.RULES) { detail = entries.getFirst(); navigate(Stage.DETAIL, List.of()); return; }
        if (stage == Stage.ITEM) {
            List<Integer> types = new ArrayList<>();
            for (int entry : entries) {
                TradeCandidate c = data.catalog().get(entry);
                if (types.stream().noneMatch(i -> { TradeCandidate a = data.catalog().get(i); return a.enchantment().equals(c.enchantment()) && ItemStack.isSameItemSameComponents(a.template(), c.template()); })) types.add(entry);
            }
            if (types.size() > 1) { navigate(Stage.TYPE, entries); return; }
        }
        if (entries.size() > 1 && stage != Stage.LEVEL) { navigate(Stage.LEVEL, entries); return; }
        selected = entries.getFirst();
        TradeCandidate candidate = data.catalog().get(selected);
        TradeRule existing = data.rules().stream().filter(r -> r.sameTarget(candidate.rule("", 999))).findFirst().orElse(null);
        minValue = existing == null || existing.minPrice() == 1 ? "" : Integer.toString(existing.minPrice());
        maxValue = existing == null ? "" : Integer.toString(existing.maxEmeralds());
        navigate(Stage.PRICE, entries);
    }

    private void navigate(Stage nextStage, List<Integer> entries) {
        history.add(new Route(stage, scope, page, query));
        stage = nextStage; scope = entries; page = 0; query = ""; scrollRemainder = 0;
        rebuild();
    }

    private void back() {
        if (waiting) return;
        if (history.isEmpty()) { onClose(); return; }
        Route route = history.removeLast();
        stage = route.stage(); scope = route.scope(); page = route.page(); query = route.query(); rebuild();
    }

    private void pricePage() {
        if (data == null) return;
        boolean saved = stage == Stage.DETAIL;
        if (saved && (detail < 0 || detail >= data.rules().size()) || !saved && (selected < 0 || selected >= data.catalog().size())) return;
        TradeRule rule = saved ? data.rules().get(detail) : data.catalog().get(selected).rule("", 999);
        Component name = label(rule.template(), rule.enchantment(), rule.level(), true);
        int contentTop = 50 + Math.max(0, (panelHeight - 68 - 152) / 2);
        int center = panelWidth / 2;
        addRenderableWidget(new ItemButton(left + center - 12, top + contentTop, 24, 24, rule.template(), name, Component.empty(), () -> {}));
        StringWidget itemLabel = text(12, contentTop + 26, panelWidth - 24, 12, fit(name, panelWidth - 24));
        itemLabel.setTooltip(Tooltip.create(name));
        Component range = saved ? Component.translatable(data.lockedSlots().get(detail) < 0 ? "target.reroll-trades.waiting" : "target.reroll-trades.locked", data.lockedSlots().get(detail) + 1)
                : range(data.catalog().get(selected).price(), rule.buying());
        StringWidget bounds = text(12, contentTop + 42, panelWidth - 24, 12, fit(range, panelWidth - 24));
        bounds.setTooltip(Tooltip.create(range.copy().append("\n").append(Component.translatable(rule.buying() ? "target.reroll-trades.buy_range_hint" : "target.reroll-trades.range_hint"))));
        text(center - 84, contentTop + 62, 76, 10, Component.translatable("target.reroll-trades.minimum"));
        text(center + 8, contentTop + 62, 76, 10, Component.translatable("target.reroll-trades.maximum_short"));
        minimum = addRenderableWidget(new EditBox(font, left + center - 84, top + contentTop + 75, 76, 18, Component.translatable("target.reroll-trades.minimum")));
        maximum = addRenderableWidget(new EditBox(font, left + center + 8, top + contentTop + 75, 76, 18, Component.translatable("target.reroll-trades.maximum_short")));
        for (EditBox box : List.of(minimum, maximum)) { box.setMaxLength(3); }
        minimum.setHint(Component.translatable("target.reroll-trades.unset"));
        maximum.setHint(Component.translatable("target.reroll-trades.unset"));
        minimum.setValue(saved ? Integer.toString(rule.minPrice()) : minValue);
        maximum.setValue(saved ? Integer.toString(rule.maxEmeralds()) : maxValue);
        minimum.setEditable(!saved); maximum.setEditable(!saved);
        minimum.setResponder(value -> { minValue = value; refreshStatus(); });
        maximum.setResponder(value -> { maxValue = value; refreshStatus(); });
        text(12, contentTop + 100, panelWidth - 24, 12, Component.translatable(rule.buying() ? "target.reroll-trades.quantity_unit" : "target.reroll-trades.emerald_unit"));
        save = addRenderableWidget(Button.builder(Component.translatable(saved ? "target.reroll-trades.remove" : "target.reroll-trades.add"), b -> submit(saved))
                .bounds(left + center - 50, top + contentTop + 122, 100, 20).build());
        save.setTooltip(Tooltip.create(Component.translatable(saved ? "target.reroll-trades.remove_hint" : "target.reroll-trades.price_hint")));
    }

    private int price(String text, int fallback) {
        if (text.isBlank()) return fallback;
        try { int value = Integer.parseInt(text); return value >= 1 && value <= 999 ? value : -1; }
        catch (NumberFormatException ignored) { return -1; }
    }

    private boolean validPrice() { return price(minValue, 1) > 0 && price(maxValue, 999) >= price(minValue, 1); }

    private void refreshStatus() {
        if (status == null) return;
        if (save != null) save.active = !waiting && data != null && data.editable() && (stage == Stage.DETAIL || validPrice());
        Component message = Component.empty();
        if (waiting) message = Component.translatable("target.reroll-trades.loading");
        else if (data != null && !data.editable()) message = Component.translatable("reason.reroll-trades.already_traded");
        else if (data != null && !data.message().isEmpty()) message = Component.translatable(data.message());
        else if (stage == Stage.PRICE && !validPrice()) message = Component.translatable("target.reroll-trades.invalid_price");
        else if (stage == Stage.PRICE && selected >= 0 && data.catalog().get(selected).price().known() && price(maxValue, 999) < data.catalog().get(selected).price().min())
            message = Component.translatable("target.reroll-trades.discount_needed");
        status.setMessage(fit(message, panelWidth - 20));
        status.setTooltip(Tooltip.create(message));
    }

    private void submit(boolean remove) {
        if (save == null || !save.active || data == null) return;
        waiting = true; pendingChange = true; waitingTicks = 0;
        ClientPlatformServices.sendTargetAction(new TradeTargetActionPayload(containerId,
                remove ? TradeTargetActionPayload.REMOVE : TradeTargetActionPayload.ADD, data.revision(),
                remove ? detail : selected, price(maxValue, 999), price(minValue, 1)));
        rebuild();
    }

    public void applyData(TradeTargetDataPayload payload) {
        if (payload.containerId() != containerId) return;
        boolean changedCatalog = data != null && !sameCatalog(data.catalog(), payload.catalog());
        boolean completed = pendingChange && data != null && payload.revision() != data.revision();
        data = payload; waiting = false; pendingChange = false; waitingTicks = 0;
        if (changedCatalog || completed) {
            stage = completed ? Stage.RULES : Stage.ITEM;
            scope = List.of(); page = 0; query = ""; selected = -1; detail = -1; history.clear();
            if (completed) history.add(new Route(Stage.ITEM, List.of(), 0, ""));
        }
        rebuild();
    }

    private static boolean sameCatalog(List<TradeCandidate> a, List<TradeCandidate> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) if (!a.get(i).rule("", 999).sameTarget(b.get(i).rule("", 999))) return false;
        return true;
    }

    public void applyState(RerollStatePayload state) {
        if (parent instanceof RerollScreenAccess access) access.rerollTrades$applyState(state);
    }

    private void request() {
        waiting = true; waitingTicks = 0;
        ClientPlatformServices.sendTargetAction(new TradeTargetActionPayload(containerId, TradeTargetActionPayload.REQUEST, 0, 0, 0));
        refreshStatus();
    }

    private Component label(ItemStack item, String enchantment, int level, boolean includeLevel) {
        if (!enchantment.isEmpty()) {
            if (minecraft != null && minecraft.level != null) try {
//#if MC >= 12111
                var id = Identifier.parse(enchantment);
//#else
//$$                 var id = ResourceLocation.parse(enchantment);
//#endif
                var holder = minecraft.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(ResourceKey.create(Registries.ENCHANTMENT, id));
                if (holder.isPresent()) return includeLevel ? Enchantment.getFullname(holder.get(), level) : holder.get().value().description();
            } catch (RuntimeException ignored) { }
            Component name = Component.translatable("enchantment." + enchantment.replace(':', '.'));
            return includeLevel ? name.copy().append(" ").append(levelLabel(level)) : name;
        }
        var name = item.getHoverName().copy();
        var stew = item.get(DataComponents.SUSPICIOUS_STEW_EFFECTS);
        if (stew != null) for (var effect : stew.effects()) name.append(" / ").append(effect.effect().value().getDisplayName());
        return name;
    }

    private static Component levelLabel(int level) {
        return level > 0 && level <= 10 ? Component.translatable("enchantment.level." + level) : Component.literal(Integer.toString(level));
    }

    private Component fit(Component message, int width) {
        return font.width(message) <= width ? message : Component.literal(font.plainSubstrByWidth(message.getString(), width - font.width("...")) + "...");
    }

    private List<FormattedCharSequence> captionLines(Component caption, int width) {
        if (caption.getString().isEmpty()) return List.of();
        var lines = font.split(caption, width);
        if (lines.size() <= 2) return lines;
        // Keep the final line bounded even for long modded names. ASCII dots cannot be misdecoded.
        return List.of(lines.getFirst(), Component.literal("...").getVisualOrderText());
    }

    private static Component range(PriceRange price, boolean buying) {
        return price.known() ? Component.translatable(buying ? "target.reroll-trades.quantity_range" : "target.reroll-trades.range", price.min(), price.max())
                : Component.translatable("target.reroll-trades.range_unknown");
    }

    private final class ItemButton extends Button {
        private ItemStack item;
        private Component caption;
        private Component subtitle = Component.empty();
        private boolean savedRow;
        private List<FormattedCharSequence> captionLines = List.of();
        ItemButton(int x, int y, int w, int h, ItemStack item, Component name, Component caption, Runnable press) {
            super(x, y, w, h, name, b -> press.run(), DEFAULT_NARRATION);
            setContent(item, name, caption);
        }
        void setContent(ItemStack item, Component name, Component caption) {
            this.item = item; this.caption = caption;
            captionLines = captionLines(caption, getWidth());
            setMessage(name); setTooltip(Tooltip.create(name));
        }
//#if MC >= 260100
        @Override protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tick) {
//#elseif MC >= 12111
//$$         @Override protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float tick) {
//#else
//$$         @Override public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float tick) {
//#endif
            boolean hover = isHoveredOrFocused() && active;
            if (savedRow) {
                graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), hover ? 0x55FFFFFF : 0x22808080);
                int x = getX() + 5, y = getY() + (getHeight() - 16) / 2;
//#if MC >= 260100
                graphics.item(item, x, y);
                graphics.text(font, fit(caption, getWidth() - 36), getX() + 30, getY() + 4, 0xFF303030, false);
                graphics.text(font, fit(subtitle, getWidth() - 36), getX() + 30, getY() + 16, 0xFF505050, false);
//#else
//$$                 graphics.renderItem(item, x, y);
//$$                 graphics.drawString(font, fit(caption, getWidth() - 36), getX() + 30, getY() + 4, 0xFF303030, false);
//$$                 graphics.drawString(font, fit(subtitle, getWidth() - 36), getX() + 30, getY() + 16, 0xFF505050, false);
//#endif
                return;
            }
            int contentHeight = 20 + (captionLines.isEmpty() ? 0 : 2 + 9 * (stage == Stage.TYPE ? 2 : captionLines.size()));
            int x = getX() + (getWidth() - 20) / 2, y = getY() + (getHeight() - contentHeight) / 2;
            graphics.fill(x, y, x + 20, y + 20, hover ? 0xFFF5F5F5 : 0xFF888888);
            graphics.fill(x + 1, y + 1, x + 19, y + 19, hover ? 0xFFBBBBBB : 0xFFB2B2B2);
//#if MC >= 260100
            graphics.item(item, x + 2, y + 2);
//#else
//$$             graphics.renderItem(item, x + 2, y + 2);
//#endif
            for (int i = 0; i < captionLines.size(); i++) {
                FormattedCharSequence line = captionLines.get(i);
                int labelX = getX() + (getWidth() - font.width(line)) / 2;
//#if MC >= 260100
                graphics.text(font, line, labelX, y + 22 + i * 9, 0xFF404040, false);
//#else
//$$                 graphics.drawString(font, line, labelX, y + 22 + i * 9, 0xFF404040, false);
//#endif
            }
        }
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
