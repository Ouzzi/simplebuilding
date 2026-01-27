package com.simplebuilding.items.custom;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

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
        public Text getText() { return Text.translatable(translationKey); }
    }

    public enum FillOrder {
        DEFAULT("simplebuilding.order.default"),
        BOTTOM_UP("simplebuilding.order.bottom_up"),
        TOP_DOWN("simplebuilding.order.top_down");

        private final String translationKey;
        FillOrder(String translationKey) { this.translationKey = translationKey; }
        public Text getText() { return Text.translatable(translationKey); }
    }

    public OctantItem(Settings settings, @Nullable DyeColor color) {
        super(settings);
        this.color = color;
    }

    public DyeColor getColor() { return this.color; }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        PlayerEntity player = context.getPlayer();
        ItemStack stack = context.getStack();

        if (!world.isClient()) {
            NbtComponent nbtData = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
            NbtCompound nbt = nbtData.copyNbt();

            if (nbt.getBoolean("Locked", false)) {
                if (player != null) {
                    player.sendMessage(Text.translatable("simplebuilding.gui.locked").formatted(Formatting.RED), true);
                }
                return ActionResult.SUCCESS;
            }

            if (player != null) {
                if (!player.isSneaking()) {
                    nbt.putIntArray("Pos1", new int[]{pos.getX(), pos.getY(), pos.getZ()});
                    world.playSound(null, pos, SoundEvents.BLOCK_COPPER_STEP, SoundCategory.PLAYERS, 0.3f, 2f);
                } else {
                    nbt.putIntArray("Pos2", new int[]{pos.getX(), pos.getY(), pos.getZ()});
                    world.playSound(null, pos, SoundEvents.BLOCK_COPPER_STEP, SoundCategory.PLAYERS, 0.3f, 1.5f);
                }
                if (!player.getAbilities().creativeMode) {
                    stack.damage(1, (ServerWorld) world, (ServerPlayerEntity) player, item -> player.sendEquipmentBreakStatus(item, context.getHand() == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND));
                }
            }
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        NbtComponent nbtData = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        if (nbtData.copyNbt().getBoolean("Locked", false)) {
             return ActionResult.PASS;
        }

        if (!world.isClient() && user.isSneaking()) {
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
            world.playSound(null, user.getBlockPos(), SoundEvents.UI_BUTTON_CLICK.value(), SoundCategory.PLAYERS, 0.5f, 1f);
            return ActionResult.SUCCESS;
        }
        return super.use(world, user, hand);
    }

    @Override
    public Text getName(ItemStack stack) {
        Text baseName = super.getName(stack);
        NbtComponent nbtComponent = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound nbt = nbtComponent.copyNbt();
        if (nbt.contains("Shape")) {
            try {
                SelectionShape shape = SelectionShape.valueOf(nbt.getString("Shape", ""));
                if (shape != SelectionShape.CUBOID) {
                    return Text.empty().append(baseName).append(Text.literal(" (")).append(shape.getText()).append(Text.literal(")").formatted(Formatting.GRAY));
                }
            } catch (Exception ignored) {}
        }
        return baseName;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        NbtComponent nbtData = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound nbt = nbtData.copyNbt();

        if (nbt.getBoolean("Locked", false)) {
            textConsumer.accept(Text.translatable("simplebuilding.gui.locked").formatted(Formatting.RED, Formatting.BOLD));
        }

        if (nbt.contains("Pos1")) {
            nbt.getIntArray("Pos1").ifPresent(p1 -> {
                if (p1.length == 3) {
                    BlockPos pos1 = new BlockPos(p1[0], p1[1], p1[2]);
                    textConsumer.accept(Text.literal(pos1.toShortString()).formatted(Formatting.YELLOW));
                    if (nbt.contains("Pos2")) {
                        nbt.getIntArray("Pos2").ifPresent(p2 -> {
                            if (p2.length == 3) {
                                BlockPos pos2 = new BlockPos(p2[0], p2[1], p2[2]);
                                textConsumer.accept(Text.literal(pos2.toShortString()).formatted(Formatting.GREEN));
                            }
                        });
                    }
                }
            });
        }
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
    }
}

