package com.simplelib.api;

import com.simplelib.SimpleLib;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;

/**
 * WH26-6: the contract between a barrel (Simple Storage) and a block it docks onto (Simple Crucibles' crucible), so
 * neither mod imports the other. The barrel block implements {@link Dockable} and uses the shared properties; a host
 * block lists itself in {@link #HOSTS} and its block entity implements {@link Host}. Alone, the barrel is a plain
 * barrel and the crucible a plain crucible; together a barrel next to a crucible can be attached (it then offers the
 * crucible's slots and opens the crucible's menu).
 */
public final class BarrelDock {
    /** Docking side of an attached barrel (horizontal); old worlds saved it on loose barrels too. */
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty ATTACHED = BooleanProperty.create("attached");
    /** Strikes that attach a barrel to a host. */
    public static final int ATTACH_STRIKES = 6;
    /** Most slots a host uses in an attached barrel (the Enderite crucible's 30). */
    public static final int MAX_HOST_SLOTS = 30;
    /** Largest stack multiplier a host gives (Enderite crucible: double stacks). */
    public static final int MAX_HOST_STACK_MULTIPLIER = 2;
    /** Blocks a barrel can dock onto ({@code #simplelib:barrel_hosts}; Simple Crucibles lists its crucibles). */
    public static final TagKey<Block> HOSTS = TagKey.create(Registries.BLOCK, SimpleLib.id("barrel_hosts"));

    /** Implemented by every barrel block that carries {@link #FACING} and {@link #ATTACHED}. */
    public interface Dockable {
        /** Stack multiplier of the barrel on its own (1; Enderite 2). */
        int barrelStackMultiplier();
    }

    /** Block entity of a dockable barrel. */
    public interface Barrel extends Container {
        /** Whether the block is attached to a host (state property). */
        boolean attached();

        /** One attach strike; the count restarts after 30 s without a strike. */
        int addAttachStrike();

        /** Just attached (slots beyond the host's count stay stored, hidden). */
        void onAttached();

        /** Just detached: the barrel is normal again, surplus moves into free slots or drops. */
        void onDetached();

        /** Stack multiplier now in force (own, or the host's if bigger while attached). */
        int stackMultiplier();
    }

    /** Block entity of a host a barrel docks onto; it is also the {@link net.minecraft.world.MenuProvider} the attached barrel opens. */
    public interface Host {
        /** Slots of the host the barrel mirrors while attached. */
        int dockSlots();

        /** Stack multiplier of the host. */
        int dockStackMultiplier();
    }

    public static boolean isBarrel(BlockState state) {
        return state.getBlock() instanceof Dockable;
    }

    public static boolean isAttached(BlockState state) {
        return isBarrel(state) && state.getValue(ATTACHED);
    }

    public static boolean isHost(BlockState state) {
        return state.is(HOSTS);
    }

    /** The host next to an unattached barrel, preferring the direction the barrel faces; null if none. */
    public static @Nullable Direction hostSide(Level level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        if (isHost(level.getBlockState(pos.relative(facing)))) return facing;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (isHost(level.getBlockState(pos.relative(d)))) return d;
        }
        return null;
    }

    private BarrelDock() {}
}
