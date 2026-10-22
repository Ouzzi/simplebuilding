package com.simpleoctets.block;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Eine Blockzelle aus bis zu acht Achtelbloecken (0,125er-Bloecke, 0,5 x 0,5 x 0,5) eines Materials. Jedes Achtel ist
 * ein Bit ({@link #OCTETS}, Name {@code o<x><y><z>}), alles im Blockzustand, kein Block-Entity: die Modelle sind
 * gebacken (Multipart je Bit). Gemeinsame Basis der Schach-Achtel (SimpleBuilding {@code CheckerOctetBlock}, Farbe im Zustand) und der
 * Material-Achtel ({@link MaterialOctetBlock}, ein Block je Material, z. B. Holz und Melone).
 *
 * <p>Wasserbindbar, solange die Zelle nicht voll ist; voll (8/8) ist sie ein ganzer Wuerfel ohne Wasser. Acht Achtel
 * eines Materials mit Vollblock werden beim Setzen dieser Vollblock (Besitzer SP22-1, {@link OctetCells#fullBlock});
 * umgekehrt bricht ein schleichender Spieler aus einem Vollblock mit Achteln nur die angeschaute Ecke heraus
 * ({@link #startCornerBreak}). Ein Spieler baut die ganze Zelle ab, schleichend nur das angeschaute Achtel ({@link #playerWillDestroy});
 * ohne Spieler droppt jedes Achtel ({@link #getDrops}, keine Loot-Tabelle); Schleichen + leere Hand nimmt das angeklickte Achtel heraus. Achtel
 * verschiedener Materialien in einer Zelle ergeben die gemischte Zelle {@link MixedOctetBlock} ({@link OctetCells}).
 */
public abstract class OctetCellBlock extends Block implements SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    /** Die acht Achtel; Index {@code x | y << 1 | z << 2} (0 = unten, Nord, West). */
    public static final BooleanProperty[] OCTETS = new BooleanProperty[8];
    public static final int FULL = 0xFF;
    /** Die untere Haelfte (y = 0) bzw. die obere als Maske. */
    public static final int BOTTOM_HALF = 0x33;
    public static final int TOP_HALF = 0xCC;
    private static final VoxelShape[] SHAPES = new VoxelShape[256];

    static {
        for (int i = 0; i < 8; i++) {
            OCTETS[i] = BooleanProperty.create("o" + x(i) + y(i) + z(i));
        }
        for (int mask = 0; mask < 256; mask++) {
            VoxelShape shape = Shapes.empty();
            for (int i = 0; i < 8; i++) {
                if ((mask & (1 << i)) != 0) {
                    shape = Shapes.or(shape, box(i));
                }
            }
            SHAPES[mask] = shape.optimize();
        }
    }

    protected OctetCellBlock(BlockBehaviour.Properties properties) {
        super(properties);
        BlockState state = this.stateDefinition.any().setValue(WATERLOGGED, false);
        for (int i = 0; i < 8; i++) {
            state = state.setValue(OCTETS[i], i == 0);
        }
        this.registerDefaultState(state);
    }

    /** Das Item eines Achtels dieser Zelle (Drop, Herausnehmen, Mausrad-Klick). */
    protected abstract Item octetItem(BlockState state);

    /** Ob {@code block} eine Achtelzelle ist (auch die Schach-Achtel von SimpleBuilding und die gemischte Zelle). */
    public static boolean isCell(@Nullable Block block) {
        return block instanceof OctetCellBlock;
    }

    /** Break particles of the mined eighth (mixed cells would otherwise show the neutral ones of the cell). */
    @Override
    public void spawnDestroyParticles(Level level, BlockPos pos, BlockState state) {
        super.spawnDestroyParticles(level, pos, particleState(level, pos, state));
    }

    public static int index(int x, int y, int z) {
        return (x & 1) | (y & 1) << 1 | (z & 1) << 2;
    }

    public static int x(int index) {
        return index & 1;
    }

    public static int y(int index) {
        return (index >> 1) & 1;
    }

    public static int z(int index) {
        return (index >> 2) & 1;
    }

    /** Die Box eines Achtels in Blockkoordinaten. */
    public static VoxelShape box(int index) {
        return Block.box(x(index) * 8, y(index) * 8, z(index) * 8, x(index) * 8 + 8, y(index) * 8 + 8, z(index) * 8 + 8);
    }

    /** Die Form einer Zelle mit diesen Achteln. */
    public static VoxelShape shape(int mask) {
        return SHAPES[mask & FULL];
    }

    /** Welche Achtel gesetzt sind, als Bitmaske. */
    public static int mask(BlockState state) {
        int mask = 0;
        for (int i = 0; i < 8; i++) {
            if (state.getValue(OCTETS[i])) {
                mask |= 1 << i;
            }
        }
        return mask;
    }

    /** Wie viele Achtel gesetzt sind. */
    public static int count(BlockState state) {
        return Integer.bitCount(mask(state));
    }

    /** Der Zustand mit genau diesen Achteln; volle Zellen halten kein Wasser. */
    public static BlockState withMask(BlockState state, int mask) {
        for (int i = 0; i < 8; i++) {
            state = state.setValue(OCTETS[i], (mask & (1 << i)) != 0);
        }
        return mask == FULL ? state.setValue(WATERLOGGED, false) : state;
    }

    /** Das Achtel, in dem ein Punkt (Weltkoordinaten) in der Zelle {@code pos} liegt. */
    public static int indexAt(BlockPos pos, Vec3 point) {
        return index(half(point.x - pos.getX()), half(point.y - pos.getY()), half(point.z - pos.getZ()));
    }

    private static int half(double local) {
        return local >= 0.5 ? 1 : 0;
    }

    /** Das Achtel unter dem Trefferpunkt (ein Stueck in die getroffene Flaeche hinein). */
    public static int hitIndex(BlockPos pos, BlockHitResult hit) {
        Direction face = hit.getDirection();
        Vec3 inside = hit.getLocation().add(face.getStepX() * -0.25, face.getStepY() * -0.25, face.getStepZ() * -0.25);
        return indexAt(pos, inside);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
        builder.add(OCTETS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[mask(state)];
    }

    /** Teilweise gefuellte Zellen lassen Licht durch wie Stufen. */
    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return mask(state) != FULL;
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    /** Wasser nur in nicht volle Zellen. */
    @Override
    public boolean canPlaceLiquid(@Nullable LivingEntity owner, BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
        return mask(state) != FULL && SimpleWaterloggedBlock.super.canPlaceLiquid(owner, level, pos, state, fluid);
    }

    @Override
    public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluid) {
        return mask(state) != FULL && SimpleWaterloggedBlock.super.placeLiquid(level, pos, state, fluid);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    /**
     * Je gesetztes Achtel ein Achtel-Item seines Materials. Nach einem gezielten Abbau durch einen Spieler
     * ({@link #playerWillDestroy}) nur das abgebaute Achtel; bei gemischten Zellen faellt ein Achtel, dessen Material ein
     * Werkzeug braucht, nur mit dem passenden (wie beim reinen Block).
     */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        Vec3 origin = params.getOptionalParameter(LootContextParams.ORIGIN);
        Breaking breaking = BREAKING.get();
        BlockState[] parts;
        if (breaking != null && origin != null && breaking.level() == params.getLevel()
                && breaking.time() == params.getLevel().getGameTime() && breaking.pos().equals(BlockPos.containing(origin))) {
            BREAKING.remove();
            parts = new BlockState[8];
            parts[0] = breaking.part();
        } else if (state.getBlock() instanceof MixedOctetBlock) {
            parts = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof MixedOctetBlockEntity be
                    ? masked(be.parts(), mask(state)) : new BlockState[8];
        } else {
            parts = new BlockState[8];
            int mask = mask(state);
            for (int i = 0; i < 8; i++) {
                parts[i] = (mask & (1 << i)) != 0 ? OctetCells.material(state) : null;
            }
        }
        boolean byPlayer = params.getOptionalParameter(LootContextParams.THIS_ENTITY) instanceof Player;
        ItemStack tool = params.getOptionalParameter(LootContextParams.TOOL) instanceof ItemStack stack ? stack : null;
        List<ItemStack> drops = new ArrayList<>();
        for (BlockState part : parts) {
            if (part == null || (byPlayer && !harvests(tool, part))) {
                continue;
            }
            ItemStack item = OctetCells.item(part);
            boolean merged = false;
            for (ItemStack drop : drops) {
                if (ItemStack.isSameItemSameComponents(drop, item)) {
                    drop.grow(1);
                    merged = true;
                    break;
                }
            }
            if (!merged && !item.isEmpty()) {
                drops.add(item);
            }
        }
        return drops;
    }

    /**
     * Whether {@code tool} harvests an octet of {@code part}: the cell's own rule and the one of the material's full block
     * (the cells carry only the mineable tags, not {@code needs_iron_tool} &amp; Co.: a diamond octet needs an iron pickaxe).
     */
    static boolean harvests(@Nullable ItemStack tool, BlockState part) {
        BlockState full = part.getBlock() instanceof MaterialOctetBlock octet ? octet.sourceState() : null;
        for (BlockState state : full == null ? new BlockState[]{part} : new BlockState[]{part, full}) {
            if (state.requiresCorrectToolForDrops() && (tool == null || !tool.isCorrectToolForDrops(state))) {
                return false;
            }
        }
        return true;
    }

    private static BlockState[] masked(BlockState[] parts, int mask) {
        for (int i = 0; i < 8; i++) {
            if ((mask & (1 << i)) == 0) {
                parts[i] = null;
            }
        }
        return parts;
    }

    /** Das Achtel unter dem Mauszeiger (gemischte Zellen: das unterste belegte). */
    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        for (BlockState part : OctetCells.parts(level, pos, state)) {
            if (part != null) {
                return OctetCells.item(part);
            }
        }
        return new ItemStack(octetItem(state));
    }

    /**
     * Ein gezielter Abbau (Queue N48): Rest der Zelle und abgebautes Achtel, je Thread (Client-Vorhersage und
     * eingebauter Server stoeren sich nicht). {@link #destroy} legt den Rest zurueck, {@link #getDrops} gibt das Achtel
     * ({@code part}; null = nichts, die Ecke eines Vollblocks, der so nichts droppen wuerde).
     */
    private record Breaking(LevelAccessor level, BlockPos pos, BlockState[] rest, boolean wet, @Nullable BlockState part, int index, long time) {
    }

    private static final ThreadLocal<Breaking> BREAKING = new ThreadLocal<>();

    /**
     * Abbauen durch einen Spieler nimmt die ganze Zelle (alle Achtel, alle Drops); schleichend nur das angeschaute Achtel
     * ({@link OctetCells#lookedAt}), das letzte Achtel nimmt die Zelle mit. Der Vorschlaghammer folgt derselben Regel:
     * sein Flaechenabbau nimmt ganze Zellen, schleichend baut er genau einen Block ab, also ein Achtel. Vanilla und alle Loader rufen {@code playerWillDestroy}, entfernen dann den Block und rufen
     * {@link #destroy}; Explosionen, Kolben und {@code Level#destroyBlock} ohne Spieler droppen weiter alles.
     */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BREAKING.remove();
        BlockState[] parts = OctetCells.parts(level, pos, state);
        int index = OctetCells.lookedAt(player, pos, state);
        // Besitzer 13.10.: normal die ganze Zelle mit allen Achteln und Drops, schleichend nur das angeschaute Achtel.
        if (parts[index] != null && player.isShiftKeyDown()) {
            BREAKING.set(new Breaking(level, pos.immutable(), OctetCells.without(parts, index), OctetCells.wet(state),
                    parts[index], index, level.getGameTime()));
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** State whose break particles are shown: the mined eighth while it is being broken (mixed cells), else the cell itself. */
    public static BlockState particleState(Level level, BlockPos pos, BlockState state) {
        Breaking breaking = BREAKING.get();
        if (breaking != null && breaking.level() == level && breaking.pos().equals(pos) && breaking.part() != null) {
            return OctetCells.single(breaking.part(), breaking.index());
        }
        return state;
    }

    /** Nach dem Entfernen durch einen Spieler: die uebrigen Achtel kommen zurueck (gemischt oder rein). */
    @Override
    public void destroy(LevelAccessor level, BlockPos pos, BlockState state) {
        super.destroy(level, pos, state);
        restoreRest(level, pos);
    }

    /** Legt nach einem gezielten Abbau an {@code pos} die uebrigen Achtel zurueck (auch fuer Vollbloecke, {@code BlockCornerBreakMixin}). */
    public static void restoreRest(LevelAccessor level, BlockPos pos) {
        Breaking breaking = BREAKING.get();
        if (breaking == null || breaking.level() != level || !breaking.pos().equals(pos) || OctetCells.mask(breaking.rest()) == 0) {
            return;
        }
        OctetCells.set(level, pos, breaking.rest(), breaking.wet(), Block.UPDATE_ALL);
    }

    /** Spieler und Tick des letzten Abbaus eines nicht angeschauten Blocks (Flaechenabbau), je Thread. */
    private record AreaBreak(Player player, long time) {
    }

    private static final ThreadLocal<AreaBreak> AREA = new ThreadLocal<>();

    /**
     * Besitzer SP22-1: ein schleichender Spieler bricht aus einem Vollblock mit Achteln ({@link OctetCells#cornerCell})
     * nur die angeschaute Ecke heraus - der Block wird zur Zelle mit 7 Achteln, ein Achtel faellt. Aufgerufen am Anfang
     * von {@code Block#playerWillDestroy} ({@code BlockCornerBreakMixin}); danach laeuft Vanilla weiter (Partikel und
     * Klang des Materials, Piglins bei Gold, Game-Event), entfernt den Block und ruft {@code destroy}
     * ({@link #restoreRest}) und {@code playerDestroy} ({@link #cornerDropState}). Alle Schutz-Hooks (Claims, Loader-
     * Abbau-Events, Abenteuermodus) laufen vorher. Kein Ecken-Abbau: ohne Schleichen, ohne Material-Achtel, bei
     * Block-Entities, wenn der Blick einen anderen Block trifft (Ader-/Tunnelabbau, Hammer: deren Nachbarn), und fuer den
     * Startblock eines solchen Flaechenabbaus im selben Tick.
     */
    public static void startCornerBreak(Level level, BlockPos pos, BlockState state, Player player) {
        if (state.getBlock() instanceof OctetCellBlock || !player.isShiftKeyDown() || player.isSpectator()
                || state.hasBlockEntity() || !com.simpleoctets.SimpleOctets.on()) {
            return;
        }
        BREAKING.remove();
        Block cell = OctetCells.cornerCell(state.getBlock());
        if (cell == null) {
            return;
        }
        long time = level.getGameTime();
        if (!(player.pick(player.blockInteractionRange(), 1.0F, false) instanceof BlockHitResult hit)
                || hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK || !hit.getBlockPos().equals(pos)) {
            AREA.set(new AreaBreak(player, time));
            return;
        }
        AreaBreak area = AREA.get();
        if (area != null && area.player() == player && area.time() == time) {
            return;
        }
        BlockState own = OctetCells.material(cell.defaultBlockState());
        int index = hitIndex(pos, hit);
        BlockState[] rest = new BlockState[8];
        for (int i = 0; i < 8; i++) {
            rest[i] = i == index ? null : own;
        }
        BlockState part = level instanceof net.minecraft.server.level.ServerLevel server ? cornerDrop(server, pos, state, player, own) : own;
        BREAKING.set(new Breaking(level, pos.immutable(), rest, false, part, index, time));
    }

    /**
     * What the corner of a full block drops (server): what the block itself would drop with this tool, as an octet - the
     * block itself gives its own octet, another block with octets (stone -> cobblestone) that one's, anything else none.
     */
    private static @Nullable BlockState cornerDrop(net.minecraft.server.level.ServerLevel level, BlockPos pos, BlockState state, Player player, BlockState own) {
        if (player.preventsBlockDrops()) {
            return null;
        }
        List<ItemStack> drops = Block.getDrops(state, level, pos, null, player, player.getMainHandItem());
        if (drops.size() != 1 || !(drops.get(0).getItem() instanceof net.minecraft.world.item.BlockItem item)) {
            return null;
        }
        if (item.getBlock() == state.getBlock()) {
            return own;
        }
        Block other = OctetCells.cornerCell(item.getBlock());
        return other == null ? null : OctetCells.material(other.defaultBlockState());
    }

    /**
     * {@code Block#playerDestroy} of a full block whose corner was just broken off: the drops come from the cell (only the
     * broken octet, {@link #getDrops}); Forge hands the full block's state on, Vanilla and NeoForge too.
     */
    public static BlockState cornerDropState(Level level, BlockPos pos, BlockState state) {
        Breaking breaking = BREAKING.get();
        if (state.getBlock() instanceof OctetCellBlock || breaking == null || breaking.level() != level || !breaking.pos().equals(pos)
                || breaking.time() != level.getGameTime()) {
            return state;
        }
        for (BlockState part : breaking.rest()) {
            if (part != null) {
                return withMask(part, FULL);
            }
        }
        return state;
    }

    /** Schleichen + leere Hand: das angeklickte Achtel (sein Material) kommt in die Hand. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isSecondaryUseActive() || !player.mayBuild()) {
            return InteractionResult.PASS;
        }
        int index = hitIndex(pos, hit);
        BlockState[] parts = OctetCells.parts(level, pos, state);
        BlockState part = parts[index];
        if (part == null) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            OctetCells.set(level, pos, OctetCells.without(parts, index), state.getValue(WATERLOGGED), Block.UPDATE_ALL);
            OctetCells.give(player, OctetCells.item(part));
            level.playSound(null, pos, part.getSoundType().getBreakSound(), SoundSource.BLOCKS, 0.8F, 1.2F);
            level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        }
        return InteractionResult.SUCCESS;
    }
}
