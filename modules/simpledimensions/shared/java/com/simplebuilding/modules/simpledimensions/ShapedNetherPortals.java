package com.simplebuilding.modules.simpledimensions;

import dev.simpledimension.common.portal.ArchPortalScanner;
import dev.simpledimension.common.portal.BlockPos3i;
import dev.simpledimension.common.portal.PortalAxis;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.PortalShape;

import java.util.Map;
import java.util.Optional;

/**
 * Nether portals in arch shape (N33, owner 12.11.2026; F1=B smallest 1 × 2, F2=A the far side builds Vanilla's
 * rectangle, F3=A on by default, F5=A at most 21 × 21). Vanilla is asked first: a frame Vanilla accepts stays a
 * plain Vanilla portal. Only otherwise the {@link ArchPortalScanner} looks for an arch of obsidian (the Vanilla
 * frame tag) and fills it with {@link ShapedNetherPortalBlock}, which the exit search treats as a nether portal
 * (POI {@code minecraft:nether_portal}). Lighting, size and cost stay Vanilla; the search runs only when fire is
 * placed or a frame neighbour changes and is bounded to 21 × 21 cells per axis.
 */
public final class ShapedNetherPortals {

    /** Owner rules for the nether arch: any outline, 1 × 2 up to 21 × 21 (Vanilla's maximum). */
    public static final ArchPortalScanner SCANNER = new ArchPortalScanner(ArchPortalScanner.Mode.ANY,
            1, PortalShape.MAX_WIDTH, 2, PortalShape.MAX_HEIGHT);
    /** Set by the loader when a mod that replaces nether portals itself is present (e.g. Immersive Portals). */
    public static volatile boolean yielded = false;

    private ShapedNetherPortals() {
    }

    /** Arches may be lit here: server level of the overworld or the nether, module on, setting on, no portal mod. */
    public static boolean active(Level level) {
        return !yielded && DimensionRegistry.SHAPED != null && level instanceof ServerLevel server
                && (level.dimension() == Level.OVERWORLD || level.dimension() == Level.NETHER)
                && DimensionRegistry.on() && DimensionRuntime.get(server.getServer()).settings.shapedNetherPortals;
    }

    private static ArchPortalScanner.Cells frame(BlockGetter level) {
        return (x, y, z) -> PortalShape.FRAME.test(level.getBlockState(new BlockPos(x, y, z)));
    }

    /** A cell the portal may fill when lit: air or fire, like Vanilla's empty cell. */
    private static ArchPortalScanner.Cells empty(BlockGetter level) {
        return (x, y, z) -> {
            BlockState state = level.getBlockState(new BlockPos(x, y, z));
            return state.isAir() || state.is(BlockTags.FIRE);
        };
    }

    private static ArchPortalScanner.Cells portal(BlockGetter level, Direction.Axis axis) {
        return (x, y, z) -> {
            BlockState state = level.getBlockState(new BlockPos(x, y, z));
            return state.is(DimensionRegistry.SHAPED) && state.getValue(ShapedNetherPortalBlock.AXIS) == axis;
        };
    }

    private static PortalAxis axis(Direction.Axis axis) {
        return axis == Direction.Axis.Z ? PortalAxis.Z : PortalAxis.X;
    }

    /** Vanilla would light this rectangle itself (2..21 × 3..21). */
    private static boolean vanillaSize(ArchPortalScanner.Result shape) {
        return shape.rectangle() && shape.width() >= 2 && shape.height() >= 3;
    }

    /**
     * The empty arch that fire at {@code pos} would light, never one Vanilla handles itself. Every cell of a valid arch
     * reaches the frame straight down and to both sides, so that cheap walk runs first: fire in a burning forest
     * (a flammable block below) costs one block read per axis, not a fill.
     */
    public static Optional<ArchPortalScanner.Result> findEmpty(Level level, BlockPos pos) {
        ArchPortalScanner.Cells frame = frame(level), empty = empty(level);
        for (PortalAxis axis : PortalAxis.values()) {
            if (!SCANNER.enclosed(frame, empty, pos.getX(), pos.getY(), pos.getZ(), axis)) {
                continue;
            }
            ArchPortalScanner.Result shape = SCANNER.scan(frame, empty, pos.getX(), pos.getY(), pos.getZ(), axis);
            if (shape.ok() && !vanillaSize(shape)) {
                return Optional.of(shape);
            }
        }
        return Optional.empty();
    }

