package net.misemise.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.misemise.RerollTrades;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Lightweight JSON config — no external dependencies required.
 * Stored at config/reroll-trades.json
 */
public class RerollConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("reroll-trades.json");

    // ④ volatile + synchronized to prevent race conditions on server threads
    private static volatile RerollConfig INSTANCE;

    // ----- Config fields -----
    public boolean requireSneaking = false;
    public boolean enableParticles = true;

    // ----- Access -----
    public static RerollConfig get() {
        if (INSTANCE == null) {
            synchronized (RerollConfig.class) {
                if (INSTANCE == null) {
                    INSTANCE = load();
                }
            }
        }
        return INSTANCE;
    }

    // ----- I/O -----
    private static RerollConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                RerollConfig config = GSON.fromJson(json, RerollConfig.class);
                if (config != null) {
                    return config;
                }
            } catch (IOException e) {
                RerollTrades.LOGGER.error("Failed to load config, using defaults", e);
            }
        }
        // Create default config file
        RerollConfig config = new RerollConfig();
        config.save();
        return config;
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(this));
        } catch (IOException e) {
            RerollTrades.LOGGER.error("Failed to save config", e);
        }
    }
}
