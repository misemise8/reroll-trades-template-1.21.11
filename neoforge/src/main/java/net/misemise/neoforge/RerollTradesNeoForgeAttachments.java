package net.misemise.neoforge;

import com.mojang.serialization.Codec;
import java.util.function.Supplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;

final class RerollTradesNeoForgeAttachments {

    private static final Codec<HashSet<UUID>> LOCKED_PLAYERS_CODEC = Codec
            .list(Codec.STRING.xmap(UUID::fromString, UUID::toString))
            .xmap(HashSet::new, ArrayList::new);

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, RerollTradesNeoForge.NEOFORGE_MOD_ID);

    private static final DeferredHolder<AttachmentType<?>, AttachmentType<HashSet<UUID>>> LOCKED_PLAYERS =
            ATTACHMENT_TYPES.register("locked_players",
                    () -> AttachmentType.builder((Supplier<HashSet<UUID>>) HashSet::new)
                            .serialize(LOCKED_PLAYERS_CODEC.fieldOf("locked_players"))
                            .build());

    private RerollTradesNeoForgeAttachments() {
    }

    static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }

    static AttachmentType<HashSet<UUID>> lockedPlayers() {
        return LOCKED_PLAYERS.get();
    }
}
