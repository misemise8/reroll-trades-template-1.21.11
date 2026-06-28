package net.misemise.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractVillager.class)
public interface VillagerEntityAccessor {

    @Invoker("updateTrades")
    void rerollTrades$updateTrades(ServerLevel level);
}
