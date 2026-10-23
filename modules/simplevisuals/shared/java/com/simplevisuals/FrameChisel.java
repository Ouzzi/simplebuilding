package com.simplevisuals;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * W23-5 (docs/ai/PLAN-A-UI23B-2026-10-23.md): sneaking with a chisel on an item frame that holds a flat block or pane
 * ({@link FrameTilt}) "chisels" the frame: the item tilts one step further ({@link FrameTilt#nextStep}, 0 to 90 degrees
 * in 15-degree steps, then flat again). The step is a synced entity datum of the frame (saved with it), so every player
 * sees the same angle. Chisels are the item tag {@code simplevisuals:frame_chisels} (SimpleBuilding's chisels when it is
 * loaded, empty otherwise). Option visuals.enableTiltedFrameItems, like the flat view itself.
 */
public final class FrameChisel {
    public static final TagKey<Item> CHISELS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Visuals.MOD_ID, "frame_chisels"));

    private FrameChisel() {}

    /** Called at the head of {@link ItemFrame#interact}; null keeps Vanilla and the other frame actions. */
    public static InteractionResult interact(ItemFrame frame, Player player, InteractionHand hand) {
        if (!player.isSecondaryUseActive() || frame.isRemoved()) return null;
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(CHISELS) || FrameTilt.kind(frame.getItem()) == FrameTilt.Kind.VANILLA) return null;
        if (!Visuals.active(Visuals.CONFIG.visuals.enableTiltedFrameItems)) return null;
        ChiseledFrame chiseled = (ChiseledFrame) frame;
        if (chiseled.simplevisuals$fixed() || !player.mayBuild()) return null;
        if (player.level().isClientSide()) return InteractionResult.SUCCESS; // the server changes the step and syncs it
        int step = FrameTilt.nextStep(chiseled.simplevisuals$tiltStep());
        chiseled.simplevisuals$setTiltStep(step);
        frame.playSound(SoundEvents.UI_STONECUTTER_TAKE_RESULT, 0.6F, 0.85F + step * 0.06F);
        frame.gameEvent(GameEvent.BLOCK_CHANGE, player);
        return InteractionResult.SUCCESS;
    }
}
