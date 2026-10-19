package com.simplebuilding.util;

import com.google.gson.JsonElement;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blocks.custom.TieredShulkerBoxBlock;
import com.simplebuilding.data.SledgehammerUpgradeData;
import com.simplelib.api.HammerActions;
import com.simplelib.api.InWorldUpgrades;
import com.simplelib.api.OctetCells;
import com.simplelib.api.SimpleLibApi;
import java.util.HashMap;
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
 * 26.3: SimpleBuilding on top of the SimpleLib services of the module split (stage 3c, PLAN-MODULAUFTEILUNG). Only
 * {@code com.simplelib.api} is imported (principle 6a); the 26.2 twin in common/src/mc26_2/java keeps SimpleBuilding's
 * own ways (no SimpleLib there).
 * <ul>
 *   <li>The sledgehammer upgrades are the data type {@code in_world_upgrades} ({@link InWorldUpgrades}); the old folder
 *       {@code sledgehammer_upgrades} is still read (modpacks), SimpleBuilding's files apply first.</li>
 *   <li>Tiered chests and shulker boxes move their contents through upgrade handlers; crucibles and barrels through
 *       SimpleLib's own.</li>
 *   <li>Simple Octets registers the octet cells with {@link OctetCells} (split stage 6); the sledgehammer carves through it.</li>
 *   <li>The sledgehammer is Simple Hammers' (split stage 7); SimpleBuilding plugs its hammer ways in through
 *       {@link HammerActions} ({@link #registerHammerWays}): placed trim templates, flat discs, chips, the octant
 *       selection, double chests, fresh breaker pistons, its advancements and counters.</li>
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
     * Principle 5a, axe/hammer switch (split plan §2.6): since split stage 7 Simple Hammers (required sub-mod) switches
     * SimpleLib's axe ways off itself when it loads; SimpleBuilding no longer does.
     */
    public static void hammersReplaceAxes() {
    }

    /** After the blocks and items are registered (ModItems.registerModItems). */
    public static void init() {
        // Split stage 3d: SimpleBuilding uses every shared material - their recipes and the anvil crushing stay on.
        SharedMaterials.request();
        // Simple Mobheads (required sub-mod) asks framework WornHeads for extra worn heads: the extra head slot (N49).
        com.simplebuilding.framework.api.WornHeads.register(Simplebuilding.MOD_ID, entity ->
                entity instanceof net.minecraft.world.entity.LivingEntity living ? java.util.List.of(ExtraHead.get(living)) : java.util.List.of());
        InWorldUpgrades.registerDefaults(Simplebuilding.MOD_ID);
        // Split stage 4: SimpleBuilding reads the upgrade files of every namespace itself (with its legacy folder) and
        // syncs them; it also feeds the Enderite loot with its own switches - SimpleLib's own load/sync and its material
        // loot for mods without a partner (Simple Ovens alone) stay idle.
        InWorldUpgrades.loadedByPartner();
        SimpleLibApi.materialLootByPartner();
        // Same split as SledgehammerUpgrades.Upgrade#isChest / #isShulkerBox: every chest target moves like a tiered chest.
        SimpleLibApi.registerUpgradeHandler(block -> block instanceof ChestBlock, TieredChests::upgradeInPlace);
        SimpleLibApi.registerUpgradeHandler(block -> block instanceof TieredShulkerBoxBlock, (level, pos, to) -> {
            TieredShulkerBoxes.upgradeInPlace(level, pos, to);
            return 1;
        });
        registerHammerWays();
    }

    /** Split stage 7: SimpleBuilding's hammer ways for Simple Hammers (SimpleLib {@link HammerActions}; the engine statics through the overlay subclasses). */
    private static void registerHammerWays() {
        // Placed trim template with glow ink / glowstone in the off hand: right-click strikes it (owner 2026-10-06).
        HammerActions.registerUse(HammerActions.Stage.FIRST, context -> {
            var player = context.getPlayer();
            if (player == null || !PlacedTemplates.isHammerTarget(context.getLevel(), context.getClickedPos(), player)) return null;
            if (context.getLevel() instanceof ServerLevel server) PlacedTemplates.strike(server, context.getClickedPos(), player);
            return net.minecraft.world.InteractionResult.SUCCESS;
        });
        HammerActions.registerUse(HammerActions.Stage.PERMITTED, context -> {
            var player = context.getPlayer();
            if (player == null) return null;
            // Sneak + hammer on a pile with a flippable disc: flat display (D7); without sneaking the block flips it.
            if (player.isSecondaryUseActive() && FlatToggles.toggle(context.getLevel(), context.getClickedPos(), player,
                    context.getItemInHand(), context.getHand())) {
                return net.minecraft.world.InteractionResult.SUCCESS;
            }
            var chips = SledgehammerChips.tryCrush(context);
            return chips.consumesAction() ? chips : null;
        });
        HammerActions.registerKeep((player, state, level, pos) ->
                state.getBlock() instanceof com.simplebuilding.blocks.custom.PlacedTemplateBlock && PlacedTemplates.isHammerStance(player));
        HammerActions.registerArea(OctantHammerArea::selection);
        HammerActions.registerAreaVeto(BuildPermissions::isProbing);
        // Claims: Simple Hammers asks framework Protection itself (what WorldPermissions bridges to on 26.3), per action.
        HammerActions.registerParts(block -> block instanceof ChestBlock, TieredChests::halves);
        HammerActions.registerUpgradedState(state -> com.simplebuilding.blocks.custom.NetheriteBreakerPistonBlock.withDamage(state, 0));
        SledgehammerUpgrades.setFallbackTable(SledgehammerUpgrades::builtInTable);
        HammerActions.listen(HammerActions.RESHAPE, player -> com.simplebuilding.advancement.ModTriggers.feature(player,
                com.simplebuilding.advancement.ModTriggers.HAMMER_RESHAPE));
        HammerActions.listen(HammerActions.DIAMOND_CRUSH, player -> com.simplebuilding.advancement.ModTriggers.feature(player,
                com.simplebuilding.advancement.ModTriggers.DIAMOND_CRUSH));
        HammerActions.listen(HammerActions.UPGRADE_NETHERITE, player -> com.simplebuilding.advancement.ModTriggers.feature(player,
                com.simplebuilding.advancement.ModTriggers.HAMMER_UPGRADE_NETHERITE));
        HammerActions.listen(HammerActions.UPGRADE_ENDERITE, player -> com.simplebuilding.advancement.ModTriggers.feature(player,
                com.simplebuilding.advancement.ModTriggers.HAMMER_UPGRADE_ENDERITE));
        HammerActions.listen(HammerActions.AREA_BLOCK, player -> com.simplebuilding.advancement.ModCounters.add(player,
                com.simplebuilding.advancement.ModCounters.HAMMER_BLOCKS, 1));
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

    public static @Nullable Block octetBaseOf(@Nullable Block cell) {
        return OctetCells.baseOf(cell);
    }
}
