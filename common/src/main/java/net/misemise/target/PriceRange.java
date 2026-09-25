package net.misemise.target;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Bounds of the generated base price, before demand and player discounts. */
public record PriceRange(int min, int max) {
    public static final PriceRange UNKNOWN = new PriceRange(-1, -1);

    public boolean known() { return min >= 0 && max >= min; }

    public static PriceRange fixed(int value) { return new PriceRange(value, value); }

    public PriceRange plus(PriceRange other) {
        if (!known() || !other.known()) return UNKNOWN;
        long low = (long) min + other.min, high = (long) max + other.max;
        return low < 0 || high > Integer.MAX_VALUE ? UNKNOWN : new PriceRange((int) low, (int) high);
    }

    public PriceRange clamp(int cap) {
        return known() ? new PriceRange(Math.max(1, Math.min(cap, min)), Math.max(1, Math.min(cap, max))) : UNKNOWN;
    }

    public PriceRange union(PriceRange other) {
        return known() && other.known() ? new PriceRange(Math.min(min, other.min), Math.max(max, other.max)) : UNKNOWN;
    }

    public static PriceRange enchantedBook(int level, boolean doubled) {
        if (level < 1 || level > 255) return UNKNOWN;
        int factor = doubled ? 2 : 1;
        return new PriceRange((2 + 3 * level) * factor, (6 + 13 * level) * factor).clamp(64);
    }

    public static PriceRange numberProvider(JsonElement value) {
        if (value == null) return fixed(1);
        try {
            if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
                return fixed((int) Math.floor(value.getAsDouble()));
            }
            if (!value.isJsonObject()) return UNKNOWN;
            JsonObject object = value.getAsJsonObject();
            String type = object.has("type") ? object.get("type").getAsString().replace("minecraft:", "") : "";
            return switch (type) {
                case "constant" -> object.has("value") ? numberProvider(object.get("value")) : UNKNOWN;
                case "uniform" -> {
                    if (!object.has("min") || !object.has("max")) yield UNKNOWN;
                    PriceRange low = numberProvider(object.get("min")), high = numberProvider(object.get("max"));
                    yield low.known() && high.known() && low.min <= high.max ? new PriceRange(low.min, high.max) : UNKNOWN;
                }
                case "binomial" -> {
                    if (!object.has("n") || !object.has("p")) yield UNKNOWN;
                    PriceRange n = numberProvider(object.get("n"));
                    // Fractional probability needs its original value, not its floored integer bounds.
                    if (n.known() && object.get("p").isJsonPrimitive()) {
                        double probability = object.get("p").getAsDouble();
                        yield probability <= 0 ? fixed(0) : probability >= 1 ? n : new PriceRange(0, n.max);
                    }
                    yield UNKNOWN;
                }
                case "sum" -> {
                    PriceRange sum = fixed(0);
                    if (!object.has("summands")) yield UNKNOWN;
                    for (JsonElement term : object.getAsJsonArray("summands")) sum = sum.plus(numberProvider(term));
                    yield sum;
                }
                default -> UNKNOWN;
            };
        } catch (RuntimeException ignored) {
            return UNKNOWN;
        }
    }
}
