package net.misemise.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.misemise.platform.PlatformServices;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Villager.class)
public class VillagerEntityTradeMixin {

    @Inject(method = "mobInteract", at = @At("RETURN"))
    private void rerollTrades$mobInteract(Player player, InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> callbackInfo) {
        if (!(player instanceof ServerPlayer serverPlayer) || !callbackInfo.getReturnValue().consumesAction()) {
            return;
        }

        Villager self = (Villager) (Object) this;
        if (!self.level().isClientSide() && PlatformServices.isRerollLocked(self, serverPlayer)) {
            PlatformServices.sendLocked(serverPlayer);
        }
    }
}
