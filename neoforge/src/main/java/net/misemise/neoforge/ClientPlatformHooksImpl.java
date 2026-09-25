package net.misemise.neoforge;

import com.mojang.blaze3d.platform.InputConstants;
//#if MC >= 12110
import net.minecraft.client.input.KeyEvent;
//#endif
import net.minecraft.network.chat.Component;
import net.misemise.network.RerollActionPayload;
import net.misemise.network.TradeTargetActionPayload;
import net.misemise.platform.ClientPlatformHooks;
import net.misemise.reroll.RerollAction;
//#if MC >= 12108
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
//#else
//$$ import net.neoforged.neoforge.network.PacketDistributor;
//#endif

public final class ClientPlatformHooksImpl implements ClientPlatformHooks {

    @Override public void sendTargetAction(TradeTargetActionPayload payload) {
//#if MC >= 12108
        ClientPacketDistributor.sendToServer(payload);
//#else
//$$         PacketDistributor.sendToServer(payload);
//#endif
    }

    @Override
//#if MC >= 12110
    public boolean matchesRerollKey(KeyEvent event) {
        return RerollTradesNeoForgeClient.REROLL_KEY.isActiveAndMatches(InputConstants.getKey(event));
//#else
//$$     public boolean matchesRerollKey(int keyCode, int scanCode) {
//$$         return RerollTradesNeoForgeClient.REROLL_KEY.isActiveAndMatches(InputConstants.getKey(keyCode, scanCode));
//#endif
    }

    @Override
    public Component rerollKeyName() {
        return RerollTradesNeoForgeClient.REROLL_KEY.getTranslatedKeyMessage();
    }

    @Override
    public void sendAction(RerollAction action, int containerId) {
//#if MC >= 12108
        ClientPacketDistributor.sendToServer(new RerollActionPayload(action, containerId));
//#else
//$$         PacketDistributor.sendToServer(new RerollActionPayload(action, containerId));
//#endif
    }
}
