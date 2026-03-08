package net.misemise.mixin;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.village.TradeOffer;
import net.misemise.RerollTrades;
import net.misemise.network.RerollLockedPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.UUID;

@Mixin(VillagerEntity.class)
public class VillagerEntityTradeMixin {

    @Inject(method = "afterUsing", at = @At("TAIL"))
    private void onAfterUsing(TradeOffer offer, CallbackInfo ci) {
        VillagerEntity self = (VillagerEntity) (Object) this;
        if (self.getEntityWorld().isClient())
            return;

        // lastCustomer is PlayerEntity; it's only a ServerPlayerEntity on server
        PlayerEntity customer = ((VillagerLastCustomerAccessor) self).getLastCustomer();
        if (!(customer instanceof ServerPlayerEntity serverPlayer))
            return;

        // Persistently lock this player for this villager
        HashSet<UUID> locked = self.getAttached(RerollTrades.REROLL_LOCKED);
        if (locked == null)
            locked = new HashSet<>();
        else
            locked = new HashSet<>(locked);
        locked.add(serverPlayer.getUuid());
        self.setAttached(RerollTrades.REROLL_LOCKED, locked);

        // Notify the client to gray out the button
        ServerPlayNetworking.send(serverPlayer, new RerollLockedPayload());
    }

    @Inject(method = "interactMob", at = @At("RETURN"))
    private void onInteractMob(PlayerEntity player, Hand hand,
            CallbackInfoReturnable<ActionResult> cir) {
        if (!(player instanceof ServerPlayerEntity serverPlayer))
            return;
        if (!cir.getReturnValue().isAccepted())
            return;

        VillagerEntity self = (VillagerEntity) (Object) this;
        if (self.getEntityWorld().isClient())
            return;

        HashSet<UUID> locked = self.getAttached(RerollTrades.REROLL_LOCKED);
        if (locked != null && locked.contains(serverPlayer.getUuid())) {
            ServerPlayNetworking.send(serverPlayer, new RerollLockedPayload());
        }
    }
}
