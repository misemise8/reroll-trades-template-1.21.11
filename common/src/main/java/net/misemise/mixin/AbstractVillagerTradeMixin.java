package net.misemise.mixin;

import net.minecraft.server.level.ServerPlayer;
//#if MC >= 12111
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
//#else
//$$ import net.minecraft.world.entity.npc.AbstractVillager;
//$$ import net.minecraft.world.entity.npc.Villager;
//#endif
import net.minecraft.world.item.trading.MerchantOffer;
import net.misemise.reroll.RerollController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractVillager.class)
public class AbstractVillagerTradeMixin {

    @Inject(method = "notifyTrade", at = @At("TAIL"))
    private void rerollTrades$notifyTrade(MerchantOffer offer, CallbackInfo callbackInfo) {
        AbstractVillager self = (AbstractVillager) (Object) this;
        if (!(self instanceof Villager villager) || villager.level().isClientSide()) {
            return;
        }
        if (!(villager.getTradingPlayer() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        RerollController.onTrade(serverPlayer, villager);
    }
}
