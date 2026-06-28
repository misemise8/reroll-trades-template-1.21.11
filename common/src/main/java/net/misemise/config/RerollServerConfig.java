package net.misemise.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.misemise.RerollTrades;
import net.misemise.platform.PlatformServices;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class RerollServerConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static volatile RerollServerConfig instance;

    public boolean requireSneaking = false;
    public int cooldownTicks = 5;
    public int maxRerollsPerPlayerPerVillager = 0;
    public boolean enableUndo = true;
    public int undoTimeoutSeconds = 30;

    private RerollServerConfig() {
    }

    public static RerollServerConfig get() {
        RerollServerConfig current = instance;
        if (current == null) {
            synchronized (RerollServerConfig.class) {
                current = instance;
                if (current == null) {
                    current = load();
                    instance = current;
                }
            }
        }
        return current;
    }

    public static void reload() {
        instance = load();
    }

    private static RerollServerConfig load() {
        Path path = configPath();
        if (Files.exists(path)) {
            try {
                RerollServerConfig config = GSON.fromJson(Files.readString(path), RerollServerConfig.class);
                if (config != null) {
                    config.validate();
                    return config;
                }
                throw new JsonParseException("Config root is null");
            } catch (IOException | JsonParseException exception) {
                RerollTrades.LOGGER.error("Failed to load {}, using defaults without overwriting the file", path, exception);
                return defaults();
            }
        }

        RerollServerConfig config = migrateLegacy();
        config.validate();
        config.save();
        return config;
    }

    private static RerollServerConfig migrateLegacy() {
        RerollServerConfig config = defaults();
        Path legacyPath = PlatformServices.getConfigDir().resolve("reroll-trades.json");
        if (!Files.exists(legacyPath)) {
            return config;
        }

        try {
            JsonObject legacy = JsonParser.parseString(Files.readString(legacyPath)).getAsJsonObject();
            if (legacy.has("requireSneaking")) {
                config.requireSneaking = legacy.get("requireSneaking").getAsBoolean();
            }
            RerollTrades.LOGGER.info("Imported server settings from legacy config {}", legacyPath);
        } catch (IOException | JsonParseException | IllegalStateException exception) {
            RerollTrades.LOGGER.warn("Could not import legacy config {}, using defaults", legacyPath, exception);
        }
        return config;
    }

    private static RerollServerConfig defaults() {
        return new RerollServerConfig();
    }

    private void validate() {
        cooldownTicks = Math.max(0, cooldownTicks);
        maxRerollsPerPlayerPerVillager = Math.max(0, maxRerollsPerPlayerPerVillager);
        undoTimeoutSeconds = Math.max(1, undoTimeoutSeconds);
    }

    private void save() {
        Path path = configPath();
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(this));
        } catch (IOException exception) {
            RerollTrades.LOGGER.error("Failed to save {}", path, exception);
        }
    }

    private static Path configPath() {
        return PlatformServices.getConfigDir().resolve("reroll-trades-server.json");
    }
}
