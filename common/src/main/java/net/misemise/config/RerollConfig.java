package net.misemise.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.misemise.RerollTrades;
import net.misemise.platform.PlatformServices;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class RerollConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = PlatformServices.getConfigDir().resolve("reroll-trades.json");

    private static RerollConfig instance;

    public boolean requireSneaking = false;
    public boolean enableParticles = true;

    public static RerollConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static RerollConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                RerollConfig config = GSON.fromJson(json, RerollConfig.class);
                if (config != null) {
                    return config;
                }
            } catch (IOException exception) {
                RerollTrades.LOGGER.error("Failed to load config, using defaults", exception);
            }
        }

        RerollConfig config = new RerollConfig();
        config.save();
        return config;
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(this));
        } catch (IOException exception) {
            RerollTrades.LOGGER.error("Failed to save config", exception);
        }
    }
}
