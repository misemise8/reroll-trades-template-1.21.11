package net.misemise.neoforge;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public final class RerollTradesNeoForgeClient {

    static final KeyMapping REROLL_KEY = new KeyMapping(
            "key.reroll-trades.reroll",
            KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.categories.gameplay"
    );

    private RerollTradesNeoForgeClient() {
        throw new IllegalStateException("Utility class");
    }

    static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(REROLL_KEY);
    }

}
