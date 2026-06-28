package net.misemise;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RerollTrades {

    public static final String MOD_ID = "reroll-trades";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private RerollTrades() {
    }

    public static void init() {
        LOGGER.info("Reroll Trades initialized");
    }
}
