package net.misemise.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.misemise.IRerollLockable;
import net.misemise.config.RerollConfig;

public final class RerollTradesClient {

    private RerollTradesClient() {
    }

    public static void handleLocked() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui.screen() instanceof IRerollLockable lockable) {
            lockable.rerollTrades$lock();
        }
    }

    public static void handleRejected() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui.screen() instanceof IRerollLockable lockable) {
            lockable.rerollTrades$unlock();
        }
    }

    public static void handleParticle(BlockPos pos) {
        if (!RerollConfig.get().enableParticles) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

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
