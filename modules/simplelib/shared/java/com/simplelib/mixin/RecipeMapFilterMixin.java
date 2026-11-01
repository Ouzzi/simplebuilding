package com.simplelib.mixin;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.simplelib.api.RecipeFilters;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Takes the recipes of switched-off module features out of the recipe table ({@link RecipeFilters}). */
@Mixin(RecipeMap.class)
public abstract class RecipeMapFilterMixin {

    @Shadow @Final @Mutable
    private Multimap<RecipeType<?>, RecipeHolder<?>> byType;

    @Shadow @Final @Mutable
    private Map<ResourceKey<Recipe<?>>, RecipeHolder<?>> byKey;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void simplelib$dropDisabledFeatures(CallbackInfo ci) {
        if (byKey.isEmpty() || !RecipeFilters.any() || byKey.keySet().stream().noneMatch(k -> RecipeFilters.removes(k.identifier()))) {
            return;
        }
        ImmutableMultimap.Builder<RecipeType<?>, RecipeHolder<?>> types = ImmutableMultimap.builder();
        for (Map.Entry<RecipeType<?>, RecipeHolder<?>> entry : byType.entries()) {
            if (!RecipeFilters.removes(entry.getValue().id().identifier())) {
                types.put(entry.getKey(), entry.getValue());
            }
        }
        ImmutableMap.Builder<ResourceKey<Recipe<?>>, RecipeHolder<?>> keys = ImmutableMap.builder();
        for (Map.Entry<ResourceKey<Recipe<?>>, RecipeHolder<?>> entry : byKey.entrySet()) {
            if (!RecipeFilters.removes(entry.getKey().identifier())) {
                keys.put(entry.getKey(), entry.getValue());
            }
        }
        byType = types.build();
        byKey = keys.build();
    }
}
