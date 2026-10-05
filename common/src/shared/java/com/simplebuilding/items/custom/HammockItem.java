package com.simplebuilding.items.custom;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.custom.HammockLayout;
import java.util.Map;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jspecify.annotations.Nullable;

/**
 * Hangs a hammock (docs/ai/PLAN-HAENGEMATTE-2026-10-02.md, {@link HammockLayout}).
 * <ul>
 *   <li>One click: on the side of an anchor (or the floor) a straight hammock hangs at once if a second anchor 2 to 4
 *       free blocks away fits.</li>
 *   <li>Two clicks: otherwise (or always while sneaking) a click on an anchor remembers it (custom data on the item,
 *       green sparks at the anchor for the holder only); a click on the second anchor hangs a hammock at any angle
 *       (2 to 4 free cells along the main axis, same height, {@link HammockLayout#between}). The first anchor is
 *       forgotten when it is clicked again, the item leaves the main hand, after 30 s, in another dimension or when
 *       the anchor is gone.</li>
 * </ul>
 * Refusals are the fail sound for the player only (no text).
 */
public class HammockItem extends BlockItem {
    public static final String ANCHOR_KEY = "simplebuilding_hammock_anchor";
    public static final int ANCHOR_TICKS = 600;

    public HammockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockPos hit = context.getClickedPos();
        Optional<BlockPos> first = storedAnchor(stack, level);
        if (first.isPresent()) {
            if (first.get().equals(hit)) {
                clearAnchor(stack);
                return InteractionResult.SUCCESS;
            }
            Optional<HammockLayout.Spot> spot = HammockLayout.isAnchor(level, hit) ? HammockLayout.between(first.get(), hit)
                    .filter(s -> HammockLayout.fits(level, s)) : Optional.empty();
            if (spot.isPresent() && hang(level, player, stack, spot.get())) {
                clearAnchor(stack);
                return InteractionResult.SUCCESS;
            }
            return refuse(player, hit);
        }
        BlockPlaceContext place = new BlockPlaceContext(context);
        Optional<HammockLayout.Spot> straight = HammockLayout.find(level, place.getClickedPos(), context.getClickedFace(),
                context.getHorizontalDirection());
        boolean sneaking = player != null && player.isSecondaryUseActive();
        if (!sneaking && straight.isPresent() && hang(level, player, stack, straight.get())) {
            return InteractionResult.SUCCESS;
        }
        if (HammockLayout.isAnchor(level, hit)) {
            if (level instanceof ServerLevel server) {
                rememberAnchor(stack, server, hit);
                if (player instanceof ServerPlayer serverPlayer) {
                    sparks(server, serverPlayer, hit);
                    com.simplebuilding.util.Feedback.playTo(serverPlayer, SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 0.5F, 1.4F);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return refuse(player, place.getClickedPos());
    }

    private static InteractionResult refuse(@Nullable Player player, BlockPos at) {
        if (player instanceof ServerPlayer serverPlayer) {
            HammockLayout.refuse(serverPlayer, at);
        }
        return InteractionResult.FAIL;
    }

    /** Places the hammock at {@code spot}; false (nothing placed) if somebody stands in the way or may not build. */
    private boolean hang(Level level, @Nullable Player player, ItemStack stack, HammockLayout.Spot spot) {
        Map<BlockPos, BlockState> states = HammockLayout.states(getBlock(), ModBlocks.HAMMOCK_ROPE, spot);
        if (!HammockLayout.unobstructed(level, states) || player != null && !player.mayBuild()) {
            return false;
        }
        if (!level.isClientSide()) {
            HammockLayout.hang(level, spot, states);
            BlockPos head = spot.clothHead();
            level.scheduleTick(head, getBlock(), com.simplebuilding.blocks.custom.HammockBlock.CHECK_TICKS);
            SoundType sound = getBlock().defaultBlockState().getSoundType();
            level.playSound(null, head, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
            level.gameEvent(player, GameEvent.BLOCK_PLACE, head);
        }
        stack.consume(1, player);
        return true;
    }

    // --- remembered first anchor -------------------------------------------------------------------------------

    public static void rememberAnchor(ItemStack stack, ServerLevel level, BlockPos anchor) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            CompoundTag a = new CompoundTag();
            a.putInt("x", anchor.getX());
            a.putInt("y", anchor.getY());
            a.putInt("z", anchor.getZ());
            a.putString("dimension", level.dimension().identifier().toString());
            a.putLong("time", level.getGameTime());
            tag.put(ANCHOR_KEY, a);
        });
    }

    public static void clearAnchor(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || !data.copyTag().contains(ANCHOR_KEY)) {
            return;
        }
        CompoundTag tag = data.copyTag();
        tag.remove(ANCHOR_KEY);
        if (tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    /** The remembered first anchor while it is valid: same dimension, younger than 30 s, still an anchor. */
    public static Optional<BlockPos> storedAnchor(ItemStack stack, Level level) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return Optional.empty();
        }
        Optional<CompoundTag> anchor = data.copyTag().getCompound(ANCHOR_KEY);
        if (anchor.isEmpty()) {
            return Optional.empty();
        }
        CompoundTag a = anchor.get();
        BlockPos pos = new BlockPos(a.getIntOr("x", 0), a.getIntOr("y", 0), a.getIntOr("z", 0));
        boolean valid = level.dimension().identifier().toString().equals(a.getStringOr("dimension", ""))
                && level.getGameTime() - a.getLongOr("time", 0L) <= ANCHOR_TICKS && HammockLayout.isAnchor(level, pos);
        return valid ? Optional.of(pos) : Optional.empty();
    }

    private static void sparks(ServerLevel level, ServerPlayer player, BlockPos anchor) {
        level.sendParticles(player, ParticleTypes.HAPPY_VILLAGER, false, false, anchor.getX() + 0.5, anchor.getY() + 0.5,
                anchor.getZ() + 0.5, 4, 0.4, 0.4, 0.4, 0.0);
    }

    /** Forgets the first anchor when the item leaves the main hand or the anchor runs out; sparks while it waits. */
    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || !data.copyTag().contains(ANCHOR_KEY)) {
            return;
        }
        Optional<BlockPos> anchor = storedAnchor(stack, level);
        if (anchor.isEmpty() || slot != EquipmentSlot.MAINHAND) {
            clearAnchor(stack);
            return;
        }
        if (entity instanceof ServerPlayer player && level.getGameTime() % 10 == 0) {
            sparks(level, player, anchor.get());
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            java.util.function.Consumer<Component> lines, TooltipFlag flag) {
        lines.accept(Component.translatable("tooltip.simplebuilding.hammock").withStyle(ChatFormatting.GRAY));
        lines.accept(Component.translatable("tooltip.simplebuilding.hammock.2").withStyle(ChatFormatting.GRAY));
        lines.accept(Component.translatable("tooltip.simplebuilding.hammock.3").withStyle(ChatFormatting.GRAY));
    }
}
