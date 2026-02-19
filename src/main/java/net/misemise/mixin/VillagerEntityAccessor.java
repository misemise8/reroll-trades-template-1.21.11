package net.misemise.mixin;

import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Accessor to invoke the protected fillRecipes(ServerWorld) method on
 * MerchantEntity
 * (which VillagerEntity overrides) to regenerate trade offers.
 */
@Mixin(MerchantEntity.class)
public interface VillagerEntityAccessor {

    @Invoker("fillRecipes")
    void invokeFillRecipes(ServerWorld world);
}
