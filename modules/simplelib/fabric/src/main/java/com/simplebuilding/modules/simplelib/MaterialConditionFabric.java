package com.simplebuilding.modules.simplelib;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simplelib.SimpleLib;
import com.simplelib.registry.LibMaterials;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.resources.RegistryOps;

/** {@code simplelib:material_requested}: true when a loaded mod requested the shared material group (stage 3d). */
public record MaterialConditionFabric(String material) implements ResourceCondition {
    public static final MapCodec<MaterialConditionFabric> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("material").forGetter(MaterialConditionFabric::material)).apply(i, MaterialConditionFabric::new));
    public static final ResourceConditionType<MaterialConditionFabric> TYPE = ResourceConditionType.create(SimpleLib.id("material_requested"), CODEC);

    @Override
    public ResourceConditionType<?> getType() {
        return TYPE;
    }

    @Override
    public boolean test(RegistryOps.RegistryInfoLookup lookup) {
        return LibMaterials.requested(material);
    }

    public static void register() {
        ResourceConditions.register(TYPE);
    }
}
