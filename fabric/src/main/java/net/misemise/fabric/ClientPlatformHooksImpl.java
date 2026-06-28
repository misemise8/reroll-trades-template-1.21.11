package net.misemise.fabric;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.misemise.network.RerollActionPayload;
import net.misemise.platform.ClientPlatformHooks;
import net.misemise.reroll.RerollAction;

public final class ClientPlatformHooksImpl implements ClientPlatformHooks {

    @Override
    public boolean matchesRerollKey(KeyEvent event) {
        return RerollTradesFabricClient.getRerollKey() != null
                && RerollTradesFabricClient.getRerollKey().matches(event);
    }

    @Override
    public Component rerollKeyName() {
        return RerollTradesFabricClient.getRerollKey() == null
                ? Component.literal("R")
                : RerollTradesFabricClient.getRerollKey().getTranslatedKeyMessage();
    }

    @Override
    public void sendAction(RerollAction action, int containerId) {
        ClientPlayNetworking.send(new RerollActionPayload(action, containerId));
    }
}
