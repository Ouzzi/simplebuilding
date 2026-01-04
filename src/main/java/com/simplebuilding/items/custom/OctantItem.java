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

import java.util.function.Consumer;

public class OctantItem extends Item {

    public static final int DURABILITY_OCTANT = 128;

    private final DyeColor color;

    public enum SelectionShape {
        CUBOID("Cuboid"),
        CYLINDER("Cylinder"),
        TRIANGLE("Triangle"),
        PYRAMID("Pyramid"),
        SPHERE("Sphere");

        private final String name;

        SelectionShape(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public void scrollAttribute(ItemStack stack, int amount, boolean isShift, boolean isControl, boolean isAlt, PlayerEntity player) {
        NbtComponent nbtData = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound nbt = nbtData.copyNbt();

        // Wenn Locked ist, verhindern wir auch das Scrollen der Form, um Verwirrung zu vermeiden
        // (Optional: Wenn du willst, dass man Form trotz Lock ändern kann, entferne diese Zeile)
        if (nbt.getBoolean("Locked").orElse(false)) {
            player.sendMessage(Text.literal("Octant is Locked!").formatted(Formatting.RED), true);
            return;
        }

        // 1. Shift: Formen durchschalten (Shapes)
        if (isShift) {
            SelectionShape[] values = SelectionShape.values();
            String currentName = nbt.getString("Shape").orElse("");
            int index = 0;
            if (!currentName.isEmpty()) {
                try {
                    index = SelectionShape.valueOf(currentName).ordinal();
                } catch (Exception ignored) {}
            }

            int newIndex = (index + amount) % values.length;
            if (newIndex < 0) newIndex += values.length;

            SelectionShape newShape = values[newIndex];
            nbt.putString("Shape", newShape.name());
            player.sendMessage(Text.of("Form: " + newShape.name()), true);
        }
        // 2. Control: Größe/Radius ändern
        else if (isControl) {
            int currentRadius = nbt.getInt("Radius").orElse(5);
            int newRadius = Math.max(1, currentRadius + amount);
            nbt.putInt("Radius", newRadius);
            player.sendMessage(Text.of("Radius: " + newRadius), true);
        }
        // 3. Alt: Modus ändern
        else if (isAlt) {
            boolean currentMode = nbt.getBoolean("SomeMode").orElse(false);
            nbt.putBoolean("SomeMode", !currentMode);
            player.sendMessage(Text.of("Mode: " + (!currentMode ? "An" : "Aus")), true);
        }

        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    public OctantItem(Settings settings, @Nullable DyeColor color) {
        super(settings);
        this.color = color;
    }

    public DyeColor getColor() {
        return this.color;
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

            // --- NEU: CHECK LOCK STATUS ---
            // Wenn Locked, verhindern wir das Setzen neuer Koordinaten.
            if (nbt.getBoolean("Locked").orElse(false)) {
                if (player != null) {
                    player.sendMessage(Text.literal("Locked! Press G to unlock.").formatted(Formatting.RED), true);
                }
                // Wir geben SUCCESS zurück, damit keine "normale" Block-Interaktion (z.B. Truhe öffnen)
                // passiert, die den User verwirren könnte, oder PASS, wenn du Truhen öffnen erlauben willst.
                return ActionResult.SUCCESS;
            }
            // ------------------------------

            assert player != null;
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

            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
        }
        return net.minecraft.util.ActionResult.SUCCESS;
    }


    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        // Check Lock vor dem Reset
        NbtComponent nbtData = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        // Wir lesen nur, wir kopieren erst wenn wir schreiben wollen
        if (nbtData.copyNbt().getBoolean("Locked").orElse(false)) {
             // Wenn Locked, passiert beim Rechtsklick in die Luft nichts (kein Reset)
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
            String shapeName = nbt.getString("Shape").orElse("");
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

        // --- NEU: Lock Anzeige im Tooltip ---
        if (nbt.getBoolean("Locked").orElse(false)) {
            textConsumer.accept(Text.literal(" [LOCKED] ").formatted(Formatting.RED, Formatting.BOLD));
        }
        // ------------------------------------

        if (nbt.contains("Pos1")) {
            int[] p1 = nbt.getIntArray("Pos1").orElse(new int[0]);

            if (p1.length == 3) {
                BlockPos pos1 = new BlockPos(p1[0], p1[1], p1[2]);
                textConsumer.accept(Text.literal(pos1.toShortString()).formatted(Formatting.YELLOW));

                if (nbt.contains("Pos2")) {
                    int[] p2 = nbt.getIntArray("Pos2").orElse(new int[0]);

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
                }
            }
        }
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
    }
}