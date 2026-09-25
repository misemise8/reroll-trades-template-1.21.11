//#if MC < 260100
package net.misemise.mixin.catalog;

import net.minecraft.world.item.component.SuspiciousStewEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

//#if MC >= 12111
@Mixin(targets = "net.minecraft.world.entity.npc.villager.VillagerTrades$SuspiciousStewForEmerald")
//#else
//$$ @Mixin(targets = "net.minecraft.world.entity.npc.VillagerTrades$SuspiciousStewForEmerald")
//#endif
public interface LegacyStewAccessor {
    @Accessor("effects") SuspiciousStewEffects rerollTrades$effects();
}
//#endif
