package net.misemise.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.misemise.reroll.RerollController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantMenu.class)
public class MerchantMenuLifecycleMixin {

    @Inject(method = "removed", at = @At("HEAD"), require = 1)
    private void rerollTrades$removed(Player player, CallbackInfo callbackInfo) {
        if (player instanceof ServerPlayer serverPlayer) {
            MerchantMenu self = (MerchantMenu) (Object) this;
            RerollController.onMenuClosed(serverPlayer, self.containerId);
        }
    }
}
