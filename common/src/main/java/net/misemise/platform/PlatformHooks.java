package net.misemise.platform;

import net.minecraft.server.level.ServerPlayer;
//#if MC >= 12111
import net.minecraft.world.entity.npc.villager.Villager;
//#else
//$$ import net.minecraft.world.entity.npc.Villager;
//#endif
import net.misemise.network.RerollEffectPayload;
import net.misemise.network.RerollStatePayload;
import net.misemise.network.TradeTargetDataPayload;
import net.misemise.target.TradeLockData;

import java.nio.file.Path;
import java.util.UUID;

public interface PlatformHooks {

    Path getConfigDir();

    void sendState(ServerPlayer player, RerollStatePayload payload);

    void sendEffect(ServerPlayer player, RerollEffectPayload payload);

    void sendTargets(ServerPlayer player, TradeTargetDataPayload payload);

    TradeLockData getTradeLocks(Villager villager);

    void setTradeLocks(Villager villager, TradeLockData data);

    boolean isGloballyLocked(Villager villager);

    void markGloballyLocked(Villager villager);

    int getRerollCount(Villager villager, UUID playerId);

    void setRerollCount(Villager villager, UUID playerId, int count);
}
