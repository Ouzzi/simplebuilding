package com.simplebuilding.items.custom;

import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class OctantItem extends Item {

    public static final int DURABILITY_OCTANT = 128;
    private final DyeColor color;

    public enum SelectionShape {
        CUBOID("simplebuilding.shape.cuboid"),
        CYLINDER("simplebuilding.shape.cylinder"),
        TRIANGLE("simplebuilding.shape.triangle"), // Prism
        PYRAMID("simplebuilding.shape.pyramid"),
        SPHERE("simplebuilding.shape.sphere"),
        RECTANGLE("simplebuilding.shape.rectangle"), // 2D Cuboid
        ELLIPSE("simplebuilding.shape.ellipse"); // 2D Cylinder

        private final String translationKey;
        SelectionShape(String translationKey) { this.translationKey = translationKey; }
        public String getTranslationKey() { return translationKey; }
        public Component getText() { return Component.translatable(translationKey); }
    }

    public enum FillOrder {
        DEFAULT("simplebuilding.order.default"),
        BOTTOM_UP("simplebuilding.order.bottom_up"),
        TOP_DOWN("simplebuilding.order.top_down");

        private final String translationKey;
        FillOrder(String translationKey) { this.translationKey = translationKey; }
        public Component getText() { return Component.translatable(translationKey); }
    }

    public OctantItem(Properties settings, @Nullable DyeColor color) {
        super(settings);
        this.color = color;
    }

    public DyeColor getColor() { return this.color; }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (!world.isClientSide()) {
            CustomData nbtData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            CompoundTag nbt = nbtData.copyTag();

            if (nbt.getBooleanOr("Locked", false)) {
                if (player != null) {
                    player.displayClientMessage(Component.translatable("simplebuilding.gui.locked").withStyle(ChatFormatting.RED), true);
                }
                return InteractionResult.SUCCESS;
            }

            if (player != null) {
                if (!player.isShiftKeyDown()) {
                    nbt.putIntArray("Pos1", new int[]{pos.getX(), pos.getY(), pos.getZ()});
                    world.playSound(null, pos, SoundEvents.COPPER_STEP, SoundSource.PLAYERS, 0.3f, 2f);
                } else {
                    nbt.putIntArray("Pos2", new int[]{pos.getX(), pos.getY(), pos.getZ()});
                    world.playSound(null, pos, SoundEvents.COPPER_STEP, SoundSource.PLAYERS, 0.3f, 1.5f);
                }
                if (!player.getAbilities().instabuild) {
                    stack.hurtAndBreak(1, (ServerLevel) world, (ServerPlayer) player, item -> player.onEquippedItemBroken(item, context.getHand() == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND));
                }
            }
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        CustomData nbtData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (nbtData.copyTag().getBooleanOr("Locked", false)) {
             return InteractionResult.PASS;
        }

        if (!world.isClientSide() && user.isShiftKeyDown()) {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            world.playSound(null, user.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5f, 1f);
            return InteractionResult.SUCCESS;
        }
        return super.use(world, user, hand);
    }

    @Override
    public Component getName(ItemStack stack) {
        Component baseName = super.getName(stack);
        CustomData nbtComponent = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag nbt = nbtComponent.copyTag();
        if (nbt.contains("Shape")) {
            try {
                SelectionShape shape = SelectionShape.valueOf(nbt.getStringOr("Shape", ""));
                if (shape != SelectionShape.CUBOID) {
                    return Component.empty().append(baseName).append(Component.literal(" (")).append(shape.getText()).append(Component.literal(")").withStyle(ChatFormatting.GRAY));
                }
            } catch (Exception ignored) {}
        }
        return baseName;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
        CustomData nbtData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag nbt = nbtData.copyTag();

        if (nbt.getBooleanOr("Locked", false)) {
            textConsumer.accept(Component.translatable("simplebuilding.gui.locked").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        }

        if (nbt.contains("Pos1")) {
            nbt.getIntArray("Pos1").ifPresent(p1 -> {
                if (p1.length == 3) {
                    BlockPos pos1 = new BlockPos(p1[0], p1[1], p1[2]);
                    textConsumer.accept(Component.literal(pos1.toShortString()).withStyle(ChatFormatting.YELLOW));
                    if (nbt.contains("Pos2")) {
                        nbt.getIntArray("Pos2").ifPresent(p2 -> {
                            if (p2.length == 3) {
                                BlockPos pos2 = new BlockPos(p2[0], p2[1], p2[2]);
                                textConsumer.accept(Component.literal(pos2.toShortString()).withStyle(ChatFormatting.GREEN));
                            }
                        });
                    }
                }
            });
        }
        super.appendHoverText(stack, context, displayComponent, textConsumer, type);
    }
}

