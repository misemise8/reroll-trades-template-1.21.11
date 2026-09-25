package net.misemise.target;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.Map;

public record TradeLockData(String profession, int revision, List<TradeRule> rules, List<LockedOffer> locks) {
    public static final int MAX_RULES = 16;
    public static final TradeLockData EMPTY = new TradeLockData("", 0, List.of(), List.of());
    public static final Codec<TradeLockData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("profession").forGetter(TradeLockData::profession),
            Codec.INT.optionalFieldOf("revision", 0).forGetter(TradeLockData::revision),
            TradeRule.CODEC.listOf().fieldOf("rules").forGetter(TradeLockData::rules),
            LockedOffer.CODEC.listOf().fieldOf("locks").forGetter(TradeLockData::locks)
    ).apply(i, TradeLockData::new));

    public TradeLockData {
        rules = List.copyOf(rules);
        locks = List.copyOf(locks);
    }

    public TradeLockData sanitize(String currentProfession, MerchantOffers offers) {
        if (!profession.equals(currentProfession)) return new TradeLockData(currentProfession, revision + 1, List.of(), List.of());
        List<TradeRule> validRules = rules.stream().limit(MAX_RULES).toList();
        Set<Integer> slots = new HashSet<>();
        Set<String> ids = new HashSet<>();
        List<LockedOffer> valid = new ArrayList<>();
        for (LockedOffer lock : locks) {
            if (lock.slot < 0 || lock.slot >= offers.size() || slots.contains(lock.slot) || ids.contains(lock.ruleId)
                    || validRules.stream().noneMatch(rule -> rule.id().equals(lock.ruleId))
                    || !lock.sameOffer(offers.get(lock.slot))) continue;
            valid.add(lock);
            slots.add(lock.slot);
            ids.add(lock.ruleId);
        }
        return valid.size() == locks.size() && validRules.size() == rules.size() ? this
                : new TradeLockData(profession, revision + 1, validRules, valid);
    }

    public TradeLockData lockMatches(MerchantOffers offers) {
        List<LockedOffer> next = new ArrayList<>(locks);
        Set<Integer> slots = new HashSet<>();
        Set<String> ids = new HashSet<>();
        for (LockedOffer lock : locks) { slots.add(lock.slot); ids.add(lock.ruleId); }
        Map<Integer, TradeRule> assignments = new LinkedHashMap<>();
        for (TradeRule rule : rules) if (ids.add(rule.id())) assign(rule, offers, slots, assignments, new HashSet<>());
        assignments.forEach((slot, rule) -> next.add(new LockedOffer(rule.id(), slot, offers.get(slot).copy())));
        return next.size() == locks.size() ? this : new TradeLockData(profession, revision + 1, rules, next);
    }

    // Move only new assignments when overlapping rules compete for a slot. Existing locks never move.
    private static boolean assign(TradeRule rule, MerchantOffers offers, Set<Integer> locked,
            Map<Integer, TradeRule> assignments, Set<Integer> visited) {
        for (int slot = 0; slot < offers.size(); slot++) {
            if (locked.contains(slot) || !rule.matches(offers.get(slot)) || !visited.add(slot)) continue;
            TradeRule occupant = assignments.get(slot);
            if (occupant == null || assign(occupant, offers, locked, assignments, visited)) {
                assignments.put(slot, rule);
                return true;
            }
        }
        return false;
    }

    public record LockedOffer(String ruleId, int slot, MerchantOffer snapshot) {
        public static final Codec<LockedOffer> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("rule").forGetter(LockedOffer::ruleId),
                Codec.INT.fieldOf("slot").forGetter(LockedOffer::slot),
                MerchantOffer.CODEC.fieldOf("offer").forGetter(LockedOffer::snapshot)
        ).apply(i, LockedOffer::new));

        public boolean sameOffer(MerchantOffer offer) {
            return sameStack(snapshot.getResult(), offer.getResult())
                    && sameStack(snapshot.getBaseCostA(), offer.getBaseCostA())
                    && sameStack(snapshot.getCostB(), offer.getCostB());
        }

        private static boolean sameStack(ItemStack a, ItemStack b) {
            return a.getCount() == b.getCount() && ItemStack.isSameItemSameComponents(a, b);
        }
    }
}
