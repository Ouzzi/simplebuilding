package com.simplebuilding.modules.simplemobs.mixin;

import com.simplebuilding.modules.simplemobs.trials.TrialsExtras;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import java.util.function.Predicate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The config weatheredStructures: when off, the Weathered Trial finds no generation point in new chunks. */
@Mixin(Structure.class)
public abstract class StructureWeatheredMixin {
    @Inject(method = "generate", at = @At("HEAD"), cancellable = true)
    private void simplemobs$switch(Holder<Structure> selected, ResourceKey<Level> dimension, RegistryAccess registryAccess, ChunkGenerator generator, BiomeSource biomes,
            Climate.Sampler climate, RandomState random, StructureTemplateManager templates, long seed, ChunkPos chunk, int references, LevelHeightAccessor height,
            Predicate<Holder<Biome>> validBiome, CallbackInfoReturnable<StructureStart> cir) {
        if (TrialsExtras.weatheredBlocked(selected)) cir.setReturnValue(StructureStart.INVALID_START);
    }
}
