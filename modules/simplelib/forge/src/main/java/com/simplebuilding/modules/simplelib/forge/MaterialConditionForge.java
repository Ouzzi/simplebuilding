package com.simplebuilding.modules.simplelib.forge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simplelib.registry.LibMaterials;
import net.minecraftforge.common.crafting.conditions.ICondition;

/** {@code simplelib:material_requested}: true when a loaded mod requested the shared material group (stage 3d). */
public record MaterialConditionForge(String material) implements ICondition {
    public static final MapCodec<MaterialConditionForge> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("material").forGetter(MaterialConditionForge::material)).apply(i, MaterialConditionForge::new));

    @Override
    public boolean test(IContext context, DynamicOps<?> ops) {
        return LibMaterials.requested(material);
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }
}
