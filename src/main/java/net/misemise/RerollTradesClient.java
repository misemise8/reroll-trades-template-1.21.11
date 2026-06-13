package net.misemise;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.misemise.config.RerollConfig;
import net.misemise.network.RerollLockedPayload;
import net.misemise.network.RerollParticlePayload;
import net.misemise.network.RerollRejectPayload;
import org.lwjgl.glfw.GLFW;

public class RerollTradesClient implements ClientModInitializer {

    public static KeyMapping rerollKey;

    @Override
    public void onInitializeClient() {
        rerollKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.reroll-trades.reroll",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                KeyMapping.Category.GAMEPLAY
        ));

        ClientPlayNetworking.registerGlobalReceiver(RerollLockedPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                Minecraft client = context.client();
                if (client.screen instanceof IRerollLockable lockable) {
                    lockable.rerollTrades$lock();
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(RerollRejectPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                Minecraft client = context.client();
                if (client.screen instanceof IRerollLockable lockable) {
                    lockable.rerollTrades$unlock();
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(RerollParticlePayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                RerollConfig config = RerollConfig.get();
                if (!config.enableParticles) return;

                Minecraft client = context.client();
                if (client.level == null) return;

                double cx = payload.pos().getX() + 0.5;
                double cy = payload.pos().getY() + 1.0;
                double cz = payload.pos().getZ() + 0.5;

                for (int i = 0; i < 10; i++) {
                    double dx = (Math.random() - 0.5) * 0.8;
                    double dy = Math.random() * 0.5;
                    double dz = (Math.random() - 0.5) * 0.8;
                    client.level.addParticle(ParticleTypes.HAPPY_VILLAGER,
                            cx + dx, cy + dy, cz + dz,
                            dx * 0.1, dy * 0.1, dz * 0.1);
                }
            });
        });
    }
}
