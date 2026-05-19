package net.misemise.fabric;

import net.minecraft.client.input.KeyEvent;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.misemise.network.RerollParticlePayload;
import net.misemise.network.RerollTradesPayload;
import net.misemise.platform.PlatformHooks;

import java.nio.file.Path;

public final class PlatformHooksImpl implements PlatformHooks {

    @Override
    public Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public boolean matchesRerollKey(KeyEvent event) {
        return RerollTradesFabricClient.getRerollKey() != null
                && RerollTradesFabricClient.getRerollKey().matches(event);
    }

    @Override
    public void sendRerollRequest() {
        ClientPlayNetworking.send(new RerollTradesPayload());
    }

    @Override
    public void sendParticle(ServerPlayer player, BlockPos pos) {
        ServerPlayNetworking.send(player, new RerollParticlePayload(pos));
    }
}
