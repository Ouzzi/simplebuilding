package com.simplebuilding.modules.simpleinterfaces.style;

import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.Nullable;

/**
 * What the client knows about an open container when picking its colours: the menu type, the title's translation key
 * ({@code null} for a custom name) and the block the player looked at when opening it ({@code null} for entities,
 * items or nothing; e.g. {@code minecraft:red_shulker_box}).
 */
public record StyleContext(MenuType<?> menu, @Nullable String titleKey, @Nullable String blockId) {}
