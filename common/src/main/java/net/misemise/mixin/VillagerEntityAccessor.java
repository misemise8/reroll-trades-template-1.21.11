package net.misemise.mixin;

import net.minecraft.world.entity.npc.AbstractVillager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractVillager.class)
public interface VillagerEntityAccessor {

    @Invoker("updateTrades")
    void rerollTrades$updateTrades();
}
