package com.simplebuilding.modules.simpletools;

import com.simplebuilding.modules.simpletools.mixin.ItemFrameFixedAccessor;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;

/**
 * Better item frames (docs/ai/PLAN-B-ITEMFRAMES-2026-10-19.md), item frames and glow item frames alike. Only while
 * sneaking and only on a frame holding an item: shears hide/show the frame, honeycomb or a glass pane locks it, an axe unlocks it,
 * anything else turns the item backwards. State lives in the Vanilla fields only ({@code Invisible}, {@code Fixed}).
 */
public final class ItemFrames {
    private ItemFrames() {}

    /** Called at the head of {@link ItemFrame#interact}; null keeps Vanilla (and Simple Maps' unsneaked map click). */
    public static InteractionResult interact(ItemFrame frame, Player player, InteractionHand hand) {
        if (player.level().isClientSide() || !player.isSecondaryUseActive() || frame.getItem().isEmpty() || frame.isRemoved()) return null;
        if (!SimpleTools.active()) return null;
        ToolsConfig config = SimpleTools.config();
        ItemStack held = player.getItemInHand(hand);
        boolean fixed = fixed(frame);
        boolean axe = held.is(ItemTags.AXES);
        if (fixed) {
            if (!config.enableItemFrameLock || !axe || !InteractionGuard.allow(player, frame.getPos())) return null;
            setFixed(frame, false);
            held.hurtAndBreak(1, player, hand);
            feedback(frame, player, SoundEvents.AXE_WAX_OFF.value(), ParticleTypes.WAX_OFF);
            return InteractionResult.SUCCESS;
        }
        if (config.enableItemFrameInvisibility && held.getItem() instanceof ShearsItem) {
            if (!InteractionGuard.allow(player, frame.getPos())) return null;
            frame.setInvisible(!frame.isInvisible());
            held.hurtAndBreak(1, player, hand);
            feedback(frame, player, SoundEvents.SHEARS_SNIP, null);
            return InteractionResult.SUCCESS;
        }
        if (config.enableItemFrameLock && held.is(Items.HONEYCOMB)) {
            if (!InteractionGuard.allow(player, frame.getPos())) return null;
            setFixed(frame, true);
            held.consume(1, player);
            feedback(frame, player, SoundEvents.HONEYCOMB_WAX_ON, ParticleTypes.WAX_ON);
            return InteractionResult.SUCCESS;
        }
        // SimpleBuilding's old habit: a glass pane locks too (it used to be its own state, now the same Fixed flag)
        if (config.enableItemFrameLock && held.is(Items.GLASS_PANE)) {
            if (!InteractionGuard.allow(player, frame.getPos())) return null;
            setFixed(frame, true);
            held.consume(1, player);
            feedback(frame, player, SoundEvents.GLASS_PLACE, null);
            return InteractionResult.SUCCESS;
        }
        if (config.enableItemFrameReverseRotate) {
            frame.setRotation(frame.getRotation() + ItemFrame.NUM_ROTATIONS - 1);
            feedback(frame, player, frame.getRotateItemSound(), null);
            return InteractionResult.SUCCESS;
        }
        return null;
    }

    /** Called after the frame dropped its item: an invisible frame without an item would be lost, so it shows again. */
    public static void afterDrop(ItemFrame frame) {
        if (frame.isInvisible() && frame.getItem().isEmpty() && !fixed(frame) && SimpleTools.active() && SimpleTools.config().enableItemFrameInvisibility) {
            frame.setInvisible(false);
        }
    }

    /** Frames saved by SimpleBuilding's own lock ({@code SimpleBuildingLocked}) become Vanilla Fixed; SimpleBuilding stops writing the tag. */
    public static void migrate(ItemFrame frame, ValueInput input) {
        if (input.getBooleanOr("SimpleBuildingLocked", false)) setFixed(frame, true);
    }

    public static boolean fixed(ItemFrame frame) {
        return ((ItemFrameFixedAccessor) frame).simpletools$fixed();
    }

    public static void setFixed(ItemFrame frame, boolean fixed) {
        ((ItemFrameFixedAccessor) frame).simpletools$setFixed(fixed);
    }

    private static void feedback(ItemFrame frame, Player player, SoundEvent sound, ParticleOptions particle) {
        frame.playSound(sound, 1.0F, 1.0F);
        frame.gameEvent(GameEvent.BLOCK_CHANGE, player);
        if (particle != null && frame.level() instanceof ServerLevel level) {
            level.sendParticles(particle, frame.getX(), frame.getY(), frame.getZ(), 6, 0.25, 0.25, 0.25, 0.0);
        }
    }
}
