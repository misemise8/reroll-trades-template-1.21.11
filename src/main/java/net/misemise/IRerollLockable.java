package net.misemise;

/**
 * Interface injected into MerchantScreen via MerchantScreenMixin.
 * Must NOT be in net.misemise.mixin - Mixin system owns that package
 * and prohibits direct class loading, causing IllegalClassLoadError.
 */
public interface IRerollLockable {
    void rerollTrades$lock();
    void rerollTrades$unlock();
}
