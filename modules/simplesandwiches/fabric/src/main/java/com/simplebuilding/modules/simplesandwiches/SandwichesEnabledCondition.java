package com.simplebuilding.modules.simplesandwiches;

import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;

/** Data condition {@code simplesandwiches:enabled}: recipes load only while the super-mod switch is on. */
public record SandwichesEnabledCondition() implements ResourceCondition {
    public static final MapCodec<SandwichesEnabledCondition> CODEC = MapCodec.unit(new SandwichesEnabledCondition());
    public static final ResourceConditionType<SandwichesEnabledCondition> TYPE =
            ResourceConditionType.create(Identifier.fromNamespaceAndPath("simplesandwiches", "enabled"), CODEC);

    @Override public ResourceConditionType<?> getType() { return TYPE; }
    @Override public boolean test(RegistryOps.RegistryInfoLookup lookup) { return com.simplesandwiches.Sandwiches.on(); }
    public static void register() { ResourceConditions.register(TYPE); }
}
