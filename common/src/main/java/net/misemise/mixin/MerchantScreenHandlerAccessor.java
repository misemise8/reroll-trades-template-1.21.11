package net.misemise.mixin;

//#if MC < 12108 || MC >= 12109 && MC < 12110 || MC >= 12111
//#else
//$$ import net.minecraft.world.inventory.MerchantMenu;
//#endif
import net.minecraft.world.item.trading.Merchant;
//#if MC < 12108 || MC >= 12109 && MC < 12110 || MC >= 12111
import net.minecraft.world.inventory.MerchantMenu;
//#endif
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MerchantMenu.class)
public interface MerchantScreenHandlerAccessor {

    @Accessor("tradeContainer")
    net.minecraft.world.inventory.MerchantContainer rerollTrades$getTradeContainer();

    @Accessor("trader")
    Merchant rerollTrades$getMerchant();
}
