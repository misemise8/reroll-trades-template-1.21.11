package net.misemise.target;

import net.minecraft.server.level.ServerPlayer;
//#if MC >= 12111
import net.minecraft.world.entity.npc.villager.Villager;
//#else
//$$ import net.minecraft.world.entity.npc.Villager;
//#endif
import net.minecraft.world.inventory.MerchantMenu;
import net.misemise.mixin.MerchantScreenHandlerAccessor;
import net.misemise.network.TradeTargetActionPayload;
import net.misemise.network.TradeTargetDataPayload;
import net.misemise.platform.PlatformServices;
import net.misemise.reroll.RerollAction;
import net.misemise.reroll.RerollController;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TradeTargetController {
    private static final Map<UUID, CatalogSession> SESSIONS = new HashMap<>();
    private TradeTargetController() {}

    public static TradeLockData current(Villager villager) {
        TradeLockData saved = PlatformServices.getTradeLocks(villager);
        TradeLockData current = saved.sanitize(TradeCatalog.profession(villager), villager.getOffers()).lockMatches(villager.getOffers());
        if (saved != current) PlatformServices.setTradeLocks(villager, current);
        return current;
    }

    public static void close(ServerPlayer player) { SESSIONS.remove(player.getUUID()); }
    public static void clearSessions() { SESSIONS.clear(); }

    public static void handle(ServerPlayer player, TradeTargetActionPayload action) {
        if (!(player.containerMenu instanceof MerchantMenu menu) || menu.containerId != action.containerId()) return;
        if (!(((MerchantScreenHandlerAccessor) menu).rerollTrades$getMerchant() instanceof Villager villager)
                || villager.getTradingPlayer() != player) return;
        long now = player.level().getServer().getTickCount();
        CatalogSession session = SESSIONS.get(player.getUUID());
        boolean validSession = session != null && session.matches(menu, villager);
        if (action.action() == TradeTargetActionPayload.REQUEST && validSession && now - session.lastSent < 5) return;
        // Keep the catalog that the editor displayed stable while the player chooses a target.
        if (!validSession || (action.action() == TradeTargetActionPayload.REQUEST && now - session.created > 200)) {
            session = new CatalogSession(menu.containerId, villager.getUUID(), TradeCatalog.profession(villager),
                    TradeCatalog.level(villager), TradeCatalog.create(villager), now, now);
            SESSIONS.put(player.getUUID(), session);
            if (action.action() != TradeTargetActionPayload.REQUEST) {
                send(player, menu, villager, session, "target.reroll-trades.changed");
                return;
            }
        }
        TradeLockData data = current(villager);
        if (action.action() == TradeTargetActionPayload.REQUEST) {
            send(player, menu, villager, session, "");
            return;
        }
        if (PlatformServices.isGloballyLocked(villager)) {
            send(player, menu, villager, session, "reason.reroll-trades.already_traded");
            return;
        }
        if (action.revision() != data.revision()) {
            send(player, menu, villager, session, "target.reroll-trades.changed");
            return;
        }
        List<TradeRule> rules = new ArrayList<>(data.rules());
        List<TradeLockData.LockedOffer> locks = new ArrayList<>(data.locks());
        if (action.action() == TradeTargetActionPayload.ADD) {
            if (action.selection() < 0 || action.selection() >= session.catalog.size()
                    || action.maxEmeralds() < 1 || action.maxEmeralds() > 999) return;
            TradeRule rule = session.catalog.get(action.selection()).rule(UUID.randomUUID().toString(), action.maxEmeralds());
            TradeRule previous = rules.stream().filter(existing -> existing.sameTarget(rule)).findFirst().orElse(null);
            if (previous == null && rules.size() >= TradeLockData.MAX_RULES) {
                send(player, menu, villager, session, "target.reroll-trades.rule_limit");
                return;
            }
            if (previous != null) {
                rules.remove(previous);
                locks.removeIf(lock -> lock.ruleId().equals(previous.id()));
            }
            rules.add(rule);
        } else if (action.action() == TradeTargetActionPayload.REMOVE) {
            if (action.selection() < 0 || action.selection() >= rules.size()) return;
            TradeRule removed = rules.remove(action.selection());
            locks.removeIf(lock -> lock.ruleId().equals(removed.id()));
        } else return;
        PlatformServices.setTradeLocks(villager, new TradeLockData(data.profession(), data.revision() + 1, rules, locks).lockMatches(villager.getOffers()));
        RerollController.invalidateUndo(player);
        send(player, menu, villager, session, "");
        RerollController.handleAction(player, RerollAction.REQUEST_STATE, menu.containerId);
    }

    private static void send(ServerPlayer player, MerchantMenu menu, Villager villager, CatalogSession session, String message) {
        TradeLockData data = current(villager);
        List<Integer> slots = data.rules().stream().map(rule -> data.locks().stream()
                .filter(lock -> lock.ruleId().equals(rule.id())).map(TradeLockData.LockedOffer::slot).findFirst().orElse(-1)).toList();
        PlatformServices.sendTargets(player, new TradeTargetDataPayload(menu.containerId, data.revision(), session.catalog,
                data.rules(), slots, !PlatformServices.isGloballyLocked(villager), message));
        SESSIONS.put(player.getUUID(), new CatalogSession(session.containerId, session.villager, session.profession,
                session.level, session.catalog, session.created, player.level().getServer().getTickCount()));
    }

    private record CatalogSession(int containerId, UUID villager, String profession, int level,
            List<TradeCandidate> catalog, long created, long lastSent) {
        boolean matches(MerchantMenu menu, Villager entity) {
            return containerId == menu.containerId && villager.equals(entity.getUUID())
                    && profession.equals(TradeCatalog.profession(entity)) && level == TradeCatalog.level(entity);
        }
    }
}
