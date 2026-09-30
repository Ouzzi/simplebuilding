package com.simplebuilding.modules.simplemodels;

/** Server-owned settings. JSON keys are stable; every numerical value has a hard cap. */
public final class ModelPolicy {
    public static final int DEFAULT_LEVEL_COST = 1;
    public static final int MAX_MODELS = 64;
    public static final int MAX_FILE_BYTES = 16384;
    public static final int MAX_LEVEL_COST = 10;
    public boolean enabled = true;
    public boolean operatorsOnly = true;
    public boolean allowModItems = false;
    public boolean legacyNameMatching = false;
    public int levelCost = DEFAULT_LEVEL_COST;
    public int maxModels = 64;
    public int maxFileBytes = 16384;
    public void clamp() {
        levelCost = Math.clamp(levelCost, 1, MAX_LEVEL_COST);
        maxModels = Math.clamp(maxModels, 1, MAX_MODELS);
        maxFileBytes = Math.clamp(maxFileBytes, 256, MAX_FILE_BYTES);
    }
}
