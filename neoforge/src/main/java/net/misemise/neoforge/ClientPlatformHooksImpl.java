package net.misemise.neoforge;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.misemise.network.RerollActionPayload;
import net.misemise.platform.ClientPlatformHooks;
import net.misemise.reroll.RerollAction;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class ClientPlatformHooksImpl implements ClientPlatformHooks {

    @Override
    public boolean matchesRerollKey(KeyEvent event) {
        return RerollTradesNeoForgeClient.REROLL_KEY.isActiveAndMatches(InputConstants.getKey(event));
    }

    @Override
    public Component rerollKeyName() {
        return RerollTradesNeoForgeClient.REROLL_KEY.getTranslatedKeyMessage();
    }

    @Override
    public void sendAction(RerollAction action, int containerId) {
        ClientPacketDistributor.sendToServer(new RerollActionPayload(action, containerId));
    }
}
