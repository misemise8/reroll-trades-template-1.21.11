package net.misemise.neoforge;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.misemise.client.RerollTradesClient;
import net.misemise.network.RerollParticlePayload;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import org.lwjgl.glfw.GLFW;

public final class RerollTradesNeoForgeClient {

    static final KeyMapping REROLL_KEY = new KeyMapping(
            "key.reroll-trades.reroll",
            KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            KeyMapping.Category.GAMEPLAY
    );

    private RerollTradesNeoForgeClient() {
        throw new IllegalStateException("Utility class");
    }

    static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(REROLL_KEY);
    }

    static void registerClientPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(RerollParticlePayload.TYPE, (payload, context) -> RerollTradesClient.handleParticle(payload.pos()));
    }
}
