package com.simplebuilding.util;

import com.google.gson.JsonElement;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blocks.custom.MaterialOctetBlock;
import com.simplebuilding.blocks.custom.TieredShulkerBoxBlock;
import com.simplebuilding.data.SledgehammerUpgradeData;
import com.simplelib.api.InWorldUpgrades;
import com.simplelib.api.OctetCells;
import com.simplelib.api.SimpleLibApi;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import org.jetbrains.annotations.Nullable;

/**
 * 26.3: SimpleBuilding on top of the SimpleLib services of the module split (stage 3c, PLAN-MODULAUFTEILUNG). Only
 * {@code com.simplelib.api} is imported (principle 6a); the 26.2 twin in common/src/mc26_2/java keeps SimpleBuilding's
 * own ways (no SimpleLib there).
 * <ul>
 *   <li>The sledgehammer upgrades are the data type {@code in_world_upgrades} ({@link InWorldUpgrades}); the old folder
 *       {@code sledgehammer_upgrades} is still read (modpacks), SimpleBuilding's files apply first.</li>
 *   <li>Tiered chests and shulker boxes move their contents through upgrade handlers; crucibles and barrels through
 *       SimpleLib's own.</li>
 *   <li>SimpleBuilding's octet cells are registered with {@link OctetCells}; the sledgehammer carves through it.</li>
 * </ul>
 */
public final class LibServices {
    /** Datapack folder of the upgrade files. */
    public static final String UPGRADE_DIRECTORY = InWorldUpgrades.DIRECTORY;
    /** The folder before the split, still read; files there win over a same-named file in the new folder. */
    public static final @Nullable String LEGACY_UPGRADE_DIRECTORY = "sledgehammer_upgrades";
    /** Field of the minimum hammer grade in the shipped files. */
    public static final String MIN_TOOL_FIELD = "min_tool";

    private LibServices() {
    }

    /**
     * Principle 5a, axe/hammer switch (split plan §2.6): SimpleBuilding has a working hammer way, so SimpleLib's axe
     * ways (crucible build, barrel attach, crucible/cauldron upgrades) go off and only the sledgehammer runs them. Its
     * own call, because it moves to Simple Hammers in stage 7 (the hammer mod switches the axe off, not SimpleBuilding).
     */
    public static void hammersReplaceAxes() {
        SimpleLibApi.disableAxeWays();
    }

    /** After the blocks and items are registered (ModItems.registerModItems). */
    public static void init() {
        // Split stage 3d: SimpleBuilding uses every shared material - their recipes and the anvil crushing stay on.
        SharedMaterials.request();
        // Simple Mobheads (required sub-mod) asks framework WornHeads for extra worn heads: the extra head slot (N49).
        com.simplebuilding.framework.api.WornHeads.register(Simplebuilding.MOD_ID, entity ->
                entity instanceof net.minecraft.world.entity.LivingEntity living ? java.util.List.of(ExtraHead.get(living)) : java.util.List.of());
        InWorldUpgrades.registerDefaults(Simplebuilding.MOD_ID);
        // Same split as SledgehammerUpgrades.Upgrade#isChest / #isShulkerBox: every chest target moves like a tiered chest.
        SimpleLibApi.registerUpgradeHandler(block -> block instanceof ChestBlock, TieredChests::upgradeInPlace);
        SimpleLibApi.registerUpgradeHandler(block -> block instanceof TieredShulkerBoxBlock, (level, pos, to) -> {
            TieredShulkerBoxes.upgradeInPlace(level, pos, to);
            return 1;
        });
        for (Map.Entry<Block, Block> entry : MaterialOctets.ownCells().entrySet()) {
            Block cell = entry.getValue();
            Supplier<Item> piece = cell instanceof MaterialOctetBlock octet ? octet::piece : () -> null;
            OctetCells.register(entry.getKey(), cell, piece);
        }
    }

    /** Applies the upgrade files through SimpleLib and returns SimpleBuilding's view of the effective table. */
    public static Map<Block, SledgehammerUpgrades.Upgrade> applyUpgrades(Map<Identifier, JsonElement> files) {
        InWorldUpgrades.apply(files);
        Map<Block, SledgehammerUpgrades.Upgrade> table = new HashMap<>();
        for (InWorldUpgrades.Entry entry : InWorldUpgrades.table().values()) {
            SledgehammerUpgrades.Upgrade upgrade = SledgehammerUpgradeData.upgrade(entry.from(), entry.to(), entry.material(),
                    entry.minTool(), entry.damagePerHit(), entry.tier(), entry.durationFactor(), entry.materialCount());
            if (upgrade != null) {
                table.put(upgrade.from(), upgrade);
            }
        }
        return table;
    }

    /** Whether {@code tool} is the tool the upgrade from {@code from} names (default {@code #simplelib:upgrade_tools}). */
    public static boolean toolFits(Block from, ItemStack tool) {
        InWorldUpgrades.Entry entry = InWorldUpgrades.entryFor(from);
        return entry == null || InWorldUpgrades.toolFits(entry, tool);
    }

    /** Moves a chest, shulker box, crucible or barrel to {@code to} with its contents; returns the blocks changed. */
    public static int replace(ServerLevel level, BlockPos pos, Block to) {
        return InWorldUpgrades.replace(level, pos, to);
    }

    public static @Nullable Block octetCellFor(@Nullable Block full) {
        return OctetCells.cellFor(full);
    }

    public static @Nullable Item octetPiece(@Nullable Block cell) {
        return OctetCells.pieceOf(cell);
    }
}
