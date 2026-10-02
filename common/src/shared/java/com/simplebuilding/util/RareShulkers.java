package com.simplebuilding.util;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blocks.custom.ChestTier;
import com.simplebuilding.config.ServerTuning;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Seltene Shulker der End-Stadt (Besitzer 2026-10-02): beim Erzeugen einer End-Stadt wird jeder Wachposten-Shulker
 * mit {@code server.loot.reinforcedShulkerPercent} (2 %, hoechstens 10 %) verstaerkt oder mit
 * {@code server.loot.enderiteShulkerPercent} (0,5 %, hoechstens 5 %) zum Enderit-Shulker.
 *
 * <p><b>Stufe = Lebens-Modifikator.</b> Die Stufe steckt als Modifikator auf der maximalen Gesundheit
 * ({@link #REINFORCED_HEALTH_ID} +50 % = 1,5-fach, {@link #ENDERITE_HEALTH_ID} +200 % = 3-fach). Vanilla speichert ihn
 * mit dem Shulker und schickt ihn an die Clients (maximale Gesundheit wird synchronisiert) - der Renderer zeigt daran
 * die Huelle der Stufe ({@code ShulkerRendererMixin}), ganz ohne eigene Datenleitung.
 *
 * <p><b>Drops:</b> 0-2 Schalen der eigenen Stufe zusaetzlich zur Vanilla-Beute ({@link #dropShells}).
 */
public final class RareShulkers {
    public static final Identifier REINFORCED_HEALTH_ID = Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "reinforced_shulker");
    public static final Identifier ENDERITE_HEALTH_ID = Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "enderite_shulker");
    /** Leben verstaerkt: 1,5-fach. */
    public static final double REINFORCED_HEALTH_FACTOR = 1.5;
    /** Leben Enderit: 3-fach. */
    public static final double ENDERITE_HEALTH_FACTOR = 3.0;
    /** Hoechstens so viele Stufen-Schalen je Tod (gleichverteilt 0..MAX). */
    public static final int MAX_SHELL_DROPS = 2;

    private RareShulkers() {
    }

    /** Die Stufe aus dem Wurf {@code r} in [0, 1): erst Enderit, dann verstaerkt, sonst keine. */
    public static @Nullable ChestTier tierFor(double r, double enderiteChance, double reinforcedChance) {
        if (r < enderiteChance) {
            return ChestTier.ENDERITE;
        }
        return r < enderiteChance + reinforcedChance ? ChestTier.REINFORCED : null;
    }

    /** End-Stadt-Wachposten (Mixin auf {@code handleDataMarker}): wuerfelt die Stufe und setzt sie. */
    public static void onEndCitySpawn(Shulker shulker, RandomSource random) {
        if (!com.simplebuilding.version.McVersion.RARE_STRUCTURE_FINDS) {
            return;
        }
        ChestTier tier = tierFor(random.nextDouble(), ServerTuning.enderiteShulkerChance(), ServerTuning.reinforcedShulkerChance());
        if (tier != null) {
            apply(shulker, tier);
            if (ServerTuning.endermitesPerRareShulker() > 0) {
                shulker.addTag(ESCORT_TAG);
            }
        }
    }

    // =====================================================================================
    // Endermiten-Begleitung (Besitzer 2026-10-02)
    // =====================================================================================

    /**
     * Ein seltener Shulker, dessen Endermiten noch ausstehen (Entity-Tag, wird mit dem Shulker gespeichert). Die
     * Endermiten kommen nicht schon beim Erzeugen der Struktur: Vanilla wuerde sie entfernen, bevor ein Spieler ankommt
     * (Monster verschwinden fern jedes Spielers, Endermiten nach 2 Minuten). Sie erscheinen einmal, sobald ein Spieler
     * (nicht Kreativ/Zuschauer) in {@link #ESCORT_TRIGGER_RANGE} Bloecken ist; der Tag ist dann weg - nichts wiederholt sich.
     */
    public static final String ESCORT_TAG = "simplebuilding.endermite_escort";
    /** Ab dieser Spielernaehe (Bloecke) erscheinen die Endermiten. */
    public static final double ESCORT_TRIGGER_RANGE = 24.0;
    /** Endermiten erscheinen hoechstens so weit (waagrecht, Bloecke) vom Shulker. */
    public static final int ESCORT_RADIUS = 3;
    /** Versuche je Endermite, einen sicheren Platz zu finden. */
    private static final int ESCORT_ATTEMPTS = 12;
    /** Geprueft wird nur jede Sekunde. */
    private static final int ESCORT_CHECK_INTERVAL = 20;

    /** Jeder Tick eines Shulkers auf dem Server (Mixin auf {@code Shulker#tick}). */
    public static void tickEscort(Shulker shulker) {
        if (!(shulker.level() instanceof ServerLevel level) || shulker.tickCount % ESCORT_CHECK_INTERVAL != 0
                || !shulker.entityTags().contains(ESCORT_TAG) || level.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) {
            // Auf Friedlich laesst Vanilla keine Endermiten zu; die Begleitung wartet dann, statt verloren zu gehen.
            return;
        }
        if (level.getNearestPlayer(shulker.getX(), shulker.getY(), shulker.getZ(), ESCORT_TRIGGER_RANGE,
                net.minecraft.world.entity.EntitySelector.NO_CREATIVE_OR_SPECTATOR) == null) {
            return;
        }
        triggerEscort(level, shulker);
    }

    /**
     * Loest die ausstehende Begleitung aus (einmalig: der Tag geht dabei weg). Liefert die Zahl der Endermiten, 0 ohne
     * ausstehende Begleitung oder auf Friedlich (dann bleibt sie ausstehend).
     */
    public static int triggerEscort(ServerLevel level, Shulker shulker) {
        if (!shulker.entityTags().contains(ESCORT_TAG) || level.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) {
            return 0;
        }
        shulker.removeTag(ESCORT_TAG);
        return spawnEscort(level, shulker, ServerTuning.endermitesPerRareShulker());
    }

    /**
     * Setzt bis zu {@code count} Endermiten auf sichere Plaetze nahe dem Shulker (Luft fuer den ganzen Koerper, fester
     * Boden, keine Fluessigkeit); nicht dauerhaft (Vanilla laesst sie nach 2 Minuten verschwinden). Liefert die Anzahl.
     */
    public static int spawnEscort(ServerLevel level, Shulker shulker, int count) {
        RandomSource random = shulker.getRandom();
        BlockPos origin = shulker.blockPosition();
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            for (int attempt = 0; attempt < ESCORT_ATTEMPTS; attempt++) {
                BlockPos pos = origin.offset(random.nextInt(2 * ESCORT_RADIUS + 1) - ESCORT_RADIUS, random.nextInt(3) - 1,
                        random.nextInt(2 * ESCORT_RADIUS + 1) - ESCORT_RADIUS);
                if (!isSafeSpot(level, pos)) {
                    continue;
                }
                net.minecraft.world.entity.monster.Endermite mite = net.minecraft.world.entity.EntityTypes.ENDERMITE.spawn(
                        level, pos, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
                if (mite != null) {
                    spawned++;
                    break;
                }
            }
        }
        return spawned;
    }

    /** Fester Boden darunter, keine Fluessigkeit, und die Endermite passt ohne Kollision hinein. */
    public static boolean isSafeSpot(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)
                || !level.getFluidState(pos).isEmpty()) {
            return false;
        }
        return level.noCollision(net.minecraft.world.entity.EntityTypes.ENDERMITE.getSpawnAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5));
    }

    /** Macht {@code shulker} zur Stufe {@code tier} (verstaerkt oder Enderit; Netherit gibt es nicht) mit vollem Leben. */
    public static void apply(Shulker shulker, ChestTier tier) {
        AttributeInstance health = shulker.getAttribute(Attributes.MAX_HEALTH);
        if (health == null || tier == ChestTier.NETHERITE) {
            return;
        }
        health.removeModifier(REINFORCED_HEALTH_ID);
        health.removeModifier(ENDERITE_HEALTH_ID);
        boolean enderite = tier == ChestTier.ENDERITE;
        health.addPermanentModifier(new AttributeModifier(enderite ? ENDERITE_HEALTH_ID : REINFORCED_HEALTH_ID,
                (enderite ? ENDERITE_HEALTH_FACTOR : REINFORCED_HEALTH_FACTOR) - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        shulker.setHealth(shulker.getMaxHealth());
    }

    /** Die Stufe eines Shulkers (auf Server und Client), oder null. */
    public static @Nullable ChestTier tierOf(Shulker shulker) {
        AttributeInstance health = shulker.getAttribute(Attributes.MAX_HEALTH);
        if (health == null) {
            return null;
        }
        if (health.getModifier(ENDERITE_HEALTH_ID) != null) {
            return ChestTier.ENDERITE;
        }
        return health.getModifier(REINFORCED_HEALTH_ID) != null ? ChestTier.REINFORCED : null;
    }

    /**
     * Die Huelle einer Stufe ({@code textures/entity/rare_shulker/<stufe>.png}, {@code tools/textures/shulker_shell_textures.py}):
     * die ungefaerbte Stufen-Shulkerkiste mit Vanillas Shulkerkopf; ein gefaerbter Stufen-Shulker zeigt sie ebenfalls.
     */
    public static Identifier textureOf(ChestTier tier) {
        return Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "textures/entity/rare_shulker/" + tier.textureName() + ".png");
    }

    /** Beim Tod (Mixin auf {@code Mob#dropCustomDeathLoot}): 0-2 Schalen der eigenen Stufe. */
    public static void dropShells(ServerLevel level, Shulker shulker) {
        ChestTier tier = tierOf(shulker);
        Item shell = tier == null ? null : ShulkerShells.shellOf(tier);
        if (shell == null) {
            return;
        }
        int count = shulker.getRandom().nextInt(MAX_SHELL_DROPS + 1);
        if (count > 0) {
            shulker.spawnAtLocation(level, new ItemStack(shell, count));
        }
    }
}
