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
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
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
	public static final AttachmentType<HashSet<UUID>> REROLL_LOCKED = AttachmentRegistry.create(
			Identifier.of(MOD_ID, "locked_players"),
			builder -> builder
					.persistent(Codec.list(Codec.STRING.xmap(UUID::fromString, UUID::toString))
							.xmap(HashSet::new, ArrayList::new))
					.initializer(HashSet::new));

	private static final Set<UUID> IN_PROGRESS = Collections.synchronizedSet(new HashSet<>());

	@Override
	public void onInitialize() {
		// Register C2S packet type (client -> server)
		PayloadTypeRegistry.playC2S().register(RerollTradesPayload.ID, RerollTradesPayload.CODEC);

		// Register S2C packet type (server -> client)
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
		if (!IN_PROGRESS.add(player.getUuid())) {
			return;
		}

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

		HashSet<UUID> locked = villager.getAttached(REROLL_LOCKED);
		if (locked != null && locked.contains(player.getUuid())) {
			player.sendMessage(
					Text.translatable("message.reroll-trades.already_traded").formatted(Formatting.RED),
					true);
			return;
		}

		RegistryEntry<VillagerProfession> professionEntry = villager.getVillagerData().profession();
		int level = villager.getVillagerData().level();
		Int2ObjectMap<TradeOffers.Factory[]> leveledTrades = professionEntry.getKey()
				.map(TradeOffers.PROFESSION_TO_LEVELED_TRADE::get)
				.orElse(null);

		if (leveledTrades == null) {
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
			if (factories == null) {
				continue;
			}

			int offerCount = Math.min(factories.length, lvl == 1 ? 2 : 1);
			int[] indices = new int[factories.length];
			for (int i = 0; i < indices.length; i++) {
				indices[i] = i;
			}
			for (int i = indices.length - 1; i > 0; i--) {
				int j = random.nextBetween(0, i);
				int tmp = indices[i];
				indices[i] = indices[j];
				indices[j] = tmp;
			}
			for (int i = 0; i < offerCount; i++) {
				TradeOffer offer = factories[indices[i]].create((ServerWorld) villager.getEntityWorld(), villager, random);
				if (offer != null) {
					newOffers.add(offer);
				}
			}
		}

		if (newOffers.isEmpty()) {
			LOGGER.warn("No trades generated for villager at {}, reroll skipped.", villager.getBlockPos());
			ServerPlayNetworking.send(player, new RerollRejectPayload());
			return;
		}

		TradeOfferList villagerOffers = villager.getOffers();
		villagerOffers.clear();
		villagerOffers.addAll(newOffers);

		// Update server-side handler
		merchantHandler.setOffers(villagerOffers);

		// Sync new offers to client WITHOUT closing the screen
		// SetTradeOffersS2CPacket(syncId, offers, levelProgress, experience, leveled,
		// refreshable)
		int experience = villager.getExperience();
		player.networkHandler.sendPacket(new SetTradeOffersS2CPacket(
				merchantHandler.syncId,
				villagerOffers,
				level,
				experience,
				true, // leveled (villager)
				false // refreshable
		));

		// Send particle effect to client
		if (config.enableParticles) {
			BlockPos villagerPos = villager.getBlockPos();
			ServerPlayNetworking.send(player, new RerollParticlePayload(villagerPos));
		}

		LOGGER.debug("Player {} rerolled trades for villager at {}",
				player.getName().getString(), villager.getBlockPos());
	}
}
