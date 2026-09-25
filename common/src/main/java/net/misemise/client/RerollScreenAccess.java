package net.misemise.client;

import net.misemise.network.RerollStatePayload;

public interface RerollScreenAccess {
    void rerollTrades$applyState(RerollStatePayload state);
    void rerollTrades$setTargetScreenOpen(boolean open);
}
