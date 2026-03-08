package net.misemise.mixin;

import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor to read VillagerEntity's lastCustomer field (type: PlayerEntity),
 * which stores the most recent player who traded with this villager.
 */
@Mixin(VillagerEntity.class)
public interface VillagerLastCustomerAccessor {

    @Accessor("lastCustomer")
    @Nullable
    PlayerEntity getLastCustomer();
}
