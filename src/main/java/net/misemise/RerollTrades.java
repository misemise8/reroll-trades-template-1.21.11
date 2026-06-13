package net.misemise;

import com.mojang.serialization.Codec;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
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
import net.minecraft.world.item.trading.MerchantOffers;
import net.misemise.config.RerollConfig;
import net.misemise.mixin.MerchantScreenHandlerAccessor;
import net.misemise.mixin.VillagerEntityAccessor;
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
			net.minecraft.resources.Identifier.fromNamespaceAndPath(MOD_ID, "locked_players"),
			builder -> builder
					.persistent(Codec.list(Codec.STRING.xmap(UUID::fromString, UUID::toString))
							.xmap(HashSet::new, ArrayList::new))
					.initializer(HashSet::new));

	private static final Set<UUID> IN_PROGRESS = Collections.synchronizedSet(new HashSet<>());

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.serverboundPlay().register(RerollTradesPayload.ID, RerollTradesPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(RerollParticlePayload.ID, RerollParticlePayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(RerollLockedPayload.ID, RerollLockedPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(RerollRejectPayload.ID, RerollRejectPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(RerollTradesPayload.ID, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> handleReroll(player));
		});

		LOGGER.info("Reroll Trades initialized!");
	}

	private void handleReroll(ServerPlayer player) {
		if (!IN_PROGRESS.add(player.getUUID())) {
			return;
		}

		try {
			handleRerollInternal(player);
		} finally {
			IN_PROGRESS.remove(player.getUUID());
		}
	}

	private void handleRerollInternal(ServerPlayer player) {
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
			ServerPlayNetworking.send(player, new RerollRejectPayload());
			return;
		}

		HashSet<UUID> locked = villager.getAttached(REROLL_LOCKED);
		if (locked != null && locked.contains(player.getUUID())) {
			player.sendSystemMessage(
					Component.translatable("message.reroll-trades.already_traded").withStyle(ChatFormatting.RED),
					true);
			return;
		}

		MerchantOffers offers = villager.getOffers();
		offers.clear();
		((VillagerEntityAccessor) villager)
				.invokeUpdateTrades((ServerLevel) villager.level());

		MerchantOffers newOffers = villager.getOffers();
		if (newOffers.isEmpty()) {
			player.sendSystemMessage(
					Component.translatable("message.reroll-trades.no_profession").withStyle(ChatFormatting.RED),
					true);
			ServerPlayNetworking.send(player, new RerollRejectPayload());
			return;
		}

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
