package com.simplebuilding.version;

import net.minecraft.advancements.predicates.TagPredicate;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.material.PushReaction;
import org.jspecify.annotations.Nullable;

import java.util.stream.Stream;

/**
 * Minecraft 26.3 side of the version shim (server-safe part). See the 26.2 twin in
 * common/src/mc26_2/java for the contract; both must keep the same public signatures.
 */
public final class McVersion {

    private McVersion() {
    }

    public static final PushReaction PUSH_BLOCKED = PushReaction.IMMOVEABLE;
    public static final PushReaction PUSH_DESTROYS = PushReaction.POPPED;
    public static final PushReaction PUSH_ONLY = PushReaction.PUSH;

    /** 26.2 semantics: the former name list counts as a normal block again (see the 26.2 twin). */
    public static PushReaction pushReaction(net.minecraft.world.level.block.state.BlockState state) {
        if (state.is(net.minecraft.world.level.block.Blocks.OBSIDIAN)
                || state.is(net.minecraft.world.level.block.Blocks.CRYING_OBSIDIAN)
                || state.is(net.minecraft.world.level.block.Blocks.RESPAWN_ANCHOR)
                || state.is(net.minecraft.world.level.block.Blocks.REINFORCED_DEEPSLATE)) {
            return PushReaction.PUSH_PULL;
        }
        return state.getPistonPushReaction();
    }

    public static final TagKey<Structure> MANSION_MAP_STRUCTURES = StructureTags.ON_WOODLAND_MANSION_MAPS;
    public static final TagKey<Structure> MONUMENT_MAP_STRUCTURES = StructureTags.ON_OCEAN_MONUMENT_MAPS;
    public static final TagKey<Structure> TRIAL_CHAMBERS_MAP_STRUCTURES = StructureTags.ON_BURIED_TRIAL_CHAMBERS_MAPS;

    public static net.minecraft.world.entity.item.@Nullable ItemEntity drop(Player player, ItemStack stack,
                                                                            boolean thrownFromHand,
                                                                            boolean predictedByClient) {
        return player.drop(stack, thrownFromHand, predictedByClient ? Prediction.PREDICTED : Prediction.SERVER_ONLY);
    }

    public static void swing(LivingEntity entity, InteractionHand hand, boolean sendToSwingingEntity) {
        entity.swing(hand, SwingAnimation.DEFAULT, sendToSwingingEntity);
    }

    public static void setInvulnerable(Entity entity, boolean invulnerable) {
        entity.setPermanentlyInvulnerable(invulnerable);
    }

    /** 26.3 split the damage cooldown (LivingEntity#damageCooldownTime) from Entity#invulnerableTime. */
    public static void resetInvulnerableTime(Entity entity) {
        entity.setInvulnerableTime(0);
        if (entity instanceof LivingEntity living) {
            living.damageCooldownTime = 0;
        }
    }

    public static double visibilityPercent(LivingEntity entity, @Nullable Entity targeting) {
        return entity.getVisibilityPercent((ServerLevel) entity.level(), targeting);
    }

    public static Stream<ItemStack> bundleItemCopies(BundleContents contents) {
        return contents.itemCopies();
    }

    public static BundleContents.Mutable emptyBundleMutable() {
        return new BundleContents.Mutable();
    }

    public static HolderSet<VillagerTrade> tradeSetTrades(TradeSet tradeSet) {
        return tradeSet.trades();
    }

    public static <T> TagPredicate<T> tagPredicate(BootstrapContext<?> context,
                                                   ResourceKey<? extends Registry<T>> registry, TagKey<T> tag) {
        return TagPredicate.is(context.lookup(registry), tag);
    }

    /**
     * 26.3 trims name a palette texture instead of an asset group:
     * assets/simplebuilding/textures/palettes/trim/&lt;name&gt;.png.
     */
    public static TrimMaterial trimMaterial(String paletteName, Component description) {
        return new TrimMaterial(Identifier.fromNamespaceAndPath("simplebuilding", "trim/" + paletteName), description);
    }

    /** Vanilla equipment assets whose own trim material shows the "_darker" palette (26.3 trim_overrides). */
    private static final java.util.Set<String> DARKER_ON_OWN_MATERIAL =
            java.util.Set.of("iron", "gold", "diamond", "netherite", "copper");

    /**
     * 26.3: the material names a palette (trim/<suffix>); the "_darker" variant comes from the
     * equipment asset's trim_overrides, which vanilla sets for exactly the five materials above on
     * their own armour - the same pairs 26.2's MaterialAssetGroup overrides.
     */
    public static String trimColourSuffix(TrimMaterial material,
                                          ResourceKey<net.minecraft.world.item.equipment.EquipmentAsset> asset) {
        String path = material.paletteId().getPath();
        String suffix = path.startsWith("trim/") ? path.substring("trim/".length()) : path;
        Identifier armour = asset.identifier();
        if (armour.getNamespace().equals("minecraft") && material.paletteId().getNamespace().equals("minecraft")
                && suffix.equals(armour.getPath()) && DARKER_ON_OWN_MATERIAL.contains(suffix)) {
            return suffix + "_darker";
        }
        return suffix;
    }

    public static <E> net.minecraft.gametest.framework.TestData<E> testData(
            E environment, Identifier structure, int maxTicks, int setupTicks, boolean required,
            net.minecraft.world.level.block.Rotation rotation, boolean manualOnly, int maxAttempts, int requiredSuccesses,
            boolean skyAccess, int padding) {
        return new net.minecraft.gametest.framework.TestData<>(environment, net.minecraft.world.level.Level.OVERWORLD,
                structure, maxTicks, setupTicks, required, rotation, manualOnly, maxAttempts, requiredSuccesses,
                skyAccess, padding);
    }

    public static BlockBehaviour.Properties neverViewBlocking(BlockBehaviour.Properties properties) {
        return properties.isViewBlocking((state, level, pos, nearPlane) -> false);
    }
}
