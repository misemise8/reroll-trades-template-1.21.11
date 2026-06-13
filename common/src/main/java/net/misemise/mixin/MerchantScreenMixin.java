package net.misemise.mixin;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import net.misemise.IRerollLockable;
import net.misemise.platform.PlatformServices;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin extends AbstractContainerScreen<MerchantMenu> implements IRerollLockable {

    @Unique
    private Button rerollTrades$button;

    @Unique
    private boolean rerollTrades$locked = false;

    private MerchantScreenMixin(MerchantMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Unique
    public void rerollTrades$lock() {
        rerollTrades$locked = true;
        if (rerollTrades$button != null) {
            rerollTrades$button.active = false;
        }
    }

    @Unique
    public void rerollTrades$unlock() {
        rerollTrades$locked = false;
        if (rerollTrades$button != null) {
            rerollTrades$button.active = true;
        }
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void rerollTrades$addButton(CallbackInfo callbackInfo) {
        this.rerollTrades$button = Button.builder(
                        Component.literal("\u21BB"),
                        button -> {
                            if (!rerollTrades$locked) {
                                PlatformServices.sendRerollRequest();
                                rerollTrades$lock();
                            }
                        }
                )
                .bounds(this.leftPos - 24, this.topPos + 8, 22, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.reroll-trades.reroll")))
                .build();

        this.rerollTrades$button.active = !rerollTrades$locked;
        this.addRenderableWidget(this.rerollTrades$button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!rerollTrades$locked && PlatformServices.matchesRerollKey(keyCode, scanCode)) {
            PlatformServices.sendRerollRequest();
            rerollTrades$lock();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
