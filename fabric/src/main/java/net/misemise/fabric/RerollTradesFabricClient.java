package net.misemise.fabric;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
//#if MC >= 260100
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
//#else
//$$ import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
//#endif
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.misemise.network.RerollEffectPayload;
import net.misemise.network.RerollStatePayload;
import org.lwjgl.glfw.GLFW;

public final class RerollTradesFabricClient implements ClientModInitializer {

    private static KeyMapping rerollKey;

    @Override
    public void onInitializeClient() {
//#if MC >= 260100
        rerollKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
//#else
//$$         rerollKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
//#endif
                "key.reroll-trades.reroll",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
//#if MC >= 12109
                KeyMapping.Category.GAMEPLAY
//#else
//$$                 KeyMapping.CATEGORY_GAMEPLAY
//#endif
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
