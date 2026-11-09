package com.simplelib.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Gadgets laid down as a plate (OW24-2 G3, docs/ai/PLAN-B-GADG14-2026-11-09.md): a gadget mod (Simple Gadgets: Attractor,
 * Detector) says per item whether it can be laid down and what it does there each tick; a partner that owns a plate block
 * (SimpleBuilding's placed smithing templates) lays it down and ticks it. Neither side imports the other. Without a partner
 * {@link #tryPlace} answers {@code null} and the gadget keeps its held use only. Content-free: only Vanilla types.
 */
public final class PlacedGadgets {
    /** The partner's plate holding one gadget stack. */
    public interface Plate {
        ItemStack stack();

        /** Replaces the stack (an empty stack: the gadget broke; the partner removes the plate). */
        void setStack(ItemStack stack);

        /** The stack changed in place (damage, components): the partner saves it. */
        void changed();

        /** The centre of the plate's visible face. */
        Vec3 surfaceCentre();

        /** The direction the plate faces. */
        Direction normal();

        /** Whether the plate at its position may move {@code entity} (claims of the partner). */
        boolean mayAutomate(Entity entity);
    }

    /** What a gadget does as a plate. */
    public interface Behaviour {
        /** Whether this stack may be laid down at all (e.g. only a calibrated detector). */
        boolean placeable(ItemStack stack);

        /** Whether a plain right click (no sneaking) lays it down; otherwise sneak + right click does. */
        default boolean placesOnPlainClick(ItemStack stack, @Nullable Level level) {
            return false;
        }

        /** One server tick of the plate at {@code pos}. */
        void tick(ServerLevel level, BlockPos pos, Plate plate);
    }

    /** The partner that lays gadgets down: {@code null} = this click lays nothing down. */
    @FunctionalInterface
    public interface Placer {
        @Nullable InteractionResult tryPlace(UseOnContext context);
    }

    private static final Map<Item, Behaviour> BEHAVIOURS = new ConcurrentHashMap<>();
    private static volatile @Nullable Placer placer;

    private PlacedGadgets() {}

    public static void register(Item item, Behaviour behaviour) {
        BEHAVIOURS.put(item, behaviour);
    }

    /** The behaviour of the stack's item, or {@code null}. */
    public static @Nullable Behaviour behaviour(ItemStack stack) {
        return stack.isEmpty() ? null : BEHAVIOURS.get(stack.getItem());
    }

    /** Whether {@code stack} is a gadget that may be laid down now. */
    public static boolean placeable(ItemStack stack) {
        Behaviour b = behaviour(stack);
        return b != null && b.placeable(stack);
    }

    public static boolean placesOnPlainClick(ItemStack stack, @Nullable Level level) {
        Behaviour b = behaviour(stack);
        return b != null && b.placesOnPlainClick(stack, level);
    }

    /** Ticks the gadget on {@code plate}; nothing for a stack without a behaviour. */
    public static void tick(ServerLevel level, BlockPos pos, Plate plate) {
        Behaviour b = behaviour(plate.stack());
        if (b != null) b.tick(level, pos, plate);
    }

    public static void setPlacer(@Nullable Placer partner) {
        placer = partner;
    }

    /** Whether a partner can lay gadgets down. */
    public static boolean hasPlacer() {
        return placer != null;
    }

    /** Asks the partner to lay the clicked gadget down; {@code null} without a partner or when it lays nothing down. */
    public static @Nullable InteractionResult tryPlace(UseOnContext context) {
        Placer p = placer;
        return p == null ? null : p.tryPlace(context);
    }
}
