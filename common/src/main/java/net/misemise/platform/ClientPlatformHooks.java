package net.misemise.platform;

import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.misemise.reroll.RerollAction;

public interface ClientPlatformHooks {

    boolean matchesRerollKey(KeyEvent event);

    Component rerollKeyName();

    void sendAction(RerollAction action, int containerId);
}
