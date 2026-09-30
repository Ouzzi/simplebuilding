package com.simplefun.registry;

import com.simplefun.Constants;
import com.simplefun.effect.PiggyEffect;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;

public class ModEffects {

    public static final ResourceKey<MobEffect> PIGGY_KEY =
            ResourceKey.create(Registries.MOB_EFFECT, Identifier.fromNamespaceAndPath(Constants.MOD_ID, "piggy_effect"));

    public static final MobEffect PIGGY_EFFECT = new PiggyEffect();

    private static Holder<MobEffect> piggyHolder;

    /** Lazily resolved at first use (after registries are frozen) - safe on both loaders. */
    public static Holder<MobEffect> holder() {
        Holder<MobEffect> h = piggyHolder;
        if (h == null) {
            h = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(PIGGY_EFFECT);
            piggyHolder = h;
        }
        return h;
    }
}
