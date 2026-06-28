package net.misemise.platform;

import net.minecraft.network.chat.Component;
import net.misemise.reroll.RerollAction;

public interface ClientPlatformHooks {

    boolean matchesRerollKey(int keyCode, int scanCode);

    Component rerollKeyName();

    void sendAction(RerollAction action, int containerId);
}
