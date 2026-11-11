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
            "creativeTabSpacers",
            "vanillaEnchantedBookTextures",
            "modEnchantedBookTextures",
            "jukebox264Textures",
            "noteBlockFrameTexture",
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
            "enableSimpleMaps",
            "enableSimpleQuestionary",
            // <submods:begin restart>
            "enableSimpleMoney",
            "enableSimpleRiding",
            "enableSimpleFun",
            "enableSimpleTweaks",
            "enableSimpleDimensions",
            "enableSimpleSandwiches",
            "enableSimpleTrims",
            "enableSimpleMobs",
            "enableSimpleEnd",
            "enableSimpleRespawn",
            "enableSimpleBeacons",
            "enableSimpleShops",
            "enableSimpleWanderers",
            "enableSimpleBlocks",
            "enableSimpleOctets",
            "enableSimpleHammers",
            "enableSimpleStorage",
            "enableSimpleMusic",
            "enableSimpleLights",
            "enableSimpleGadgets",
            "enableSimpleEarth",
            "enableSimpleChallenges",
            "enableSimpleAnimals",
            "enableSimpleDinos",
            "enableSimpleOres",
            "enableSimpleMachines",
            "enableSimpleArrows",
            "enableSimpleResonance",
            "enableSimpleEnchanting",
            "enableSimplePads",
            "enableSimpleBuildingtools",
            "enableSimpleChisels",
            "enableSimpleLayers",
            "enableSimpleVault",
            // <submods:end restart>
            "server.charges.lensMaxCharge",
            "server.charges.rotatorMaxCharge",
            "server.charges.echoSounderMaxCharge",
            "server.oreGeneration.endOres",
            "server.oreGeneration.astralitOre",
            "server.oreGeneration.nihilitOre",
            "server.oreGeneration.sageOre",
            "server.oreGeneration.dimensionalScrap",
            "server.oreGeneration.crackedIceFields");

    /**
     * Split stage 9: options whose feature moved into a required sub-mod on this line. The sub-mod took the value
     * over once from {@code simplebuilding.json} (framework {@code ConfigFiles.adoptLegacy}) and reads its own file
     * since, so here they do nothing. The fields stay (old files load without error and keep the value when saved);
     * only the config screens, {@code config list} and the wiki leave them out ({@link Option#hidden}). Each set
     * belongs to the flag that says the feature is SimpleBuilding's own; 26.2 keeps all of them.
     */
    public static final Set<String> MOVED_TO_OVENS = Set.of(
            "server.machines.reinforcedFurnaceSpeed",
            "server.machines.netheriteFurnaceSpeed",
            "server.machines.enderiteFurnaceSpeed");
    public static final Set<String> MOVED_TO_HAMMERS = Set.of(
            "server.tools.diamondBlockPebbles",
            "server.tools.sledgehammerUpgradeSeconds",
            "server.tools.reinforcedUpgradeDamagePerHit",
            "server.tools.netheriteUpgradeDamagePerHit",
            "server.tools.enderiteUpgradeDamagePerHit");
    /**
     * WE25-7: the chisel cooldowns live in Simple Chisels on 26.3 ({@code config/simplechisels-server.json},
     * {@code cooldownTicks.<tier>}, taken over once from here; {@code McVersion.CHISELS_IN_SB}).
     */
    public static final Set<String> MOVED_TO_CHISELS = Set.of(
            "server.tools.stoneChiselCooldownTicks", "server.tools.copperChiselCooldownTicks", "server.tools.ironChiselCooldownTicks",
            "server.tools.goldChiselCooldownTicks", "server.tools.diamondChiselCooldownTicks", "server.tools.netheriteChiselCooldownTicks",
            "server.tools.enderiteChiselCooldownTicks");
    public static final Set<String> MOVED_TO_STORAGE = Set.of(
            "tools.invertBundleInteractions",
            "server.features.backpack",
            "server.loot.betterChestPercent",
            "server.loot.reinforcedShulkerPercent",
            "server.loot.netheriteShulkerPercent",
            "server.loot.enderiteShulkerPercent",
            "server.loot.endermitesPerRareShulker");

    /**
     * MACH32-MOD stage 2: the piston upgrades' options live in Simple Machines on 26.3 ({@code config/simplemachines-server.json},
     * {@code pistons.*}, taken over once from here); {@code McVersion.PISTONS_IN_SB}.
     */
    public static final Set<String> MOVED_TO_MACHINES = Set.of(
            "breakerPistonsLoseDurability",
            "pistonsBreachEndPortalFrames",
            "pistonsBreachModdedUnbreakables",
            "pistonsFireBreakEvents");

    /**
     * ENCH02 (03.11.): the endgame switch of the enchanters lives in Simple Enchanting on 26.3 ({@code config/simpleenchanting-server.json},
     * {@code features.endgameLevels}, taken over once from here); {@code McVersion.ASTRAL_ENCHANTING} (the tables are the module's there).
     */
    public static final Set<String> MOVED_TO_ENCHANTING = Set.of("server.features.endgameEnchantLevels");

    /**
     * MACH32-MOD stage 3: the hopper speeds live in Simple Machines on 26.3 ({@code hoppers.*}, taken over once from here);
     * {@code McVersion.HOPPERS_IN_SB}.
     */
    public static final Set<String> MOVED_TO_MACHINES_HOPPERS = Set.of(
            "server.machines.reinforcedHopperSpeed",
            "server.machines.netheriteHopperSpeed",
            "server.machines.enderiteHopperSpeed");

    /**
     * MACH32-MOD stage 4: the Auto Smither's delay lives in Simple Machines on 26.3 ({@code autoSmither.delayTicks}, taken over once
     * from here); {@code McVersion.AUTO_SMITHER_IN_MACHINES} (inverted flag: hidden when true).
     */
    public static final Set<String> MOVED_TO_MACHINES_SMITHER = Set.of("server.machines.autoSmitherDelayTicks");

    /**
     * IG31-13: world generation of Sage Ore and Dimensional Scrap lives in Simple Ores on 26.3
     * ({@code McVersion.MATERIALS_IN_ORES}, inverted flag: hidden when true); Simple Ores has its own switches.
     */
    public static final Set<String> MOVED_TO_ORES = Set.of(
            "server.oreGeneration.sageOre",
            "server.oreGeneration.dimensionalScrap");

    /**
     * b-pads01 (DW04C-5): the pad options live in Simple Pads on 26.3 ({@code config/simplepads-server.json}, taken over once from
     * here; {@code McVersion.PADS_IN_SB}).
     */
    public static final Set<String> MOVED_TO_PADS = Set.of(
            "tweaks.pads.enableChunkLoaders", "tweaks.pads.enableElytraPads", "tweaks.pads.enableFlypads",
            "tweaks.pads.enableSpawnTeleporters", "tweaks.pads.enableLaunchpads", "tweaks.pads.enableTimedCopperPlates",
            "tweaks.pads.enableFilterPlates", "tweaks.pads.enablePotionPads",
            "tweaks.padTuning.teleporterTier1WarmupTicks", "tweaks.padTuning.teleporterTier2WarmupTicks",
            "tweaks.padTuning.teleporterTier3WarmupTicks", "tweaks.padTuning.launchpadStrengthMultiplier",
            "tweaks.padTuning.potionPadChargeStepTicks", "tweaks.padTuning.potionPadCooldownFactor",
            "tweaks.spawn.disableFallDamageInSpawn", "tweaks.spawn.firstJoinTeleporterCount", "tweaks.spawn.firstJoinElytraPadCount",
            "tweaks.spawn.giveElytraOnSpawn", "tweaks.spawn.spawnElytraRadius", "tweaks.spawn.useWorldSpawnAsCenter",
            "tweaks.spawn.customSpawnElytraX", "tweaks.spawn.customSpawnElytraZ", "tweaks.spawn.flightTimeSeconds",
            "tweaks.spawn.maxBoosts", "tweaks.spawn.boostStrength", "tweaks.spawn.spawn1X", "tweaks.spawn.spawn1Y",
            "tweaks.spawn.spawn1Z", "server.pads.strangerPadBreakSeconds", "server.pads.strangerPlateBreakSeconds",
            "server.dimensionLocks.chunkLoaderBlockedDimensions", "server.dimensionLocks.flypadBlockedDimensions",
            "server.chunkLoaders.requireOwnerOnline");

    /**
     * b-cfg10: the building tools live in Simple Building Tools on 26.3 ({@code config/simplebuildingtools-server.json}, taken over
     * once from here; {@code McVersion.BTOOLS_IN_SB}). {@code tools.invertOctantSneak} stays: SimpleBuilding hands it to the module.
     */
    public static final Set<String> MOVED_TO_BTOOLS = Set.of(
            "server.features.blueprint", "server.blueprint.maxBlocksPerTick", "server.blueprint.pictureImport",
            "server.blueprint.picturePermission", "server.blueprint.pictureMaxSide",
            "tools.buildingWandHungerCost", "tools.wandHungerMultiplier");

    /**
     * b-cfg10: the Gauge, Transposer (rotator), Resonance Rod (laser pointer) and Echo Sounder live in Simple Gadgets on 26.3
     * ({@code config/simplegadgets-server.json}, taken over once from here; {@code McVersion.GADGETS_IN_SB}); since b-gadg14 the
     * Attractor and the Detector too.
     */
    public static final Set<String> MOVED_TO_GADGETS = Set.of(
            "tools.rotatorChargePerTurn", "tools.rotatorSwapEnabled", "tools.rotatorSwapWindupTicks",
            "tools.rotatorSwapMaxHardness", "tools.rotatorSwapWhitelist", "tools.rotatorSwapBlacklist",
            "server.charges.rotatorMaxCharge", "server.charges.lensMaxCharge", "server.charges.echoSounderMaxCharge",
            "server.laser.igniteFlammables", "server.laser.igniteTnt", "server.laser.igniteEntities",
            "server.laser.scanEntities", "server.laser.scanPlayers", "server.laser.scanIntervalTicks",
            "server.features.echoSounder", "server.dimensionLocks.echoSounderBlockedDimensions",
            "tweaks.balancing.echoSounderJumpCooldownTicks", "tweaks.balancing.echoSounderAttemptLockTicks",
            "tweaks.laserPointer.enable", "tweaks.laserPointer.range", "tweaks.laserPointer.chargePerSecond",
            "tweaks.laserPointer.effectCost", "tweaks.laserPointer.color", "tweaks.laserPointer.scale",
            // b-gadg14 (OW24-2 G3): Attractor and Detector.
            "server.features.attractor", "tools.magnetRangeMultiplier", "server.tools.attractorMinimumDistance",
            "server.features.oreDetector", "server.oreDetector.rangeMultiplier", "server.oreDetector.scanIntervalTicks");

    /**
     * b-cfg11: the Light Panes' siblings, the glowing trims, live in Simple Trims on 26.3 ({@code McVersion.TRIMS_IN_SB}); SimpleBuilding's
     * emission is 0 there ({@code GlowingTrimUtils}), so {@code DynamicLightHandler.enabled()} lights nothing, and Simple Trims
     * follows its own switch ({@code SimpleTrims.on()}).
     */
    public static final Set<String> MOVED_TO_TRIMS = Set.of("server.features.dynamicLight");

    /**
     * b-cfg11: the levitating and suspended sand/gravel are Simple Blocks' on 26.3 ({@code McVersion.BLOCKS_IN_SB}); the only reader
     * of the switch, {@code RecipeFilter}, touches the namespace {@code simplebuilding}, whose recipes are gone there.
     */
    public static final Set<String> MOVED_TO_BLOCKS = Set.of("server.features.levitatingBlocks");

    /**
     * b-trimstats27 (MOB27-3 R2): the base of the trim resonance lives in Simple Resonance on 26.3 ({@code config/simpleresonance.json},
     * taken over once from here; {@code McVersion.RESONANCE_IN_SB}).
     */
    public static final Set<String> MOVED_TO_RESONANCE = Set.of("trimBenefitBaseMultiplier");

    /** True if {@code path} belongs to a sub-mod on this line (see {@link #MOVED_TO_OVENS}). */
    public static boolean hidden(String path) {
        return !com.simplebuilding.version.McVersion.OVENS_IN_SB && MOVED_TO_OVENS.contains(path)
                || !com.simplebuilding.version.McVersion.HAMMERS_IN_SB && MOVED_TO_HAMMERS.contains(path)
                || !com.simplebuilding.version.McVersion.CHISELS_IN_SB && MOVED_TO_CHISELS.contains(path)
                || !com.simplebuilding.version.McVersion.STORAGE_IN_SB && MOVED_TO_STORAGE.contains(path)
                || com.simplebuilding.version.McVersion.MATERIALS_IN_ORES && MOVED_TO_ORES.contains(path)
                || !com.simplebuilding.version.McVersion.PISTONS_IN_SB && MOVED_TO_MACHINES.contains(path)
                || com.simplebuilding.version.McVersion.ASTRAL_ENCHANTING && MOVED_TO_ENCHANTING.contains(path)
                || !com.simplebuilding.version.McVersion.HOPPERS_IN_SB && MOVED_TO_MACHINES_HOPPERS.contains(path)
                || com.simplebuilding.version.McVersion.AUTO_SMITHER_IN_MACHINES && MOVED_TO_MACHINES_SMITHER.contains(path)
                || !com.simplebuilding.version.McVersion.PADS_IN_SB && MOVED_TO_PADS.contains(path)
                || !com.simplebuilding.version.McVersion.BTOOLS_IN_SB && MOVED_TO_BTOOLS.contains(path)
                || !com.simplebuilding.version.McVersion.GADGETS_IN_SB && MOVED_TO_GADGETS.contains(path)
                || !com.simplebuilding.version.McVersion.TRIMS_IN_SB && MOVED_TO_TRIMS.contains(path)
                || !com.simplebuilding.version.McVersion.BLOCKS_IN_SB && MOVED_TO_BLOCKS.contains(path)
                || !com.simplebuilding.version.McVersion.RESONANCE_IN_SB && MOVED_TO_RESONANCE.contains(path);
    }

    private static final List<Option> ALL;

    static {
        List<Option> options = new ArrayList<>();
        walk(options, "", SimplebuildingConfig.class, List.of());
        ALL = Collections.unmodifiableList(options);
    }

    private ConfigOptions() {
    }

    /** All options in declaration order (the config screens show {@link #visible}). */
    public static List<Option> all() {
        return ALL;
    }

    /**
     * The fields of the {@link Option#hidden} options. Fabric (ModMenu's {@code AutoConfigClient.getGuiRegistry}) and
     * NeoForge (its own registry) register a cloth predicate transformer that turns these into no row and no tooltip;
     * Forge's own screen walks {@link #visible}. The screens save the whole object, so the values stay in the file.
     */
    public static Set<Field> hiddenFields() {
        return ALL.stream().filter(Option::hidden).map(Option::field).collect(java.util.stream.Collectors.toSet());
    }

    /** {@link #all} without the {@link Option#hidden} ones: what the config screens and {@code config list} show. */
    public static List<Option> visible() {
        return ALL.stream().filter(option -> !option.hidden()).toList();
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

        /** Moved to a sub-mod on this line: still loaded and saved, no longer shown ({@link #hidden(String)}). */
        public boolean hidden() {
            return ConfigOptions.hidden(path);
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
