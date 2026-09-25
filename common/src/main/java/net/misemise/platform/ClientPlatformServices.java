package net.misemise.platform;

//#if MC >= 12109
import net.minecraft.client.input.KeyEvent;
//#endif
import net.minecraft.network.chat.Component;
import net.misemise.reroll.RerollAction;
import net.misemise.network.TradeTargetActionPayload;

import java.util.ServiceLoader;

public final class ClientPlatformServices {

    private static final ClientPlatformHooks PLATFORM = ServiceLoader.load(ClientPlatformHooks.class)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No client platform hooks implementation found"));

    private ClientPlatformServices() {
    }

//#if MC >= 12109
    public static boolean matchesRerollKey(KeyEvent event) {
        return PLATFORM.matchesRerollKey(event);
//#else
//$$     public static boolean matchesRerollKey(int keyCode, int scanCode) {
//$$         return PLATFORM.matchesRerollKey(keyCode, scanCode);
//#endif
    }

    public static Component rerollKeyName() {
        return PLATFORM.rerollKeyName();
    }

    public static void sendAction(RerollAction action, int containerId) {
        PLATFORM.sendAction(action, containerId);
    }

    public static void sendTargetAction(TradeTargetActionPayload payload) { PLATFORM.sendTargetAction(payload); }
}
