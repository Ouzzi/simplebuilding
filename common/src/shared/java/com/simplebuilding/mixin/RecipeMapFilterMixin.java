package com.simplebuilding.mixin;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.simplebuilding.recipe.RecipeFilter;
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

/**
 * Nimmt die Rezepte abgeschalteter Funktionen aus der Rezepttabelle ({@link RecipeFilter}). Der
 * private Konstruktor von {@link RecipeMap} ist auf 1.21.11, 26.2, 26.3 und 26.4 derselbe (26.2 baut
 * die Tabelle beim Laden der Datenpakete aus den JSONs, 26.3 aus der Rezept-Registry); hier landet
 * also jede Tabelle, die der Server benutzt, auf jedem Loader.
 */
@Mixin(RecipeMap.class)
public abstract class RecipeMapFilterMixin {

    @Shadow @Final @Mutable
    private Multimap<RecipeType<?>, RecipeHolder<?>> byType;

    @Shadow @Final @Mutable
    private Map<ResourceKey<Recipe<?>>, RecipeHolder<?>> byKey;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void simplebuilding$dropDisabledFeatures(CallbackInfo ci) {
        if (byKey.isEmpty() || !RecipeFilter.anyDisabled()) {
            return;
        }
        ImmutableMultimap.Builder<RecipeType<?>, RecipeHolder<?>> types = ImmutableMultimap.builder();
        for (Map.Entry<RecipeType<?>, RecipeHolder<?>> entry : byType.entries()) {
            if (!RecipeFilter.removes(entry.getValue().id().identifier())) {
                types.put(entry.getKey(), entry.getValue());
            }
        }
        ImmutableMap.Builder<ResourceKey<Recipe<?>>, RecipeHolder<?>> keys = ImmutableMap.builder();
        for (Map.Entry<ResourceKey<Recipe<?>>, RecipeHolder<?>> entry : byKey.entrySet()) {
            if (!RecipeFilter.removes(entry.getKey().identifier())) {
                keys.put(entry.getKey(), entry.getValue());
            }
        }
        byType = types.build();
        byKey = keys.build();
    }
}
