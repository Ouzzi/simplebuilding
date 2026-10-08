package com.simplebuilding.modules.simplecontainers.style;

import com.simplelib.api.client.ui.UiPalette;
import java.util.List;
import java.util.Locale;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.Nullable;

/**
 * Group "storage" (W0): chests and everything else on the generic 9xN screen (double chest, barrel, ender chest, chest
 * boats and minecarts), shulker boxes, hoppers, dispensers and droppers. Fills as in the W0-B preview palette table
 * (docs/ai/PLAN-SIMPLECONTAINERS-2026-10-08.md, from owner image 3), all other colours from {@link UiPalette#derived}. The title's translation
 * key decides the block family (a chest stays a chest even if the player looks at a barrel); the block looked at only
 * refines it (copper chest weathering, shulker colour) or stands in for a custom name. Otherwise: an oak chest.
 */
public final class StorageStyles {
    /** Chest: warm oak (image 3). */
    public static final UiPalette OAK = UiPalette.derived(0xFFCE9148);
    /** Barrel: the crucible's barrel colours. */
    public static final UiPalette BARREL = UiPalette.BARREL;
    /** Ender chest: petrol (image 3). */
    public static final UiPalette ENDER = UiPalette.derived(0xFF597880);
    /** Shulker box without dye: purple (image 3). */
    public static final UiPalette SHULKER = UiPalette.derived(0xFF876C99);
    /** Light blue shulker box (preview W0-B). */
    public static final UiPalette SHULKER_LIGHT_BLUE = UiPalette.derived(0xFF5C97B8);
    /** Hopper: dark iron. */
    public static final UiPalette HOPPER = UiPalette.derived(0xFF5A5C63);
    /** Dispenser and dropper: stone grey. */
    public static final UiPalette STONE = UiPalette.derived(0xFF878787);
    /** Copper chest by weathering: copper, exposed, weathered, oxidized (block texture colours, muted). */
    public static final List<UiPalette> COPPER = List.of(UiPalette.derived(0xFFC0714F), UiPalette.derived(0xFFA27C68),
            UiPalette.derived(0xFF6E9A7E), UiPalette.derived(0xFF52A08C));
    private static final List<String> COPPER_STAGES = List.of("copper_chest", "exposed_copper_chest",
            "weathered_copper_chest", "oxidized_copper_chest");

    public static final List<ScreenStyle> STYLES = List.of(
            new ScreenStyle("chest", ScreenStyle.VANILLA + "ContainerScreen", List.of(MenuType.GENERIC_9x1, MenuType.GENERIC_9x2,
                    MenuType.GENERIC_9x3, MenuType.GENERIC_9x4, MenuType.GENERIC_9x5, MenuType.GENERIC_9x6), StorageStyles::chest),
            new ScreenStyle("shulker_box", ScreenStyle.VANILLA + "ShulkerBoxScreen", List.of(MenuType.SHULKER_BOX), StorageStyles::shulker),
            new ScreenStyle("hopper", ScreenStyle.VANILLA + "HopperScreen", List.of(MenuType.HOPPER), context -> HOPPER),
            new ScreenStyle("dispenser", ScreenStyle.VANILLA + "DispenserScreen", List.of(MenuType.GENERIC_3x3), context -> STONE));

    private StorageStyles() {}

    /** Generic 9xN screen: chest family by title, else by the block looked at, else oak. */
    static UiPalette chest(StyleContext context) {
        String key = context.titleKey();
        if (key != null) {
            switch (key) {
                case "container.barrel": return BARREL;
                case "container.enderchest": return ENDER;
                case "container.chest", "container.chestDouble": return chestBlock(context.blockId());
                default: break;
            }
        }
        String block = vanillaPath(context.blockId());
        if ("barrel".equals(block)) return BARREL;
        if ("ender_chest".equals(block)) return ENDER;
        return chestBlock(context.blockId());
    }

    /** A chest's colour from its block: the copper stages, otherwise oak (chest, trapped chest, unknown). */
    static UiPalette chestBlock(@Nullable String blockId) {
        String block = vanillaPath(blockId);
        if (block == null) return OAK;
        int stage = COPPER_STAGES.indexOf(block.startsWith("waxed_") ? block.substring(6) : block);
        return stage >= 0 ? COPPER.get(stage) : OAK;
    }

    /** Shulker box: the dye colour of the box looked at, else image 3's purple. */
    static UiPalette shulker(StyleContext context) {
        String block = vanillaPath(context.blockId());
        if (block == null || !block.endsWith("_shulker_box")) return SHULKER;
        String color = block.substring(0, block.length() - "_shulker_box".length());
        for (DyeColor dye : DyeColor.values()) {
            if (dye.getName().equals(color)) return dyed(dye);
        }
        return SHULKER;
    }

    /** A dyed shulker box: purple and light blue as previewed, other dyes their texture colour mixed half with grey. */
    public static UiPalette dyed(DyeColor dye) {
        if (dye == DyeColor.PURPLE) return SHULKER;
        if (dye == DyeColor.LIGHT_BLUE) return SHULKER_LIGHT_BLUE;
        return UiPalette.derived(UiPalette.mix(dye.getTextureDiffuseColor(), 0xFF808080, 0.4));
    }

    /** The path of a {@code minecraft:} block id, or {@code null} (no block, or a block of another mod). */
    static @Nullable String vanillaPath(@Nullable String blockId) {
        if (blockId == null || !blockId.startsWith("minecraft:")) return null;
        return blockId.substring("minecraft:".length()).toLowerCase(Locale.ROOT);
    }
}
