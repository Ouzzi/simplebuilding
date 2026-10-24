package com.simplelights;

import com.simplelib.api.IdMoves;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple Lights (owner 24.10., WB24-6 / AUD-5, docs/ai/PLAN-B-AUD5-2026-10-24.md): the 37 Light Panes, moved out of
 * SimpleBuilding. The sulfur lights stay there with the sulfur fire. The loader entry point calls {@link #init} once
 * before it registers anything, so the id moves out of SimpleBuilding are known in time.
 */
public final class SimpleLights {
    public static final String MOD_ID = "simplelights";
    public static final Logger LOG = LoggerFactory.getLogger("Simple Lights");
    /** The mod the panes came from; old worlds hold its ids. */
    public static final String OLD_NAMESPACE = "simplebuilding";

    private static boolean movesRegistered;

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static Identifier old(String path) {
        return Identifier.fromNamespaceAndPath(OLD_NAMESPACE, path);
    }

    /**
     * All 37 pane paths in tab order (same as SimpleBuilding before the move): normal, transparent, the sixteen colors,
     * redstone, copper, the sixteen copper colors, copper redstone.
     */
    public static List<String> panePaths() {
        List<String> out = new ArrayList<>(List.of("light_pane", "transparent_light_pane"));
        for (DyeColor dye : DyeColor.values()) out.add(dye.getSerializedName() + "_light_pane");
        out.add("redstone_light_pane");
        out.add("copper_light_pane");
        for (DyeColor dye : DyeColor.values()) out.add(dye.getSerializedName() + "_copper_light_pane");
        out.add("copper_redstone_light_pane");
        return List.copyOf(out);
    }

    /** Folder of a pane's recipe unlock {@code recipes/<folder>/<path>}: the plain and colored panes are decorations, the rest redstone. */
    public static String recipeFolder(String path) {
        return path.equals("redstone_light_pane") || path.contains("copper") ? "redstone" : "decorations";
    }

    /** Super-mod switch "Enable Simple Lights" (framework SubMods, set by SimpleBuilding): off = no tab entries, no guide. */
    public static boolean on() {
        return com.simplebuilding.framework.api.SubMods.enabled(MOD_ID);
    }

    public static void init() {
        registerIdMoves();
        // Test centre station (framework contract, no import of the test centre): the module's items by id.
        List<String> items = new ArrayList<>();
        panePaths().forEach(path -> items.add(MOD_ID + ":" + path));
        items.add(MOD_ID + ":guide_book");
        com.simplebuilding.framework.api.TestCentreStations.register(MOD_ID, "lights", "simplelights.testcentre.station", items);
    }

    /** Old {@code simplebuilding:} ids resolve to the new ones (SimpleLib {@link IdMoves}; the next save writes the new id). */
    public static synchronized void registerIdMoves() {
        if (movesRegistered) return;
        movesRegistered = true;
        for (String path : panePaths()) {
            IdMoves.move(Registries.BLOCK, old(path), id(path));
            IdMoves.move(Registries.ITEM, old(path), id(path));
            IdMoves.move(Registries.RECIPE, old(path), id(path));
            String advancement = "recipes/" + recipeFolder(path) + "/" + path;
            IdMoves.move(Registries.ADVANCEMENT, old(advancement), id(advancement));
        }
    }

    private SimpleLights() {}
}
