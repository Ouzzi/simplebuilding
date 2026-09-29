package com.simplebuilding.tweaks.item;

import com.simplebuilding.component.LensMeasurement;
import com.simplebuilding.component.ModDataComponentTypes;
import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.util.EnchantmentHelper;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Amethystlinse (Registry-Id {@code amethyst_lens}, bis 2026-09-28 laser_pointer; Simple Tweaks: Laserpointer). Gedrueckt
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
 *
 * <p>Messen (Besitzer 2026-09-28): ohne Verzauberung zeigt das HUD neben dem Fadenkreuz nur
 * "Laser". Mit Beruehrung des Konstrukteurs (Amboss + Buch, wie beim Geschwindigkeitsmesser)
 * wird die Linse zum Entfernungsmesser: das HUD zeigt Entfernung, Zielblock und Hoehe, und der
 * Server schreibt die letzte Messung ({@link LensMeasurement}) in die Linse - beim ersten Tick,
 * danach je Sekunde und beim Loslassen; der Tooltip zeigt sie.
 */
public class LaserPointerItem extends Item implements com.simplebuilding.items.AnvilRechargeable {
    /** Standard der vollen Ladung. */
    public static final int DEFAULT_MAX_CHARGE = 640;
    /**
     * Volle Ladung (= Haltbarkeit): {@code server.charges.lensMaxCharge}, beim Registrieren gelesen
     * (Neustart noetig; Client und Server brauchen denselben Wert).
     */
    public static final int MAX_CHARGE = com.simplebuilding.config.ServerTuning.startupLensMaxCharge();
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

    /** Ladung je angefangener Sekunde Strahlen: Config {@code tweaks.laserPointer.beamCostPerSecond} (Standard {@link #BEAM_COST}). */
    public static int beamCost() {
        return Math.max(0, SimpleTweaks.config().laserPointer.beamCostPerSecond);
    }

    /** Ladung je Wirkung: Config {@code tweaks.laserPointer.effectCost} (Standard {@link #EFFECT_COST}). */
    public static int effectCost() {
        return Math.max(0, SimpleTweaks.config().laserPointer.effectCost);
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
        HitResult target = aim(player);
        if (usedTicks >= 0 && usedTicks % 20 == 0) {
            drain(player, stack, beamCost());
            recordMeasurement(player, stack, target);
        }
        boolean hum = usedTicks % LaserBeam.HUM_PERIOD == 0;
        if (target instanceof EntityHitResult entityHit) {
            if (hum) {
                LaserBeam.hum(player, entityHit.getLocation());
            }
            LaserBeam.beamAtEntity(player, stack, entityHit);
        } else if (target instanceof BlockHitResult blockHit && target.getType() == HitResult.Type.BLOCK) {
            if (hum) {
                LaserBeam.hum(player, blockHit.getLocation());
            }
            LaserBeam.beamAt(player, stack, blockHit);
        } else {
            LaserBeam.reset(player);
        }
    }

    /**
     * Worauf der Strahl zeigt: das erste Lebewesen innerhalb {@link LaserBeam#ENTITY_RANGE} vor dem
     * Block, sonst der Block ({@link #effectRange}), sonst ein Fehlschuss.
     */
    public static HitResult aim(ServerPlayer player) {
        double range = effectRange(player);
        HitResult hit = player.pick(range, 1.0f, false);
        Vec3 eye = player.getEyePosition();
        Vec3 view = player.getViewVector(1.0f);
        double reach = Math.min(LaserBeam.ENTITY_RANGE, hit.getType() == HitResult.Type.MISS ? range : hit.getLocation().distanceTo(eye));
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(player, eye, eye.add(view.scale(reach)),
                player.getBoundingBox().expandTowards(view.scale(reach)).inflate(1.0),
                entity -> entity instanceof LivingEntity && !entity.isSpectator() && entity.isPickable(), reach * reach);
        return entityHit != null ? entityHit : hit;
    }

    /** Ob die Linse misst: nur mit Beruehrung des Konstrukteurs. */
    public static boolean measures(ItemStack stack, Level level) {
        return EnchantmentHelper.hasConstructorsTouch(stack, level);
    }

    /**
     * Schreibt die Messung zu {@code target} als {@link ModDataComponentTypes#LENS_MEASUREMENT} in
     * die Linse - nur mit Beruehrung des Konstrukteurs und nur bei einem Treffer. Liefert, ob
     * geschrieben wurde.
     */
    public static boolean recordMeasurement(ServerPlayer player, ItemStack stack, HitResult target) {
        if (target == null || target.getType() == HitResult.Type.MISS || !measures(stack, player.level())) {
            return false;
        }
        int targetY;
        String key;
        if (target instanceof EntityHitResult entityHit) {
            targetY = Mth.floor(entityHit.getEntity().getY());
            key = entityHit.getEntity().getType().getDescriptionId();
        } else if (target instanceof BlockHitResult blockHit) {
            targetY = blockHit.getBlockPos().getY();
            key = player.level().getBlockState(blockHit.getBlockPos()).getBlock().getDescriptionId();
        } else {
            return false;
        }
        float distance = LensMeasurement.round(target.getLocation().distanceTo(player.getEyePosition()));
        stack.set(ModDataComponentTypes.LENS_MEASUREMENT,
                new LensMeasurement(distance, targetY - Mth.floor(player.getY()), key));
        return true;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity user, int ticksRemaining) {
        if (user instanceof ServerPlayer player) {
            LaserBeam.reset(player);
            if (canBeam(stack)) {
                recordMeasurement(player, stack, aim(player));
            }
        }
        return super.releaseUsing(stack, level, user, ticksRemaining);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
        LensMeasurement last = stack.get(ModDataComponentTypes.LENS_MEASUREMENT);
        if (last != null) {
            lines.accept(Component.translatable("tooltip.simplebuilding.amethyst_lens.last_measured",
                    String.format("%.1f", last.distance())).withStyle(ChatFormatting.GRAY));
            lines.accept(Component.translatable("tooltip.simplebuilding.amethyst_lens.last_target",
                    Component.translatable(last.target()), signed(last.heightDifference())).withStyle(ChatFormatting.DARK_GRAY));
        } else if (!stack.isEnchanted()) {
            lines.accept(Component.translatable("tooltip.simplebuilding.amethyst_lens.touch_hint").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    /** Hoehenunterschied mit Vorzeichen: "+3", "-2", "0". */
    public static String signed(int value) {
        return value > 0 ? "+" + value : Integer.toString(value);
    }
}
