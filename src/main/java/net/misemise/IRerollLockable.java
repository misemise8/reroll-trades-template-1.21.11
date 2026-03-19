package net.misemise;

/**
 * Interface injected into MerchantScreen via MerchantScreenMixin.
 * Must NOT be in the net.misemise.mixin package -- the Mixin system
 * owns that package and prohibits direct class loading from it.
 */
public interface IRerollLockable {
    void rerollTrades$lock();
    void rerollTrades$unlock();
}