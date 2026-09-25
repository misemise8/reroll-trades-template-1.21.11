package net.misemise.fabric;

import net.misemise.platform.PlatformHooks;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
//#if MC >= 12111
import net.minecraft.resources.Identifier;
//#else
//$$ import net.minecraft.resources.ResourceLocation;
//#endif
import net.minecraft.server.level.ServerPlayer;
//#if MC >= 12111
import net.minecraft.world.entity.npc.villager.Villager;
//#else
//$$ import net.minecraft.world.entity.npc.Villager;
//#endif
import net.misemise.RerollTrades;
import net.misemise.network.RerollEffectPayload;
import net.misemise.network.RerollStatePayload;
import net.misemise.network.TradeTargetDataPayload;
import net.misemise.target.TradeLockData;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

public final class PlatformHooksImpl implements PlatformHooks {

//#if MC >= 12111
    private static final AttachmentType<TradeLockData> TRADE_LOCKS = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(RerollTrades.MOD_ID, "trade_targets"),
            builder -> builder.persistent(TradeLockData.CODEC).initializer(() -> TradeLockData.EMPTY));
//#elseif MC >= 12104
//$$     private static final AttachmentType<TradeLockData> TRADE_LOCKS = AttachmentRegistry.create(
//$$             ResourceLocation.fromNamespaceAndPath(RerollTrades.MOD_ID, "trade_targets"),
//$$             builder -> builder.persistent(TradeLockData.CODEC).initializer(() -> TradeLockData.EMPTY));
//#else
//$$     private static final AttachmentType<TradeLockData> TRADE_LOCKS = AttachmentRegistry.<TradeLockData>builder()
//$$             .persistent(TradeLockData.CODEC).initializer(() -> TradeLockData.EMPTY)
//$$             .buildAndRegister(ResourceLocation.fromNamespaceAndPath(RerollTrades.MOD_ID, "trade_targets"));
//#endif

    @Override public void sendTargets(ServerPlayer player, TradeTargetDataPayload payload) { ServerPlayNetworking.send(player, payload); }
    @Override public TradeLockData getTradeLocks(Villager villager) {
        TradeLockData data = villager.getAttached(TRADE_LOCKS);
        return data == null ? TradeLockData.EMPTY : data;
    }
    @Override public void setTradeLocks(Villager villager, TradeLockData data) { villager.setAttached(TRADE_LOCKS, data); }

    private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);
    private static final Codec<Map<UUID, Integer>> REROLL_COUNTS_CODEC = Codec.unboundedMap(UUID_CODEC, Codec.INT);
    private static final Codec<HashSet<UUID>> LEGACY_LOCKED_PLAYERS_CODEC = Codec
            .list(UUID_CODEC)
            .xmap(HashSet::new, ArrayList::new);

//#if MC >= 12111
    private static final AttachmentType<Boolean> TRADED = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(RerollTrades.MOD_ID, "traded"),
            builder -> builder.persistent(Codec.BOOL).initializer(() -> false)
    );
    private static final AttachmentType<HashSet<UUID>> LEGACY_LOCKED_PLAYERS = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(RerollTrades.MOD_ID, "locked_players"),
            builder -> builder.persistent(LEGACY_LOCKED_PLAYERS_CODEC).initializer(HashSet::new)
    );
    private static final AttachmentType<Map<UUID, Integer>> REROLL_COUNTS = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(RerollTrades.MOD_ID, "reroll_counts"),
            builder -> builder.persistent(REROLL_COUNTS_CODEC).initializer(HashMap::new)
    );
//#elseif MC >= 12104 && MC < 12111
//$$     private static final AttachmentType<Boolean> TRADED = AttachmentRegistry.create(
//$$             ResourceLocation.fromNamespaceAndPath(RerollTrades.MOD_ID, "traded"),
//$$             builder -> builder.persistent(Codec.BOOL).initializer(() -> false)
//$$     );
//$$     private static final AttachmentType<HashSet<UUID>> LEGACY_LOCKED_PLAYERS = AttachmentRegistry.create(
//$$             ResourceLocation.fromNamespaceAndPath(RerollTrades.MOD_ID, "locked_players"),
//$$             builder -> builder.persistent(LEGACY_LOCKED_PLAYERS_CODEC).initializer(HashSet::new)
//$$     );
//$$     private static final AttachmentType<Map<UUID, Integer>> REROLL_COUNTS = AttachmentRegistry.create(
//$$             ResourceLocation.fromNamespaceAndPath(RerollTrades.MOD_ID, "reroll_counts"),
//$$             builder -> builder.persistent(REROLL_COUNTS_CODEC).initializer(HashMap::new)
//$$     );
//#else
//$$     private static final AttachmentType<Boolean> TRADED = AttachmentRegistry.<Boolean>builder()
//$$             .persistent(Codec.BOOL)
//$$             .initializer(() -> false)
//$$             .buildAndRegister(ResourceLocation.fromNamespaceAndPath(RerollTrades.MOD_ID, "traded"));
//$$     private static final AttachmentType<HashSet<UUID>> LEGACY_LOCKED_PLAYERS = AttachmentRegistry.<HashSet<UUID>>builder()
//$$             .persistent(LEGACY_LOCKED_PLAYERS_CODEC)
//$$             .initializer(HashSet::new)
//$$             .buildAndRegister(ResourceLocation.fromNamespaceAndPath(RerollTrades.MOD_ID, "locked_players"));
//$$     private static final AttachmentType<Map<UUID, Integer>> REROLL_COUNTS = AttachmentRegistry.<Map<UUID, Integer>>builder()
//$$             .persistent(REROLL_COUNTS_CODEC)
//$$             .initializer(HashMap::new)
//$$             .buildAndRegister(ResourceLocation.fromNamespaceAndPath(RerollTrades.MOD_ID, "reroll_counts"));
//#endif

    @Override
    public Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public void sendState(ServerPlayer player, RerollStatePayload payload) {
        ServerPlayNetworking.send(player, payload);
    }

    @Override
    public void sendEffect(ServerPlayer player, RerollEffectPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }

    @Override
    public boolean isGloballyLocked(Villager villager) {
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

    @Override
    public void markGloballyLocked(Villager villager) {
        villager.setAttached(TRADED, true);
        villager.removeAttached(LEGACY_LOCKED_PLAYERS);
    }

    @Override
    public int getRerollCount(Villager villager, UUID playerId) {
        Map<UUID, Integer> counts = villager.getAttached(REROLL_COUNTS);
        return counts == null ? 0 : counts.getOrDefault(playerId, 0);
    }

    @Override
    public void setRerollCount(Villager villager, UUID playerId, int count) {
        Map<UUID, Integer> current = villager.getAttached(REROLL_COUNTS);
        Map<UUID, Integer> counts = current == null ? new HashMap<>() : new HashMap<>(current);
        counts.put(playerId, Math.max(0, count));
        villager.setAttached(REROLL_COUNTS, counts);
    }
}
