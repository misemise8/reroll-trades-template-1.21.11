package net.misemise.platform;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.misemise.RerollTrades;
import net.misemise.network.RerollEffectPayload;
import net.misemise.network.RerollStatePayload;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

public final class PlatformServices {

    private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);
    private static final Codec<Map<UUID, Integer>> REROLL_COUNTS_CODEC = Codec.unboundedMap(UUID_CODEC, Codec.INT);
    private static final Codec<HashSet<UUID>> LEGACY_LOCKED_PLAYERS_CODEC = Codec
            .list(UUID_CODEC)
            .xmap(HashSet::new, ArrayList::new);

    private static final AttachmentType<Boolean> TRADED = AttachmentRegistry.<Boolean>builder()
            .persistent(Codec.BOOL)
            .initializer(() -> false)
            .buildAndRegister(ResourceLocation.fromNamespaceAndPath(RerollTrades.MOD_ID, "traded"));
    private static final AttachmentType<HashSet<UUID>> LEGACY_LOCKED_PLAYERS = AttachmentRegistry.<HashSet<UUID>>builder()
            .persistent(LEGACY_LOCKED_PLAYERS_CODEC)
            .initializer(HashSet::new)
            .buildAndRegister(ResourceLocation.fromNamespaceAndPath(RerollTrades.MOD_ID, "locked_players"));
    private static final AttachmentType<Map<UUID, Integer>> REROLL_COUNTS = AttachmentRegistry.<Map<UUID, Integer>>builder()
            .persistent(REROLL_COUNTS_CODEC)
            .initializer(HashMap::new)
            .buildAndRegister(ResourceLocation.fromNamespaceAndPath(RerollTrades.MOD_ID, "reroll_counts"));

    private PlatformServices() {
    }

    public static void init() {
    }

    public static Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    public static void sendState(ServerPlayer player, RerollStatePayload payload) {
        ServerPlayNetworking.send(player, payload);
    }

    public static void sendEffect(ServerPlayer player, RerollEffectPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }

    public static boolean isGloballyLocked(Villager villager) {
        if (Boolean.TRUE.equals(villager.getAttached(TRADED))) {
            return true;
        }
        HashSet<UUID> legacyLocks = villager.getAttached(LEGACY_LOCKED_PLAYERS);
        boolean traded = (legacyLocks != null && !legacyLocks.isEmpty())
                || villager.getOffers().stream().anyMatch(offer -> offer.getUses() > 0);
        if (traded) {
            markGloballyLocked(villager);
        }
        return traded;
    }

    public static void markGloballyLocked(Villager villager) {
        villager.setAttached(TRADED, true);
        villager.removeAttached(LEGACY_LOCKED_PLAYERS);
    }

    public static int getRerollCount(Villager villager, UUID playerId) {
        Map<UUID, Integer> counts = villager.getAttached(REROLL_COUNTS);
        return counts == null ? 0 : counts.getOrDefault(playerId, 0);
    }

    public static void setRerollCount(Villager villager, UUID playerId, int count) {
        Map<UUID, Integer> current = villager.getAttached(REROLL_COUNTS);
        Map<UUID, Integer> counts = current == null ? new HashMap<>() : new HashMap<>(current);
        counts.put(playerId, Math.max(0, count));
        villager.setAttached(REROLL_COUNTS, counts);
    }
}
