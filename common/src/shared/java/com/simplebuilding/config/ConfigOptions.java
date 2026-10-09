package com.simplebuilding.config;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import org.jetbrains.annotations.Nullable;

/**
 * Every config option as a dotted path ({@code tools.wandHungerMultiplier},
 * {@code tweaks.pads.enableFlypads}), found by walking {@link SimplebuildingConfig} by reflection.
 *
 * <p>The in-game command {@code /simplebuilding config get|set|reset|list} reads this list, so a new
 * field is reachable by command the moment it exists - the command cannot fall behind the config
 * file or the config screen. This includes the bounded 26.3 machine, pebble, arrow and scan options. The same walk feeds {@code ConfigOptionTests} (defaults, lang keys,
 * categories). Fields marked {@link ConfigEntry.Gui.Excluded} and static fields are not options:
 * legacy keys that are only read for a migration, and the fixed {@code maxMultiplierLimit}.
 */
public final class ConfigOptions {

    /**
     * Options only the client reads (display, local controls). Changing them through the command on
     * a dedicated server changes the server's file but no player's game; the command says so.
     */
    public static final Set<String> CLIENT_SIDE = Set.of(
            "enableDoubleJump",
            "enableArmorTrimBenefits",
            "showDevEnchantedTab",
            "creativeTabSpacers",
            "vanillaEnchantedBookTextures",
            "modEnchantedBookTextures",
            "visibleTrimIconsVanillaArmor",
            "visibleTrimIconsModArmor",
            "showModHud",
            "hudPositionX",
            "hudPositionY",
            "hudScale",
            "tools.invertOctantSneak",
            "tools.buildingHighlightOpacity",
            "tools.enableToolAnimations",
            "tools.enableChiselAnimation",
            "tools.enableCoreAnimations",
            "tools.transformHintStrength",
            "tools.placedPartParticles",
            "tweaks.laserPointer.color",
            "tweaks.laserPointer.scale",
            "tweaks.optimization.scaleXpOrbs");

    /** Options read while the datapacks load (world start, {@code /reload}), not continuously. */
    public static final Set<String> APPLY_ON_RELOAD = Set.of(
            "worldGen.enableLootTableChanges",
            "worldGen.buildingCoreLootChanceMultiplier",
            "worldGen.enableVillagerTrades",
            "worldGen.enableWanderingTrades",
            "server.loot.globalLootMultiplier",
            "server.loot.strongholdLoot",
            "server.loot.endCityLoot",
            "server.loot.ancientCityLoot",
            "server.loot.bastionLoot",
            "server.loot.netherFortressLoot",
            "server.loot.pillagerOutpostLoot",
            "server.loot.woodlandMansionLoot",
            "server.loot.buriedTreasureLoot",
            "server.loot.dungeonLoot",
            "server.loot.shipwreckLoot",
            "server.loot.iglooLoot",
            "server.loot.mineshaftLoot",
            "server.loot.trialChambersLoot",
            "server.loot.ruinedPortalLoot",
            "server.loot.fishingLoot");

    /**
     * Feature switches that also remove recipes ({@code RecipeFilter}): the behaviour switches at
     * once, the recipes with the next datapack load. The command says so.
     */
    public static final Set<String> RECIPES_ON_RELOAD = Set.of(
            "server.features.endSignals", "server.features.astralVault", "server.features.nihilVault", "server.features.endPistons", "server.features.endRails",
            "server.features.backpack",
            "server.features.attractor",
            "server.features.echoSounder",
            "server.features.blueprint",
            "server.features.oreDetector",
            "server.features.levitatingBlocks",
            "tweaks.pads.enableChunkLoaders",
            "tweaks.pads.enableElytraPads",
            "tweaks.pads.enableFlypads",
            "tweaks.pads.enableSpawnTeleporters",
            "tweaks.pads.enableLaunchpads",
            "tweaks.pads.enablePotionPads",
            "tweaks.laserPointer.enable");

    /**
     * Read once at startup: the maximum charges are item durabilities registered with the items,
     * the End ores are added to the biomes when the world loads. The command says a restart is needed.
     */
    public static final Set<String> RESTART_REQUIRED = Set.of(
            "server.soulLava.fuelMultiplier",
            "server.charges.lensMaxCharge",
            "server.charges.rotatorMaxCharge",
            "server.charges.echoSounderMaxCharge",
            "server.oreGeneration.endOres",
            "server.oreGeneration.astralitOre",
            "server.oreGeneration.nihilitOre",
            "server.oreGeneration.sageOre",
            "server.oreGeneration.dimensionalScrap");

    private static final List<Option> ALL;

