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

public final class RerollClientConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static volatile RerollClientConfig instance;

    public boolean showRerollButton = true;
    public boolean showUndoButton = true;
    public boolean showKeyHint = true;
    public boolean enableParticles = true;
    public boolean enableSounds = true;

    private RerollClientConfig() {
    }

    public static RerollClientConfig get() {
        RerollClientConfig current = instance;
        if (current == null) {
            synchronized (RerollClientConfig.class) {
                current = instance;
                if (current == null) {
                    current = load();
                    instance = current;
                }
            }
        }
        return current;
    }

    public static void saveCurrent() {
        get().save();
    }

    private static RerollClientConfig load() {
        Path path = configPath();
        if (Files.exists(path)) {
            try {
                RerollClientConfig config = GSON.fromJson(Files.readString(path), RerollClientConfig.class);
                if (config != null) {
                    return config;
                }
                throw new JsonParseException("Config root is null");
            } catch (IOException | JsonParseException exception) {
                RerollTrades.LOGGER.error("Failed to load {}, using defaults without overwriting the file", path, exception);
                return defaults();
            }
        }

        RerollClientConfig config = migrateLegacy();
        config.save();
        return config;
    }

    private static RerollClientConfig migrateLegacy() {
        RerollClientConfig config = defaults();
        Path legacyPath = PlatformServices.getConfigDir().resolve("reroll-trades.json");
        if (!Files.exists(legacyPath)) {
            return config;
        }

        try {
            JsonObject legacy = JsonParser.parseString(Files.readString(legacyPath)).getAsJsonObject();
            if (legacy.has("enableParticles")) {
                config.enableParticles = legacy.get("enableParticles").getAsBoolean();
            }
            RerollTrades.LOGGER.info("Imported client settings from legacy config {}", legacyPath);
        } catch (IOException | JsonParseException | IllegalStateException exception) {
            RerollTrades.LOGGER.warn("Could not import legacy config {}, using defaults", legacyPath, exception);
        }
        return config;
    }

    private static RerollClientConfig defaults() {
        return new RerollClientConfig();
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
        return PlatformServices.getConfigDir().resolve("reroll-trades-client.json");
    }
}
