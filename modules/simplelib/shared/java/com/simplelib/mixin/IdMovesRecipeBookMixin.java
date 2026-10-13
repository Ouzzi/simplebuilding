package com.simplelib.mixin;

import com.simplelib.api.IdMoves;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceKey;
import net.minecraft.stats.ServerRecipeBook;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The recipe book keeps moved recipes ({@link IdMoves}, table {@code minecraft:recipe}): a saved key the server does
 * not know is replaced by its moved key before vanilla drops it with "Tried to load unrecognized recipe". Known keys,
 * and unknown keys without a known move, go to vanilla unchanged.
 */
@Mixin(ServerRecipeBook.class)
public abstract class IdMovesRecipeBookMixin {

    @Shadow
    public abstract void loadUntrusted(ServerRecipeBook.Packed packed, Predicate<ResourceKey<Recipe<?>>> validator);

    @Inject(method = "loadUntrusted", at = @At("HEAD"), cancellable = true)
    private void simplelib$moveRecipes(ServerRecipeBook.Packed packed, Predicate<ResourceKey<Recipe<?>>> validator, CallbackInfo ci) {
        List<ResourceKey<Recipe<?>>> known = simplelib$moved(packed.known(), validator);
        List<ResourceKey<Recipe<?>>> highlight = simplelib$moved(packed.highlight(), validator);
        if (known == packed.known() && highlight == packed.highlight()) return;
        // The moved keys are known, so this second call changes nothing more and goes to vanilla.
        this.loadUntrusted(new ServerRecipeBook.Packed(packed.settings(), known, highlight), validator);
        ci.cancel();
    }

    @Unique
    private static List<ResourceKey<Recipe<?>>> simplelib$moved(List<ResourceKey<Recipe<?>>> keys, Predicate<ResourceKey<Recipe<?>>> validator) {
        List<ResourceKey<Recipe<?>>> out = null;
        for (int i = 0; i < keys.size(); i++) {
            ResourceKey<Recipe<?>> key = keys.get(i);
            if (validator.test(key)) continue;
            // Follow a chain of moves (e.g. a test namespace onto SimpleBuilding, then into SimpleLib); move() refuses loops.
            ResourceKey<Recipe<?>> now = IdMoves.target(key);
            for (int hop = 0; now != null && !validator.test(now) && hop < 8; hop++) {
                ResourceKey<Recipe<?>> next = IdMoves.target(now);
                if (next == null) break;
                now = next;
            }
            if (now == null || !validator.test(now)) continue;
            if (out == null) out = new ArrayList<>(keys);
            out.set(i, now);
        }
        return out == null ? keys : out;
    }
}
