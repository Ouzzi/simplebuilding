package com.simplebuilding.modules.simpleend;

import java.nio.file.Path;
import net.minecraft.resources.Identifier;

/** Simple End: End wood, End grass, Ash Valley/Dawnlands; super-mod (level 2) of Simple End Dragon Fight. */
public final class SimpleEnd {
    public static final String ID = "simpleend";
    public static final String DRAGON_FIGHT = "simpleenddragonfight";
    /** Owner of Nihilit (simplebuilding:nihilith_shard): the Nihilit planks of each End wood exist only with it (a-nihilplanks). */
    public static final String NIHILITH_OWNER = "simplebuilding";
    private static boolean simpleBuilding;

    private SimpleEnd() {}

    /** Set by the loader entry before anything registers: whether SimpleBuilding (Nihilit's owner) is installed. */
    public static void setSimpleBuilding(boolean loaded) {
        simpleBuilding = loaded;
    }

    /** Whether SimpleBuilding is installed; then each End wood also has its Nihilit planks ({@link EndWood#nihilPlanks}). */
    public static boolean simpleBuilding() {
        return simpleBuilding;
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(ID, path);
    }

    /** Own config switch and the SimpleBuilding "Enable Simple End" switch (framework SubMods, id string only). */
    public static boolean on() {
        return SimpleEndConfig.enabled && com.simplebuilding.framework.api.SubMods.enabled(ID);
    }

    public static void init(Path configDir) {
        SimpleEndConfig.load(configDir);
        applySubSwitches();
    }

    /**
     * Nested switch: Dragon Fight plays only if Simple End itself is on (its own config and the SimpleBuilding switch)
     * and "Enable Simple End Dragon Fight" is on. Called at init and again when the server starts, after every mod
     * (SimpleBuilding included) has set its switches.
     */
    public static void applySubSwitches() {
        com.simplebuilding.framework.api.SubMods.set(DRAGON_FIGHT, on() && SimpleEndConfig.enableSimpleEndDragonFight);
    }
}