    /**
     * {@code BaseFireBlock.onPlace} hook: fire placed inside an arch Vanilla does not accept lights it.
     * Returns true when the fire was replaced by the portal (Vanilla's own handling is then skipped).
     */
    public static boolean tryLight(Level level, BlockPos pos) {
        if (!active(level)) {
            return false;
        }
        // Cheap arch walk first (every fire placement passes here); Vanilla's own rectangle check only for a real arch.
        Optional<ArchPortalScanner.Result> shape = findEmpty(level, pos);
        if (shape.isEmpty() || PortalShape.findEmptyPortalShape(level, pos, Direction.Axis.X).isPresent()) {
            return false;
        }
        create(level, shape.get());
        return true;
    }

    /** Fill the arch like Vanilla's {@code createPortalBlocks}: flags 18, no shape updates while building. */
    public static void create(Level level, ArchPortalScanner.Result shape) {
        BlockState state = DimensionRegistry.SHAPED.defaultBlockState()
                .setValue(ShapedNetherPortalBlock.AXIS, shape.axis() == PortalAxis.Z ? Direction.Axis.Z : Direction.Axis.X);
        for (BlockPos3i cell : shape.interior()) {
            level.setBlock(new BlockPos(cell.x(), cell.y(), cell.z()), state, 18);
        }
    }

    /** {@code BaseFireBlock.isPortal} hook: fire without support may still be placed into a valid arch. */
    public static boolean canLight(Level level, BlockPos pos) {
        return active(level) && findEmpty(level, pos).isPresent();
    }

    /** The arch of shaped portal cells around {@code pos} is still complete and valid (block shape updates). */
    public static boolean intact(BlockGetter level, BlockPos pos, Direction.Axis axis) {
        return SCANNER.scan(frame(level), portal(level, axis), pos.getX(), pos.getY(), pos.getZ(), axis(axis)).ok();
    }

    /**
     * Why fire at {@code pos} lights no portal, for the action bar (owner wish N33). Only for an attempt: the cell
     * has obsidian below and on both sides; silent when Vanilla or the arch accepts the frame.
     */
    public static Optional<Component> reason(Level level, BlockPos pos) {
        if (!active(level) || PortalShape.findEmptyPortalShape(level, pos, Direction.Axis.X).isPresent()) {
            return Optional.empty();
        }
        ArchPortalScanner.Cells frame = frame(level), empty = empty(level);
        for (PortalAxis axis : PortalAxis.values()) {
            if (!SCANNER.enclosed(frame, empty, pos.getX(), pos.getY(), pos.getZ(), axis)) {
                continue;
            }
            ArchPortalScanner.Result shape = SCANNER.scan(frame, empty, pos.getX(), pos.getY(), pos.getZ(), axis);
            return shape.ok() ? Optional.empty() : Optional.of(text(shape.verdict(), SCANNER));
        }
        return Optional.empty();
    }

    /** Action-bar text of a refusal; sizes from the rule that refused. */
    public static Component text(ArchPortalScanner.Verdict verdict, ArchPortalScanner rule) {
        String key = "simpledimensions.portal_shape." + verdict.key();
        return switch (verdict) {
            case OPEN -> Component.translatable(key, rule.maxWidth(), rule.maxHeight());
            case TOO_SMALL -> Component.translatable(key, rule.minWidth(), rule.minHeight());
            default -> Component.translatable(key);
        };
    }

    /** Send the reason for a flint and steel / fire charge use that lights nothing (called after Vanilla's PASS). */
    public static void hint(ServerPlayer player, BlockPos pos) {
        reason(player.level(), pos).ifPresent(player::sendOverlayMessage);
    }

    /** Count every shaped portal state as the Vanilla nether portal POI (exit search both ways). */
    public static void registerPoi(Map<BlockState, Holder<PoiType>> typeByState) {
        Holder<PoiType> netherPortal = BuiltInRegistries.POINT_OF_INTEREST_TYPE.getOrThrow(PoiTypes.NETHER_PORTAL);
        for (BlockState state : DimensionRegistry.SHAPED.getStateDefinition().getPossibleStates()) {
            typeByState.putIfAbsent(state, netherPortal);
        }
    }
}
