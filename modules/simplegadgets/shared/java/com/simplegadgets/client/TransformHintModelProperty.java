package com.simplegadgets.client;

import com.mojang.serialization.MapCodec;
import com.simplegadgets.SimpleGadgets;
import com.simplegadgets.GadgetProtection;
import com.simplegadgets.rotator.RotatorItem;
import com.simplelib.api.GadgetActions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * {@code simplegadgets:transform_hint} (moved from SimpleBuilding's {@code simplebuilding:transform_hint}, OW24-2 G2): true while
 * this stack sits in a hand of the local player and a right click with that hand would transform the aimed block. The
 * Transposer uses it for its turning animation ({@code assets/simplegadgets/items/rotator.json}); its turn is instant, so
 * Vanilla's {@code minecraft:using_item} is only true during the swap wind-up - this property covers that too. With SimpleBuilding
 * its full hand-hint question answers (SimpleLib {@link GadgetActions#handHint}: cooldown, permission, the block's own click,
 * the other hand); alone the Transposer's own check does. Read-only, client side, no gameplay effect.
 *
 * <p>Fabric registers it directly on the {@code ID_MAPPER} ({@code GadgetsFabricClient}).
 */
public record TransformHintModelProperty() implements ConditionalItemModelProperty {

    public static final Identifier ID = SimpleGadgets.id("transform_hint");

    public static final MapCodec<TransformHintModelProperty> CODEC = MapCodec.unit(new TransformHintModelProperty());

    @Override
    public boolean get(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed, ItemDisplayContext context) {
        // Wind-up of a held click (the swap): the spinning model runs while the stack is being used.
        if (entity != null && entity.isUsingItem() && entity.getUseItem() == stack) {
            return true;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (level == null || !(entity instanceof Player player) || player != minecraft.player
                || !(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != BlockHitResult.Type.BLOCK) {
            return false;
        }
        InteractionHand hand = player.getMainHandItem() == stack ? InteractionHand.MAIN_HAND
                : player.getOffhandItem() == stack ? InteractionHand.OFF_HAND : null;
        if (hand == null) {
            return false;
        }
        Boolean partner = GadgetActions.handHint(level, hit, player, hand);
        if (partner != null) {
            return partner;
        }
        return stack.getItem() instanceof RotatorItem rotator && !player.getCooldowns().isOnCooldown(stack)
                && GadgetProtection.mayTransform(level, player, hit.getBlockPos(), hit.getDirection(), stack)
                && rotator.canTransformTarget(new UseOnContext(player, hand, hit));
    }

    @Override
    public MapCodec<TransformHintModelProperty> type() {
        return CODEC;
    }
}
