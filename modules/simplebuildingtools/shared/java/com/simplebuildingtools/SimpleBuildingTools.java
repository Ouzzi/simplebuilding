package com.simplebuildingtools;

import com.simplebuildingtools.config.BtConfig;
import com.simplebuildingtools.items.BtItems;
import com.simplelib.api.IdMoves;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.DyeColor;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple Building Tools (b-btools07, 07.11.2026, plan docs/ai/PLAN-B-BTOOLS07-2026-11-07.md, owner WF25-9): a required sub-mod of
 * SimpleBuilding with the building tools - the Building Wands (copper to Enderite and creative), the Octant in 17 colours and the
 * Blueprint (with the creative one). Until 07.11. SimpleBuilding's own items; every {@code simplebuilding:} id of them moves to
 * {@code simplebuildingtools:} ({@link #registerIdMoves}). Requires SimpleLib only (cores, UI, hammer contract); Simple Ores adds the
 * Enderite wand, SimpleBuilding the backpack/bundle supply (Master Builder), the placed octants and the advancements. The loader
 * entry calls {@link #init} once with its config directory.
 */
public final class SimpleBuildingTools {
    public static final String MOD_ID = "simplebuildingtools";
    public static final Logger LOG = LoggerFactory.getLogger("Simple Building Tools");
    /** Where the tools lived until 07.11.2026. */
    public static final String SB_NAMESPACE = "simplebuilding";

    /** Item paths, in tab order (the old paths are the new ones). */
    public static final List<String> WAND_TIERS = List.of("copper", "iron", "gold", "diamond", "netherite", "enderite");
    public static final List<String> ITEMS;
    public static final List<String> COMPONENTS = List.of("blueprint", "blueprint_rotation");
    public static final List<String> ENCHANTMENTS = List.of("cover", "bridge", "linear");
    /** Recipe paths and the category folder of their recipe advancement. */
    public static final List<String[]> RECIPES;

    static {
        List<String> items = new ArrayList<>();
        for (String tier : WAND_TIERS) items.add(tier + "_building_wand");
        items.add("creative_building_wand");
        items.add("octant");
        for (DyeColor color : DyeColor.values()) items.add("octant_" + color.getName());
        items.add("blueprint");
        items.add("creative_blueprint");
        ITEMS = List.copyOf(items);
        List<String[]> recipes = new ArrayList<>();
        for (String tier : List.of("copper", "iron", "gold", "diamond")) recipes.add(new String[] {tier + "_building_wand", "tools"});
        recipes.add(new String[] {"netherite_building_wand_smithing", "tools"});
        recipes.add(new String[] {"enderite_building_wand_smithing", "combat"});
        recipes.add(new String[] {"upgrade_copper_building_wand_to_iron_building_wand", "misc"});
        recipes.add(new String[] {"upgrade_iron_building_wand_to_gold_building_wand", "misc"});
        recipes.add(new String[] {"upgrade_gold_building_wand_to_diamond_building_wand", "misc"});
        recipes.add(new String[] {"octant", "tools"});
        for (DyeColor color : DyeColor.values()) recipes.add(new String[] {"octant_" + color.getName() + "_from_dye", "tools"});
        recipes.add(new String[] {"blueprint", "tools"});
        RECIPES = List.copyOf(recipes);
    }

    private static BtConfig local = new BtConfig().validate();
    private static volatile @Nullable BtConfig synced;
    private static @Nullable Path configDir;
    private static boolean initialized;
    private static boolean movesRegistered;

    private SimpleBuildingTools() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    /** Super-mod switch "Enable Simple Building Tools" (framework SubMods, set by SimpleBuilding): off = no tab entries. */
    public static boolean on() {
        return com.simplebuilding.framework.api.SubMods.enabled(MOD_ID);
    }

    /** What applies now: on the client thread the server's config (once sent), otherwise the own file. */
    public static BtConfig config() {
        BtConfig s = synced;
        if (s != null && BtClientHooks.onClientThread()) return s;
        return local;
    }

    public static BtConfig localConfig() {
        return local;
    }

    public static void replaceConfig(BtConfig config) {
        local = (config == null ? new BtConfig() : config).validate();
    }

    public static void saveConfig() {
        BtConfig.save(configDir, local);
    }

    public static String localConfigJson() {
        return new com.google.gson.Gson().toJson(local);
    }

    /** Client: the server's config arrived (null = disconnected). */
    public static void setSynced(@Nullable String json) {
        if (json == null) {
            synced = null;
            return;
        }
        try {
            synced = new com.google.gson.Gson().fromJson(json, BtConfig.class).validate();
        } catch (RuntimeException e) {
            LOG.warn("Ignoring the server's tool config: {}", e.toString());
        }
    }

    /** Loads the config and registers everything (registries are open during the Fabric init). Runs once. */
    public static synchronized void init(@Nullable Path dir) {
        if (initialized) return;
        initialized = true;
        configDir = dir;
        local = BtConfig.load(dir);
        registerIdMoves();
        // The wand recipes use SimpleLib's building cores and the basic upgrade template (shared material, requested).
        com.simplelib.api.SimpleLibApi.requestMaterial("cores");
        com.simplebuildingtools.component.BtComponents.init();
        BtItems.init();
        com.simplebuildingtools.util.OctantHammerArea.register();
        // A switched-off blueprint loses its recipes too (as in SimpleBuilding until 07.11.; live, /reload brings them back).
        com.simplelib.api.RecipeFilters.register(MOD_ID, path -> !local.blueprint.enabled && path.contains("blueprint"));
        com.simplebuilding.framework.api.TestCentreStations.register(MOD_ID, "tools", "simplebuildingtools.testcentre.station",
                ITEMS.stream().map(p -> MOD_ID + ":" + p).toList());
    }

    /**
     * Every {@code simplebuilding:} id of the tools resolves to {@code simplebuildingtools:}: the 26 items, the two blueprint
     * components, the three wand enchantments, every tool recipe and its recipe advancement. SimpleBuilding's tool advancements
     * ({@code wand/*}, {@code octant/*}, {@code blueprint/*}) stay SimpleBuilding's; this mod reports to them through SimpleLib.
     */
    public static synchronized void registerIdMoves() {
        if (movesRegistered) return;
        movesRegistered = true;
        for (String path : ITEMS) move(Registries.ITEM, path);
        for (String path : COMPONENTS) move(Registries.DATA_COMPONENT_TYPE, path);
        for (String path : ENCHANTMENTS) move(Registries.ENCHANTMENT, path);
        for (String[] recipe : RECIPES) {
            move(Registries.RECIPE, recipe[0]);
            move(Registries.ADVANCEMENT, "recipes/" + recipe[1] + "/" + recipe[0]);
        }
    }

    private static void move(ResourceKey<? extends Registry<?>> registry, String path) {
        IdMoves.move(registry, Identifier.fromNamespaceAndPath(SB_NAMESPACE, path), id(path));
    }

    /** One block a wand or blueprint placed (SimpleBuilding counts it for its statistic and Master Mason). */
    public static final String WAND_BLOCK = "wand_block";

    /** Tells SimpleBuilding's advancements what a tool did ({@code wand_build}, {@code octant_mark}, ...; SimpleLib contract). */
    public static void fire(net.minecraft.world.entity.player.@Nullable Player player, String event) {
        com.simplelib.api.GadgetActions.fire(event, player);
    }
}
