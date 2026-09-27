package com.simplebuilding.forge;

import com.simplebuilding.platform.PistonBreakGuard;
import com.simplebuilding.platform.PlatformServices;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.ForgeEventFactory;

/**
 * Forge's side of {@link PistonBreakGuard}: {@code PistonEvent.Pre} for the piston, the event
 * Forge protection mods use for pistons and that vanilla's {@code triggerEvent} would only fire
 * after the mod had already broken the block. Forge 26.2 ships no fake player and its
 * {@code BlockEvent.BreakEvent} needs a real one, so unlike NeoForge and Fabric no block break
 * event is fired here.
 */
public final class ForgePistonBreakGuard implements PistonBreakGuard {

    private ForgePistonBreakGuard() {
    }

    public static void install() {
        PlatformServices.setPistonBreakGuard(new ForgePistonBreakGuard());
    }

    @Override
    public boolean mayBreak(ServerLevel level, BlockPos piston, Direction facing, BlockPos target, BlockState state) {
        return !ForgeEventFactory.onPistonMovePre(level, piston, facing, true);
    }
}
