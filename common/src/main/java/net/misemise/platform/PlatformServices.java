package net.misemise.platform;

import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;

import java.nio.file.Path;
import java.util.ServiceLoader;

public final class PlatformServices {

    private static final PlatformHooks PLATFORM = ServiceLoader.load(PlatformHooks.class)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No platform hooks implementation found"));

    private PlatformServices() {
    }

    public static Path getConfigDir() {
        return PLATFORM.getConfigDir();
    }

    public static boolean matchesRerollKey(KeyEvent event) {
        return PLATFORM.matchesRerollKey(event);
    }

    public static void sendRerollRequest() {
        PLATFORM.sendRerollRequest();
    }

    public static void sendParticle(ServerPlayer player, BlockPos pos) {
        PLATFORM.sendParticle(player, pos);
    }

    public static void sendLocked(ServerPlayer player) {
        PLATFORM.sendLocked(player);
    }

    public static void sendReject(ServerPlayer player) {
        PLATFORM.sendReject(player);
    }

    public static boolean isRerollLocked(Villager villager, ServerPlayer player) {
        return PLATFORM.isRerollLocked(villager, player);
    }

    public static void lockReroll(Villager villager, ServerPlayer player) {
        PLATFORM.lockReroll(villager, player);
    }
}
