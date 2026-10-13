package com.simplebuilding.modules.simplelib;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simplelib.registry.LibMaterials;
import net.neoforged.neoforge.common.conditions.ICondition;

/** {@code simplelib:material_requested}: true when a loaded mod requested the shared material group (stage 3d). */
public record MaterialConditionNeo(String material) implements ICondition {
    public static final MapCodec<MaterialConditionNeo> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("material").forGetter(MaterialConditionNeo::material)).apply(i, MaterialConditionNeo::new));

    @Override
    public boolean test(IContext context) {
        return LibMaterials.requested(material);
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }
}
