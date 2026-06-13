package net.misemise.fabric;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.misemise.client.RerollTradesClient;
import net.misemise.network.RerollLockedPayload;
import net.misemise.network.RerollParticlePayload;
import net.misemise.network.RerollRejectPayload;
import org.lwjgl.glfw.GLFW;

public final class RerollTradesFabricClient implements ClientModInitializer {

    static KeyMapping rerollKey;

    @Override
    public void onInitializeClient() {
        rerollKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.reroll-trades.reroll",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                KeyMapping.Category.GAMEPLAY
        ));

        ClientPlayNetworking.registerGlobalReceiver(RerollParticlePayload.TYPE, (payload, context) ->
                context.client().execute(() -> RerollTradesClient.handleParticle(payload.pos()))
        );
        ClientPlayNetworking.registerGlobalReceiver(RerollLockedPayload.TYPE, (payload, context) ->
                context.client().execute(RerollTradesClient::handleLocked)
        );
        ClientPlayNetworking.registerGlobalReceiver(RerollRejectPayload.TYPE, (payload, context) ->
                context.client().execute(RerollTradesClient::handleRejected)
        );
    }

    static KeyMapping getRerollKey() {
        return rerollKey;
    }
}
