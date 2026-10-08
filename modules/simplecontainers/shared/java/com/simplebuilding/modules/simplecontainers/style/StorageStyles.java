package com.simplebuilding.modules.simplecontainers.style;

import com.simplelib.api.client.ui.UiPalette;
import java.util.List;
import java.util.Locale;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.Nullable;

/**
 * Group "storage" (W0): chests and everything else on the generic 9xN screen (double chest, barrel, ender chest, chest
 * boats and minecarts), shulker boxes, hoppers, dispensers and droppers. Colours measured on owner image 3
 * ({@code previews/refs-n12/bild3-container-stil.webp}: box fill and slot colour per block). The title's translation
 * key decides the block family (a chest stays a chest even if the player looks at a barrel); the block looked at only
 * refines it (copper chest weathering, shulker colour) or stands in for a custom name. Otherwise: an oak chest.
 */
public final class StorageStyles {
    /** Image 3, chest: warm oak. */
    public static final UiPalette OAK = UiPalette.derived(0xFFC89044, 0xFFA47438);
    /** Image 3, barrel: darker spruce brown. */
    public static final UiPalette BARREL = UiPalette.derived(0xFFA47850, 0xFF805C44);
    /** Image 3, ender chest: petrol. */
    public static final UiPalette ENDER = UiPalette.derived(0xFF587880, 0xFF405C64);
    /** Image 3, shulker box without dye: purple. */
    public static final UiPalette SHULKER = UiPalette.derived(0xFF846C98, 0xFF705880);
    /** Hopper: iron grey (the crucible's iron tier). */
    public static final UiPalette HOPPER = new UiPalette(0xFF9A9DA2, 0xFFC4C7CB, 0xFF7E8186, 0xFF7B7E83, 0xFF64676C, 0xFF2E3034);
    /** Dispenser and dropper: cobblestone grey (image 3, furnace). */
    public static final UiPalette STONE = UiPalette.derived(0xFF909498, 0xFF6C7070);
    /** Copper chest by weathering: copper, exposed, weathered, oxidized. */
    public static final List<UiPalette> COPPER = List.of(
            UiPalette.derived(0xFFC0714F, 0xFF9A5A3E), UiPalette.derived(0xFFA27C68, 0xFF826352),
            UiPalette.derived(0xFF6E9A7E, 0xFF557A63), UiPalette.derived(0xFF52A08C, 0xFF3F7E6E));
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

    /** A dyed shulker box: the dye's texture colour, a little greyed like image 3. */
    public static UiPalette dyed(DyeColor dye) {
        int fill = UiPalette.mix(dye.getTextureDiffuseColor(), 0xFF8A8A8A, 0.45);
        return UiPalette.derived(fill, UiPalette.scale(fill, 0.85));
    }

    /** The path of a {@code minecraft:} block id, or {@code null} (no block, or a block of another mod). */
    static @Nullable String vanillaPath(@Nullable String blockId) {
        if (blockId == null || !blockId.startsWith("minecraft:")) return null;
        return blockId.substring("minecraft:".length()).toLowerCase(Locale.ROOT);
    }
}
