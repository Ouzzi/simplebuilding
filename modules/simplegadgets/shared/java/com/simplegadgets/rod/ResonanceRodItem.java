package com.simplegadgets.rod;

import com.simplegadgets.GadgetComponents;
import com.simplegadgets.GadgetEnchantments;
import com.simplegadgets.config.GadgetsConfig;
import com.simplelib.api.AnvilRechargeable;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
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
 * Resonance Rod (registry id {@code amethyst_lens} stays; until 2026-09-29 "Amethyst Lens", until 2026-09-28 laser_pointer; moved
 * from SimpleBuilding's Simple-Tweaks part, OW24-2 G2). Holding use shows a dot everybody nearby sees; when the beam rests on a
 * block it acts there ({@link RodBeam}: melt, ignite, dry, TNT, set creatures alight - the dwell grows with the distance, sounds
 * at the hit point). In use the rod tilts forward and sprays amethyst sparks ({@link #emitSparks}); the crosshair steps aside, the
 * dot sits exactly in its middle.
 *
 * <p>The durability is the charge: beaming costs {@link #BEAM_COST} per started second - even mere pointing, into the air or at a
 * block without effect, and the very first tick - and every block effect {@link #EFFECT_COST} more. The rod never breaks, it only
 * runs empty and then does not beam. Recharging at the anvil with its material, amethyst shards, costs no levels: 16 shards
 * charge it fully ({@link #CHARGE_PER_SHARD} each). Mending stays refused.
 *
 * <p>Measuring (owner 2026-09-28): without an enchantment no text, only the dot. With Constructor's Touch (anvil + book, as on the
 * Gauge) the rod becomes a rangefinder: the HUD shows distance, target block and height in the shared readout box, and the server
 * writes the last measurement ({@link LensMeasurement}) into the rod - on the first tick, then every second and on release; the
 * tooltip shows it.
 */
public class ResonanceRodItem extends Item implements AnvilRechargeable {
    /** Default full charge. */
    public static final int DEFAULT_MAX_CHARGE = GadgetsConfig.ROD_CHARGE_DEFAULT;
    /** Full charge (= durability): {@code rodMaxCharge}, read when registering (restart needed; client and server need the same value). */
    public static final int MAX_CHARGE = GadgetsConfig.rodMaxCharge;
    /** Shards for a full charge. */
    public static final int SHARDS_PER_FULL_CHARGE = 16;
    /** One amethyst shard charges 1/16 of the full charge (10 s of beam). */
    public static final int CHARGE_PER_SHARD = MAX_CHARGE / SHARDS_PER_FULL_CHARGE;
    /** Charge per second of beaming (even just pointing); 2026-09-29 from 1 to 4. */
    public static final int BEAM_COST = GadgetsConfig.ROD_BEAM_COST_DEFAULT;
    /** Charge per triggered block effect. */
    public static final int EFFECT_COST = GadgetsConfig.ROD_EFFECT_COST_DEFAULT;
    /** The tag of the items that charge the rod (amethyst shards). */
    public static final TagKey<Item> REPAIRS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("simplegadgets", "repairs_resonance_rod"));

    /**
     * How far the beam acts on the server: the rod's range (config {@code rodRange}), at most the server's view distance - the
     * chunks around the player are loaded there, so the beam loads none. At least 2 chunks, since a server without a set view
     * distance (game test server) reports 0.
     */
    public static double effectRange(ServerPlayer player) {
        int viewBlocks = Math.max(2, player.level().getServer().getPlayerList().getViewDistance()) * 16;
        return Math.min(GadgetsConfig.rodRange, viewBlocks);
    }

    public ResonanceRodItem(Item.Properties properties) {
        super(properties.repairable(REPAIRS));
    }

    @Override
    public boolean isRechargeMaterial(ItemStack material) {
        return material.is(REPAIRS);
    }

    @Override
    public int chargePerMaterial() {
        return CHARGE_PER_SHARD;
    }

    /** Charge per started second of beaming: config {@code rodChargePerSecond} (default {@link #BEAM_COST}). */
    public static int beamCost() {
        return GadgetsConfig.rodChargePerSecond;
    }

    /** Charge per effect: config {@code rodEffectCost} (default {@link #EFFECT_COST}). */
    public static int effectCost() {
        return GadgetsConfig.rodEffectCost;
    }

    /** Empty: the whole charge is used up (damage = durability). */
    public static boolean isEmpty(ItemStack stack) {
        return stack.isDamageableItem() && stack.getDamageValue() >= stack.getMaxDamage();
    }

    /** Takes charge, never beyond empty - the rod does not break. Creative costs nothing. */
    public static void drain(Player player, ItemStack stack, int amount) {
        if (player.hasInfiniteMaterials() || !stack.isDamageableItem()) {
            return;
        }
        stack.setDamageValue(Math.min(stack.getMaxDamage(), stack.getDamageValue() + amount));
    }

    /** How many amethyst shards (at most {@code available}) the rod needs to be full; 0 = already full. */
    public static int shardsNeeded(ItemStack stack, int available) {
        int missing = stack.getDamageValue();
        return Math.max(0, Math.min(available, (missing + CHARGE_PER_SHARD - 1) / CHARGE_PER_SHARD));
    }

    /** A copy of the rod, recharged with {@code shards} amethyst shards. */
    public static ItemStack recharged(ItemStack stack, int shards) {
        ItemStack result = stack.copy();
        result.setDamageValue(Math.max(0, stack.getDamageValue() - shards * CHARGE_PER_SHARD));
        return result;
    }

    /** Whether the rod may beam now: switched on (server config; on the client its own file) and not empty. */
    public static boolean canBeam(ItemStack stack) {
        return GadgetsConfig.rodEnabled && !isEmpty(stack);
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
                RodBeam.reset(player);
            }
            return;
        }
        if (!(user instanceof ServerPlayer player)) {
            return;
        }
        // Per started second, beginning with the first tick: every way to use the rod costs charge - tapping and pointing
        // into the air too.
        int usedTicks = getUseDuration(stack, user) - ticksRemaining;
        HitResult target = aim(player);
        if (usedTicks >= 0 && usedTicks % 20 == 0) {
            drain(player, stack, beamCost());
            recordMeasurement(player, stack, target);
        }
        if (usedTicks % SPARK_PERIOD == 0) {
            emitSparks(player);
        }
        boolean hum = usedTicks % RodBeam.HUM_PERIOD == 0;
        if (target instanceof EntityHitResult entityHit) {
            if (hum) {
                RodBeam.hum(player, entityHit.getLocation());
            }
            RodBeam.beamAtEntity(player, stack, entityHit);
        } else if (target instanceof BlockHitResult blockHit && target.getType() == HitResult.Type.BLOCK) {
            if (hum) {
                RodBeam.hum(player, blockHit.getLocation());
            }
            RodBeam.beamAt(player, stack, blockHit);
        } else {
            RodBeam.reset(player);
        }
    }

    /** Every this many ticks the rod sprays sparks. */
    public static final int SPARK_PERIOD = 3;

    /**
     * Amethyst sparks at the rod's tip, for everybody else nearby: a bit forward and right below the eyes (where the hand holds
     * the rod). The user sees the sparks of his own first person at the drawn tip ({@code RodClient}) - excluded here, or they would
     * float beside his hand.
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

    /** Amethyst-lilac spark dust. */
    public static final net.minecraft.core.particles.DustParticleOptions SPARK =
            new net.minecraft.core.particles.DustParticleOptions(0xC890F0, 0.6f);

    /** Where the tip of the held rod roughly is: 0.6 blocks before the eyes, 0.3 to the side of the holding hand, 0.25 lower. */
    public static Vec3 tipPosition(Vec3 eye, Vec3 view, float yRot, boolean rightHand) {
        double yaw = Math.toRadians(yRot);
        Vec3 right = new Vec3(-Math.cos(yaw), 0.0, -Math.sin(yaw));
        return eye.add(view.scale(0.6)).add(right.scale(rightHand ? 0.3 : -0.3)).add(0.0, -0.25, 0.0);
    }

    /** What the beam points at: the first creature within {@link RodBeam#ENTITY_RANGE} before the block, else the block ({@link #effectRange}), else a miss. */
    public static HitResult aim(ServerPlayer player) {
        return aim(player, effectRange(player), 1.0f);
    }

    /**
     * Like {@link #aim(ServerPlayer)} with a given range, the same on server and client (2026-10-02: the dot and the HUD
     * measurement sit on the creature instead of the wall behind it).
     */
    public static HitResult aim(Player player, double range, float partialTick) {
        HitResult hit = player.pick(range, partialTick, false);
        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 view = player.getViewVector(partialTick);
        double reach = Math.min(RodBeam.ENTITY_RANGE, hit.getType() == HitResult.Type.MISS ? range : hit.getLocation().distanceTo(eye));
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(player, eye, eye.add(view.scale(reach)),
                player.getBoundingBox().expandTowards(view.scale(reach)).inflate(1.0),
                entity -> entity instanceof LivingEntity && !entity.isSpectator() && entity.isPickable(), reach * reach);
        return entityHit != null ? entityHit : hit;
    }

    /** Whether the rod measures: only with Constructor's Touch. */
    public static boolean measures(ItemStack stack, Level level) {
        return GadgetEnchantments.hasConstructorsTouch(stack, level);
    }

    /**
     * Writes the measurement to {@code target} as {@link GadgetComponents#LENS_MEASUREMENT} into the rod - only with Constructor's
     * Touch and only on a hit. Returns whether it was written.
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
        stack.set(GadgetComponents.LENS_MEASUREMENT,
                new LensMeasurement(distance, targetY - Mth.floor(player.getY()), key));
        return true;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity user, int ticksRemaining) {
        if (user instanceof ServerPlayer player) {
            RodBeam.reset(player);
            if (canBeam(stack)) {
                recordMeasurement(player, stack, aim(player));
            }
        }
        return super.releaseUsing(stack, level, user, ticksRemaining);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
        lines.accept(Component.translatable("tooltip.simplegadgets.amethyst_lens.repair").withStyle(ChatFormatting.GRAY));
        LensMeasurement last = stack.get(GadgetComponents.LENS_MEASUREMENT);
        if (last != null) {
            lines.accept(Component.translatable("tooltip.simplegadgets.amethyst_lens.last_measured",
                    String.format("%.1f", last.distance())).withStyle(ChatFormatting.GRAY));
            lines.accept(Component.translatable("tooltip.simplegadgets.amethyst_lens.last_target",
                    Component.translatable(last.target()), signed(last.heightDifference())).withStyle(ChatFormatting.DARK_GRAY));
        } else if (!stack.isEnchanted()) {
            lines.accept(Component.translatable("tooltip.simplegadgets.amethyst_lens.touch_hint").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    /** Height difference with a sign: "+3", "-2", "0". */
    public static String signed(int value) {
        return value > 0 ? "+" + value : Integer.toString(value);
    }
}
