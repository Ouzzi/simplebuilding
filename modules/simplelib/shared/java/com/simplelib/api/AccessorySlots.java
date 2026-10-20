package com.simplelib.api;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Worn accessory slots from an optional accessory mod, so a backpack or quiver can be worn on the back or belt instead
 * of in the chest slot (split stage 8c-1, docs/ai/PLAN-B-S8C1-STORAGE-2026-10-20.md: moved here from SimpleBuilding so
 * Simple Storage's quivers and SimpleBuilding's backpacks share one hook list without importing each other).
 *
 * <p>The game code never touches an accessory mod's API directly. It asks this class, and the mod that brings the
 * accessory integration registers one {@link Hook} per accessory mod at startup, only when that mod is actually loaded
 * (SimpleBuilding: Trinkets Updated on Fabric, Curios on NeoForge). With no hook registered every query returns
 * "nothing worn" and every mod behaves exactly as without accessory support.
 *
 * <p>Lookup order everywhere (backpack key, quiver for bow and crossbow, Funnel, Master Builder, worn render): chest
 * slot first, then the accessory slots, then the inventory - an accessory slot counts as "worn".
 *
 * <p>Game tests register a player-scoped hook of their own ({@link #register}/{@link #unregister}), which is why this is
 * a list and not a single field: tests run in parallel on one server.
 */
public final class AccessorySlots {
    private static final Logger LOGGER = LoggerFactory.getLogger("simplelib/accessories");
    private static final List<Hook> HOOKS = new CopyOnWriteArrayList<>();

    private AccessorySlots() {
    }

    /** One accessory mod's view of an entity's worn accessories. */
    public interface Hook {
        /**
         * The live stacks in the entity's accessory slots (not cosmetic slots), in the accessory
         * mod's slot order; empty slots may be left out. Never null. The stacks must be the ones the
         * slots hold (identity), because callers change their components in place.
         */
        List<ItemStack> worn(LivingEntity entity);

        /** Like {@link #worn}, restricted to slots whose render toggle is on. */
        default List<ItemStack> visible(LivingEntity entity) {
            return worn(entity);
        }
    }

    public static void register(Hook hook) {
        HOOKS.add(hook);
    }

    public static void unregister(Hook hook) {
        HOOKS.remove(hook);
    }

    /** Is any accessory hook active (an accessory mod loaded, or a test hook)? */
    public static boolean active() {
        return !HOOKS.isEmpty();
    }

    /** The first worn accessory matching {@code test}, or {@link ItemStack#EMPTY}. */
    public static ItemStack findFirst(LivingEntity entity, Predicate<ItemStack> test) {
        return first(entity, test, false);
    }

    /** The first visibly worn accessory matching {@code test} (for rendering), or EMPTY. */
    public static ItemStack findFirstVisible(LivingEntity entity, Predicate<ItemStack> test) {
        return first(entity, test, true);
    }

    /** All worn accessories matching {@code test}, in slot order. */
    public static List<ItemStack> findAll(LivingEntity entity, Predicate<ItemStack> test) {
        if (HOOKS.isEmpty() || entity == null) {
            return List.of();
        }
        List<ItemStack> found = new ArrayList<>();
        for (Hook hook : HOOKS) {
            for (ItemStack stack : query(hook, entity, false)) {
                if (!stack.isEmpty() && test.test(stack)) {
                    found.add(stack);
                }
            }
        }
        return found;
    }

    /** Does an accessory slot of {@code entity} hold exactly this stack (identity)? */
    public static boolean isWorn(LivingEntity entity, ItemStack stack) {
        return !stack.isEmpty() && !findFirst(entity, s -> s == stack).isEmpty();
    }

    private static ItemStack first(LivingEntity entity, Predicate<ItemStack> test, boolean visibleOnly) {
        if (HOOKS.isEmpty() || entity == null) {
            return ItemStack.EMPTY;
        }
        for (Hook hook : HOOKS) {
            for (ItemStack stack : query(hook, entity, visibleOnly)) {
                if (!stack.isEmpty() && test.test(stack)) {
                    return stack;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * Asks one hook. An accessory mod whose API changed under us (a {@link LinkageError} such as
     * NoSuchMethodError) must not crash the game on every bow shot: the hook is dropped once, with
     * one log line, and the mod carries on as if the accessory mod were absent.
     */
    private static List<ItemStack> query(Hook hook, LivingEntity entity, boolean visibleOnly) {
        try {
            List<ItemStack> stacks = visibleOnly ? hook.visible(entity) : hook.worn(entity);
            return stacks == null ? List.of() : stacks;
        } catch (LinkageError error) {
            HOOKS.remove(hook);
            LOGGER.error("Accessory integration {} is incompatible with the installed accessory mod and was disabled",
                    hook.getClass().getName(), error);
            return List.of();
        }
    }
}
