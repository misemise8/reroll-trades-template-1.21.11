package net.misemise;

/**
 * Interface injected into MerchantScreen via MerchantScreenMixin.
 * Allows RerollTradesClient to call lock()/unlock() without using instanceof
 * against the mixin class directly.
 *
 * Must not live in net.misemise.mixin, because the Mixin system owns that
 * package and prohibits direct non-mixin class loading from it.
 */
public interface IRerollLockable {
    void rerollTrades$lock();

    void rerollTrades$unlock();
}
