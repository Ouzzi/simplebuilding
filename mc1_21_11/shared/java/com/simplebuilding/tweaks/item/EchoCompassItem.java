package com.simplebuilding.tweaks.item;

import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Echolot (en "Echo Sounder", Registry-Id weiter {@code echo_compass}; ersetzt das Datenpaket "Echo
 * Compass", das Simple Tweaks per libs/ einband; neu geschrieben, kein Code daraus). Rechtsklick auf
 * einen Leitstein verknuepft; zum Springen haelt man die Benutzen-Taste {@value #CHARGE_TICKS} Ticks
 * lang gedrueckt (Aufladen mit steigenden Klaengen, weit gestreuten Partikeln und einem FOV-Sog auf
 * dem Client). Laesst man vorher los, passiert nichts und nichts wird verbraucht. Seit 2026-09-27
 * kostet der Sprung keine Enderperle mehr (Besitzer) - bezahlt wird allein mit der Haltbarkeit.
 *
 * <p>Haltbarkeit (Besitzer 2026-09-27): ein Sprung leert den Kompass ganz - er ist dann "zerbrochen"
 * (Schaden = {@value #MAX_DAMAGE}, Riss-Textur, kein Glanz) und muss wieder aufgeladen werden: Mending
 * repariert 2 Punkte je XP-Punkt (750 XP fuer den leeren Kompass), am Amboss stellt jede Echoscherbe
 * ein Viertel wieder her (4 Scherben = voll). Unbreaking verringert den Verbrauch wie bei jedem
 * Werkzeug (Unbreaking III leert im Mittel nur ein Viertel). Erst ganz repariert springt er normal.
 * Wer den nicht voll reparierten Kompass trotzdem benutzt, provoziert den Bruch: doppelte Ladezeit
 * ({@value #CRACKED_CHARGE_TICKS} Ticks), Warnzeichen (Knacken, Rauch, Funken, Kreischer), der Sprung
 * gelingt, danach zerspringt der Kompass endgueltig - Unbreaking rettet ihn dabei nicht.
 * Im Kreativmodus nutzt er sich wie jedes Werkzeug nicht ab.
 */
public class EchoCompassItem extends Item {
    /** Reparaturpunkte eines ganz geleerten Kompasses (Besitzer: 1500). */
    public static final int MAX_DAMAGE = 1500;
    /** Ladezeit des voll reparierten Kompasses: 3 s. */
    public static final int CHARGE_TICKS = 60;
    /** Ladezeit des nicht voll reparierten Kompasses: doppelt so lang, danach zerspringt er. */
    public static final int CRACKED_CHARGE_TICKS = CHARGE_TICKS * 2;
    public static final int COOLDOWN_TICKS = 120;
    /** Ticks vor dem Sprung, zu denen der Warden-Ladeklang einsetzt (so lang ist er etwa). */
    private static final int SONIC_CHARGE_LEAD = 34;
    /** Kreisbahn der Sculk-Seelen beim Aufladen: startet weit aussen und zieht sich zusammen. */
    private static final double CHARGE_RADIUS_START = 2.4;
    private static final double CHARGE_RADIUS_END = 0.9;

    public EchoCompassItem(Item.Properties properties) {
        super(properties);
    }

    public static @Nullable GlobalPos target(ItemStack stack) {
        LodestoneTracker tracker = stack.get(DataComponents.LODESTONE_TRACKER);
        return tracker == null ? null : tracker.target().orElse(null);
    }

    /** Nicht voll repariert: zeigt die Riss-Textur, laedt doppelt so lange und zerspringt beim Sprung. */
    public static boolean isCracked(ItemStack stack) {
        return stack.isDamaged();
    }

    /** Ladezeit in Ticks fuer den aktuellen Zustand. */
    public static int chargeTicks(ItemStack stack) {
        return isCracked(stack) ? CRACKED_CHARGE_TICKS : CHARGE_TICKS;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        LodestoneTracker tracker = stack.get(DataComponents.LODESTONE_TRACKER);
        if (tracker != null) {
            LodestoneTracker updated = tracker.tick(level);
            if (updated != tracker) {
                stack.set(DataComponents.LODESTONE_TRACKER, updated);
            }
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!level.getBlockState(pos).is(Blocks.LODESTONE)) {
            return super.useOn(context);
        }
        if (!level.isClientSide()) {
            context.getItemInHand().set(DataComponents.LODESTONE_TRACKER,
                    new LodestoneTracker(Optional.of(GlobalPos.of(level.dimension(), pos)), true));
            level.playSound(null, pos, SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.PLAYERS, 1.0f, 1.0f);
            level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 1.5f);
            level.playSound(null, pos, SoundEvents.WARDEN_ATTACK_IMPACT, SoundSource.PLAYERS, 1.0f, 0.0f);
            if (level instanceof ServerLevel serverLevel) {
                spawnArrivalParticles(serverLevel, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
            }
            if (context.getPlayer() != null) {
                context.getPlayer().addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20, 0, true, false, true));
            }
        }
        return InteractionResult.SUCCESS;
    }

    // ---------------------------------------------------------------------------------------------
    // Aufladen
    // ---------------------------------------------------------------------------------------------

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return chargeTicks(stack);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            if (!canJump(serverPlayer, stack)) {
                return InteractionResult.FAIL;
            }
            player.startUsingItem(hand);
            playChargeStart((ServerLevel) level, player, isCracked(stack));
            return InteractionResult.CONSUME;
        }
        // Client: nur was er selbst weiss (Verknuepfung, Abklingzeit); den Rest prueft der Server.
        if (target(stack) == null || player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int ticksRemaining) {
        if (level instanceof ServerLevel serverLevel) {
            int total = chargeTicks(stack);
            chargeEffects(serverLevel, entity, total - ticksRemaining, total, isCracked(stack));
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            teleport(player, player.getUsedItemHand(), stack);
        }
        return stack;
    }

    /** Vorzeitig losgelassen: nichts passiert, nichts wird verbraucht - nur die Ladung verpufft hoerbar. */
    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingTime) {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BEACON_DEACTIVATE,
                    SoundSource.PLAYERS, 0.6f, 1.6f);
            serverLevel.sendParticles(ParticleTypes.SMOKE, entity.getX(), entity.getY() + 1.0, entity.getZ(), 8, 0.3, 0.4, 0.3, 0.01);
        }
        return false;
    }

    /** Alle Vorbedingungen des Sprungs (Server); meldet den Grund einer Ablehnung im Overlay. */
    private static boolean canJump(ServerPlayer player, ItemStack stack) {
        GlobalPos target = target(stack);
        if (target == null) {
            player.displayClientMessage(Component.translatable("message.simplebuilding.echo_compass.unlinked").withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (player.getCooldowns().isOnCooldown(stack)) {
            return false;
        }
        ServerLevel targetLevel = player.level().getServer().getLevel(target.dimension());
        if (targetLevel == null || !targetLevel.getBlockState(target.pos()).is(Blocks.LODESTONE)) {
            player.displayClientMessage(Component.translatable("message.simplebuilding.echo_compass.lodestone_missing").withStyle(ChatFormatting.RED), true);
            return false;
        }
        return true;
    }

    /**
     * Der eigentliche Sprung am Ende der Ladezeit; true, wenn teleportiert wurde. Danach ist der
     * Kompass leer (voll repariert) oder zersprungen (nicht voll repariert).
     */
    public static boolean teleport(ServerPlayer player, InteractionHand hand, ItemStack stack) {
        if (!canJump(player, stack)) {
            return false;
        }
        GlobalPos target = target(stack);
        ServerLevel targetLevel = player.level().getServer().getLevel(target.dimension());
        ServerLevel origin = player.level();
        double fromX = player.getX();
        double fromY = player.getY();
        double fromZ = player.getZ();
        double x = target.pos().getX() + 0.5;
        double y = target.pos().getY() + 1.0;
        double z = target.pos().getZ() + 0.5;
        // Erst springen, dann bezahlen: ein blockierter Sprung (gesperrte Dimension, anderer Mod)
        // kostete frueher Haltbarkeit und Abklingzeit (Audit #33).
        if (!player.teleportTo(targetLevel, x, y, z, Set.of(), player.getYRot(), player.getXRot(), true)) {
            return false;
        }
        com.simplebuilding.advancement.ModTriggers.feature(player, com.simplebuilding.advancement.ModTriggers.ECHO_TELEPORT);
        origin.playSound(null, fromX, fromY, fromZ, SoundEvents.PLAYER_TELEPORT, SoundSource.PLAYERS, 1.0f, 0.5f);
        origin.sendParticles(ParticleTypes.REVERSE_PORTAL, fromX, fromY + 1.0, fromZ, 80, 1.1, 1.1, 1.1, 0.08);
        origin.sendParticles(ParticleTypes.SCULK_SOUL, fromX, fromY + 0.5, fromZ, 24, 1.0, 0.7, 1.0, 0.03);
        targetLevel.playSound(null, x, y, z, SoundEvents.PLAYER_TELEPORT, SoundSource.PLAYERS, 1.0f, 0.0f);
        targetLevel.playSound(null, x, y, z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.0f, 1.0f);
        targetLevel.playSound(null, x, y, z, SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.PLAYERS, 0.9f, 1.2f);
        spawnArrivalParticles(targetLevel, x, y, z);
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20, 0, true, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, true, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20, 0, true, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, 9, true, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 120, 0, true, false, true));

        player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
        wear(player, hand, stack, targetLevel, x, y, z);
        return true;
    }

    /** Abnutzung nach einem gelungenen Sprung: leeren (voll repariert) oder zerspringen (sonst). */
    private static void wear(ServerPlayer player, InteractionHand hand, ItemStack stack, ServerLevel level, double x, double y, double z) {
        if (player.hasInfiniteMaterials()) {
            return;
        }
        if (isCracked(stack)) {
            stack.shrink(1);
            // Dasselbe Ereignis wie LivingEntity#onEquippedItemBroken (Bruchklang + Splitter des Items).
            level.broadcastEntityEvent(player, hand == InteractionHand.MAIN_HAND ? (byte) 47 : (byte) 48);
            level.playSound(null, x, y, z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0f, 0.7f);
            level.playSound(null, x, y, z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.0f, 0.5f);
            level.sendParticles(ParticleTypes.SCULK_SOUL, x, y + 1.0, z, 30, 0.3, 0.3, 0.3, 0.08);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y + 1.0, z, 12, 0.3, 0.3, 0.3, 0.02);
            return;
        }
        // Unbreaking wirkt wie bei jedem Werkzeug je Punkt; ueber hurtAndBreak ginge es nicht, weil
        // Schaden = Maximalwert dort den Bruch ausloest.
        int damage = EnchantmentHelper.processDurabilityChange(level, stack, MAX_DAMAGE);
        stack.setDamageValue(Math.min(stack.getMaxDamage(), stack.getDamageValue() + damage));
        if (isCracked(stack)) {
            level.playSound(null, x, y, z, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.PLAYERS, 1.0f, 1.0f);
            level.playSound(null, x, y, z, SoundEvents.GLASS_HIT, SoundSource.PLAYERS, 1.0f, 0.6f);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Klang und Partikel
    // ---------------------------------------------------------------------------------------------

    private static void playChargeStart(ServerLevel level, LivingEntity entity, boolean cracked) {
        sound(level, entity, SoundEvents.SCULK_CATALYST_BLOOM, 1.0f, 0.8f);
        sound(level, entity, SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.5f, 0.6f);
        if (cracked) {
            sound(level, entity, SoundEvents.GLASS_HIT, 1.0f, 0.5f);
        }
    }

    /**
     * Ein Ladetick: Sculk-Seelen kreisen in drei Armen von weit aussen (gut zwei Bloecke) immer enger
     * um den Spieler, weit gestreute Portalpartikel ziehen hinein (Besitzer 2026-09-27: "weiter
     * gestreut"), ein
     * Amethyst-Resonanzton steigt von tief nach hoch, Sculk-Klicken und Seelenanker-Aufladen markieren
     * die Drittel, zum Schluss laedt der Warden-Schallklang auf. Nicht voll repariert kommen Knacken,
     * Rauch, Funken und ein Kreischer zur Haelfte dazu, alles lauter.
     */
    private static void chargeEffects(ServerLevel level, LivingEntity entity, int elapsed, int total, boolean cracked) {
        float progress = Mth.clamp(elapsed / (float) total, 0.0f, 1.0f);
        float loud = cracked ? 1.5f : 1.0f;
        double cx = entity.getX();
        double cz = entity.getZ();
        if (elapsed % 2 == 0) {
            double angle = elapsed * 0.4;
            double radius = CHARGE_RADIUS_START - (CHARGE_RADIUS_START - CHARGE_RADIUS_END) * progress;
            double height = entity.getY() + 0.1 + 2.2 * ((elapsed * 0.04) % 1.0);
            for (int arm = 0; arm < 3; arm++) {
                double a = angle + arm * (Math.PI * 2.0 / 3.0);
                particle(level, ParticleTypes.SCULK_SOUL, cx + Math.cos(a) * radius, height, cz + Math.sin(a) * radius, 1, 0.08, 0.0);
            }
        }
        if (elapsed % 3 == 0) {
            particle(level, ParticleTypes.PORTAL, cx, entity.getY() + 1.0, cz, 5 + Math.round(12 * progress), 1.6, 0.8);
        }
        if (progress > 0.66f && elapsed % 2 == 0) {
            particle(level, ParticleTypes.END_ROD, cx, entity.getY() + 1.0, cz, 2, 1.2, 0.02);
        }
        if (elapsed % 5 == 0) {
            sound(level, entity, SoundEvents.AMETHYST_BLOCK_RESONATE, (0.4f + 0.5f * progress) * loud, 0.5f + 1.5f * progress);
        }
        if (elapsed % 10 == 0) {
            sound(level, entity, SoundEvents.SCULK_CLICKING, 0.6f * loud, 0.6f + progress);
        }
        if (elapsed == total / 3 || elapsed == 2 * total / 3) {
            sound(level, entity, SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.7f * loud, elapsed == total / 3 ? 0.8f : 1.1f);
            particle(level, ParticleTypes.REVERSE_PORTAL, cx, entity.getY() + 1.0, cz, 40, 1.4, 0.07);
        }
        if (elapsed == total - SONIC_CHARGE_LEAD) {
            sound(level, entity, SoundEvents.WARDEN_SONIC_CHARGE, 0.8f * loud, cracked ? 0.9f : 1.2f);
        }
        if (!cracked) {
            return;
        }
        // Warnzeichen: das Knacken wird dichter, je naeher der Bruch kommt.
        Vec3 hand = entity.getEyePosition().add(entity.getLookAngle().scale(0.6)).add(0.0, -0.35, 0.0);
        int crackEvery = Math.max(3, 12 - Math.round(9 * progress));
        if (elapsed % crackEvery == 0) {
            sound(level, entity, elapsed % 2 == 0 ? SoundEvents.GLASS_HIT : SoundEvents.AMETHYST_CLUSTER_BREAK,
                    0.5f + 0.7f * progress, 0.6f + 0.6f * level.getRandom().nextFloat());
            particle(level, ParticleTypes.ELECTRIC_SPARK, hand.x, hand.y, hand.z, 4, 0.12, 0.2);
            particle(level, ParticleTypes.SMOKE, hand.x, hand.y, hand.z, 3, 0.1, 0.01);
        }
        if (elapsed == total / 2) {
            sound(level, entity, SoundEvents.SCULK_SHRIEKER_SHRIEK, 0.7f, 1.4f);
        }
        if (total - elapsed <= 20) {
            particle(level, ParticleTypes.CRIT, hand.x, hand.y, hand.z, 2, 0.15, 0.1);
            particle(level, ParticleTypes.LARGE_SMOKE, hand.x, hand.y, hand.z, 1, 0.05, 0.01);
        }
    }

    private static void sound(ServerLevel level, LivingEntity entity, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    private static void particle(ServerLevel level, ParticleOptions type, double x, double y, double z, int count, double spread, double speed) {
        level.sendParticles(type, x, y, z, count, spread, spread, spread, speed);
    }

    /** Landung: weite Wolke (gut zwei Bloecke) und ein Ring aus Sculk-Seelen, der am Boden auseinanderlaeuft. */
    private static void spawnArrivalParticles(ServerLevel level, double x, double y, double z) {
        level.sendParticles(ParticleTypes.PORTAL, x, y, z, 120, 1.4, 1.2, 1.4, 0.2);
        level.sendParticles(ParticleTypes.END_ROD, x, y + 0.5, z, 50, 1.3, 1.1, 1.3, 0.06);
        level.sendParticles(ParticleTypes.SCULK_SOUL, x, y + 0.5, z, 40, 1.2, 1.0, 1.2, 0.05);
        int ring = 28;
        for (int i = 0; i < ring; i++) {
            double a = i * (Math.PI * 2.0 / ring);
            // count 0: die Werte sind eine Richtung - jedes Teilchen fliegt vom Mittelpunkt nach aussen.
            level.sendParticles(ParticleTypes.SCULK_SOUL, x + Math.cos(a) * 0.6, y + 0.1, z + Math.sin(a) * 0.6, 0,
                    Math.cos(a), 0.02, Math.sin(a), 0.18);
        }
        level.sendParticles(ParticleTypes.SONIC_BOOM, x, y + 1.0, z, 1, 0, 0, 0, 0);
    }

    // ---------------------------------------------------------------------------------------------
    // Anzeige
    // ---------------------------------------------------------------------------------------------

    /** Glanz nur voll repariert - auch verzaubert (Mending) glaenzt der leere Kompass nicht. */
    @Override
    public boolean isFoil(ItemStack stack) {
        return !isCracked(stack);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> builder, TooltipFlag flag) {
        if (isCracked(stack)) {
            int percent = Math.round(100.0f * (stack.getMaxDamage() - stack.getDamageValue()) / stack.getMaxDamage());
            builder.accept(Component.translatable("item.simplebuilding.echo_compass.cracked", percent).withStyle(ChatFormatting.RED));
            builder.accept(Component.translatable("item.simplebuilding.echo_compass.cracked_hint").withStyle(ChatFormatting.GRAY));
        } else {
            builder.accept(Component.translatable("item.simplebuilding.echo_compass.charged").withStyle(ChatFormatting.DARK_AQUA));
        }
    }
}
