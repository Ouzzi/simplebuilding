package com.simplebuilding.tweaks.item;

import com.simplebuilding.tweaks.SimpleTweaks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Amethystlinse (Registry-Id weiter {@code laser_pointer}; Simple Tweaks: Laserpointer). Gedrueckt
 * halten zeigt einen Punkt, den alle in der Naehe sehen; ruht der Strahl auf einem Block, wirkt er
 * dort ({@link LaserBeam}: schmelzen, zuenden, trocknen, TNT, Lebewesen anzuenden - Verweildauer
 * waechst mit dem Abstand, Klaenge am Trefferpunkt).
 *
 * <p>Die Haltbarkeit ist die Ladung: Strahlen kostet {@link #BEAM_COST} je angefangener Sekunde -
 * auch blosses Zeigen, in die Luft oder auf einen Block ohne Wirkung, und schon der erste Tick
 * (vorher erst nach 20 Ticks, kurzes Antippen war dadurch gratis) -, jede Blockwirkung zusaetzlich
 * {@link #EFFECT_COST}. Die Linse zerbricht nie, sie wird nur leer und
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
    /**
     * Wie weit der Strahl auf dem Server wirkt: die Reichweite der Linse (Config {@code range}),
     * hoechstens aber die Sichtweite des Servers - dort sind die Chunks um den Spieler geladen, der
     * Strahl laedt also keine (bis 2026-09-27 fest 24 Bloecke). Mindestens 2 Chunks, denn ein Server
     * ohne gesetzte Sichtweite (Spieltest-Server) meldet 0.
     */
    public static double effectRange(ServerPlayer player) {
        int viewBlocks = Math.max(2, player.level().getServer().getPlayerList().getViewDistance()) * 16;
        return Math.min(SimpleTweaks.config().laserPointer.range, viewBlocks);
    }

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
        // Je angefangene Sekunde, beginnend mit dem ersten Tick: jeder Weg, die Linse zu benutzen,
        // kostet Ladung - auch Antippen und Zeigen ins Leere.
        int usedTicks = getUseDuration(stack, user) - ticksRemaining;
        if (usedTicks >= 0 && usedTicks % 20 == 0) {
            drain(player, stack, BEAM_COST);
        }
        double range = effectRange(player);
        HitResult hit = player.pick(range, 1.0f, false);
        Vec3 eye = player.getEyePosition();
        Vec3 view = player.getViewVector(1.0f);
        double reach = Math.min(LaserBeam.ENTITY_RANGE, hit.getType() == HitResult.Type.MISS ? range : hit.getLocation().distanceTo(eye));
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(player, eye, eye.add(view.scale(reach)),
                player.getBoundingBox().expandTowards(view.scale(reach)).inflate(1.0),
                entity -> entity instanceof LivingEntity && !entity.isSpectator() && entity.isPickable(), reach * reach);
        boolean hum = usedTicks % LaserBeam.HUM_PERIOD == 0;
        if (entityHit != null) {
            if (hum) {
                LaserBeam.hum(player, entityHit.getLocation());
            }
            LaserBeam.beamAtEntity(player, stack, entityHit);
        } else if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            if (hum) {
                LaserBeam.hum(player, blockHit.getLocation());
            }
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
