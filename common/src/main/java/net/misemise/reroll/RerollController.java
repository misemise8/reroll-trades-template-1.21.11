package net.misemise.reroll;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffers;
import net.misemise.RerollTrades;
import net.misemise.config.RerollServerConfig;
import net.misemise.mixin.MerchantScreenHandlerAccessor;
import net.misemise.mixin.VillagerEntityAccessor;
import net.misemise.network.RerollEffectPayload;
import net.misemise.network.RerollStatePayload;
import net.misemise.platform.PlatformServices;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class RerollController {

    private static final Map<UUID, Long> NEXT_REROLL_TICK = new HashMap<>();
    private static final Map<UUID, UndoEntry> UNDO_ENTRIES = new HashMap<>();

    private RerollController() {
    }

    public static void handleAction(ServerPlayer player, RerollAction action, int requestedContainerId) {
        if (!(player.containerMenu instanceof MerchantMenu menu) || menu.containerId != requestedContainerId) {
            return;
        }

        Merchant merchant = ((MerchantScreenHandlerAccessor) menu).rerollTrades$getMerchant();
        if (!(merchant instanceof Villager villager)) {
            PlatformServices.sendState(player, unsupportedState(menu.containerId));
            return;
        }

        switch (action) {
            case REQUEST_STATE -> sendState(player, menu, villager, true);
            case REROLL -> reroll(player, menu, villager);
            case UNDO -> undo(player, menu, villager);
        }
    }

    public static void onTrade(ServerPlayer player, Villager villager) {
        PlatformServices.markGloballyLocked(villager);
        UNDO_ENTRIES.remove(player.getUUID());
        if (player.containerMenu instanceof MerchantMenu menu
                && ((MerchantScreenHandlerAccessor) menu).rerollTrades$getMerchant() == villager) {
            sendState(player, menu, villager, true);
        }
    }

    public static void onMenuClosed(ServerPlayer player, int containerId) {
        UndoEntry entry = UNDO_ENTRIES.get(player.getUUID());
        if (entry != null && entry.containerId == containerId) {
            UNDO_ENTRIES.remove(player.getUUID());
        }
    }

    public static void onPlayerLogout(ServerPlayer player) {
        UUID playerId = player.getUUID();
        UNDO_ENTRIES.remove(playerId);
        NEXT_REROLL_TICK.remove(playerId);
    }

    public static void tick(MinecraftServer server) {
        long now = server.getTickCount();
        NEXT_REROLL_TICK.entrySet().removeIf(entry -> entry.getValue() <= now);

        long timeoutTicks = RerollServerConfig.get().undoTimeoutSeconds * 20L;
        Iterator<UndoEntry> iterator = UNDO_ENTRIES.values().iterator();
        while (iterator.hasNext()) {
            UndoEntry entry = iterator.next();
            if (now - entry.createdTick > timeoutTicks) {
                iterator.remove();
            }
        }
    }

    public static void refreshOpenScreens(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.containerMenu instanceof MerchantMenu menu) {
                handleAction(player, RerollAction.REQUEST_STATE, menu.containerId);
            }
        }
    }

    public static String status(ServerPlayer player) {
        if (!(player.containerMenu instanceof MerchantMenu menu)) {
            return "no merchant screen open";
        }
        Merchant merchant = ((MerchantScreenHandlerAccessor) menu).rerollTrades$getMerchant();
        if (!(merchant instanceof Villager villager)) {
            return "unsupported merchant";
        }

        RerollStatePayload state = buildState(player, menu, villager, true);
        return "reason=" + state.reason().name().toLowerCase()
                + ", undo=" + state.canUndo()
                + ", cooldown=" + state.remainingCooldownTicks()
                + ", remaining=" + (state.remainingRerolls() < 0 ? "unlimited" : state.remainingRerolls());
    }

    private static void reroll(ServerPlayer player, MerchantMenu menu, Villager villager) {
        RerollStatePayload state = buildState(player, menu, villager, true);
        if (!state.canReroll()) {
            PlatformServices.sendState(player, state);
            return;
        }

        MerchantOffers previousOffers = villager.getOffers().copy();
        MerchantOffers workingOffers = villager.getOffers();
        workingOffers.clear();
        ((VillagerEntityAccessor) villager).rerollTrades$updateTrades((ServerLevel) villager.level());

        if (villager.getOffers().isEmpty()) {
            villager.setOffers(previousOffers.copy());
            PlatformServices.sendState(player, buildState(player, menu, villager, false, RerollBlockReason.NO_PROFESSION));
            return;
        }

        RerollServerConfig config = RerollServerConfig.get();
        if (config.maxRerollsPerPlayerPerVillager > 0) {
            int count = PlatformServices.getRerollCount(villager, player.getUUID());
            PlatformServices.setRerollCount(villager, player.getUUID(), count + 1);
        }

        long now = currentTick(player);
        NEXT_REROLL_TICK.put(player.getUUID(), now + config.cooldownTicks);
        UNDO_ENTRIES.put(player.getUUID(), new UndoEntry(
                player.getUUID(),
                villager.getUUID(),
                menu.containerId,
                previousOffers,
                now
        ));

        syncOffers(player, menu, villager);
        sendState(player, menu, villager, true);
        PlatformServices.sendEffect(player, new RerollEffectPayload(villager.blockPosition(), false));
        RerollTrades.LOGGER.debug("Player {} rerolled trades for villager at {}",
                player.getName().getString(), villager.blockPosition());
    }

    private static void undo(ServerPlayer player, MerchantMenu menu, Villager villager) {
        UndoEntry entry = validUndo(player, menu, villager, true);
        if (!RerollServerConfig.get().enableUndo || entry == null) {
            PlatformServices.sendState(player,
                    buildState(player, menu, villager, false, RerollBlockReason.UNDO_UNAVAILABLE));
            return;
        }

        UNDO_ENTRIES.remove(player.getUUID());
        villager.setOffers(entry.offers.copy());
        syncOffers(player, menu, villager);
        sendState(player, menu, villager, true);
        PlatformServices.sendEffect(player, new RerollEffectPayload(villager.blockPosition(), true));
    }

    private static void sendState(ServerPlayer player, MerchantMenu menu, Villager villager, boolean includeSneaking) {
        PlatformServices.sendState(player, buildState(player, menu, villager, includeSneaking));
    }

    private static RerollStatePayload buildState(
            ServerPlayer player,
            MerchantMenu menu,
            Villager villager,
            boolean includeSneaking
    ) {
        return buildState(player, menu, villager, includeSneaking, null);
    }

    private static RerollStatePayload buildState(
            ServerPlayer player,
            MerchantMenu menu,
            Villager villager,
            boolean includeSneaking,
            RerollBlockReason forcedReason
    ) {
        RerollServerConfig config = RerollServerConfig.get();
        int count = PlatformServices.getRerollCount(villager, player.getUUID());
        int remaining = config.maxRerollsPerPlayerPerVillager == 0
                ? -1
                : Math.max(0, config.maxRerollsPerPlayerPerVillager - count);
        int cooldown = remainingCooldown(player);

        RerollBlockReason reason = forcedReason != null
                ? forcedReason
                : evaluate(player, villager, config, remaining, cooldown, includeSneaking);
        boolean canUndo = config.enableUndo && validUndo(player, menu, villager, false) != null;

        return new RerollStatePayload(
                menu.containerId,
                true,
                reason == RerollBlockReason.NONE,
                reason,
                canUndo,
                cooldown,
                remaining,
                config.requireSneaking
        );
    }

    private static RerollBlockReason evaluate(
            ServerPlayer player,
            Villager villager,
            RerollServerConfig config,
            int remaining,
            int cooldown,
            boolean includeSneaking
    ) {
        if (PlatformServices.isGloballyLocked(villager)) {
            return RerollBlockReason.ALREADY_TRADED;
        }
        if (villager.getOffers().isEmpty()) {
            return RerollBlockReason.NO_PROFESSION;
        }
        if (remaining == 0) {
            return RerollBlockReason.LIMIT_REACHED;
        }
        if (cooldown > 0) {
            return RerollBlockReason.COOLDOWN;
        }
        if (includeSneaking && config.requireSneaking && !player.isShiftKeyDown()) {
            return RerollBlockReason.MUST_SNEAK;
        }
        return RerollBlockReason.NONE;
    }

    private static UndoEntry validUndo(ServerPlayer player, MerchantMenu menu, Villager villager, boolean removeInvalid) {
        UndoEntry entry = UNDO_ENTRIES.get(player.getUUID());
        if (entry == null) {
            return null;
        }

        long age = currentTick(player) - entry.createdTick;
        boolean valid = entry.playerId.equals(player.getUUID())
                && entry.villagerId.equals(villager.getUUID())
                && entry.containerId == menu.containerId
                && age <= RerollServerConfig.get().undoTimeoutSeconds * 20L
                && !PlatformServices.isGloballyLocked(villager);
        if (!valid && removeInvalid) {
            UNDO_ENTRIES.remove(player.getUUID());
        }
        return valid ? entry : null;
    }

    private static int remainingCooldown(ServerPlayer player) {
        long remaining = NEXT_REROLL_TICK.getOrDefault(player.getUUID(), 0L) - currentTick(player);
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, remaining));
    }

    private static long currentTick(ServerPlayer player) {
        return player.level().getServer().getTickCount();
    }

    private static void syncOffers(ServerPlayer player, MerchantMenu menu, Villager villager) {
        player.connection.send(new ClientboundMerchantOffersPacket(
                menu.containerId,
                villager.getOffers(),
                villager.getVillagerData().level(),
                villager.getVillagerXp(),
                menu.showProgressBar(),
                menu.canRestock()
        ));
    }

    private static RerollStatePayload unsupportedState(int containerId) {
        return new RerollStatePayload(
                containerId,
                false,
                false,
                RerollBlockReason.UNSUPPORTED_MERCHANT,
                false,
                0,
                -1,
                false
        );
    }

    private record UndoEntry(
            UUID playerId,
            UUID villagerId,
            int containerId,
            MerchantOffers offers,
            long createdTick
    ) {
    }
}
