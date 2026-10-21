package com.simplebuilding.crucible;

import com.simplecrucibles.api.SimpleCruciblesApi;
import com.simplelib.api.SimpleLibApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;

/**
 * 26.3: SimpleBuilding's bridge to the crucibles (split stage 5: they live in the required, bundled sub-mod Simple
 * Crucibles, {@code simplecrucibles:*}, ids migrate through SimpleLib's id moves). The only place in SimpleBuilding that
 * imports the module - and only its API (principle 6a). The 26.2 twin in common/src/mc26_2/java is empty
 * ({@code McVersion.CRUCIBLE} is false there).
 * <ul>
 *   <li>The Enderite crucible and barrel are the module's blocks, found by id.</li>
 *   <li>Sledgehammer ways (principle 5a): since split stage 7 building the iron crucible (2 durability per strike),
 *       attaching a barrel and the strike-by-strike crucible upgrade are Simple Crucibles' own hammer ways (SimpleLib
 *       {@code HammerActions}, run by Simple Hammers); SimpleBuilding only listens for the built crucible (advancement).</li>
 *   <li>Soul lava, its buckets and the copper/Enderite/ceramic buckets are the module's (split stage 5b); SimpleBuilding
 *       only listens for scooping (advancement) and lends the buckets to its wooden cauldron ({@link #cauldronBucket}).</li>
 * </ul>
 */
public final class CrucibleCompat {
    /** Durability per build / attach strike of the hammer (Simple Crucibles' hammer ways since split stage 7). */
    public static final int HAMMER_DAMAGE_PER_STRIKE = 2;

    /** Namespace of the crucibles (the module). */
    public static final String NAMESPACE = "simplecrucibles";

    /**
     * After registration: plug the sledgehammer into Simple Crucibles (axe ways: LibServices.hammersReplaceAxes) and
     * listen for soul lava scooping (advancement "Soul Scooper"). Soul lava and the buckets are the module's own since
     * split stage 5b - it handles them on the reinforced cauldron itself.
     */
    public static void init() {
        // Split stage 7: building, attaching and the menu hand-over with the hammer are Simple Crucibles' own hammer ways
        // (SimpleLib HammerActions); SimpleBuilding only keeps its advancement.
        SimpleCruciblesApi.onCrucibleBuilt(player ->
                com.simplebuilding.advancement.ModTriggers.feature(player, com.simplebuilding.advancement.ModTriggers.CRUCIBLE_BUILT));
        SimpleCruciblesApi.onSoulLavaScooped(player ->
                com.simplebuilding.advancement.ModTriggers.feature(player, com.simplebuilding.advancement.ModTriggers.SOUL_LAVA_SCOOPED));
        // LOHEN17 V3 (owner 2026-10-21): blazing obsidian under a crucible gives high heat, like lava (config blazingHeat).
        SimpleCruciblesApi.registerHeatSource("high", state -> com.simplebuilding.blocks.ModBlocks.BLAZING_OBSIDIAN != null
                && state.is(com.simplebuilding.blocks.ModBlocks.BLAZING_OBSIDIAN)
                && com.simplebuilding.config.ServerTuning.get().features.blazingHeat);
    }

    /**
     * Simple Crucibles' copper, Enderite and ceramic buckets on SimpleBuilding's wooden cauldron, whose state is mapped to
     * the matching Vanilla cauldron ({@code WoodenCauldronBlock#toVanilla}). Null when {@code stack} is no such bucket case.
     */
    public static @Nullable InteractionResult cauldronBucket(ItemStack stack, BlockState vanillaState, Level level, BlockPos pos,
                                                             Player player, InteractionHand hand) {
        return SimpleCruciblesApi.cauldronBucket(stack, vanillaState, level, pos, player, hand);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(NAMESPACE, path);
    }

    public static @Nullable Block enderiteCrucible() {
        return BuiltInRegistries.BLOCK.getOptional(id("enderite_crucible")).orElse(null);
    }

    public static @Nullable Block enderiteBarrel() {
        return BuiltInRegistries.BLOCK.getOptional(id("enderite_barrel")).orElse(null);
    }

    public static @Nullable Block reinforcedCauldron() {
        return SimpleCruciblesApi.reinforcedCauldron();
    }

    public static BlockState reinforcedCauldron(String content) {
        return SimpleCruciblesApi.reinforcedCauldron(content);
    }

    public static @Nullable String cauldronContent(BlockState state) {
        return SimpleCruciblesApi.cauldronContent(state);
    }

    public static String heatAt(Level level, BlockPos crucible) {
        return SimpleCruciblesApi.heatAt(level, crucible);
    }

    public static int crucibleSlots(Block block) {
        return SimpleCruciblesApi.crucibleSlots(block);
    }

    public static int[] status(net.minecraft.world.level.block.entity.BlockEntity entity) {
        return SimpleCruciblesApi.crucibleStatus(entity);
    }

    public static int requiredHeat(ItemStack input, net.minecraft.world.item.crafting.RecipeType<?> type) {
        return SimpleCruciblesApi.requiredHeat(input, type);
    }

    public static int cookingTicks(int baseTicks, int heat) {
        return SimpleCruciblesApi.crucibleCookingTicks(baseTicks, heat);
    }

    public static int warmingTicks() { return SimpleLibApi.warmingTicks(); }

    public static boolean warmable(ItemStack input) { return SimpleLibApi.warmable(input); }

    public static int stackMultiplier(Block block) {
        return SimpleCruciblesApi.stackMultiplier(block);
    }

    public static boolean isAttached(BlockState state) {
        return SimpleCruciblesApi.isAttached(state);
    }

    public static boolean upgradesInPlace(Block block) {
        return SimpleCruciblesApi.upgradesInPlace(block);
    }

    public static void upgradeInPlace(ServerLevel level, BlockPos pos, Block to) {
        SimpleCruciblesApi.upgradeCrucible(level, pos, to);
    }

    /** Crucible upgrades go strike by strike (owner N51): 12 strikes, one unit every 3. */
    public static boolean stagedUpgrade(Block block) {
        return SimpleCruciblesApi.stagedUpgrade(block);
    }

    public static int crucibleUpgradeStrikes() { return SimpleCruciblesApi.crucibleUpgradeStrikes(); }

    public static int crucibleUpgradeUnits(BlockState state) { return SimpleCruciblesApi.crucibleUpgradeUnits(state); }

    /** Blow {@code done} of a crucible upgrade: SimpleLib's preview, a unit every 3 blows, the last one upgrades in place. */
    public static void crucibleUpgradeStrike(ServerLevel level, BlockPos pos, Block to, int done, ItemStack material) {
        SimpleCruciblesApi.crucibleUpgradeStrike(level, pos, to, done, material);
    }

    public static boolean axeWaysEnabled() {
        return SimpleLibApi.axeWaysEnabled();
    }

    /** Strikes to build the iron crucible / to attach a barrel (JEI, wiki). */
    public static int buildStrikes() { return SimpleCruciblesApi.buildStrikes(); }

    public static int attachStrikes() { return SimpleCruciblesApi.attachStrikes(); }

    public static int buildWallStrikes() { return SimpleCruciblesApi.buildWallStrikes(); }

    public static String attachedBarrelSlots() { return SimpleCruciblesApi.attachedBarrelSlots(); }

    private CrucibleCompat() {}
}
