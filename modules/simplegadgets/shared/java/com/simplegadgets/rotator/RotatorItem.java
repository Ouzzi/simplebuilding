package com.simplegadgets.rotator;

import com.simplegadgets.config.GadgetsConfig;
import com.simplegadgets.GadgetProtection;
import com.simplelib.api.AnvilRechargeable;
import com.simplelib.api.GadgetActions;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Rotator: dreht Bloecke mit Achse, Blickrichtung oder 16 Stufen.
 *
 * <p><b>Ladung statt Haltbarkeit</b> (Besitzer 2026-09-28, wie die Amethystlinse): die Haltbarkeit
 * ist die Ladung, {@link #MAX_CHARGE} = 1024 Drehungen. Jede Drehung kostet {@link #USE_COST}
 * (Unbreaking spart wie bei jedem Werkzeug, Kreativ kostet nichts). Der Rotator zerbricht nie: leer
 * dreht er nichts mehr und zeigt das Leer-Bild ({@code item/rotator_empty}). Aufladen im Amboss mit
 * Enderperlen, ohne Stufenkosten: 16 Perlen (ein Stapel) laden ganz auf, jede Perle
 * {@link #CHARGE_PER_PEARL} = 1/16; verbraucht wird nur, was bis voll fehlt.
 * Unbreaking ja, Mending nein (Besitzer 2026-09-28): Erfahrung laedt keine Ladung auf, der Rotator
 * steht in {@code simplebuilding:xp_repair_incompatible} ({@code EnchantmentMixin}).
 *
 * <p><b>Klang:</b> zuerst das metallische Ratschen (Fernrohr), kurz bevor es verklingt
 * ({@link #ECHO_DELAY_TICKS} spaeter) ein leises Ender-Teleport - das erste Ratschen ist das Metall,
 * das zweite sagt, dass etwas teleportiert wurde. Bei jeder Drehung Portal-Partikel am Block, beim
 * zweiten Klang ein paar umgekehrte Portal-Partikel.
 *
 * <p><b>Zweite Funktion: Tauschen</b> (Queue Nachtrag 63): ein platzierbarer Block in der Nebenhand + Rechtsklick
 * tauscht den angeklickten Block gegen ihn ({@link RotatorSwap}: Regeln, Listen, Ausfuehrung).
 * Vorher holt der Rotator aus (Klick halten, {@link RotatorSwap#windupTicks}: 1 s, je Blockhaerte
 * etwas laenger, Effizienz verkuerzt, V sofort); die Hand zieht wie beim Bogen zurueck, das Item dreht sich
 * ({@code rotator_active}), Tonhoehe und Partikel steigen mit dem Fortschritt. Ein Tausch kostet doppelt so viel
 * Ladung wie eine Drehung. Loslassen oder Wegschauen bricht ab, nichts passiert.
 */
public class RotatorItem extends Item implements AnvilRechargeable {
    /** Standard der vollen Ladung in Drehungen. */
    public static final int DEFAULT_MAX_CHARGE = 1024;
    /**
     * Volle Ladung (= Haltbarkeit) in Drehungen: {@code server.charges.rotatorMaxCharge}, beim
     * Registrieren gelesen (Neustart noetig; Client und Server brauchen denselben Wert).
     */
    public static final int MAX_CHARGE = GadgetsConfig.rotatorMaxCharge;
    /** So viele Enderperlen laden einen leeren Rotator ganz auf - ein voller Perlenstapel. */
    public static final int PEARLS_FOR_FULL = 16;
    /** Ladung je Enderperle (1/16 der vollen Ladung). */
    public static final int CHARGE_PER_PEARL = MAX_CHARGE / PEARLS_FOR_FULL;
    /** Ladung je Drehung. */
    public static final int USE_COST = 1;
    /** Abstand zwischen Ratschen und Ender-Klang: das Fernrohr-Ratschen klingt nach etwa 0,4 s aus. */
    public static final int ECHO_DELAY_TICKS = 6;
    public static final float RATCHET_VOLUME = 1.0f;
    public static final float ECHO_VOLUME = 0.3f;
    public static final float ECHO_PITCH = 1.6f;
    /** Portal-Partikel je Drehung am Block. */
    public static final int PORTAL_PARTICLES = 14;
    /** Umgekehrte Portal-Partikel beim Ender-Klang. */
    public static final int ECHO_PARTICLES = 5;

    /** Ausstehende Ender-Klaenge je Spieler; abgearbeitet im {@link #inventoryTick} des Rotators. */
    private static final Map<UUID, PendingEcho> PENDING_ECHOES = new HashMap<>();

    private record PendingEcho(ResourceKey<Level> dimension, BlockPos pos, long dueTick) {
    }

    public RotatorItem(Properties settings) {
        super(settings);
    }

    /** Publishes the numbers SimpleBuilding's in-world catalogue shows (SimpleLib {@link GadgetActions#fact}). */
    public static void publishFacts() {
        GadgetActions.setFact("rotator.useCost", USE_COST);
        GadgetActions.setFact("rotator.maxCharge", MAX_CHARGE);
        GadgetActions.setFact("rotator.pearlsForFull", PEARLS_FOR_FULL);
    }

    @Override
    public boolean isRechargeMaterial(ItemStack material) {
        return material.is(Items.ENDER_PEARL);
    }

    @Override
    public int chargePerMaterial() {
        return CHARGE_PER_PEARL;
    }

    /** Ladung je Drehung: Config {@code rotatorChargePerTurn} (Standard {@link #USE_COST}), 0 = kostenlos. */
    public static int useCost() {
        return GadgetsConfig.rotatorChargePerTurn;
    }

    /** Leer: die ganze Ladung ist verbraucht, der Rotator dreht nichts mehr. */
    public static boolean isEmpty(ItemStack stack) {
        return AnvilRechargeable.isEmpty(stack);
    }

    /**
     * Zieht Ladung ab, nie ueber leer hinaus - der Rotator zerbricht nicht. Unbreaking wirkt wie bei
     * {@code hurtAndBreak}, Kreativ kostet nichts.
     */
    public static void drain(Player player, ItemStack stack, int amount) {
        if (player.hasInfiniteMaterials() || !stack.isDamageableItem() || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        int cost = net.minecraft.world.item.enchantment.EnchantmentHelper.processDurabilityChange(level, stack, amount);
        if (cost > 0) {
            stack.setDamageValue(Math.min(stack.getMaxDamage(), stack.getDamageValue() + cost));
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = world.getBlockState(pos);
        Player player = context.getPlayer();
        // Dieselben Rechte wie beim Setzen eines Blocks (Abenteuermodus, Spawnschutz, Claims) - die
        // Hand-Neigung (TransformTargets) fragt genauso.
        if (player != null && !GadgetProtection.mayTransform(world, player, pos,
                context.getClickedFace(), context.getItemInHand())) {
            return InteractionResult.PASS;
        }
        InteractionResult swap = trySwap(context, world, pos, player);
        if (swap != null) {
            return swap;
        }
        BlockState newState = rotatedState(context);

        if (newState != null) {
            // Leer: nichts drehen, nur ein trockenes Klicken (beide Seiten entscheiden gleich).
            if (isEmpty(context.getItemInHand())) {
                if (!world.isClientSide()) {
                    world.playSound(null, pos, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 0.4f, 1.6f);
                }
                return InteractionResult.FAIL;
            }
            if (!world.isClientSide()) {
                world.setBlock(pos, newState, Block.UPDATE_ALL);
                GadgetActions.fire(GadgetActions.ROTATE, player);
                world.playSound(null, pos, SoundEvents.SPYGLASS_USE, SoundSource.BLOCKS, RATCHET_VOLUME, 1.0f);
                if (world instanceof ServerLevel serverLevel) {
                    Vec3 c = Vec3.atCenterOf(pos);
                    serverLevel.sendParticles(ParticleTypes.PORTAL, c.x, c.y, c.z, PORTAL_PARTICLES, 0.35, 0.35, 0.35, 0.35);
                    if (player != null) {
                        PENDING_ECHOES.put(player.getUUID(), new PendingEcho(world.dimension(), pos.immutable(),
                                serverLevel.getGameTime() + ECHO_DELAY_TICKS));
                    }
                }

                if (player != null) {
                    drain(player, context.getItemInHand(), useCost());
                }
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    // ------------------------------------------------------------------ zweite Funktion: Tauschen

    /**
     * Der Block, auf dem ein Ausholen begann, je Spieler und Seite (wie beim Hammer: {@code Entity#equals}
     * vergleicht Ids, im Einzelspieler teilen Client- und Server-Spieler eine - eine Map je Seite).
     */
    private static final Map<Player, SwapTarget> SERVER_TARGETS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Player, SwapTarget> CLIENT_TARGETS = Collections.synchronizedMap(new WeakHashMap<>());

    private static Map<Player, SwapTarget> targets(Player player) {
        return player.level().isClientSide() ? CLIENT_TARGETS : SERVER_TARGETS;
    }

    private record SwapTarget(BlockPos pos, BlockState state, Direction face, Vec3 hit) {
    }

    /**
     * Der Tausch-Klick: Rotator in der Haupthand, platzierbarer Block in der Nebenhand, tauschbarer Block
     * angeklickt. Liefert null, wenn das kein Tausch ist (dann dreht der Rotator wie bisher); sofort bei
     * Effizienz V und im Kreativmodus, sonst beginnt das Ausholen.
     */
    private @Nullable InteractionResult trySwap(UseOnContext context, Level world, BlockPos pos, @Nullable Player player) {
        ItemStack rotator = context.getItemInHand();
        if (player == null || context.getHand() != InteractionHand.MAIN_HAND
                || !RotatorSwap.canSwap(world, pos, player.getOffhandItem())) {
            return null;
        }
        if (isEmpty(rotator)) {
            if (!world.isClientSide()) {
                world.playSound(null, pos, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 0.4f, 1.6f);
            }
            return InteractionResult.FAIL;
        }
        int ticks = RotatorSwap.windupTicks(world, player, rotator, pos);
        if (ticks <= 0) {
            if (world instanceof ServerLevel serverLevel) {
                return finishSwap(serverLevel, player, pos, context.getClickedFace(), context.getClickLocation(), rotator)
                        ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            }
            return InteractionResult.SUCCESS;
        }
        targets(player).put(player, new SwapTarget(pos.immutable(), world.getBlockState(pos), context.getClickedFace(),
                context.getClickLocation()));
        player.startUsingItem(context.getHand());
        return InteractionResult.CONSUME;
    }

    private static boolean finishSwap(ServerLevel level, Player player, BlockPos pos, Direction face, Vec3 hit, ItemStack rotator) {
        if (!RotatorSwap.perform(level, player, pos, face, hit, rotator)) {
            return false;
        }
        GadgetActions.fire(GadgetActions.ROTATE, player);
        player.getCooldowns().addCooldown(rotator, RotatorSwap.COOLDOWN_TICKS);
        return true;
    }

    /** Klang und Partikel des fertigen Tauschs: das Ratschen, Portal-Partikel und der Ender-Nachklang wie beim Drehen. */
    public static void swapEffects(ServerLevel level, Player player, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.SPYGLASS_USE, SoundSource.BLOCKS, RATCHET_VOLUME, 1.0f);
        Vec3 c = Vec3.atCenterOf(pos);
        level.sendParticles(ParticleTypes.PORTAL, c.x, c.y, c.z, PORTAL_PARTICLES, 0.35, 0.35, 0.35, 0.35);
        PENDING_ECHOES.put(player.getUUID(), new PendingEcho(level.dimension(), pos.immutable(), level.getGameTime() + ECHO_DELAY_TICKS));
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        if (user instanceof Player player) {
            SwapTarget target = targets(player).get(player);
            if (target != null) {
                return Math.max(1, RotatorSwap.windupTicks(user.level(), player, stack, target.pos()));
            }
        }
        return 0;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    /**
     * Das Ausholen, Server: bricht ab, sobald der Spieler den Block nicht mehr anschaut oder sich Block,
     * Nebenhand oder Ladung geaendert haben; sonst steigt mit dem Fortschritt die Tonhoehe eines leisen
     * Klickens und es sammeln sich mehr Portal-Partikel am Block (kein Text auf dem Bildschirm).
     */
    @Override
    public void onUseTick(Level world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (!(user instanceof Player player) || !(world instanceof ServerLevel level)) {
            return;
        }
        SwapTarget target = targets(player).get(player);
        if (target == null) {
            return;
        }
        if (!stillValid(level, player, target, stack)) {
            targets(player).remove(player);
            player.stopUsingItem();
            return;
        }
        int total = getUseDuration(stack, user);
        int elapsed = total - remainingUseTicks;
        if (elapsed > 0 && elapsed % 4 == 0) {
            float progress = total <= 0 ? 1.0f : Math.min(1.0f, (float) elapsed / total);
            level.playSound(null, target.pos(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.5f, 0.7f + progress * 1.3f);
            Vec3 c = Vec3.atCenterOf(target.pos());
            level.sendParticles(ParticleTypes.PORTAL, c.x, c.y, c.z, 1 + (int) (progress * 5), 0.4, 0.4, 0.4, 0.2);
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level world, LivingEntity user, int remainingUseTicks) {
        if (user instanceof Player player) {
            targets(player).remove(player); // abgebrochen: nichts passiert
        }
        return false;
    }

    /** Ausholen fertig, Server: tauscht genau den Block, auf dem es begann, solange er angeschaut und tauschbar ist. */
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user) {
        if (!(user instanceof Player player)) {
            return stack;
        }
        SwapTarget target = targets(player).remove(player);
        if (target != null && world instanceof ServerLevel level && stillValid(level, player, target, stack)) {
            finishSwap(level, player, target.pos(), target.face(), target.hit(), stack);
        }
        return stack;
    }

    /** Derselbe Block steht noch, der Spieler zielt darauf, darf ihn aendern und hat Ladung und Nebenhand-Block. */
    private static boolean stillValid(ServerLevel level, Player player, SwapTarget target, ItemStack rotator) {
        if (isEmpty(rotator) || level.getBlockState(target.pos()) != target.state()
                || !RotatorSwap.canSwap(level, target.pos(), player.getOffhandItem())) {
            return false;
        }
        HitResult hit = player.pick(player.blockInteractionRange(), 1.0f, false);
        return hit.getType() == HitResult.Type.BLOCK && ((BlockHitResult) hit).getBlockPos().equals(target.pos())
                && GadgetProtection.mayTransform(level, player, target.pos(), target.face(), rotator);
    }

    /** Aufraeumen bei Disconnect (Review F-06). */
    public static void onDisconnect(Player player) {
        PENDING_ECHOES.remove(player.getUUID());
    }

    /** Aufraeumen beim Server-Stopp (Review F-06). */
    public static void clearAll() {
        PENDING_ECHOES.clear();
    }

    /** Nur fuer Tests. */
    public static int pendingEchoCount() {
        return PENDING_ECHOES.size();
    }

    /** Spielt den faelligen Ender-Klang des Spielers, der diesen Rotator traegt. */
    @Override
    public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, @Nullable EquipmentSlot slot) {
        if (entity instanceof Player player && !PENDING_ECHOES.isEmpty()) {
            playDueEcho(world, player);
        }
    }

    /**
     * Der zweite Klang: wenn {@link #ECHO_DELAY_TICKS} nach einer Drehung verstrichen sind, ein leises
     * Ender-Teleport am gedrehten Block plus ein paar umgekehrte Portal-Partikel. Liefert, ob er jetzt
     * gespielt wurde. Oeffentlich fuer die Tests.
     */
    public static boolean playDueEcho(ServerLevel world, Player player) {
        PendingEcho echo = PENDING_ECHOES.get(player.getUUID());
        if (echo == null || world.getGameTime() < echo.dueTick()) {
            return false;
        }
        PENDING_ECHOES.remove(player.getUUID());
        if (echo.dimension() != world.dimension()) {
            return false;
        }
        world.playSound(null, echo.pos(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, ECHO_VOLUME, ECHO_PITCH);
        Vec3 c = Vec3.atCenterOf(echo.pos());
        world.sendParticles(ParticleTypes.REVERSE_PORTAL, c.x, c.y, c.z, ECHO_PARTICLES, 0.3, 0.3, 0.3, 0.02);
        return true;
    }

    /** Leer: roter Hinweis; immer: wie man im Amboss auflaedt. */
    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<net.minecraft.network.chat.Component> out, net.minecraft.world.item.TooltipFlag flag) {
        if (isEmpty(stack)) {
            out.accept(net.minecraft.network.chat.Component.translatable("tooltip.simplegadgets.rotator.empty")
                    .withStyle(net.minecraft.ChatFormatting.RED));
        }
        out.accept(net.minecraft.network.chat.Component.translatable("tooltip.simplegadgets.rotator.recharge",
                String.valueOf(PEARLS_FOR_FULL)).withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        out.accept(net.minecraft.network.chat.Component.translatable("tooltip.simplegadgets.rotator.swap")
                .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
    }

    /** Game-Tick, an dem der Ender-Klang des Spielers faellig ist, oder -1 ohne ausstehenden Klang (fuer Tests). */
    public static long pendingEchoTick(Player player) {
        PendingEcho echo = PENDING_ECHOES.get(player.getUUID());
        return echo == null ? -1 : echo.dueTick();
    }

    /** Read-only twin of {@link #useOn} for the hand hint: a charged rotator that would turn the block. */
    public boolean canTransformTarget(UseOnContext context) {
        if (isEmpty(context.getItemInHand())) {
            return false;
        }
        Player player = context.getPlayer();
        if (player != null && context.getHand() == InteractionHand.MAIN_HAND
                && RotatorSwap.canSwap(context.getLevel(), context.getClickedPos(), player.getOffhandItem())) {
            return true;
        }
        return rotatedState(context) != null;
    }

    /**
     * The block turned as this click would turn it (rim within 2 pixels of the edge, sneaking turns
     * backwards), or null if it does not turn. {@link #useOn} and the hint both ask here.
     */
    private @Nullable BlockState rotatedState(UseOnContext context) {
        BlockState state = context.getLevel().getBlockState(context.getClickedPos());
        BlockState next = calculateNewState(state, context.getClickedFace(), getRimDirection(context, 0.125),
                context.getPlayer() != null && context.getPlayer().isShiftKeyDown());
        return next != null && next != state ? next : null;
    }

    private Direction getRimDirection(UseOnContext context, double margin) {
        Vec3 hitPos = context.getClickLocation().subtract(Vec3.atLowerCornerOf(context.getClickedPos()));
        Direction face = context.getClickedFace();

        double x = hitPos.x;
        double y = hitPos.y;
        double z = hitPos.z;

        if (face.getAxis() == Direction.Axis.Y) { // Oben/Unten
            if (x < margin) return Direction.WEST;
            if (x > 1 - margin) return Direction.EAST;
            if (z < margin) return Direction.NORTH;
            if (z > 1 - margin) return Direction.SOUTH;
        }
        else if (face.getAxis() == Direction.Axis.X) { // Ost/West
            if (y < margin) return Direction.DOWN;
            if (y > 1 - margin) return Direction.UP;
            if (z < margin) return Direction.NORTH;
            if (z > 1 - margin) return Direction.SOUTH;
        }
        else if (face.getAxis() == Direction.Axis.Z) { // Nord/Süd
            if (y < margin) return Direction.DOWN;
            if (y > 1 - margin) return Direction.UP;
            if (x < margin) return Direction.WEST;
            if (x > 1 - margin) return Direction.EAST;
        }

        return null;
    }

    private BlockState calculateNewState(BlockState state, Direction clickedFace, @Nullable Direction rimDirection, boolean isSneaking) {

        // --- AUSGEFAHRENE KOLBEN, KOLBENKOEPFE, BEWEGTE BLOECKE: nie drehen ---
        // Ein gedrehter ausgefahrener Kolben verliert seinen Kopf und zeigt mit der Kopfzelle auf
        // einen neuen Block; sein Einfahren loescht dann genau diesen Block (removeBlock ohne jede
        // Haertepruefung) - auch Grundgestein. Kopf und bewegter Block gehoeren zu so einem Kolben.
        if (isLockedPistonPart(state)) {
            return null;
        }

        // --- LOG (Axis) ---
        if (state.getProperties().contains(BlockStateProperties.AXIS)) {
            return handleAxisRotation(state, clickedFace, rimDirection);
        }

        // --- PISTON / FURNACE (Facing) ---
        Property<Direction> facingProp = getFacingProperty(state);
        if (facingProp != null) {
            return handleFacingRotation(state, facingProp, clickedFace, rimDirection, isSneaking);
        }

        // --- ROTATION (0-15) ---
        if (state.getProperties().contains(BlockStateProperties.ROTATION_16)) {
            int current = state.getValue(BlockStateProperties.ROTATION_16);
            int change = isSneaking ? -1 : 1;
            if (rimDirection != null) change *= 4;
            int next = (current + change + 16) % 16;
            return state.setValue(BlockStateProperties.ROTATION_16, next);
        }

        return null;
    }

    // --- LOGIC: AXIS (Der wichtigste Fix) ---
    private BlockState handleAxisRotation(BlockState state, Direction clickedFace, Direction rimDirection) {
        Direction.Axis currentAxis = state.getValue(BlockStateProperties.AXIS);

        // 1. Rand-Klick: Richte Achse parallel zum Rand aus.
        if (rimDirection != null) {
            return state.setValue(BlockStateProperties.AXIS, rimDirection.getAxis());
        }

        // 2. Zentrum-Klick: Wechsel zwischen der geklickten Achse und der aktuellen.
        // Das fühlt sich am natürlichsten an.
        // Wenn ich auf die Seite (X) eines stehenden Stammes (Y) klicke -> Stamm wird X.
        // Wenn er schon X ist -> Stamm wird Y (oder Z, je nach dritter Dimension).

        // Neue Logik: Zyklus zwischen den zwei Achsen, die NICHT die Blickrichtung sind? Nein.
        // Besser: Wenn Block nicht in Blickrichtung liegt, drehe ihn in Blickrichtung.
        // Wenn er schon in Blickrichtung liegt, drehe ihn in die Dritte.

        Direction.Axis clickedAxis = clickedFace.getAxis();

        if (currentAxis != clickedAxis) {
            return state.setValue(BlockStateProperties.AXIS, clickedAxis);
        } else {
            // Block zeigt bereits auf uns zu (oder weg). Wir rotieren zur nächsten Achse.
            // Zyklus: X -> Y -> Z -> X
            return state.setValue(BlockStateProperties.AXIS, nextAxis(currentAxis));
        }
    }

    private Direction.Axis nextAxis(Direction.Axis axis) {
        return switch (axis) {
            case X -> Direction.Axis.Y;
            case Y -> Direction.Axis.Z;
            case Z -> Direction.Axis.X;
        };
    }

    // --- LOGIC: FACING ---
    private BlockState handleFacingRotation(BlockState state, Property<Direction> prop, Direction clickedFace, Direction rimDirection, boolean isSneaking) {
        Direction currentFacing = state.getValue(prop);
        Collection<Direction> validDirections = prop.getPossibleValues();

        if (rimDirection != null) {
            if (validDirections.contains(rimDirection)) return state.setValue(prop, rimDirection);
            if (validDirections.contains(rimDirection.getOpposite())) return state.setValue(prop, rimDirection.getOpposite());
        }


        Direction nextFacing = rotateAroundAxis(currentFacing, clickedFace.getAxis(), isSneaking);

        if (nextFacing == currentFacing && validDirections.size() > 1) {
            nextFacing = getStandardRotationStart(clickedFace.getAxis(), validDirections);

            if (nextFacing == currentFacing) {
                nextFacing = cycleDirectionList(currentFacing, isSneaking, validDirections);
            }
        }

        if (validDirections.contains(nextFacing)) {
            return state.setValue(prop, nextFacing);
        }

        // Fallback: Zyklus durch Liste
        return state.setValue(prop, cycleDirectionList(currentFacing, isSneaking, validDirections));
    }

    private Direction cycleDirectionList(Direction current, boolean backwards, Collection<Direction> valid) {
        List<Direction> list = valid.stream().toList();
        int index = list.indexOf(current);
        int next = (index + (backwards ? -1 : 1) + list.size()) % list.size();
        return list.get(next);
    }

    private Direction getStandardRotationStart(Direction.Axis axis, Collection<Direction> valid) {
        // Prio-Liste für Startwerte, wenn man auf Pole klickt
        List<Direction> preference;
        if (axis == Direction.Axis.Y) preference = List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);
        else if (axis == Direction.Axis.X) preference = List.of(Direction.UP, Direction.NORTH, Direction.DOWN, Direction.SOUTH);
        else preference = List.of(Direction.UP, Direction.EAST, Direction.DOWN, Direction.WEST);

        for (Direction d : preference) {
            if (valid.contains(d)) return d;
        }
        return valid.iterator().next();
    }

    // Uhrzeigersinn Rotation um Achse
    private Direction rotateAroundAxis(Direction dir, Direction.Axis axis, boolean counterClockwise) {
        if (dir.getAxis() == axis) return dir;

        if (axis == Direction.Axis.Y) {
            return counterClockwise ? dir.getCounterClockWise() : dir.getClockWise();
        }

        if (axis == Direction.Axis.X) {
            // Rotation um X (Seitenansicht)
            // Uhr: UP -> NORTH -> DOWN -> SOUTH
            if (dir == Direction.UP) return counterClockwise ? Direction.SOUTH : Direction.NORTH;
            if (dir == Direction.NORTH) return counterClockwise ? Direction.UP : Direction.DOWN;
            if (dir == Direction.DOWN) return counterClockwise ? Direction.NORTH : Direction.SOUTH;
            if (dir == Direction.SOUTH) return counterClockwise ? Direction.DOWN : Direction.UP;
            return dir;
        }

        if (axis == Direction.Axis.Z) {
            // Rotation um Z (Vorderansicht)
            // Uhr: UP -> EAST -> DOWN -> WEST
            if (dir == Direction.UP) return counterClockwise ? Direction.WEST : Direction.EAST;
            if (dir == Direction.EAST) return counterClockwise ? Direction.UP : Direction.DOWN;
            if (dir == Direction.DOWN) return counterClockwise ? Direction.EAST : Direction.WEST;
            if (dir == Direction.WEST) return counterClockwise ? Direction.DOWN : Direction.UP;
            return dir;
        }
        return dir;
    }

    public static boolean isLockedPistonPart(BlockState state) {
        return state.getBlock() instanceof net.minecraft.world.level.block.piston.PistonHeadBlock
                || state.getBlock() instanceof net.minecraft.world.level.block.piston.MovingPistonBlock
                || (state.hasProperty(BlockStateProperties.EXTENDED) && state.getValue(BlockStateProperties.EXTENDED));
    }

    @SuppressWarnings("unchecked")
    private Property<Direction> getFacingProperty(BlockState state) {
        for (Property<?> prop : state.getProperties()) {
            if (prop.getName().equals("facing") && prop.getValueClass() == Direction.class) return (Property<Direction>) prop;
            if (prop.getName().equals("horizontal_facing") && prop.getValueClass() == Direction.class) return (Property<Direction>) prop;
            if (prop.getName().equals("hopper_facing") && prop.getValueClass() == Direction.class) return (Property<Direction>) prop;
        }
        return null;
    }
}
