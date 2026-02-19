package net.misemise;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.network.packet.s2c.play.SetTradeOffersS2CPacket;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.misemise.config.RerollConfig;
import net.misemise.mixin.MerchantScreenHandlerAccessor;
import net.misemise.mixin.VillagerEntityAccessor;
import net.misemise.network.RerollParticlePayload;
import net.misemise.network.RerollTradesPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RerollTrades implements ModInitializer {

	public static final String MOD_ID = "reroll-trades";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// Register C2S packet type (client -> server)
		PayloadTypeRegistry.playC2S().register(RerollTradesPayload.ID, RerollTradesPayload.CODEC);

		// Register S2C packet type (server -> client)
		PayloadTypeRegistry.playS2C().register(RerollParticlePayload.ID, RerollParticlePayload.CODEC);

		// Handle reroll request from client
		ServerPlayNetworking.registerGlobalReceiver(RerollTradesPayload.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			context.server().execute(() -> handleReroll(player));
		});

		LOGGER.info("Reroll Trades initialized!");
	}

	private void handleReroll(ServerPlayerEntity player) {
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
			return;
		}

		// Check that no trades have been used
		TradeOfferList offers = villager.getOffers();
		for (TradeOffer offer : offers) {
			if (offer.getUses() > 0) {
				player.sendMessage(
						Text.translatable("message.reroll-trades.already_traded").formatted(Formatting.RED),
						true);
				return;
			}
		}

		// Perform the reroll: clear offers and regenerate
		offers.clear();
		((VillagerEntityAccessor) villager)
				.invokeFillRecipes((net.minecraft.server.world.ServerWorld) villager.getEntityWorld());

		TradeOfferList newOffers = villager.getOffers();

		// Update server-side handler
		merchantHandler.setOffers(newOffers);

		// Sync new offers to client WITHOUT closing the screen
		// SetTradeOffersS2CPacket(syncId, offers, levelProgress, experience, leveled,
		// refreshable)
		int level = villager.getVillagerData().level();
		int experience = villager.getExperience();
		player.networkHandler.sendPacket(new SetTradeOffersS2CPacket(
				merchantHandler.syncId,
				newOffers,
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