package net.misemise.fabric;

import net.misemise.platform.ClientPlatformHooks;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
//#if MC >= 12109
import net.minecraft.client.input.KeyEvent;
//#endif
import net.minecraft.network.chat.Component;
import net.misemise.network.RerollActionPayload;
import net.misemise.network.TradeTargetActionPayload;
import net.misemise.reroll.RerollAction;

public final class ClientPlatformHooksImpl implements ClientPlatformHooks {

    @Override public void sendTargetAction(TradeTargetActionPayload payload) { ClientPlayNetworking.send(payload); }

    @Override
//#if MC >= 12109
    public boolean matchesRerollKey(KeyEvent event) {
        return RerollTradesFabricClient.getRerollKey() != null && RerollTradesFabricClient.getRerollKey().matches(event);
//#else
//$$     public boolean matchesRerollKey(int keyCode, int scanCode) {
//$$         return RerollTradesFabricClient.getRerollKey() != null
//$$                 && RerollTradesFabricClient.getRerollKey().matches(keyCode, scanCode);
//#endif
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
