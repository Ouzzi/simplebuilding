package com.simplebuilding.blocks.custom;

import com.simplebuilding.version.BlockCodecs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simplebuilding.util.PistonBreach;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Verstaerkter Kolben, normal ({@code sticky = false}) und klebrig ({@code sticky = true}).
 *
 * <p>Was ihn von Vanillas Kolben unterscheidet, sitzt zum groessten Teil in den Mixins:
 * {@code PistonHandlerMixin} hebt das Schublimit von 12 auf 18 und laesst den einen
 * durchbrechbaren Block direkt vor ihm (siehe {@link PistonBreach}) mitschieben, wenn ein
 * Redstoneblock neben ihm bezahlt; {@code PistonBlockMixin} macht ihn ausgefahren unverschiebbar;
 * {@code PistonHeadBlockMixin} erkennt ihn als Basis des Vanilla-Kolbenkopfs an.
 *
 * <p>Hier passiert nur das Bezahlen: Der Redstoneblock verschwindet erst, nachdem Vanillas
 * Ausfahren tatsaechlich gelungen ist. Mit einem durchbrechbaren Block vorn kann das Ausfahren nur
 * ueber die Ausnahme im Mixin gelingen, also beweist ein gelungenes Ausfahren, dass sie genutzt
 * wurde. Scheitert es (Schublimit, Bauhoehe, Obsidian in der Reihe, ein abgesagtes NeoForge-
 * {@code PistonEvent.Pre}), bleibt der Redstoneblock liegen. Einfahren nimmt den Block nie mit
 * zurueck: der Resolver des Einfahrens wird nie scharf geschaltet.
 */
public class ReinforcedPistonBlock extends PistonBaseBlock {
    public static final MapCodec<ReinforcedPistonBlock> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    Codec.BOOL.fieldOf("sticky").forGetter(ReinforcedPistonBlock::isStickyPiston),
                    BlockCodecs.propertiesField()
            ).apply(instance, ReinforcedPistonBlock::new));

    private final boolean sticky;

    public ReinforcedPistonBlock(boolean sticky, Properties settings) {
        super(sticky, settings);
        this.sticky = sticky;
    }

    /** Ob dieser Kolben klebrig ist; Vanillas eigenes Feld {@code isSticky} ist privat. */
    public boolean isStickyPiston() {
        return this.sticky;
    }

    // No @Override: MC 26.3 removed block codecs; this only overrides on 26.2.
    @SuppressWarnings("unchecked")
    public MapCodec<PistonBaseBlock> codec() {
        return (MapCodec<PistonBaseBlock>) (Object) CODEC;
    }

    @Override
    public boolean triggerEvent(BlockState state, Level world, BlockPos pos, int type, int data) {
        BlockPos fuel = null;
        if (type == TRIGGER_EXTEND && !world.isClientSide()) {
            Direction facing = state.getValue(FACING);
            if (PistonBreach.isBreachable(world, pos.relative(facing))) {
                fuel = PistonBreach.findFuel(world, pos, facing);
            }
        }
        boolean fired = super.triggerEvent(state, world, pos, type, data);
        if (fired && fuel != null) {
            // Erst nach dem gelungenen Schub, und ohne Drop: der Redstoneblock ist verbraucht.
            world.destroyBlock(fuel, false);
        }
        return fired;
    }
}
