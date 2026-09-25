//#if MC >= 260100
package net.misemise.target;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.misemise.mixin.catalog.ModernTradeAccessor;
import java.util.List;
import java.util.Optional;

final class ModernTradeCatalog {
    private ModernTradeCatalog() {}

    static void populate(TradeCatalog.Builder b) {
        var villager = b.villager;
        var key = villager.getVillagerData().profession().value().getTrades(TradeCatalog.level(villager));
        if (key == null) return;
        var tradeSet = villager.registryAccess().lookupOrThrow(Registries.TRADE_SET).get(key);
        if (tradeSet.isEmpty()) return;
        var ops = RegistryOps.create(JsonOps.INSTANCE, villager.registryAccess());
        LootContext context = new LootContext.Builder(new LootParams.Builder((ServerLevel) villager.level())
                .withParameter(LootContextParams.ORIGIN, villager.position())
                .withParameter(LootContextParams.THIS_ENTITY, villager)
                .withParameter(LootContextParams.ADDITIONAL_COST_COMPONENT_ALLOWED, Unit.INSTANCE)
                .create(LootContextParamSets.VILLAGER_TRADE)).create(Optional.empty());
        for (var holder : tradeSet.get().value().getTrades()) {
            ModernTradeAccessor accessor = (ModernTradeAccessor) holder.value();
            if (accessor.rerollTrades$predicate().isPresent() && !accessor.rerollTrades$predicate().get().test(context)) continue;
            var encoded = VillagerTrade.CODEC.encodeStart(ops, holder.value()).result();
            if (encoded.isEmpty()) continue;
            JsonObject trade = encoded.get().getAsJsonObject();
            JsonObject wants = trade.getAsJsonObject("wants");
            if (!wants.get("id").getAsString().equals("minecraft:emerald")) continue;
            ItemStack item = accessor.rerollTrades$gives().create();
            PriceRange price = PriceRange.numberProvider(wants.get("count"));
            JsonObject randomBook = null;
            JsonObject fixedBook = null;
            boolean equipment = false;
            JsonObject equipmentFunction = null;
            boolean unknownPrice = false;
            if (trade.has("given_item_modifiers")) for (JsonElement element : trade.getAsJsonArray("given_item_modifiers")) {
                JsonObject function = element.getAsJsonObject();
                String type = function.get("function").getAsString().replace("minecraft:", "");
                switch (type) {
                    case "enchant_randomly" -> {
                        if (item.is(Items.ENCHANTED_BOOK) || item.is(Items.BOOK)) {
                            randomBook = function;
                        } else {
                            equipment = true;
                            if (additionalCost(function)) unknownPrice = true;
                        }
                    }
                    case "enchant_with_levels" -> {
                        equipment = true;
                        equipmentFunction = function;
                        if (additionalCost(function)) price = price.plus(PriceRange.numberProvider(function.get("levels")));
                    }
                    case "set_enchantments" -> {
                        fixedBook = function;
                    }
                    case "exploration_map" -> item = new ItemStack(Items.FILLED_MAP);
                    case "set_name" -> {
                        var name = ComponentSerialization.CODEC.parse(ops, function.get("name")).result();
                        if (name.isPresent()) item.set(DataComponents.ITEM_NAME, name.get());
                    }
                    case "filtered" -> {
                        // Vanilla uses this only to discard a failed enchantment. Conditional transforms need unknown bounds.
                        if (function.has("on_pass")) unknownPrice = true;
                    }
                    case "set_stew_effect", "set_random_potion", "set_random_dyes", "set_potion" -> { }
                    default -> unknownPrice = true;
                }
            }
            if (unknownPrice) price = PriceRange.UNKNOWN;
            if (randomBook != null) {
                for (var enchantment : enchantments(b, randomBook.get("options"))) {
                    boolean doubled = selected(enchantment, trade.get("double_trade_price_enchantments"));
                    if (additionalCost(randomBook) && !unknownPrice) {
                        b.book(enchantment, 1, 255, price, doubled);
                    } else {
                        for (int level = enchantment.value().getMinLevel(); level <= Math.min(255, enchantment.value().getMaxLevel()); level++) {
                            b.add(new ItemStack(Items.ENCHANTED_BOOK), enchantment.unwrapKey().map(TradeCatalog::key).orElse(""), level, price.clamp(64), false);
                        }
                    }
                }
            } else if (fixedBook != null && fixedBook.has("enchantments")) {
                ItemStack result = item.is(Items.BOOK) ? new ItemStack(Items.ENCHANTED_BOOK) : item;
                if (!result.is(Items.ENCHANTED_BOOK)) b.item(result, price.clamp(64), false);
                for (var entry : fixedBook.getAsJsonObject("enchantments").entrySet()) {
                    PriceRange levels = PriceRange.numberProvider(entry.getValue());
                    if (!levels.known()) continue;
                    for (int level = Math.max(1, levels.min()); level <= Math.min(255, levels.max()); level++) {
                        b.add(result, entry.getKey(), level, price.clamp(64), false);
                    }
                }
            } else if (equipmentFunction != null) {
                b.equipment(item, price.clamp(64), PriceRange.numberProvider(equipmentFunction.get("levels")),
                        enchantments(b, equipmentFunction.get("options")));
            } else {
                b.item(item, price.clamp(64), equipment);
            }
        }
    }

    private static boolean additionalCost(JsonObject function) {
        return function.has("include_additional_cost_component") && function.get("include_additional_cost_component").getAsBoolean();
    }

    private static List<Holder.Reference<Enchantment>> enchantments(TradeCatalog.Builder b, JsonElement options) {
        return b.villager.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements()
                .filter(holder -> options == null || selected(holder, options)).toList();
    }

    private static boolean selected(Holder<Enchantment> holder, JsonElement selection) {
        if (selection == null) return false;
        if (selection.isJsonArray()) {
            for (JsonElement item : selection.getAsJsonArray()) if (selected(holder, item)) return true;
            return false;
        }
        if (!selection.isJsonPrimitive()) return false;
        String value = selection.getAsString();
        return value.startsWith("#") ? holder.is(TagKey.create(Registries.ENCHANTMENT, Identifier.parse(value.substring(1))))
                : holder.unwrapKey().map(TradeCatalog::key).orElse("").equals(value);
    }
}
//#endif
