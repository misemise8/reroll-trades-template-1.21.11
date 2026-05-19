package net.misemise.neoforge;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.misemise.network.RerollParticlePayload;
import net.misemise.network.RerollTradesPayload;
import net.misemise.platform.PlatformHooks;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;

import java.nio.file.Path;

public final class PlatformHooksImpl implements PlatformHooks {

    @Override
    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public boolean matchesRerollKey(KeyEvent event) {
        return RerollTradesNeoForgeClient.REROLL_KEY.isActiveAndMatches(InputConstants.getKey(event));
    }

    @Override
    public void sendRerollRequest() {
        ClientPacketDistributor.sendToServer(new RerollTradesPayload());
    }

    @Override
    public void sendParticle(ServerPlayer player, BlockPos pos) {
        PacketDistributor.sendToPlayer(player, new RerollParticlePayload(pos));
    }
}