    static {
        List<Option> options = new ArrayList<>();
        walk(options, "", SimplebuildingConfig.class, List.of());
        ALL = Collections.unmodifiableList(options);
    }

    private ConfigOptions() {
    }

    /** All options in declaration order (= the order of the config screen). */
    public static List<Option> all() {
        return ALL;
    }

    public static @Nullable Option byPath(String path) {
        for (Option option : ALL) {
            if (option.path().equals(path)) {
                return option;
            }
        }
        return null;
    }

    /** Whether a field type is a leaf value (an option) rather than a group of options. */
    public static boolean isValueType(Class<?> type) {
        return type == boolean.class || type == int.class || type == double.class || type == float.class
                || type == long.class || type == String.class;
    }

    private static void walk(List<Option> into, String prefix, Class<?> owner, List<Field> parents) {
        for (Field field : owner.getFields()) {
            if (field.getDeclaringClass() != owner || field.isSynthetic() || Modifier.isStatic(field.getModifiers())
                    || field.isAnnotationPresent(ConfigEntry.Gui.Excluded.class)) {
                continue;
            }
            String path = prefix + field.getName();
            if (isValueType(field.getType())) {
                into.add(new Option(path, field, parents));
            } else {
                List<Field> chain = new ArrayList<>(parents);
                chain.add(field);
                walk(into, path + ".", field.getType(), List.copyOf(chain));
            }
        }
    }

    /** One option: the field and the chain of group fields leading to it from the config root. */
    public record Option(String path, Field field, List<Field> parents) {

        public Class<?> type() {
            return field.getType();
        }

        /** {@code boolean}, {@code int}, {@code double}, ... as shown to players. */
        public String typeName() {
            return field.getType().getSimpleName();
        }

        public boolean clientSide() {
            return CLIENT_SIDE.contains(path);
        }

        public boolean appliesOnReload() {
            return APPLY_ON_RELOAD.contains(path);
        }

        public boolean recipesOnReload() {
            return RECIPES_ON_RELOAD.contains(path);
        }

        public boolean restartRequired() {
            return RESTART_REQUIRED.contains(path);
        }

        /** The object holding the field inside {@code root}; null if a group is missing. */
        public @Nullable Object owner(SimplebuildingConfig root) {
            Object current = root;
            try {
                for (Field parent : parents) {
                    if (current == null) {
                        return null;
                    }
                    current = parent.get(current);
                }
            } catch (IllegalAccessException e) {
                return null;
            }
            return current;
        }

        public @Nullable Object get(SimplebuildingConfig root) {
            Object owner = owner(root);
            if (owner == null) {
                return null;
            }
            try {
                return field.get(owner);
            } catch (IllegalAccessException e) {
                return null;
            }
        }

        public Object defaultValue() {
            return get(new SimplebuildingConfig());
        }

        /** Writes an already parsed value ({@link #parse}); false if the group is missing. */
        public boolean set(SimplebuildingConfig root, Object value) {
            Object owner = owner(root);
            if (owner == null) {
                return false;
            }
            try {
                field.set(owner, value);
                return true;
            } catch (IllegalAccessException | IllegalArgumentException e) {
                return false;
            }
        }

        /**
         * Parses a command argument into this option's type; null if it is not a valid value.
         * Booleans also take on/off; numbers must be finite.
         */
        public @Nullable Object parse(String text) {
            String value = text.trim();
            Class<?> type = field.getType();
            try {
                if (type == boolean.class) {
                    return switch (value.toLowerCase(Locale.ROOT)) {
                        case "true", "on", "yes" -> Boolean.TRUE;
                        case "false", "off", "no" -> Boolean.FALSE;
                        default -> null;
                    };
                }
                if (type == int.class) {
                    // Colours may be written as #RRGGBB or 0xRRGGBB.
                    if (value.startsWith("#")) {
                        return Integer.parseInt(value.substring(1), 16);
                    }
                    if (value.startsWith("0x") || value.startsWith("0X")) {
                        return Integer.parseInt(value.substring(2), 16);
                    }
                    return Integer.parseInt(value);
                }
                if (type == long.class) {
                    return Long.parseLong(value);
                }
                if (type == double.class) {
                    double parsed = Double.parseDouble(value);
                    return Double.isFinite(parsed) ? parsed : null;
                }
                if (type == float.class) {
                    float parsed = Float.parseFloat(value);
                    return Float.isFinite(parsed) ? parsed : null;
                }
                if (type == String.class) {
                    // "" (two quotes) clears a text option; greedyString cannot hand over nothing.
                    return "\"\"".equals(value) ? "" : value;
                }
            } catch (NumberFormatException e) {
                return null;
            }
            return null;
        }
    }
}
