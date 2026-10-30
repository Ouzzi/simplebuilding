package com.simplebuilding.modules.simplechallenges.core;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelData;

/** The fixed spawn of a fresh challenge world: block 0,0 without the Vanilla scatter, so the start always lies inside the start area. */
public final class Spawn {
    private Spawn() {}

    /** Height of the first free block above the ground at 0,0 (loads that chunk, as the world's own spawn search does). */
    public static int surfaceY(ServerLevel level) {
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 0, 0);
    }

    /** Sets the world spawn to the block {@code (0, y, 0)} and the scatter radius to 0 (the game rule stays set in this world). */
    public static void setAtOrigin(MinecraftServer server, int y) {
        ServerLevel ow = server.overworld();
        ow.setRespawnData(LevelData.RespawnData.of(Level.OVERWORLD, new BlockPos(0, y, 0), 0.0F, 0.0F));
        ow.getGameRules().set(GameRules.RESPAWN_RADIUS, 0, server);
    }
}
