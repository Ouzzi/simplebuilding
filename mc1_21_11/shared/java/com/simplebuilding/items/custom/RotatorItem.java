package com.simplebuilding.items.custom;

import com.simplebuilding.items.AnvilRechargeable;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
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
 */
public class RotatorItem extends Item implements AnvilRechargeable {
    /** Volle Ladung (= Haltbarkeit) in Drehungen. */
    public static final int MAX_CHARGE = 1024;
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

    @Override
    public boolean isRechargeMaterial(ItemStack material) {
        return material.is(Items.ENDER_PEARL);
    }

    @Override
    public int chargePerMaterial() {
        return CHARGE_PER_PEARL;
    }

    /** Ladung je Drehung: Config {@code tools.rotatorChargePerTurn} (Standard {@link #USE_COST}), 0 = kostenlos. */
    public static int useCost() {
        com.simplebuilding.config.SimplebuildingConfig config = com.simplebuilding.Simplebuilding.getConfig();
        return config == null ? USE_COST : Math.max(0, config.tools.rotatorChargePerTurn);
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
        boolean isSneaking = player != null && player.isShiftKeyDown();

        // 1. Rand-Erkennung (ca. 2 Pixel am Rand des Blocks)
        Direction rimDirection = getRimDirection(context, 0.125);

        // 2. Neuen Status berechnen
        BlockState newState = calculateNewState(state, context.getClickedFace(), rimDirection, isSneaking);

        if (newState != null && newState != state) {
            // Leer: nichts drehen, nur ein trockenes Klicken (beide Seiten entscheiden gleich).
            if (isEmpty(context.getItemInHand())) {
                if (!world.isClientSide()) {
                    world.playSound(null, pos, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 0.4f, 1.6f);
                }
                return InteractionResult.FAIL;
            }
            if (!world.isClientSide()) {
                world.setBlock(pos, newState, Block.UPDATE_ALL);
                com.simplebuilding.advancement.ModTriggers.feature(player, com.simplebuilding.advancement.ModTriggers.ROTATE);
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
            out.accept(net.minecraft.network.chat.Component.translatable("tooltip.simplebuilding.rotator.empty")
                    .withStyle(net.minecraft.ChatFormatting.RED));
        }
        out.accept(net.minecraft.network.chat.Component.translatable("tooltip.simplebuilding.rotator.recharge",
                String.valueOf(PEARLS_FOR_FULL)).withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
    }

    /** Game-Tick, an dem der Ender-Klang des Spielers faellig ist, oder -1 ohne ausstehenden Klang (fuer Tests). */
    public static long pendingEchoTick(Player player) {
        PendingEcho echo = PENDING_ECHOES.get(player.getUUID());
        return echo == null ? -1 : echo.dueTick();
    }

    @Nullable
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

    private static boolean isLockedPistonPart(BlockState state) {
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