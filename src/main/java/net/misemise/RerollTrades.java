package net.misemise;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
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
		PayloadTypeRegistry.serverboundPlay().register(RerollTradesPayload.ID, RerollTradesPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(RerollParticlePayload.ID, RerollParticlePayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(RerollTradesPayload.ID, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> handleReroll(player));
		});

		LOGGER.info("Reroll Trades initialized!");
	}

	private void handleReroll(ServerPlayer player) {
		if (!(player.containerMenu instanceof MerchantMenu merchantHandler)) {
			return;
		}

		if (!(((MerchantScreenHandlerAccessor) merchantHandler).getMerchant() instanceof Villager villager)) {
			return;
		}

		RerollConfig config = RerollConfig.get();
		if (config.requireSneaking && !player.isShiftKeyDown()) {
			player.sendSystemMessage(
					Component.translatable("message.reroll-trades.must_sneak").withStyle(ChatFormatting.RED),
					true);
			return;
		}

		MerchantOffers offers = villager.getOffers();
		for (MerchantOffer offer : offers) {
			if (offer.getUses() > 0) {
				player.sendSystemMessage(
						Component.translatable("message.reroll-trades.already_traded").withStyle(ChatFormatting.RED),
						true);
				return;
			}
		}

		offers.clear();
		((VillagerEntityAccessor) villager)
				.invokeUpdateTrades((ServerLevel) villager.level());

		MerchantOffers newOffers = villager.getOffers();
		merchantHandler.setOffers(newOffers);

		int level = villager.getVillagerData().level();
		int experience = villager.getVillagerXp();
		player.connection.send(new ClientboundMerchantOffersPacket(
				merchantHandler.containerId,
				newOffers,
				level,
				experience,
				true,
				false
		));

		if (config.enableParticles) {
			BlockPos villagerPos = villager.blockPosition();
			ServerPlayNetworking.send(player, new RerollParticlePayload(villagerPos));
		}

		LOGGER.debug("Player {} rerolled trades for villager at {}",
				player.getName().getString(), villager.blockPosition());
	}
}