package net.misemise;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.misemise.network.RerollEffectPayload;
import net.misemise.network.RerollStatePayload;
import org.lwjgl.glfw.GLFW;

public final class RerollTradesClient implements ClientModInitializer {

    private static KeyMapping rerollKey;

    @Override
    public void onInitializeClient() {
        rerollKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.reroll-trades.reroll",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                KeyMapping.Category.GAMEPLAY
        ));

        ClientPlayNetworking.registerGlobalReceiver(RerollStatePayload.TYPE, (payload, context) ->
                context.client().execute(() -> net.misemise.client.RerollTradesClient.handleState(payload))
        );
        ClientPlayNetworking.registerGlobalReceiver(RerollEffectPayload.TYPE, (payload, context) ->
                context.client().execute(() -> net.misemise.client.RerollTradesClient.handleEffect(payload))
        );
    }

    public static KeyMapping getRerollKey() {
        return rerollKey;
    }
}
