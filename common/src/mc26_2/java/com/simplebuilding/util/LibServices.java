package com.simplebuilding.util;

import com.google.gson.JsonElement;
import com.simplebuilding.blocks.custom.TieredShulkerBoxBlock;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import org.jetbrains.annotations.Nullable;

/**
 * 26.2 twin of the 26.3 bridge to the SimpleLib services (mc26_3/overlay/java): no SimpleLib on this line, so the
 * upgrade files stay in {@code sledgehammer_upgrades} and are read by {@code SledgehammerUpgradeData} itself, and
 * contents use SimpleBuilding's own code; 26.2 has no octets (Simple Octets, split stage 6) and no picture blueprint.
 */
public final class LibServices {
    public static final String UPGRADE_DIRECTORY = "sledgehammer_upgrades";
    public static final @Nullable String LEGACY_UPGRADE_DIRECTORY = null;
    public static final String MIN_TOOL_FIELD = "min_hammer";

    private LibServices() {
    }

    /** Principle 5a, axe/hammer switch: nothing to switch on 26.2 (no SimpleLib). */
    public static void hammersReplaceAxes() {
    }

    public static void init() {
    }

    /** Client: the editor for the placed blueprint at {@code pos} (26.2: SimpleBuilding's own). */
    public static void openPlacedBlueprint(BlockPos pos, ItemStack stack) {
        com.simplebuilding.items.custom.BlueprintItem.openPlaced(pos, stack);
    }

    /** No SimpleLib on 26.2: the core {@code RecipeMapFilterMixin} filters the recipe table itself. */
    public static boolean recipesFilteredByLib() {
        return false;
    }

    /** Simple Gadgets' gadgets are 26.3 only: no item hint, no published numbers, no events, no probe on this line. */
    public static boolean gadgetItemHint(Item item, net.minecraft.world.item.context.UseOnContext context) {
        return false;
    }

    public static int gadgetFact(String key, int fallback) {
        return fallback;
    }

    public static void fireGadgetEvent(String event, net.minecraft.world.entity.player.Player player) {
    }

    /** WE25-7: 26.2 keeps its own chisels (SimpleBuilding's ChiselItem), the same view as Simple Chisels gives on 26.3. */
    public static java.util.List<ChiselTool> chiselTools() {
        java.util.List<ChiselTool> out = new java.util.ArrayList<>();
        for (Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            if (item instanceof com.simplebuilding.items.custom.ChiselItem chisel) {
                out.add(new ChiselTool(chisel, chisel.getForwardMap(), chisel.getBackwardMap(), chisel.getTouchForwardMap(),
                        chisel.getTouchBackwardMap(), chisel.getCooldownTicks(), chisel.isDedicatedSpatula(),
                        com.simplebuilding.items.custom.ChiselItem.TRANSFORM_DAMAGE, com.simplebuilding.items.custom.ChiselItem.REVERSE_TRANSFORM_DAMAGE));
            }
        }
        return out;
    }

    public static boolean gadgetProbing() {
        return false;
    }

    /** Null: {@code SledgehammerUpgradeData} reads the files itself. */
    public static @Nullable Map<Block, SledgehammerUpgrades.Upgrade> applyUpgrades(Map<Identifier, JsonElement> files) {
        return null;
    }

    public static boolean toolFits(Block from, ItemStack tool) {
        return true;
    }

    public static int replace(ServerLevel level, BlockPos pos, Block to) {
        if (to instanceof ChestBlock) {
            return TieredChests.upgradeInPlace(level, pos, to);
        }
        if (to instanceof TieredShulkerBoxBlock) {
            TieredShulkerBoxes.upgradeInPlace(level, pos, to);
            return 1;
        }
        com.simplebuilding.crucible.CrucibleCompat.upgradeInPlace(level, pos, to);
        return 1;
    }

    public static @Nullable Block octetCellFor(@Nullable Block full) {
        return null;
    }

    public static @Nullable Item octetPiece(@Nullable Block cell) {
        return null;
    }

    public static @Nullable Block octetBaseOf(@Nullable Block cell) {
        return null;
    }

    /** No picture blueprint on 26.2 (the conversion lives in SimpleLib); {@code pixels} lines report so. */
    public static boolean pictures() {
        return false;
    }

    public static int @Nullable [] decodePixelRow(String text, int maxLength) {
        return null;
    }

    public static String encodePixelRow(int[] indices, int from, int to) {
        throw new UnsupportedOperationException("no picture blueprint on 26.2");
    }

    public static int pixelSymbolIndex(char c) {
        return -2;
    }

    public static int @Nullable [] convertPicture(int[] argb, int width, int height, int maxSide, int[] paletteRgb, int dither) {
        return null;
    }

    public static void toolPlaced(ServerLevel level, net.minecraft.server.level.ServerPlayer player, BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
    }

    public static net.minecraft.world.item.Item.Properties rarity(String path, net.minecraft.world.item.Item.Properties properties) {
        return properties;
    }
}
