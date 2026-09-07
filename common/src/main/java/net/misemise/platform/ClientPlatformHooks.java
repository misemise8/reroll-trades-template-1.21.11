package net.misemise.platform;

//#if MC >= 12109
import net.minecraft.client.input.KeyEvent;
//#endif
import net.minecraft.network.chat.Component;
import net.misemise.reroll.RerollAction;

public interface ClientPlatformHooks {

//#if MC >= 12109
    boolean matchesRerollKey(KeyEvent event);
//#else
//$$     boolean matchesRerollKey(int keyCode, int scanCode);
//#endif

    Component rerollKeyName();

    void sendAction(RerollAction action, int containerId);
}
