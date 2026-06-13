package net.misemise;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.particle.ParticleTypes;
import net.misemise.config.RerollConfig;
import net.misemise.network.RerollLockedPayload;
import net.misemise.network.RerollParticlePayload;
import net.misemise.network.RerollRejectPayload;
import org.lwjgl.glfw.GLFW;

public class RerollTradesClient implements ClientModInitializer {

    public static KeyBinding rerollKey;

    @Override
    public void onInitializeClient() {
        rerollKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.reroll-trades.reroll",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "key.categories.reroll-trades.general"));

        ClientPlayNetworking.registerGlobalReceiver(RerollLockedPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                MinecraftClient client = context.client();
                if (client.currentScreen instanceof IRerollLockable lockable) {
                    lockable.rerollTrades$lock();
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(RerollRejectPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                MinecraftClient client = context.client();
                if (client.currentScreen instanceof IRerollLockable lockable) {
                    lockable.rerollTrades$unlock();
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(RerollParticlePayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                RerollConfig config = RerollConfig.get();
                if (!config.enableParticles) {
                    return;
                }

                MinecraftClient client = context.client();
                if (client.world == null) {
                    return;
                }

                double cx = payload.pos().getX() + 0.5;
                double cy = payload.pos().getY() + 1.0;
                double cz = payload.pos().getZ() + 0.5;

                for (int i = 0; i < 10; i++) {
                    double dx = (Math.random() - 0.5) * 0.8;
                    double dy = Math.random() * 0.5;
                    double dz = (Math.random() - 0.5) * 0.8;
                    client.world.addImportantParticleClient(ParticleTypes.HAPPY_VILLAGER,
                            cx + dx, cy + dy, cz + dz,
                            dx * 0.1, dy * 0.1, dz * 0.1);
                }
            });
        });
    }
}
