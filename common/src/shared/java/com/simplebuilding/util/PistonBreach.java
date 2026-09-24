package com.simplebuilding.util;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.config.SimplebuildingConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Die gemeinsame Regel der Kolben, die "unzerstoerbare" Bloecke schieben oder durchbrechen
 * (verstaerkter und verstaerkter klebriger Kolben, Netherit- und Enderitkolben).
 *
 * <p><b>Was durchbrochen werden darf</b> ({@link #isBreachable}): ein Block mit einer
 * Zerstoerungsgeschwindigkeit unter 0 oder im Tag {@code simplebuilding:piston_breachable_extra}
 * (Vanilla: verstaerkter Tiefenschiefer), der weder im Tag {@code simplebuilding:piston_breach_immune}
 * steht noch eine Block-Entity hat. Der Endportalrahmen zaehlt nur, solange die Konfigurationsoption
 * {@code pistonsBreachEndPortalFrames} an ist. In Vanilla bleiben damit Grundgestein,
 * Endportalrahmen und verstaerkter Tiefenschiefer uebrig.
 *
 * <p><b>Wer bezahlt</b> ({@link #findFuel}): ein Redstoneblock direkt neben dem Kolben, zuerst der
 * direkt dahinter, dann die vier Seiten quer zur Blickrichtung. Nie die Front: dort steht das
 * Ziel, und Vanilla zaehlt die Front beim Ausfahrsignal nicht mit. Die fuenf gezaehlten Seiten sind
 * genau die, die {@code PistonBaseBlock#getNeighborSignal} abfragt; ein Redstoneblock dort versorgt
 * den Kolben also immer auch mit Strom. Ein Redstoneblock, der nur ueber die Quasi-Konnektivitaet
 * (neben dem Block ueber dem Kolben) wirkt, schaltet den Kolben, bezahlt aber keinen Durchbruch.
 */
public final class PistonBreach {

    private PistonBreach() {
    }

    /** {@link #isBreachable(BlockState, BlockGetter, BlockPos)} fuer den Block, der dort steht. */
    public static boolean isBreachable(BlockGetter level, BlockPos pos) {
        return isBreachable(level.getBlockState(pos), level, pos);
    }

    /** Ob ein Kolben der Mod diesen Block als "unzerstoerbar" schieben oder durchbrechen darf. */
    public static boolean isBreachable(BlockState state, BlockGetter level, BlockPos pos) {
        if (state.isAir() || state.is(ModTags.Blocks.PISTON_BREACH_IMMUNE) || state.hasBlockEntity()) {
            return false;
        }
        if (state.is(Blocks.END_PORTAL_FRAME) && !endPortalFramesBreachable()) {
            return false;
        }
        return isUnbreakableClass(state, level, pos);
    }

    /**
     * Die Klasse der Bloecke, um die es geht, ohne die Ausnahmen: Zerstoerungsgeschwindigkeit unter
     * 0 oder im Tag {@code piston_breachable_extra}. Der Explosionsschutz fuer bewegte Bloecke
     * ({@code MovingPistonExplosionMixin}) fragt nur das.
     */
    public static boolean isUnbreakableClass(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getDestroySpeed(level, pos) < 0.0F || state.is(ModTags.Blocks.PISTON_BREACHABLE_EXTRA);
    }

    /**
     * Der Redstoneblock, der einen Durchbruch bezahlt: direkt hinter dem Kolben zuerst, dann die vier
     * Seiten quer zur Blickrichtung in der Reihenfolge von {@link Direction#values()}. Nie die Front.
     */
    public static @Nullable BlockPos findFuel(Level level, BlockPos piston, Direction facing) {
        BlockPos behind = piston.relative(facing.getOpposite());
        if (level.getBlockState(behind).is(Blocks.REDSTONE_BLOCK)) {
            return behind;
        }
        for (Direction side : Direction.values()) {
            if (side.getAxis() != facing.getAxis()) {
                BlockPos candidate = piston.relative(side);
                if (level.getBlockState(candidate).is(Blocks.REDSTONE_BLOCK)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    /**
     * Vanillas {@code PistonBaseBlock#isPushable} ohne die beiden Regeln, die einen durchbrechbaren
     * Block sonst festhalten: die Zerstoerungsgeschwindigkeit -1 und die Namensliste (Obsidian,
     * weinender Obsidian, Seelenanker, verstaerkter Tiefenschiefer). Alles andere gilt weiter:
     * Weltgrenze, Bauhoehe (nach unten am Weltboden, nach oben an der Bauobergrenze), die
     * Push-Reaktion und das Verbot fuer Block-Entities. Nur {@code PistonHandlerMixin} ruft das,
     * und nur fuer den einen Block direkt vor einem bezahlten verstaerkten Kolben.
     */
    public static boolean isPushableAsBreach(BlockState state, Level level, BlockPos pos, Direction direction,
                                             boolean allowDestroyable, Direction connectionDirection) {
        if (pos.getY() < level.getMinY() || pos.getY() > level.getMaxY() || !level.getWorldBorder().isWithinBounds(pos)) {
            return false;
        }
        if (direction == Direction.DOWN && pos.getY() == level.getMinY()) {
            return false;
        }
        if (direction == Direction.UP && pos.getY() == level.getMaxY()) {
            return false;
        }
        switch (state.getPistonPushReaction()) {
            case BLOCK:
                return false;
            case DESTROY:
                return allowDestroyable;
            case PUSH_ONLY:
                return direction == connectionDirection;
            default:
                break;
        }
        return !state.hasBlockEntity();
    }

    private static boolean endPortalFramesBreachable() {
        SimplebuildingConfig config = Simplebuilding.getConfig();
        return config == null || config.pistonsBreachEndPortalFrames;
    }
}
