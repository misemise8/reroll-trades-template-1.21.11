package net.misemise;

/**
 * Interface injected into MerchantScreen via MerchantScreenMixin.
 * Allows RerollTradesClient to call lock()/unlock() without using instanceof
 * against the mixin class directly (which is always false at runtime).
 *
 * Usage: if (screen instanceof IRerollLockable l) l.rerollTrades$lock();
 *
 * NOTE: Must NOT be in the net.misemise.mixin package — the Mixin system
 * owns that package and prohibits direct (non-mixin) class loading from it,
 * causing IllegalClassLoadError / BootstrapMethodError at runtime.
 */
public interface IRerollLockable {
    /** Called when the server confirms the player has traded (permanent lock). */
    void rerollTrades$lock();

    /**
     * Called when the server rejects a reroll request (e.g. must sneak, no
     * profession).
     * Reverts the optimistic lock applied when the reroll request was sent.
     */
    void rerollTrades$unlock();
}
