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
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class OctantItem extends Item {

    public static final int DURABILITY_OCTANT = 128;
    private final DyeColor color;

    // Bestehende Formen
    public enum SelectionShape {
        CUBOID("Würfel"),
        CYLINDER("Zylinder"),
        SPHERE("Kugel"),
        PYRAMID("Pyramide"),
        TRIANGLE("Prisma"), // Dreieck/Dach
        ROUND_ARCH("Rundbogen");

        private final String name;
        SelectionShape(String name) { this.name = name; }
        public String getName() { return name; }
    }

    // NEU: Modus (2D vs 3D)
    public enum SelectionMode {
        MODE_3D("3D"),
        MODE_2D("2D (Flach)");

        private final String name;
        SelectionMode(String name) { this.name = name; }
        public String getName() { return name; }
    }

    public OctantItem(Settings settings, @Nullable DyeColor color) {
        super(settings);
        this.color = color;
    }

    public DyeColor getColor() { return this.color; }

    // --- SCROLL LOGIK (Angepasst) ---
    public void scrollAttribute(ItemStack stack, int amount, boolean isShift, boolean isControl, boolean isAlt, PlayerEntity player) {
        NbtComponent nbtData = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound nbt = nbtData.copyNbt();

        if (nbt.getBoolean("Locked", false)) {
            player.sendMessage(Text.literal("✖ Octant ist gesperrt!").formatted(Formatting.RED), true);
            return;
        }

        // Shift: Formen durchschalten
        if (isShift) {
            SelectionShape[] values = SelectionShape.values();
            String currentName = nbt.getString("Shape", "");
            SelectionShape currentShape = SelectionShape.CUBOID;
            try { if(!currentName.isEmpty()) currentShape = SelectionShape.valueOf(currentName); } catch (Exception ignored) {}

            int index = (currentShape.ordinal() + amount) % values.length;
            if (index < 0) index += values.length;

            SelectionShape newShape = values[index];
            nbt.putString("Shape", newShape.name());
            player.sendMessage(Text.literal("Form: " + newShape.getName()).formatted(Formatting.AQUA), true);
        }
        // Alt: Modus ändern (2D/3D)
        else if (isAlt) {
            SelectionMode[] values = SelectionMode.values();
            String currentModeName = nbt.getString("Mode", "");
            SelectionMode currentMode = SelectionMode.MODE_3D;
            try { if(!currentModeName.isEmpty()) currentMode = SelectionMode.valueOf(currentModeName); } catch (Exception ignored) {}

            int index = (currentMode.ordinal() + amount) % values.length;
            if (index < 0) index += values.length;

            SelectionMode newMode = values[index];
            nbt.putString("Mode", newMode.name());
            player.sendMessage(Text.literal("Modus: " + newMode.getName()).formatted(Formatting.YELLOW), true);
        }

        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

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
                    player.sendMessage(Text.literal("Gesperrt! Drücke G zum Entsperren.").formatted(Formatting.RED), true);
                }
                return ActionResult.SUCCESS;
            }

            if (player != null) {
                if (!player.isSneaking()) {
                    nbt.putIntArray("Pos1", new int[]{pos.getX(), pos.getY(), pos.getZ()});
                    world.playSound(null, pos, SoundEvents.BLOCK_COPPER_STEP, SoundCategory.PLAYERS, 0.3f, 2f);
                    player.sendMessage(Text.literal("Pos 1 gesetzt").formatted(Formatting.GRAY), true);
                } else {
                    nbt.putIntArray("Pos2", new int[]{pos.getX(), pos.getY(), pos.getZ()});
                    world.playSound(null, pos, SoundEvents.BLOCK_COPPER_STEP, SoundCategory.PLAYERS, 0.3f, 1.5f);
                    player.sendMessage(Text.literal("Pos 2 gesetzt").formatted(Formatting.GRAY), true);
                }
            }
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        // Check Lock vor dem Reset
        NbtComponent nbtData = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);

        // CHECK LOCK vor Reset
        if (nbtData.copyNbt().getBoolean("Locked", false)) {
             return ActionResult.PASS; // Nichts tun wenn locked
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
            String shapeName = nbt.getString("Shape", "");
            try {
                SelectionShape shape = SelectionShape.valueOf(shapeName);
                if (shape != SelectionShape.CUBOID) {
                    return Text.empty().append(baseName).append(Text.literal(" (" + shape.getName() + ")").formatted(Formatting.GRAY));
                }
            } catch (Exception e) {}
        }
        return baseName;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        NbtComponent nbtData = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound nbt = nbtData.copyNbt();

        // TOOLTIP LOCK ANZEIGE
        if (nbt.getBoolean("Locked", false)) {
            textConsumer.accept(Text.literal(" [LOCKED] ").formatted(Formatting.RED, Formatting.BOLD));
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

                                int dx = Math.abs(pos1.getX() - pos2.getX()) + 1;
                                int dy = Math.abs(pos1.getY() - pos2.getY()) + 1;
                                int dz = Math.abs(pos1.getZ() - pos2.getZ()) + 1;

                                if (dy == 1 && (dx == 1 || dz == 1)) {
                                    textConsumer.accept(Text.literal("Distance: " + Math.max(dx, dz)).formatted(Formatting.AQUA));
                                } else if (dy == 1) {
                                    textConsumer.accept(Text.literal("Area: " + (dx * dz) + " ("+ dx + " x " + dz + ")").formatted(Formatting.AQUA));
                                } else {
                                    textConsumer.accept(Text.literal("Volume: " + (dx * dy * dz) + " ("+ dx + " x " + dy + " x " + dz + ")").formatted(Formatting.AQUA));
                                }
                            }
                        });
                    }
                }
            });
        }
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
    }
}

