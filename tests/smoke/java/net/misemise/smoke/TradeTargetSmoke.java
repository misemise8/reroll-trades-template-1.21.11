package net.misemise.smoke;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.EntityType;
//#if MC >= 12111
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
//#else
//$$ import net.minecraft.world.entity.npc.Villager;
//$$ import net.minecraft.world.entity.npc.VillagerProfession;
//#endif
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.misemise.config.RerollServerConfig;
import net.misemise.mixin.VillagerEntityAccessor;
import net.misemise.network.*;
import net.misemise.platform.PlatformServices;
import net.misemise.reroll.*;
import net.misemise.target.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Real server/registries/mixins with a synthetic player whose outbound packets are captured. */
public final class TradeTargetSmoke {
    private static int assertions;
    private static TradeTargetDataPayload targets;
    private static RerollStatePayload state;
    private static final List<String> phases = new ArrayList<>();

    public static void run(MinecraftServer server) {
            Map<String, Object> result = new LinkedHashMap<>();
            try {
                ranges();
                catalog(server);
                rules(server);
                controller(server);
                result.put("status", "passed");
            } catch (Throwable failure) {
                failure.printStackTrace();
                result.put("status", "failed");
                result.put("error", failure.toString());
            } finally {
                result.put("assertions", assertions);
                result.put("phases", phases);
                try { Files.writeString(Path.of("trade-smoke-result.json"), new GsonBuilder().setPrettyPrinting().create().toJson(result)); }
                catch (Exception e) { throw new RuntimeException(e); }
                server.halt(false);
            }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        assertions++;
    }

    private static void ranges() {
        check(PriceRange.enchantedBook(1, false).equals(new PriceRange(5, 19)), "level I range");
        check(PriceRange.enchantedBook(1, true).equals(new PriceRange(10, 38)), "mending range");
        check(PriceRange.enchantedBook(5, false).equals(new PriceRange(17, 64)), "level V cap");
        check(PriceRange.enchantedBook(5, true).equals(new PriceRange(34, 64)), "double before clamp");
        check(PriceRange.numberProvider(JsonParser.parseString("{\"type\":\"minecraft:uniform\",\"min\":5,\"max\":19}")).equals(new PriceRange(5, 19)), "uniform bounds");
        check(PriceRange.numberProvider(JsonParser.parseString("{\"type\":\"minecraft:sum\",\"summands\":[2,3]}")).equals(PriceRange.fixed(5)), "sum bounds");
        check(!PriceRange.numberProvider(JsonParser.parseString("{\"type\":\"custom:unknown\"}")).known(), "unknown provider");
        check(!PriceRange.fixed(Integer.MAX_VALUE).plus(PriceRange.fixed(1)).known(), "overflow");
        phases.add("price bounds");
    }

    private static Villager villager(MinecraftServer server, String profession, int level) {
//#if MC >= 260200
        Villager villager = new Villager(net.minecraft.world.entity.EntityTypes.VILLAGER, server.overworld());
//#else
//$$         Villager villager = new Villager(EntityType.VILLAGER, server.overworld());
//#endif
        var registry = server.registryAccess().lookupOrThrow(Registries.VILLAGER_PROFESSION);
        var holder = registry.listElements().filter(h -> TradeCatalog.key(h.key()).equals("minecraft:" + profession)).findFirst().orElseThrow();
//#if MC >= 12105
        villager.setVillagerData(villager.getVillagerData().withProfession(holder).withLevel(level));
//#else
//$$         villager.setVillagerData(villager.getVillagerData().setProfession(holder.value()).setLevel(level));
//#endif
        villager.setOffers(new MerchantOffers());
        return villager;
    }

    private static void generate(Villager villager) {
        villager.getOffers().clear();
//#if MC >= 12111
        ((VillagerEntityAccessor) villager).rerollTrades$updateTrades((net.minecraft.server.level.ServerLevel) villager.level());
//#else
//$$         ((VillagerEntityAccessor) villager).rerollTrades$updateTrades();
//#endif
    }

