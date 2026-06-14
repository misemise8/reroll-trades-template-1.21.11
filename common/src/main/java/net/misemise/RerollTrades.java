package net.misemise;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffers;
import net.misemise.config.RerollConfig;
import net.misemise.mixin.MerchantScreenHandlerAccessor;
import net.misemise.mixin.VillagerEntityAccessor;
import net.misemise.platform.PlatformServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class RerollTrades {

    public static final String MOD_ID = "reroll-trades";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final Set<UUID> IN_PROGRESS = Collections.synchronizedSet(new HashSet<>());

    private RerollTrades() {
    }

    public static void init() {
        LOGGER.info("Reroll Trades initialized");
    }

    public static void handleReroll(ServerPlayer player) {
        if (!IN_PROGRESS.add(player.getUUID())) {
            return;
        }

        try {
            handleRerollInternal(player);
        } finally {
            IN_PROGRESS.remove(player.getUUID());
        }
    }

    private static void handleRerollInternal(ServerPlayer player) {
        if (!(player.containerMenu instanceof MerchantMenu merchantMenu)) {
            return;
        }

        if (!(((MerchantScreenHandlerAccessor) merchantMenu).rerollTrades$getMerchant() instanceof Villager villager)) {
            return;
        }

        RerollConfig config = RerollConfig.get();
        if (config.requireSneaking && !player.isShiftKeyDown()) {
            player.sendSystemMessage(
                    Component.translatable("message.reroll-trades.must_sneak").withStyle(ChatFormatting.RED),
                    true
            );
            PlatformServices.sendReject(player);
            return;
        }

        if (PlatformServices.isRerollLocked(villager, player)) {
            player.sendSystemMessage(
                    Component.translatable("message.reroll-trades.already_traded").withStyle(ChatFormatting.RED),
                    true
            );
            PlatformServices.sendLocked(player);
            return;
        }

        MerchantOffers offers = villager.getOffers();
        offers.clear();
        ((VillagerEntityAccessor) villager).rerollTrades$updateTrades((ServerLevel) villager.level());

        MerchantOffers newOffers = villager.getOffers();
        if (newOffers.isEmpty()) {
            player.sendSystemMessage(
                    Component.translatable("message.reroll-trades.no_profession").withStyle(ChatFormatting.RED),
                    true
            );
            PlatformServices.sendReject(player);
            return;
        }

        merchantMenu.setOffers(newOffers);

        player.connection.send(new ClientboundMerchantOffersPacket(
                merchantMenu.containerId,
                newOffers,
                villager.getVillagerData().level(),
                villager.getVillagerXp(),
                true,
                false
        ));

        if (config.enableParticles) {
            BlockPos pos = villager.blockPosition();
            PlatformServices.sendParticle(player, pos);
        }

        LOGGER.debug("Player {} rerolled trades for villager at {}", player.getName().getString(), villager.blockPosition());
    }
}
