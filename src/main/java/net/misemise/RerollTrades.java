package net.misemise;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.network.packet.s2c.play.SetTradeOffersS2CPacket;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.TradeOffers;
import net.minecraft.village.VillagerProfession;
import net.misemise.config.RerollConfig;
import net.misemise.mixin.MerchantScreenHandlerAccessor;
import net.misemise.network.RerollLockedPayload;
import net.misemise.network.RerollParticlePayload;
import net.misemise.network.RerollRejectPayload;
import net.misemise.network.RerollTradesPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class RerollTrades implements ModInitializer {

	public static final String MOD_ID = "reroll-trades";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/**
	 * Persistent Data Attachment on VillagerEntity.
	 * Stores the UUIDs of players who have traded with this villager.
	 * Survives server restarts via NBT serialization.
	 * Set by VillagerEntityTradeMixin#onAfterUsing when a player completes a trade.
	 * Never cleared — once locked, always locked for this player+villager pair.
	 */
	// 1.21 / 1.21.1: AttachmentRegistry.create() does not exist yet.
	// Use AttachmentRegistry.builder() instead.
	// Note: create() was added in 1.21.4; builder() was deprecated at that point.
	public static final AttachmentType<HashSet<UUID>> REROLL_LOCKED = AttachmentRegistry.<HashSet<UUID>>builder()
			.persistent(Codec.list(
					Codec.STRING.xmap(UUID::fromString, UUID::toString))
					.xmap(HashSet::new, ArrayList::new))
			.initializer(HashSet::new)
			.buildAndRegister(Identifier.of(MOD_ID, "locked_players"));

	/**
	 * In-memory guard to prevent a player from having two reroll operations
	 * executing concurrently within the same server tick.
	 *
	 * Note: because reroll tasks are dispatched via server().execute(), they are
	 * always processed sequentially on the server thread. This guard therefore
	 * only blocks if somehow two tasks begin in the exact same execution frame
	 * (which is not possible with Minecraft's single-threaded server model).
	 * It is kept as a safety net and for clarity of intent.
	 */
	private static final Set<UUID> IN_PROGRESS = Collections.synchronizedSet(new HashSet<>());

	@Override
	public void onInitialize() {
		// Register C2S packet type (client -> server: reroll request)
		PayloadTypeRegistry.playC2S().register(RerollTradesPayload.ID, RerollTradesPayload.CODEC);

		// Register S2C packet types (server -> client)
		PayloadTypeRegistry.playS2C().register(RerollParticlePayload.ID, RerollParticlePayload.CODEC);
		PayloadTypeRegistry.playS2C().register(RerollLockedPayload.ID, RerollLockedPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(RerollRejectPayload.ID, RerollRejectPayload.CODEC);

		// Handle reroll request from client
		ServerPlayNetworking.registerGlobalReceiver(RerollTradesPayload.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			context.server().execute(() -> handleReroll(player));
		});

		LOGGER.info("Reroll Trades initialized!");
	}

	private void handleReroll(ServerPlayerEntity player) {
		// ⑤ Guard against rapid duplicate packets from the same player
		if (!IN_PROGRESS.add(player.getUuid()))
			return;
		try {
			handleRerollInternal(player);
		} finally {
			IN_PROGRESS.remove(player.getUuid());
		}
	}

	private void handleRerollInternal(ServerPlayerEntity player) {
		// Must have a MerchantScreenHandler open
		if (!(player.currentScreenHandler instanceof MerchantScreenHandler merchantHandler)) {
			return;
		}

		// Must be trading with a VillagerEntity (not Wandering Trader)
		if (!(((MerchantScreenHandlerAccessor) merchantHandler).getMerchant() instanceof VillagerEntity villager)) {
			return;
		}

		// Check sneak requirement
		RerollConfig config = RerollConfig.get();
		if (config.requireSneaking && !player.isSneaking()) {
			player.sendMessage(
					Text.translatable("message.reroll-trades.must_sneak").formatted(Formatting.RED),
					true);
			ServerPlayNetworking.send(player, new RerollRejectPayload());
			return;
		}

		// Check persistent lock: has this player already traded with this villager?
		// This persists across server restarts via Data Attachment.
		HashSet<UUID> locked = villager.getAttached(REROLL_LOCKED);
		if (locked != null && locked.contains(player.getUuid())) {
			player.sendMessage(
					Text.translatable("message.reroll-trades.already_traded").formatted(Formatting.RED),
					true);
			ServerPlayNetworking.send(player, new RerollLockedPayload());
			return;
		}

		// Generate new offers directly from the profession's trade table.
		// We do NOT call fillRecipes() to avoid side-effects that cause
		// temporary profession loss.
		// 1.21 / 1.21.1: VillagerData.profession() returns VillagerProfession directly.
		VillagerProfession profession = villager.getVillagerData().getProfession();
		int level = villager.getVillagerData().getLevel();

		// 1.21 / 1.21.1: PROFESSION_TO_LEVELED_TRADE is keyed by VillagerProfession.
		Int2ObjectMap<TradeOffers.Factory[]> leveledTrades = TradeOffers.PROFESSION_TO_LEVELED_TRADE.get(profession);

		if (leveledTrades == null) {
			// Nitwit or unemployed — correct, specific message
			player.sendMessage(
					Text.translatable("message.reroll-trades.no_profession").formatted(Formatting.RED),
					true);
			ServerPlayNetworking.send(player, new RerollRejectPayload());
			return;
		}

		Random random = Random.create();
		TradeOfferList newOffers = new TradeOfferList();

		for (int lvl = 1; lvl <= level; lvl++) {
			TradeOffers.Factory[] factories = leveledTrades.get(lvl);
			if (factories == null)
				continue;

			// Fisher-Yates shuffle for randomness (same as vanilla)
			int offerCount = Math.min(factories.length, lvl == 1 ? 2 : 1);
			int[] indices = new int[factories.length];
			for (int i = 0; i < indices.length; i++)
				indices[i] = i;
			for (int i = indices.length - 1; i > 0; i--) {
				int j = random.nextBetween(0, i);
				int tmp = indices[i];
				indices[i] = indices[j];
				indices[j] = tmp;
			}
			for (int i = 0; i < offerCount; i++) {
				// 1.21 – 1.21.3: Factory.create takes (Entity, Random) — ServerWorld added in
				// 1.21.4.
				TradeOffer offer = factories[indices[i]].create(villager, random);
				if (offer != null)
					newOffers.add(offer);
			}
		}

		if (newOffers.isEmpty()) {
			LOGGER.warn("No trades generated for villager at {}, reroll skipped.", villager.getBlockPos());
			return;
		}

		// Replace offers in-place (no fillRecipes side-effects)
		TradeOfferList villagerOffers = villager.getOffers();
		villagerOffers.clear();
		villagerOffers.addAll(newOffers);

		// Note: the trade lock is NOT cleared on reroll.
		// The lock is only removed when the player opens the screen fresh (no lock in
		// attachment).
		// When a new trade occurs, VillagerEntityTradeMixin re-applies the lock.

		// Update server-side handler
		merchantHandler.setOffers(villagerOffers);

		// Sync new offers to client WITHOUT closing the screen
		player.networkHandler.sendPacket(new SetTradeOffersS2CPacket(
				merchantHandler.syncId,
				villagerOffers,
				level,
				villager.getExperience(),
				true, // leveled villager
				false // not refreshable
		));

		// Send particle effect to client
		if (config.enableParticles) {
			BlockPos pos = villager.getBlockPos();
			ServerPlayNetworking.send(player, new RerollParticlePayload(pos));
		}

		LOGGER.debug("Player {} rerolled trades for villager at {}",
				player.getName().getString(), villager.getBlockPos());
	}
}
