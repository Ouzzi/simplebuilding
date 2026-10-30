package com.simplebuilding.modules.simplechallenges.client;

import com.simplebuilding.modules.simplechallenges.SimpleChallenges;
import com.simplebuilding.modules.simplechallenges.core.PendingChallenge;
import com.simplebuilding.modules.simplechallenges.core.PresetRegistry;
import com.simplebuilding.modules.simplechallenges.core.RecentChallenges;
import com.simplebuilding.modules.simplechallenges.preset.Preset;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorPresets;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

/**
 * What the world creation screen knows about challenges (S3): the world type "Simple Challenges" (a world preset of the mod, in the
 * type button next to Superflat), the choice that waits for the new world's server ({@link PendingChallenge}) and the dimensions
 * the choice needs: a void Overworld for the Skyblock family, the normal world for everything else.
 */
public final class CreateChallenge {
    public static final ResourceKey<WorldPreset> WORLD_TYPE = ResourceKey.create(Registries.WORLD_PRESET, SimpleChallenges.id("simple_challenges"));

    private CreateChallenge() {}

    /** True when the world type button shows "Simple Challenges". */
    public static boolean selected(WorldCreationUiState ui) {
        Holder<WorldPreset> h = ui.getWorldType().preset();
        return h != null && h.is(WORLD_TYPE);
    }

    /** The presets for the screens: shipped and the player's own (before any server exists). */
    public static void loadPresets() {
        PresetRegistry.loadClient(SimpleChallenges.configDir);
    }

    public static List<String> recent() {
        return RecentChallenges.shown(RecentChallenges.load(SimpleChallenges.configDir));
    }

    /** The player plays this preset: it waits for the new world, goes to the recent buttons and the dimensions match it. */
    public static boolean choose(CreateWorldScreen screen, String id) {
        Preset p = PresetRegistry.get(id);
        if (p == null || !p.runnable()) return false;
        PendingChallenge.set(id);
        RecentChallenges.save(SimpleChallenges.configDir, RecentChallenges.push(RecentChallenges.load(SimpleChallenges.configDir), id));
        sync(screen.getUiState());
        return true;
    }

    /** Whether the chosen preset starts in the void world (the Skyblock family). */
    public static boolean needsVoid(String presetId) {
        Preset p = presetId == null ? null : PresetRegistry.get(presetId);
        return p != null && p.world() != null;
    }

    /** Makes the Overworld generator fit the choice (void for Skyblock, normal otherwise); a no-op when it already fits. */
    public static void sync(WorldCreationUiState ui) {
        boolean wantVoid = needsVoid(PendingChallenge.peek());
        boolean isVoid = ui.getSettings().selectedDimensions().overworld() instanceof FlatLevelSource;
        if (wantVoid && !isVoid) ui.updateDimensions(voidWorld());
        else if (!wantVoid && isVoid) ui.updateDimensions((registries, dims) -> WorldPresets.createNormalWorldDimensions(registries));
    }

    /** The Vanilla "The Void" flat preset with plains as the biome (animals and trees can live there), no structures. */
    static WorldCreationContext.DimensionsUpdater voidWorld() {
        return (registries, dims) -> {
            FlatLevelGeneratorSettings base = registries.lookupOrThrow(Registries.FLAT_LEVEL_GENERATOR_PRESET)
                    .getOrThrow(FlatLevelGeneratorPresets.THE_VOID).value().settings();
            Holder<Biome> plains = registries.lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS);
            FlatLevelGeneratorSettings settings = base.withBiomeAndLayers(List.of(new FlatLayerInfo(1, Blocks.AIR)), Optional.of(HolderSet.empty()), plains);
            return dims.replaceOverworldGenerator(registries, new FlatLevelSource(settings));
        };
    }
}
