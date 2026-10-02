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
 * Resonanzstab (Registry-Id {@code amethyst_lens} bleibt; bis 2026-09-29 "Amethystlinse", bis
 * 2026-09-28 laser_pointer; Simple Tweaks: Laserpointer). Gedrueckt halten zeigt einen Punkt, den alle
 * in der Naehe sehen; ruht der Strahl auf einem Block, wirkt er dort ({@link LaserBeam}: schmelzen,
 * zuenden, trocknen, TNT, Lebewesen anzuenden - Verweildauer waechst mit dem Abstand, Klaenge am
 * Trefferpunkt). Beim Benutzen neigt sich der Stab nach vorn und sprueht Amethyst-Funken
 * ({@link #emitSparks}); das Fadenkreuz verschwindet, der Punkt sitzt genau in seiner Mitte.
 *
 * <p>Die Haltbarkeit ist die Ladung: Strahlen kostet {@link #BEAM_COST} je angefangener Sekunde -
 * auch blosses Zeigen, in die Luft oder auf einen Block ohne Wirkung, und schon der erste Tick -,
 * jede Blockwirkung zusaetzlich {@link #EFFECT_COST}. Seit 2026-09-29 (Besitzer: "leert sich zu
 * langsam") vier je Sekunde statt einer: eine volle Ladung reicht fuer 160 s Dauerstrahl statt fast
 * 11 min. Der Stab zerbricht nie, er wird nur leer und strahlt dann nicht mehr. Aufladen im Amboss mit
 * seinem Material, Amethystscherben, ohne Stufenkosten: 16 Scherben laden ganz auf
 * ({@link #CHARGE_PER_SHARD} je Scherbe, bis 2026-09-29 Redstone). Reparatur (Mending) bleibt gesperrt.
 *
 * <p>Messen (Besitzer 2026-09-28): ohne Verzauberung kein Text, nur der Punkt. Mit Beruehrung des
 * Konstrukteurs (Amboss + Buch, wie bei der Messuhr) wird der Stab zum Entfernungsmesser: das HUD zeigt
 * im gemeinsamen Anzeigekasten der Mod (wie der Oktant) Entfernung, Zielblock und Hoehe, und der Server
 * schreibt die letzte Messung ({@link LensMeasurement}) in den Stab - beim ersten Tick, danach je
 * Sekunde und beim Loslassen; der Tooltip zeigt sie.
 */
public class LaserPointerItem extends Item implements com.simplebuilding.items.AnvilRechargeable {
    /** Standard der vollen Ladung. */
    public static final int DEFAULT_MAX_CHARGE = 640;
    /**
     * Volle Ladung (= Haltbarkeit): {@code server.charges.lensMaxCharge}, beim Registrieren gelesen
     * (Neustart noetig; Client und Server brauchen denselben Wert).
     */
    public static final int MAX_CHARGE = com.simplebuilding.config.ServerTuning.startupLensMaxCharge();
    /** Scherben fuer eine volle Ladung. */
    public static final int SHARDS_PER_FULL_CHARGE = 16;
    /** Eine Amethystscherbe laedt 1/16 der vollen Ladung (10 s Strahl). */
    public static final int CHARGE_PER_SHARD = MAX_CHARGE / SHARDS_PER_FULL_CHARGE;
    /** Ladung je Sekunde Strahlen (auch nur zeigen); 2026-09-29 von 1 auf 4. */
    public static final int BEAM_COST = 4;
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
        return Math.min(com.simplebuilding.tweaks.TweaksConfig.capped(SimpleTweaks.config().laserPointer.range, 1, com.simplebuilding.tweaks.TweaksConfig.MAX_LASER_RANGE, 512), viewBlocks);
    }

    public LaserPointerItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public boolean isRechargeMaterial(ItemStack material) {
        return material.is(net.minecraft.world.item.Items.AMETHYST_SHARD);
    }

    @Override
    public int chargePerMaterial() {
        return CHARGE_PER_SHARD;
    }

    /** Ladung je angefangener Sekunde Strahlen: Config {@code tweaks.laserPointer.chargePerSecond} (Standard {@link #BEAM_COST}). */
    public static int beamCost() {
        return Math.max(0, SimpleTweaks.config().laserPointer.chargePerSecond);
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

    /** Wie viele Amethystscherben (hoechstens {@code available}) der Stab bis voll braucht; 0 = schon voll. */
    public static int shardsNeeded(ItemStack stack, int available) {
        int missing = stack.getDamageValue();
        return Math.max(0, Math.min(available, (missing + CHARGE_PER_SHARD - 1) / CHARGE_PER_SHARD));
    }

    /** Kopie des Stabs, mit {@code shards} Amethystscherben aufgeladen. */
    public static ItemStack recharged(ItemStack stack, int shards) {
        ItemStack result = stack.copy();
        result.setDamageValue(Math.max(0, stack.getDamageValue() - shards * CHARGE_PER_SHARD));
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
        if (usedTicks % SPARK_PERIOD == 0) {
            emitSparks(player);
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

    /** Alle so viele Ticks sprueht der Stab Funken. */
    public static final int SPARK_PERIOD = 3;

    /**
     * Amethyst-Funken an der Spitze des Stabs, fuer alle anderen in der Naehe: etwas vor und rechts
     * unter den Augen (wo die Hand den Stab haelt). Der Benutzer selbst sieht die Funken seiner
     * ersten Person von {@code TweaksClient} an der gezeichneten Spitze - hier ausgenommen, sonst
     * schwebten sie fuer ihn neben der Hand.
     */
    public static void emitSparks(ServerPlayer player) {
        Vec3 tip = tipPosition(player.getEyePosition(), player.getViewVector(1.0f), player.getYRot(),
                player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT
                        == (player.getUsedItemHand() == InteractionHand.MAIN_HAND));
        net.minecraft.server.level.ServerLevel level = player.level();
        for (ServerPlayer other : level.players()) {
            if (other != player && other.distanceToSqr(tip) < 32 * 32) {
                level.sendParticles(other, SPARK, false, false, tip.x, tip.y, tip.z, 2, 0.03, 0.03, 0.03, 0.0);
            }
        }
    }

    /** Amethystlila Funkenstaub. */
    public static final net.minecraft.core.particles.DustParticleOptions SPARK =
            new net.minecraft.core.particles.DustParticleOptions(0xC890F0, 0.6f);

    /**
     * Wo die Spitze des gehaltenen Stabs ungefaehr ist: 0,6 Bloecke vor den Augen, 0,3 zur Seite der
     * haltenden Hand, 0,25 tiefer.
     */
    public static Vec3 tipPosition(Vec3 eye, Vec3 view, float yRot, boolean rightHand) {
        double yaw = Math.toRadians(yRot);
        Vec3 right = new Vec3(-Math.cos(yaw), 0.0, -Math.sin(yaw));
        return eye.add(view.scale(0.6)).add(right.scale(rightHand ? 0.3 : -0.3)).add(0.0, -0.25, 0.0);
    }

    /**
     * Worauf der Strahl zeigt: das erste Lebewesen innerhalb {@link LaserBeam#ENTITY_RANGE} vor dem
     * Block, sonst der Block ({@link #effectRange}), sonst ein Fehlschuss.
     */
    public static HitResult aim(ServerPlayer player) {
        return aim(player, effectRange(player), 1.0f);
    }

    /**
     * Wie {@link #aim(ServerPlayer)} mit gegebener Reichweite, auf Server und Client gleich (2026-10-02: der Punkt
     * und die Messung im HUD sitzen auf dem Lebewesen statt auf der Wand dahinter).
     */
    public static HitResult aim(Player player, double range, float partialTick) {
        HitResult hit = player.pick(range, partialTick, false);
        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 view = player.getViewVector(partialTick);
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
