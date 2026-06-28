package net.misemise.reroll;

public enum RerollAction {
    REQUEST_STATE,
    REROLL,
    UNDO;

    public static RerollAction byId(int id) {
        RerollAction[] values = values();
        return id >= 0 && id < values.length ? values[id] : REQUEST_STATE;
    }
}
