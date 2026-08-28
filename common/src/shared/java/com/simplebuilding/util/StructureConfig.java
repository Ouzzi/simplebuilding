package com.simplebuilding.util;

import net.minecraft.ChatFormatting;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;

public record StructureConfig(TagKey<Structure> tag, String name, ChatFormatting color) {}