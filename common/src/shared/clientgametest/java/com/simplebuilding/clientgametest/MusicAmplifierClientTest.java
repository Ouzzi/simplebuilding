package com.simplebuilding.clientgametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.version.McVersion;
import java.util.function.Consumer;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;

/**
 * Queue N31 (2026-10-10, 26.3): a playing jukebox relays its music by radio to the jukebox amplifiers around it, and
 * each of them shows the jukebox's music notes. One picture with notes above the jukebox and both amplifiers
 * ({@code amplifier-relay-notes}); clears its blocks and puts the particle option back.
 */
public final class MusicAmplifierClientTest {
    private static final int Z = 19;

    private MusicAmplifierClientTest() {
    }

    public static void inWorld(Script script) {
        if (!McVersion.MUSIC_DISCS) {
            return;
        }
        TestScene.build(script, "minecraft:smooth_stone", "creative");
        script.act("show every particle", client -> client.options.particles().set(ParticleStatus.ALL));
        onServer(script, "place a jukebox and two amplifiers, then play a disc", server -> {
            ServerLevel level = server.overworld();
            level.setBlock(new BlockPos(7, 0, Z), Blocks.JUKEBOX.defaultBlockState(), 3);
            level.setBlock(new BlockPos(10, 0, Z), ModBlocks.JUKEBOX_AMPLIFIER.defaultBlockState(), 3);
            level.setBlock(new BlockPos(14, 0, Z), ModBlocks.JUKEBOX_AMPLIFIER.defaultBlockState(), 3);
            if (level.getBlockEntity(new BlockPos(7, 0, Z)) instanceof JukeboxBlockEntity jukebox) {
                jukebox.setTheItem(new ItemStack(Items.MUSIC_DISC_CAT));
            }
        });
        script.command("tp @a 10.5 1.0 13.5 0.0 20.0");
        script.awaitPackets();
        script.idle("let the blocks render", 10);
        script.await("notes above the jukebox and both amplifiers", 200, client -> particles(client) >= 3,
                client -> "only " + client.particleEngine.countParticles() + " particles");
        script.shot("amplifier-relay-notes");
        script.command("fill 4 0 " + Z + " 16 2 " + Z + " minecraft:air", true);
        script.command("tp @a 10.5 0.0 16.5 0.0 0.0");
        script.act("back to minimal particles", client -> client.options.particles().set(ParticleStatus.MINIMAL));
        script.awaitPackets();
    }

    private static int particles(net.minecraft.client.Minecraft client) {
        String count = client.particleEngine.countParticles();
        return Integer.parseInt(count.substring(count.lastIndexOf(' ') + 1));
    }

    private static void onServer(Script script, String name, Consumer<MinecraftServer> work) {
        script.act(name, client -> {
            MinecraftServer server = client.getSingleplayerServer();
            if (server == null) {
                throw new AssertionError("There is no integrated server");
            }
            server.execute(() -> work.accept(server));
        });
    }
}
