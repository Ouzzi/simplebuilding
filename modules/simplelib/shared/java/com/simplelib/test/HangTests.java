package com.simplelib.test;

import com.simplelib.api.HangWatchdog;
import com.simplelib.api.OwnedLights;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;

/** BS27-1: client/server hang while travelling and quitting - the deferred light sweep and the hang watchdog. */
final class HangTests {
    private HangTests() {}

    /**
     * The chunk-load hook only remembers the chunk (touching its blocks inside the load deadlocks the server thread); the
     * marked, unclaimed light block goes at the end of the tick, a light nobody marked stays.
     */
    static void ownedLightsSweepDeferred(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos marked = h.absolutePos(new BlockPos(1, 2, 1)), foreign = h.absolutePos(new BlockPos(2, 2, 1));
        var light = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 12);
        level.setBlock(marked, light, 3);
        level.setBlock(foreign, light, 3);
        OwnedLights.mark(level, marked);
        int before = OwnedLights.pending();
        OwnedLights.chunkLoaded(level, ChunkPos.containing(marked));
        h.assertTrue(OwnedLights.pending() == before + 1, "the loaded chunk is remembered");
        h.assertTrue(level.getBlockState(marked).is(Blocks.LIGHT), "nothing is touched inside the chunk load");
        OwnedLights.tick(level.getServer());
        h.assertTrue(OwnedLights.pending() == 0, "the tick sweeps the remembered chunks");
        h.assertTrue(level.getBlockState(marked).isAir(), "the marked light without a carrier is gone after the tick");
        h.assertTrue(!OwnedLights.isMarked(level, marked), "its mark is gone");
        h.assertTrue(level.getBlockState(foreign).is(Blocks.LIGHT), "a light nobody marked stays");
        level.setBlock(foreign, Blocks.AIR.defaultBlockState(), 3);
        h.succeed();
    }

    /** The dump names the server thread first with its stack; the due rule: at the limit, again at four times, then never. */
    static void hangWatchdogDump(GameTestHelper h) {
        long s = 1_000_000_000L;
        h.assertTrue(!HangWatchdog.due(29 * s, 30, 0) && HangWatchdog.due(30 * s, 30, 0), "first dump at the limit");
        h.assertTrue(!HangWatchdog.due(119 * s, 30, 1) && HangWatchdog.due(120 * s, 30, 1), "second dump at four times the limit");
        h.assertTrue(!HangWatchdog.due(3600 * s, 30, HangWatchdog.MAX_DUMPS), "no more dumps in the same stall");
        Thread serverThread = h.getLevel().getServer().getRunningThread();
        Path dir = null, file = null;
        try {
            dir = Files.createTempDirectory("simplelib-hang-test");
            file = HangWatchdog.write(dir, "test", serverThread);
            h.assertTrue(file != null && file.getFileName().toString().startsWith(HangWatchdog.PREFIX), "dump file " + file);
            String text = Files.readString(file);
            h.assertTrue(text.contains("Reason: test") && text.contains("Deadlocked threads:"), "dump header");
            int first = text.indexOf("\n\n\"");
            h.assertTrue(first > 0 && text.startsWith("\"" + serverThread.getName() + "\"", first + 2), "the server thread comes first");
            h.assertTrue(text.contains("\tat "), "full stacks");
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        } finally {
            try {
                if (file != null) Files.deleteIfExists(file);
                if (dir != null) Files.deleteIfExists(dir);
            } catch (java.io.IOException ignored) {
            }
        }
        h.succeed();
    }
}
