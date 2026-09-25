//#if MC >= 260200
package net.misemise.smoke.mixin;
import net.misemise.client.TradeTargetScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value = TradeTargetScreen.class, remap = false)
public class TargetScreenFixtureMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void keepFixtureOpen(CallbackInfo ci) { if (Boolean.getBoolean("tradeTargetClientSmoke")) ci.cancel(); }
}
//#endif
