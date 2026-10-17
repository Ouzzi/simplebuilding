package com.simplecrucibles.effect;

import com.simplecrucibles.SimpleCrucibles;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import org.jspecify.annotations.Nullable;

/** Seelenbrand (soul burn), given by touching soul lava (split stage 5b: was {@code simplebuilding:soul_burn}). */
public final class CrucibleEffects {
    public static final Identifier SOUL_BURN_ID = SimpleCrucibles.id("soul_burn");
    public static @Nullable Holder<MobEffect> SOUL_BURN;

    /** Fabric directly, NeoForge/Forge in the MOB_EFFECT RegisterEvent. */
    public static void register() {
        if (SOUL_BURN == null) SOUL_BURN = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, SOUL_BURN_ID, new SoulBurnEffect(SoulBurnEffect.COLOR));
    }

    private CrucibleEffects() {}
}
