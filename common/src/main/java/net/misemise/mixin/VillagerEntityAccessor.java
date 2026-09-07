package net.misemise.mixin;

//#if MC >= 12111
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
//#else
//$$ import net.minecraft.world.entity.npc.AbstractVillager;
//#endif
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractVillager.class)
public interface VillagerEntityAccessor {

    @Invoker("updateTrades")
//#if MC >= 12111
    void rerollTrades$updateTrades(ServerLevel level);
//#else
//$$     void rerollTrades$updateTrades();
//#endif
}
