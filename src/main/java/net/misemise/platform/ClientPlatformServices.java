package net.misemise.platform;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.misemise.RerollTradesClient;
import net.misemise.network.RerollActionPayload;
import net.misemise.reroll.RerollAction;

public final class ClientPlatformServices {

    private ClientPlatformServices() {
    }

    public static boolean matchesRerollKey(KeyEvent event) {
        return RerollTradesClient.getRerollKey() != null && RerollTradesClient.getRerollKey().matches(event);
    }

    public static Component rerollKeyName() {
        return RerollTradesClient.getRerollKey() == null
                ? Component.literal("R")
                : RerollTradesClient.getRerollKey().getTranslatedKeyMessage();
    }

    public static void sendAction(RerollAction action, int containerId) {
        ClientPlayNetworking.send(new RerollActionPayload(action, containerId));
    }
}
