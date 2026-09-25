package com.simplebuilding.version;

import net.minecraft.advancements.predicates.TagPredicate;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.equipment.trim.MaterialAssetGroup;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.material.PushReaction;
import org.jspecify.annotations.Nullable;

import java.util.stream.Stream;

/**
 * Minecraft 26.2 side of the version shim (server-safe part).
 *
 * <p>The shared tree common/src/shared/java is compiled against 26.2 AND 26.3. Where the two
 * Minecraft versions differ in a small API detail, shared code calls this class instead; there is
 * exactly one copy per Minecraft line: this one in common/src/mc26_2/java (compiled only by the
 * 26.2 modules) and its twin in mc26_3/overlay/java (compiled only by the 26.3 modules). Both must
 * keep the same public signatures - checkOverlays in the root build.gradle verifies that every
 * overlay class has a counterpart on the other line.
 */
public final class McVersion {

    private McVersion() {
    }

    /** Piston reactions under their 26.2 names (26.3: IMMOVEABLE / POPPED / PUSH). */
    public static final PushReaction PUSH_BLOCKED = PushReaction.BLOCK;
    public static final PushReaction PUSH_DESTROYS = PushReaction.DESTROY;
    public static final PushReaction PUSH_ONLY = PushReaction.PUSH_ONLY;

    /**
     * A block's piston push reaction as the 26.2 rules see it. 26.3 folded vanilla's hard-coded
     * "never push" list (obsidian, crying obsidian, respawn anchor, reinforced deepslate) into the
     * IMMOVEABLE reaction; the mod's pistons treat that list separately, so they ask this instead of
     * {@code getPistonPushReaction()}.
     */
    public static PushReaction pushReaction(net.minecraft.world.level.block.state.BlockState state) {
        return state.getPistonPushReaction();
    }

    /** Explorer-map structure tags (26.3 renamed them after the structure). */
    public static final TagKey<Structure> MANSION_MAP_STRUCTURES = StructureTags.ON_WOODLAND_EXPLORER_MAPS;
    public static final TagKey<Structure> MONUMENT_MAP_STRUCTURES = StructureTags.ON_OCEAN_EXPLORER_MAPS;
    public static final TagKey<Structure> TRIAL_CHAMBERS_MAP_STRUCTURES = StructureTags.ON_TRIAL_CHAMBERS_MAPS;

    /**
     * Drops a stack from a player. {@code predictedByClient} only matters on 26.3, where a drop
     * also swings the arm and the flag decides whether the dropping player is told about it.
     */
    public static net.minecraft.world.entity.item.@Nullable ItemEntity drop(Player player, ItemStack stack,
                                                                            boolean thrownFromHand,
                                                                            boolean predictedByClient) {
        return player.drop(stack, thrownFromHand);
    }

    /** Swings the arm; {@code sendToSwingingEntity} as in vanilla. */
    public static void swing(LivingEntity entity, InteractionHand hand, boolean sendToSwingingEntity) {
        entity.swing(hand, sendToSwingingEntity);
    }

    /** The per-entity "Invulnerable" flag (26.3: setPermanentlyInvulnerable). */
    public static void setInvulnerable(Entity entity, boolean invulnerable) {
        entity.setInvulnerable(invulnerable);
    }

    /** Clears the post-damage invulnerability window (26.3 made the field private). */
    public static void resetInvulnerableTime(Entity entity) {
        entity.invulnerableTime = 0;
    }

    /** {@code LivingEntity#getVisibilityPercent} (26.3 added a ServerLevel parameter). */
    public static double visibilityPercent(LivingEntity entity, @Nullable Entity targeting) {
        return entity.getVisibilityPercent(targeting);
    }

    /** Copies of the stacks in a bundle (26.3: itemCopies). */
    public static Stream<ItemStack> bundleItemCopies(BundleContents contents) {
        return contents.itemCopyStream();
    }

    /** An empty, mutable bundle content (26.3 dropped the copy constructor). */
    public static BundleContents.Mutable emptyBundleMutable() {
        return new BundleContents.Mutable(BundleContents.EMPTY);
    }

    /** The trades a trade set draws from (26.3 turned TradeSet into a record). */
    public static HolderSet<VillagerTrade> tradeSetTrades(TradeSet tradeSet) {
        return tradeSet.getTrades();
    }

    /** A tag predicate built during datagen/bootstrap (26.3 resolves the tag through a lookup). */
    public static <T> TagPredicate<T> tagPredicate(BootstrapContext<?> context,
                                                   ResourceKey<? extends Registry<T>> registry, TagKey<T> tag) {
        return TagPredicate.is(tag);
    }

    /** A trim material for a mod palette (26.2: asset group; 26.3: palette id). */
    public static TrimMaterial trimMaterial(String paletteName, Component description) {
        return new TrimMaterial(MaterialAssetGroup.create(paletteName), description);
    }

    /**
     * The palette suffix a trim material shows on armour of the given equipment asset, e.g. "iron"
     * or "iron_darker" (iron trim on iron armour). 26.2: the material's asset group decides.
     */
    public static String trimColourSuffix(TrimMaterial material,
                                          ResourceKey<net.minecraft.world.item.equipment.EquipmentAsset> asset) {
        return material.assets().assetId(asset).suffix();
    }

    /** Gametest metadata (26.3 added the test dimension; the 26.2 tests run in the overworld). */
    public static <E> net.minecraft.gametest.framework.TestData<E> testData(
            E environment, net.minecraft.resources.Identifier structure, int maxTicks, int setupTicks, boolean required,
            net.minecraft.world.level.block.Rotation rotation, boolean manualOnly, int maxAttempts, int requiredSuccesses,
            boolean skyAccess, int padding) {
        return new net.minecraft.gametest.framework.TestData<>(environment, structure, maxTicks, setupTicks, required,
                rotation, manualOnly, maxAttempts, requiredSuccesses, skyAccess, padding);
    }

    /** Block properties that never block the view (26.3 added a near-plane box parameter). */
    public static BlockBehaviour.Properties neverViewBlocking(BlockBehaviour.Properties properties) {
        return properties.isViewBlocking((state, level, pos) -> false);
    }
}
