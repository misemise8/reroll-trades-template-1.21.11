package net.misemise.fabric;

import com.mojang.serialization.Codec;
import net.minecraft.client.input.KeyEvent;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.misemise.RerollTrades;
import net.misemise.network.RerollLockedPayload;
import net.misemise.network.RerollParticlePayload;
import net.misemise.network.RerollRejectPayload;
import net.misemise.network.RerollTradesPayload;
import net.misemise.platform.PlatformHooks;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;

public final class PlatformHooksImpl implements PlatformHooks {

    private static final AttachmentType<HashSet<UUID>> REROLL_LOCKED = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(RerollTrades.MOD_ID, "locked_players"),
            builder -> builder
                    .persistent(Codec.list(Codec.STRING.xmap(UUID::fromString, UUID::toString))
                            .xmap(HashSet::new, ArrayList::new))
                    .initializer(HashSet::new));

    @Override
    public Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public boolean matchesRerollKey(KeyEvent event) {
        return RerollTradesFabricClient.getRerollKey() != null
                && RerollTradesFabricClient.getRerollKey().matches(event);
    }

    @Override
    public void sendRerollRequest() {
        ClientPlayNetworking.send(new RerollTradesPayload());
    }

    @Override
    public void sendParticle(ServerPlayer player, BlockPos pos) {
        ServerPlayNetworking.send(player, new RerollParticlePayload(pos));
    }

    @Override
    public void sendLocked(ServerPlayer player) {
        ServerPlayNetworking.send(player, new RerollLockedPayload());
    }

    @Override
    public void sendReject(ServerPlayer player) {
        ServerPlayNetworking.send(player, new RerollRejectPayload());
    }

    @Override
    public boolean isRerollLocked(Villager villager, ServerPlayer player) {
        HashSet<UUID> locked = villager.getAttached(REROLL_LOCKED);
        return locked != null && locked.contains(player.getUUID());
    }

    @Override
    public void lockReroll(Villager villager, ServerPlayer player) {
        HashSet<UUID> locked = villager.getAttached(REROLL_LOCKED);
        locked = locked == null ? new HashSet<>() : new HashSet<>(locked);
        locked.add(player.getUUID());
        villager.setAttached(REROLL_LOCKED, locked);
    }
}
