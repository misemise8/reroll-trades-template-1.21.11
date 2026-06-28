package net.misemise.neoforge;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.misemise.network.RerollEffectPayload;
import net.misemise.network.RerollStatePayload;
import net.misemise.platform.PlatformHooks;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

public final class PlatformHooksImpl implements PlatformHooks {

    @Override
    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public void sendState(ServerPlayer player, RerollStatePayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    @Override
    public void sendEffect(ServerPlayer player, RerollEffectPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    @Override
    public boolean isGloballyLocked(Villager villager) {
        if (villager.getExistingData(RerollTradesNeoForgeAttachments.traded()).orElse(false)) {
            return true;
        }

        boolean legacyLocked = villager.getExistingData(RerollTradesNeoForgeAttachments.legacyLockedPlayers())
                .map(locked -> !locked.isEmpty())
                .orElse(false);
        boolean traded = legacyLocked
                || villager.getOffers().stream().anyMatch(offer -> offer.getUses() > 0);
        if (traded) {
            markGloballyLocked(villager);
        }
        return traded;
    }

    @Override
    public void markGloballyLocked(Villager villager) {
        villager.setData(RerollTradesNeoForgeAttachments.traded(), true);
        villager.removeData(RerollTradesNeoForgeAttachments.legacyLockedPlayers());
    }

    @Override
    public int getRerollCount(Villager villager, UUID playerId) {
        return villager.getExistingData(RerollTradesNeoForgeAttachments.rerollCounts())
                .map(counts -> counts.getOrDefault(playerId, 0))
                .orElse(0);
    }

    @Override
    public void setRerollCount(Villager villager, UUID playerId, int count) {
        Map<UUID, Integer> counts = villager.getExistingData(RerollTradesNeoForgeAttachments.rerollCounts())
                .map(HashMap::new)
                .orElseGet(HashMap::new);
        counts.put(playerId, Math.max(0, count));
        villager.setData(RerollTradesNeoForgeAttachments.rerollCounts(), counts);
    }
}
