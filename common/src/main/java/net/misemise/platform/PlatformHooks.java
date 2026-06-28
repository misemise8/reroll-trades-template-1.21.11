package net.misemise.platform;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.misemise.network.RerollEffectPayload;
import net.misemise.network.RerollStatePayload;

import java.nio.file.Path;
import java.util.UUID;

public interface PlatformHooks {

    Path getConfigDir();

    void sendState(ServerPlayer player, RerollStatePayload payload);

    void sendEffect(ServerPlayer player, RerollEffectPayload payload);

    boolean isGloballyLocked(Villager villager);

    void markGloballyLocked(Villager villager);

    int getRerollCount(Villager villager, UUID playerId);

    void setRerollCount(Villager villager, UUID playerId, int count);
}
