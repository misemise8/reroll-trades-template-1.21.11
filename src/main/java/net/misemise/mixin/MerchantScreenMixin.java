package net.misemise.mixin;

import com.mojang.blaze3d.platform.InputConstants; // net. は不要
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MerchantMenu;
import net.misemise.RerollTradesClient;
import net.misemise.network.RerollTradesPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin extends AbstractContainerScreen<MerchantMenu> {

    private MerchantScreenMixin(MerchantMenu menu, net.minecraft.world.entity.player.Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Unique
    private Button rerollButton;

    @Inject(method = "init", at = @At("TAIL"))
    private void rerollTrades$addRerollButton(CallbackInfo ci) {
        rerollButton = Button.builder(
                        Component.literal("\u21BB"),
                        button -> ClientPlayNetworking.send(new RerollTradesPayload()))
                .bounds(this.leftPos - 24, this.topPos + 8, 22, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.reroll-trades.reroll")))
                .build();

        this.addRenderableWidget(rerollButton);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (RerollTradesClient.rerollKey != null &&
                RerollTradesClient.rerollKey.matches(event)) {

            ClientPlayNetworking.send(new RerollTradesPayload());
            return true;
        }
        return super.keyPressed(event);
    }
}