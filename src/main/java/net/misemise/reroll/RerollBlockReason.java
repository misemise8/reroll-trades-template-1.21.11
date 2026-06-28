package net.misemise.reroll;

public enum RerollBlockReason {
    NONE,
    UNSUPPORTED_MERCHANT,
    MUST_SNEAK,
    ALREADY_TRADED,
    NO_PROFESSION,
    COOLDOWN,
    LIMIT_REACHED,
    UNDO_UNAVAILABLE;

    public static RerollBlockReason byId(int id) {
        RerollBlockReason[] values = values();
        return id >= 0 && id < values.length ? values[id] : UNSUPPORTED_MERCHANT;
    }
}
