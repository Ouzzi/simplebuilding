package com.simplebuilding.client.property;

import com.mojang.serialization.MapCodec;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.util.TransformTargets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * {@code simplebuilding:transform_hint} (2026-10-03): true while this stack sits in a hand of the local
 * player and a right click with that hand would transform the aimed block - the same question the 26.3
 * hand hint asks ({@link TransformTargets#canTransformTarget}). The 26.3 Rotator uses it for its turning
 * animation ({@code assets/simplebuilding/items/rotator.json}); its click is instant, so vanilla's
 * {@code minecraft:using_item} is never true for it. Read-only, client side, no gameplay effect.
 *
 * <p>Registered like {@link GaugeNeedleModelProperty}: Fabric directly on the {@code ID_MAPPER} (also
 * for datagen), NeoForge through {@code RegisterConditionalItemModelPropertyEvent}, Forge by mixin.
 */
public record TransformHintModelProperty() implements ConditionalItemModelProperty {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "transform_hint");

    public static final MapCodec<TransformHintModelProperty> CODEC = MapCodec.unit(new TransformHintModelProperty());

    @Override
    public boolean get(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed, ItemDisplayContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (level == null || !(entity instanceof Player player) || player != minecraft.player
                || !(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != BlockHitResult.Type.BLOCK) {
            return false;
        }
        InteractionHand hand = player.getMainHandItem() == stack ? InteractionHand.MAIN_HAND
                : player.getOffhandItem() == stack ? InteractionHand.OFF_HAND : null;
        return hand != null && TransformTargets.canTransformTarget(level, hit, player, hand);
    }

    @Override
    public MapCodec<TransformHintModelProperty> type() {
        return CODEC;
    }
}
