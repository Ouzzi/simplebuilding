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

    public static void resetInvulnerableTime(Entity entity) {
        entity.setInvulnerableTime(0);
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

    public static BlockBehaviour.Properties neverViewBlocking(BlockBehaviour.Properties properties) {
        return properties.isViewBlocking((state, level, pos, nearPlane) -> false);
    }
}
