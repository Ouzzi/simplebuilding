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
 *   <li>Sledgehammer ways (principle 5a): building the iron crucible (2 durability per strike), attaching a
 *       barrel (6 strikes); upgrades run through the sledgehammer upgrade table
 *       ({@code SledgehammerUpgrades}). The Vanilla axe ways are switched off.</li>
 *   <li>Reinforced cauldron: soul lava as its extreme content, copper/enderite buckets.</li>
 * </ul>
 */
public final class CrucibleCompat {
    public static final int HAMMER_DAMAGE_PER_STRIKE = 2;

    /** Namespace of the crucibles (the module). */
    public static final String NAMESPACE = "simplecrucibles";

    /** After registration: plug the sledgehammer and the buckets into Simple Crucibles (axe ways: LibServices.hammersReplaceAxes). */
    public static void init() {
        SimpleLibApi.registerToolUse(CrucibleCompat::hammerWants);
        SimpleCruciblesApi.registerCauldronBucket(new SbCauldronBuckets());
        SimpleCruciblesApi.setExtremeCauldronEffect(CrucibleCompat::soulLavaInside);
    }

    private static void soulLavaInside(Level level, Entity entity) {
        com.simplebuilding.fluid.SoulLava.touch(level, entity);
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

    /** A sledgehammer click on a crucible/barrel reaches the hammer instead of the menu when it upgrades, builds or attaches. */
    private static boolean hammerWants(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !SimpleLibApi.isHammer(player.getMainHandItem())) return false;
        if (com.simplebuilding.util.SledgehammerUpgrades.shouldSkipBlockUse(state, level, pos, player, hand)) return true;
        return SimpleCruciblesApi.canAttach(level, pos, state);
    }

    /**
     * The sledgehammer's in-world crucible ways: build the iron crucible on an iron block (off hand:
     * walls, then handles) and attach a barrel to a crucible. Null when neither applies.
     */
    public static @Nullable InteractionResult hammerUse(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || context.getHand() != InteractionHand.MAIN_HAND) return null;
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        ItemStack tool = context.getItemInHand();
        if (SimpleCruciblesApi.canAttach(level, pos, state)) {
            return SimpleCruciblesApi.attachStrike(level, pos, player, tool, HAMMER_DAMAGE_PER_STRIKE) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }
        if (SimpleCruciblesApi.canBuildStrike(state, player.getOffhandItem())) {
            if (!com.simplebuilding.api.WorldPermissions.mayAct(level, player, pos)) return InteractionResult.FAIL;
            boolean hit = SimpleCruciblesApi.buildStrike(level, pos, player, tool, HAMMER_DAMAGE_PER_STRIKE);
            if (hit && player instanceof net.minecraft.server.level.ServerPlayer server
                    && level.getBlockState(pos).getBlock() == BuiltInRegistries.BLOCK.getValue(id("iron_crucible"))) {
                com.simplebuilding.advancement.ModTriggers.feature(server, com.simplebuilding.advancement.ModTriggers.CRUCIBLE_BUILT);
            }
            return hit ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }
        return null;
    }

    /**
     * SimpleBuilding's containers for the reinforced cauldron: soul lava is its "extreme" content (iron bucket
     * breaks when pouring it, the Enderite bucket comes back), copper and Enderite buckets take and pour water
     * and lava like everywhere else (copper breaks on lava, cannot take soul lava).
     */
    private static final class SbCauldronBuckets implements SimpleCruciblesApi.CauldronBucket {
        @Override
        public @Nullable String pours(ItemStack held) {
            if (!(held.getItem() instanceof com.simplebuilding.fluid.ModBucketItem bucket)) return null;
            net.minecraft.world.level.material.Fluid content = bucket.getContent();
            if (content.isSame(com.simplebuilding.fluid.ModFluids.SOUL_LAVA)) return "extreme";
            if (content.isSame(net.minecraft.world.level.material.Fluids.LAVA)) return "lava";
            if (content.isSame(net.minecraft.world.level.material.Fluids.WATER)) return "water";
            return null;
        }

        @Override
        public ItemStack afterPour(ItemStack held) {
            return held.getItem() instanceof com.simplebuilding.fluid.ModBucketItem bucket ? bucket.afterPour(held) : ItemStack.EMPTY;
        }

        @Override
        public @Nullable ItemStack take(ItemStack held, String content) {
            // Owner N21: a half Enderite bucket takes one more bucket of its own fluid and becomes full.
            Item fuller = com.simplebuilding.fluid.ModFluids.fullEnderite(held.getItem());
            if (fuller != null) return content.equals(pours(held)) ? com.simplebuilding.fluid.ModBucketItem.fill(held, fuller) : null;
            com.simplebuilding.fluid.ModBucketItem.Kind kind;
            if (held.is(net.minecraft.world.item.Items.BUCKET)) kind = com.simplebuilding.fluid.ModBucketItem.Kind.IRON;
            else if (held.getItem() instanceof com.simplebuilding.fluid.ModBucketItem bucket
                    && bucket.getContent() == net.minecraft.world.level.material.Fluids.EMPTY
                    && com.simplebuilding.fluid.ModBucketItem.canScoop(held)) kind = bucket.kind();
            else return null;
            net.minecraft.world.level.material.Fluid fluid = switch (content) {
                case "extreme" -> com.simplebuilding.fluid.ModFluids.SOUL_LAVA;
                case "lava" -> kind == com.simplebuilding.fluid.ModBucketItem.Kind.IRON ? null : net.minecraft.world.level.material.Fluids.LAVA;
                case "water" -> kind == com.simplebuilding.fluid.ModBucketItem.Kind.IRON ? null : net.minecraft.world.level.material.Fluids.WATER;
                default -> null;
            };
            if (fluid == null) return null;
            Item filled = com.simplebuilding.fluid.ModBucketItem.filled(kind, fluid);
            return filled == null ? null : com.simplebuilding.fluid.ModBucketItem.fill(held, filled);
        }
    }

    private CrucibleCompat() {}
}
