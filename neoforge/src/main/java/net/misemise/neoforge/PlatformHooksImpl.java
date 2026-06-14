package net.misemise.neoforge;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.misemise.network.RerollLockedPayload;
import net.misemise.network.RerollParticlePayload;
import net.misemise.network.RerollRejectPayload;
import net.misemise.network.RerollTradesPayload;
import net.misemise.platform.PlatformHooks;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.UUID;

public final class PlatformHooksImpl implements PlatformHooks {

    @Override
    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public boolean matchesRerollKey(int keyCode, int scanCode) {
        return RerollTradesNeoForgeClient.REROLL_KEY.isActiveAndMatches(InputConstants.getKey(keyCode, scanCode));
    }

    @Override
    public void sendRerollRequest() {
        ClientPacketDistributor.sendToServer(new RerollTradesPayload());
    }

    @Override
    public void sendParticle(ServerPlayer player, BlockPos pos) {
        PacketDistributor.sendToPlayer(player, new RerollParticlePayload(pos));
    }

    @Override
    public void sendLocked(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new RerollLockedPayload());
    }

    @Override
    public void sendReject(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new RerollRejectPayload());
    }

    @Override
    public boolean isRerollLocked(Villager villager, ServerPlayer player) {
        return villager.getExistingData(RerollTradesNeoForgeAttachments.lockedPlayers())
                .map(locked -> locked.contains(player.getUUID()))
                .orElse(false);
    }

    @Override
    public void lockReroll(Villager villager, ServerPlayer player) {
        HashSet<UUID> locked = villager.getExistingData(RerollTradesNeoForgeAttachments.lockedPlayers())
                .map(HashSet::new)
                .orElseGet(HashSet::new);
        locked.add(player.getUUID());
        villager.setData(RerollTradesNeoForgeAttachments.lockedPlayers(), locked);
    }
}
