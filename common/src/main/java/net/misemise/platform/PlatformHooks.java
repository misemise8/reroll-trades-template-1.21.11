package net.misemise.platform;

import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;

import java.nio.file.Path;

public interface PlatformHooks {

    Path getConfigDir();

    boolean matchesRerollKey(KeyEvent event);

    void sendRerollRequest();

    void sendParticle(ServerPlayer player, BlockPos pos);

    void sendLocked(ServerPlayer player);

    void sendReject(ServerPlayer player);

    boolean isRerollLocked(Villager villager, ServerPlayer player);

    void lockReroll(Villager villager, ServerPlayer player);
}
