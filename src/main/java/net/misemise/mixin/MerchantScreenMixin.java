package net.misemise.mixin;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
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
     * has traded with this villager. Never reset client-side 窶・a new screen
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
     * Reverts the optimistic lock so the player can try again.
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
                Text.literal("\u21BB"), // 竊ｻ symbol
                button -> {
                    if (!rerollLocked) {
                        ClientPlayNetworking.send(new RerollTradesPayload());
                        // Optimistically disable until server confirms or rejects.
                        rerollLocked = true;
                        rerollButton.active = false;
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

    // 1.21.9+: keyPressed uses (KeyInput) 窶・introduced alongside
    // KeyBinding.Category.
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void rerollTrades$keyPressed(KeyInput keyInput,
            CallbackInfoReturnable<Boolean> cir) {
        if (!rerollLocked
                && RerollTradesClient.rerollKey != null
                && RerollTradesClient.rerollKey.matchesKey(keyInput)) {
            ClientPlayNetworking.send(new RerollTradesPayload());
            // Optimistically lock until server responds.
            rerollLocked = true;
            if (rerollButton != null)
                rerollButton.active = false;
            cir.setReturnValue(true);
        }
    }
}
