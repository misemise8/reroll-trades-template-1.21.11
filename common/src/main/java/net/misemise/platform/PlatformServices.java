package net.misemise.platform;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.misemise.network.RerollEffectPayload;
import net.misemise.network.RerollStatePayload;

import java.nio.file.Path;
import java.util.ServiceLoader;
import java.util.UUID;

public final class PlatformServices {

    private static final PlatformHooks PLATFORM = ServiceLoader.load(PlatformHooks.class)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No platform hooks implementation found"));

    private PlatformServices() {
    }

    public static Path getConfigDir() {
        return PLATFORM.getConfigDir();
    }

    public static void sendState(ServerPlayer player, RerollStatePayload payload) {
        PLATFORM.sendState(player, payload);
    }

    public static void sendEffect(ServerPlayer player, RerollEffectPayload payload) {
        PLATFORM.sendEffect(player, payload);
    }

    public static boolean isGloballyLocked(Villager villager) {
        return PLATFORM.isGloballyLocked(villager);
    }

    public static void markGloballyLocked(Villager villager) {
        PLATFORM.markGloballyLocked(villager);
    }

    public static int getRerollCount(Villager villager, UUID playerId) {
        return PLATFORM.getRerollCount(villager, playerId);
    }

    public static void setRerollCount(Villager villager, UUID playerId, int count) {
        PLATFORM.setRerollCount(villager, playerId, count);
    }
}