    private static void catalog(MinecraftServer server) {
        for (String profession : List.of("armorer", "butcher", "cartographer", "cleric", "farmer", "fisherman", "fletcher", "leatherworker", "librarian", "mason", "shepherd", "toolsmith", "weaponsmith")) {
            int count = 0;
            for (int level = 1; level <= 5; level++) {
                var candidates = TradeCatalog.create(villager(server, profession, level));
                count += candidates.size();
                check(candidates.size() <= TradeCatalog.MAX_ENTRIES, "bounded catalog " + profession);
                check(candidates.stream().allMatch(c -> c.price().known()), "known vanilla prices " + profession + "/" + level);
            }
            check(count > 0, "sale catalog for " + profession);
        }
        Villager librarian = villager(server, "librarian", 1);
        var candidates = TradeCatalog.create(librarian);
        var mending = candidates.stream().filter(c -> c.enchantment().equals("minecraft:mending")).findFirst().orElseThrow();
        check(mending.price().equals(new PriceRange(10, 38)), "catalog mending 10-38");
        check(candidates.stream().anyMatch(c -> c.buying() && c.template().is(Items.PAPER) && c.price().equals(PriceRange.fixed(24))), "paper buy target has correct quantity");
        int books = 0;
        for (int roll = 0; roll < 400; roll++) {
            generate(librarian);
            for (MerchantOffer offer : librarian.getOffers()) {
                boolean buying = !offer.getCostA().is(Items.EMERALD);
                var matches = candidates.stream().filter(c -> c.buying() == buying && c.rule("test", 999).matchesResult(buying ? offer.getCostA() : offer.getResult())).toList();
                check(!matches.isEmpty(), "generated sale belongs to catalog");
                int price = offer.getBaseCostA().getCount();
                check(matches.stream().anyMatch(c -> c.price().min() <= price && c.price().max() >= price), "generated price inside catalog range");
                if (offer.getResult().is(Items.ENCHANTED_BOOK)) books++;
            }
        }
        check(books > 30, "sampled librarian books");
        phases.add("all 13 professions / five levels; 400 librarian rolls");
        var swords = TradeCatalog.create(villager(server, "weaponsmith", 5));
        check(swords.stream().noneMatch(c -> c.enchantment().equals("minecraft:sharpness") && c.level() == 5), "unobtainable sharpness V is excluded");
    }

    private static MerchantOffer offer(int price, ItemStack result) {
        return new MerchantOffer(new ItemCost(Items.EMERALD, price), result, 12, 1, 0.05f);
    }

