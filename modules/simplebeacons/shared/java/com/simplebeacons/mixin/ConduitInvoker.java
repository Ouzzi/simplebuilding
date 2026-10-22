package com.simplebeacons.mixin;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ConduitBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Tests call the Vanilla conduit's private shape check and effect step directly (SP22-9: the normal conduit gives only
 * Conduit Power), so a Strong Conduit of another test nearby cannot mix its Dolphin's Grace into the result.
 */
@Mixin(ConduitBlockEntity.class)
public interface ConduitInvoker {
    @Invoker("updateShape")
    static boolean simplebeacons$updateShape(Level level, BlockPos pos, List<BlockPos> effectBlocks) {
        throw new AssertionError();
    }

    @Invoker("applyEffects")
    static void simplebeacons$applyEffects(Level level, BlockPos pos, List<BlockPos> effectBlocks) {
        throw new AssertionError();
    }
}
