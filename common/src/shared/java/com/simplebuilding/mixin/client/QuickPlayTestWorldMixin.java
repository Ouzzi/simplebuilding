package com.simplebuilding.mixin.client;

import com.simplebuilding.dev.testcentre.TestCentreCommand;
import com.simplebuilding.platform.ModEnvironment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.quickplay.QuickPlay;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Launch hub "Client + fresh test world" (owner 2026-10-01: the test world never started by itself): in a
 * development environment, quick play into {@link TestCentreCommand#WORLD_NAME} creates the world when it
 * does not exist yet - flat, creative, cheats on, peaceful, no structures - and enters it; the test centre
 * then builds itself on the first join ({@code TestCentreCommand#onPlayerJoin}). Every other quick play,
 * and every production client, keeps vanilla's behaviour.
 */
@Mixin(QuickPlay.class)
public abstract class QuickPlayTestWorldMixin {

    @Inject(method = "joinSingleplayerWorld", at = @At("HEAD"), cancellable = true)
    private static void simplebuilding$createMissingTestWorld(Minecraft minecraft, String name, CallbackInfo ci) {
        if (!ModEnvironment.isDevelopmentEnvironment() || !TestCentreCommand.WORLD_NAME.equals(name)
                || minecraft.getLevelSource().levelExists(name)) {
            return;
        }
        LevelSettings settings = new LevelSettings(name, GameType.CREATIVE,
                new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false), true, WorldDataConfiguration.DEFAULT);
        WorldOptions options = new WorldOptions(WorldOptions.randomSeed(), false, false);
        minecraft.createWorldOpenFlows().createFreshLevel(name, settings, options,
                registries -> registries.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                new TitleScreen());
        ci.cancel();
    }
}
