package com.simplebuilding.tweaks.item;

import com.simplebuilding.tweaks.SimpleTweaks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Amethystlinse (Registry-Id weiter {@code laser_pointer}; Simple Tweaks: Laserpointer). Gedrueckt
 * halten zeigt einen Punkt, den alle in der Naehe sehen; ruht der Strahl auf einem Block, wirkt er
 * dort ({@link LaserBeam}: schmelzen, zuenden, trocknen).
 *
 * <p>Die Haltbarkeit ist die Ladung: Strahlen kostet {@link #BEAM_COST} je Sekunde, jede
 * Blockwirkung zusaetzlich {@link #EFFECT_COST}. Die Linse zerbricht nie, sie wird nur leer und
 * strahlt dann nicht mehr. Aufladen im Amboss mit Redstone, ohne Stufenkosten: ein voller Stapel
 * (64) laedt ganz auf ({@link #CHARGE_PER_REDSTONE} je Staub).
 */
public class LaserPointerItem extends Item implements com.simplebuilding.items.AnvilRechargeable {
    /** Volle Ladung (= Haltbarkeit). */
    public static final int MAX_CHARGE = 640;
    /** Ein Redstone laedt 1/64 der vollen Ladung. */
    public static final int CHARGE_PER_REDSTONE = MAX_CHARGE / 64;
    /** Ladung je Sekunde Strahlen (auch nur zeigen). */
    public static final int BEAM_COST = 1;
    /** Ladung je ausgeloester Blockwirkung. */
    public static final int EFFECT_COST = 5;
    /** Nur bis hierhin wirkt der Strahl auf Bloecke (der Punkt selbst reicht bis zur Config-Reichweite). */
    public static final double EFFECT_RANGE = 24.0;

    public LaserPointerItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public boolean isRechargeMaterial(ItemStack material) {
        return material.is(net.minecraft.world.item.Items.REDSTONE);
    }

    @Override
    public int chargePerMaterial() {
        return CHARGE_PER_REDSTONE;
    }

    /** Leer: die ganze Ladung ist verbraucht (Schaden = Haltbarkeit). */
    public static boolean isEmpty(ItemStack stack) {
        return stack.isDamageableItem() && stack.getDamageValue() >= stack.getMaxDamage();
    }

    /** Zieht Ladung ab, nie ueber leer hinaus - die Linse zerbricht nicht. Kreativ kostet nichts. */
    public static void drain(Player player, ItemStack stack, int amount) {
        if (player.hasInfiniteMaterials() || !stack.isDamageableItem()) {
            return;
        }
        stack.setDamageValue(Math.min(stack.getMaxDamage(), stack.getDamageValue() + amount));
    }

    /** Wie viel Redstone (hoechstens {@code available}) die Linse bis voll braucht; 0 = schon voll. */
    public static int redstoneNeeded(ItemStack stack, int available) {
        int missing = stack.getDamageValue();
        return Math.max(0, Math.min(available, (missing + CHARGE_PER_REDSTONE - 1) / CHARGE_PER_REDSTONE));
    }

    /** Kopie der Linse, mit {@code redstone} Staub aufgeladen. */
    public static ItemStack recharged(ItemStack stack, int redstone) {
        ItemStack result = stack.copy();
        result.setDamageValue(Math.max(0, stack.getDamageValue() - redstone * CHARGE_PER_REDSTONE));
        return result;
    }

    /** Ob die Linse gerade strahlen darf: Laser eingeschaltet (Server-Config, auf dem Client die gemeldete) und nicht leer. */
    public static boolean canBeam(ItemStack stack) {
        return SimpleTweaks.effectiveValues().laserEnabled() && !isEmpty(stack);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!canBeam(player.getItemInHand(hand))) {
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int ticksRemaining) {
        if (!canBeam(stack)) {
            user.stopUsingItem();
            if (user instanceof ServerPlayer player) {
                LaserBeam.reset(player);
            }
            return;
        }
        if (!(user instanceof ServerPlayer player)) {
            return;
        }
        int usedTicks = getUseDuration(stack, user) - ticksRemaining;
        if (usedTicks > 0 && usedTicks % 20 == 0) {
            drain(player, stack, BEAM_COST);
        }
        HitResult hit = player.pick(Math.min(EFFECT_RANGE, SimpleTweaks.config().laserPointer.range), 1.0f, false);
        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            LaserBeam.beamAt(player, stack, blockHit);
        } else {
            LaserBeam.reset(player);
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity user, int ticksRemaining) {
        if (user instanceof ServerPlayer player) {
            LaserBeam.reset(player);
        }
        return super.releaseUsing(stack, level, user, ticksRemaining);
    }
}
