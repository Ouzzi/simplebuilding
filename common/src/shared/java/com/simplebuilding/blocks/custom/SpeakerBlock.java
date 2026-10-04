package com.simplebuilding.blocks.custom;

import com.simplebuilding.util.SpeakerBoost;
import net.minecraft.world.level.block.Block;

/**
 * Lautsprecher (Besitzer 2026-10-03): ein schlichter Holzblock wie der Notenblock. Er tut selbst nichts; ein
 * Plattenspieler bzw. Notenblock zaehlt die direkt angrenzenden Lautsprecher seiner Art und spielt entsprechend
 * lauter und weiter ({@link SpeakerBoost}). Astralit gehoert zum Plattenspieler, Nihilit zum Notenblock.
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
}
