package net.misemise.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.misemise.config.RerollClientConfig;
import net.misemise.network.RerollEffectPayload;
import net.misemise.network.RerollStatePayload;

public final class RerollTradesClient {

    private RerollTradesClient() {
    }

    public static void handleState(RerollStatePayload state) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof RerollScreenAccess screen) {
            screen.rerollTrades$applyState(state);
        }
    }

    public static void handleEffect(RerollEffectPayload effect) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        RerollClientConfig config = RerollClientConfig.get();
        if (config.enableParticles) {
            spawnParticles(minecraft, effect.pos());
        }
        if (config.enableSounds) {
            BlockPos pos = effect.pos();
            minecraft.level.playLocalSound(
                    pos.getX() + 0.5D,
                    pos.getY() + 1.0D,
                    pos.getZ() + 0.5D,
                    SoundEvents.VILLAGER_YES,
                    SoundSource.NEUTRAL,
                    0.8F,
                    effect.undo() ? 0.8F : 1.1F,
                    false
            );
        }
    }

    private static void spawnParticles(Minecraft minecraft, BlockPos pos) {
        RandomSource random = RandomSource.create();
        double centerX = pos.getX() + 0.5D;
        double centerY = pos.getY() + 1.0D;
        double centerZ = pos.getZ() + 0.5D;

        for (int i = 0; i < 10; i++) {
            double dx = (random.nextDouble() - 0.5D) * 0.8D;
            double dy = random.nextDouble() * 0.5D;
            double dz = (random.nextDouble() - 0.5D) * 0.8D;
            minecraft.level.addParticle(
                    ParticleTypes.HAPPY_VILLAGER,
                    centerX + dx,
                    centerY + dy,
                    centerZ + dz,
                    dx * 0.1D,
                    dy * 0.1D,
                    dz * 0.1D
            );
        }
    }
}
