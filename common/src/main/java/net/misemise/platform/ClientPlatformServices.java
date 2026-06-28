package net.misemise.platform;

import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.misemise.reroll.RerollAction;

import java.util.ServiceLoader;

public final class ClientPlatformServices {

    private static final ClientPlatformHooks PLATFORM = ServiceLoader.load(ClientPlatformHooks.class)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No client platform hooks implementation found"));

    private ClientPlatformServices() {
    }

    public static boolean matchesRerollKey(KeyEvent event) {
        return PLATFORM.matchesRerollKey(event);
    }

    public static Component rerollKeyName() {
        return PLATFORM.rerollKeyName();
    }

    public static void sendAction(RerollAction action, int containerId) {
        PLATFORM.sendAction(action, containerId);
    }
}
