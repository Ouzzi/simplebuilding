package com.simplebuilding.util;

import net.minecraft.util.registry.tag.TagKey;
import net.minecraft.util.Formatting;
import net.minecraft.world.gen.structure.Structure;

public record StructureConfig(TagKey<Structure> tag, String name, Formatting color) {}
