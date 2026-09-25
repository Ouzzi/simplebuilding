package com.simplebuilding.util;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.Nullable;

/**
 * Setzt Bloecke fuer den Baustab so, wie ein Spieler sie setzen wuerde (Entscheidung des Besitzers,
 * 2026-09-25): Treppen nach Blickrichtung und Klickhoehe, Staemme nach der Achse der Klickseite,
 * Stufen oben/unten, Zaeune und Mauern verbunden, Tueren und Betten vollstaendig, Komponenten des
 * Items (Name, Blockzustand, Block-Entity-Daten) bleiben erhalten.
 *
 * <p>Je Stelle wird ein {@link BlockPlaceContext} gebaut, als haette der Spieler die Klickseite des
 * Nachbarblocks <em>hinter</em> der Stelle an derselben relativen Trefferposition angeklickt wie
 * beim echten Klick. Steht der angeklickte Block selbst aus demselben Block wie das Material, werden
 * seine Ausrichtungs-Eigenschaften uebernommen - eine Treppenreihe, eine liegende Stammlage oder
 * eine Reihe oberer Stufen wird also fortgesetzt statt neu ausgerichtet.
 *
 * <p>Vorschau (Client) und Bau (Server) rufen dieselbe Methode, deshalb zeigt die Vorschau die
 * Ausrichtung, die der Klick setzt.
 */
public final class WandPlacement {
    private WandPlacement() {
    }

    /** Eigenschaften, die vom angeklickten Block gleicher Sorte uebernommen werden. */
    private static final List<Property<?>> ORIENTATION = List.of(
            BlockStateProperties.FACING, BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.FACING_HOPPER,
            BlockStateProperties.AXIS, BlockStateProperties.HORIZONTAL_AXIS, BlockStateProperties.HALF,
            BlockStateProperties.ROTATION_16, BlockStateProperties.ATTACH_FACE);

    /** Trefferposition auf der Oberseite (fuer Oktant-Fuellungen: wie auf den Boden gesetzt). */
    public static final Vec3 TOP_CENTER = new Vec3(0.5, 1.0, 0.5);

    /**
     * Der Zustand, den ein Spieler an {@code pos} setzen wuerde, oder {@code null}, wenn er dort nicht
     * geht (kein Halt, Entity im Weg, Block lehnt ab).
     *
     * @param face    die Seite, auf die geklickt wird (die Stelle liegt vor dieser Seite ihres Nachbarn)
     * @param hitRel  Trefferposition relativ zum angeklickten Block, jede Achse 0..1
     * @param clicked der angeklickte Block, oder {@code null} (dann wird nichts uebernommen)
     */
    public static @Nullable BlockState stateFor(Level level, @Nullable Player player, ItemStack item, BlockPos pos,
                                                Direction face, Vec3 hitRel, @Nullable BlockState clicked) {
        BlockState state = baseState(level, player, item, pos, face, hitRel, clicked);
        if (state == null) {
            return null;
        }
        state = Block.updateFromNeighbourShapes(state, level, pos);
        if (!state.canSurvive(level, pos)) {
            return null;
        }
        if (!level.isUnobstructed(state, pos, CollisionContext.placementContext(player))) {
            return null;
        }
        return state;
    }

    /**
     * Wie {@link #stateFor}, aber ohne Nachbarformen, Halt- und Entity-Pruefung - fuer grosse
     * Oktant-Fuellungen, die das je Stelle guenstiger selbst pruefen.
     */
    public static @Nullable BlockState baseState(Level level, @Nullable Player player, ItemStack item, BlockPos pos,
                                                 Direction face, Vec3 hitRel, @Nullable BlockState clicked) {
        if (!(item.getItem() instanceof BlockItem blockItem)) {
            return null;
        }
        Block block = blockItem.getBlock();
        BlockPos support = pos.relative(face.getOpposite());
        Vec3 location = new Vec3(support.getX() + hitRel.x, support.getY() + hitRel.y, support.getZ() + hitRel.z);
        BlockHitResult hit = new BlockHitResult(location, face, support, false);
        BlockPlaceContext context = new BlockPlaceContext(level, player, InteractionHand.MAIN_HAND, item, hit) {
            @Override
            public BlockPos getClickedPos() {
                return pos;
            }

            @Override
            public boolean replacingClickedOnBlock() {
                return false;
            }
        };
        BlockState state = block.getStateForPlacement(context);
        if (state == null) {
            return null;
        }
        if (clicked != null && clicked.getBlock() == block) {
            state = copyOrientation(clicked, state);
        }
        return item.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY).apply(state);
    }

    /** Uebernimmt die Ausrichtung eines Blocks gleicher Sorte; doppelte Stufen bleiben beim Platzierten. */
    public static BlockState copyOrientation(BlockState from, BlockState to) {
        BlockState result = to;
        for (Property<?> property : ORIENTATION) {
            result = copy(from, result, property);
        }
        if (from.hasProperty(BlockStateProperties.SLAB_TYPE) && result.hasProperty(BlockStateProperties.SLAB_TYPE)
                && from.getValue(BlockStateProperties.SLAB_TYPE) != SlabType.DOUBLE
                && result.getValue(BlockStateProperties.SLAB_TYPE) != SlabType.DOUBLE) {
            result = result.setValue(BlockStateProperties.SLAB_TYPE, from.getValue(BlockStateProperties.SLAB_TYPE));
        }
        return result;
    }

    private static <T extends Comparable<T>> BlockState copy(BlockState from, BlockState to, Property<T> property) {
        if (from.hasProperty(property) && to.hasProperty(property)) {
            return to.setValue(property, from.getValue(property));
        }
        return to;
    }

    /**
     * Was {@code BlockItem#place} nach dem Setzen noch tut: Block-Entity-Daten und Komponenten des
     * Items uebertragen und {@code setPlacedBy} rufen (die obere Tuerhaelfte, das Bettkopfteil).
     * Nur auf dem Server; ohne Ton und ohne Verbrauch (das erledigt der Aufrufer).
     */
    public static void afterPlace(Level level, @Nullable Player player, BlockPos pos, BlockState placed, ItemStack item) {
        if (level.isClientSide() || !level.getBlockState(pos).is(placed.getBlock())) {
            return;
        }
        BlockItem.updateCustomBlockEntityTag(level, player, pos, item);
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity != null) {
            entity.applyComponentsFromItemStack(item);
            entity.setChanged();
        }
        placed.getBlock().setPlacedBy(level, pos, level.getBlockState(pos), player, item);
    }
}
