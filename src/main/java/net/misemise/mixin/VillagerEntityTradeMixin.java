package net.misemise.mixin;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.misemise.RerollTrades;
import net.misemise.network.RerollLockedPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.UUID;

@Mixin(Villager.class)
public class VillagerEntityTradeMixin {

    @Inject(method = "mobInteract", at = @At("RETURN"))
    private void rerollTrades$mobInteract(Player player, InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir) {
        if (!(player instanceof ServerPlayer serverPlayer) || !cir.getReturnValue().consumesAction()) {
            return;
        }

        Villager self = (Villager) (Object) this;
        if (self.level().isClientSide()) {
            return;
        }

        HashSet<UUID> locked = self.getAttached(RerollTrades.REROLL_LOCKED);
        if (locked != null && locked.contains(serverPlayer.getUUID())) {
            ServerPlayNetworking.send(serverPlayer, new RerollLockedPayload());
        }
    }
}
