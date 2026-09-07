package net.misemise.neoforge;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
//#if MC >= 12108
import net.misemise.client.RerollTradesClient;
import net.misemise.network.RerollEffectPayload;
import net.misemise.network.RerollStatePayload;
//#endif
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
//#if MC >= 12108
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
//#endif
import org.lwjgl.glfw.GLFW;

public final class RerollTradesNeoForgeClient {

    static final KeyMapping REROLL_KEY = new KeyMapping(
            "key.reroll-trades.reroll",
            KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
//#if MC >= 12110
            KeyMapping.Category.GAMEPLAY
//#else
//$$             "key.categories.gameplay"
//#endif
    );

    private RerollTradesNeoForgeClient() {
        throw new IllegalStateException("Utility class");
    }

    static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(REROLL_KEY);
    }

//#if MC >= 12108
    static void registerClientPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(RerollStatePayload.TYPE, (payload, context) -> RerollTradesClient.handleState(payload));
        event.register(RerollEffectPayload.TYPE, (payload, context) -> RerollTradesClient.handleEffect(payload));
    }
//#endif
}
