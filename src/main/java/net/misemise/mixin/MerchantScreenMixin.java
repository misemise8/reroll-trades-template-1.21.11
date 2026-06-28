package net.misemise.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import net.misemise.client.RerollScreenAccess;
import net.misemise.config.RerollClientConfig;
import net.misemise.network.RerollStatePayload;
import net.misemise.platform.ClientPlatformServices;
import net.misemise.reroll.RerollAction;
import net.misemise.reroll.RerollBlockReason;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;

@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin extends AbstractContainerScreen<MerchantMenu> implements RerollScreenAccess {

    @Unique
    private Button rerollTrades$rerollButton;

    @Unique
    private Button rerollTrades$undoButton;

    @Unique
    private RerollStatePayload rerollTrades$state;

    private MerchantScreenMixin(MerchantMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void rerollTrades$applyState(RerollStatePayload state) {
        if (state.containerId() != this.menu.containerId) {
            return;
        }
        this.rerollTrades$state = state;
        rerollTrades$updateControls();
    }

    @Inject(method = "init", at = @At("TAIL"), require = 1)
    private void rerollTrades$addButtons(CallbackInfo callbackInfo) {
        int buttonX = this.leftPos + 106;
        int rerollY = this.topPos + 8;
        int undoY = this.topPos + 30;

        this.rerollTrades$rerollButton = Button.builder(
                        Component.literal("\u21BB"),
                        button -> rerollTrades$send(RerollAction.REROLL)
                )
                .bounds(buttonX, rerollY, 18, 18)
                .build();
        this.rerollTrades$rerollButton.visible = false;
        this.addRenderableWidget(this.rerollTrades$rerollButton);

        this.rerollTrades$undoButton = Button.builder(
                        Component.literal("\u21B6"),
                        button -> rerollTrades$send(RerollAction.UNDO)
                )
                .bounds(buttonX, undoY, 18, 18)
                .build();
        this.rerollTrades$undoButton.visible = false;
        this.addRenderableWidget(this.rerollTrades$undoButton);

        ClientPlatformServices.sendAction(RerollAction.REQUEST_STATE, this.menu.containerId);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (rerollTrades$state != null && rerollTrades$state.remainingCooldownTicks() > 0) {
            int remaining = rerollTrades$state.remainingCooldownTicks() - 1;
            RerollBlockReason reason = remaining == 0 && rerollTrades$state.reason() == RerollBlockReason.COOLDOWN
                    ? RerollBlockReason.NONE
                    : rerollTrades$state.reason();
            rerollTrades$state = new RerollStatePayload(
                    rerollTrades$state.containerId(),
                    rerollTrades$state.supported(),
                    reason == RerollBlockReason.NONE,
                    reason,
                    rerollTrades$state.canUndo(),
                    remaining,
                    rerollTrades$state.remainingRerolls(),
                    rerollTrades$state.requireSneaking()
            );
        }
        rerollTrades$updateControls();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (rerollTrades$canRequestReroll() && ClientPlatformServices.matchesRerollKey(event)) {
            rerollTrades$send(RerollAction.REROLL);
            return true;
        }
        return super.keyPressed(event);
    }

    @Unique
    private void rerollTrades$send(RerollAction action) {
        ClientPlatformServices.sendAction(action, this.menu.containerId);
    }

    @Unique
    private boolean rerollTrades$canRequestReroll() {
        if (rerollTrades$state == null || !rerollTrades$state.supported()) {
            return false;
        }
        if (rerollTrades$state.canReroll()) {
            return true;
        }
        if (rerollTrades$state.reason() == RerollBlockReason.MUST_SNEAK) {
            Minecraft minecraft = Minecraft.getInstance();
            return minecraft.player != null && minecraft.player.isShiftKeyDown();
        }
        return false;
    }

    @Unique
    private void rerollTrades$updateControls() {
        if (rerollTrades$rerollButton == null || rerollTrades$undoButton == null) {
            return;
        }

        RerollClientConfig config = RerollClientConfig.get();
        boolean supported = rerollTrades$state != null && rerollTrades$state.supported();
        rerollTrades$rerollButton.visible = supported && config.showRerollButton;
        rerollTrades$undoButton.visible = supported && config.showUndoButton;
        rerollTrades$rerollButton.active = rerollTrades$canRequestReroll();
        rerollTrades$undoButton.active = supported && rerollTrades$state.canUndo();

        rerollTrades$rerollButton.setTooltip(Tooltip.create(rerollTrades$rerollTooltip(config)));
        rerollTrades$undoButton.setTooltip(Tooltip.create(rerollTrades$undoTooltip()));
    }

    @Unique
    private Component rerollTrades$rerollTooltip(RerollClientConfig config) {
        MutableComponent tooltip = Component.translatable("gui.reroll-trades.reroll");
        if (config.showKeyHint) {
            tooltip.append("\n").append(Component.translatable(
                    "gui.reroll-trades.key_hint",
                    ClientPlatformServices.rerollKeyName()
            ));
        }
        if (rerollTrades$state != null && rerollTrades$state.remainingRerolls() >= 0) {
            tooltip.append("\n").append(Component.translatable(
                    "gui.reroll-trades.remaining",
                    rerollTrades$state.remainingRerolls()
            ));
        }
        if (rerollTrades$state != null && rerollTrades$state.reason() != RerollBlockReason.NONE) {
            tooltip.append("\n").append(Component.translatable(
                    "reason.reroll-trades." + rerollTrades$state.reason().name().toLowerCase(Locale.ROOT)
            ));
        }
        return tooltip;
    }

    @Unique
    private Component rerollTrades$undoTooltip() {
        if (rerollTrades$state != null && rerollTrades$state.canUndo()) {
            return Component.translatable("gui.reroll-trades.undo");
        }
        return Component.translatable("reason.reroll-trades.undo_unavailable");
    }
}
