package net.misemise.neoforge;

import com.mojang.serialization.Codec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

final class RerollTradesNeoForgeAttachments {

    private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);
    private static final Codec<Map<UUID, Integer>> REROLL_COUNTS_CODEC =
            Codec.unboundedMap(UUID_CODEC, Codec.INT);
    private static final Codec<HashSet<UUID>> LEGACY_LOCKED_PLAYERS_CODEC = Codec
            .list(UUID_CODEC)
            .xmap(HashSet::new, ArrayList::new);

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, RerollTradesNeoForge.NEOFORGE_MOD_ID);

    private static final DeferredHolder<AttachmentType<?>, AttachmentType<Map<UUID, Integer>>> REROLL_COUNTS =
            ATTACHMENT_TYPES.register("reroll_counts",
                    () -> AttachmentType.builder((Supplier<Map<UUID, Integer>>) HashMap::new)
//#if MC >= 12108
                            .serialize(REROLL_COUNTS_CODEC.fieldOf("reroll_counts"))
//#else
//$$                             .serialize(REROLL_COUNTS_CODEC)
//#endif
                            .build());

    private static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> TRADED =
            ATTACHMENT_TYPES.register("traded",
                    () -> AttachmentType.builder(() -> false)
//#if MC >= 12108
                            .serialize(Codec.BOOL.fieldOf("traded"))
//#else
//$$                             .serialize(Codec.BOOL)
//#endif
                            .build());

    private static final DeferredHolder<AttachmentType<?>, AttachmentType<HashSet<UUID>>> LEGACY_LOCKED_PLAYERS =
            ATTACHMENT_TYPES.register("locked_players",
                    () -> AttachmentType.builder((Supplier<HashSet<UUID>>) HashSet::new)
//#if MC >= 12108
                            .serialize(LEGACY_LOCKED_PLAYERS_CODEC.fieldOf("locked_players"))
//#else
//$$                             .serialize(LEGACY_LOCKED_PLAYERS_CODEC)
//#endif
                            .build());

    private RerollTradesNeoForgeAttachments() {
    }

    static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }

    static AttachmentType<Map<UUID, Integer>> rerollCounts() {
        return REROLL_COUNTS.get();
    }

    static AttachmentType<Boolean> traded() {
        return TRADED.get();
    }

    static AttachmentType<HashSet<UUID>> legacyLockedPlayers() {
        return LEGACY_LOCKED_PLAYERS.get();
    }
}