    private static void rules(MinecraftServer server) {
        var mending = server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.MENDING);
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        book.enchant(mending, 1);
        TradeRule rule = new TradeRule("mending", new ItemStack(Items.ENCHANTED_BOOK), "minecraft:mending", 1, 10);
        check(rule.matches(offer(10, book)), "inclusive price boundary");
        check(!rule.matches(offer(11, book)), "reject above boundary");
        check(!new TradeRule("wrong", rule.template(), rule.enchantment(), 2, 10).matches(offer(10, book)), "exact enchantment level");
        check(!rule.matches(offer(10, new ItemStack(Items.BOOK))), "different item");
        MerchantOffers offers = new MerchantOffers();
        offers.add(offer(10, book)); offers.add(offer(20, book.copy()));
        TradeRule broad = new TradeRule("broad", rule.template(), "", 0, 30);
        TradeLockData locked = new TradeLockData("minecraft:librarian", 0, List.of(broad, rule), List.of()).lockMatches(offers);
        check(locked.locks().size() == 2, "overlapping rules both get available slots");
        check(locked.locks().stream().anyMatch(l -> l.ruleId().equals("mending") && l.slot() == 0), "narrow rule gets eligible slot");
        offers.get(0).setSpecialPriceDiff(5);
        check(locked.sanitize(locked.profession(), offers).locks().size() == 2, "discount changes preserve locks");
        var ops = RegistryOps.create(JsonOps.INSTANCE, server.registryAccess());
        var encoded = TradeLockData.CODEC.encodeStart(ops, locked).getOrThrow();
        TradeLockData decoded = TradeLockData.CODEC.parse(ops, encoded).getOrThrow();
        check(decoded.sanitize(locked.profession(), offers).locks().size() == 2, "persistence codec restores locks");
        check(decoded.sanitize("minecraft:farmer", offers).rules().isEmpty(), "profession change clears obsolete targets");
        offers.set(0, offer(1, new ItemStack(Items.APPLE)));
        check(decoded.sanitize(locked.profession(), offers).locks().size() == 1, "replaced offer clears stale lock");
        TradeRule bounded = new TradeRule("bounded", rule.template(), rule.enchantment(), 1, 10, 5, false);
        check(!bounded.matches(offer(4, book)) && bounded.matches(offer(5, book)) && bounded.matches(offer(10, book)), "inclusive minimum and maximum");
        TradeRule paper = new TradeRule("paper", new ItemStack(Items.PAPER), "", 0, 24, 20, true);
        MerchantOffer buy = new MerchantOffer(new ItemCost(Items.PAPER, 24), new ItemStack(Items.EMERALD), 12, 1, 0.05f);
        check(paper.matches(buy), "buy rule matches input count");
        check(!paper.matches(offer(24, new ItemStack(Items.PAPER))), "buy rule cannot match sale of same item");
        buy.setSpecialPriceDiff(-5);
        check(!paper.matches(buy), "buy rule rejects quantity below minimum");
        var buyEncoded = TradeRule.CODEC.encodeStart(ops, paper).getOrThrow();
        TradeRule buyDecoded = TradeRule.CODEC.parse(ops, buyEncoded).getOrThrow();
        check(buyDecoded.buying() && buyDecoded.minPrice() == 20 && buyDecoded.maxEmeralds() == 24, "buy bounds persist");
        var legacyEncoded = TradeRule.CODEC.encodeStart(ops, rule).getOrThrow().getAsJsonObject();
        legacyEncoded.remove("min_price"); legacyEncoded.remove("buying");
        TradeRule legacy = TradeRule.CODEC.parse(ops, legacyEncoded).getOrThrow();
        check(legacy.minPrice() == 1 && !legacy.buying() && legacy.matches(offer(10, book)), "old saved target migration");
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), server.registryAccess());
        var action = new TradeTargetActionPayload(7, 1, 3, 5, 24, 20);
        TradeTargetActionPayload.STREAM_CODEC.encode(buffer, action);
        check(action.equals(TradeTargetActionPayload.STREAM_CODEC.decode(buffer)), "price bounds action round trip");
        var payload = new TradeTargetDataPayload(7, 3, List.of(new TradeCandidate(new ItemStack(Items.PAPER), "", 0, PriceRange.fixed(24), false, true)), List.of(paper), List.of(-1), true, "");
        TradeTargetDataPayload.STREAM_CODEC.encode(buffer, payload);
        var decodedPayload = TradeTargetDataPayload.STREAM_CODEC.decode(buffer);
        check(decodedPayload.catalog().getFirst().buying() && decodedPayload.rules().getFirst().minPrice() == 20, "buy rules network round trip");
        buffer.release();
        phases.add("price/enchantment matching; buy quantities; lower bound; old-save migration; packet codecs; overlapping targets; changed offers");
    }

    private static void controller(MinecraftServer server) throws Exception {
        GameProfile profile = new GameProfile(UUID.randomUUID(), "TargetTest");
        ServerPlayer player = new ServerPlayer(server, server.overworld(), profile, ClientInformation.createDefault());
        player.connection = new ServerGamePacketListenerImpl(server, new Connection(PacketFlow.SERVERBOUND), player, CommonListenerCookie.createInitial(profile, false)) {
            @Override public void send(Packet<?> packet) {
                if (packet instanceof ClientboundCustomPayloadPacket custom) {
                    if (custom.payload() instanceof TradeTargetDataPayload payload) targets = payload;
                    if (custom.payload() instanceof RerollStatePayload payload) state = payload;
                }
            }
        };
        RerollServerConfig.get().cooldownTicks = 0;
        Villager villager = villager(server, "farmer", 1);
        MerchantOffers offers = new MerchantOffers();
        offers.add(offer(1, new ItemStack(Items.BREAD, 6)));
        offers.add(offer(20, new ItemStack(Items.APPLE)));
        villager.setOffers(offers);
        villager.setTradingPlayer(player);
        MerchantMenu menu = new MerchantMenu(7, player.getInventory(), villager);
        player.containerMenu = menu;
        TradeTargetController.handle(player, new TradeTargetActionPayload(7, 0, 0, 0, 0));
        check(targets != null && !targets.catalog().isEmpty(), "catalog response");
        int bread = -1;
        for (int i = 0; i < targets.catalog().size(); i++) if (targets.catalog().get(i).template().is(Items.BREAD)) { bread = i; break; }
        check(bread >= 0, "bread selection");
        TradeTargetController.handle(player, new TradeTargetActionPayload(7, 1, targets.revision(), bread, 10, 11));
        check(targets.rules().isEmpty() && targets.message().equals("target.reroll-trades.invalid_price"), "server rejects inverted bounds");
        TradeTargetController.handle(player, new TradeTargetActionPayload(7, 1, targets.revision(), bread, 10, 0));
        check(targets.rules().isEmpty(), "server rejects nonpositive minimum");
        TradeTargetController.handle(player, new TradeTargetActionPayload(7, 1, targets.revision(), bread, 10));
        check(targets.rules().size() == 1 && targets.lockedSlots().get(0) == 0, "saving target locks current matching slot");
        MerchantOffer fixed = villager.getOffers().get(0).copy();
        for (int roll = 0; roll < 30; roll++) {
            RerollController.handleAction(player, RerollAction.REROLL, 7);
            check(new TradeLockData.LockedOffer("", 0, fixed).sameOffer(villager.getOffers().get(0)), "locked slot survives reroll");
            check(state.canUndo(), "undo available while another slot is free");
            RerollController.handleAction(player, RerollAction.UNDO, 7);
            check(new TradeLockData.LockedOffer("", 0, fixed).sameOffer(villager.getOffers().get(0)), "locked slot survives undo");
            check(villager.getOffers().get(1).getResult().is(Items.APPLE), "undo restores unlocked slot");
        }
        // Force a newly generated target in the formerly free slot, then exercise Undo.
        RerollController.handleAction(player, RerollAction.REROLL, 7);
        var generated = villager.getOffers().get(1);
        check(!generated.getResult().is(Items.APPLE), "unlocked slot rerolls");
        check(PlatformServices.getTradeLocks(villager).locks().size() == 1, "exactly one slot per rule");
        TradeTargetController.handle(player, new TradeTargetActionPayload(7, 2, -1, 0, 0));
        check(targets.rules().size() == 1, "stale revision rejected");
        TradeTargetController.handle(player, new TradeTargetActionPayload(999, 2, targets.revision(), 0, 0));
        check(PlatformServices.getTradeLocks(villager).rules().size() == 1, "wrong menu rejected");
        TradeTargetController.handle(player, new TradeTargetActionPayload(7, 2, targets.revision(), 0, 0));
        check(targets.rules().isEmpty() && targets.lockedSlots().isEmpty(), "remove releases fixed slot");
        TradeTargetController.handle(player, new TradeTargetActionPayload(7, 1, targets.revision(), bread, 10));
        // Test loader attachment serialization through the actual entity save/load path.
        Villager restored = villager(server, "farmer", 1);
//#if MC >= 12106
        var output = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, server.registryAccess());
        villager.saveWithoutId(output);
        restored.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, server.registryAccess(), output.buildResult()));
