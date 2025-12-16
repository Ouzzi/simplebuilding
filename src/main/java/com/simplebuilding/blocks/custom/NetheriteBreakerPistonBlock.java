package com.simplebuilding.blocks.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockState;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class NetheriteBreakerPistonBlock extends PistonBlock {
    public static final MapCodec<NetheriteBreakerPistonBlock> CODEC = createCodec(NetheriteBreakerPistonBlock::new);

    public NetheriteBreakerPistonBlock(Settings settings) {
        super(false, settings);
    }

    @Override
    public MapCodec<PistonBlock> getCodec() {
        return (MapCodec<PistonBlock>) (Object) CODEC;
    }

    @Override
    public boolean onSyncedBlockEvent(BlockState state, World world, BlockPos pos, int type, int data) {
        // type 0 = Piston Event (Ausfahren/Einfahren)
        if (type == 0) {
            // Wir prüfen nur beim Ausfahren
            if (!state.get(EXTENDED)) {
                Direction facing = state.get(FACING);
                BlockPos targetPos = pos.offset(facing);
                BlockState targetState = world.getBlockState(targetPos);

                // Wenn der Block vor dem Piston kein Bedrock ist und nicht Luft:
                if (!targetState.isAir() && targetState.getHardness(world, targetPos) >= 0) {

                    // --- Redstone Stärke Logik ---
                    // Wir holen die Redstone-Power am Piston (0-15).
                    int power = world.getReceivedRedstonePower(pos);

                    // Obsidian hat Härte 50. Power 15 soll Obsidian brechen können.
                    // Formel: (Power / 15) * 50
                    // Power 1  -> Break bis 3.33 (z.B. Stein, Erde, Holz)
                    // Power 15 -> Break bis 50.0 (Obsidian)
                    float breakThreshold = (power / 15.0f) * 50.0f;
                    float blockHardness = targetState.getHardness(world, targetPos);

                    // Nur brechen, wenn das Signal stark genug ist!
                    if (blockHardness <= breakThreshold) {

                        if (targetState.getPistonBehavior() != PistonBehavior.BLOCK) {
                            // Zerstören (Client & Server)
                            world.breakBlock(targetPos, true);

                            if (!world.isClient()) {
                                world.playSound(null, pos, SoundEvents.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.BLOCKS, 0.5f, 0.8f);
                            }
                        }
                    }
                    // Falls Power zu schwach ist, passiert hier nichts (breakBlock wird nicht gerufen).
                    // Der Piston versucht dann normal auszufahren (super.onSynced...),
                    // scheitert aber bei Obsidian/unverschiebbaren Blöcken -> Vanilla Verhalten.
                }
            }
        }

        return super.onSyncedBlockEvent(state, world, pos, type, data);
    }
}