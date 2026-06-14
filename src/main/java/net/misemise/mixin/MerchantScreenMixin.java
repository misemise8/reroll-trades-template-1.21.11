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

@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin extends HandledScreen<MerchantScreenHandler> implements IRerollLockable {

    private MerchantScreenMixin() {
        super(null, null, null);
    }

    @Unique
    private ButtonWidget rerollButton;

    @Unique
    private boolean rerollLocked = false;

    @Unique
    public void rerollTrades$lock() {
        rerollLocked = true;
        if (rerollButton != null) {
            rerollButton.active = false;
        }
    }

    @Unique
    public void rerollTrades$unlock() {
        rerollLocked = false;
        if (rerollButton != null) {
            rerollButton.active = true;
        }
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void rerollTrades$addRerollButton(CallbackInfo ci) {
        // Place button to the LEFT of the trade list panel as a tab
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

        rerollButton.active = !rerollLocked;
        addDrawableChild(rerollButton);
    }

    // Override keyPressed with the 1.21.11 signature: keyPressed(KeyInput)
    // MerchantScreen doesn't define this, so we can add it via mixin
    @Override
    public boolean keyPressed(KeyInput keyInput) {
        if (!rerollLocked
                && RerollTradesClient.rerollKey != null
                && RerollTradesClient.rerollKey.matchesKey(keyInput)) {
            ClientPlayNetworking.send(new RerollTradesPayload());
            return true;
        }
        return super.keyPressed(keyInput);
    }
}
