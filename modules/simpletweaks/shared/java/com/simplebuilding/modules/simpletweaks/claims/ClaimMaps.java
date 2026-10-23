package com.simplebuilding.modules.simpletweaks.claims;

import com.simplebuilding.framework.api.SubMods;
import com.simplebuilding.modules.simpletweaks.play.Mods;

/**
 * Simple Maps and claims (SP22-8): with Simple Maps the wayfinder map shows claims as a layer
 * ({@link com.simplebuilding.modules.simpletweaks.table.ClaimMapLayer}); without it the deed shows the hint
 * "Simple Maps recommended". The server decides and sends {@link #present()} as {@code mapsPresent}. No class of Simple
 * Maps is touched: the loader adapter answers {@link Mods#loaded} by mod id (principle 4).
 */
public final class ClaimMaps {
    public static final String MOD = "simplemaps";

    /** Whether Simple Maps is loaded and switched on (a super-mod may switch it off with "Enable Simple Maps"). */
    public static boolean present() { return Mods.loaded(MOD) && SubMods.enabled(MOD); }

    private ClaimMaps() {}
}
