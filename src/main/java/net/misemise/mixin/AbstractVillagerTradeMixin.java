package net.misemise.mixin;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.trading.MerchantOffer;
import net.misemise.RerollTrades;
import net.misemise.network.RerollLockedPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.UUID;

@Mixin(AbstractVillager.class)
public class AbstractVillagerTradeMixin {

    @Inject(method = "notifyTrade", at = @At("TAIL"))
    private void rerollTrades$notifyTrade(MerchantOffer offer, CallbackInfo ci) {
        AbstractVillager self = (AbstractVillager) (Object) this;
        if (!(self instanceof Villager villager) || villager.level().isClientSide()) {
            return;
        }
        if (!(villager.getTradingPlayer() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        HashSet<UUID> locked = villager.getAttached(RerollTrades.REROLL_LOCKED);
        locked = locked == null ? new HashSet<>() : new HashSet<>(locked);
        locked.add(serverPlayer.getUUID());
        villager.setAttached(RerollTrades.REROLL_LOCKED, locked);

        ServerPlayNetworking.send(serverPlayer, new RerollLockedPayload());
    }
}
