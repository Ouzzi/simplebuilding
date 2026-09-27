package com.simplebuilding.util;

import net.minecraft.ChatFormatting;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;

/** Ein Struktur-Kompass aus dem Amboss; {@code name} ist der Uebersetzungsschluessel seines Namens. */
public record StructureConfig(TagKey<Structure> tag, String name, ChatFormatting color) {}