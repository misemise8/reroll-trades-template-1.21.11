package net.misemise.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.misemise.config.RerollServerConfig;
import net.misemise.reroll.RerollController;

public final class RerollCommands {

    private RerollCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rerolltrades")
//#if MC >= 12106
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
//#else
//$$                 .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
//#endif
                .then(Commands.literal("reload").executes(context -> {
                    RerollServerConfig.reload();
                    RerollController.refreshOpenScreens(context.getSource().getServer());
                    context.getSource().sendSuccess(
                            () -> Component.translatable("command.reroll-trades.reload.success"),
                            true
                    );
                    return 1;
                }))
                .then(Commands.literal("status").executes(context -> {
                    RerollServerConfig config = RerollServerConfig.get();
                    String configStatus = "sneak=" + config.requireSneaking
                            + ", cooldown=" + config.cooldownTicks
                            + ", max=" + (config.maxRerollsPerPlayerPerVillager == 0
                            ? "unlimited" : config.maxRerollsPerPlayerPerVillager)
                            + ", undo=" + config.enableUndo
                            + ", undoTimeout=" + config.undoTimeoutSeconds + "s";
                    context.getSource().sendSuccess(
                            () -> Component.translatable("command.reroll-trades.status.config", configStatus),
                            false
                    );

                    ServerPlayer player = context.getSource().getPlayer();
                    if (player != null) {
                        String screenStatus = RerollController.status(player);
                        context.getSource().sendSuccess(
                                () -> Component.translatable("command.reroll-trades.status.screen", screenStatus),
                                false
                        );
                    }
                    return 1;
                }))
        );
    }
}
