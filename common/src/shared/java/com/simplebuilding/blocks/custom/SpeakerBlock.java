package com.simplebuilding.blocks.custom;

import com.simplebuilding.util.SpeakerBoost;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Lautsprecher (Besitzer 2026-10-03/04): ein schlichter Holzblock wie der Notenblock. Er tut selbst nichts; ein
 * Plattenspieler bzw. Notenblock zaehlt die direkt angrenzenden Lautsprecher seiner Art (lauter und weiter) und spielt
 * zusaetzlich an jedem Lautsprecher seiner Kette ({@link SpeakerBoost}). Astralit gehoert zum Plattenspieler, Nihilit
 * zum Notenblock. Setzen und Abbauen machen die zwischengespeicherten Ketten ungueltig.
 */
public class SpeakerBlock extends Block {
    private final SpeakerBoost.Source source;

    public SpeakerBlock(SpeakerBoost.Source source, Properties properties) {
        super(properties);
        this.source = source;
    }

    /** Die Klangquelle, die dieser Lautsprecher verstaerkt. */
    public SpeakerBoost.Source source() {
        return source;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        SpeakerBoost.invalidateChains();
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        SpeakerBoost.invalidateChains();
    }
}
