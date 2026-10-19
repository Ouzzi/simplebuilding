package com.simplebuilding.util;

/**
 * 26.3 (split stage 7): the saved strike progress is Simple Hammers' ({@link com.simplehammers.engine.SledgehammerProgress},
 * same saved-data name); SimpleBuilding's code calls it through the inherited statics of this class and does not tick it
 * (the module does). The 26.2 twin (common/src/mc26_2/java) keeps SimpleBuilding's own code.
 */
public final class SledgehammerProgress extends com.simplehammers.engine.SledgehammerProgress {
    private SledgehammerProgress() {
    }
}
