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
 * contents use SimpleBuilding's own code; 26.2 has no octets (Simple Octets, split stage 6).
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
}
