package net.misemise.mixin;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.text.Text;
import net.misemise.IRerollLockable;
import net.misemise.RerollTradesClient;
import net.misemise.network.RerollTradesPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin extends HandledScreen<MerchantScreenHandler>
        implements IRerollLockable {

    private MerchantScreenMixin() {
        super(null, null, null);
    }

    @Unique
    private ButtonWidget rerollButton;

    /**
     * One-way latch: set by server signal (RerollLockedPayload) when the player
     * has traded with this villager. Never reset client-side — a new screen
     * opening gets fresh state and server re-sends the signal if still locked.
     */
    @Unique
    private boolean rerollLocked = false;

    /**
     * Called by RerollTradesClient when RerollLockedPayload is received (permanent
     * lock).
     */
    @Unique
    public void rerollTrades$lock() {
        rerollLocked = true;
        if (rerollButton != null)
            rerollButton.active = false;
    }

    /**
     * Called by RerollTradesClient when RerollRejectPayload is received.
     * Re-enables the button after a temporary server-side rejection.
     */
    @Unique
    public void rerollTrades$unlock() {
        rerollLocked = false;
        if (rerollButton != null)
            rerollButton.active = true;
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void rerollTrades$addRerollButton(CallbackInfo ci) {
        rerollButton = ButtonWidget.builder(
                Text.literal("\u21BB"), // ↻ symbol
                button -> {
                    if (!rerollLocked) {
                        ClientPlayNetworking.send(new RerollTradesPayload());
                    }
                })
                .dimensions(this.x - 24, this.y + 8, 22, 20)
                .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(
                        Text.translatable("gui.reroll-trades.reroll")))
                .build();

        // Apply existing lock state so resize doesn't visually un-gray the button
        rerollButton.active = !rerollLocked;
        addDrawableChild(rerollButton);
    }

    // 1.21 / 1.21.1: keyPressed uses (int keyCode, int scanCode, int modifiers)
    // KeyInput class did not exist until 1.21.10.
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true, require = 0)
    private void rerollTrades$keyPressed(int keyCode, int scanCode, int modifiers,
            CallbackInfoReturnable<Boolean> cir) {
        if (!rerollLocked
                && RerollTradesClient.rerollKey != null
                && RerollTradesClient.rerollKey.matchesKey(keyCode, scanCode)) {
            ClientPlayNetworking.send(new RerollTradesPayload());
            cir.setReturnValue(true);
        }
    }
}
