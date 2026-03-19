package net.misemise.mixin;

import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.inventory.MerchantMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MerchantMenu.class)
public interface MerchantScreenHandlerAccessor {

    @Accessor("trader")
    Merchant getMerchant();
}