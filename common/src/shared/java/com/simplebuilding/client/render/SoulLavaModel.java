package com.simplebuilding.client.render;

import com.simplebuilding.fluid.ModFluids;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;

/** Fluid model of soul lava (turquoise lava textures, no overlay, no tint) for the loaders' fluid model hooks. */
public final class SoulLavaModel {
    public static FluidModel.Unbaked unbaked() {
        return new FluidModel.Unbaked(new Material(ModFluids.id("block/soul_lava_still")),
                new Material(ModFluids.id("block/soul_lava_flow")), null, null);
    }

    private SoulLavaModel() {}
}
