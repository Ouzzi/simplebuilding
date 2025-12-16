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
            // WICHTIG: Wir entfernen hier den "!world.isClient" Check für die Logik!
            // Der Client muss AUCH wissen, dass der Block zerstört ist, sonst
            // simuliert er fälschlicherweise ein "Schieben" (Geisterblock).

            // Prüfen ob wir ausfahren (state ist noch eingefahren)
            if (!state.get(EXTENDED)) {
                Direction facing = state.get(FACING);
                BlockPos targetPos = pos.offset(facing);
                BlockState targetState = world.getBlockState(targetPos);

                // Breaker-Logik:
                if (!targetState.isAir() && targetState.getHardness(world, targetPos) >= 0) {
                    if (targetState.getPistonBehavior() != PistonBehavior.BLOCK) {
                        // ZERSTÖREN (auf Client UND Server)
                        // Auf dem Server droppt es Items.
                        // Auf dem Client setzt es Luft und spielt Partikel/Sound.
                        world.breakBlock(targetPos, true);

                        // Custom Sound nur auf Server abspielen, um Echo zu vermeiden
                        // (breakBlock macht schon Standard-Sound)
                        if (!world.isClient()) {
                            world.playSound(null, pos, SoundEvents.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.BLOCKS, 0.5f, 0.8f);
                        }
                    }
                }
            }
        }

        // Jetzt rufen wir super auf.
        // Da 'breakBlock' oben nun auf BEIDEN Seiten lief, ist der Weg für den Piston frei.
        // Er fährt also auf Client und Server einfach nur den Arm aus (Animation), ohne zu schieben.
        return super.onSyncedBlockEvent(state, world, pos, type, data);
    }
}