//#else
//$$         var tag = new net.minecraft.nbt.CompoundTag();
//$$         villager.saveWithoutId(tag);
//$$         restored.load(tag);
//#endif
        check(PlatformServices.getTradeLocks(restored).rules().size() == 1, "entity attachment survives save/load");
        check(TradeTargetController.current(restored).locks().size() == 1, "fixed slot survives save/load");
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), server.registryAccess());
        TradeTargetDataPayload.STREAM_CODEC.encode(buffer, targets);
        TradeTargetDataPayload roundTrip = TradeTargetDataPayload.STREAM_CODEC.decode(buffer);
        check(roundTrip.catalog().size() == targets.catalog().size() && roundTrip.rules().size() == 1, "network round trip");
        buffer.release();
        RerollController.onTrade(player, villager);
        MerchantOffers beforeBlocked = villager.getOffers().copy();
        RerollController.handleAction(player, RerollAction.REROLL, 7);
        check(state.reason() == RerollBlockReason.ALREADY_TRADED, "completed trade prohibits reroll");
        check(new TradeLockData.LockedOffer("", 0, beforeBlocked.get(0)).sameOffer(villager.getOffers().get(0)), "blocked reroll preserves offers");
        phases.add("controller reroll/undo x30; revisions; menu ownership; remove; entity persistence; packet codec; completed trade block");
        delayedAndAllLocked(server, player);
        RerollController.onPlayerLogout(player);
    }

    private static void delayedAndAllLocked(MinecraftServer server, ServerPlayer player) {
        Villager villager = villager(server, "farmer", 1);
        MerchantOffers initial = new MerchantOffers();
        initial.add(offer(3, new ItemStack(Items.PORKCHOP)));
        initial.add(offer(3, new ItemStack(Items.PORKCHOP)));
        villager.setOffers(initial);
        villager.setTradingPlayer(player);
        player.containerMenu = new MerchantMenu(8, player.getInventory(), villager);
        PlatformServices.setTradeLocks(villager, new TradeLockData("minecraft:farmer", 0,
                List.of(new TradeRule("bread", new ItemStack(Items.BREAD), "", 0, 1)), List.of()));
        check(TradeTargetController.current(villager).locks().isEmpty(), "target starts waiting");
        for (int attempt = 0; attempt < 100 && TradeTargetController.current(villager).locks().isEmpty(); attempt++)
            RerollController.handleAction(player, RerollAction.REROLL, 8);
        var data = TradeTargetController.current(villager);
        check(data.locks().size() == 1, "newly generated match locks automatically");
        int slot = data.locks().getFirst().slot();
        RerollController.handleAction(player, RerollAction.UNDO, 8);
        check(villager.getOffers().get(slot).getResult().is(Items.BREAD), "undo preserves newly acquired target");
        MerchantOffers all = new MerchantOffers();
        all.add(offer(1, new ItemStack(Items.BREAD)));
        all.add(offer(1, new ItemStack(Items.APPLE)));
        villager.setOffers(all);
        PlatformServices.setTradeLocks(villager, new TradeLockData("minecraft:farmer", data.revision() + 1,
                List.of(new TradeRule("bread", new ItemStack(Items.BREAD), "", 0, 1),
                        new TradeRule("apple", new ItemStack(Items.APPLE), "", 0, 1)), List.of()));
        RerollController.handleAction(player, RerollAction.REROLL, 8);
        check(state.reason() == RerollBlockReason.ALL_LOCKED && !state.canReroll(), "all slots fixed blocks reroll");
        check(villager.getOffers().get(1).getResult().is(Items.APPLE), "blocked all-fixed reroll preserves offers");
        phases.add("waiting target automatically locks; new locks survive undo; all-fixed state");
    }
}
