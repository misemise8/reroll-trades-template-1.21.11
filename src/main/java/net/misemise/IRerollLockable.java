package net.misemise;

public interface IRerollLockable {
    void rerollTrades$lock();

    void rerollTrades$unlock();
}
